"""Documented encoding-only amendment after source generation, before any evaluation predictions."""
from pathlib import Path
import json,hashlib,datetime,subprocess
r=Path.cwd();out=r/'docs/publication144';sha=lambda b:hashlib.sha256(b).hexdigest();name='workspace-desktop/renderer/observation-panel.js';p=r/name;old=p.read_bytes();s=old.decode('utf8',errors='surrogateescape');invalid=[ord(c)-0xdc00 for c in s if 0xdc80<=ord(c)<=0xdcff];assert invalid and set(invalid)=={0xb7},invalid
assert not (r/'build/ambiguity144-app').exists(),'Must amend before evaluation'
fixed=''.join(bytes([ord(c)-0xdc00]).decode('cp1252') if 0xdc80<=ord(c)<=0xdcff else c for c in s).encode('utf8');fixed.decode('utf8')
lockPath=r/'docs/OBSERVATION_144_LOCK.json';oldLock=lockPath.read_bytes();lock=json.loads(oldLock);(out/'lock144-original.json').write_bytes(oldLock);(out/'observation-panel144-original.bin').write_bytes(old)
assert sha(old.replace(b'\r\n',b'\n'))==lock['sourceHashes'][name];p.write_bytes(fixed);lock['sourceHashes'][name]=sha(fixed.replace(b'\r\n',b'\n'));lockPath.write_bytes((json.dumps(lock,indent=2)+'\n').encode())
sourcePath=out/'source144.json';source=sourcePath.read_bytes();(out/'source144-before-encoding.json').write_bytes(source);manifest=json.loads(source);manifest['algorithmLockSha256']=sha(lockPath.read_bytes());sourcePath.write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
record={'atUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'beforeEvaluation':True,'reason':'Actual app source-integrity check refused invalid UTF-8 in newly added UI middle-dot characters. Converted only isolated CP1252 B7 bytes to UTF-8 C2 B7. No logic, thresholds, source pixels, label rows or outcome scorer change.','path':name,'repairedBytes':len(invalid),'originalRawSha256':sha(old),'amendedRawSha256':sha(fixed),'originalCanonicalSha256':sha(old.replace(b'\r\n',b'\n')),'amendedCanonicalSha256':sha(fixed.replace(b'\r\n',b'\n')),'originalLockSha256':sha(oldLock),'amendedLockSha256':sha(lockPath.read_bytes()),'preservedAgencyFailure':'build/agency144-evidence/agency043-failed-1794636493047.json'}
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
for row in manifest['records']:assert sha((r/row['labels']).read_bytes())==row['labelsSha256'];assert sha((r/row['pack']).read_bytes())==row['packSha256']
(out/'ui-encoding144.json').write_bytes((json.dumps(record,indent=2)+'\n').encode());print(json.dumps(record))
