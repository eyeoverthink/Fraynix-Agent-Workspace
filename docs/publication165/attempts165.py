from pathlib import Path
import json

r = Path.cwd()
baseline = Path('J:/Codex-Work/fraynix-baseline-164')
failed = json.loads((baseline/'build/study164-on-c/result.json').read_bytes())
assert not failed['passed'] and 'ENOENT' in failed['failure'] and 'validation.csv' in failed['failure']
assert sum(t.get('task', {}).get('status') == 'completed' for t in failed['tasks']) == 2
for name in ['on-a', 'off-a', 'off-b', 'on-b']:
    assert json.loads((r/f'build/study165-{name}/result.json').read_bytes())['passed']
for name in ['on-e', 'on-d']:
    assert json.loads((baseline/f'build/study164-{name}/result.json').read_bytes())['passed']
description = ('The first sparse published164 baseline attempt (on-c) completed two native workflows and then stopped because the original Delfys validation CSV was missing from that checkout. Both original CSV blobs were restored from published164; the old implementation and installed binaries remained unchanged. The failed result and log, preliminary lock and corrected pretrial lock are retained. Final baseline runs on-e and on-d bracket the four candidate sessions. All six final sessions passed on their first attempts under the corrected lock; no unsuccessful final trial was discarded or relabelled. The 12 focused Java checks passed, followed by the 91-case selected regression pass, 181 desktop checks and actual rendering checks. No acceptance threshold was relaxed.')
value = {'description': description, 'failedBaseline': 'baseline164/build/study164-on-c/result.json', 'failedBaselineCompletedTasks': 2, 'finalCompletedWorkflows': 60, 'operationalNotes': ['The initial sparse checkout needed its index materialized before its original build receipt could be checked.', 'A local JVM stack diagnostic returned access denied during focused tests; no bypass was attempted. The tests subsequently finished successfully.', 'A Python compile command initially passed a literal wildcard on Windows; compileall then checked the helper directory successfully. This was not an application or test failure.'], 'physicalValidationPending': True}
(r/'build/attempts165.json').write_text(json.dumps(value, indent=2)+'\n', encoding='utf8')
print(description)
