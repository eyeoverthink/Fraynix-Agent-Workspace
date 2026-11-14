from pathlib import Path
import hashlib,json,datetime
r=Path.cwd();p=r/'docs/OBSERVATION_141_LOCK.json';assert not p.exists()
prior=json.loads((r/'docs/OBSERVATION_140_LOCK.json').read_text(encoding='utf8'));paths=set(prior['sourceHashes'])
paths.update(['docs/EDITION_141_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/LossRecovery141.java','workspace-engine/src/main/java/fraymus/workspace/RecoverySearch141.java','workspace-engine/src/main/java/fraymus/workspace/LossRecoveryAudit141.java','workspace-engine/src/test/java/fraymus/workspace/LossRecovery141Test.java','workspace-desktop/test/recovery141-integration.cjs','docs/publication141/select141.py','docs/publication141/prepare141.py'])
data={'edition':141,'lockedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'parent':'d27846b206c73b8dc3326b398f746d0212a01fd9','methods':['legacy-flow','foreground-guard-139','recovery-monitor-141','recovery-guard-141'],'sourceHashes':{n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest() for n in sorted(paths)},'scope':'Production, scorer, controls, UI, source selection and fixed protocol frozen before fresh annotations or pixels. No fresh-outcome tuning. Source search is bounded and excludes ineligible entries without replacements. Default and all checks retained.'}
p.write_bytes((json.dumps(data,indent=2)+'\n').encode())
p=r/'.gitattributes';s=p.read_text(encoding='utf8');s+='\n/docs/OBSERVATION_141_LOCK.json -text\n/docs/publication141/source141.json -text\n/docs/publication141/*-labels.json -text\n/docs/publication141/*-groundtruth.txt -text\n';p.write_text(s,encoding='utf8')
print(data['lockedAtUtc'])
