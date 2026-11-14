"""Capacity-only protocol correction before evaluation; never changes pixels or labels."""
from pathlib import Path
import json,copy,hashlib,datetime,shutil
r=Path.cwd();out=r/'docs/publication145';sha=lambda b:hashlib.sha256(b).hexdigest()
original=json.loads((out/'source145.json').read_text(encoding='utf8'));assert all(x['frames']==120 for x in original['records'] if x['role']=='evaluation' and x['domain']=='real-camera')
study=json.loads((r/'build/trajectory145-app/result.json').read_text(encoding='utf8'));assert all(x['role']=='reference-source' for x in study['runs']);assert '5..96' in study['error']
shutil.copy2(out/'source145.json',out/'initial-source145.json');shutil.copy2(r/'build/trajectory145-app/result.json',r/'build/trajectory145-capacity-failure.json');shutil.copy2(r/'build/trajectory145-app.log',r/'build/trajectory145-capacity-failure.log')
main=copy.deepcopy(original);tail=copy.deepcopy(original);main['records']=[];tail['records']=[];changes=[]
for row in original['records']:
 if row['domain']!='real-camera' or row['role']!='evaluation':
  main['records'].append(row)
  if row['domain']=='real-camera':tail['records'].append(row)
  continue
 pack=json.loads((r/row['pack']).read_text(encoding='utf8'));labels=json.loads((r/row['labels']).read_text(encoding='utf8'));scored=[]
 for start,end,dest in [(0,96,main),(95,120,tail)]:
  sub=copy.deepcopy(row);sub['start']=row['start']+start;sub['end']=row['start']+end-1;sub['frames']=end-start;sub['case']=row['group']+'-'+str(sub['start']);sub['sourceFrames']=list(range(sub['start'],sub['end']+1))
  subpack=copy.deepcopy(pack);subpack['frames']=pack['frames'][start:end];subpack['title']=sub['case']+' / unchanged fixed-window capacity partition'
  if start:subpack['objects'][0]['box']=next(x['targetBox'] for x in labels['rows'] if x['ordinal']==start)
  lab=copy.deepcopy(labels);lab['rows']=[dict(x,ordinal=x['ordinal']-start) for x in labels['rows'] if start<x['ordinal']<end];scored.extend(x['sourceFrame'] for x in lab['rows'])
  p=r/'build/otb145'/(sub['case']+'-bounded.fobs.json');p.write_bytes(json.dumps(subpack).encode());lp=out/(sub['case']+'-bounded-labels.json');lp.write_bytes((json.dumps(lab,indent=2)+'\n').encode())
  sub.update(pack=p.relative_to(r).as_posix(),packSha256=sha(p.read_bytes()),labels=lp.relative_to(r).as_posix(),labelsSha256=sha(lp.read_bytes()),visibilityCounts={v:sum(x['visibility']==v for x in lab['rows']) for v in row['visibilityCounts']},distractorLabelledSamples=sum('otherBoxes' in x for x in lab['rows']),partitionOf=row['case'],originalPackSha256=row['packSha256'],originalLabelsSha256=row['labelsSha256'])
  dest['records'].append(sub);changes.append({'case':sub['case'],'first':sub['start'],'last':sub['end'],'frames':sub['frames'],'seedFrame':sub['start'],'scored':len(lab['rows'])})
 assert scored==[x['sourceFrame'] for x in labels['rows']]
for obj in [main,tail]:obj['capacityAmendment']='docs/publication145/capacity-amendment145.json'
(out/'source145.json').write_bytes((json.dumps(main,indent=2)+'\n').encode());(out/'source145-tail.json').write_bytes((json.dumps(tail,indent=2)+'\n').encode())
amendment={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'reason':'The first app attempt refused a120-frame pack at the existing96-frame import limit after three donor runs; zero evaluation runs had started. Original failure and manifest retained.','unchanged':['Production code, scorer, original labels, source pixels and lock','All selected source transitions, no omission or replacement','Existing96-frame,24-run and16-dataset limits','Every verification check'],'change':'Partition each fixed120-frame camera interval into96 and25 frames, overlapping one boundary image as the second seed. This supplies fresh ground-truth seed geometry at the boundary; not uninterrupted120-frame tracking. Extra camera chunks use a separate isolated project, identical methods and their own donor verification. All119 planned scored transitions remain exactly once per method when references are available.','partitions':changes,'firstAttemptOnlyDonors':True,'originalManifestSha256':sha((out/'initial-source145.json').read_bytes()),'freshEvaluationNotStarted':True}
(out/'capacity-amendment145.json').write_text(json.dumps(amendment,indent=2)+'\n',encoding='utf8');print(json.dumps(amendment))
