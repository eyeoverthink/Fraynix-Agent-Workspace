from pathlib import Path
import json,hashlib,subprocess,time
r=Path.cwd();lock=json.loads((r/'docs/OBSERVATION_133_LOCK.json').read_text())
for n,h in lock['sourceHashes'].items():assert hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,n
prior=json.loads((r/'docs/readmes/132/app-study.json').read_text())
manifest=json.loads((r/'docs/readmes/editions.json').read_text());entry=next(x for x in manifest['editions'] if x['number']==132);asset=next(x for x in entry['snapshotFiles'] if x['path']=='docs/readmes/132/app-study.json');assert hashlib.sha256((r/asset['path']).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==asset['sha256']
media=Path('S:/Codex-Work/fraynix-workspace-edition131/build/footage131-held');folder=r/'build/association133-comparison';folder.mkdir(exist_ok=True)
java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe';cp='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;S:/Codex-Work/fraynix-workspace-edition132/workspace-engine/build/install/workspace-engine/lib/*'
for n in lock['frozenComparison']:
 pack=media/f'held-{n}.fobs.json';assert hashlib.sha256(pack.read_bytes()).hexdigest()==next(d['packSha256'] for d in prior['datasets'] if d['window']==n)
 for method in ['candidate','default-support']:
  out=folder/f'{method}-{n}.json';receipt=folder/f'{method}-{n}.execution.json';assert not out.exists() and not receipt.exists(),'Preserve original attempts'
  args=[java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.ObservationStudy133',str(pack),'docs/publication132/source-labels.json',str(out),str(n)]
  if method=='default-support':args+=['docs/readmes/132/app-study.json']
  start=time.monotonic()
  with (folder/f'{method}-{n}.log').open('wb') as log:p=subprocess.run(args,stdout=log,stderr=subprocess.STDOUT)
  result={'window':n,'method':method,'exit':p.returncode,'wallSeconds':time.monotonic()-start,'lockSha256':hashlib.sha256((r/'docs/OBSERVATION_133_LOCK.json').read_bytes()).hexdigest()};receipt.write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
  if p.returncode:raise SystemExit(p.returncode)
  print(json.loads(out.read_text())['metrics'],flush=True)
