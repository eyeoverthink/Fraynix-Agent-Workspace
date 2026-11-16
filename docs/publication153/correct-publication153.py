"""Preserve exact study-lock bytes after a pre-push Git newline normalization refusal."""
from pathlib import Path
import json,hashlib,gzip,subprocess,re
r=Path.cwd();out=r/'docs/readmes/153';sha=lambda b:hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-153']).returncode==1
raw=(r/'docs/OBSERVATION_153_LOCK.json').read_bytes()
assert subprocess.check_output(['git','show',':docs/OBSERVATION_153_LOCK.json'])==raw
for group in ['generated','exposed150']:
 s=json.loads((r/f'build/study153-{group}-final/result.json').read_bytes());assert s['passed'] and s['methodLockSha256']==sha(raw)
for name,h in json.loads(raw)['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
index=json.loads((out/'original-evidence.json').read_bytes())
for name in ['build/release153.log','docs/OBSERVATION_153_LOCK.json']:
 assert all(x['logicalPath']!=name for x in index['files'])
 b=(r/name).read_bytes();packed=gzip.compress(b,mtime=0);p=out/'originals'/(sha(b)+'.gz');p.write_bytes(packed)
 index['files'].append({'logicalPath':name,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':sha(b)})
(out/'original-evidence.json').write_bytes((json.dumps(index,indent=2)+'\n').encode())
paths={x['path'] for x in json.loads((out/'file-inventory.json').read_bytes())};paths.add('.gitattributes');paths.update(p.relative_to(r).as_posix() for p in (r/'docs/publication153').iterdir() if p.is_file());rows=[]
for name in sorted(paths):
 b=(r/name).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 rows.append({'path':name,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
(out/'file-inventory.json').write_bytes((json.dumps(rows,indent=2)+'\n').encode())
p=out/'index.html';s=p.read_text(encoding='utf8');s,n=re.subn(r'<script id="inventory-data" type="application/json">.*?</script>',lambda _:'<script id="inventory-data" type="application/json">'+json.dumps(rows).replace('</','<\\/')+'</script>',s,flags=re.S);assert n==1;p.write_bytes(s.encode())
print(json.dumps({'passed':True,'originalLockPreserved':True,'sources':len(rows),'evidence':len(index['files']),'trackingCodeAndStudyReceiptsUnchanged':True}))
