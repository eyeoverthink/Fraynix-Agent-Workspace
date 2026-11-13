from pathlib import Path
import json,hashlib,subprocess
r=Path.cwd();folder=r/'build/diagnostic130';lock=json.loads((r/'docs/OBSERVATION_130_LOCK.json').read_text())
for n,h in lock['sources'].items():assert hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,n
methods={};allrows={}
for method in lock['methods']:
 data=[json.loads((folder/f'held-{n}-{method}.json').read_text()) for n in lock['heldStarts']];scores=[s for d in data for s in d['scores']];errors=[s['errorM'] for s in scores if 'errorM'in s]
 methods[method]={'references':len(scores),'available':len(errors),'missing':len(scores)-len(errors),'withinOneMetre':sum(e<=1 for e in errors),'overTwoMetres':sum(e>2 for e in errors),'meanAcceptedErrorM':sum(errors)/len(errors),'trackerMs':sum(d['metrics']['trackerMs'] for d in data)}
 allrows[method]={(n,s['ordinal']):s for n,d in zip(lock['heldStarts'],data) for s in d['scores']}
a,b=methods['flow'],methods['object-flow'];g=lock['gate'];checks={'availability':b['available']>=g['minAcceptedFractionOfIncumbent']*a['available'],'meanError':b['meanAcceptedErrorM']<=g['maxMeanErrorRatio']*a['meanAcceptedErrorM'],'withinOneMetre':b['withinOneMetre']>a['withinOneMetre'],'severeErrors':b['overTwoMetres']<=a['overTwoMetres'],'allSeeds':len(json.loads((r/'build/footage130-held/preparation.json').read_text())['records'])==5}
pairs=[(v['errorM'],allrows['object-flow'][k]['errorM']) for k,v in allrows['flow'].items() if 'errorM'in v and 'errorM'in allrows['object-flow'][k]]
result={'methods':methods,'lockedGate':checks,'gatePassed':all(checks.values()),'defaultChanged':False,'pairedDiagnostic':{'references':len(pairs),'incumbentMeanM':sum(a for a,b in pairs)/len(pairs),'candidateMeanM':sum(b for a,b in pairs)/len(pairs),'scope':'Posthoc paired diagnostic, not a replacement for the predeclared gate; all missing positions remain in primary totals'},'limits':'Same-camera temporal windows. Candidate loses2/15 references vs1/15; no general object recognition, full occlusion recovery or probability guarantee. First exposed development attempt lost10/15; its source/results retained.'}
(folder/'decision.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe';cp='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;S:/Codex-Work/fraynix-workspace-edition129/workspace-engine/build/install/workspace-engine/lib/*'
for f in sorted(folder.glob('held-*.json')):
 out=folder/('audit-'+f.name)
 subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.AuditStudy130',str(f),str(out)],check=True)
summary={}
for method in lock['methods']:
 for policy in ['measurement','legacy-adjacent','explicit-budget-1m']:
  rows=[]
  for n in lock['heldStarts']:
   d=json.loads((folder/f'audit-held-{n}-{method}.json').read_text());rows.extend(d['measurementScores'] if policy=='measurement' else d['forecastScores'][policy])
  accepted=[x for x in rows if x['available']];summary[method+'/'+policy]={'references':len(rows),'available':len(accepted),'coveredCentre':sum(x['coveredCentre'] for x in accepted),'coveredIncludingReferenceRadius':sum(x['coveredIncludingReferenceRadius'] for x in accepted),'meanErrorM':sum(x['errorM'] for x in accepted)/len(accepted) if accepted else None,'meanRadiusM':sum(x['radiusM'] for x in accepted)/len(accepted) if accepted else None}
(folder/'coverage.json').write_text(json.dumps({'summary':summary,'scope':'Coverage only at sparse later reference timestamps. Both accepted availability and all-reference denominators reported. Not a validation at the full0.6second forecast horizon.'},indent=2),encoding='utf8');print(json.dumps(summary),flush=True)
