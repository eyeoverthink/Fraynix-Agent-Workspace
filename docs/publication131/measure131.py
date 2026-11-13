from pathlib import Path
import json,hashlib,subprocess,sys
r=Path.cwd();folder=r/'build/diagnostic131';prefix='held';outputPrefix='';lock=json.loads((r/'docs/OBSERVATION_131_LOCK.json').read_text());methodsList=['original130-flow']+lock['methods']
for n,h in lock['sources'].items():assert hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,n
methods={};allrows={};failures=[]
def load(n,method):
 p=folder/f'{prefix}-{n}-{method}.json'
 if p.exists():return json.loads(p.read_text())
 failure=json.loads(p.with_suffix('.execution.json').read_text());failures.append({'start':n,'method':method,'failure':failure})
 refs=json.loads((r/'build/footage131-held'/f'held-{n}.references.json').read_text())['references']
 return {'scores':[{'ordinal':x['ordinal'],'referenceM':x['positionM'],'accepted':False} for x in refs],'metrics':{'trackerMs':0},'executionFailed':True}

for method in methodsList:
 data=[load(n,method) for n in lock['heldStarts']];scores=[s for d in data for s in d['scores']];errors=[s['errorM'] for s in scores if 'errorM'in s]
 methods[method]={'references':len(scores),'available':len(errors),'missing':len(scores)-len(errors),'withinOneMetre':sum(e<=1 for e in errors),'overTwoMetres':sum(e>2 for e in errors),'meanAcceptedErrorM':sum(errors)/len(errors) if errors else None,'trackerMs':sum(d['metrics']['trackerMs'] for d in data)}
 allrows[method]={(n,s['ordinal']):s for n,d in zip(lock['heldStarts'],data) for s in d['scores']}
a,b=methods['flow'],methods['renewing-flow'];g=lock['gate'];checks={'availability':b['available']>=g['minAcceptedFractionOfIncumbent']*a['available'],'meanError':b['meanAcceptedErrorM'] is not None and a['meanAcceptedErrorM'] is not None and b['meanAcceptedErrorM']<=g['maxMeanErrorRatio']*a['meanAcceptedErrorM'],'withinOneMetre':b['withinOneMetre']>a['withinOneMetre'],'severeErrors':b['overTwoMetres']<=a['overTwoMetres'],'allSeeds':all('refused' not in x for x in json.loads((r/'build/footage131-held/preparation.json').read_text())['records'])}
pairs=[(v['errorM'],allrows['renewing-flow'][k]['errorM']) for k,v in allrows['flow'].items() if 'errorM'in v and 'errorM'in allrows['renewing-flow'][k]]
result={'methods':methods,'lockedGate':checks,'gatePassed':all(checks.values()) and not any(x['method'] in lock['methods'] for x in failures),'executionFailures':failures,'defaultChanged':False,'qualificationEligible':True,'sourceLock':'docs/OBSERVATION_131_LOCK.json','pairedDiagnostic':{'references':len(pairs),'incumbentMeanM':sum(a for a,b in pairs)/len(pairs) if pairs else None,'candidateMeanM':sum(b for a,b in pairs)/len(pairs) if pairs else None,'scope':'Posthoc paired diagnostic, not a replacement for the predeclared gate; all missing positions remain in primary totals'},'limits':'New camera1 pixels of shared event/people; not independent trajectories; completed accepted-only errors must be read alongside all missing references and execution failures. Failed execution contributes no invented partial trace or zero-cost timing claim; summed timing covers completed executions only. No general object recognition, full occlusion recovery or probability guarantee.'}
(folder/(outputPrefix+'decision.json')).write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
summary={}
for method in methodsList:
 for policy in ['measurement','legacy-adjacent','explicit-budget-1m']:
  rows=[]
  for n in lock['heldStarts']:
   p=folder/f'audit-{prefix}-{n}-{method}.json'
   if not p.exists():
    assert (folder/f'{prefix}-{n}-{method}.execution.json').exists();rows.extend([{'available':False} for _ in json.loads((r/'build/footage131-held'/f'held-{n}.references.json').read_text())['references']]);continue
   d=json.loads(p.read_text());rows.extend(d['measurementScores'] if policy=='measurement' else d['forecastScores'][policy])
  accepted=[x for x in rows if x['available']];summary[method+'/'+policy]={'references':len(rows),'available':len(accepted),'coveredCentre':sum(x['coveredCentre'] for x in accepted),'coveredIncludingReferenceRadius':sum(x['coveredIncludingReferenceRadius'] for x in accepted),'meanErrorM':sum(x['errorM'] for x in accepted)/len(accepted) if accepted else None,'meanRadiusM':sum(x['radiusM'] for x in accepted)/len(accepted) if accepted else None}
(folder/(outputPrefix+'coverage.json')).write_text(json.dumps({'summary':summary,'scope':'Coverage only at sparse later reference timestamps. Both accepted availability and all-reference denominators reported. Not a validation at the full0.6second forecast horizon.'},indent=2),encoding='utf8');print(json.dumps(summary),flush=True)

diag=[]
for n in lock['heldStarts']:
 refs=json.loads((r/'build/footage131-held'/f'held-{n}.references.json').read_text())['references']
 for method in methodsList:
  execution=json.loads((folder/f'held-{n}-{method}.execution.json').read_text());p=folder/f'held-{n}-{method}.json'
  d=json.loads(p.read_text()) if p.exists() else None
  diag.append({'start':n,'method':method,'execution':execution,'references':len(refs),'projectedGroundPointsInsideImage':sum(all(0<=v<=1 for v in x['projectedGroundImage']) for x in refs),'acceptedFrames':d['metrics']['accepted'] if d else None,'invalidBoxRejections':sum(x['tracker'].get('motion',{}).get('invalidBoxRejected',False) for x in d['rows']) if d else None,'reacquiredFrames':[x['ordinal'] for x in d['rows'] if x['tracker']['status']=='reacquired'] if d else [],'twoImageRecoveries':[x['ordinal'] for x in d['rows'] if x['tracker'].get('motion',{}).get('method')=='two-frame-recovery'] if d else []})
(folder/'diagnostics.json').write_text(json.dumps({'rows':diag,'scope':'Descriptive only. Projected ground points inside image do not establish visible object identity or absence of occlusion; primary reference counts unchanged. Wall time includes startup, loading, tracking and scoring; trackerMs is a narrower stage.'},indent=2),encoding='utf8')
