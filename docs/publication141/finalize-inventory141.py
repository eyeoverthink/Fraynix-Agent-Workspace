"""Finalize documentation hashes after notes, without accepting changed production inputs."""
from pathlib import Path
import hashlib,json,re
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest()
lock=json.loads((r/'docs/OBSERVATION_141_LOCK.json').read_text(encoding='utf8'))
for name,h in lock['sourceHashes'].items():
 assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
p=r/'docs/readmes/141/file-inventory.json';rows=json.loads(p.read_text(encoding='utf8'));changed=[]
for row in rows:
 raw=(r/row['path']).read_bytes()
 if row['hashKind']=='utf8-lf':raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode()
 digest=sha(raw)
 if digest!=row['sha256'] or len(raw)!=row['bytes']:
  assert row['path'] in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md'],row['path']
  changed.append(row['path']);row['sha256']=digest;row['bytes']=len(raw)
p.write_text(json.dumps(rows,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
p=r/'docs/readmes/141/index.html';s=p.read_text(encoding='utf8')
replacement='<script id="inventory-data" type="application/json">'+json.dumps(rows).replace('</','<\\/')+'</script>'
s,count=re.subn(r'<script id="inventory-data" type="application/json">.*?</script>',lambda _:replacement,s,flags=re.S)
assert count==1;p.write_text(s,encoding='utf8')
print(json.dumps({'passed':True,'finalizedDocumentation':changed,'productionLockPreserved':True}))
