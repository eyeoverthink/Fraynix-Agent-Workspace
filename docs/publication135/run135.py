from pathlib import Path
import json,hashlib,subprocess,time
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest();lock=json.loads((r/'docs/OBSERVATION_135_LOCK.json').read_text(encoding='utf8'));sources=json.loads((r/'docs/publication135/source135.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
folder=r/'build/association135-comparison';folder.mkdir(exist_ok=True)
java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe';cp='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;S:/Codex-Work/fraynix-workspace-edition134/workspace-engine/build/install/workspace-engine/lib/*'
for source in sources['records']:
 for key in ['pack','labels']:assert sha((r/source[key]).read_bytes())==source[key+'Sha256']
 for policy in lock['methods']:
  key=source['case']+'-'+policy;out=folder/(key+'.json');receipt=folder/(key+'.execution.json');assert not out.exists() and not receipt.exists(),'Preserve attempts'
  start=time.monotonic()
  with (folder/(key+'.log')).open('wb') as log:p=subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.AssociationStudy134',source['pack'],source['labels'],str(out),policy],stdout=log,stderr=subprocess.STDOUT)
  result={'case':source['case'],'policy':policy,'exit':p.returncode,'wallSeconds':time.monotonic()-start,'lockSha256':sources['algorithmLockSha256']};receipt.write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
  if p.returncode:raise SystemExit(p.returncode)
  print(json.dumps(json.loads(out.read_text(encoding='utf8'))['audit']['summary']),flush=True)
