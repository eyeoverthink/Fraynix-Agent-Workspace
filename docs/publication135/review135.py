from pathlib import Path
import zipfile,io,re,json
from PIL import Image,ImageDraw
root=Path('build/otb135');out=root/'source-review';out.mkdir(exist_ok=True);records=[]
for name in ['Tiger2','Box']:
 p=root/(name+'.zip')
 if not p.exists():continue
 z=zipfile.ZipFile(p);files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and not n.startswith('__MACOSX/') and not Path(n).name.startswith('._'));gt=z.read(next(n for n in z.namelist() if 'groundtruth' in n));dest=Path('docs/publication135')/(name+'-groundtruth_rect.txt');assert not dest.exists();dest.write_bytes(gt)
 boxes=[list(map(float,re.split(r'[,\s]+',l.strip()))) for l in gt.decode().strip().splitlines()];w,h=Image.open(io.BytesIO(z.read(files[0]))).size
 indices=list(range(1,len(files)+1,8));indices+=[] if indices[-1]==len(files) else [len(files)]
 for offset in range(0,len(indices),48):
  sheet=Image.new('RGB',(8*200,6*170),(20,26,32));draw=ImageDraw.Draw(sheet)
  for j,n in enumerate(indices[offset:offset+48]):
   image=Image.open(io.BytesIO(z.read(files[n-1]))).convert('RGB');d=ImageDraw.Draw(image);x,y,bw,bh=boxes[n-1];d.rectangle((x-1,y-1,x-1+bw,y-1+bh),outline=(255,230,80),width=2);image.thumbnail((198,146));cx=(j%8)*200;cy=(j//8)*170;sheet.paste(image,(cx,cy+20));draw.text((cx+3,cy+2),f'{name} / {n}',fill='white')
  sheet.save(out/f'{name}-overview-{offset//48}.png')
 outside=[i+1 for i,(x,y,bw,bh) in enumerate(boxes) if x-1+bw<=0 or y-1+bh<=0 or x-1>=w or y-1>=h]
 records.append({'name':name,'images':len(files),'width':w,'height':h,'outsideAnnotatedBoxFrames':outside,'overviewStride':8})
(root/'source-index.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf8');print(json.dumps(records))
