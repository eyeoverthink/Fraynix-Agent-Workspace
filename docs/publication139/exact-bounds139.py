from pathlib import Path
from fractions import Fraction
import json,math,hashlib,shutil
r=Path.cwd();mp=r/'docs/publication139/source139.json';m=json.loads(mp.read_text(encoding='utf8'));changes=[];archive=r/'build/source-labels139-exact-before';archive.mkdir(exist_ok=False);shutil.copy2(mp,archive/mp.name)
for spec in m['records']:
 text=(r/spec['originalGroundTruth']).read_text(encoding='utf8').splitlines();w,h=spec['width'],spec['height']
 def normalized(n):
  vals=[Fraction(s) for s in text[n-1].split(',')];x,y,bw,bh=vals;b=[max(Fraction(0),x/w),max(Fraction(0),y/h),min(Fraction(1),(x+bw)/w),min(Fraction(1),(y+bh)/h)];out=list(map(float,b))
  for a in [0,1]:
   if b[a+2]-b[a]>=Fraction(1,40) and out[a+2]-out[a]<.025:
    old=out[a+2];out[a+2]=math.nextafter(out[a+2],1);changes.append({'case':spec['case'],'frame':n,'axis':a,'oldEndpoint':old,'newEndpoint':out[a+2],'reason':'Exact rational side meets unchanged1/40 bound; outward one-ULP endpoint prevents floating subtraction underflow.'})
  return out
 p=r/spec['pack'];pack=json.loads(p.read_text(encoding='utf8'));seed=normalized(spec['start']);assert min(seed[2]-seed[0],seed[3]-seed[1])>=.025
 if pack['objects'][0]['box']!=seed:
  shutil.copy2(p,archive/p.name);pack['objects'][0]['box']=seed;p.write_text(json.dumps(pack),encoding='utf8');spec['packSha256']=hashlib.sha256(p.read_bytes()).hexdigest()
 lp=r/spec['labels'];d=json.loads(lp.read_text(encoding='utf8'));shutil.copy2(lp,archive/lp.name)
 first=json.loads((r/'build/source-labels139-first'/lp.name).read_text(encoding='utf8'))
 for row,original in zip(d['rows'],first['rows']):
  if 'targetBox' not in original:continue
  b=normalized(row['sourceFrame'])
  if min(b[2]-b[0],b[3]-b[1])>=.025:
   row['targetBox']=b;row['visibility']=original['visibility'];row['note']=original['note']+' Exact rational normalization respects the existing side bound; any one-ULP endpoint adjustment is recorded in source manifest.'
 lp.write_text(json.dumps(d,indent=2)+'\n',encoding='utf8');spec['labelsSha256']=hashlib.sha256(lp.read_bytes()).hexdigest();spec['visibilityCounts']={v:sum(a['visibility']==v for a in d['rows']) for v in spec['visibilityCounts']}
m['rationalNormalizationCorrections']=changes;m['sourceReview']=m['sourceReview'].replace('20 visible small/clipped external boxes','14 visible small/clipped external boxes');m['sourceReview']+=' Six score boxes and one seed meet the exact rational1/40 side bound but double subtraction rounded below it; a single outward endpoint ULP preserves the mathematically valid rectangle. No pixel-scale padding, relaxed bound or changed source image; each exact correction retained.';mp.write_text(json.dumps(m,indent=2)+'\n',encoding='utf8');print(json.dumps(changes))
