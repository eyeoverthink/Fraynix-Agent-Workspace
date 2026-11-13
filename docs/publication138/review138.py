from pathlib import Path
import zipfile,io,re,json
from PIL import Image,ImageDraw
r=Path.cwd();folder=r/'build/otb138';out=folder/'source-review';out.mkdir(exist_ok=True);records=[]
for name in ['Skating2','Suv']:
 z=zipfile.ZipFile(folder/(name+'.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and not n.startswith('__MACOSX/') and not Path(n).name.startswith('._'))
 ground=[]
 for entry in sorted(n for n in z.namelist() if 'groundtruth' in n and not n.startswith('__MACOSX/')):
  raw=z.read(entry);dest=r/'docs/publication138'/(name+'-'+Path(entry).name)
  if dest.exists():assert dest.read_bytes()==raw
  else:dest.write_bytes(raw)
  ground.append([list(map(float,re.split(r'[,\s]+',s.strip()))) for s in raw.decode().strip().splitlines()])
 w,h=Image.open(io.BytesIO(z.read(files[0]))).size;indices=list(range(1,len(files)+1,8));indices+=[] if indices[-1]==len(files) else [len(files)]
 for off in range(0,len(indices),36):
  sheet=Image.new('RGB',(6*260,6*195),(18,23,29));draw=ImageDraw.Draw(sheet)
  for j,n in enumerate(indices[off:off+36]):
   image=Image.open(io.BytesIO(z.read(files[n-1]))).convert('RGB');d=ImageDraw.Draw(image)
   for k,boxes in enumerate(ground):
    x,y,bw,bh=boxes[n-1];d.rectangle((x-1,y-1,x-1+bw,y-1+bh),outline=['#ffe070','#6dc8ff'][k],width=2)
   image.thumbnail((258,170));cx=(j%6)*260;cy=(j//6)*195;sheet.paste(image,(cx,cy+22));draw.text((cx+4,cy+3),f'{name} / {n} | gold1 blue2',fill='white')
  sheet.save(out/f'{name}-overview-{off//36}.png')
 records.append({'name':name,'images':len(files),'width':w,'height':h,'annotationTargets':len(ground),'overviewStride':8,'sourceReview':'Original pixels and external boxes only; no model output.'})
(folder/'source-index.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf8');print(json.dumps(records))
