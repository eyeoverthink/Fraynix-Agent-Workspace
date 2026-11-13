from pathlib import Path
import os,subprocess,time,shutil,json,sys
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('JAVA_TOOL_OPTIONS',None);env.pop('GALLIUM_DRIVER',None)
folder=r/'build/validation135';folder.mkdir(parents=True,exist_ok=True)
def run(name,args,cwd=r):
 start=time.monotonic()
 for suffix in ['log','json']:
  prior=r/'build'/f'{name}.{suffix}'
  if prior.exists():
   archived=folder/'prior-attempts';archived.mkdir(exist_ok=True);shutil.copy2(prior,archived/(str(time.time_ns())+'-'+prior.name))
 with (r/'build'/f'{name}.log').open('wb') as out:p=subprocess.run(args,cwd=cwd,env=env,stdout=out,stderr=subprocess.STDOUT)
 result={'name':name,'exit':p.returncode,'seconds':time.monotonic()-start};(r/'build'/f'{name}.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
group=sys.argv[1]
if group=='java':
 run('kernel135-final',['cmd','/c','gradlew.bat',':factory-kernel:verifyIntegration','--console=plain','--max-workers=2'])
 tests=['AppearanceFlow135Test','AppearanceAudit135Test','IdentityRecovery134Test','AssociationAudit134Test','AssociationContact133Test','ContactAudit133Test','ObservationLabels132Test','Tracking131Test','FlowTrackerTest','PhysicalAudit130Test','Tracking130Test','ObservationForecast129Test','Tracking129Test','ObservationArtifact128Test','MetricObservationStoreTest','WorldObservationStoreTest','ObservationFilterTest','PlaneCalibrationTest','ProcedureLifecycle127Test','ProcedureGraph127Test','SkillGraph046Test','GoalRunnerTest','MotionDataTest','CreationTest','ProceduralTest','WorkflowTest','DataSummary104Test','Qualification105Test','Archive110Test','GoalArchive111Test','ChatArchive112Test','Agency043Test','Domain044Test','CreationPlanner044Test','BrowserOutcome126Test','WitnessRecovery126Test']
 args=['cmd','/c','gradlew.bat',':workspace-engine:test']
 for t in tests:args+=['--tests','fraymus.workspace.'+t]
 run('java135-final',args+['--console=plain','--max-workers=2'])
 shutil.copytree(r/'workspace-engine/build/test-results/test',folder/'final',dirs_exist_ok=False)
elif group=='export':
 run('verified-export135',['cmd','/c','gradlew.bat',':workspace-engine:test','--tests','fraymus.workspace.VerifiedAppearances135Test','--tests','fraymus.workspace.AppearanceAudit135Test','--console=plain','--max-workers=2'])
 shutil.copytree(r/'workspace-engine/build/test-results/test',folder/'verified-export-final',dirs_exist_ok=False)
elif group=='desktop':
 run('desktop135-final',['cmd','/c','npm.cmd','test'],r/'workspace-desktop')
 env['AGENCY043_OUTPUT']=str(r/'build/agency135-evidence')
 run('agency135-regression',['node','workspace-desktop/test/agency043-integration.cjs'])
elif group=='agency':
 env['AGENCY043_OUTPUT']=str(r/'build/agency135-evidence')
 run('agency135-regression',['node','workspace-desktop/test/agency043-integration.cjs'])
elif group=='package':
 env['FRAYNIX_WORKSPACE_PROJECT']=str(r/'build/launcher135-isolated-project')
 run('launcher135',['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File','scripts/Start-Agent-Workspace.ps1','-PrepareOnly','-Offline'])
 env.pop('FRAYNIX_WORKSPACE_PROJECT',None)
 run('package135',['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'])
 run('stamp135',['node','scripts/Study-Build095.cjs','--stamp'])
 run('receipt135',['node','scripts/Study-Build095.cjs'])
else:raise ValueError('Unknown validation group')
