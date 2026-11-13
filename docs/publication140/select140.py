"""Fixed source-only selection; never reads tracker output."""
from pathlib import Path
from fractions import Fraction
import json,math,zipfile,hashlib,io
from PIL import Image,ImageDraw
r=Path.cwd();folder=r/'build/vot140';out=r/'docs/publication140'
meta=json.loads((out/'vot-metadata-before-lock.json').read_text(encoding='utf8'));result=[]
def rows(raw):return [line.strip().split(',') for line in raw.decode('utf-8-sig').splitlines() if line.strip()]
def finite(v):return len(v)==4 and all(math.isfinite(float(x)) for x in v)
def rectangle(v,w,h):
 x,y,bw,bh=map(Fraction,v);return [max(Fraction(0),x/w),max(Fraction(0),y/h),min(Fraction(1),(x+bw)/w),min(Fraction(1),(y+bh)/h)]
def measurable(v,w,h):
 if not finite(v):return False
 b=rectangle(v,w,h);return b[2]-b[0]>=Fraction(1,40) and b[3]-b[1]>=Fraction(1,40)
if __name__=='__main__':
 for spec in meta['sequences']:
  name=spec['name']
  if name not in ['cat1','cat2','group1']:continue
  w,h=spec['width'],spec['height'];annotations=zipfile.ZipFile(folder/(name+'-annotations.zip'));groundname=next(n for n in annotations.namelist() if n.endswith('groundtruth.txt'));raw=annotations.read(groundname)
  (out/(name+'-groundtruth.txt')).write_bytes(raw);ground=rows(raw);events=[];i=0
  while i<len(ground):
   if finite(ground[i]):i+=1;continue
   a=i
   while i<len(ground) and not finite(ground[i]):i+=1
   b=i-1;qualifies=b-a+1>=3 and i+10<=len(ground) and all(finite(x) for x in ground[i:i+10]);events.append({'start':a+1,'end':b+1,'qualifies':qualifies})
  selected=None;skipped=[]
  for e in events:
   if not e['qualifies']:continue
   start=max(1,e['start']-15);end=min(len(ground),e['end']+20)
   if not measurable(ground[start-1],w,h):skipped.append({**e,'reason':'Initial external rectangle violates existing measurable bounds'});continue
   for stride in range(1,31):
    indices=sorted(set(range(start,end+1,stride))|{start,end,e['start'],e['end'],e['end']+1})
    if len(indices)<=96:break
   if len(indices)>96:skipped.append({**e,'reason':'Event exceeds fixed image/stride bounds'});continue
   selected={'windowStart':start,'windowEnd':end,'stride':stride,'indices':indices,'absence':e};break
  if selected is None:
   assert measurable(ground[0],w,h),(name,'No qualifying measurable event or control seed')
   end=min(96,len(ground));selected={'windowStart':1,'windowEnd':end,'stride':1,'indices':list(range(1,end+1)),'absence':None,'note':'No qualifying event under fixed bounds; retain declared first96 control'}
  z=zipfile.ZipFile(folder/(name+'-color.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg'));assert len(files)==len(ground),(name,len(files),len(ground))
  indices=selected['indices'];sheets=[]
  for page in range(0,len(indices),24):
   sheet=Image.new('RGB',(1120,1200),'#10252a');draw=ImageDraw.Draw(sheet)
   for k,n in enumerate(indices[page:page+24]):
    image=Image.open(io.BytesIO(z.read(files[n-1]))).convert('RGB');assert image.size==(w,h)
    if finite(ground[n-1]):
     box=rectangle(ground[n-1],w,h);d=ImageDraw.Draw(image);d.rectangle(tuple(float(v)*size for v,size in zip(box,[w,h,w,h])),outline='#ffcc44',width=3)
    image.thumbnail((274,166));x=(k%4)*280;y=(k//4)*200;sheet.paste(image,(x,y+26));draw.text((x+5,y+5),f'{name} / {n} / '+('box' if finite(ground[n-1]) else 'NaN'),fill='white')
   p=folder/f'{name}-sheet-{page//24+1}.jpg';sheet.save(p,quality=92);sheets.append(p.relative_to(r).as_posix())
  result.append({'name':name,'width':w,'height':h,'sourceLength':len(ground),'groundtruthSha256':hashlib.sha256(raw).hexdigest(),'events':events,'selection':selected,'skippedEvents':skipped,'sheets':sheets})
 (folder/'source-index.json').write_bytes((json.dumps(result,indent=2)+'\n').encode())
 print(json.dumps([{k:s[k] for k in ['name','selection','skippedEvents','sheets']} for s in result]))
