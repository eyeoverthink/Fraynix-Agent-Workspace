"""Private source-only contact sheets. No predicted boxes or tracking results."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,sys,hashlib
r=Path.cwd();root=r/'build/webcam147-private';folder=(root/sys.argv[1]).resolve();assert folder.parent==root.resolve()
capture=json.loads((folder/'capture.json').read_bytes());out=folder/'source-review';out.mkdir(exist_ok=True)
frames=capture['frames'];size=(400,330)
for base in range(0,len(frames),16):
 sheet=Image.new('RGB',(1600,1320),'#101b29');draw=ImageDraw.Draw(sheet)
 for slot,f in enumerate(frames[base:base+16]):
  raw=(folder/f['file']).read_bytes();assert hashlib.sha256(raw).hexdigest()==f['imageSha256']
  im=Image.open(folder/f['file']).convert('RGB');im.thumbnail((400,300));x=slot%4*400;y=slot//4*330
  sheet.paste(im,(x,y));draw.text((x+4,y+302),f"{f['sourceFrame']} | {f['seconds']:.2f}s | cue {f['phase']}",fill='white')
 sheet.save(out/f'source-{base+1:04d}-{min(len(frames),base+16):04d}.jpg',quality=90)
print(json.dumps({'privateReviewFolder':str(out),'frames':len(frames),'labels':False,'tracking':False}))
