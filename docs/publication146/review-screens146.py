"""Contact sheets for actual visual inspection; no automated pass declaration."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,hashlib
r=Path.cwd();out=r/'build/screens146';out.mkdir(exist_ok=False);items=[]
manifest=json.loads((r/'docs/publication146/source146.json').read_text(encoding='utf8'))
for group in sorted({x['group'] for x in manifest['records']}):
 for kind in ['app','review']:
  for p in sorted((r/'build'/('return146-'+group+'-'+kind)).glob('*.png')):
   if p.name=='failure.png':continue
   items.append({'path':p.relative_to(r).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
for offset in range(0,len(items),4):
 sheet=Image.new('RGB',(1600,1100),'#10202d');d=ImageDraw.Draw(sheet)
 for i,item in enumerate(items[offset:offset+4]):
  im=Image.open(r/item['path']);im.thumbnail((790,515));x=(i%2)*800;y=(i//2)*550;sheet.paste(im,(x,y+28));d.text((x+4,y+7),item['path'].replace('build/',''),fill='white')
 sheet.save(out/('sheet-'+str(offset//4+1)+'.jpg'),quality=94)
(out/'inventory.json').write_text(json.dumps({'images':items,'sheets':(len(items)+3)//4},indent=2)+'\n',encoding='utf8');print(json.dumps({'images':len(items),'sheets':(len(items)+3)//4}))
