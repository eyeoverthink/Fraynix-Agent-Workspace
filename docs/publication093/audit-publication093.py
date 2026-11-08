import subprocess,json,hashlib,sys,datetime,gzip
from pathlib import Path
r=Path(__file__).resolve().parent.parent;ref=sys.argv[1];expected=sys.argv[2]
actual=subprocess.check_output(['git','rev-parse',ref],cwd=r,text=True).strip();assert actual==expected
p=subprocess.Popen(['git','cat-file','--batch'],cwd=r,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
def blob(name):
 p.stdin.write((ref+':'+name+'\n').encode());p.stdin.flush();h=p.stdout.readline().decode().strip().split();assert len(h)==3 and h[1]=='blob',(name,h)
 b=p.stdout.read(int(h[2]));assert p.stdout.read(1)==b'\n';return b
def text(name):return blob(name).decode('utf-8-sig').replace('\r\n','\n')
def digest(b):return hashlib.sha256(b).hexdigest()
try:
 inventory=json.loads(text('docs/readmes/093/file-inventory.json'))
 for row in inventory:
  b=blob(row['path'])
  if row['hashKind']=='utf8-lf':b=b.decode('utf8').replace('\r\n','\n').encode()
  assert digest(b)==row['sha256'] and len(b)==row['bytes'],row['path']
 manifest=json.loads(text('docs/readmes/editions.json'));frozen=0
 for e in manifest['editions']:
  assert digest(text(e['markdown']).encode())==e['markdownSha256'],e['markdown'];frozen+=1
  for row in e['snapshotFiles']:
   b=blob(row['path']) if row['kind']=='binary' else text(row['path']).encode();assert digest(b)==row['sha256'],row['path'];frozen+=1
 original=json.loads(text('docs/readmes/093/original-evidence.json'))
 archiveChecks={}
 for row in original['files']:
  key=row['path']
  if key not in archiveChecks:
   b=blob(key);raw=gzip.decompress(b);archiveChecks[key]=(len(b),digest(b),len(raw),digest(raw))
  assert archiveChecks[key]==(row['bytes'],row['sha256'],row['rawBytes'],row['rawSha256']),row['logicalPath']
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','fetchedRef':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'canonicalSourceFiles':len(inventory),'editions':len(manifest['editions']),'frozenHashes':frozen,'original093EvidenceFiles':len(original['files']),'distinctOriginalPayloads':len(archiveChecks),'method':'Every enumerated blob read from the specified Git tree; remoteFetched identifies a fresh verification ref. Source byte counts and canonical UTF-8 LF/binary hashes checked. All frozen edition hashes checked. Each unique immutable Git archive blob is decompressed once; every logical original entry is compared to its verified compressed/raw byte counts and SHA-256. No mutable-file timestamp or runtime-proof shortcut.'}
 (r/'build/publication-audit093.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
finally:p.stdin.close();p.wait()
