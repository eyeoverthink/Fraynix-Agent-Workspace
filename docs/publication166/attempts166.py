"""Keep unsuccessful rehearsal evidence visible in the final explanation."""
from pathlib import Path
import json

root = Path.cwd()
failed = json.loads((root/'build/attempts166/compatibility-a.json').read_bytes())
assert not failed['passed'] and 'AssertionError' in failed['failure']
assert json.loads((root/'build/compatibility166/result.json').read_bytes())['passed']
note = ('The first saved-workflow compatibility attempt stopped after the original verifier '
        'passed its first parent: JavaScript deep comparison distinguished returned -0 from 0 '
        'in the retained JSON receipt. The harness now compares both in the receipt\'s JSON '
        'representation, where serialization canonicalizes -0 to 0. The failed result, log, '
        'earlier harness and preliminary locks are retained in the external evidence. A '
        'subsequent lock-only correction excludes the lock document from its own hash inventory. '
        'The first fresh application session passed all ten tasks. The initial historical audit '
        'was then stopped because Git mapped about14.6GB into its working set. Git object windows '
        'are now explicitly capped at128MiB,16MiB per window, with32MiB delta cache. The same '
        'full audit was restarted, archive tests repeated, and a second full app session run under '
        'a new lock. Both fresh sessions pass; the reported final comparison uses on-b. The final '
        'saved-workflow check passes all ten parents. No source, outcome or acceptance tolerance '
        'was relaxed.')
for name in ['README-166.md', 'README.md']:
    path = root/name
    text = path.read_text(encoding='utf8')
    text = text.replace('## Reversible archive rehearsal', '## Attempt history\n\n'+note+'\n\n## Reversible archive rehearsal')
    text = text.replace('## How the boundary works', 'The actual screenshots retain a known presentation issue: the generic timeline can show a request to choose a persistent/linked-game world while a recorded small-world task has already verified successfully. Expert cost-table headings are also cramped. These issues are visible, not cropped away, and remain for the interface consolidation pass.\n\n## How the boundary works')
    path.write_text(text, encoding='utf8')
for name in ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md',
             'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md']:
    path = root/name
    text = path.read_text(encoding='utf8')
    offset = text.index('\n\n')
    path.write_text(text[:offset]+'\n\n'+note+text[offset:], encoding='utf8')
(root/'docs/readmes/166/attempts.json').write_text(json.dumps({'description': note,
    'failedCompatibilityAttempts': 1, 'finalFreshSessionsFailed': 0,
    'originalAcceptanceChanged': False}, indent=2)+'\n')
print('Retained unsuccessful attempt documented')
