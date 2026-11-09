from pathlib import Path
import json,gzip,base64,hashlib,math
r=Path.cwd();manifest=json.loads((r/'build/ui096-attempts.json').read_text());base=r/manifest['accepted'];record=json.loads((base/'result.json').read_text(encoding='utf8'))
assert record['passed'] and not record['errors']
candidates=record['review']['counterfactual045']['candidates'];assert len(candidates)==3
assert [c['status'] for c in candidates].count('completed')==1 and [c['status'] for c in candidates].count('not-needed')==2
attempts=[]
for name in ['review-originals.json','built-originals.json']:
 bundle=json.loads((base/name).read_text(encoding='utf8'));assert len(bundle['originalAttempts'])==1
 packs=[('forecast',a['run']) for a in bundle['originalAttempts']]
 if name=='built-originals.json':packs.append(('actual',bundle['actualRun']))
 for kind,p in packs:
  raw=gzip.decompress(base64.b64decode(p['gzipBase64']));assert len(raw)==p['rawBytes'] and hashlib.sha256(raw).hexdigest()==p['rawSha256']
  run=json.loads(raw);assert run['definition']==bundle['study']['source'];ticks=0;effort=0
  for sample in run['samples']:
   for row in sample['integration']:
    ticks+=1;assert row[0]==ticks;effort+=math.hypot(*row[2])/240
  assert ticks>0
  attempts.append({'file':name,'kind':kind,'id':run['id'],'ticks':ticks,'effortNs':effort,'rawSha256':p['rawSha256']})
b=record['built'];actuals=[a for a in attempts if a['kind']=='actual'];assert len(actuals)==1 and actuals[0]['id']==b['runId'] and actuals[0]['id']!=attempts[0]['id'];assert b['status']=='completed' and b['directFirst055'] and b['worldCommit']['revision']==1
for key in ['metrics','constructionVerification051','poseView','counterfactual045View']:assert b[key]['passed']
assert b['counterfactual045']['comparison']['passed'] and record['artifact']['test']['passed']
skill=next(x for x in record['skills'] if x['name']=='verified-bridge096');assert skill['sourceGoalId']==record['goalId'] and skill['defaults']['design053'] and skill['defaults']['forceLimitN']==40
checks=['Three declared alternatives; one actual direct-first trial succeeded, two explicitly not needed. Original source bytes and integration rows independently reconstructed. Review leaves the world unchanged.',*record['checks'][1:]]
result={'passed':True,'source':record['source'],'scope':'Independent audit of the accepted fresh app run; no source or outcome substitution.','descriptionCorrection':'The original harness first-check sentence retained older three-actual-trial wording. Its own native records show one successful direct-first trial and two not-needed alternatives. The sentence is corrected for future runs; original result.json remains unchanged.','declaredCandidates':3,'executedReviewCandidates':1,'notNeededCandidates':2,'nativeRecords':attempts,'checks':checks}
(r/'build/ui096-audit.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps({k:v for k,v in result.items() if k!='nativeRecords'}))
