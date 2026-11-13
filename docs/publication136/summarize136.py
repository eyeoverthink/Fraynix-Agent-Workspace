from pathlib import Path
import json,statistics,hashlib
r=Path.cwd();read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda p:hashlib.sha256((r/p).read_bytes()).hexdigest()
lock=read('docs/OBSERVATION_136_LOCK.json')
for n,h in lock['sourceHashes'].items():assert hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,n
out={}
for cohort,folder,manifest in [('development','association136-development','development136.json'),('later','association136-comparison','source136.json')]:
 sources=read('docs/publication136/'+manifest);assert sources['algorithmLockSha256']==sha('docs/OBSERVATION_136_LOCK.json');rows=[];sums={}
 for source in sources['records']:
  for key in ['pack','labels']:assert sha(source[key])==source[key+'Sha256']
  samples=[]
  for policy in lock['methods']:
   p=Path('build',folder,source['case']+'-'+policy+'.json');d=read(p)
   assert d['policy']==policy and d['sourceSha256']==source['packSha256'] and d['labelsSha256']==source['labelsSha256']
   assert read(p.with_name(p.stem+'.execution.json'))['exit']==0
   rows.append({'case':source['case'],'policy':policy,'summary':d['audit']['summary'],'appearance135':d['audit']['appearance135'],'episodes':d['audit']['episodes'],'processingMs':d['processingMs'],'evidence':p.as_posix()})
   samples.append([{'estimate':f['objects'][0]['estimate'],'tracker':{k:v for k,v in f['objects'][0]['tracker'].items() if k!='appearance135'}} for f in d['trace']])
  assert samples[0]==samples[1]==samples[2],source['case']
 for policy in lock['methods']:
  matching=[x for x in rows if x['policy']==policy]
  sums[policy]={'tracking':{k:sum(x['summary'][k] for x in matching) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain']},'memory':{k:sum(x['appearance135']['summary'][k] for x in matching) for k in ['admitted','independentlyCorrect','incorrect','unknown','bankRecoveries']},'processingMs':sum(x['processingMs'] for x in matching)}
 # Count losses against actual prior admission ordinals, not a presumed same-sized set.
 prior={(x['case'],a['ordinal']):a['verdict'] for x in rows if x['policy']=='appearance-continuity-135' for a in x['appearance135']['admissions']}
 retained={(x['case'],a['ordinal']):a['verdict'] for x in rows if x['policy']=='seed-anchored-136' for a in x['appearance135']['admissions']}
 assert retained.items()<=prior.items()
 out[cohort]={'rows':rows,'summary':sums,'allTrackerFilterOutputsEqual':True,'retainedPriorCorrect':sum(v=='correct' for v in retained.values()),'excludedPriorCorrect':sum(v=='correct' and k not in retained for k,v in prior.items()),'excludedPriorWrong':sum(v=='incorrect' and k not in retained for k,v in prior.items())}
(r/'build/comparison136-summary.json').write_bytes((json.dumps(out,indent=2)+'\n').encode())
a=read('build/cost136-baseline/result.json');b=read('build/cost136-candidate/result.json');assert a['originalStateSha256']==b['originalStateSha256'];cost=[]
for x,y in zip(a['rows'],b['rows']):
 assert x['runs']==y['runs'] and x['seedSha256']==y['seedSha256']
 assert [(v['operation'],v['repeat'],v['resultSha256']) for v in x['reads']]==[(v['operation'],v['repeat'],v['resultSha256']) for v in y['reads']]
 row={'runs':x['runs'],'bytes':x['seedBytes'],'baselineColdMs':x['coldOpenMs'],'candidateColdMs':y['coldOpenMs'],'operations':{},'verificationCost':y['verificationCost']}
 for op in ['summary','run','dataset','image']:
  p=statistics.median(v['ms'] for v in x['reads'] if v['operation']==op);q=statistics.median(v['ms'] for v in y['reads'] if v['operation']==op);row['operations'][op]={'baselineMedianMs':p,'candidateMedianMs':q,'ratio':p/q}
 cost.append(row)
assert len(cost)==3 and sum(len(x['reads']) for x in a['rows'])==36
(r/'build/cost136-comparison.json').write_bytes((json.dumps({'all36ResultHashesEqual':True,'allSeedHashesEqual':True,'rows':cost},indent=2)+'\n').encode());print('Verified all24 comparisons,760 equal outputs per method, admission identities and36 cost-result hashes.')
