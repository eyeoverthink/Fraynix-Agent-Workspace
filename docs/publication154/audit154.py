from pathlib import Path
import hashlib,json,subprocess,sys,gzip,datetime
r=Path.cwd();ref=sys.argv[1];expected=sys.argv[2];actual=subprocess.check_output(['git','rev-parse',ref],text=True).strip();assert actual==expected
p=subprocess.Popen(['git','cat-file','--batch'],stdin=subprocess.PIPE,stdout=subprocess.PIPE);sha=lambda b:hashlib.sha256(b).hexdigest()
def blob(name):
 p.stdin.write((ref+':'+name+'\n').encode());p.stdin.flush();h=p.stdout.readline().decode().split();assert len(h)==3 and h[1]=='blob',(name,h);b=p.stdout.read(int(h[2]));assert p.stdout.read(1)==b'\n';return b
def canon(name):return blob(name).decode('utf-8-sig').replace('\r\n','\n').encode()
try:
 lock=json.loads(blob('docs/OBSERVATION_154_LOCK.json'))
 for name,h in lock['sourceHashes'].items():assert sha(canon(name))==h,name
 inventory=json.loads(blob('docs/readmes/154/file-inventory.json'))
 for item in inventory:
  b=canon(item['path']) if item['hashKind']=='utf8-lf' else blob(item['path']);assert len(b)==item['bytes'] and sha(b)==item['sha256'],item['path']
 manifest=json.loads(blob('docs/readmes/editions.json'));prior=json.loads(subprocess.check_output(['git','show','d897b0ff:docs/readmes/editions.json']));assert manifest['latest']==154 and manifest['editions'][:-1]==prior['editions'];frozen=0
 for edition in manifest['editions']:
  assert sha(canon(edition['markdown']))==edition['markdownSha256'];frozen+=1
  for item in edition['snapshotFiles']:
   b=blob(item['path']) if item['kind']=='binary' else canon(item['path']);assert sha(b)==item['sha256'],item['path'];frozen+=1
 index=json.loads(blob('docs/readmes/154/original-evidence.json'))
 for item in index['files']:
  b=blob(item['path']);raw=gzip.decompress(b);assert len(b)==item['bytes'] and sha(b)==item['sha256'];assert len(raw)==item['rawBytes'] and sha(raw)==item['rawSha256']
  assert (r/item['logicalPath']).read_bytes()==raw,item['logicalPath']
 for group in ['generated','exposed150']:
  study=json.loads(blob('docs/readmes/154/'+group+'-study.json'));assert study['passed'] and study['methodLockSha256']==sha(blob('docs/OBSERVATION_154_LOCK.json'));subprocess.run(['git','merge-base','--is-ancestor',study['source'],ref],check=True)
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','ref':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'editions':154,'frozenHashes':frozen,'canonicalSourceFiles':len(inventory),'evidenceFiles':len(index['files']),'originalReceiptsCompared':True,'privateCameraPixelsPublished':False}
 (r/'build/publication-audit154.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
finally:p.stdin.close();p.wait()
