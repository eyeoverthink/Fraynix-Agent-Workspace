from pathlib import Path
import json,hashlib,shutil
r=Path.cwd();p=r/'docs/publication139/source139.json';m=json.loads(p.read_text(encoding='utf8'));archive=r/'build/source-labels139-first';archive.mkdir(exist_ok=False);shutil.copy2(p,archive/p.name);changed=[]
for spec in m['records']:
 lp=r/spec['labels'];raw=lp.read_bytes();assert hashlib.sha256(raw).hexdigest()==spec['labelsSha256'];shutil.copy2(lp,archive/lp.name);d=json.loads(raw)
 for row in d['rows']:
  row.pop('externalRegion') # Its distinction is already preserved in each note and the original annotation.
  if 'targetBox' in row:
   b=row['targetBox']
   if min(b[2]-b[0],b[3]-b[1])<.025:
    changed.append({'case':spec['case'],'sourceFrame':row['sourceFrame'],'originalVisibility':row['visibility'],'externalBox':b});row.pop('targetBox');row['visibility']='uncertain';row['note']='External finite rectangle is visible but below the unchanged0.025 normalized side bound. Retained as uncertain/unscorable by this existing schema, not hidden. Original rectangle preserved in source annotations.'
 lp.write_text(json.dumps(d,indent=2)+'\n',encoding='utf8');spec['labelsSha256']=hashlib.sha256(lp.read_bytes()).hexdigest();spec['visibilityCounts']={v:sum(x['visibility']==v for x in d['rows']) for v in spec['visibilityCounts']}
m['sourceReview']+=' Existing audit format requires every scored rectangle to have both normalized sides at least0.025. '+str(len(changed))+' visible small/clipped external boxes are therefore retained as uncertain/unscorable, never expanded or classified hidden. Original external rectangles remain intact; pre-format inputs retained in Git d84ccd12 and original evidence. Unsupported extra row metadata was removed; the same external finite/NaN distinction remains in row notes.'
m['formatCorrectionsBeforeTracking']=changed;p.write_text(json.dumps(m,indent=2)+'\n',encoding='utf8');print(json.dumps(changed))
