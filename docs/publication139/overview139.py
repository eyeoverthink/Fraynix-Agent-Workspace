from pathlib import Path
import zipfile,io
from PIL import Image,ImageDraw
folder=Path('build/vot139');z=zipfile.ZipFile(folder/'bike1-color.zip');files=sorted(z.namelist());gt=[list(map(float,s.split(','))) for s in zipfile.ZipFile(folder/'bike1-annotations.zip').read('groundtruth.txt').decode().splitlines()];ns=list(range(1,len(gt)+1,30))
for off in range(0,len(ns),35):
 s=Image.new('RGB',(1560,1400));d=ImageDraw.Draw(s)
 for j,n in enumerate(ns[off:off+35]):
  im=Image.open(io.BytesIO(z.read(files[n-1]))).convert('RGB');x,y,w,h=gt[n-1];ImageDraw.Draw(im).rectangle((x,y,x+w,y+h),outline='yellow',width=3);im.thumbnail((310,175));s.paste(im,((j%5)*312,(j//5)*200+22));d.text(((j%5)*312+2,(j//5)*200+2),str(n),fill='white')
 s.save(folder/'source-review'/('bike1-overview-'+str(off//35)+'.png'))
