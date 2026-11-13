from pathlib import Path
import json,hashlib,zipfile,base64,datetime,re,io
from PIL import Image
r=Path.cwd();folder=r/'build/otb134';out=r/'docs/publication134';sha=lambda b:hashlib.sha256(b).hexdigest()
lock=json.loads((r/'docs/OBSERVATION_134_LOCK.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h
records=[]
for name,stride,last in [('Jogging',4,305),('Lemming',5,476)]:
 z=zipfile.ZipFile(folder/(name+'.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg'));width,height=Image.open(io.BytesIO(z.read(files[0]))).size
 names=sorted(n for n in z.namelist() if 'groundtruth' in n);ground=[]
 for n in names:
  raw=z.read(n);dest=out/(name+'-'+Path(n).name);assert not dest.exists();dest.write_bytes(raw);ground.append([list(map(float,re.split(r'[,\s]+',line.strip()))) for line in raw.decode('utf8').strip().splitlines()])
 indices=list(range(1,last+1,stride));frames=[]
 for i in indices:
  raw=z.read(files[i-1]);frames.append({'sourceFrame':i,'image':base64.b64encode(raw).decode(),'imageSha256':sha(raw)})
 def box(target,frame):
  x,y,w,h=ground[target][frame-1];return [max(0,(x-1)/width),max(0,(y-1)/height),min(1,(x-1+w)/width),min(1,(y-1+h)/height)]
 def visibility(target,ordinal):
  if name=='Jogging':
   if target==0 and ordinal==18:return 'occluded'
   if target==0 and ordinal in [17,19]:return 'partial'
   if target==1 and ordinal in [11,12,13,14]:return 'partial'
  elif ordinal in [47,48,53,*range(64,74),88,89,90]:return 'partial'
  return 'visible'
 for target in range(len(ground)):
  case=name.lower()+'-'+str(target+1);pack={'schema':'fraynix-observation-frames-036-v1','title':'OTB '+case+' independent association evaluation','source':'Visual Tracker Benchmark / Yi Wu, Ming-Hsuan Yang, Jongwoo Lim; https://rvlab.snu.ac.kr/research/tracker_benchmark; original JPEG bytes, stride '+str(stride),'license':'Public research benchmark; original authors/source retain rights. Local evaluation only; no raw video/JPEG archive redistribution. Limited attributed app screenshots.','clock':'source-frame','objects':[{'key':'subject','label':'Original benchmark target '+str(target+1),'box':box(target,1)}],'frames':frames}
  p=folder/(case+'.fobs.json');assert not p.exists();p.write_text(json.dumps(pack),encoding='utf8');rows=[]
  for ordinal,frame in enumerate(frames):
   if not ordinal:continue
   state=visibility(target,ordinal);row={'ordinal':ordinal,'sourceFrame':frame['sourceFrame'],'imageSha256':frame['imageSha256'],'visibility':state,'note':('Target fully hidden behind pole; no visible target box.' if state=='occluded' else 'Source-only review: target partly covered or clipped.' if state=='partial' else 'Source-only review: target visible. Independent benchmark target box, not tracker output.')}
   if state in ['visible','partial']:row['targetBox']=box(target,frame['sourceFrame'])
   if name=='Jogging' and visibility(1-target,ordinal) in ['visible','partial']:row['otherBoxes']=[box(1-target,frame['sourceFrame'])]
   rows.append(row)
  labels={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'OTB authors (boxes); Codex source-only AI (visibility)','method':'external-annotations','exposure':'before-tracking','provenance':'Original OTB target boxes; first-image box alone initializes tracker. Source-only AI visibility review before any evaluation outputs, with external target/other boxes overlaid for identity. No independent human visibility adjudication. MATLAB one-based XYWH converted to normalized corners, clipped to image. Full Jogging temporal extent sampled every4 images; first476 Lemming images sampled every5. Small visible remnants count partial, not fully occluded.','rows':rows}
  lp=out/(case+'-labels.json');assert not lp.exists();lp.write_text(json.dumps(labels,indent=2)+'\n',encoding='utf8')
  records.append({'case':case,'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'originalGroundTruth':names[target],'width':width,'height':height,'frames':len(frames),'stride':stride,'visible':sum(x['visibility'] in ['visible','partial'] for x in rows),'hidden':sum(x['visibility']=='occluded' for x in rows),'partial':sum(x['visibility']=='partial' for x in rows)})
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha((r/'docs/OBSERVATION_134_LOCK.json').read_bytes()),'downloads':json.loads((folder/'download.json').read_text(encoding='utf8')),'records':records,'sourceReview':'All173 selected original images inspected in source-only sheets, external boxes only. Pole frames53/73 and Lemming336..361 additionally inspected. Lemming retains a visible eye fragment: partial, not full occlusion. One fully hidden sampled Jogging target1 frame. No actual complete out-of-frame return in this chosen set; synthetic controls test large returns. Both Jogging targets share a recording. Not benchmark leaderboard results or general identity certification.'}
(out/'source134.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8');print(json.dumps(records))
