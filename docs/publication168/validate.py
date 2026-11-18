from pathlib import Path
import subprocess,os,json,shutil,xml.etree.ElementTree as E

r=Path.cwd();env=dict(os.environ,JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot')
env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
classes=['OriginalGameContentTest','OriginalCoursesTest','EmbeddedGameTest','GameLevels114Test','GameLevelRuntime114Test',
    'GameTask115Test','GameReference115Test','GameReference116Test',
    'SmallObjectRoutesTest','WorkspaceServerBoundaryTest']
out=r/'build/validation168';out.mkdir(exist_ok=True)
def run(args,name,cwd=r):
 with (r/'build'/name).open('wb') as log:subprocess.run(args,cwd=cwd,env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
def collect(folder,destination):
 destination.mkdir(parents=True,exist_ok=True);count=0
 for p in folder.glob('*.xml'):
  e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0,p
  count+=int(e.get('tests'));shutil.copy2(p,destination/p.name)
 return count
run([str(r/'gradlew.bat'),'--offline','--max-workers=1',':test'],'root168-tests.log')
root_count=collect(r/'build/test-results/test',out/'root')
args=[str(r/'gradlew.bat'),'--offline','--max-workers=1',':workspace-engine:test']
for c in classes:args+=['--tests','fraymus.workspace.'+c]
run(args,'workspace168-tests.log')
workspace_count=collect(r/'workspace-engine/build/test-results/test',out/'workspace')
run(['npm.cmd','test'],'desktop168-tests.log',r/'workspace-desktop')
run(['python','-m','unittest','discover','-s','scripts','-p','test_*archive.py','-v'],'archive168-tests.log')
run([str(r/'gradlew.bat'),'--offline','--max-workers=1',':workspace-engine:jar','--rerun',':workspace-engine:installDist','--rerun'],'install168.log')
result={'passed':True,'rootJava':root_count,'workspaceJava':workspace_count,'classes':classes,
    'scope':'Full root-engine suite plus selected integrated-game, source verification and server regressions.'}
(out/'summary.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
