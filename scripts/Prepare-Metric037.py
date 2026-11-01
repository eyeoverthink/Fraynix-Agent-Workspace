"""Original EPFL footage -> clean pixel input, source timing, scorer-only reference.
Requires existing ffmpeg/ffprobe for research decoding; application does not.
Never installs a dependency. Reserved extraction requires a committed source lock.
"""
import argparse,base64,hashlib,json,math,pathlib,subprocess,urllib.request,zipfile,xml.etree.ElementTree as ET

ROOT=pathlib.Path(__file__).resolve().parent.parent
def sha(b):return hashlib.sha256(b).hexdigest()
def canonical(p):return sha(p.read_bytes().replace(b'\r\n',b'\n'))
def main():
 p=argparse.ArgumentParser();p.add_argument('--split',choices=['calibration','reserved'],required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
 protocol=json.loads((ROOT/'workspace-engine/src/main/resources/metric-037-contract.json').read_text(encoding='utf-8'))
 if a.split=='reserved':
  subprocess.run(['git','cat-file','-e','HEAD:docs/METRIC_037_CANDIDATE_LOCK.json'],cwd=ROOT,check=True)
  for n,h in json.loads((ROOT/'docs/METRIC_037_CANDIDATE_LOCK.json').read_text(encoding='utf-8'))['sources'].items():
   if canonical(ROOT/n)!=h:raise ValueError('Candidate source changed: '+n)
 def fetch(url,name):
  target=a.output/name
  if not target.exists():
   with urllib.request.urlopen(url,timeout=60) as r:target.write_bytes(r.read())
  return target
 video=fetch(protocol['footage'],'terrace1-c0.original.avi');cal=fetch(protocol['calibration'],'terrace-tsai.original.zip');gt=fetch(protocol['reference'],'gt_terrace1.original.txt')
 cam=ET.fromstring(zipfile.ZipFile(cal).read('terrace-tsai-c0.xml'));t={}
 for child in cam:
  for k,v in child.attrib.items():t[k]=float(v)
 t={k:t[k] for k in ['focal','kappa1','cx','cy','sx','dpx','dpy','tx','ty','tz','rx','ry','rz','width','height']};t['metresPerUnit']=.001
 geometry={'mode':'tsai','cameraKey':'epfl-terrace-c0-fixed','note':'EPFL terrace Tsai camera0; ground z=0, millimetre source convention interpreted as metres x0.001. Reference grid250mm; ground-contact estimate, not certified metrology.','anchorSigmaM':.125*math.sqrt(2),'scaleRelativeSigma':.01,'tsai':t}
 (a.output/'geometry.json').write_text(json.dumps(geometry,indent=2),encoding='utf-8')
 meta=json.loads(subprocess.check_output(['ffprobe','-v','quiet','-show_streams','-of','json',str(video)]));stream=meta['streams'][0]
 if stream['r_frame_rate']!='25/1':raise ValueError('Source metadata does not agree with declared25fps')
 # Reference parsing happens only for the selected split's predeclared windows.
 lines=gt.read_text(encoding='utf-8').splitlines();header=list(map(int,lines[1].split()));rows=[list(map(int,s.split())) for s in lines[2:] if s.strip()]
 if header[:5]!=[5010,9,30,44,25]:raise ValueError('Original reference metadata differs')
 cx,sx,cy,sy,cz,sz=math.cos(t['rx']),math.sin(t['rx']),math.cos(t['ry']),math.sin(t['ry']),math.cos(t['rz']),math.sin(t['rz'])
 R=[[cy*cz,cz*sx*sy-cx*sz,sx*sz+cx*cz*sy],[cy*sz,cx*cz+sx*sy*sz,cx*sy*sz-cz*sx],[-sy,cy*sx,cx*cy]]
 def ground(pos):return[((pos%30)+.5)*250-500,((pos//30)+.5)*250-1500]
 def image(x,y,z):
  v=[R[i][0]*x+R[i][1]*y+R[i][2]*z+t[k] for i,k in enumerate(['tx','ty','tz'])];xu=t['focal']*v[0]/v[2];yu=t['focal']*v[1]/v[2];ru=math.hypot(xu,yu);rd=ru
  for _ in range(30):rd-=(rd*(1+t['kappa1']*rd*rd)-ru)/(1+3*t['kappa1']*rd*rd)
  fac=rd/ru if ru else 1;return[(xu*fac*t['sx']/t['dpx']+t['cx'])/t['width'],(yu*fac/t['dpy']+t['cy'])/t['height']]
 results=[]
 for case in protocol['cases']:
  if case['split']!=a.split:continue
  choice=None
  for start in range(case['startInclusive'],case['endExclusive']-400,25):
   for person in range(9):
    if all(rows[f][person]>=0 for f in range(start,start+401,25)):choice=(start,person);break
   if choice:break
  if choice is None:raise ValueError('Declared case has no continuous reference; no substitution: '+case['name'])
  start,person=choice;folder=a.output/case['name'];folder.mkdir(exist_ok=True)
  subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-i',str(video),'-vf',fr'select=between(n\,{start}\,{start+400})*not(mod(n-{start}\,5))','-fps_mode','vfr','-frames:v','81','-q:v','2','-y',str(folder/'%03d.jpg')],check=True)
  files=sorted(folder.glob('*.jpg'))
  if len(files)!=81:raise ValueError('Incomplete declared footage')
  x,y=ground(rows[start][person]);foot=image(x,y,0);head=image(x,y,1750);left=image(x-250,y,0);right=image(x+250,y,0);width=max(.025,abs(right[0]-left[0]));box=[max(0,foot[0]-width/2),max(0,min(head[1],foot[1])),min(1,foot[0]+width/2),min(1,max(head[1],foot[1]))]
  frames=[]
  for i,f in enumerate(files):
   b=f.read_bytes();frames.append({'sourceFrame':start+5*i,'image':base64.b64encode(b).decode(),'imageSha256':sha(b)})
  pack={'schema':'fraynix-observation-frames-037-v1','title':'EPFL '+case['name'],'source':protocol['footage']+'; original Indeo5 decoded by existing ffmpeg; 25source fps, every5th frame; seeded only from first annotated ground point and1750mm height assumption','license':'EPFL CVLAB research footage; all rights reserved; original media stays private, source linked. Limited dated research screenshots attributed to EPFL.','clock':'source-frame','cameraKey':geometry['cameraKey'],'timingSource':'EPFL stated25fps, verified AVI metadata25/1; sourceFrame/25 seconds','seconds':[(start+5*i)/25 for i in range(81)],'objects':[{'key':'subject','label':'human-guided ground-contact subject','box':box}],'frames':frames}
  refs={'source':protocol['reference']+'; EPFL grid cell centres250mm; quantization ±125mm per axis; first seed excluded','case':case,'personColumn':person,'start':start,'references':[{'ordinal':i*5,'positionM':[v*.001 for v in ground(rows[start+25*i][person])]} for i in range(1,17)]}
  (a.output/(case['name']+'.fobs.json')).write_text(json.dumps(pack),encoding='utf-8');(a.output/(case['name']+'.references.json')).write_text(json.dumps(refs,indent=2),encoding='utf-8');results.append({'case':case,'start':start,'personColumn':person,'frames':81,'firstBox':box,'frameHashes':[f['imageSha256'] for f in frames]})
 result={'split':a.split,'originals':{p.name:{'bytes':p.stat().st_size,'sha256':sha(p.read_bytes())} for p in [video,cal,gt]},'decoder':subprocess.check_output(['ffmpeg','-version'],text=True).splitlines()[0],'cases':results}
 (a.output/('prepared-'+a.split+'.json')).write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps({'split':a.split,'cases':[{'name':c['case']['name'],'start':c['start'],'person':c['personColumn']} for c in results]}))
if __name__=='__main__':main()
