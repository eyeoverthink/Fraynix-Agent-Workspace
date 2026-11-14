"""Fixed separated sources; source-only visibility must exist before predictions."""
from pathlib import Path
from PIL import Image
import io,json,hashlib,zipfile,base64,re,datetime
r=Path.cwd();folder=r/'build/otb145';out=r/'docs/publication145';sha=lambda b:hashlib.sha256(b).hexdigest()
lock=r/'docs/OBSERVATION_145_LOCK.json';review=json.loads((out/'visibility145.json').read_text(encoding='utf8'));records=[];annotations=[]
for name in ['Basketball','Girl2']:
 z=zipfile.ZipFile(folder/(name+'.zip'));images=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and '/img/' in n and '__MACOSX' not in n);assert len(images)>=(504 if name=='Girl2' else 144)
 gtbytes=(out/(name+'-groundtruth.txt')).read_bytes();ground=[[float(v) for v in re.split(r'[,\s]+',line.strip())] for line in gtbytes.decode('utf-8-sig').splitlines() if line.strip()];w,h=Image.open(io.BytesIO(z.read(images[0]))).size
 annotations.append({'path':f'docs/publication145/{name}-groundtruth.txt','sha256':sha(gtbytes),'bytes':len(gtbytes),'source':'Exact original annotation ZIP bytes; CRC verified and archive digest retained'})
 def box(i):
  x,y,bw,bh=ground[i-1];b=[max(0,x/w),max(0,y/h),min(1,(x+bw)/w),min(1,(y+bh)/h)];assert b[2]-b[0]>=.025 and b[3]-b[1]>=.025,(name,i,b);return b
 for role,start,end in [('reference-source',1,24),('evaluation',385 if name=='Girl2' else 25,504 if name=='Girl2' else 144)]:
  case=name.lower()+'-'+str(start);frames=[];labels=[]
  for ordinal,i in enumerate(range(start,end+1)):
   raw=z.read(images[i-1]);digest=sha(raw);frames.append({'sourceFrame':i,'image':base64.b64encode(raw).decode(),'imageSha256':digest})
   if ordinal==0:continue
   v=next((x['visibility'] for x in review[name]['ranges'] if x['start']<=i<=x['end']),None);assert v,(name,i)
   l={'ordinal':ordinal,'sourceFrame':i,'imageSha256':digest,'visibility':v,'note':'External benchmark rectangle; source-only AI visibility review, not human adjudication.'}
   if v in ['visible','partial']:l['targetBox']=box(i)
   if str(i) in review[name].get('otherBoxes',{}):l['otherBoxes']=review[name]['otherBoxes'][str(i)];l['note']+=' Other-object boxes independently marked from source pixels before predictions.'
   labels.append(l)
  pack={'schema':'fraynix-observation-frames-036-v1','title':case+' / real-camera145 '+role,'source':'OTB / https://rvlab.snu.ac.kr/research/tracker_benchmark/ ; exact original JPEGs','license':'Public research benchmark; authors retain rights. Source archive not redistributed.','clock':'source-frame','objects':[{'key':'subject','label':name+' target','box':box(start)}],'frames':frames}
  p=folder/(case+'.fobs.json');p.write_bytes(json.dumps(pack).encode());assert p.stat().st_size<20_000_000
  lab={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'OTB authors (rectangles); Codex source-only AI (visibility and marked competitors)','method':'external-annotations','exposure':'before-tracking','provenance':'Original benchmark geometry with separate source-only visibility review; no independent human adjudication. Predictions never guide labels.','rows':labels};lp=out/(case+'-labels.json');lp.write_bytes((json.dumps(lab,indent=2)+'\n').encode())
  records.append({'case':case,'group':name.lower(),'name':name,'target':1,'role':role,'domain':'real-camera','start':start,'end':end,'stride':1,'frames':len(frames),'width':w,'height':h,'sourceFrames':list(range(start,end+1)),'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':{v:sum(l['visibility']==v for l in labels) for v in ['visible','partial','occluded','out-of-view','uncertain']},'distractorLabelledSamples':sum('otherBoxes' in l for l in labels),'originalGroundTruth':annotations[-1]['path'],'originalGroundTruthSha256':sha(gtbytes)})
generated=r/'build/generated145'
for row in json.loads((generated/'generated-index.json').read_text(encoding='utf8')):
 p=generated/(row['case']+'.fobs.json');lp=out/(row['case']+'-labels.json');lp.write_bytes((generated/lp.name).read_bytes());lab=json.loads(lp.read_text(encoding='utf8'));row.update(name=row['case'],target=1,start=1,end=24,stride=1,width=320,height=240,sourceFrames=list(range(1,25)),pack=p.relative_to(r).as_posix(),packSha256=sha(p.read_bytes()),labels=lp.relative_to(r).as_posix(),labelsSha256=sha(lp.read_bytes()),visibilityCounts={v:sum(l['visibility']==v for l in lab['rows']) for v in ['visible','partial','occluded','out-of-view','uncertain']},distractorLabelledSamples=sum('otherBoxes' in l for l in lab['rows']));records.append(row)
# Exact pixel separation is also checked by the Java resolver before every use.
for group in {x['group'] for x in records}:
 donor=next(x for x in records if x['group']==group and x['role']=='reference-source');donorHashes={f['imageSha256'] for f in json.loads((r/donor['pack']).read_text(encoding='utf8'))['frames']}
 for target in [x for x in records if x['group']==group and x['role']=='evaluation']:assert not donorHashes.intersection(f['imageSha256'] for f in json.loads((r/target['pack']).read_text(encoding='utf8'))['frames']),(group,'Source image overlap')
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha(lock.read_bytes()),'records':records,'downloads':json.loads((folder/'download.json').read_text(encoding='utf8')),'sourceReview':review['summary'],'limitations':['Bounded source windows, not a full benchmark.','External geometry and AI source-only visibility/competitor review, not independent human adjudication.','Generated occlusion and copy controls are separate from real-camera evidence.','Same-object declaration is not semantic identity proof; partial geometry remains inferred.']}
(out/'annotation-integrity145.json').write_text(json.dumps(annotations,indent=2)+'\n',encoding='utf8');(out/'source145.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode());print(json.dumps([{'case':x['case'],'role':x['role'],'counts':x['visibilityCounts']} for x in records]))
