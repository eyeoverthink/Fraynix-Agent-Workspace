"""Archive the next frozen edition without deleting or rewriting its originals."""
from pathlib import Path
import hashlib, json, sys, shutil, time

ROOT = Path.cwd()
sys.path.insert(0, str(ROOT/'scripts'))
import history_archive as history
import evidence_archive as archive

OUT = ROOT/'build/archive168'
CATALOG = ROOT/'build/migration166/catalog.json.gz'
CATALOG_SHA = 'c6d1d9118bab71c57040c6b1e328f8fde0801a10b62ef176f41d7b3f09aa4ce2'
SOURCE = '671a3605d7481e7e5994b75265bc7fab8ce70578'

def write(path, value): path.write_text(json.dumps(value, indent=2)+'\n', encoding='utf8')

def prepare():
    OUT.mkdir()  # Refuse overwriting an earlier attempt.
    start = time.perf_counter()
    full = history.load_catalog(CATALOG, CATALOG_SHA)
    assert full['sourceCommit'] == SOURCE
    rows = [r for r in full['files'] if r['edition'] == 162]
    assert rows
    subset = dict(full, files=rows)
    write(OUT/'catalog162.json', subset)
    names = [r['path'] for r in rows]
    with history.GitEvidence(ROOT, SOURCE) as source:
        restored = history.restore(subset, names, OUT/'git-originals', git=source)
        manifest = archive.pack([{'logicalPath': n, 'source': OUT/'git-originals'/n} for n in names],
            OUT/'legacy162.zip', OUT/'legacy162-manifest.json')
        retrieved = history.restore(subset, names, OUT/'archive-originals', archive=OUT/'legacy162.zip', manifest=manifest)
        for name in names: assert (OUT/'archive-originals'/name).read_bytes() == source.read(name)
    # A separately named local volume copy, not a claim of off-site disaster recovery.
    second = Path('C:/Users/eyeka/Documents/Codex/evidence-vault/edition168-legacy162')
    second.mkdir(parents=True, exist_ok=False)
    for name in ['legacy162.zip', 'legacy162-manifest.json', 'catalog162.json']:
        shutil.copy2(OUT/name, second/name)
        assert (OUT/name).read_bytes() == (second/name).read_bytes()
    second_result = history.restore(subset, names, second/'restore-check',
        archive=second/'legacy162.zip', manifest=manifest)
    with history.GitEvidence(ROOT, SOURCE) as source:
        for name in names:
            assert (second/'restore-check'/name).read_bytes() == source.read(name)
    bad = OUT/'corrupt.zip'; raw = bytearray((OUT/'legacy162.zip').read_bytes()); raw[-1] ^= 1; bad.write_bytes(raw)
    refusals = []
    for provider, dest in [(bad, OUT/'bad-result'), (OUT/'missing.zip', OUT/'missing-result')]:
        try: history.restore(subset, names, dest, archive=provider, manifest=manifest)
        except (AssertionError, FileNotFoundError): refusals.append(provider.name)
        else: raise AssertionError('Invalid provider accepted')
        assert not dest.exists()
    result = {'passed': True, 'sourceCommit': SOURCE, 'editionArchived': 162,
        'parentCatalogSha256': CATALOG_SHA, 'catalogSha256': history.digest((OUT/'catalog162.json').read_bytes()),
        'originalFiles': len(rows), 'originalBytes': sum(r['bytes'] for r in rows), 'archive': manifest['archive'],
        'gitRestore': restored, 'archiveRestore': retrieved, 'secondCopyRestore': second_result,
        'secondCopyLocation': str(second), 'providerRefusals': refusals, 'seconds': time.perf_counter()-start,
        'scope': 'One additional frozen edition. Original repository unchanged; second local volume is not an off-site backup.'}
    write(OUT/'result.json', result)
    print(json.dumps(result))

def downloaded():
    outer = json.loads((ROOT/'build/evidence168-manifest.json').read_bytes())
    pack = ROOT/'build/downloaded-evidence168.zip'
    archive.verify(outer, pack)
    target = OUT/'downloaded'; target.mkdir()
    for name in ['legacy162.zip', 'legacy162-manifest.json', 'catalog162.json']:
        raw = archive.original(outer, pack, 'build/archive168/'+name)
        (target/name).write_bytes(raw)
        assert raw == (OUT/name).read_bytes()
    subset = history.load_catalog(target/'catalog162.json', history.digest((OUT/'catalog162.json').read_bytes()))
    result = history.restore(subset, [r['path'] for r in subset['files']], target/'restored',
        archive=target/'legacy162.zip', manifest=json.loads((target/'legacy162-manifest.json').read_bytes()))
    with history.GitEvidence(ROOT, SOURCE) as source:
        for row in subset['files']: assert (target/'restored'/row['path']).read_bytes() == source.read(row['path'])
    write(OUT/'downloaded-result.json', dict(result, downloadedOuterSha256=outer['archive']['sha256'],
        allOriginalGitBytesCompared=True))
    print(json.dumps(result))

if __name__ == '__main__': {'prepare': prepare, 'downloaded': downloaded}[sys.argv[1]]()
