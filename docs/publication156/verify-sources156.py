from pathlib import Path
import hashlib,json,math
r=Path.cwd();m=json.loads((r/'build/reserved156/manifest.json').read_bytes());checks=[]
donor_hashes={}
for donor in m['donors']:
 p=Path(donor['pack']);l=Path(donor['labels']);assert hashlib.sha256(p.read_bytes()).hexdigest()==donor['packSha256'];assert hashlib.sha256(l.read_bytes()).hexdigest()==donor['labelsSha256']
 pack=json.loads(p.read_bytes());labels=json.loads(l.read_bytes());assert len(pack['frames'])==5 and len(labels['rows'])==4
 for row in labels['rows']:
  assert row['visibility']=='visible' and row['sourceFrame']==row['ordinal']+1
  assert row['imageSha256']==pack['frames'][row['ordinal']]['imageSha256']
  assert all(abs(a-b)<1e-12 for a,b in zip(row['targetBox'],[24/192,24/144,56/192,88/144]))
 donor_hashes[donor['pattern']]={f['imageSha256'] for f in pack['frames']}
 checks.append({'case':donor['name'],'frames':5,'labels':4,'independentHalfExtents':True,'originalPackSha256':donor['packSha256'],'originalLabelsSha256':donor['labelsSha256']})
for case in m['cases']:
 p=Path(case['pack']);l=Path(case['labels']);assert hashlib.sha256(p.read_bytes()).hexdigest()==case['packSha256'];assert hashlib.sha256(l.read_bytes()).hexdigest()==case['labelsSha256']
 pack=json.loads(p.read_bytes());assert not donor_hashes[case['pattern']] & {f['imageSha256'] for f in pack['frames']};labels=json.loads(l.read_bytes());n=case['name'];count=32 if n=='reversing-motion' else 38 if n=='double-return' else 31 if n=='long-cover-return' else 28
 assert len(labels['rows'])==count-1
 for row in labels['rows']:
  i=row['ordinal'];start=i<6;continuous=n=='reversing-motion';two=n=='double-return';part=n=='partial-before-return';second=two and i>=24
  covered=not continuous and (6<=i<=10 if part else (6<=i<=11 or 24<=i<=29) if two else 6<=i<=17)
  wrong=n in ['wrong-only','near-copy-only'] and i>=18;copy=n=='copy-veto' and i>=18;partial=part and 11<=i<=14
  assert row['visibility']==('occluded' if covered else 'out-of-view' if wrong else 'partial' if partial else 'visible')
  if covered:assert 'targetBox' not in row;continue
  returned=11 if part else (30 if second else 12) if two else 18
  x=40+.65*(i if i<=16 else 32-i) if continuous else 40 if start else 80+(i-30)*.35 if second else 124+max(0,i-returned)*.35
  y=56 if continuous or start else 60 if second else 78;a=math.radians(0 if continuous or start else -43 if part else (137 if second else -103) if two else 103)
  # Independent half-extents, rather than Java's four-corner enumeration.
  hx=abs(math.cos(a))*16+abs(math.sin(a))*32;hy=abs(math.sin(a))*16+abs(math.cos(a))*32
  box=[(x-hx)/192,(y-hy)/144,(x+hx)/192,(y+hy)/144]
  observed=row['otherBoxes'][0] if wrong else row['targetBox'];assert all(abs(a-b)<1e-12 for a,b in zip(box,observed))
  if copy:assert all(abs(a-b)<1e-12 for a,b in zip(row['otherBoxes'][0],[24/192,24/144,56/192,88/144]))
 checks.append({'case':n,'frames':count,'labels':len(labels['rows']),'originalPackSha256':case['packSha256'],'originalLabelsSha256':case['labelsSha256'],'independentHalfExtents':True})
result={'passed':True,'checks':checks,'scope':'Independent source-only Python schedule and half-extents verification. No tracking results or predictions used.'}
(r/'build/source-check156.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
