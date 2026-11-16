"""Compare frozen scalar/optimized search on every new source without reading labels."""
from pathlib import Path
import json,hashlib,base64,subprocess
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest();lock=json.loads((r/'docs/OBSERVATION_153_LOCK.json').read_bytes())
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
subprocess.run(['node','scripts/Study-Build095.cjs'],check=True)
manifest=json.loads((r/'build/reserved153/manifest.json').read_bytes());rows=[]
for case in manifest['cases']:
 raw=Path(case['pack']).read_bytes();assert sha(raw)==case['packSha256'];pack=json.loads(raw);folder=r/'build/arithmetic153'/case['name'];folder.mkdir(parents=True,exist_ok=False);images=[]
 for i,frame in enumerate(pack['frames']):
  b=base64.b64decode(frame['image']);assert sha(b)==frame['imageSha256'];p=folder/f'{i}.png';p.write_bytes(b);images.append(str(p))
 input=folder/'input.json';input.write_text(json.dumps({'seed':pack['objects'][0]['box'],'memory':{'examples':[]},'images':images}),encoding='utf8');out=folder/'result.json'
 with (folder/'measurement.log').open('w',encoding='utf8') as log:
  subprocess.run(['java','-XX:ActiveProcessorCount=2','-Xmx768m','-cp','build/measure153-classes;workspace-engine/build/install/workspace-engine/lib/*','fraymus.workspace.RotationMeasure153',str(input),str(out)],stdout=log,stderr=subprocess.STDOUT,check=True)
 result=json.loads(out.read_bytes());assert result['passed'] and result['exactTraceComparison'];assert result['SCALAR']['trace']==result['PRECOMPUTED']['trace']
 rows.append({'case':case['name'],'sourcePackSha256':case['packSha256'],'result':out.relative_to(r).as_posix(),'resultSha256':sha(out.read_bytes()),'scalarMs':result['SCALAR']['ms'],'optimizedMs':result['PRECOMPUTED']['ms'],'rowsCompared':len(result['SCALAR']['trace'])});print(json.dumps(rows[-1]),flush=True)
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
(r/'build/generated153-arithmetic.json').write_text(json.dumps({'passed':True,'cases':rows,'methodLockSha256':sha((r/'docs/OBSERVATION_153_LOCK.json').read_bytes()),'scope':'Full traces exactly equal for all five new rendered sources. No evaluation labels supplied.'},indent=2)+'\n',encoding='utf8')
