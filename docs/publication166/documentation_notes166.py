from pathlib import Path
import json
import shutil

root = Path.cwd()
result = json.loads((root/'build/documentation-io166.json').read_bytes())
assert result['passed'] and result['changedCopyRefused'] and result['originalUnchanged']
note = ('The initial full documentation audit was stopped after reading8.62GB through about2.13million '
        'small reads. The binary SHA-256 path now uses a64KiB sequential file buffer, retaining the '
        'same bytes, hash algorithm, text canonicalization and sharing rule. Four alternating '
        'hashes match an existing published binary; a changed copy is refused and its original '
        'remains unchanged. These short local timings are affected by caching and do not establish '
        'a general speed improvement. The complete documentation check is rerun with every frozen '
        'source and link check preserved. This maintenance-tool change does not affect the app '
        'runtime or its locked workflow trials.')
for name in ['README-166.md', 'README.md']:
    path = root/name
    text = path.read_text(encoding='utf8')
    assert '## Reversible archive rehearsal' in text
    path.write_text(text.replace('## Reversible archive rehearsal', note+'\n\n## Reversible archive rehearsal'), encoding='utf8')
for name in ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md']:
    path = root/name
    text = path.read_text(encoding='utf8')
    offset = text.index('\n\n')
    path.write_text(text[:offset]+'\n\n'+note+text[offset:], encoding='utf8')
shutil.copy2(root/'build/documentation-io166.json', root/'docs/readmes/166/documentation-io.json')
