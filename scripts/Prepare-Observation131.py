"""Explicit fresh windows from the already downloaded EPFL source. No installs or downloads.
First-frame annotation may seed a box; later annotations are separate scorer-only files.
"""
from pathlib import Path
import json,math,base64,hashlib,subprocess,sys
root=Path(__file__).resolve().parent.parent
cache=Path(sys.argv[1]);out=Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=False)
lock=json.loads((root/'docs/OBSERVATION_131_LOCK.json').read_text(encoding='utf8'))
sha=lambda b:hashlib.sha256(b).hexdigest()
for name,digest in lock['sources'].items():
 assert sha((root/name).read_bytes().replace(b'\r\n',b'\n'))==digest,name
video=cache/'terrace1-c1.original.avi'
import zipfile,xml.etree.ElementTree as ET
with zipfile.ZipFile(cache/'terrace-tsai.original.zip') as z:camera=ET.fromstring(z.read('terrace-tsai-c1.xml'))
attrs={k:float(v) for child in camera for k,v in child.attrib.items()}
keys=['focal','kappa1','cx','cy','sx','dpx','dpy','tx','ty','tz','rx','ry','rz','width','height']
t={k:attrs[k] for k in keys};t['metresPerUnit']=.001
geometry={'mode':'tsai','cameraKey':'epfl-terrace-c1-fixed','note':'EPFL terrace camera1; ground z=0, mm to metres. New camera pixels, shared people and event with c0; 250mm grid, not certified metrology.','anchorSigmaM':math.sqrt(2)*.125,'scaleRelativeSigma':.01,'tsai':t}
(out/'geometry.json').write_text(json.dumps(geometry,indent=2),encoding='utf8')
metadata=json.loads(subprocess.check_output(['ffprobe','-v','error','-select_streams','v:0','-show_entries','stream=width,height,avg_frame_rate,nb_frames','-of','json',str(video)],text=True))
assert metadata['streams'][0]['avg_frame_rate']=='25/1'
(out/'source-metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf8')
lines=(cache/'gt_terrace1.original.txt').read_text(encoding='utf8').splitlines();assert list(map(int,lines[1].split()))[:5]==[5010,9,30,44,25];rows=[list(map(int,s.split())) for s in lines[2:] if s.strip()]
cx,sx,cy,sy,cz,sz=math.cos(t['rx']),math.sin(t['rx']),math.cos(t['ry']),math.sin(t['ry']),math.cos(t['rz']),math.sin(t['rz'])
rotation=[[cy*cz,cz*sx*sy-cx*sz,sx*sz+cx*cz*sy],[cy*sz,cx*cz+sx*sy*sz,cx*sy*sz-cz*sx],[-sy,cy*sx,cx*cy]]
def ground(cell):return [((cell%30)+.5)*250-500,((cell//30)+.5)*250-1500]
def image(x,y,z):
 v=[rotation[i][0]*x+rotation[i][1]*y+rotation[i][2]*z+t[k] for i,k in enumerate(['tx','ty','tz'])];xu=t['focal']*v[0]/v[2];yu=t['focal']*v[1]/v[2];ru=math.hypot(xu,yu);rd=ru
 for _ in range(30):rd-=(rd*(1+t['kappa1']*rd*rd)-ru)/(1+3*t['kappa1']*rd*rd)
 factor=rd/ru if ru else 1
 return [(xu*factor*t['sx']/t['dpx']+t['cx'])/t['width'],(yu*factor/t['dpy']+t['cy'])/t['height']]
records=[]
for start in lock['heldStarts']:
 name='held-'+str(start);choice=None
 for person,cell in enumerate(rows[start]):
  if cell<0:continue
  x,y=ground(cell);foot=image(x,y,0);head=image(x,y,1750);left=image(x-250,y,0);right=image(x+250,y,0);width=max(.025,abs(right[0]-left[0]));box=[foot[0]-width/2,min(head[1],foot[1]),foot[0]+width/2,max(head[1],foot[1])]
  if all(0<=v<=1 for v in box) and box[2]-box[0]>=.025 and box[3]-box[1]>=.025:choice=(person,box);break
 if choice is None:records.append({'name':name,'refused':'No valid first-frame seed; no substitution.'});continue
 person,box=choice;folder=out/name;folder.mkdir();subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-i',str(video),'-vf',fr'select=between(n\,{start}\,{start+95})','-fps_mode','vfr','-frames:v','96','-q:v','2',str(folder/'%03d.jpg')],check=True)
 files=sorted(folder.glob('*.jpg'));assert len(files)==96;frames=[]
 for i,p in enumerate(files):
  data=p.read_bytes();frames.append({'sourceFrame':start+i,'image':base64.b64encode(data).decode(),'imageSha256':sha(data)})
 pack={'schema':'fraynix-observation-frames-037-v1','title':name,'source':lock['footage']+'; 96 consecutive source frames; first-frame ground annotation and 1750 mm height assumption only seed the tracker.','license':'EPFL CVLab research footage; all rights reserved; original media remains local, limited dated screenshots attributed.','clock':'source-frame','cameraKey':geometry['cameraKey'],'timingSource':'Original EPFL 25 fps source; sourceFrame/25 seconds','seconds':[(start+i)/25 for i in range(96)],'objects':[{'key':'subject','label':'human-guided ground-contact subject','box':box}],'frames':frames}
 (out/(name+'.fobs.json')).write_text(json.dumps(pack),encoding='utf8')
 refs=[]
 for i in range(1,96):
  cell=rows[start+i][person]
  if cell>=0:refs.append({'ordinal':i,'positionM':[v*.001 for v in ground(cell)],'projectedGroundImage':image(*ground(cell),0)})
 (out/(name+'.references.json')).write_text(json.dumps({'source':'Original EPFL ground grid, 250 mm cells; correlated references; seed excluded.','personColumn':person,'start':start,'references':refs},indent=2),encoding='utf8');records.append({'name':name,'start':start,'person':person,'firstBox':box,'packSha256':sha((out/(name+'.fobs.json')).read_bytes()),'references':len(refs)})
(out/'preparation.json').write_text(json.dumps({'sourceLock':lock,'originalSha256':sha(video.read_bytes()),'calibrationSha256':sha((cache/'terrace-tsai.original.zip').read_bytes()),'annotationsSha256':sha((cache/'gt_terrace1.original.txt').read_bytes()),'records':records,'decoder':subprocess.check_output(['ffmpeg','-version'],text=True).splitlines()[0]},indent=2),encoding='utf8');print(json.dumps(records))
