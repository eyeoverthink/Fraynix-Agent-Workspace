import subprocess,json,hashlib,sys,datetime,gzip
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'publication149'))
from public_evidence149 import public_bytes,check_public
r=Path.cwd();ref=sys.argv[1];expected=sys.argv[2]
actual=subprocess.check_output(['git','rev-parse',ref],text=True).strip();assert actual==expected
p=subprocess.Popen(['git','cat-file','--batch'],stdin=subprocess.PIPE,stdout=subprocess.PIPE)
def blob(name):
 p.stdin.write((ref+':'+name+'\n').encode());p.stdin.flush();h=p.stdout.readline().decode().strip().split();assert len(h)==3 and h[1]=='blob',(name,h)
 b=p.stdout.read(int(h[2]));assert p.stdout.read(1)==b'\n';return b
def text(name):return blob(name).decode('utf-8-sig').replace('\r\n','\n')
def digest(b):return hashlib.sha256(b).hexdigest()
try:
 sources=json.loads(text('docs/publication150/source150.json'));lock=json.loads(text('docs/OBSERVATION_150_LOCK.json'))
 assert digest(blob('docs/OBSERVATION_150_LOCK.json'))==sources['algorithmLockSha256']
 subprocess.run(['git','merge-base','--is-ancestor','b851a1d8',ref],check=True)
 assert digest(blob('docs/OBSERVATION_149_LOCK.json'))==lock['previous149LockSha256']
 for name,h in lock['sourceHashes'].items():assert digest(text(name).encode())==h,name
 for source in sources['records']:
  assert digest(blob(source['labels']))==source['labelsSha256'],source['labels']
  if source['domain']=='fresh-real-camera':
   assert source['primaryEligible'] and source['preflight']['passed']
   assert source['stride']==1 and source['sourceFrames']==list(range(source['start'],source['end']+1)) and source['frames']<=96
   if source['role']=='reference-source':assert source['start']==1 and source['end']==24
   else:assert source['start']==25
 camera=json.loads(text('docs/publication150/webcam-lock150.json'))
 assert camera['algorithmLockSha256']==sources['algorithmLockSha256']
 for name,h in camera['sourceHashes'].items():assert digest(text(name).encode())==h,name
 assert digest(blob('docs/publication150/webcam-lock150.json'))==sources['webcamLock150Sha256']
 verification=json.loads(text('docs/readmes/150/verification.json'))
 for group in ['generated150','webcam150-displaced-return']:
  review=json.loads(text('docs/readmes/150/'+group+'-screenshot-review.json'))
  assert review['captureHarnessSha256']==digest(blob('docs/publication150/focused-capture150.cjs'))
 for group,receipt in verification['studies'].items():
  study_commit=receipt['source'];subprocess.run(['git','merge-base','--is-ancestor',study_commit,ref],check=True)
  then=json.loads(subprocess.check_output(['git','show',study_commit+':docs/publication150/source150.json']))
  assert [x for x in then['records'] if x['group']==group]==[x for x in sources['records'] if x['group']==group],group
  assert then['algorithmLockSha256']==sources['algorithmLockSha256']
 inventory=json.loads(text('docs/readmes/150/file-inventory.json'))
 for row in inventory:
  b=blob(row['path'])
  if row['hashKind']=='utf8-lf':b=b.decode('utf-8-sig').replace('\r\n','\n').encode()
  assert digest(b)==row['sha256'] and len(b)==row['bytes'],row['path']
 manifest=json.loads(text('docs/readmes/editions.json'));assert manifest['latest']==150
 prior=json.loads(subprocess.check_output(['git','show','e5479b7193df57eb0fdfbc930bd87fe9a0e2cc70:docs/readmes/editions.json']));assert manifest['editions'][:-1]==prior['editions']
 frozen=0
 for e in manifest['editions']:
  assert digest(text(e['markdown']).encode())==e['markdownSha256'],e['markdown'];frozen+=1
  for row in e['snapshotFiles']:
   b=blob(row['path']) if row['kind']=='binary' else text(row['path']).encode();assert digest(b)==row['sha256'],row['path'];frozen+=1
 original_index=json.loads(text('docs/readmes/150/original-evidence.json'));restart=json.loads(text('docs/readmes/150/restart-check.json'))
 executed=next(x for x in original_index['files'] if x['logicalPath']=='docs/publication150/restart150.cjs')
 assert executed['rawSha256']==restart['harnessSha256']
 assert gzip.decompress(blob(executed['path'])).decode('utf8').replace('\r\n','\n')==text(executed['logicalPath'])
 checked={}
 for row in original_index['files']:
  if row['path'] not in checked:
   b=blob(row['path']);raw=gzip.decompress(b);checked[row['path']]=(len(b),digest(b),len(raw),digest(raw))
  assert checked[row['path']]==(row['bytes'],row['sha256'],row['rawBytes'],row['rawSha256']),row['logicalPath']
  if row.get('transformation')=='public-view-149':
   private_original=(r/row['logicalPath']).read_bytes();assert len(private_original)==row['privateOriginalBytes'] and digest(private_original)==row['privateOriginalSha256']
   public,omitted=public_bytes(private_original);assert len(omitted)==row['omittedFeatureArrays'] and public==gzip.decompress(blob(row['path']));check_public(json.loads(public))
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','fetchedRef':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'rawLabelInputsVerified':len(sources['records']),'rawMethodLockVerified':True,'canonicalSourceFiles':len(inventory),'editions':150,'frozenHashes':frozen,'original150EvidenceFiles':len(original_index['files']),'distinctOriginalPayloads':len(checked),'privateReceiptViews':sum(row.get('transformation')=='public-view-149' for row in original_index['files']),'sourceDeclarationsUnchangedSincePreEvaluation':True,'privateSourceImagesPublished':False,'method':'Every enumerated Git blob checked for length and canonicalLF/binarySHA256. Previous149 editions unchanged. Original receipts and explicitly derived private views verified. Fetched refs establish actual publication.'}
 (r/'build/publication-audit150.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
finally:p.stdin.close();p.wait()
