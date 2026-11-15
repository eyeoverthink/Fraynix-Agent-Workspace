import subprocess,json,hashlib,sys,datetime,gzip
from pathlib import Path
r=Path.cwd();ref=sys.argv[1];expected=sys.argv[2]
actual=subprocess.check_output(['git','rev-parse',ref],cwd=r,text=True).strip();assert actual==expected
p=subprocess.Popen(['git','cat-file','--batch'],cwd=r,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
def blob(name):
 p.stdin.write((ref+':'+name+'\n').encode());p.stdin.flush();h=p.stdout.readline().decode().strip().split();assert len(h)==3 and h[1]=='blob',(name,h)
 b=p.stdout.read(int(h[2]));assert p.stdout.read(1)==b'\n';return b
def text(name):return blob(name).decode('utf-8-sig').replace('\r\n','\n')
def digest(b):return hashlib.sha256(b).hexdigest()
try:
 sources=json.loads(text('docs/publication148/source148.json'));lock=json.loads(text('docs/OBSERVATION_148_LOCK.json'));webcam=json.loads(text('docs/publication148/webcam-lock148.json'))
 assert digest(blob('docs/OBSERVATION_148_LOCK.json'))==sources['algorithmLockSha256']
 assert digest(blob('docs/publication148/webcam-lock148.json'))==sources['webcamLockSha256']
 for commit in ['7d48191c']:subprocess.run(['git','merge-base','--is-ancestor',commit,ref],check=True)
 for frozen in [lock,webcam]:
  for name,h in frozen['sourceHashes'].items():assert digest(text(name).encode())==h,name
 for source in sources['records']:assert digest(blob(source['labels']))==source['labelsSha256'],source['labels']
 inventory=json.loads(text('docs/readmes/148/file-inventory.json'))
 for row in inventory:
  b=blob(row['path'])
  if row['hashKind']=='utf8-lf':b=b.decode('utf-8-sig').replace('\r\n','\n').encode()
  assert digest(b)==row['sha256'] and len(b)==row['bytes'],row['path']
 manifest=json.loads(text('docs/readmes/editions.json'));assert manifest['latest']==148;frozen=0
 prior=json.loads(subprocess.check_output(['git','show','c8a8c7efabd9a3a6ee2bb4e01180ffc736944edd:docs/readmes/editions.json']));assert manifest['editions'][:-1]==prior['editions']
 for e in manifest['editions']:
  assert digest(text(e['markdown']).encode())==e['markdownSha256'],e['markdown'];frozen+=1
  for row in e['snapshotFiles']:
   b=blob(row['path']) if row['kind']=='binary' else text(row['path']).encode();assert digest(b)==row['sha256'],row['path'];frozen+=1
 original=json.loads(text('docs/readmes/148/original-evidence.json'));restart=json.loads(text('docs/readmes/148/restart-check.json'))
 executed=next(x for x in original['files'] if x['logicalPath']=='docs/publication148/restart148.cjs');assert executed['rawSha256']==restart['harnessSha256'];assert gzip.decompress(blob(executed['path'])).decode('utf8').replace('\r\n','\n')==text(executed['logicalPath'])
 checked={}
 for row in original['files']:
  if row['path'] not in checked:
   b=blob(row['path']);raw=gzip.decompress(b);checked[row['path']]=(len(b),digest(b),len(raw),digest(raw))
  assert checked[row['path']]==(row['bytes'],row['sha256'],row['rawBytes'],row['rawSha256']),row['logicalPath']
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','fetchedRef':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'rawLabelInputsVerified':len(sources['records']),'rawMethodLockVerified':True,'webcamSourceLockVerified':True,'canonicalSourceFiles':len(inventory),'editions':len(manifest['editions']),'frozenHashes':frozen,'original148EvidenceFiles':len(original['files']),'distinctOriginalPayloads':len(checked),'productionScorerSourceLabelsUnchangedSincePreEvaluation':True,'privateSourceImagesPublished':False,'method':'Every enumerated Git blob checked for length and canonical LF or binary SHA-256. Frozen editions unchanged; every compressed original checked. Fresh fetched refs distinguish remote publication from local validation. Private camera pixels excluded.'}
 (r/'build/publication-audit148.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
finally:p.stdin.close();p.wait()
