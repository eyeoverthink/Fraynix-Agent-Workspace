from pathlib import Path
import json,hashlib,zipfile,base64,datetime,re,io
from PIL import Image
r=Path.cwd();folder=r/'build/otb136';out=r/'docs/publication136';sha=lambda b:hashlib.sha256(b).hexdigest();lock=json.loads((r/'docs/OBSERVATION_136_LOCK.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
selection=json.loads((folder/'window-selection.json').read_text());records=[]
for name,start,end,reason in selection['windows']:
 z=zipfile.ZipFile(Path('S:/Codex-Work/fraynix-workspace-edition135/build/otb135')/(name+'.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and not n.startswith('__MACOSX/') and not Path(n).name.startswith('._'));w,h=Image.open(io.BytesIO(z.read(files[0]))).size
 gt=next(n for n in z.namelist() if 'groundtruth' in n and not n.startswith('__MACOSX/'));raw=z.read(gt);assert (r/'docs/publication135'/(name+'-groundtruth_rect.txt')).read_bytes()==raw;boxes=[list(map(float,re.split(r'[,\s]+',l))) for l in raw.decode().strip().splitlines()]
 def box(i):
  x,y,bw,bh=boxes[i-1];b=[max(0,(x-1)/w),max(0,(y-1)/h),min(1,(x-1+bw)/w),min(1,(y-1+bh)/h)];assert b[2]-b[0]>=.025 and b[3]-b[1]>=.025,(name,i,b);return b
 def visibility(i):
  if name=='Tiger2':return 'partial' if i>=165 else 'visible'
  if name=='Box':return 'partial' if i<=541 else 'visible'
  if name=='David3':return 'partial' if 183<=i<=191 or 234<=i<=245 else 'visible'
  return 'visible'
 frames=[];rows=[]
 for ordinal,n in enumerate(range(start,end+1)):
  raw=z.read(files[n-1]);frames.append({'sourceFrame':n,'image':base64.b64encode(raw).decode(),'imageSha256':sha(raw)})
  if not ordinal:continue
  state=visibility(n);note={'visible':'Target visible in source image; external benchmark box.','partial':'Target remains visible despite partial cover, haze or clipping; external benchmark box.','occluded':'Rendered cloud fully obscures the target; no visible target box.','uncertain':'Faint rendered transition; source-only reviewer cannot establish visibility confidently. No invented visible box.'}[state]
  row={'ordinal':ordinal,'sourceFrame':n,'imageSha256':sha(raw),'visibility':state,'note':note}
  if state in ['visible','partial']:row['targetBox']=box(n)
  rows.append(row)
 case=name.lower()+'-'+str(start);pack={'schema':'fraynix-observation-frames-036-v1','title':'OTB '+case+' '+reason,'source':'Visual Tracker Benchmark / Yi Wu, Ming-Hsuan Yang, Jongwoo Lim; https://rvlab.snu.ac.kr/research/tracker_benchmark; consecutive original JPEG bytes. '+('Rendered/animated source, not real-world camera footage.' if name=='Bird1' else 'Real camera recording.'),'license':'Public research benchmark; original authors/source retain rights. Local research; no raw JPEG/ZIP redistribution. Limited attributed app screenshots.','clock':'source-frame','objects':[{'key':'subject','label':'Original benchmark target','box':box(start)}],'frames':frames}
 p=folder/(case+'.fobs.json');assert not p.exists();p.write_text(json.dumps(pack),encoding='utf8')
 labels={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'OTB authors (boxes); Codex source-only AI (visibility)','method':'external-annotations','exposure':'before-tracking','provenance':'External original OTB boxes converted from one-based XYWH to normalized corners. Only the first source box initializes tracking. Every selected image reviewed source-only after algorithm/scorer lock, before predictions. AI visibility is not independent human adjudication. No labelled distractors supplied. '+reason+'. No real full out-of-frame return. Rendered Bird1 transitions remain uncertain; existing scorer does not bridge uncertain intervals into recovery delay.','rows':rows}
 lp=out/(case+'-labels.json');assert not lp.exists();lp.write_text(json.dumps(labels,indent=2)+'\n',encoding='utf8');counts={s:sum(x['visibility']==s for x in rows) for s in ['visible','partial','occluded','out-of-view','uncertain']}
 records.append({'case':case,'name':name,'start':start,'end':end,'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'frames':len(frames),'stride':1,'width':w,'height':h,'visible':counts['visible']+counts['partial'],'hidden':counts['occluded'],'partial':counts['partial'],'uncertain':counts['uncertain'],'visibilityCounts':counts,'domain':'rendered' if name=='Bird1' else 'real-camera','reason':reason,'originalGroundTruth':gt})
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha((r/'docs/OBSERVATION_136_LOCK.json').read_bytes()),'downloads':json.loads((r/'docs/publication135/source135.json').read_text())['downloads'],'records':records,'sourceReview':'All384 predeclared later-window original images inspected in source-only crops before suffix predictions. External boxes never came from tracker output. Tiger2 leaf cover, early Box cover and David3 tree/post cover remain partial; rendered Bird1 remains visible. No complete occlusion or out-of-frame return in these later windows. The earlier135 development windows retain the rendered cloud disappearance. Recordings were already source-reviewed; these are new temporal predictions, not independent unseen videos.','limitations':['AI source visibility without independent human adjudication.','No independent distractor boxes; zero confirmed switches cannot establish no switching.','These later windows contain partial cover, not complete disappearance; no new full occlusion or out-of-frame return measurement.','Partial-cover camera tasks and rendered hidden task must be reported separately.']}
(out/'source136.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8');print(json.dumps(records))
