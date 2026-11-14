"""Original source-only sheets. No tracker, source outcome or automatic visibility assignment."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,zipfile,io,re,sys,hashlib
r=Path.cwd();name=sys.argv[1];root=r/'build/otb147';out=root/(name+'-review');out.mkdir(exist_ok=False)
z=zipfile.ZipFile(root/(name+'.zip'));images=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and '/img/' in n and '__MACOSX' not in n)
raw=(r/'docs/publication147'/(name+'-groundtruth.txt')).read_bytes();boxes=[[float(v) for v in re.split(r'[,\s]+',line.strip())] for line in raw.decode('utf-8-sig').splitlines() if line.strip()]
assert len(boxes)==len(images),(len(boxes),len(images));frames=[]
for offset in range(0,len(images),16):
 sheet=Image.new('RGB',(1600,1200),'#15232b');d=ImageDraw.Draw(sheet)
 for j in range(offset,min(offset+16,len(images))):
  b=z.read(images[j]);im=Image.open(io.BytesIO(b)).convert('RGB');w,h=im.size;draw=ImageDraw.Draw(im);x,y,bw,bh=boxes[j]
  if all(v==v for v in boxes[j]) and bw>0 and bh>0:draw.rectangle([x,y,x+bw,y+bh],outline='#ffd35c',width=2)
  im.thumbnail((398,272));left=((j-offset)%4)*400;top=((j-offset)//4)*300;sheet.paste(im,(left,top+26));d.text((left+4,top+5),name+' frame '+str(j+1),fill='white');frames.append({'sourceFrame':j+1,'archivePath':images[j],'imageSha256':hashlib.sha256(b).hexdigest(),'width':w,'height':h,'originalBox':boxes[j]})
 sheet.save(out/('sheet-'+str(offset//16+1)+'.jpg'),quality=95)
(out/'source-review-index.json').write_text(json.dumps({'source':name,'frames':frames,'sheets':(len(images)+15)//16,'scope':'Original rectangles only; visibility/identity must be assigned by source-only review before any predictions.'},indent=2)+'\n',encoding='utf8');print(json.dumps({'source':name,'frames':len(images),'sheets':(len(images)+15)//16}))
