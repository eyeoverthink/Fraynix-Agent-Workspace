"""Narrow pre-tracking metadata correction; preserve original rejected packs and hashes."""
from pathlib import Path
import json,hashlib,shutil
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest();p=r/'docs/publication150/source150.json';manifest=json.loads(p.read_bytes())
failure=json.loads((r/'build/reference150-webcam150-displaced-return-app/result.json').read_bytes())
assert not failure['passed'] and not failure['runs'] and 'Unique short object keys required' in failure['error']
corrections=[]
for spec in manifest['records']:
 if spec['group']!='webcam150-displaced-return':continue
 packpath=r/spec['pack'];raw=packpath.read_bytes();assert sha(raw)==spec['packSha256'];original=json.loads(raw)
 assert len(original['objects'])==1 and len(original['objects'][0]['label'])>80
 archive=packpath.with_name(packpath.stem+'-import-refused.json');assert not archive.exists();shutil.copy2(packpath,archive)
 changed=json.loads(raw);changed['objects'][0]['label']='Black target lighter'
 check=json.loads(json.dumps(changed));check['objects'][0]['label']=original['objects'][0]['label'];assert check==original
 corrected=json.dumps(changed).encode();packpath.write_bytes(corrected)
 corrections.append({'case':spec['case'],'originalPackSha256':sha(raw),'correctedPackSha256':sha(corrected),'originalPrivatePack':archive.relative_to(r).as_posix(),'previousLabel':original['objects'][0]['label'],'label':changed['objects'][0]['label'],'unchanged':'All source image bytes, timestamps, seed geometry, independent label files and source eligibility. Correction happened after import refusal, before any tracking.'})
 spec['packSha256']=sha(corrected)
assert len(corrections)==2
manifest['importLabelCorrection150']=corrections;p.write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
(r/'docs/publication150/import-label-correction150.json').write_bytes((json.dumps({'trackingBeforeCorrection':False,'corrections':corrections},indent=2)+'\n').encode())
print(json.dumps({'correctedLabels':len(corrections),'imagesLabelsTimingUnchanged':True}))
