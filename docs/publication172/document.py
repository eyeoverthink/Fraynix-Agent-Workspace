"""Freeze the browser-to-world edition using the actual retained desktop study."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess

root = Path.cwd()
out = root / 'docs/readmes/172'
out.mkdir(exist_ok=True)
publication = root / 'docs/publication172'
app_path = root / 'build/ramp172-verified'

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
tests = read('build/validation172-final/summary.json')
upload = read('build/evidence172-upload.json')
assert app['passed'] and tests['passed'] and upload['download']['passed'] and app['archived']['passed']
assert 'RampMissionTest' in tests['classes']
assert app['one']['parent']['resumes'] == 2
assert 'visible browser table differs' in app['forged']['reason']
assert 'revision changed' in app['stale']['reason']
model_calls = sum(app[key]['parent']['timing052']['modelCalls'] for key in ('one','two'))
assert model_calls == 0

validation = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
              f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed. "
              "The live desktop study ran in a fresh project with no models enabled and the browser in offline mode.")
rows = []
runs = []
verification = []
for key in ('one','two'):
    mission = app[key]['verified']
    source = mission['source']
    v = source['version']
    value = source['values']
    audit = mission['audit']
    assert mission['canvasReceipt']['passed'] and len(mission['canvasReceipt']['checks']) == 9
    assert audit['maxPositionErrorM'] == 0 and audit['maxVelocityErrorMps'] == 0
    rows.append(f"| {v} | {value['angleDeg']:.0f}° | {value['friction']:.2f} | {audit['seconds']:.2f} "
                f"| {audit['distanceM']:.3f} | {audit['sampleCount']} |")
    original = app[key]['evidence']['original']['samples']
    replay = app[key]['evidence']['replay']['samples']
    runs.append({'id':str(v),'title':f'Revision {v}','duration':audit['seconds'],
          'description':f"{value['angleDeg']:.0f}° slope · friction {value['friction']:.2f} · "
                        f"{value['massKg']:g} kg · gravity {value['gravityMps2']:g} m/s²",
          'points':[{'seconds':a['timeS'],'original':a['distanceM'],'replay':b['distanceM'],'contacts':a['contacts']}
                    for a,b in zip(original,replay,strict=True)]})
    verification.append({'source':source,'audit':audit,'report':mission['report'],'canvasChecks':9,'stages':mission['stages']})
a,b=[entry['audit'] for entry in verification]
summary=(f"The block travelled {a['distanceM']:.3f} m and {b['distanceM']:.3f} m under the two browser input revisions. "
          "The report checks passed in Chromium. Both missions completed with zero model calls.")
results=("| Input revision | Slope | Friction | Time (s) | Downhill travel (m) | Matching replay samples |\n"
         "| --- | ---: | ---: | ---: | ---: | ---: |\n" + '\n'.join(rows) + "\n\n"
         + f"Both fresh replays matched all {a['sampleCount']+b['sampleCount']} retained samples. "
           "The position and velocity differences were zero at the reported precision. Each artifact passed nine live canvas checks.")
markdown = (publication / 'README-template.md').read_text(encoding='utf8')
(root / 'README-172.md').write_text(markdown.replace('@@VALIDATION@@', validation).replace('@@RESULTS@@', results), encoding='utf8')
(root / 'README.md').write_text(
    '# Fraynix Agent Workspace\n\n'
    '[Edition 172 — local browser to verified world experiment](README-172.md) · '
    '[Previous edition](README-171.md) · [Interactive guide](docs/readmes/172/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + summary + '\n', encoding='utf8')
index = root / 'docs/readmes/INDEX.md'
old = index.read_text(encoding='utf8')
if 'README-172.md' not in old:
    index.write_text(old.replace('\n','\n\n[Edition 172 — local browser to verified world experiment](../../README-172.md) · '
                                 '[Guide](172/index.html)\n',1), encoding='utf8')
note = ("## Edition 172 — local browser, native physics and verified creation\n\n2026-11-18. " + summary + " "
        + validation + " The live desktop verified two abrupt stops, changed input and refusal of stale or tampered page evidence. "
        "Completed work can reopen its original report from History. The scheduler now snapshots each journal entry to "
        "prevent later native observations changing an earlier event. See README-172.md. Next, compose registered offline "
        "steps through the same contracts and qualify a second source format.\n\n")
for name in ('AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md', 'docs/ARCHITECTURE_RENEWAL_PLAN.md', 'docs/INTEGRATED_AGENT_ROADMAP.md'):
    prepend(name, note)
note_path = 'docs/obsidian/180-browser-world-experiment.md'
prepend('docs/obsidian/00-home.md', '[Edition 172 — browser-to-world experiment](180-browser-world-experiment.md)\n\n')
(root / note_path).write_text(
    '# Local browser-to-world experiment\n\n' + summary + '\n\n'
    '[Edition 172](../../README-172.md) · [Offline guide](../readmes/172/index.html) · '
    '[Outcomes](../readmes/172/verification.json) · [Next acceptance gates](../publication172/plan.md)\n', encoding='utf8')

modules = [
    {'id':'browser','title':'Local browser observation','source':'workspace-desktop/ramp-mission.cjs',
     'description':'Reads the actual local page in Chromium and returns its visible typed rows.'},
    {'id':'world','title':'Java experiment and replays','source':'workspace-engine/src/main/java/fraymus/workspace/RampMission.java',
     'description':'Pins the source, keeps stage checkpoints and replays the experiment in a fresh Bullet world.'},
    {'id':'artifact','title':'Report and canvas checks','source':'workspace-engine/src/main/java/fraymus/workspace/RampMissionReport.java',
     'description':'Builds a chart and evidence downloads and binds the live canvas checks to Java measurements.'}
]
for module in modules:
    module['note'] = note_path
map_paths = ['README-172.md', *(module['source'] for module in modules),
             'docs/readmes/172/verification.json', 'docs/publication172/plan.md']
nodes = [{'id':str(i),'type':'file','file':name,'x':550*(i%3),'y':430*(i//3),'width':500,'height':390}
         for i,name in enumerate(map_paths)]
edges = [{'id':f'edge{i}','fromNode':str(i),'fromSide':'right','toNode':str(i+1),'toSide':'left'} for i in range(5)]
write(root/'docs/obsidian/180-browser-world-experiment.canvas', {'nodes':nodes,'edges':edges})
shots = []
captions = {'ramp-input.png':'Read the local input page','ramp-checkpoint.png':'Keep the browser checkpoint',
            'ramp-verified.png':'Open the verified report','ramp-changed-input.png':'Run a revised experiment',
            'ramp-reopened.png':'Reopen the original report after restart'}
for capture in app['captures']:
    name = capture['name']
    raw = app_path / name
    assert sha(raw.read_bytes()) == capture['sha256']
    shutil.copy2(raw, out / name)
    shots.append({'file':name, 'path':'docs/readmes/172/'+name,'title':captions[name]})
shots.sort(key=lambda row: row['file']!='ramp-verified.png')
system = {'summary':summary,'runs':runs,'screenshots':shots,'modules':modules,
          'evidence':[{'path':'docs/readmes/172/verification.json'}]}
write(out/'system-map.json', system)
write(out/'verification.json', {'passed':True,'source':app['source'],'tests':tests,'modelCalls':model_calls,
      'experiments':verification,'processStops':2,'archiveVerified':app['archived']['passed'],
      'domRefusal':app['forged']['reason'],'revisionRefusal':app['stale']['reason'],'ciFirst':read('build/ci172-first.json')})
for source, target in [('build/evidence172-manifest.json','original-evidence.json'),
                       ('build/evidence172-upload.json','evidence-publication.json')]:
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

changed = subprocess.check_output(['git','diff','--name-only','57972d3eb',app['source']],text=True).splitlines()
engine = [name for name in changed if name.startswith(('src/main/', 'assets/')) or name.endswith(('BulletRamp.java','RampDefinition.java'))]
assert not engine, engine
locked = [row(name) for name in changed]
lock_name = 'docs/LOCAL_RAMP_172_LOCK.json'
write(root/lock_name, {'source':app['source'],'sourceHashes':{item['path']:item['sha256'] for item in locked},
      'binaryPaths':[item['path'] for item in locked if item['hashKind']=='binary'], 'physicsImplementationChanged':False,
      'browserToReportProof':True,'modelCalls':model_calls})

paths = {item['path'] for item in read('docs/readmes/171/file-inventory.json')}
paths.update(changed)
paths.update(file.relative_to(root).as_posix() for file in publication.iterdir() if file.is_file())
paths.update([lock_name,'workspace-engine/src/test/java/fraymus/workspace/RampMissionTest.java',
              'docs/obsidian/180-browser-world-experiment.canvas',note_path])
inventory = [row(name) for name in sorted(paths)]
write(out/'file-inventory.json', inventory)
html = (publication/'guide-template.html').read_text(encoding='utf8')
for key,value in [('SYSTEM',system),('INVENTORY',inventory)]:
    html = html.replace('@@'+key+'@@',json.dumps(value).replace('</','<\\/'))
(out/'index.html').write_text(html, encoding='utf8')
shutil.copy2(publication/'guide.js', out/'guide.js')
print(json.dumps({'passed':True,'inventory':len(inventory),'screenshots':len(shots),'replayedSamples':a['sampleCount']+b['sampleCount']}))
