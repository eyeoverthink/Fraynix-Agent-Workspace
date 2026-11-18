"""Count the actual cloned Git tree and compare every exported source blob."""
from pathlib import Path
import json
import subprocess
import sys
import zipfile

root = Path.cwd()
sys.path.insert(0, str(root/'scripts'))
from history_archive import GitEvidence, digest
out = root/'build/migration166'
result = json.loads((out/'candidate.json').read_bytes())
expected = json.loads((out/'candidate-files.json').read_bytes())
clone = out/'clone'
commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=clone, text=True).strip()
assert commit == result['candidateCommit']
with GitEvidence(clone, commit) as candidate:
    assert set(candidate.files) == {row['path'] for row in expected} | {
        'history-catalog.json.gz', 'HISTORY-RETRIEVAL.md'}
    for row in expected:
        assert candidate.files[row['path']]['blob'] == row['blob'], row['path']
        assert candidate.files[row['path']]['bytes'] == row['bytes'], row['path']
    total = sum(row['bytes'] for row in candidate.files.values())
    assert total == result['treeBytes']
    count = len(candidate.files)
identities = json.loads((root/'build/native-identity166.json').read_bytes())['classes']
with zipfile.ZipFile(clone/'workspace-engine/build/install/workspace-engine/lib/workspace-engine.jar') as jar:
    for name, checksum in identities.items():
        assert digest(jar.read(name)) == checksum, name
value = {'passed': True, 'actualCloneFiles': count, 'actualTreeBytes': total,
         'sourceBlobsCompared': len(expected), 'unchangedCompiledClasses': len(identities),
         'scope': 'Actual cloned Git tree equals every selected source blob plus the pinned catalog and retrieval note; compiled native/parent identities also match.'}
(root/'build/candidate-crosscheck166.log').write_text(json.dumps(value, indent=2)+'\n')
print(json.dumps(value))
