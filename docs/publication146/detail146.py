"""Render requested original frames with external rectangles for source-only boundary review."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,zipfile,io,sys
r=Path.cwd();name=sys.argv[1];root=r/'build/otb146';out=root/(name+'-review');index=json.loads((out/'source-review-index.json').read_text(encoding='utf8'));z=zipfile.ZipFile(root/(name+'.zip'))
for ordinal in map(int,sys.argv[2:]):
 row=index['frames'][ordinal-1];im=Image.open(io.BytesIO(z.read(row['archivePath']))).convert('RGB');d=ImageDraw.Draw(im);x,y,w,h=row['originalBox'];d.rectangle([x,y,x+w,y+h],outline='#ffd35c',width=2);im.save(out/('detail-'+str(ordinal)+'.png'))
print('Source-only detail frames saved')
