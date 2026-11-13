from pathlib import Path
import json,hashlib,subprocess,datetime
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest();p=r/'docs/OBSERVATION_138_LOCK.json';assert not p.exists()
paths=list(json.loads((r/'docs/OBSERVATION_137_LOCK.json').read_text())['sourceHashes'])
paths += ['workspace-engine/src/main/java/fraymus/workspace/'+n+'.java' for n in ['AppearanceEvidence138','DriftGuard138','DriftAudit138']]
paths += ['workspace-engine/src/test/java/fraymus/workspace/DriftGuard138Test.java','docs/EDITION_138_CONTRACT.md']
data={'edition':138,'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'methods':['legacy-flow','monitor-only-138','drift-guard-138'],'sourceHashes':{n:sha((r/n).read_bytes().replace(b'\r\n',b'\n')) for n in sorted(set(paths))},'scope':'Frozen before download/viewing fresh Skating2/Suv pixels; source-only windows and labels fixed before predictions. No outcome tuning; no default promotion. Previous137 labels are development only.'}
p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf8');print(json.dumps({'files':len(data['sourceHashes']),'lockSha256':sha(p.read_bytes())}))
