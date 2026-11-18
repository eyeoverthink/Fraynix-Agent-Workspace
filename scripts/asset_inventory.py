"""Inventory actual tracked delivery inputs without inferring redistribution rights."""
from pathlib import PurePosixPath, Path
import argparse
import json
from history_archive import GitEvidence, digest


def inventory(source):
    notice_paths = ['THIRD_PARTY_NOTICES.md', 'licenses/MarioYoutube-LICENSE.txt',
                    'docs/mario-import-provenance.json', 'docs/provenance/IMPORT_MANIFEST.json']
    notices = [{'path': name, 'sha256': digest(source.read(name))} for name in notice_paths]
    mario = json.loads(source.read(notice_paths[2]))['imported']
    engine = json.loads(source.read(notice_paths[3]))['files']
    imported = {}
    for origin, rows in [(notice_paths[2], mario), (notice_paths[3], engine)]:
        for row in rows:
            imported.setdefault(row['path'], []).append({'record': origin,
                                                        'originalSha256': row['sha256']})
    rows = []
    media = {'.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg', '.ogg', '.wav', '.mp3',
             '.ttf', '.otf', '.woff', '.woff2', '.glb', '.gltf', '.obj', '.fbx', '.blend'}
    for name, blob in source.files.items():
        if name.startswith(('docs/readmes/', 'docs/publication', 'build/')):
            continue
        suffix = PurePosixPath(name).suffix.lower()
        selected = (name.startswith(('assets/', 'levels/', 'licenses/', 'libs/',
                                     'workspace-engine/src/main/resources/', 'models/')) or
                    suffix in media or name in imported or name == 'level.txt')
        if not selected:
            continue
        checksum = digest(source.read(name))
        origins = [{**record, 'exactBytesMatch': record['originalSha256'] == checksum}
                   for record in imported.get(name, [])]
        category = 'resource-or-code-review-required'
        if name.startswith(('assets/images/', 'assets/sounds/')) or name in (
                'level.txt', 'levels/mario-original.json'):
            category = 'game-content-rights-unresolved'
        elif name.startswith('licenses/'):
            category = 'license-notice-document'
        elif suffix == '.jar':
            category = 'vendored-binary-review-required'
        elif origins:
            category = 'import-record-present-not-a-rights-grant'
        rows.append({'path': name, 'bytes': blob['bytes'], 'blob': blob['blob'],
                     'sha256': checksum, 'classification': category, 'origins': origins})
    counts = {}
    for row in rows:
        counts[row['classification']] = counts.get(row['classification'], 0) + 1
    return {'format': 'fraynix.asset-inventory/v1', 'sourceCommit': source.commit,
            'scope': 'Tracked media, delivery resource roots, levels, model recipes, '
                     'vendored binaries, notices and recorded imported source; historical '
                     'snapshots excluded. Hash identity does not establish permissions.',
            'supportingRecords': notices, 'counts': counts, 'files': rows,
            'releaseBlockers': [
                'No application-wide project license has been selected.',
                'Supplied Mario-style images, sound and levels lack established redistribution rights.',
                'The imported MIT code notice is not clearance for third-party game artwork or music.',
                'Review all dependencies and vendored binaries, including the unidentified jbox2d version.',
                'A clean candidate still contains these inputs; it is not a rights-cleared public product.']}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repository', type=Path, default=Path('.'))
    p.add_argument('--commit', required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    if a.output.exists():
        raise ValueError('Choose a new inventory path')
    with GitEvidence(a.repository, a.commit) as source:
        value = inventory(source)
    a.output.write_text(json.dumps(value, indent=2) + '\n', encoding='utf8')
    print(json.dumps({'files': len(value['files']), 'counts': value['counts']}))
