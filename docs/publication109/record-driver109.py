from pathlib import Path
import json,hashlib,subprocess,xml.etree.ElementTree as ET
r=Path.cwd();tests=[]
for p in (r/'workspace-engine/build/test-results/test').glob('TEST-*.xml'):
 e=ET.parse(p).getroot();tests.append(e.attrib);assert all(e.attrib[k]=='0' for k in ['failures','errors','skipped'])
assert sum(int(x['tests']) for x in tests)==23
identity=json.loads((r/'build/source-identity109.json').read_text())
root=r/'workspace-engine/build/classes/java/main'
actual={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*.class')}
assert actual==identity['classes']
diff=subprocess.check_output(['git','diff','028e2451','--','workspace-engine/src/test/java/fraymus/workspace/Qualification105Test.java','workspace-engine/src/test/java/fraymus/workspace/Qualification108Test.java']).decode('utf-8');(r/'build/test-driver109.diff').write_text(diff,encoding='utf-8',newline='\n')
value={'passed':True,'initialMethods':126,'initialPassed':125,'initialFailures':1,'initialFailure':'Test driver acknowledged selected audit before observing its stop predicate; expected running, observed completed, before source mutation.','affectedMethodsPassed':23,'affectedClasses':[t['name'] for t in tests],'unchangedProductionClasses':len(actual),'scope':'Only lifecycle test driver and exact-stage assertions changed. Stop check and audit claim share the existing runner lock; original refusal expectations unchanged. All three helper consumers rerun. Original full run and test sources retained.'}
(r/'build/test-driver109.json').write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8');print(json.dumps(value))
