"""Generate the edition, snapshots and project notes from the verified desktop results."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess

root = Path.cwd()
out = root/'docs/readmes/173'
out.mkdir(exist_ok=True)
publication = root/'docs/publication173'
app_path = root/'build/ramp173-complete'

def read(path):
    return json.loads((root/path).read_bytes())

def write(path, value):
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf8')

def prepend(name, text):
    path = root/name
    existing = path.read_text(encoding='utf-8-sig')
    if not existing.startswith(text):
        path.write_text(text+existing,encoding='utf8')

def sha(data):
    return hashlib.sha256(data).hexdigest()

app = read(app_path/'result.json')
tests = read('build/validation173-final/summary.json')
legacy = read('build/ramp173-legacy.json')
refusal = read('build/ramp173-model-refusal.json')
upload = read('build/evidence173-upload.json')
assert app['passed'] and tests['passed'] and legacy['passed'] and upload['download']['passed']
assert not refusal['executed'] and 'pauseAfterReading' in refusal['reason']
assert 'RampProcedureTest' in tests['classes']
catalog = app['catalog']
direct = app['direct']
planned = app['planned']
assert direct['mission']['source'] == planned['mission']['source']
assert direct['evidence']['original']['samples'] == planned['evidence']['original']['samples']
assert app['recovered']['mission']['stages'][:4] == app['beforeStop']['stages']
assert app['recovered']['parent']['resumes'] == 2
d = direct['mission']['audit']
p = planned['mission']['audit']
assert d['maxPositionErrorM'] == p['maxPositionErrorM'] == 0
assert d['maxVelocityErrorMps'] == p['maxVelocityErrorMps'] == 0
model = planned['parent']['model']
calls = [app[key]['parent']['timing052']['modelCalls'] for key in ('form','direct','recovered','planned')]
assert calls == [0,0,0,1]
validation=(f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
            f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed. "
            "The checks include changed CSV bytes, duplicate headers, formulas, out-of-order receipts and the original edition 172 archive.")
summary=(f"A form and an imported CSV used the same registered Java procedure. On the same CSV revision, direct "
         f"execution and {model} produced identical native samples and measured {d['distanceM']:.3f} m of travel. "
         "A crash before canvas verification preserved both physics runs and the saved report.")
results=("| Request | Model calls | Time to verified result | Replay samples | Canvas checks |\n"
         "| --- | ---: | ---: | ---: | ---: |\n"
         f"| Direct, same CSV | 0 | {direct['elapsedMs']/1000:.2f} s | {d['sampleCount']} | 10 |\n"
         f"| {model}, same CSV | 1 | {planned['elapsedMs']/1000:.2f} s | {p['sampleCount']} | 10 |\n\n"
         f"Both fresh replays matched all {d['sampleCount']} checkpoints at the sampled precision. "
         "The desktop also recovered a separate CSV mission with the original browser, experiment, replay and report receipts. "
         "The final canvas checks ran after restart. A copied edition 172 project reopened its archived report with the same original evidence. "
         "The timings are one pair of complete app runs on this machine, with no warm-up or repeated latency study. "
         "An earlier local-model attempt sent the pause as a string and Java refused it before execution. "
         "The released route derives that setting from the original user request and exposes only the procedure choice to the model.")
markdown=(publication/'README-template.md').read_text(encoding='utf8')
(root/'README-173.md').write_text(markdown.replace('@@RESULTS@@',results).replace('@@TESTS@@',validation),encoding='utf8')
(root/'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 173 — a registered local experiment with CSV input](README-173.md) · '
    '[Previous edition](README-172.md) · [Offline companion](docs/readmes/173/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n'+summary+'\n',encoding='utf8')
index=root/'docs/readmes/INDEX.md'
old=index.read_text(encoding='utf8')
if 'README-173.md' not in old:
    index.write_text(old.replace('\n','\n\n[Edition 173 — registered local experiment and CSV input](../../README-173.md) · '
                                '[Guide](173/index.html)\n',1),encoding='utf8')
note=("## Edition 173 — registered local procedure and CSV input\n\n2026-11-18. "+summary+" "+validation+
      " Java now verifies an ordered stage contract and retains the original CSV bytes. The optional local model "
      "can choose the registered procedure. Source and pause remain bound to the user request. The archived edition 172 "
      "report matched its original evidence. Read README-173.md and docs/publication173/plan.md. "
      "Next, add a second meaningful procedure choice and measure routing quality and cost across reserved requests.\n\n")
for filename in ('AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
                 'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md'):
    prepend(filename,note)
note_path='docs/obsidian/181-registered-local-experiment.md'
prepend('docs/obsidian/00-home.md','[Edition 173 — registered local experiment and CSV input](181-registered-local-experiment.md)\n\n')
(root/note_path).write_text('# Registered local experiment\n\n'+summary+'\n\n'
    '[Edition 173](../../README-173.md) · [Offline guide](../readmes/173/index.html) · '
    '[Outcomes](../readmes/173/verification.json) · [Implementation plan](../publication173/plan.md)\n',encoding='utf8')

modules=[
 {'id':'catalog','title':'Registered admission and stages','source':'workspace-engine/src/main/java/fraymus/workspace/RampProcedure.java',
  'description':'Declares the typed stage and source contracts and validates optional model selection.'},
 {'id':'input','title':'Form and CSV source','source':'workspace-engine/src/main/java/fraymus/workspace/RampInputPage.java',
  'description':'Validates decimal inputs and binds both the actual browser page and original CSV bytes.'},
 {'id':'runtime','title':'Durable Java execution','source':'workspace-engine/src/main/java/fraymus/workspace/RampMission.java',
  'description':'Runs both native worlds and reconstructs the five retained stage receipts.'}
]
for module in modules:
    module['note']=note_path
map_paths=['README-173.md',*(m['source'] for m in modules),'docs/readmes/173/verification.json','docs/publication173/plan.md']
nodes=[{'id':str(i),'type':'file','file':n,'x':550*(i%3),'y':430*(i//3),'width':500,'height':390} for i,n in enumerate(map_paths)]
edges=[{'id':f'edge{i}','fromNode':str(i),'fromSide':'right','toNode':str(i+1),'toSide':'left'} for i in range(5)]
write(root/'docs/obsidian/181-registered-local-experiment.canvas',{'nodes':nodes,'edges':edges})
shots=[]
captions={'ramp-csv-import.png':'Check the imported CSV and registered stages','ramp-csv-checkpoint.png':'Preserve the report checkpoint',
          'ramp-csv-verified.png':'Review the checked CSV report','ramp-planner-verified.png':'Verify the model-selected experiment'}
for capture in app['captures']:
    name=capture['name']
    raw=app_path/name
    assert sha(raw.read_bytes())==capture['sha256']
    shutil.copy2(raw,out/name)
    shots.append({'file':name,'path':'docs/readmes/173/'+name,'title':captions[name]})
shots.sort(key=lambda row:row['file']!='ramp-csv-verified.png')

runs=[]
metrics=[]
for key,title,num in [('form','Form input',0),('direct','CSV · direct',0),('planned','CSV · model selected',1)]:
    run=app[key]
    mission=run['mission']
    audit=mission['audit']
    v=mission['source']['values']
    a=run['evidence']['original']['samples']
    b=run['evidence']['replay']['samples']
    clock=f"{run['elapsedMs']/1000:.2f} s to verified report · {num} model call"+('' if num==1 else 's')
    runs.append({'id':key,'title':title,'duration':audit['seconds'],'clock':clock,
      'description':f"{v['angleDeg']:g}° slope · friction {v['friction']:g} · {v['massKg']:g} kg · gravity {v['gravityMps2']:g} m/s²",
      'points':[{'seconds':l['timeS'],'original':l['distanceM'],'replay':r['distanceM'],'contacts':l['contacts']}
                for l,r in zip(a,b,strict=True)]})
    metrics.append({'route':key,'source':mission['source'],'audit':audit,'report':mission['report'],
                    'elapsedMs':run['elapsedMs'],'modelCalls':num,'canvasChecks':len(mission['canvasReceipt']['checks'])})
system={'summary':summary,'runs':runs,'modules':modules,'screenshots':shots,
        'evidence':[{'path':'docs/readmes/173/verification.json'}]}
write(out/'system-map.json',system)
write(out/'verification.json',{'passed':True,'source':app['source'],'tests':tests,'catalog':catalog,
      'directModelCalls':calls[1],'plannedModelCalls':calls[3],'model':model,'nativeSamplesIdentical':True,
      'experiments':metrics,'recoveredOriginalStages':True,'priorArchive':legacy,
      'firstModelRefusal':refusal['reason'],'ciFirst':read('build/ci173-first.json')})
for path,name in [('build/evidence173-manifest.json','original-evidence.json'),
                  ('build/evidence173-upload.json','evidence-publication.json')]:
    shutil.copy2(root/path,out/name)

def row(name):
    raw=(root/name).read_bytes()
    kind='binary'
    if Path(name).suffix not in ('.png','.jpg','.wav','.gz','.bin','.zip'):
        try:
            raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode()
            kind='utf8-lf'
        except UnicodeDecodeError:
            pass
    return {'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind}

changed=subprocess.check_output(['git','diff','--name-only','19ab85caa',app['source']],text=True).splitlines()
engine=[name for name in changed if name.startswith(('src/main/','assets/')) or name.endswith(('BulletRamp.java','RampDefinition.java'))]
assert not engine,engine
locked=[row(name) for name in changed]
lock_name='docs/REGISTERED_RAMP_173_LOCK.json'
write(root/lock_name,{'source':app['source'],'sourceHashes':{item['path']:item['sha256'] for item in locked},
      'binaryPaths':[item['path'] for item in locked if item['hashKind']=='binary'],
      'physicsImplementationChanged':False,'contractSha256':catalog['sha256'],'identicalSameSourceSamples':True})
paths={item['path'] for item in read('docs/readmes/172/file-inventory.json')}
paths.update(changed)
paths.update(file.relative_to(root).as_posix() for file in publication.iterdir() if file.is_file())
paths.update([lock_name,note_path,'docs/obsidian/181-registered-local-experiment.canvas'])
inventory=[row(name) for name in sorted(paths)]
write(out/'file-inventory.json',inventory)
html=(publication/'guide-template.html').read_text(encoding='utf8')
for key,value in [('SYSTEM',system),('INVENTORY',inventory)]:
    html=html.replace('@@'+key+'@@',json.dumps(value).replace('</','<\\/'))
(out/'index.html').write_text(html,encoding='utf8')
shutil.copy2(publication/'guide.js',out/'guide.js')
print(json.dumps({'passed':True,'inventory':len(inventory),'screenshots':len(shots),'matchedSamples':d['sampleCount']}))
