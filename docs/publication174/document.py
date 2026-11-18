"""Generate edition 174 from the retained paired experiments and real model proposals."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess

root=Path.cwd()
out=root/'docs/readmes/174'
out.mkdir(exist_ok=True)
publication=root/'docs/publication174'
app_path=root/'build/ramp174-release'

def read(path):
    return json.loads((root/path).read_bytes())

def write(path,value):
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf8')

def prepend(name,text):
    path=root/name
    existing=path.read_text(encoding='utf-8-sig')
    if not existing.startswith(text):
        path.write_text(text+existing,encoding='utf8')

def sha(data):
    return hashlib.sha256(data).hexdigest()

app=read(app_path/'result.json')
tests=read('build/validation174-final/summary.json')
legacy=read('build/ramp174-legacy.json')
planned_resume=read('build/ramp174-planned-resume.json')
first_model=read('build/ramp174-initial-model.json')
original=read('build/ramp174-original-gpt-routing.json')
upload=read('build/evidence174-upload.json')
assert app['passed'] and tests['passed'] and legacy['passed'] and planned_resume['passed'] and upload['download']['passed']
assert first_model['wrongChoiceBlocked']
assert planned_resume['modelCallsBefore']==planned_resume['modelCallsAfter']==1
assert app['beforeStop']['stages']==app['recovered']['mission']['stages'][:4]
assert len(app['cases'])==6 and all(row['selected']==row['wanted'] for row in app['cases'])
assert len(original['cases'])==6 and sum(row['offered']==row['wanted'] for row in original['cases'])==5
assert original['wrongChoiceBlocked']
model_routes=sum(row['by']=='model' for row in app['cases'])
assert model_routes==5 and app['cases'][-1]['by']=='java' and app['cases'][-1]['task']['timing052']['modelCalls']==0
assert all(next(event for event in row['task']['history'] if event['kind']=='plan')['detail']['compute']['cpuOnly']
           for row in app['cases'] if row['by']=='model')
labels={'local-ramp-experiment-v1':'First setup','local-ramp-comparison-v1':'Compare both','clarify':'Clarification'}
routes=[]
for i,row in enumerate(app['cases'],1):
    routes.append({'id':f'route-{i}','title':f"Request {i} · {labels[row['wanted']]}",
        'request':row['text'],'expected':labels[row['wanted']],'selected':labels[row['selected']],'by':row['by'],
        'elapsedMs':row['elapsedMs'],'outcome':'no physics started' if row.get('refusedBeforePhysics') else 'verified result',
        'reason':row['task']['reason'],'modelCalls':row['task']['timing052']['modelCalls']})
assert sum(row['outcome']=='no physics started' for row in routes)==2
clock=app['reversed']['mission']['audit']
a=clock['setups']['first']['distanceM']
b=clock['setups']['second']['distanceM']
samples=clock['samplesPerSetup']
model=app['model']
validation=(f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
            f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed. "
            "The checks cover swapped native traces, mismatched duration, missing input, route admission and the previous archived report.")
summary=(f"{model} selected both procedures and asked for clarification on five fresh requests. Java asked "
         f"for a missing setup on the sixth without calling the model. The paired report compared {a:.3f} m "
         f"and {b:.3f} m on the same clock, with {samples} matched replay samples per setup. "
         "The direct route finished with zero model calls.")
results=("| Expected result | Correct choices | Verified completion or clarification |\n"
         "| --- | ---: | --- |\n"
         "| First setup | 2/2 | Two verified single-setup artifacts |\n"
         "| Compare both | 2/2 | Two verified paired artifacts |\n"
         "| Clarification | 2/2 | The model asked about the undecided request. Java asked for the missing setup. |\n\n"
         +f"{summary} Reversing the CSV row order reversed the sign of the measured difference. Both original native "
          "sessions and both replays were retained after an abrupt Java stop before canvas verification. "
          "A separate model-selected comparison resumed with model discovery disabled and made no additional model call. "
         +f"On the same paired source, the direct comparison took {app['reversed']['elapsedMs']/1000:.2f} s. The two "
          "model-routed comparisons took "
         +" and ".join(f"{row['elapsedMs']/1000:.2f} s" for row in app['cases'] if row['wanted']=='local-ramp-comparison-v1')
         +f". Each used one model call. Java detected the missing setup in {routes[-1]['elapsedMs']/1000:.2f} s with zero model calls. "
          "These are complete task timings with CPU-only planning on this machine. The initial GPT-OSS run got five of six choices right. "
          "It asked for clarification on the ambiguous request, then proposed comparing a single-row source. Java blocked it "
          "before simulation. The first Llama 3.2 trial stopped after its third request, when it chose a single run for a "
          "comparison. That mismatch was also blocked. The release checks source availability before a model call when the "
          "comparison is explicit. Both initial failures remain in the evidence bundle.")
markdown=(publication/'README-template.md').read_text(encoding='utf8')
(root/'README-174.md').write_text(markdown.replace('@@RESULTS@@',results).replace('@@TESTS@@',validation),encoding='utf8')
(root/'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 174 — two verified local ramp procedures](README-174.md) · '
    '[Previous edition](README-173.md) · [Offline guide](docs/readmes/174/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n'+summary+'\n',encoding='utf8')
index=root/'docs/readmes/INDEX.md'
old=index.read_text(encoding='utf8')
if 'README-174.md' not in old:
    index.write_text(old.replace('\n','\n\n[Edition 174 — verified local procedure routing](../../README-174.md) · '
                                '[Guide](174/index.html)\n',1),encoding='utf8')
note=("## Edition 174 — verified local procedure routing\n\n2026-11-18. "+summary+" "+validation
      +" A paired CSV comparison uses four distinct Bullet sessions. Recovery kept both native outcomes and the "
       "verified report inputs. Original model mistakes and incomplete-source refusals are retained. "
       "Edition 173 evidence reopened without a new run. Read README-174.md and docs/publication174/plan.md. "
       "Next, scale the held-out test across physics, data and creation.\n\n")
for filename in ('AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
                 'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md'):
    prepend(filename,note)
note_path='docs/obsidian/182-local-ramp-routing.md'
prepend('docs/obsidian/00-home.md','[Edition 174 — verified local procedure routing](182-local-ramp-routing.md)\n\n')
(root/note_path).write_text('# Verified local procedure routing\n\n'+summary+'\n\n'
    '[Edition 174](../../README-174.md) · [Offline guide](../readmes/174/index.html) · '
    '[Outcomes](../readmes/174/verification.json) · [Implementation plan](../publication174/plan.md)\n',encoding='utf8')

modules=[
 {'id':'catalog','title':'Local procedure registry','source':'workspace-engine/src/main/java/fraymus/workspace/RampProcedure.java',
  'description':'Offers the two registered outcomes and checks the local model choice against the request and source.'},
 {'id':'comparison','title':'Paired simulation and tolerance checks','source':'workspace-engine/src/main/java/fraymus/workspace/RampComparison.java',
  'description':'Verifies both independent replays on a common clock and renders the measured comparison.'},
 {'id':'runtime','title':'Durable Java task','source':'workspace-engine/src/main/java/fraymus/workspace/RampMission.java',
  'description':'Binds the source, resumes from confirmed stage receipts and independently recomputes reports.'}
]
for module in modules:
    module['note']=note_path
map_paths=['README-174.md',*(m['source'] for m in modules),'docs/readmes/174/verification.json','docs/publication174/plan.md']
nodes=[{'id':str(i),'type':'file','file':n,'x':550*(i%3),'y':430*(i//3),'width':500,'height':390}
       for i,n in enumerate(map_paths)]
edges=[{'id':f'edge{i}','fromNode':str(i),'fromSide':'right','toNode':str(i+1),'toSide':'left'} for i in range(5)]
write(root/'docs/obsidian/182-local-ramp-routing.canvas',{'nodes':nodes,'edges':edges})
shots=[]
captions={'paired-ramp-input.png':'Import the paired configurations','paired-ramp-checkpoint.png':'Retain the comparison checkpoint',
          'paired-ramp-verified.png':'Review the verified paired report','paired-ramp-missing-input.png':'Ask for the missing setup',
          'paired-ramp-model-routing.png':'Inspect the model-selected comparison'}
for capture in app['captures']:
    name=capture['name']
    raw=app_path/name
    assert sha(raw.read_bytes())==capture['sha256']
    shutil.copy2(raw,out/name)
    shots.append({'file':name,'path':'docs/readmes/174/'+name,'title':captions[name]})
shots.sort(key=lambda row:row['file']!='paired-ramp-verified.png')

runs=[]
metrics=[]
for key,title in [('direct','First setup travels farther'),('reversed','Reversed order')]:
    run=app[key]
    mission=run['mission']
    audit=mission['audit']
    first=run['evidence']['original']['first']['samples']
    second=run['evidence']['original']['second']['samples']
    runs.append({'id':key,'title':title,'duration':audit['seconds'],
                 'clock':f"{run['elapsedMs']/1000:.2f} s to a verified report · zero model calls",
                 'description':f"First setup {mission['source']['values']['angleDeg']:g}° · "
                               f"second setup {mission['source']['secondValues']['angleDeg']:g}°",
                 'points':[{'seconds':l['timeS'],'original':l['distanceM'],'replay':r['distanceM']}
                           for l,r in zip(first,second,strict=True)]})
    metrics.append({'route':key,'source':mission['source'],'audit':audit,'report':mission['report'],
                    'elapsedMs':run['elapsedMs'],'canvasChecks':len(mission['canvasReceipt']['checks'])})
system={'summary':summary,'runs':runs,'routes':routes,'modules':modules,'screenshots':shots,
        'evidence':[{'path':'docs/readmes/174/verification.json'}]}
write(out/'system-map.json',system)
write(out/'verification.json',{'passed':True,'source':app['source'],'tests':tests,'registry':app['registry'],
      'correctRoutes':len(routes),'correctModelRoutes':model_routes,'model':model,'routes':routes,'experiments':metrics,'recoveredOriginalStages':True,
      'priorArchive':legacy,'plannedResumeWithModelsDisabled':planned_resume,'initialLlamaWrongChoiceBlocked':first_model['wrongChoiceBlocked'],
      'initialGptCorrectRoutes':5,'initialGptWrongChoiceBlocked':original['wrongChoiceBlocked'],'ciFirst':read('build/ci174-first.json')})
for path,name in [('build/evidence174-manifest.json','original-evidence.json'),
                  ('build/evidence174-upload.json','evidence-publication.json')]:
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

changed=subprocess.check_output(['git','diff','--name-only','d3b207c3e',app['source']],text=True).splitlines()
engine=[name for name in changed if name.startswith(('src/main/','assets/')) or name.endswith(('BulletRamp.java','RampDefinition.java'))]
assert not engine,engine
locked=[row(name) for name in changed]
lock_name='docs/PAIRED_RAMP_174_LOCK.json'
write(root/lock_name,{'source':app['source'],'sourceHashes':{item['path']:item['sha256'] for item in locked},
      'binaryPaths':[item['path'] for item in locked if item['hashKind']=='binary'],
      'physicsImplementationChanged':False,'procedureHashes':{p['id']:p['sha256'] for p in app['registry']['procedures']}})
paths={item['path'] for item in read('docs/readmes/173/file-inventory.json')}
paths.update(changed)
paths.update(file.relative_to(root).as_posix() for file in publication.iterdir() if file.is_file())
paths.update([lock_name,note_path,'docs/obsidian/182-local-ramp-routing.canvas'])
inventory=[row(name) for name in sorted(paths)]
write(out/'file-inventory.json',inventory)
html=(publication/'guide-template.html').read_text(encoding='utf8')
for key,value in [('SYSTEM',system),('INVENTORY',inventory)]:
    html=html.replace('@@'+key+'@@',json.dumps(value).replace('</','<\\/'))
(out/'index.html').write_text(html,encoding='utf8')
shutil.copy2(publication/'guide.js',out/'guide.js')
print(json.dumps({'passed':True,'inventory':len(inventory),'screenshots':len(shots),'correctRoutes':len(routes)}))
