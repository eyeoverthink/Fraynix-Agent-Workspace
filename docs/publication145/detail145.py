"""Source-only review aid. Red boxes are original OTB annotations, not predictions."""
from pathlib import Path
from PIL import Image,ImageDraw
import zipfile,io,re,json,sys,hashlib
r=Path.cwd();folder=r/'build/otb145';out=folder/'details';out.mkdir(exist_ok=True)
name=sys.argv[1];frames=[int(x) for x in sys.argv[2:]];assert name in ['Basketball','Girl2']
z=zipfile.ZipFile(folder/(name+'.zip'));names=sorted(n for n in z.namelist() if '/img/' in n and n.lower().endswith('.jpg') and '__MACOSX' not in n)
gt=[[float(v) for v in re.split(r'[,\s]+',line.strip())] for line in (r/'docs/publication145'/(name+'-groundtruth.txt')).read_text(encoding='utf-8-sig').splitlines() if line.strip()]
for f in frames:
 raw=z.read(names[f-1]);im=Image.open(io.BytesIO(raw)).convert('RGB');d=ImageDraw.Draw(im);x,y,w,h=gt[f-1];d.rectangle((x,y,x+w,y+h),outline='red',width=2);d.text((5,5),name+' '+str(f)+' / original OTB target',fill='red');p=out/(name+'-'+str(f)+'.png');im.save(p);print(json.dumps({'path':p.relative_to(r).as_posix(),'sourceFrame':f,'originalSha256':hashlib.sha256(raw).hexdigest(),'targetPixels':[x,y,x+w,y+h],'dimensions':im.size}))
