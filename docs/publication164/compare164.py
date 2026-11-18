from pathlib import Path
import json,hashlib,statistics,math
r=Path.cwd();names=['on-a','off-a','off-b','on-b'];runs={n:json.loads((r/f'build/study164-{n}/result.json').read_bytes())for n in names}
def normalized(t):
 if t['kind']=='summary':
  audit=t['workflow']['audit']
  return {'kind':'summary','csv':t['dataset']['csv'],'sourceSha256':t['dataset']['sourceSha256'],'mapping':t['dataset']['mapping'],'metrics':t['workflow']['metrics'],'audit':{k:audit[k]for k in ['passed','rowCount','frames','errors','sourceSha256','summaries102','maxTraceError102']},'code':{k:t['artifacts'][1]['contents'][k]for k in ['index.html','summary.js']}}
 w=t['workflow'];before=w['before162'];mapping={before['id']:'scene',w['plan162']['sceneHash']:'scene-state',t['child']['id']:'job'};mapping.update({o['id']:o['sourceId']for o in before['objects']})
 def walk(v):
  if isinstance(v,dict):return {k:walk(x)for k,x in v.items()}
  if isinstance(v,list):return [walk(x)for x in v]
  if isinstance(v,str):
   for a,b in mapping.items():v=v.replace(a,b)
  return v
 return walk({'kind':'motion','source':w['source162'],'before':before,'plan':w['plan162'],'run':t['child']['run'],'outcome':w['outcome162'],'observed':w['audit']['observed']})
equal=[];costs=[];probes=[];diagnostics=[]
for n,s in runs.items():
 assert s['passed']and len(s['tasks'])==10 and s['finalWorld']['revision']==8,n
 assert [p['revision']for p in s['probes']]==[2,4,6,8]
 assert len([t for t in s['tasks']if 'beforeRestart'in t])==2
 for i,t in enumerate(s['tasks']):
  assert t['task']['status']=='completed'and t['task']['timing052']['modelCalls']==0 and t['verified']['passed']
  a,b=normalized(runs['on-a']['tasks'][i]),normalized(t)
  if a!=b:
   (r/'build/comparison164-difference.json').write_text(json.dumps({'run':n,'index':i,'reference':a,'actual':b},indent=2),encoding='utf8');raise AssertionError((n,i,'Numeric/native/actual report comparison differs'))
  equal.append({'run':n,'index':i,'sha256':hashlib.sha256(json.dumps(b,sort_keys=True,separators=(',',':')).encode()).hexdigest()})
  costs.append({'run':n,'mode':s['mode'],'case':t['name'],'kind':t['kind'],'historyDepth':t.get('afterRevision',t.get('historyDepth')),'prepareMs':t['preparationMs'],'executeMs':t['executionMs'],'archiveMs':t['archiveVerificationMs'],'totalMs':t['totalMs'],'restartMs':t.get('restartMs',0)})
  segments=[(t['beforeDiagnostics'],t['beforeRestart']),({d:{'epoch':t['afterRestart'][d]['epoch'],'totals':{}}for d in ['java','browser']},t['afterDiagnostics'])]if 'beforeRestart'in t else [(t['beforeDiagnostics'],t['afterDiagnostics'])]
  for before,after in segments:
   for domain in ['java','browser']:
    assert before[domain]['epoch']==after[domain]['epoch']
    for kind,v in after[domain]['totals'].items():
     prior=before[domain]['totals'].get(kind,{})
     row={'run':n,'case':t['name'],'domain':domain,'epoch':after[domain]['epoch'],'operation':kind}
     row.update({k:v[k]-prior.get(k,0)for k in ['count','failed','inclusiveMs','exclusiveMs']});assert all(math.isfinite(row[k])and row[k]>=0 for k in ['count','failed','inclusiveMs','exclusiveMs']);diagnostics.append(row)
  assert t['afterDiagnostics']['java']['enabled']==(s['mode']=='on')
  if s['mode']=='off':assert t['afterDiagnostics']['java']['operations']==t['afterDiagnostics']['browser']['operations']==0
 for p in s['probes']:
  assert p['passed']and len(p['rows'])==p['revision'] and p['nativeTicks']>0
  probes.append({'run':n,**{k:p[k]for k in ['revision','beforeOriginalCheckMs','afterOriginalCheckMs','nativeReplayMs','reportSourceAuditMs','totalMs','wholeProbeMs','encodedReportBytes','nativeTicks']}})
 assert s['coldDiagnostics']['java']['operations']==0 and s['coldDiagnostics']['recording']['entries']==0
 assert s['coldDiagnostics']['browser']['epoch']!=s['finalDiagnostics']['browser']['epoch']
 assert s['originalFilesUnchanged']>0
pairs=[]
for suffix in ['a','b']:
 on=sum(t['totalMs']for t in runs['on-'+suffix]['tasks']);off=sum(t['totalMs']for t in runs['off-'+suffix]['tasks']);pairs.append({'pair':suffix,'instrumentedMs':on,'disabledMs':off,'differenceMs':on-off,'overheadPercent':100*(on-off)/off})
result={'passed':True,'runs':names,'workflows':40,'motionWorkflows':32,'summaryWorkflows':8,'historyDepth':8,'pairs':pairs,'meanOverheadPercent':statistics.mean(p['overheadPercent']for p in pairs),'costRows':costs,'probeRows':probes,'diagnosticRows':diagnostics,'exactComparisons':equal,'scope':'Two alternating pairs on the same build. Task totals include preparation, execution, skill saving when requested and extra archive verification. Two interruptions included per session. Probe measurements are repeated read-only computation outside task totals. Whole session totals include probes and diagnostics. Inclusive diagnostic spans overlap; do not add Java and browser clocks. Small exposed local study, not a population latency guarantee.','comparisonScope':'Motion: exact sources, uncertainty, plans, full native traces, outcomes and actual observed reports after only fresh scene/object/job identifiers and scene hash normalization. Data: exact fresh CSV/mapping, verified metrics, actual cells and summaries in four states, SVG points and shipped HTML/JavaScript. Fresh artifact identifiers are independently verified by the original workflow; they are not treated as comparable content.','sessions':[{'run':n,'startupMs':s['startupMs'],'totalSessionMs':s['totalSessionMs'],'coldVerificationMs':s['coldVerificationMs']}for n,s in runs.items()]}
(r/'build/comparison164.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps({'passed':True,'workflows':40,'pairs':pairs}))
