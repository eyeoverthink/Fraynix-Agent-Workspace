from pathlib import Path
import subprocess,json,hashlib,base64,gzip,sys
r=Path.cwd();ref=sys.argv[1]if len(sys.argv)>1 else'HEAD';assert(r/'docs/readmes/editions.json').exists()
p=subprocess.Popen(['git','cat-file','--batch'],cwd=r,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
def blob(path):
 p.stdin.write((ref+':'+path+'\n').encode());p.stdin.flush();h=p.stdout.readline().decode().strip().split();assert len(h)==3 and h[1]=='blob',(path,h);b=p.stdout.read(int(h[2]));assert p.stdout.read(1)==b'\n';return b
def value(path):return json.loads(blob(path).decode('utf-8-sig'))
def digest(b,binary=False):return hashlib.sha256(b if binary else b.decode('utf-8-sig').replace('\r\n','\n').encode('utf-8')).hexdigest()
archives=[];frozen=0
try:
 manifest=value('docs/readmes/editions.json');assert manifest['latest']==45
 inv=value('docs/readmes/045/file-inventory.json')
 for x in inv:assert digest(blob(x['path']),x['hashKind']=='binary')==x['sha256'],x['path']
 for e in manifest['editions']:
  assert digest(blob(e['markdown']))==e['markdownSha256'],e['markdown'];frozen+=1
  for f in e['snapshotFiles']:assert digest(blob(f['path']),f['kind']=='binary')==f['sha256'],f['path'];frozen+=1
 files=subprocess.check_output(['git','ls-tree','-r','--name-only',ref,'--','docs/readmes'],cwd=r,text=True).splitlines()
 for path in files:
  if not path.endswith('.json'):continue
  try:v=value(path)
  except(json.JSONDecodeError,UnicodeDecodeError):continue
  if isinstance(v,dict)and all(k in v for k in ['gzipBase64','rawSha256','rawBytes']):
   b=gzip.decompress(base64.b64decode(v['gzipBase64'],validate=True));assert len(b)==v['rawBytes']and hashlib.sha256(b).hexdigest()==v['rawSha256'],path;archives.append({'path':path,'bytes':len(b),'sha256':v['rawSha256']})
finally:p.stdin.close();p.wait()
report={'passed':True,'ref':ref,'commit':subprocess.check_output(['git','rev-parse',ref],cwd=r,text=True).strip(),'sourceFiles':len(inv),'originalProofArchives':len(archives),'frozenHashes':frozen,'editions':45,'scope':'Every canonical source, every frozen numbered README/snapshot blob and every supported top-level original gzip proof JSON is read from the selected fetched Git ref, rather than the working tree. Exact bytes/count/SHA256 are checked, including duplicate archive payloads. Counts are archive files, not distinct experiments.','archives':archives};(r/'build/publication045-audit.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print(json.dumps({k:report[k]for k in ['passed','ref','commit','sourceFiles','originalProofArchives','frozenHashes','editions']}))
