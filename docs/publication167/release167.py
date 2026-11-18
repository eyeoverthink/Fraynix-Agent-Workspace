from pathlib import Path
import json
import subprocess

root = Path.cwd()
def git(*args): return subprocess.check_output(['git', *args], text=True).strip()
def run(*args): subprocess.run(args, check=True)
assert git('branch', '--show-current') == 'edition167'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for path in ['docs/readmes/167/verification.json', 'build/documentation-check.json',
             'build/visual167.json', 'build/visual-review167.json']:
    assert json.loads((root/path).read_bytes())['passed'], path
run('git', '-c', 'gc.auto=0', 'fetch', 'origin', 'main')
run('git', 'merge-base', '--is-ancestor', 'origin/main', 'HEAD')
paths = ['AGENTS.md', 'README-CONTRIBUTORS.md', 'README.md', 'README-167.md',
         'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md',
         'docs/INTEGRATED_AGENT_ROADMAP.md', 'docs/PLAYABLE_LEVEL_CREATION_PLAN.md',
         'docs/ARCHITECTURE_RENEWAL_PLAN.md', 'docs/obsidian/00-home.md',
         'docs/obsidian/175-original-content.md', 'docs/obsidian/175-original-content.canvas',
         'docs/readmes/INDEX.md', 'docs/readmes/editions.json', 'docs/readmes/167', 'docs/publication167']
run('git', 'add', *paths)
run('git', 'diff', '--cached', '--check')
run('git', '-c', 'gc.auto=0', 'commit', '-m', 'Document edition 167 original playable content and restored archive batch')
assert not git('status', '--porcelain')
release = git('rev-parse', 'HEAD')
run('python', '-X', 'utf8', 'docs/publication167/audit167.py', 'HEAD', release)
run('git', 'tag', '-a', 'readme-167', '-m', 'Edition 167: original playable content and restored archive batch')
run('git', '-c', 'pack.threads=4', 'push', '--atomic', 'origin', 'HEAD:refs/heads/main',
    'HEAD:refs/heads/edition167', 'refs/tags/readme-167')
run('git', '-c', 'gc.auto=0', 'fetch', 'origin', 'refs/heads/main:refs/verify/edition167-main',
    'refs/heads/edition167:refs/verify/edition167-branch', 'refs/tags/readme-167:refs/verify/edition167-tag')
assert git('rev-parse', 'refs/verify/edition167-branch') == release
assert git('rev-parse', 'refs/verify/edition167-tag^{commit}') == release
run('python', '-X', 'utf8', 'docs/publication167/audit167.py', 'refs/verify/edition167-main', release)
receipt = {'edition': 167, 'releaseCommit': release, 'annotatedTag': 'readme-167',
           'tagObject': git('rev-parse', 'readme-167'),
           'externalEvidence': json.loads((root/'build/evidence167-upload.json').read_bytes()),
           'audit': json.loads((root/'build/publication-audit167.json').read_bytes()),
           'validation': 'docs/readmes/167/verification.json',
           'documentation': json.loads((root/'build/documentation-check.json').read_bytes()),
           'ci': json.loads((root/'build/ci167-before-publication.json').read_bytes())}
(root/'docs/PUBLICATION_167.json').write_text(json.dumps(receipt, indent=2)+'\n')
status = ('## Edition 167 — published and fetched back\n\n2026-11-18 UTC. Release '+release+
          '; main, edition167 and readme-167 pushed atomically and fetched. All source inventory '
          'and historical frozen hashes verified from fetched Git objects. See docs/PUBLICATION_167.json. '
          'Original Relay Garden content and its real controller playthrough verified. Another 76 frozen '
          'files restored from downloaded evidence; original history retained. Full inherited-asset replacement and archival migration '
          'remain pending. No new physical accuracy or model training claimed.\n\n')
metadata = ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md',
            'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md']
for name in metadata:
    path = root/name
    path.write_text(status+path.read_text(encoding='utf-8-sig'), encoding='utf8')
path = root/'README.md'
path.write_text(path.read_text().replace('\n', '\n\n[Published and fetched receipt](docs/PUBLICATION_167.json) · '
    '[Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-167)\n', 1), encoding='utf8')
metadata += ['README.md', 'docs/PUBLICATION_167.json']
run('git', 'add', *metadata)
run('git', 'diff', '--cached', '--check')
run('git', '-c', 'gc.auto=0', 'commit', '-m', 'Record fetched edition 167 publication and continuity')
assert not git('status', '--porcelain')
head = git('rev-parse', 'HEAD')
assert set(git('diff', '--name-only', release, head).splitlines()) <= set(metadata)
run('git', '-c', 'pack.threads=4', 'push', '--atomic', 'origin', 'HEAD:refs/heads/main', 'HEAD:refs/heads/edition167')
run('git', '-c', 'gc.auto=0', 'fetch', 'origin', 'refs/heads/main:refs/verify/edition167-final-main',
    'refs/heads/edition167:refs/verify/edition167-final-branch')
assert git('rev-parse', 'refs/verify/edition167-final-main') == head == git('rev-parse', 'refs/verify/edition167-final-branch')
for name in metadata:
    assert subprocess.check_output(['git', 'show', 'HEAD:'+name]) == subprocess.check_output(
        ['git', 'show', 'refs/verify/edition167-final-main:'+name])
result = {'passed': True, 'release': release, 'finalMain': head, 'tag': 'readme-167',
          'releaseAudit': receipt['audit'], 'finalMetadataCompared': len(metadata)}
(root/'build/final-publication167.json').write_text(json.dumps(result, indent=2)+'\n')
print(json.dumps(result))
