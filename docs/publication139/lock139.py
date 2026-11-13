from pathlib import Path
import json,hashlib,subprocess,datetime
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest();p=r/'docs/OBSERVATION_139_LOCK.json';assert not p.exists()
paths=list(json.loads((r/'docs/OBSERVATION_138_LOCK.json').read_text(encoding='utf8'))['sourceHashes'])
paths += ['workspace-engine/src/main/java/fraymus/workspace/'+n+'.java' for n in ['SpatialEvidence139','ForegroundGuard139','ForegroundAudit139']]
paths += ['workspace-engine/src/test/java/fraymus/workspace/ForegroundGuard139Test.java','docs/EDITION_139_CONTRACT.md']
data={'edition':139,'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'methods':['legacy-flow','drift-guard-138','monitor-only-139','foreground-guard-139'],'sourceHashes':{n:sha((r/n).read_bytes().replace(b'\r\n',b'\n')) for n in sorted(set(paths))},'scope':'Frozen before downloading or viewing VOT-LT2018 ballet/bicycle/bike1 source pixels or annotations. Source-only windows and labels must be committed before predictions. No outcome tuning or default promotion. Exposed138 recordings are development only.'}
p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf8');print(json.dumps({'files':len(data['sourceHashes']),'lockSha256':sha(p.read_bytes())}))
