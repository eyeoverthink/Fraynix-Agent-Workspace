from pathlib import Path
import hashlib,json,math
r=Path.cwd();m=json.loads((r/'build/reserved152/manifest.json').read_bytes());checks=[]
for case in m['cases']:
 p=Path(case['pack']);l=Path(case['labels']);assert hashlib.sha256(p.read_bytes()).hexdigest()==case['packSha256'];assert hashlib.sha256(l.read_bytes()).hexdigest()==case['labelsSha256']
 labels=json.loads(l.read_bytes());angle=-67 if case['name']=='return-minus67' else 22
 for row in labels['rows']:
  i=row['ordinal'];start=i<=4;covered=5<=i<=9;wrong=case['name']=='wrong-only' and i>=10;copy=case['name']=='copy-veto' and i>=10
  assert row['visibility']==('occluded' if covered else 'out-of-view' if wrong else 'visible')
  if covered:assert 'targetBox' not in row;continue
  x=40 if start else 120+min(5,i-10);y=56 if start else 80;a=math.radians(0 if start else angle)
  corners=[(x+u*math.cos(a)-v*math.sin(a),y+u*math.sin(a)+v*math.cos(a)) for u in [-16,16] for v in [-32,32]]
  box=[min(c[0] for c in corners)/192,min(c[1] for c in corners)/144,max(c[0] for c in corners)/192,max(c[1] for c in corners)/144]
  observed=row['otherBoxes'][0] if wrong else row['targetBox'];assert all(abs(a-b)<1e-12 for a,b in zip(box,observed))
  if copy:assert all(abs(a-b)<1e-12 for a,b in zip(row['otherBoxes'][0],[24/192,24/144,56/192,88/144]))
 checks.append({'case':case['name'],'frames':19,'labels':len(labels['rows']),'originalPackSha256':case['packSha256'],'originalLabelsSha256':case['labelsSha256'],'independentCornerCalculation':True})
result={'passed':True,'checks':checks,'scope':'Independent Python corner calculation verifies the original, unchanged generated labels. Initial producer shared Java pose-bound geometry; this separate check uses explicit transformed corners. No tracker results used.'}
(r/'build/source-check152.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
