from pathlib import Path
import json,hashlib,zipfile,base64,re,io,datetime
from PIL import Image
r=Path.cwd();folder=r/'build/otb138';out=r/'docs/publication138';sha=lambda b:hashlib.sha256(b).hexdigest();lock=json.loads((r/'docs/OBSERVATION_138_LOCK.json').read_text())
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
records=[]
specs=[('Skating2',0,1,96,1),('Skating2',1,1,96,1),('Suv',0,498,593,1),('Liquor',0,276,486,3)]
for name,target,start,end,stride in specs:
 z=zipfile.ZipFile(folder/(name+'.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and not n.startswith('__MACOSX/') and not Path(n).name.startswith('._'))
 names=sorted(n for n in z.namelist() if 'groundtruth' in n and not n.startswith('__MACOSX/'));ground=[]
 for n in names:ground.append([list(map(float,re.split(r'[,\s]+',s.strip()))) for s in z.read(n).decode().strip().splitlines()])
 width,height=Image.open(io.BytesIO(z.read(files[0]))).size
 def box(t,n):
  x,y,w,h=ground[t][n-1];return [max(0,(x-1)/width),max(0,(y-1)/height),min(1,(x-1+w)/width),min(1,(y-1+h)/height)]
 def visibility(t,n):
  if name=='Skating2':
   if t==0:
    if n in [83,84]:return 'uncertain'
    return 'visible' if n<=15 or 54<=n<=64 else 'partial'
   return 'visible' if n>=56 else 'partial'
  if name=='Suv':
   if n==518:return 'occluded'
   if 531<=n<=538 or 554<=n<=563:return 'uncertain'
   return 'visible' if n<=502 or n>=572 else 'partial'
  if n in [291,294]:return 'uncertain'
  return 'partial' if 297<=n<=465 else 'visible'
 frames=[];rows=[]
 for ordinal,n in enumerate(range(start,end+1,stride)):
  raw=z.read(files[n-1]);frame={'sourceFrame':n,'image':base64.b64encode(raw).decode(),'imageSha256':sha(raw)};frames.append(frame)
  if ordinal==0:continue
  v=visibility(target,n)
  note=('Source-only visibility uncertain; no invented target rectangle.' if v=='uncertain' else 'Target fully hidden by the dense tree; no visible target rectangle.' if v=='occluded' else 'Source-only target visible with partial body/hand/tree cover or image-edge clipping.' if v=='partial' else 'Source-only target visible. Original independent benchmark rectangle.')
  row={'ordinal':ordinal,'sourceFrame':n,'imageSha256':frame['imageSha256'],'visibility':v,'note':note}
  if v in ['visible','partial']:row['targetBox']=box(target,n)
  if name=='Skating2' and visibility(1-target,n) in ['visible','partial']:row['otherBoxes']=[box(1-target,n)]
  if name=='Liquor' and (n-276)%21==0:
   row['otherBoxes']=[[.21,.37,.35,.76],[.55,.40,.68,.72]]
   row['note']+=' Two visible other bottles manually boxed from source-only grid review; coarse AI boxes, about5 source pixels allowance; not human adjudication.'
  rows.append(row)
 case=name.lower()+'-'+str(target+1)+'-'+str(start)
 pack={'schema':'fraynix-observation-frames-036-v1','title':'Fresh138 '+case+' original camera evidence','source':'Visual Tracker Benchmark / Yi Wu, Ming-Hsuan Yang, Jongwoo Lim; https://rvlab.snu.ac.kr/research/tracker_benchmark; original JPEGs, stride '+str(stride),'license':'Public research benchmark; source authors retain rights. Local evaluation, no raw archive redistribution; limited attributed screenshots.','clock':'source-frame','objects':[{'key':'subject','label':'OTB '+name+' target '+str(target+1),'box':box(target,start)}],'frames':frames}
 p=folder/(case+'.fobs.json');assert not p.exists();p.write_text(json.dumps(pack),encoding='utf8')
 provenance='Original external OTB target boxes, MATLAB one-based XYWH converted to normalized clipped corners. Source-only AI visibility reviewed after method lock and before predictions; no independent human adjudication. Skating2 reciprocal other boxes are external annotations for the other target. Liquor other boxes are coarse source-only AI annotations on10 later sampled images only; other images lack independent distractor labels. Missing distractor labels cannot prove no switches. No tracker outputs used. Source note138 explains crossing fallback and bottle cohort addition.'
 labels={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'OTB authors (target/skater boxes); Codex source-only AI (visibility/bottle others)','method':'external-annotations','exposure':'before-tracking','provenance':provenance,'rows':rows}
 lp=out/(case+'-labels.json');assert not lp.exists();lp.write_text(json.dumps(labels,indent=2)+'\n',encoding='utf8')
 counts={v:sum(x['visibility']==v for x in rows) for v in ['visible','partial','occluded','out-of-view','uncertain']}
 records.append({'case':case,'name':name,'target':target+1,'start':start,'end':end,'stride':stride,'frames':len(frames),'width':width,'height':height,'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':counts,'distractorLabelledSamples':sum(bool(x.get('otherBoxes')) for x in rows),'originalGroundTruth':names[target],'domain':'real-camera','exposure':'New recording for this project; source reviewed after method lock, before predictions.'})
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha((r/'docs/OBSERVATION_138_LOCK.json').read_bytes()),'downloads':json.loads((folder/'download.json').read_text()),'records':records,'sourceReview':'Complete selected source frames inspected in original-pixel detail sheets before predictions: Skating2 first96 images for two targets, Suv498..593, Liquor276..486 stride3. Whole-sequence overviews reviewed for context. Critical Suv516..519/533..534 and681 inspected without boxes;518 is hidden, ambiguous leaf-covered cases remain uncertain. Skating2 body/limb fragments remain partial;83/84 target1 uncertain. Liquor cap cover lasts beyond96 images, hence stride3; target partly clips at image edge, never fully disappears. Two other bottle boxes manually checked on11 grid-review images, seed excluded from scores. Source-only AI judgments, not human-metrology ground truth. No fresh tracker outputs inspected before labels frozen.','limitations':['Three recordings, four target windows; the two skaters share one recording.','One confidently fully hidden Suv sample; no general complete-occlusion reliability claim.','Different-clothing skater crossing, similar-shaped amber bottles with different labels, and synthetic identical twins are distinct difficulty levels.','Coarse AI bottle-other boxes on10 later frames only; zero confirmed switches does not prove identity continuity.','Liquor stride3 skips intervening frames; no temporal completeness claim.','Visibility and bottle-other annotations lack independent human adjudication.']}
(out/'source138.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8');print(json.dumps(records))
