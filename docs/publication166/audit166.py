"""Verify the release's Git blobs, every frozen edition and downloaded originals."""
from pathlib import Path
import datetime
import hashlib
import json
import subprocess
import sys

root = Path.cwd()
sys.path.insert(0, str(root/'scripts'))
import history_archive
import evidence_archive
ref, expected = sys.argv[1:3]
commit = subprocess.check_output(['git', 'rev-parse', ref], text=True).strip()
assert commit == expected
sha = lambda data: hashlib.sha256(data).hexdigest()
with history_archive.GitEvidence(root, commit) as source:
    def read(name): return json.loads(source.read(name))
    lock = read('docs/DOMAIN_EXTRACTION_166_LOCK.json')
    for name, checksum in lock['sourceHashes'].items():
        assert sha(history_archive.canonical(source.read(name), 'text')) == checksum, name
    inventory = read('docs/readmes/166/file-inventory.json')
    for row in inventory:
        raw = source.read(row['path'])
        if row['hashKind'] == 'utf8-lf': raw = history_archive.canonical(raw, 'text')
        assert len(raw) == row['bytes'] and sha(raw) == row['sha256'], row['path']
    frozen = history_archive.catalog(source)
    prior = json.loads(subprocess.check_output(['git', 'show',
        '671a3605d7481e7e5994b75265bc7fab8ce70578:docs/readmes/editions.json']))
    current = read('docs/readmes/editions.json')
    assert current['latest'] == 166 and current['editions'][:-1] == prior['editions']
    manifest = read('docs/readmes/166/original-evidence.json')
    archive = root/'build/downloaded-evidence166.zip'
    evidence_archive.verify(manifest, archive)
    for row in manifest['files']:
        original = evidence_archive.original(manifest, archive, row['logicalPath'])
        assert original == (root/row['logicalPath']).read_bytes(), row['logicalPath']
    assert read('docs/readmes/166/verification.json')['passed']
    for path in ['build/study166-on-c/result.json', 'build/compatibility166/result.json',
                 'build/migration166/candidate.json']:
        assert json.loads(evidence_archive.original(manifest, archive, path))['passed']
    result = {'passed': True, 'verifiedAtUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'ref': ref, 'commit': commit, 'remoteFetched': ref.startswith('refs/verify/'),
        'editions': 166, 'frozenHashes': len(frozen['files']), 'canonicalSourceFiles': len(inventory),
        'evidenceFiles': len(manifest['files']), 'allOriginalReceiptsCompared': True,
        'historicalSnapshotsUnchanged': True, 'privateCameraPixelsPublished': False}
(root/'build/publication-audit166.json').write_text(json.dumps(result, indent=2)+'\n')
print(json.dumps(result))
