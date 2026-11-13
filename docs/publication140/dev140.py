from pathlib import Path
import json,hashlib,subprocess,time
r=Path.cwd();previous=Path('J:/Codex-Work/fraynix-workspace-edition139');folder=r/'build/corroborated140-development';folder.mkdir(exist_ok=False);sha=lambda b:hashlib.sha256(b).hexdigest()
sources=json.loads((r/'docs/publication139/source139.json').read_text(encoding='utf8'));java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe';cp='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;J:/Codex-Work/fraynix-workspace-edition139/workspace-engine/build/install/workspace-engine/lib/*';rows=[]
for spec in sources['records']:
 pack=previous/spec['pack'];labels=r/spec['labels'];assert sha(pack.read_bytes())==spec['packSha256'];assert sha(labels.read_bytes())==spec['labelsSha256']
 methods=(['legacy-flow','foreground-guard-139'] if spec['case'] in ['bike1-1','ballet-100'] else [])+['corroborated-guard-140']+(['monitor-only-140'] if spec['case']=='bike1-1' else [])
 for policy in methods:
  name=spec['case']+'-'+policy;out=folder/(name+'.json');start=time.monotonic()
  with (folder/(name+'.log')).open('wb') as log:p=subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.AssociationStudy134',str(pack),str(labels),str(out),policy],stdout=log,stderr=subprocess.STDOUT)
  row={'case':spec['case'],'policy':policy,'exit':p.returncode,'seconds':time.monotonic()-start};rows.append(row)
  if p.returncode:raise SystemExit(p.returncode)
  data=json.loads(out.read_text(encoding='utf8'));row['summary']=data['audit']['summary'];row['processingMs']=data['processingMs'];print(json.dumps(row),flush=True)
  (folder/'results.json').write_text(json.dumps({'scope':'Previously exposed139 windows; development only, not fresh evaluation','rows':rows},indent=2),encoding='utf8')
 if 'monitor-only-140' in methods:
  a=json.loads((folder/(spec['case']+'-legacy-flow.json')).read_text(encoding='utf8'));b=json.loads((folder/(spec['case']+'-monitor-only-140.json')).read_text(encoding='utf8'))
  for x,y in zip(a['trace'],b['trace']):y['objects'][0]['tracker'].pop('drift140');assert x==y
