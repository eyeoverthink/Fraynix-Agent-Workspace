"""Pack, retrieve and verify original evidence without adding raw copies to Git."""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, re, urllib.request, zipfile


def digest(data):
    return hashlib.sha256(data).hexdigest()


def logical_name(name):
    assert isinstance(name, str) and name and '\\' not in name and ':' not in name
    assert not PurePosixPath(name).is_absolute() and '..' not in PurePosixPath(name).parts
    assert str(PurePosixPath(name)) == name, 'Noncanonical evidence name'


def inspect_public(value):
    if isinstance(value, dict):
        for key, item in value.items():
            if key == 'image':
                assert isinstance(item, list) and len(item) == 2 and all(
                    isinstance(n, (int, float)) for n in item), 'Private image payload refused'
            assert key not in ['base64', 'imageData', 'dataUrl'], 'Private image payload refused'
            inspect_public(item)
    elif isinstance(value, list):
        for item in value:
            inspect_public(item)
    elif isinstance(value, str):
        assert 'data:image/' not in value, 'Private image payload refused'


def pack(inputs, archive, manifest):
    assert not archive.exists() and not manifest.exists(), 'Never overwrite retained evidence'
    rows, objects = [], {}
    for item in inputs:
        logical, source = item['logicalPath'], Path(item['source'])
        logical_name(logical)
        raw = source.read_bytes()
        if source.suffix == '.json':
            inspect_public(json.loads(raw))
        for pattern in [rb'gh[pousr]_[A-Za-z0-9]{30,}', rb'github_pat_[A-Za-z0-9_]{40,}',
                        rb'sk-[A-Za-z0-9_-]{30,}', rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']:
            assert not re.search(pattern, raw), 'Potential secret in ' + logical
        checksum = digest(raw)
        member = 'objects/' + checksum
        rows.append({'logicalPath': logical, 'member': member, 'bytes': len(raw), 'sha256': checksum})
        objects[member] = raw
    assert len({row['logicalPath'] for row in rows}) == len(rows)
    assert sum(len(raw) for raw in objects.values()) <= 1_000_000_000
    catalog = {'format': 'fraynix.evidence-archive/v1', 'files': rows}
    archive.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive, 'x', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        objects['contents.json'] = (json.dumps(catalog, indent=2) + '\n').encode()
        for name, raw in sorted(objects.items()):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            output.writestr(info, raw)
    data = archive.read_bytes()
    catalog['archive'] = {'name': archive.name, 'bytes': len(data), 'sha256': digest(data)}
    manifest.write_text(json.dumps(catalog, indent=2) + '\n', encoding='utf8')
    verify(catalog, archive)
    return catalog


def verify(manifest, archive):
    assert manifest['format'] == 'fraynix.evidence-archive/v1'
    names = [row['logicalPath'] for row in manifest['files']]
    assert len(names) == len(set(names)), 'Duplicate logical names'
    for name in names:
        logical_name(name)
    raw = Path(archive).read_bytes()
    assert len(raw) == manifest['archive']['bytes'] and digest(raw) == manifest['archive']['sha256']
    with zipfile.ZipFile(archive) as source:
        members = source.namelist()
        assert len(members) == len(set(members)), 'Duplicate archive members'
        expected = {row['member'] for row in manifest['files']} | {'contents.json'}
        assert set(members) == expected, 'Unexpected archive member'
        assert json.loads(source.read('contents.json')) == {
            'format': manifest['format'], 'files': manifest['files']}
        assert sum(item.file_size for item in source.infolist()) <= 1_001_000_000
        for row in manifest['files']:
            assert row['member'] == 'objects/' + row['sha256']
            data = source.read(row['member'])
            assert len(data) == row['bytes'] and digest(data) == row['sha256'], row['logicalPath']
    return {'passed': True, 'originalFiles': len(manifest['files']), 'archiveBytes': len(raw)}


def original(manifest, archive, logical):
    row = next(row for row in manifest['files'] if row['logicalPath'] == logical)
    with zipfile.ZipFile(archive) as source:
        data = source.read(row['member'])
    assert len(data) == row['bytes'] and digest(data) == row['sha256']
    return data


def download(manifest, destination):
    assert not destination.exists(), 'Choose a new download path'
    url = manifest['archive']['url']
    assert url.startswith('https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/download/')
    pending = destination.with_suffix(destination.suffix + '.part')
    assert not pending.exists()
    destination.parent.mkdir(parents=True, exist_ok=True)
    size, checksum = 0, hashlib.sha256()
    with urllib.request.urlopen(url, timeout=60) as response, pending.open('xb') as output:
        while data := response.read(65536):
            size += len(data)
            assert size <= manifest['archive']['bytes'], 'Remote evidence exceeds its manifest'
            checksum.update(data)
            output.write(data)
    assert size == manifest['archive']['bytes'] and checksum.hexdigest() == manifest['archive']['sha256']
    pending.rename(destination)
    return verify(manifest, destination)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['pack', 'verify', 'download'])
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--archive', type=Path, required=True)
    parser.add_argument('--inputs', type=Path)
    args = parser.parse_args()
    if args.command == 'pack':
        value = pack(json.loads(args.inputs.read_bytes()), args.archive, args.manifest)
        print(json.dumps({'files': len(value['files']), 'archive': value['archive']}))
    else:
        value = json.loads(args.manifest.read_bytes())
        result = verify(value, args.archive) if args.command == 'verify' else download(value, args.archive)
        print(json.dumps(result))
