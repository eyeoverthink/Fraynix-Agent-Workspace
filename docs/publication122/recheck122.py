from pathlib import Path
import os,subprocess,json
r=Path.cwd();jdk=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot');dest=r/'build/recheck122';dest.mkdir(parents=True,exist_ok=True)
jars=list((r/'workspace-engine/build/install/workspace-engine/lib').glob('json-*.jar'));assert len(jars)==1
cp=os.pathsep.join([str(r/'workspace-engine/build/classes/java/main'),str(jars[0]),str(dest)])
subprocess.run([str(jdk/'bin/javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(dest),'docs/publication122/Recheck122.java'],check=True)
folders=[r/'workspace-engine/build'/n for n in ['controller122-baseline','controller122-development','controller122-reserved','controller122-mesa-reserved']]+sorted((r/'workspace-engine/build').glob('controller122-old*'))
raw=subprocess.check_output([str(jdk/'bin/java.exe'),'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.Recheck122',*[str(p) for p in folders]],text=True)
d=json.loads(raw);assert d['passed'];(r/'build/receipt-recheck122.json').write_text(json.dumps(d,indent=2),encoding='utf-8');print(raw)
