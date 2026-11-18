from pathlib import Path
import json
import subprocess

root = Path.cwd()
def git(*args): return subprocess.check_output(['git', *args], text=True).strip()
def run(*args): subprocess.run(args, check=True)
assert git('branch', '--show-current') == 'edition166'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for path in ['docs/readmes/166/verification.json', 'build/documentation-check.json',
             'build/visual166.json', 'build/visual-review166.json']:
    assert json.loads((root/path).read_bytes())['passed'], path
run('git', '-c', 'gc.auto=0', 'fetch', 'origin', 'main')
run('git', 'merge-base', '--is-ancestor', 'origin/main', 'HEAD')
paths = ['AGENTS.md', 'README-CONTRIBUTORS.md', 'README.md', 'README-166.md',
         'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md',
         'docs/INTEGRATED_AGENT_ROADMAP.md', 'docs/PLAYABLE_LEVEL_CREATION_PLAN.md',
         'docs/ARCHITECTURE_RENEWAL_PLAN.md', 'docs/obsidian/00-home.md',
         'docs/obsidian/174-domain-and-archive.md', 'docs/obsidian/174-domain-and-archive.canvas',
         'docs/readmes/INDEX.md', 'docs/readmes/editions.json', 'docs/readmes/166', 'docs/publication166']
run('git', 'add', *paths)
run('git', 'diff', '--cached', '--check')
run('git', '-c', 'gc.auto=0', 'commit', '-m', 'Document edition 166 domain extraction and reversible archive rehearsal')
assert not git('status', '--porcelain')
release = git('rev-parse', 'HEAD')
run('python', '-X', 'utf8', 'docs/publication166/audit166.py', 'HEAD', release)
run('git', 'tag', '-a', 'readme-166', '-m', 'Edition 166: guarded domain extraction and archive rehearsal')
run('git', '-c', 'pack.threads=4', 'push', '--atomic', 'origin', 'HEAD:refs/heads/main',
    'HEAD:refs/heads/edition166', 'refs/tags/readme-166')
run('git', '-c', 'gc.auto=0', 'fetch', 'origin', 'refs/heads/main:refs/verify/edition166-main',
    'refs/heads/edition166:refs/verify/edition166-branch', 'refs/tags/readme-166:refs/verify/edition166-tag')
assert git('rev-parse', 'refs/verify/edition166-branch') == release
assert git('rev-parse', 'refs/verify/edition166-tag^{commit}') == release
run('python', '-X', 'utf8', 'docs/publication166/audit166.py', 'refs/verify/edition166-main', release)
receipt = {'edition': 166, 'releaseCommit': release, 'annotatedTag': 'readme-166',
           'tagObject': git('rev-parse', 'readme-166'),
           'externalEvidence': json.loads((root/'build/evidence166-upload.json').read_bytes()),
           'audit': json.loads((root/'build/publication-audit166.json').read_bytes()),
           'validation': 'docs/readmes/166/verification.json',
           'documentation': json.loads((root/'build/documentation-check.json').read_bytes()),
           'ci': json.loads((root/'build/ci166-before-publication.json').read_bytes())}
(root/'docs/PUBLICATION_166.json').write_text(json.dumps(receipt, indent=2)+'\n')
status = ('## Edition 166 — published and fetched back\n\n2026-11-18 UTC. Release '+release+
          '; main, edition166 and readme-166 pushed atomically and fetched. All source inventory '
          'and historical frozen hashes verified from fetched Git objects. See docs/PUBLICATION_166.json. '
          'Eighteen routes extracted; ten old workflows and ten fresh requests verified. Small local '
          'clone built/tested; original history retained. Asset rights and full archival migration '
          'remain pending. No new physical accuracy or model training claimed.\n\n')
metadata = ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md',
            'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md']
for name in metadata:
    path = root/name
    path.write_text(status+path.read_text(encoding='utf-8-sig'), encoding='utf8')
path = root/'README.md'
path.write_text(path.read_text().replace('\n', '\n\n[Published and fetched receipt](docs/PUBLICATION_166.json) · '
    '[Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-166)\n', 1), encoding='utf8')
metadata += ['README.md', 'docs/PUBLICATION_166.json']
run('git', 'add', *metadata)
run('git', 'diff', '--cached', '--check')
run('git', '-c', 'gc.auto=0', 'commit', '-m', 'Record fetched edition 166 publication and continuity')
assert not git('status', '--porcelain')
head = git('rev-parse', 'HEAD')
assert set(git('diff', '--name-only', release, head).splitlines()) <= set(metadata)
run('git', '-c', 'pack.threads=4', 'push', '--atomic', 'origin', 'HEAD:refs/heads/main', 'HEAD:refs/heads/edition166')
run('git', '-c', 'gc.auto=0', 'fetch', 'origin', 'refs/heads/main:refs/verify/edition166-final-main',
    'refs/heads/edition166:refs/verify/edition166-final-branch')
assert git('rev-parse', 'refs/verify/edition166-final-main') == head == git('rev-parse', 'refs/verify/edition166-final-branch')
for name in metadata:
    assert subprocess.check_output(['git', 'show', 'HEAD:'+name]) == subprocess.check_output(
        ['git', 'show', 'refs/verify/edition166-final-main:'+name])
result = {'passed': True, 'release': release, 'finalMain': head, 'tag': 'readme-166',
          'releaseAudit': receipt['audit'], 'finalMetadataCompared': len(metadata)}
(root/'build/final-publication166.json').write_text(json.dumps(result, indent=2)+'\n')
print(json.dumps(result))
