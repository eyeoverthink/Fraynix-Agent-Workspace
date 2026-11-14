from pathlib import Path
import json,hashlib,subprocess,time
r=Path.cwd();previous=Path('J:/Codex-Work/fraynix-workspace-edition140');folder=r/'build/recovery141-development';folder.mkdir(exist_ok=False);sha=lambda b:hashlib.sha256(b).hexdigest()
sources=json.loads((r/'docs/publication140/source140.json').read_text(encoding='utf8'));java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe';cp='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;J:/Codex-Work/fraynix-workspace-edition140/workspace-engine/build/install/workspace-engine/lib/*';rows=[]
def overlap(a,b):
 i=max(0,min(a[2],b[2])-max(a[0],b[0]))*max(0,min(a[3],b[3])-max(a[1],b[1]));return i/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-i)
for spec in sources['records']:
 pack=previous/spec['pack'];labels=r/spec['labels'];assert sha(pack.read_bytes())==spec['packSha256'];assert sha(labels.read_bytes())==spec['labelsSha256'];label=json.loads(labels.read_text(encoding='utf8'))
 methods=['foreground-guard-139','recovery-guard-141']+(['recovery-monitor-141'] if spec==sources['records'][0] else [])
 baseline=None
 for policy in methods:
  name=spec['case']+'-'+policy;out=folder/(name+'.json');start=time.monotonic()
  with (folder/(name+'.log')).open('wb') as log:p=subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.AssociationStudy134',str(pack),str(labels),str(out),policy],stdout=log,stderr=subprocess.STDOUT)
  row={'case':spec['case'],'policy':policy,'exit':p.returncode,'seconds':time.monotonic()-start};rows.append(row)
  if p.returncode:raise SystemExit(p.returncode)
  data=json.loads(out.read_text(encoding='utf8'));row['summary']=data['audit']['summary'];row['processingMs']=data['processingMs']
  if policy=='foreground-guard-139':baseline=data
  else:
   scores={'correct':0,'incorrect':0,'unknown':0};preserved=0
   for x,y,l in zip(baseline['trace'],data['trace'],label['rows']):
    t=y['objects'][0]['tracker'];m=t.pop('recovery141');b=x['objects'][0]['tracker']
    if b['box'] is not None:assert t==b;preserved+=1
    if policy=='recovery-monitor-141':assert x==y
    if m['recovery']['accepted']:
     v=l['visibility'];k='correct' if v in ['visible','partial'] and overlap(t['box'],l['targetBox'])>=.5 else 'incorrect' if v in ['visible','partial','occluded','out-of-view'] else 'unknown';scores[k]+=1
   row.update(additional=scores,baselineAcceptedPreserved=preserved)
  print(json.dumps(row),flush=True);(folder/'results.json').write_text(json.dumps({'scope':'Previously exposed140 recordings; development, not fresh evaluation','rows':rows},indent=2),encoding='utf8')
