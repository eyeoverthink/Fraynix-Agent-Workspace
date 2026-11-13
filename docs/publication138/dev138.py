from pathlib import Path
import json,hashlib,subprocess,time
r=Path.cwd();folder=r/'build/drift138-development';folder.mkdir(exist_ok=False);sha=lambda b:hashlib.sha256(b).hexdigest()
sources=json.loads((r/'docs/publication137/source137.json').read_text(encoding='utf8'))
java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe'
cp='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;D:/Codex-Work/fraynix-workspace-edition137/workspace-engine/build/install/workspace-engine/lib/*'
rows=[]
for spec in sources['records']:
 if spec['case'] not in ['tiger2-155','box-535']:continue
 for key in ['pack','labels']:assert sha(Path(spec[key]).read_bytes())==spec[key+'Sha256']
 for policy in ['legacy-flow','monitor-only-138','drift-guard-138']:
  name=spec['case']+'-'+policy;out=folder/(name+'.json');start=time.monotonic()
  with (folder/(name+'.log')).open('wb') as log:p=subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.AssociationStudy134',spec['pack'],spec['labels'],str(out),policy],stdout=log,stderr=subprocess.STDOUT)
  result={'case':spec['case'],'policy':policy,'exit':p.returncode,'seconds':time.monotonic()-start};rows.append(result)
  if p.returncode:raise SystemExit(p.returncode)
  data=json.loads(out.read_text(encoding='utf8'));result['summary']=data['audit']['summary'];print(json.dumps(result),flush=True)
  (folder/'results.json').write_text(json.dumps({'scope':'Previously exposed137 windows; development only, not fresh evaluation','rows':rows},indent=2),encoding='utf8')
 # The monitor's extra evidence must not alter a single incumbent measurement or filter result.
 baseline=json.loads((folder/(spec['case']+'-legacy-flow.json')).read_text());monitor=json.loads((folder/(spec['case']+'-monitor-only-138.json')).read_text())
 for a,b in zip(baseline['trace'],monitor['trace']):
  b['objects'][0]['tracker'].pop('drift138');assert a==b
