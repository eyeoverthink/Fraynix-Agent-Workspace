"""Check the exact published tree and re-read the attached evidence."""
from datetime import datetime, timezone
from hashlib import sha256
import json
from pathlib import Path
import subprocess
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive
import history_archive

ref, expected = sys.argv[1:]
commit = subprocess.check_output(['git', 'rev-parse', ref], text=True).strip()
assert commit == expected


def digest(data):
    return sha256(data).hexdigest()


with history_archive.GitEvidence(root, commit) as source:
    def read(name):
        return json.loads(source.read(name))

    lock = read('docs/RELAY_CLEARANCE_CHAIN_178_LOCK.json')
    for name, expected_hash in lock['sourceHashes'].items():
        raw = source.read(name)
        if name not in lock['binaryPaths']:
            raw = history_archive.canonical(raw, 'text')
        assert digest(raw) == expected_hash, name

    inventory = read('docs/readmes/178/file-inventory.json')
    for row in inventory:
        raw = source.read(row['path'])
        if row['hashKind'] == 'utf8-lf':
            raw = history_archive.canonical(raw, 'text')
        assert len(raw) == row['bytes'] and digest(raw) == row['sha256'], row['path']

    catalog = history_archive.catalog(source)
    previous = json.loads(subprocess.check_output(['git', 'show', 'e1faaacfa:docs/readmes/editions.json']))
    current = read('docs/readmes/editions.json')
    assert current['latest'] == 178 and current['editions'][:-1] == previous['editions']
    verification = read('docs/readmes/178/verification.json')
    assert verification['passed'] and len(verification['trials']) == 2
    assert verification['revisedPlaytestRefused'] and verification['revisedDashboardRefused']
    assert verification['planningCalls'] == 0 and verification['archive']['passed'] and verification['priorRampChain']['passed']

    manifest = read('docs/readmes/178/original-evidence.json')
    archive = root / 'build/downloaded-evidence178.zip'
    evidence_archive.verify(manifest, archive)
    for item in manifest['files']:
        raw = evidence_archive.original(manifest, archive, item['logicalPath'])
        assert raw == (root / item['logicalPath']).read_bytes(), item['logicalPath']
    subprocess.run(['git', 'merge-base', '--is-ancestor', lock['source'], commit], check=True)
    result = {'passed': True, 'verifiedAtUtc': datetime.now(timezone.utc).isoformat(), 'ref': ref, 'commit': commit,
              'remoteFetched': ref.startswith('refs/verify/'), 'editions': 178, 'frozenHashes': len(catalog['files']),
              'sourceFiles': len(inventory), 'originalFiles': len(manifest['files']), 'priorEntriesUnchanged': True}

(root / 'build/publication-audit178.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf8')
print(json.dumps(result))
