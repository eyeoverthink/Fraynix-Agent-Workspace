from pathlib import Path
import os,subprocess,time,json,hashlib
r=Path.cwd();folder=r/'build/launcher121-check';folder.mkdir(parents=True,exist_ok=True);env=os.environ.copy();env['FRAYNIX_WORKSPACE_PROJECT']=str(folder/'isolated-project');env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
# Real stopped-owner update regression: install the prior120 game JAR into this isolated
# installation, then require normal PrepareOnly to restore the freshly built121 bytes.
launch=['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File','scripts/Start-Agent-Workspace.ps1','-PrepareOnly','-Offline']
preflight=subprocess.run(launch,env=env,capture_output=True,text=True);assert preflight.returncode==0,preflight.stdout+preflight.stderr
built=next((r/'build/libs').glob('Fraynix-Agent-Workspace-*.jar'));installed=r/'build/install/Fraynix-Studio/lib'/built.name;current=installed.read_bytes();assert current==built.read_bytes()
old=Path('S:/Codex-Work/fraynix-workspace-edition120/build/install/Fraynix-Studio/lib')/built.name;older=old.read_bytes();assert older!=current
try:
 installed.write_bytes(older)
 restored=subprocess.run(launch,env=env,capture_output=True,text=True)
 (folder/'stale-update.log').write_text(restored.stdout+restored.stderr,encoding='utf-8');assert restored.returncode==0,restored.stdout+restored.stderr
 assert installed.read_bytes()==current
finally:
 if installed.read_bytes()!=current:installed.write_bytes(current)
staleHash=hashlib.sha256(older).hexdigest();restoredHash=hashlib.sha256(current).hexdigest()
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
 (folder/'result.json').write_text(json.dumps({'passed':True,'scope':'Labelled live-JVM installation-owner fixture. Launcher refuses a build for a different project while its installation is in use; original JAR bytes preserved. Owner stopped by this test only.','ownerPid':owner.pid,'jarCount':len(before),'stoppedOwnerUpdate':True,'prior120JarSha256':staleHash,'restored121JarSha256':restoredHash},indent=2),encoding='utf-8');print('Launcher live-owner refusal and original JAR preservation passed')
finally:owner.terminate();owner.wait(timeout=15)
