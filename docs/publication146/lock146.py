"""Freeze source, evaluation procedure and future generator before fresh pixel access."""
from pathlib import Path
import json,hashlib,subprocess,datetime
r=Path.cwd();out=r/'docs/publication146';sha=lambda b:hashlib.sha256(b).hexdigest()
prior=json.loads((r/'docs/OBSERVATION_145_LOCK.json').read_text(encoding='utf8'))
names=set(prior['sourceHashes'])
names.update(['docs/EDITION_146_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/ReturnContinuity146.java','workspace-engine/src/main/java/fraymus/workspace/ReturnAudit146.java','workspace-engine/src/test/java/fraymus/workspace/ReturnContinuity146Test.java','workspace-engine/src/test/java/fraymus/workspace/ReturnFixtures146.java','workspace-desktop/test/return146-integration.cjs','docs/publication146/restart146.cjs'])
assert json.loads((r/'build/focused146-first.json').read_text(encoding='utf8'))['exit']==0
selection=json.loads((r/'docs/publication145/source-selection145.json').read_text(encoding='utf8'))
source={'recordedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'selectedBeforeImageAccess':True,'priorNames':sorted(set(selection['priorNames']+[x['name'] for x in selection['selected']])),'sourcePage':'https://rvlab.snu.ac.kr/research/tracker_benchmark/','orderedCandidates':[{'name':n,'archive':a} for n,a in [('Coke','1nGsvt_IImvA4iJD22Qzm9jFPJxi-AudA'),('Subway','1mjWj04H2mxTkzWxoeNC284oVJiYqKK1E'),('Woman','14AcX4BpCar8iAMlm9Eg55uB64q_oaYu9'),('Human4','1DnZEER4mjBW-OM0TL97Sa6vN6MGdoBvs')]],'rule':'Earliest eligible complete disappearance/return from source-only review; fixed criteria in EDITION_146_CONTRACT.md. First two eligible sources in order, or explicit exhaustion. No outcomes influence selection. Donor later refused by tracking does not trigger substitution.'}
for x in source['orderedCandidates']:assert x['name'] not in source['priorNames']
(out/'source-selection146.json').write_text(json.dumps(source,indent=2)+'\n',encoding='utf8');names.add('docs/publication146/source-selection146.json')
unchanged=['TrajectoryRecovery145.java','TrajectoryGate145.java','TrajectorySearch145.java','TrajectoryAudit145.java','AmbiguityGuard144.java','AmbiguityAudit144.java','AppearanceConfirmation143.java','ReferenceSearch143.java','ForegroundGuard139.java','FlowTracker.java','ObservationFilter.java']
for name in unchanged:
 path='workspace-engine/src/main/java/fraymus/workspace/'+name
 expected=subprocess.check_output(['git','show','672894ea:'+path]);assert expected.replace(b'\r\n',b'\n')==(r/path).read_bytes().replace(b'\r\n',b'\n'),name
(r/'build/defaults146.json').write_text(json.dumps({'passed':True,'parent':'672894ea','unchangedIncumbents':unchanged,'default':'legacy-flow'},indent=2)+'\n',encoding='utf8')
lock={'edition':146,'lockedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'parent':'672894eab948f372cfa368ce7168dc667951c4eb','methods':['trajectory-guard-145','return-monitor-146','return-guard-146'],'sourceHashes':{n:sha((r/n).read_bytes().replace(b'\r\n',b'\n')) for n in sorted(names)},'noFreshPixelsOrLabelsAccessed':True,'sourceSelection':'Source-only event selection by predeclared criteria; commit independent labels before running predictions.'}
(r/'docs/OBSERVATION_146_LOCK.json').write_text(json.dumps(lock,indent=2)+'\n',encoding='utf8');print(json.dumps({'locked':len(names),'defaultPreserved':True}))
