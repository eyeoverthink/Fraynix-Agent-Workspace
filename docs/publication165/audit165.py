from pathlib import Path
import hashlib,json,subprocess,sys,gzip,datetime
r=Path.cwd();ref=sys.argv[1];expected=sys.argv[2];actual=subprocess.check_output(['git','rev-parse',ref],text=True).strip();assert actual==expected
p=subprocess.Popen(['git','-c','core.packedGitLimit=128m','-c','core.packedGitWindowSize=16m','cat-file','--batch'],stdin=subprocess.PIPE,stdout=subprocess.PIPE);sha=lambda b:hashlib.sha256(b).hexdigest()
def blob(name):
 p.stdin.write((ref+':'+name+'\n').encode());p.stdin.flush();h=p.stdout.readline().decode().split();assert len(h)==3 and h[1]=='blob',(name,h);b=p.stdout.read(int(h[2]));assert p.stdout.read(1)==b'\n';return b
def canon(name):return blob(name).decode('utf-8-sig').replace('\r\n','\n').encode()
try:
 lock=json.loads(blob('docs/EVIDENCE_REUSE_165_LOCK.json'))
 for name,h in lock['sourceHashes'].items():assert sha(canon(name))==h,name
 inventory=json.loads(blob('docs/readmes/165/file-inventory.json'))
 for item in inventory:
  b=canon(item['path']) if item['hashKind']=='utf8-lf' else blob(item['path']);assert len(b)==item['bytes'] and sha(b)==item['sha256'],item['path']
 manifest=json.loads(blob('docs/readmes/editions.json'));prior=json.loads(subprocess.check_output(['git','show','100518181:docs/readmes/editions.json']));assert manifest['latest']==165 and manifest['editions'][:-1]==prior['editions'];frozen=0
 for edition in manifest['editions']:
  assert sha(canon(edition['markdown']))==edition['markdownSha256'];frozen+=1
  for item in edition['snapshotFiles']:
   b=blob(item['path']) if item['kind']=='binary' else canon(item['path']);assert sha(b)==item['sha256'],item['path'];frozen+=1
 import importlib.util
 spec=importlib.util.spec_from_file_location('evidence_archive',r/'scripts/evidence_archive.py');archive_api=importlib.util.module_from_spec(spec);spec.loader.exec_module(archive_api)
 index=json.loads(blob('docs/readmes/165/original-evidence.json'));archive=r/'build/downloaded-evidence165.zip';archive_api.verify(index,archive)
 originals={item['logicalPath']:archive_api.original(index,archive,item['logicalPath']) for item in index['files']}
 for name,raw in originals.items():
  local=(Path('J:/Codex-Work/fraynix-baseline-164')/name.removeprefix('baseline164/')) if name.startswith('baseline164/') else r/name
  assert local.read_bytes()==raw,name
 rawLock=originals['docs/EVIDENCE_REUSE_165_LOCK.json']
 assert rawLock.decode('utf-8-sig').replace('\r\n','\n').encode()==canon('docs/EVIDENCE_REUSE_165_LOCK.json')
 for name in ['on-a','off-a','off-b','on-b']:
  study=json.loads(originals['build/study165-'+name+'/result.json']);assert study['passed'] and study['methodLockSha256']==sha(rawLock);subprocess.run(['git','merge-base','--is-ancestor',study['source'],ref],check=True)
 for name in ['on-e','on-d']:
  study=json.loads(originals['baseline164/build/study164-'+name+'/result.json']);assert study['passed'];subprocess.run(['git','merge-base','--is-ancestor',study['source'],ref],check=True)
 assert json.loads(blob('docs/readmes/165/comparison-evidence.json'))['totalCompletedWorkflowsIncludingBaseline']==60
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','ref':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'editions':165,'frozenHashes':frozen,'canonicalSourceFiles':len(inventory),'evidenceFiles':len(index['files']),'originalReceiptsCompared':True,'privateCameraPixelsPublished':False}
 (r/'build/publication-audit165.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
finally:p.stdin.close();p.wait()
