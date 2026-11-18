from pathlib import Path
import json,hashlib,shutil,gzip,re
r=Path.cwd();out=r/'docs/readmes/163';sha=lambda b:hashlib.sha256(b).hexdigest()
lock=json.loads((r/'docs/RECORDED_REPLAY_163_LOCK.json').read_bytes())
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
images=[]
for width in [1450,390]:
 for suffix in ['', '-viewport']:
  name=f'guide163-{width}{suffix}.png';p=r/'build'/name;shutil.copy2(p,out/name);images.append({'path':'build/'+name,'sha256':sha(p.read_bytes()),'width':width})
review={'passed':True,'reviewedBy':'assistant-visual-inspection','images':images,'scope':'Desktop and narrow full/viewport guide images inspected; readable text, local table scrolling, source filtering, actual app screenshot and links. Automated checks separately verify filters, navigation and absence of page overflow.'}
(r/'build/visual-review163.json').write_bytes((json.dumps(review,indent=2)+'\n').encode())
index=json.loads((out/'original-evidence.json').read_bytes())
for name in ['visual163.json','visual-review163.json']:
 p=r/'build'/name;raw=p.read_bytes();packed=gzip.compress(raw,mtime=0);dest=out/'originals'/(sha(raw)+'.gz');dest.write_bytes(packed);shutil.copy2(p,out/name)
 index['files'].append({'logicalPath':'build/'+name,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
(out/'original-evidence.json').write_bytes((json.dumps(index,indent=2)+'\n').encode())
rows=json.loads((out/'file-inventory.json').read_bytes());paths={x['path'] for x in rows};paths.update(p.relative_to(r).as_posix() for p in (r/'docs/publication163').iterdir() if p.is_file());rows=[]
for name in sorted(paths):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 rows.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
(out/'file-inventory.json').write_bytes((json.dumps(rows,indent=2)+'\n').encode())
p=out/'index.html';s=p.read_text(encoding='utf8');s,n=re.subn(r'<script id="inventory-data" type="application/json">.*?</script>',lambda _:'<script id="inventory-data" type="application/json">'+json.dumps(rows).replace('</','<\\/')+'</script>',s,flags=re.S);assert n==1;p.write_bytes(s.encode());print(json.dumps({'passed':True,'sources':len(rows),'evidenceFiles':len(index['files']),'guideImages':len(images)}))
