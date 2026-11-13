from pathlib import Path
import json,xml.etree.ElementTree as ET,shutil,subprocess,os,time
r=Path.cwd();folder=r/'build/validation136';attempt=folder/'broad-first';assert not attempt.exists();shutil.copytree(r/'workspace-engine/build/test-results/test',attempt)
fails=[]
for p in attempt.glob('*.xml'):
 e=ET.parse(p).getroot()
 for t in e.findall('testcase'):
  if t.find('failure') is not None or t.find('error') is not None:fails.append(t.get('classname')+'.'+t.get('name'))
assert fails==['fraymus.workspace.ObservationCache136Test.callerBindingsCannotPoisonCachedDurableStateAndRestartRevalidates'],fails
assert json.loads((r/'build/java136-final.json').read_text())['exit']!=0
env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';start=time.monotonic()
with (r/'build/cache136-restart-fix.log').open('wb') as out:p=subprocess.run(['cmd','/c','gradlew.bat',':workspace-engine:test','--tests','fraymus.workspace.ObservationCache136Test','--console=plain','--max-workers=2'],env=env,stdout=out,stderr=subprocess.STDOUT)
receipt={'exit':p.returncode,'seconds':time.monotonic()-start,'priorFailure':fails,'scope':'Only corrected cache class rerun; every other passing broad result retained. No production change.'};(r/'build/cache136-restart-fix.json').write_text(json.dumps(receipt,indent=2),encoding='utf8')
if p.returncode:raise SystemExit(p.returncode)
shutil.copytree(r/'workspace-engine/build/test-results/test',folder/'cache-restart-fix')
final=folder/'final';final.mkdir(exist_ok=False)
for p in attempt.glob('TEST-*.xml'):shutil.copy2(p,final/p.name)
for p in (folder/'cache-restart-fix').glob('TEST-*.xml'):shutil.copy2(p,final/p.name)
checks=[]
for p in final.glob('TEST-*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0
 checks += [t.get('classname')+'.'+t.get('name') for t in e.findall('testcase') if t.find('skipped') is None]
receipt.update({'exit':0,'distinctChecks':len(set(checks))});(r/'build/java136-combined.json').write_text(json.dumps(receipt,indent=2),encoding='utf8');print(json.dumps(receipt))
