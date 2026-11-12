from pathlib import Path
import os,subprocess,time,json,hashlib
r=Path.cwd();folder=r/'build/launcher121-check';folder.mkdir(parents=True,exist_ok=True);env=os.environ.copy();env['FRAYNIX_WORKSPACE_PROJECT']=str(folder/'isolated-project');env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
source=folder/'Owner121.java';source.write_text('public class Owner121 { public static void main(String[] a)throws Exception { System.out.println("ready"); System.out.flush(); Thread.sleep(60000); } }',encoding='utf-8')
subprocess.run([env['JAVA_HOME']+'/bin/javac.exe','-d',str(folder),str(source)],check=True,env=env)
# Labelled launcher-only fixture: a live JVM referencing the installation, not a game outcome.
cp=str(folder)+os.pathsep+str(r/'build/install/Fraynix-Studio/lib/*');before={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (r/'build/install/Fraynix-Studio/lib').glob('*.jar')}
owner=subprocess.Popen([env['JAVA_HOME']+'/bin/java.exe','-cp',cp,'Owner121'],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,creationflags=subprocess.CREATE_NO_WINDOW)
try:
 assert owner.stdout.readline().strip()=='ready'
 p=subprocess.run(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File','scripts/Start-Agent-Workspace.ps1','-PrepareOnly','-Offline'],env=env,capture_output=True,text=True)
 (folder/'refused.log').write_text(p.stdout+p.stderr,encoding='utf-8');assert p.returncode!=0 and 'A Java process still uses this checkout installation' in p.stdout+p.stderr
 assert before=={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (r/'build/install/Fraynix-Studio/lib').glob('*.jar')}
 assert owner.poll() is None
 (folder/'result.json').write_text(json.dumps({'passed':True,'scope':'Labelled live-JVM installation-owner fixture. Launcher refuses a build for a different project while its installation is in use; original JAR bytes preserved. Owner stopped by this test only.','ownerPid':owner.pid,'jarCount':len(before)},indent=2),encoding='utf-8');print('Launcher live-owner refusal and original JAR preservation passed')
finally:owner.terminate();owner.wait(timeout=15)
