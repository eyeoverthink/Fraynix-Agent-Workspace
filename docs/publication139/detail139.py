from pathlib import Path
import zipfile,io
from PIL import Image,ImageDraw
folder=Path('build/vot139');specs=[('car1',list(range(1515,1553)),(400,50,800,300)),('ballet',list(range(114,142)),(0,0,160,290)),('bicycle',list(range(112,147)),(0,0,240,350))]
for name,indices,crop in specs:
 z=zipfile.ZipFile(folder/(name+'-color.zip'));files=sorted(z.namelist())
 for off in range(0,len(indices),20):
  sheet=Image.new('RGB',(1500,1000));d=ImageDraw.Draw(sheet)
  for j,n in enumerate(indices[off:off+20]):
   im=Image.open(io.BytesIO(z.read(files[n-1]))).crop(crop);im.thumbnail((296,220));x=(j%5)*300;y=(j//5)*250;sheet.paste(im,(x,y+25));d.text((x+3,y+3),name+' / '+str(n),fill='white')
  sheet.save(folder/'source-review'/f'{name}-detail-{off//20}.png')
