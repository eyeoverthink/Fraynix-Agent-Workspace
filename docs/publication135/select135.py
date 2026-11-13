from pathlib import Path
import zipfile,io,re,json
from PIL import Image,ImageDraw
root=Path('build/otb135');out=root/'source-review';windows=[('Tiger2',59,154,'partial leaf cover and left-edge clipping'),('Box',439,534,'partial cover by caliper'),('David3',57,152,'partial tree occlusion and return'),('Bird1',97,192,'rendered cloud disappearance and return; not real-world camera footage')]
for name,start,end,reason in windows:
 z=zipfile.ZipFile(root/(name+'.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and not n.startswith('__MACOSX/') and not Path(n).name.startswith('._'))
 gt=[list(map(float,re.split(r'[,\s]+',l))) for l in z.read(next(n for n in z.namelist() if 'groundtruth' in n and not n.startswith('__MACOSX/'))).decode().strip().splitlines()]
 for offset in range(0,96,48):
  sheet=Image.new('RGB',(8*160,6*175),(20,26,32));draw=ImageDraw.Draw(sheet)
  for j,n in enumerate(range(start+offset,min(end+1,start+offset+48))):
   im=Image.open(io.BytesIO(z.read(files[n-1]))).convert('RGB');x,y,w,h=gt[n-1];pad=20;bounds=(max(0,int(x-1-pad)),max(0,int(y-1-pad)),min(im.width,int(x-1+w+pad)),min(im.height,int(y-1+h+pad)))
   crop=im.crop(bounds);crop.thumbnail((158,150));cx=(j%8)*160;cy=(j//8)*175;sheet.paste(crop,(cx,cy+20));draw.text((cx+3,cy+2),f'{name} {n}',fill='white')
  sheet.save(out/f'{name}-selected-{offset//48}.png')
Path('build/otb135/window-selection.json').write_text(json.dumps({'windows':windows,'selection':'Before any fresh tracking predictions; algorithm/scorer remain locked. Source review found partial cover in real camera clips, full disappearance into clouds in rendered Bird1. No real full out-of-frame return established. Every selected image will be reviewed; unclear transitions remain uncertain.'},indent=2)+'\n',encoding='utf8')
