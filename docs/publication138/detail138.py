from pathlib import Path
import sys,zipfile,re,io
from PIL import Image,ImageDraw
r=Path.cwd();name=sys.argv[1];start,end,stride=map(int,sys.argv[2:5]);folder=r/'build/otb138';z=zipfile.ZipFile(folder/(name+'.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and not n.startswith('__MACOSX/') and not Path(n).name.startswith('._'))
ground=[]
for entry in sorted(n for n in z.namelist() if 'groundtruth' in n and not n.startswith('__MACOSX/')):
 raw=z.read(entry);dest=r/'docs/publication138'/(name+'-'+Path(entry).name)
 if dest.exists():assert dest.read_bytes()==raw
 else:dest.write_bytes(raw)
 ground.append([list(map(float,re.split(r'[,\s]+',s.strip()))) for s in raw.decode().strip().splitlines()])
indices=list(range(start,min(end,len(files))+1,stride));out=folder/'source-review';out.mkdir(exist_ok=True)
for off in range(0,len(indices),32):
 sheet=Image.new('RGB',(8*190,4*205),(18,23,29));draw=ImageDraw.Draw(sheet)
 for j,n in enumerate(indices[off:off+32]):
  image=Image.open(io.BytesIO(z.read(files[n-1]))).convert('RGB');boxes=[g[n-1] for g in ground];d=ImageDraw.Draw(image)
  for k,(x,y,w,h) in enumerate(boxes):d.rectangle((x-1,y-1,x+w-1,y+h-1),outline=['#ffe070','#6dc8ff'][k],width=1)
  left=min(b[0] for b in boxes)-20;top=min(b[1] for b in boxes)-15;right=max(b[0]+b[2] for b in boxes)+20;bottom=max(b[1]+b[3] for b in boxes)+15
  crop=image.crop((max(0,left),max(0,top),min(image.width,right),min(image.height,bottom)));crop.thumbnail((188,178));cx=j%8*190;cy=j//8*205;sheet.paste(crop,(cx,cy+23));draw.text((cx+3,cy+3),f'{name} / {n}',fill='white')
 sheet.save(out/f'{name}-detail-{start}-{stride}-{off//32}.png')
print(str(len(indices))+' source-only images rendered')
