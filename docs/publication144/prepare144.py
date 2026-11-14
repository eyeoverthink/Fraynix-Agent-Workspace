"""Run only after freeze. Reused camera regression and fixed new renderer cases stay separate."""
from pathlib import Path
import json,hashlib,datetime,shutil
r=Path.cwd();out=r/'docs/publication144';build=r/'build/sources144';build.mkdir(exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest();read=lambda p:json.loads(p.read_text(encoding='utf8'))
lock=r/'docs/OBSERVATION_144_LOCK.json'
for n,h in read(lock)['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
old=Path('J:/Codex-Work/fraynix-workspace-edition143');previous=read(r/'docs/publication143/source143.json');records=[];annotations=[]
for item in previous['records']:
 if item['group']!='faceocc1':continue
 row=dict(item);source=old/item['pack'];assert sha(source.read_bytes())==item['packSha256'];p=build/source.name;shutil.copy2(source,p)
 lab=read(r/item['labels']);lab['exposure']='tracker-results-seen';lab['provenance']+=' Reused exposed edition143 source regression; not fresh footage or new qualification.'
 lp=out/(row['case']+'-labels.json');lp.write_bytes((json.dumps(lab,indent=2)+'\n').encode());row.update(domain='real-camera-regression',pack=p.relative_to(r).as_posix(),labels=lp.relative_to(r).as_posix(),labelsSha256=sha(lp.read_bytes()));records.append(row)
 gt=r/item['originalGroundTruth'];entry={'path':item['originalGroundTruth'],'bytes':gt.stat().st_size,'sha256':sha(gt.read_bytes()),'source':'Unmodified original143 annotation bytes; exposed regression'}
 if entry not in annotations:annotations.append(entry)
generated=r/'build/generated144'
for item in read(generated/'generated-index.json'):
 row=dict(item);p=generated/(row['case']+'.fobs.json');lp=out/(row['case']+'-labels.json');shutil.copy2(generated/lp.name,lp);lab=read(lp)
 row.update(name=row['case'],target=1,start=1,end=24,stride=1,width=320,height=240,sourceFrames=list(range(1,25)),pack=p.relative_to(r).as_posix(),packSha256=sha(p.read_bytes()),labels=lp.relative_to(r).as_posix(),labelsSha256=sha(lp.read_bytes()),visibilityCounts={v:sum(l['visibility']==v for l in lab['rows']) for v in ['visible','partial','occluded','out-of-view','uncertain']},distractorLabelledSamples=sum('otherBoxes' in l for l in lab['rows']));records.append(row)
for group in {x['group'] for x in records}:
 donor=next(x for x in records if x['group']==group and x['role']=='reference-source');hashes={f['imageSha256'] for f in read(r/donor['pack'])['frames']}
 for target in [x for x in records if x['group']==group and x['role']=='evaluation']:assert not hashes.intersection(f['imageSha256'] for f in read(r/target['pack'])['frames']),(group,'Source overlap')
assert [f['imageSha256'] for f in read(generated/'clean-full.fobs.json')['frames']]==[f['imageSha256'] for f in read(generated/'unseen-copy.fobs.json')['frames']]
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha(lock.read_bytes()),'records':records,'downloads':[],'sourceReview':'FaceOcc1 exact edition143 source images and labels reused as exposed real-camera regression. Fresh generated renderer labels precede predictions. clean-full and unseen-copy deliberately share exact pixels; their different identities expose a limit of observation, not independent statistical trials.','limitations':['No fresh camera qualification.','Generated cases do not establish physical accuracy.','Paired identical pixels cannot determine hidden identity.','No outcome-driven method changes or source replacement.']}
(out/'source144.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode());(out/'annotation-integrity144.json').write_bytes((json.dumps(annotations,indent=2)+'\n').encode());print(json.dumps([{'case':x['case'],'domain':x['domain'],'counts':x['visibilityCounts']} for x in records]))
