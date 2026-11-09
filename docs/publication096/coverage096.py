from pathlib import Path
import json,hashlib,xml.etree.ElementTree as ET,subprocess
r=Path.cwd()
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def suites(root):
 result=[]
 for p in sorted(root.rglob('TEST-*.xml')):
  x=ET.parse(p).getroot();cases=[]
  for c in x.findall('testcase'):
   error=c.find('failure');error=c.find('error') if error is None else error;skip=c.find('skipped')
   cases.append({'name':c.attrib['name'],'status':'failed' if error is not None else 'skipped' if skip is not None else 'passed','seconds':float(c.attrib.get('time',0)),'message':(error.get('message','') if error is not None else skip.get('message','') if skip is not None else '')})
  result.append({'name':x.attrib['name'],'tests':len(cases),'failed':sum(c['status']=='failed' for c in cases),'skipped':sum(c['status']=='skipped' for c in cases),'seconds':float(x.attrib['time']),'file':p.relative_to(r).as_posix(),'cases':cases,'skips':[dict(c,reason=c['message'] or ('Optional prior Electron recovery project not supplied; see original test assumption.' if x.attrib['name'].endswith(('RecoveryAudit093Test','RecoveryAudit094Test')) else 'JUnit XML does not include the reason; see original source.')) for c in cases if c['status']=='skipped']})
 return result
original=suites(r/'build/full096-results');follow=suites(r/'build/corrected096-results');assert original and follow
full=load(r/'build/full096-finished.json');correction=load(r/'build/corrected096-finished.json');assert full['exit']==1 and correction['exit']==0
expected={('fraymus.workspace.Payload057Test','explicitChatPayloadRequestCannotBeDropped'),('fraymus.workspace.Payload058PolicyTest','explicitRedesignAndUnfamiliarPayloadRequestPreservesRequirements'),('fraymus.workspace.Support059Test','nativeOptionsAreExplicitAndDisabledRedesignRefusesBothOffsets'),('fraymus.workspace.Support060Test','exactIndependentPolicyAndChatCannotBecomeAsymmetric')}
originalCases={(s['name'],c['name']):(s,c) for s in original for c in s['cases']};fixedCases={(s['name'],c['name']):(s,c) for s in follow for c in s['cases']}
assert len(originalCases)==sum(s['tests'] for s in original)
assert len(fixedCases)==sum(s['tests'] for s in follow)==37
assert {key for key,(s,c) in originalCases.items() if c['status']=='failed'}==expected
assert set(fixedCases)<=set(originalCases)
assert all(c['status']=='passed' for s,c in fixedCases.values())
assert expected<=set(fixedCases)
# Validate all compiled production bytes, including the original Studio, after the correction build.
before=load(r/'build/classes096-before-correction.json');after={}
for label,base in [('engine',r/'workspace-engine/build/classes/java/main'),('studio',r/'build/classes/java/main')]:
 after[label]={p.relative_to(base).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in base.rglob('*.class')}
assert before==after,'Production class bytes changed after full run: reassess/retest before merging coverage'
combined=[]
for suite in original:
 row={**suite};row['cases']=[]
 for case in suite['cases']:
  key=suite['name'],case['name'];used=fixedCases.get(key)
  row['cases'].append(dict(used[1] if used else case,evidenceFile=used[0]['file'] if used else suite['file'],source=correction['source'] if used else full['source'],correctedOriginalFailure=key in expected))
 row['failed']=sum(c['status']=='failed' for c in row['cases']);row['skipped']=sum(c['status']=='skipped' for c in row['cases'])
 row['originalSuiteSeconds']=suite['seconds'];row['seconds']=sum(c['seconds'] for c in row['cases'])
 combined.append(row)
result={'passed':True,'singleFullInvocationPassed':False,'scope':'Complete original method coverage combined with explicitly identified corrected-method reruns. This is not a clean single full invocation or a remote CI result. Only four test methods change; the334engine and166Studio classes match the full run byte for byte.','fullRun':full,'correctionRun':correction,'originalFailureMethods':[{'class':a,'method':b} for a,b in sorted(expected)],'originalClasses':original,'followupClasses':follow,'classes':combined,'tests':sum(s['tests'] for s in combined),'failures':sum(s['failed'] for s in combined),'skipped':sum(s['skipped'] for s in combined),'unchangedProductionClassCounts':{k:len(v) for k,v in before.items()}}
result['passedMethods']=result['tests']-result['skipped'];assert result['tests']>735 and result['failures']==0
result['timingScope']='Combined class seconds sum the selected original or rerun method durations; they are not elapsed time for a single invocation. Both original invocations retain their own times.'
(r/'build/coverage096.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps({k:v for k,v in result.items() if k not in ['originalClasses','followupClasses','classes']},indent=2))
