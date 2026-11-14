from pathlib import Path
from PIL import Image,ImageDraw
import zipfile,io,hashlib,json,datetime
r=Path.cwd();folder=r/'build/otb143';out=folder/'review';out.mkdir(exist_ok=False);receipts=[]
for name in ['FaceOcc1','Coupon']:
 z=zipfile.ZipFile(folder/(name+'.zip'));names=sorted(n for n in z.namelist() if '/img/' in n and n.lower().endswith('.jpg') and '__MACOSX' not in n)
 for start in range(0,120,24):
  sheet=Image.new('RGB',(1600,1040),'#11171e');d=ImageDraw.Draw(sheet)
  for j in range(24):
   raw=z.read(names[start+j]);im=Image.open(io.BytesIO(raw));im.thumbnail((390,140));x=(j%4)*400;y=(j//4)*173;sheet.paste(im,(x,y+23));d.text((x+5,y+4),name+' / '+str(start+j+1),fill='white')
  path=out/(name+'-'+str(start+1)+'.jpg');sheet.save(path,quality=92);receipts.append({'path':path.relative_to(r).as_posix(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'source':name,'first':start+1,'last':start+24})
(out/'sheets.json').write_text(json.dumps({'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sheets':receipts},indent=2),encoding='utf8')
print(json.dumps(receipts))
