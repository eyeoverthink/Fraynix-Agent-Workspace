"""Write the edition from the desktop, Java and publication evidence."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess

root = Path.cwd()
out = root / 'docs/readmes/171'
out.mkdir(exist_ok=True)
publication = root / 'docs/publication171'
app_path = root / 'build/game171-verified'

def read(path):
    return json.loads((root / path).read_bytes())

def write(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf8')

def prepend(name, text):
    path = root / name
    existing = path.read_text(encoding='utf-8-sig')
    if not existing.startswith(text):
        path.write_text(text + existing, encoding='utf8')

def sha(data):
    return hashlib.sha256(data).hexdigest()

app = read(app_path / 'result.json')
tests = read('build/validation171/summary.json')
native = read('workspace-engine/build/relay-playtest/result.json')
upload = read('build/evidence171-upload.json')
assert all(row['passed'] for row in (app, tests, native)) and upload['download']['passed']
assert native['changedReportRejected']
ridge = app['ridge']['checked']['audit']
switchyard = app['switchyard']['checked']['audit']
pairs = ridge['independentReplays'] + switchyard['independentReplays']
crossings = ridge['crossings'] + switchyard['crossings']
model_calls = sum(app[key]['parent']['timing052']['modelCalls'] for key in ('ridge', 'switchyard'))
assert pairs == 4 and crossings == 11 and model_calls == 0
validation = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
              f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed. "
              "The desktop integration used a new isolated test project.")
summary = (f"The desktop completed {pairs} primary trials and {pairs} fresh replays with {crossings} measured rail "
           "clearances across the four course and pace combinations. The two chat missions made zero model calls.")
results = (
    "| Course | Paces tested | Original rail clearances | Fresh replays | Minimum clearance |\n"
    "| --- | --- | ---: | ---: | ---: |\n"
    f"| Relay Ridge | Normal, steady, careful | {ridge['crossings']} | {ridge['independentReplays']} | {ridge['minimumClearance']:.3f} |\n"
    f"| Relay Switchyard | Careful | {switchyard['crossings']} | {switchyard['independentReplays']} | {switchyard['minimumClearance']:.3f} |\n\n"
    f"{summary} The Ridge test took {app['ridgeElapsedMs']/1000:.1f} seconds in this run, including an abrupt Java process stop, "
    "app restart and explicit resume. The two completed trials were preserved. The unfinished attempt was retained as interrupted "
    "and rerun in a fresh session. A later cold restart reopened the report. The app passed three rendered report checks, "
    "and the Java test refused a deliberately altered evidence file."
)
markdown = (publication / 'README-template.md').read_text(encoding='utf8')
(root / 'README-171.md').write_text(markdown.replace('@@VALIDATION@@', validation).replace('@@RESULTS@@', results), encoding='utf8')
(root / 'README.md').write_text(
    '# Fraynix Agent Workspace\n\n'
    '[Edition 171 — durable offline Relay playtest](README-171.md) · '
    '[Previous edition](README-170.md) · [Interactive guide](docs/readmes/171/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + summary + '\n', encoding='utf8')
index = root / 'docs/readmes/INDEX.md'
old = index.read_text(encoding='utf8')
if 'README-171.md' not in old:
    index.write_text(old.replace('\n','\n\n[Edition 171 — durable offline Relay playtest](../../README-171.md) · '
                                 '[Guide](171/index.html)\n',1), encoding='utf8')
note = ("## Edition 171 — durable offline Relay playtesting\n\n2026-11-18. " + summary + " "
        + validation + " Abrupt Java termination preserved both finished trials and reopened the saved report. "
        "Java binds original game assets and archives, checks genuine beacon contact and rail clearances, and reopens "
        "a separate runtime for each replay. Start here with README-171.md. Next, compose offline browser observations, "
        "a verified world experiment and a report in one durable mission. Keep the game as a first-class environment.\n\n")
for name in ('AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md', 'docs/ARCHITECTURE_RENEWAL_PLAN.md', 'docs/INTEGRATED_AGENT_ROADMAP.md',
             'docs/PLAYABLE_LEVEL_CREATION_PLAN.md'):
    prepend(name, note)
prepend('docs/obsidian/00-home.md', '[Edition 171 — durable offline Relay playtesting](179-offline-playtesting.md)\n\n')
(root / 'docs/obsidian/179-offline-playtesting.md').write_text(
    '# Offline playtesting\n\n' + summary + '\n\n'
    '[Edition 171](../../README-171.md) · [Offline guide](../readmes/171/index.html) · '
    '[Outcomes](../readmes/171/verification.json) · [Next acceptance gates](../publication171/plan.md)\n', encoding='utf8')

modules = [
    {'id':'authority','title':'Java mission authority','source':'workspace-engine/src/main/java/fraymus/workspace/Domain044.java',
     'description':'Reuses the existing Java durable chat queue and completion verifier.'},
    {'id':'runtime','title':'Isolated native trial','source':'workspace-engine/src/main/java/fraymus/workspace/RelayPlaytest.java',
     'description':'Owns native trials, saves progress and verifies the independent replay and report.'},
    {'id':'surface','title':'Desktop mission and canvas','source':'workspace-desktop/main.cjs',
     'description':'Renders the persisted mission and checks the saved report when it opens.'}
]
map_paths = ['README-171.md', modules[1]['source'], 'docs/readmes/171/verification.json', 'docs/publication171/plan.md']
nodes = [{'id':str(i),'type':'file','file':name,'x':550*i,'y':0,'width':500,'height':360} for i,name in enumerate(map_paths)]
edges = [{'id':f'edge{i}','fromNode':str(i),'fromSide':'right','toNode':str(i+1),'toSide':'left'} for i in range(3)]
write(root / 'docs/obsidian/179-offline-playtesting.canvas', {'nodes':nodes,'edges':edges})
shots = []
captions = {'start':'Prepare the chat mission in Game Lab','resume':'Resume after Java restarts',
            'completed':'Read the completed mission','report':'Open the checked canvas report'}
for capture in app['captures']:
    name = capture['name']
    raw = app_path / name
    assert sha(raw.read_bytes()) == capture['sha256']
    shutil.copy2(raw, out / name)
    shots.append({'file':name, 'path':'docs/readmes/171/'+name, 'title':captions[raw.stem.split('-')[-1]]})
shots.sort(key=lambda row: row['file'] != 'relay-mission-report.png')
for module in modules:
    module['note'] = 'docs/obsidian/179-offline-playtesting.md'
system = {'summary':summary, 'courses':[ridge,switchyard], 'screenshots':shots, 'modules':modules,
          'evidence':[{'path':'docs/readmes/171/verification.json'}]}
write(out / 'system-map.json', system)
write(out / 'verification.json', {'passed':True, 'source':app['source'], 'tests':tests, 'modelCalls':model_calls,
      'courseAudits':[ridge,switchyard], 'retainedPair':True, 'abruptInterruption':True,
      'ridgeElapsedMs':app['ridgeElapsedMs'], 'changedReportRejected':True,
      'browserOffline':app['network']['enabled'], 'ciFirst':read('build/ci171-first.json')})
for source, target in [('build/evidence171-manifest.json','original-evidence.json'),
                       ('build/evidence171-upload.json','evidence-publication.json')]:
    shutil.copy2(root / source, out / target)

def row(name):
    raw = (root / name).read_bytes()
    kind = 'binary'
    if Path(name).suffix not in ('.png','.jpg','.wav','.gz','.bin','.zip'):
        try:
            raw = raw.decode('utf-8-sig').replace('\r\n','\n').encode()
            kind = 'utf8-lf'
        except UnicodeDecodeError:
            pass
    return {'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind}

changed = subprocess.check_output(['git','diff','--name-only','320d34fba',app['source']],text=True).splitlines()
engine = [name for name in changed if name.startswith(('src/main/', 'assets/'))]
assert not engine, engine
locked = [row(name) for name in changed]
lock_name = 'docs/OFFLINE_PLAYTEST_171_LOCK.json'
write(root / lock_name, {'source':app['source'],'sourceHashes':{item['path']:item['sha256'] for item in locked},
      'binaryPaths':[item['path'] for item in locked if item['hashKind']=='binary'], 'gamePhysicsSourcesChanged':False,
      'pairedNativeVerification':pairs, 'modelCalls':model_calls})

paths = {item['path'] for item in read('docs/readmes/170/file-inventory.json')}
paths.update(changed)
paths.update(file.relative_to(root).as_posix() for file in publication.iterdir() if file.is_file())
paths.update([lock_name, 'docs/obsidian/179-offline-playtesting.canvas', 'docs/obsidian/179-offline-playtesting.md'])
rows = [row(name) for name in sorted(paths)]
write(out / 'file-inventory.json', rows)
html = (publication / 'guide-template.html').read_text(encoding='utf8')
for key, value in [('SYSTEM', system), ('INVENTORY', rows)]:
    html = html.replace('@@'+key+'@@',json.dumps(value).replace('</','<\\/'))
(out / 'index.html').write_text(html, encoding='utf8')
shutil.copy2(publication / 'guide.js', out / 'guide.js')
print(json.dumps({'passed':True,'sourceInventory':len(rows),'screenshots':len(shots),'nativeReplays':pairs}))
