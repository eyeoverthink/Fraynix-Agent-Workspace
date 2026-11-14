from pathlib import Path
import json,re,hashlib
r=Path.cwd();manifest=json.loads((r/'docs/publication142/source142.json').read_text(encoding='utf8'));seen=set();files=[]
for p in sorted((r/'docs').glob('publication*/source*.json')):
 if p.parent.name=='publication142':continue
 t=p.read_text(encoding='utf-8-sig');seen.update(re.findall(r'"imageSha256"\s*:\s*"([a-f0-9]{64})"',t));files.append(p.as_posix())
# Labels carry frame digests in earlier editions even when manifests do not.
for p in sorted((r/'docs').glob('publication*/*-labels.json')):
 if p.parent.name=='publication142':continue
 seen.update(re.findall(r'"imageSha256"\s*:\s*"([a-f0-9]{64})"',p.read_text(encoding='utf-8-sig')));files.append(p.as_posix())
checked=[]
for s in manifest['records']:
 if s['domain']!='real-camera':continue
 pack=json.loads((r/s['pack']).read_text(encoding='utf8'));hashes={x['imageSha256'] for x in pack['frames']};assert not hashes.intersection(seen),s['case'];checked.append({'case':s['case'],'frames':len(hashes),'priorMatches':0})
result={'passed':True,'priorFiles':len(files),'priorImageHashes':len(seen),'records':checked,'scope':'Exact JPEG hashes against earlier source/label manifests; not a claim against differently encoded images.'}
(r/'build/fresh142-image-check.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result))
