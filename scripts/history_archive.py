"""Read immutable Git evidence and restore verified originals into a new directory.

This is an archival adapter, not permission to remove the original repository.
The catalog itself must be pinned by a separately retained SHA-256 digest.
"""
from pathlib import Path
import argparse
import gzip
import hashlib
import json
import re
import subprocess

import evidence_archive


def digest(data):
    return hashlib.sha256(data).hexdigest()


def canonical(data, kind):
    if kind == 'binary':
        return data
    if kind != 'text':
        raise ValueError('Unknown canonicalization')
    return data.decode('utf-8-sig').replace('\r\n', '\n').encode()


class GitEvidence:
    """Read named, pinned blobs without trusting files in the working tree."""

    def __init__(self, repository, commit):
        if not re.fullmatch('[0-9a-f]{40}', commit):
            raise ValueError('A complete immutable commit is required')
        self.repository = Path(repository)
        self.commit = commit
        raw = subprocess.check_output(
            ['git', 'ls-tree', '-r', '-z', '-l', commit], cwd=self.repository)
        self.files = {}
        for record in raw.split(b'\0'):
            if not record:
                continue
            metadata, name = record.split(b'\t', 1)
            mode, kind, oid, size = metadata.decode().split()
            name = name.decode('utf8')
            evidence_archive.logical_name(name)
            if kind != 'blob' or mode not in ('100644', '100755'):
                raise ValueError('Only ordinary tracked files are supported: ' + name)
            self.files[name] = {'blob': oid, 'bytes': int(size), 'mode': mode}
        self.process = subprocess.Popen(
            ['git', '-c', 'core.packedGitLimit=128m', '-c', 'core.packedGitWindowSize=16m',
             '-c', 'core.deltaBaseCacheLimit=32m', 'cat-file', '--batch'], cwd=self.repository,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE)

    def read(self, name):
        row = self.files[name]
        self.process.stdin.write((row['blob'] + '\n').encode())
        self.process.stdin.flush()
        oid, kind, size = self.process.stdout.readline().decode().split()
        if (oid, kind, int(size)) != (row['blob'], 'blob', row['bytes']):
            raise ValueError('Unexpected Git object')
        data = self.process.stdout.read(row['bytes'])
        if len(data) != row['bytes'] or self.process.stdout.read(1) != b'\n':
            raise ValueError('Truncated Git object')
        return data

    def close(self):
        self.process.stdin.close()
        self.process.wait(timeout=30)
        self.process.stdout.close()

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()


def catalog(source):
    editions = json.loads(source.read('docs/readmes/editions.json'))
    rows, seen, hashes = [], set(), {}
    for edition in editions['editions']:
        entries = [{'path': edition['markdown'], 'kind': 'text',
                    'sha256': edition['markdownSha256']}] + edition['snapshotFiles']
        for entry in entries:
            name = entry['path']
            if name in seen:
                raise ValueError('Duplicate frozen path: ' + name)
            seen.add(name)
            blob = source.files[name]
            key = (blob['blob'], entry['kind'])
            if key not in hashes:
                raw = source.read(name)
                hashes[key] = (digest(raw), digest(canonical(raw, entry['kind'])))
            raw_hash, frozen_hash = hashes[key]
            if frozen_hash != entry['sha256']:
                raise ValueError('Frozen hash mismatch: ' + name)
            rows.append({'path': name, 'edition': edition['number'],
                         'kind': entry['kind'], 'canonicalSha256': frozen_hash,
                         'sha256': raw_hash, **blob})
    return {'format': 'fraynix.history-catalog/v1',
            'sourceRepository': 'https://github.com/eyeoverthink/Fraynix-Agent-Workspace',
            'sourceCommit': source.commit, 'files': rows,
            'warning': 'Unmigrated evidence still requires the retained original Git objects. '
                       'This catalog is an index, not a backup.'}


def load_catalog(path, expected_sha256):
    raw = Path(path).read_bytes()
    if digest(raw) != expected_sha256:
        raise ValueError('Untrusted catalog digest')
    value = json.loads(gzip.decompress(raw) if Path(path).suffix == '.gz' else raw)
    if value['format'] != 'fraynix.history-catalog/v1':
        raise ValueError('Unknown catalog format')
    names = set()
    for row in value['files']:
        evidence_archive.logical_name(row['path'])
        if row['path'] in names:
            raise ValueError('Duplicate catalog path')
        names.add(row['path'])
    return value


def restore(value, names, destination, *, git=None, archive=None, manifest=None):
    """Select exactly one provider. Never mask missing/corrupt archives with Git."""
    destination = Path(destination)
    if destination.exists() or destination.is_symlink():
        raise ValueError('Restore requires a new destination')
    parent = destination.parent.resolve(strict=True)
    if parent != destination.parent.absolute():
        raise ValueError('Linked restore parents are not supported')
    if (git is None) == (archive is None):
        raise ValueError('Select exactly one evidence provider')
    if git and git.commit != value['sourceCommit']:
        raise ValueError('Original commit differs')
    if archive:
        evidence_archive.verify(manifest, archive)
    rows = {row['path']: row for row in value['files']}
    validated = []
    if len(names) != len(set(names)):
        raise ValueError('Duplicate requested path')
    for name in names:
        evidence_archive.logical_name(name)
        row = rows[name]
        if git and git.files[name]['blob'] != row['blob']:
            raise ValueError('Original blob differs')
        raw = git.read(name) if git else evidence_archive.original(manifest, archive, name)
        if (len(raw) != row['bytes'] or digest(raw) != row['sha256'] or
                digest(canonical(raw, row['kind'])) != row['canonicalSha256']):
            raise ValueError('Original evidence differs: ' + name)
        validated.append((name, raw))
    destination.mkdir()
    for name, raw in validated:
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(raw)
    return {'passed': True, 'restored': len(validated),
            'bytes': sum(len(raw) for _, raw in validated),
            'provider': 'original-git' if git else 'verified-archive'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--catalog', type=Path, required=True)
    parser.add_argument('--catalog-sha256', required=True)
    parser.add_argument('--destination', type=Path, required=True)
    parser.add_argument('--path', action='append', required=True)
    provider = parser.add_mutually_exclusive_group(required=True)
    provider.add_argument('--repository', type=Path)
    provider.add_argument('--archive', type=Path)
    parser.add_argument('--manifest', type=Path)
    args = parser.parse_args()
    value = load_catalog(args.catalog, args.catalog_sha256)
    if args.repository:
        with GitEvidence(args.repository, value['sourceCommit']) as source:
            result = restore(value, args.path, args.destination, git=source)
    else:
        result = restore(value, args.path, args.destination, archive=args.archive,
                         manifest=json.loads(args.manifest.read_bytes()))
    print(json.dumps(result))
