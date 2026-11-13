from pathlib import Path
import zipfile,io,json,math,hashlib
from PIL import Image,ImageDraw
r=Path.cwd();folder=r/'build/vot139';out=folder/'source-review';out.mkdir(exist_ok=True);records=[]
for name in ['ballet','bicycle','bike1','bird1','car1']:
 z=zipfile.ZipFile(folder/(name+'-color.zip'));a=zipfile.ZipFile(folder/(name+'-annotations.zip'));raw=a.read('groundtruth.txt');dest=r/'docs/publication139'/(name+'-groundtruth.txt')
 if dest.exists():assert dest.read_bytes()==raw
 else:dest.write_bytes(raw)
 ground=[list(map(float,s.split(','))) for s in raw.decode().splitlines()];files=sorted(n for n in z.namelist() if n.endswith('.jpg'));assert len(ground)==len(files)
 w,h=Image.open(io.BytesIO(z.read(files[0]))).size;absent=lambda n:any(not math.isfinite(v) for v in ground[n-1]);events=[];n=1
 while n<=len(files):
  if not absent(n):n+=1;continue
  start=n
  while n<=len(files) and absent(n):n+=1
  end=n-1;following=0
  while n+following<=len(files) and not absent(n+following):following+=1
  events.append({'start':start,'end':end,'absentFrames':end-start+1,'followingPresent':following})
 chosen=None;skipped=[]
 for event in events:
  if name in ['bird1','car1'] and event['start']!=json.loads((folder/('bird-extra-source.json' if name=='bird1' else 'extra-source.json')).read_text(encoding='utf8'))['event']['start']:continue
  if event['absentFrames']<3 or event['followingPresent']<10:skipped.append({**event,'reason':'Insufficient absence or subsequent presence'});continue
  start=max(1,event['start']-15);end=min(len(files),event['end']+20);box=ground[start-1]
  if absent(start) or min(box[2]/w,box[3]/h)<.025:skipped.append({**event,'reason':'Initial external seed not measurable'});continue
  for stride in range(1,31):
   indices=sorted(set(range(start,end+1,stride))|{start,end,event['start'],event['end'],event['end']+1})
   if len(indices)<=96:break
  else:skipped.append({**event,'reason':'Bounded source sampling does not fit'});continue
  chosen={**event,'windowStart':start,'windowEnd':end,'stride':stride,'indices':indices};break
 if chosen is None and name=='bike1':
  indices=list(range(1,97));chosen={'windowStart':1,'windowEnd':96,'stride':1,'indices':indices,'fallback':'No external absence; continuous-motion control, source note139'}
 assert chosen, (name,events,skipped)
 for off in range(0,len(indices),24):
  sheet=Image.new('RGB',(4*390,6*310),(18,23,29));draw=ImageDraw.Draw(sheet)
  for j,n in enumerate(indices[off:off+24]):
   image=Image.open(io.BytesIO(z.read(files[n-1]))).convert('RGB');d=ImageDraw.Draw(image)
   if not absent(n):
    x,y,bw,bh=ground[n-1];d.rectangle((x,y,x+bw,y+bh),outline='#ffe070',width=2)
   image.thumbnail((388,280));cx=(j%4)*390;cy=(j//4)*310;sheet.paste(image,(cx,cy+25));draw.text((cx+4,cy+3),f'{name} / {n} | '+('NO EXTERNAL BOX' if absent(n) else 'external box'),fill='white')
  sheet.save(out/f'{name}-selected-{off//24}.png')
 records.append({'name':name,'width':w,'height':h,'images':len(files),'selection':chosen,'skippedEvents':skipped,'allEvents':events,'groundtruthSha256':hashlib.sha256(raw).hexdigest()})
(folder/'source-index.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf8');print(json.dumps(records))
