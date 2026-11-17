from pathlib import Path
import hashlib,json,math
r=Path.cwd();m=json.loads((r/'build/reserved154/manifest.json').read_bytes());checks=[]
for case in m['cases']:
 p=Path(case['pack']);l=Path(case['labels']);assert hashlib.sha256(p.read_bytes()).hexdigest()==case['packSha256'];assert hashlib.sha256(l.read_bytes()).hexdigest()==case['labelsSha256']
 labels=json.loads(l.read_bytes());n=case['name'];count=35 if n=='double-return' else 23
 assert len(labels['rows'])==count-1
 for row in labels['rows']:
  i=row['ordinal'];start=i<=4;continuous=n=='continuous-motion';second=n=='double-return' and i>=20
  covered=not continuous and (5<=i<=9 or second and i<=24);wrong=n in ['wrong-only','near-copy-only'] and i>=10;copy=n=='copy-veto' and i>=10
  assert row['visibility']==('occluded' if covered else 'out-of-view' if wrong else 'visible')
  if covered:assert 'targetBox' not in row;continue
  x=40+i*.6 if continuous else 40 if start else 74+max(0,i-25)*.4 if second else 118+min(8,(i-10)*.5);y=56 if continuous or start else 52 if second else 80;a=math.radians(0 if continuous or start else -77 if second else 67)
  corners=[(x+u*math.cos(a)-v*math.sin(a),y+u*math.sin(a)+v*math.cos(a)) for u in [-16,16] for v in [-32,32]]
  box=[min(c[0] for c in corners)/192,min(c[1] for c in corners)/144,max(c[0] for c in corners)/192,max(c[1] for c in corners)/144]
  observed=row['otherBoxes'][0] if wrong else row['targetBox'];assert all(abs(a-b)<1e-12 for a,b in zip(box,observed))
  if copy:assert all(abs(a-b)<1e-12 for a,b in zip(row['otherBoxes'][0],[24/192,24/144,56/192,88/144]))
 checks.append({'case':n,'frames':count,'labels':len(labels['rows']),'originalPackSha256':case['packSha256'],'originalLabelsSha256':case['labelsSha256'],'independentCornerCalculation':True})
result={'passed':True,'checks':checks,'scope':'Independent Python corner/schedule calculation against original generated labels; no tracker results or predictions used.'}
(r/'build/source-check154.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
