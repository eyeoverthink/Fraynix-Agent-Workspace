from pathlib import Path
import json, subprocess, shutil
r = Path.cwd()
git = lambda *a: subprocess.check_output(['git', *a], text=True).strip()
run = lambda *a: subprocess.run(a, check=True)
audit = json.loads((r/'build/publication-audit115.json').read_text(encoding='utf8'))
assert audit['passed'] and audit['remoteFetched']
release = git('rev-parse', 'readme-115^{commit}')
assert release == audit['commit'] == git('rev-parse', 'HEAD')
ui = json.loads((r/'docs/readmes/115/actual-ui.json').read_text(encoding='utf8'))
validation = json.loads((r/'docs/readmes/115/verification.json').read_text(encoding='utf8'))
receipt = {
    'edition': 115, 'releaseCommit': release, 'annotatedTag': 'readme-115',
    'tagObject': git('rev-parse', 'readme-115'), 'audit': audit,
    'validation': {
        'javaMethods': len(validation['javaChecks']), 'desktopChecks': validation['desktopChecks'],
        'actualRequests': [{
            'request': x['prompt'], 'status': x['task']['status'], 'elapsedMs': x['elapsedMs'],
            'trialOutcomes': [t['status'] for t in x['child']['trials']],
            'audit': x['child'].get('audit')
        } for x in ui['requests']],
        'actualAbruptRecovery': True, 'referenceChecks': validation['referenceCases'],
        'modelCalls': validation['modelCalls'], 'guideWidths': [1440, 390],
        'scope': validation['scope']
    },
    'ci': {'scope': 'Local verification and fetched publication do not establish remote CI success.'}
}
ci = r/'build/release115-ci.json'
if ci.exists(): receipt['ci']['releaseCheck'] = json.loads(ci.read_text(encoding='utf8'))
(r/'docs/PUBLICATION_115.json').write_bytes((json.dumps(receipt, indent=2)+'\n').encode())
status = f'''## Edition115 - published and fetched back

2026-11-10, America/Los_Angeles. Release {release}; annotated readme-115. Main, edition115 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical source files, {audit['frozenHashes']} frozen hashes across 115 editions and {audit['original115EvidenceFiles']} original evidence entries. Durable Java chat constructs separate levels, performs actual playtests, permits at most two observed-gap repairs, preserves interrupted trials and resumes in a fresh session. Known-sprite PNG reconstruction feeds the same constructor and outcome gate. 79 relevant Java methods, 165 desktop checks, four actual UI requests, abrupt recovery and desktop/mobile guide pass. Simple control succeeds on repaired seed0; existing AI fails there and succeeds on seed3. Preserve that counterexample. Original image-matching failures and both earlier UI attempts remain; final capture waits for an actual decoded frame after restart. Zero model calls, no general image-recognition or model-training claim. No procedure-bank admission; 32 active slots unchanged. Next: broaden image/layout evidence, improve the measured controller failure, then explicit procedure retirement/reactivation. Preserve private runtimes 33224/37676, all original projects and frozen editions 001-115. No delegation; heavy checks serial. See docs/PUBLICATION_115.json. Remote CI is reported separately.

'''
for name in ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md']:
    p = r/name
    p.write_bytes((status+p.read_text(encoding='utf-8-sig')).encode('utf8'))
p = r/'README.md'
s = p.read_text(encoding='utf8')
first, rest = s.split('\n', 1)
link = '[Published and fetched verification receipt](docs/PUBLICATION_115.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-115)'
p.write_bytes((first+'\n\n'+link+'\n'+rest).encode('utf8'))
for name in ['push115.log', 'publication-audit115.json']:
    shutil.copy2(r/'build'/name, r/'docs/publication115'/name)
files = ['AGENTS.md', 'README-CONTRIBUTORS.md', 'README.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md', 'docs/PUBLICATION_115.json', 'docs/publication115']
run('git', 'add', *files)
run('git', 'add', '-f', 'docs/publication115/push115.log')
run('git', 'diff', '--cached', '--check', '--', '.', ':!docs/publication115/*.log')
run('git', '-c', 'gc.auto=0', 'commit', '-m', 'Record fetched edition 115 publication and complete source/evidence verification')
assert not git('status', '--porcelain')
head = git('rev-parse', 'HEAD')
changed = git('diff', '--name-only', release, head).splitlines()
assert all(n in files or n.startswith('docs/publication115/') for n in changed)
run('git', '-c', 'pack.threads=4', 'push', '--atomic', 'origin', 'HEAD:refs/heads/main', 'HEAD:refs/heads/edition115')
run('git', '-c', 'gc.auto=0', 'fetch', 'origin', 'refs/heads/main:refs/verify/edition115-final-main', 'refs/heads/edition115:refs/verify/edition115-final-branch', 'refs/tags/readme-115:refs/verify/edition115-final-tag')
assert head == git('rev-parse', 'refs/verify/edition115-final-main') == git('rev-parse', 'refs/verify/edition115-final-branch')
assert receipt['tagObject'] == git('rev-parse', 'refs/verify/edition115-final-tag')
assert release == git('rev-parse', 'refs/verify/edition115-final-tag^{commit}')
for name in changed:
    assert subprocess.check_output(['git', 'show', head+':'+name]) == subprocess.check_output(['git', 'show', 'refs/verify/edition115-final-main:'+name]), name
assert not git('status', '--porcelain')
result = {'passed': True, 'finalMain': head, 'release': release, 'tag': 'readme-115', 'metadataFilesCompared': len(changed), 'scope': 'Every changed metadata blob compared with freshly fetched main; production sources and frozen evidence unchanged from the complete fetched release audit.'}
(r/'build/final-publication115.json').write_bytes((json.dumps(result, indent=2)+'\n').encode())
print(json.dumps(result))
