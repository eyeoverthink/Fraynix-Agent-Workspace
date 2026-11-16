from pathlib import Path
import json,hashlib,datetime,subprocess
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest()
old=json.loads((r/'docs/OBSERVATION_148_LOCK.json').read_bytes())
names=set(old['sourceHashes'])
names.update(str(p.relative_to(r)).replace('\\','/') for p in (r/'workspace-engine/src/main/java/fraymus/workspace').glob('*149.java'))
names.update(str(p.relative_to(r)).replace('\\','/') for p in (r/'workspace-engine/src/test/java/fraymus/workspace').glob('*149Test.java'))
names.update(['workspace-desktop/test/identity149-integration.cjs','docs/publication149/PROTOCOL_149.md','docs/publication149/restart149.cjs','docs/publication149/capture149.cjs'])
lock={'edition':149,'parent':'d3012d4bf02ce3c2396439099e9011d391ff8780','lockedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'methods':['path-confirmation-148','identity-monitor-149','identity-return-149'],'sourceHashes':{name:sha((r/name).read_bytes().replace(b'\r\n',b'\n')) for name in sorted(names)},'scope':'Full structural identity evidence followed by a new observed motion segment. Defaults unchanged; no automatic appearance admissions or physical action authority. Existing148 footage is exposed; reserved generated cases execute only after this lock is committed.'}
p=r/'docs/OBSERVATION_149_LOCK.json';assert not p.exists(),'Never overwrite a frozen method lock';p.write_bytes((json.dumps(lock,indent=2)+'\n').encode())
p=r/'docs/publication149/source149.json';source=json.loads(p.read_bytes());source['algorithmLockSha256']=sha((r/'docs/OBSERVATION_149_LOCK.json').read_bytes());p.write_bytes((json.dumps(source,indent=2)+'\n').encode())
print(json.dumps({'sourceFiles':len(names),'methods':lock['methods']}))
