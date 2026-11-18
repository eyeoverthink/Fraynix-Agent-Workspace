from pathlib import Path
import json,hashlib,statistics
r=Path.cwd();names=['full-a','memo-a','memo-b','full-b'];runs={n:json.loads((r/f'build/study163-{n}/result.json').read_bytes())for n in names}
for n,s in runs.items():
 assert s['passed'] and len(s['tasks'])==4 and s['finalWorld']['revision']==4,n
 assert all(t['verified']['passed']and t['task']['status']=='completed'and t['afterRevision']==i+1 for i,t in enumerate(s['tasks'])),n
 assert s['coldDiagnostics']['entries']==0 and s['postColdDiagnostics']['fullReplays']>=3,n
 assert s['finalDiagnostics']['reused']>0 if s['mode']=='memo' else s['finalDiagnostics']['reused']==0,n
 assert s['originalFilesUnchanged']>0
def normalized(t):
 w=t['workflow'];before=w['before162'];mapping={before['id']:'scene',w['plan162']['sceneHash']:'scene-state',t['child']['id']:'job'}
 mapping.update({o['id']:o['sourceId']for o in before['objects']})
 def walk(v):
  if isinstance(v,dict):return {k:walk(x)for k,x in v.items()}
  if isinstance(v,list):return [walk(x)for x in v]
  if isinstance(v,str):
   for a,b in mapping.items():v=v.replace(a,b)
  return v
 return walk({'source':w['source162'],'before':before,'plan':w['plan162'],'run':t['child']['run'],'outcome':w['outcome162'],'observed':w['audit']['observed']})
reference=runs['full-a'];equal=[]
for n,s in runs.items():
 for i,t in enumerate(s['tasks']):
  a,b=normalized(reference['tasks'][i]),normalized(t)
  if a!=b:
   (r/'build/comparison163-difference.json').write_text(json.dumps({'run':n,'index':i,'reference':a,'actual':b},indent=2),encoding='utf8');raise AssertionError((n,i,'Source/plan/native/report equality failed'))
  equal.append({'run':n,'index':i,'sha256':hashlib.sha256(json.dumps(b,sort_keys=True,separators=(',',':')).encode()).hexdigest()})
rows=[]
for n,s in runs.items():
 for t in s['tasks']:rows.append({'run':n,'mode':s['mode'],'case':t['name'],'revision':t['afterRevision'],'prepareMs':t['preparationMs'],'executeMs':t['executionMs'],'archiveMs':t['archiveVerificationMs'],'totalMs':t['totalMs'],'restartMs':t.get('restartMs',0),'status':'completed'})
pairs=[]
for suffix in ['a','b']:
 f,m=runs['full-'+suffix],runs['memo-'+suffix];a=sum(t['totalMs']for t in f['tasks']);b=sum(t['totalMs']for t in m['tasks']);pairs.append({'pair':suffix,'fullMs':a,'memoMs':b,'savedMs':a-b,'savedPercent':100*(a-b)/a})
result={'passed':True,'runs':names,'completedWorkflows':16,'recordings':3,'physicalEpisodes':1,'generatedSequences':2,'historyDepth':4,'sourcePlanNativeAndActualReportEquality':equal,'identityNormalization':'Only freshly generated scene/object/job IDs and their scene-state hash are replaced with fixed aliases. All numerical source, uncertainty, native state, integration samples, forces, contacts, verification metrics and actual DOM observations compare exactly. No number or timing in the simulated trace is discarded.','pairs':pairs,'meanSavedPercent':statistics.mean(p['savedPercent']for p in pairs),'costRows':rows,'sessions':[{'run':n,'startupMs':s['startupMs'],'totalSessionMs':s['totalSessionMs'],'coldVerificationMs':s['coldVerificationMs'],'diagnostics':s['finalDiagnostics']}for n,s in runs.items()],'scope':'Two alternating pairs on the same corrected implementation and existing sources. Whole tasks include preparation, durable execution/skill saving and an additional original archive verification; deliberate restart is included in task4. Copying fixtures and post-study refusal/visual checks are excluded; cold verification reported separately. Small exposed study, not a population speed estimate.'}
(r/'build/comparison163.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps({'passed':True,'workflows':16,'pairs':pairs,'meanSavedPercent':result['meanSavedPercent']}))
