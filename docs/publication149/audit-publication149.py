import subprocess,json,hashlib,sys,datetime,gzip
from pathlib import Path
from public_evidence149 import public_bytes,check_public
r=Path.cwd();ref=sys.argv[1];expected=sys.argv[2]
actual=subprocess.check_output(['git','rev-parse',ref],cwd=r,text=True).strip();assert actual==expected
p=subprocess.Popen(['git','cat-file','--batch'],cwd=r,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
def blob(name):
 p.stdin.write((ref+':'+name+'\n').encode());p.stdin.flush();h=p.stdout.readline().decode().strip().split();assert len(h)==3 and h[1]=='blob',(name,h)
 b=p.stdout.read(int(h[2]));assert p.stdout.read(1)==b'\n';return b
def text(name):return blob(name).decode('utf-8-sig').replace('\r\n','\n')
def digest(b):return hashlib.sha256(b).hexdigest()
try:
 sources=json.loads(text('docs/publication149/source149.json'));lock=json.loads(text('docs/OBSERVATION_149_LOCK.json'))
 assert digest(blob('docs/OBSERVATION_149_LOCK.json'))==sources['algorithmLockSha256']
 for commit in ['9e19970a','9090c62d']:subprocess.run(['git','merge-base','--is-ancestor',commit,ref],check=True)
 assert digest(blob('docs/publication149/lock149-v1.json'))==lock['previousLockSha256']
 for frozen in [lock]:
  for name,h in frozen['sourceHashes'].items():assert digest(text(name).encode())==h,name
 for source in sources['records']:assert digest(blob(source['labels']))==source['labelsSha256'],source['labels']
 for source in sources['records']:
  if source['domain']=='exploratory-real-camera':
   assert source['primaryEligible'] is False and source['preflight']['passed'] is False
   assert source['preflight']['actualHiddenSamples']==62 and source['preflight']['primaryMaximum']==60
   assert digest(blob(source['preflight']['declaration']))==source['preflight']['declarationSha256']
   assert digest(blob('docs/publication149/prepare-exploratory149.py'))==source['preflight']['preparerSha256']
   declaration=next(s for s in sources['freshSourceReview149']['sources'] if s['name']==source['group'])
   assert declaration['eligible'] is False and declaration['donor']==[1,24] and declaration['evaluation']==[32,124]
   assert declaration['event']=={'hiddenStart':42,'returnFrame':104}
   assert source['sourceFrames']==list(range(source['start'],source['end']+1)) and source['stride']==1 and source['frames']<=96
 camera=json.loads(text('docs/publication149/webcam-lock149.json'));assert digest(blob('docs/OBSERVATION_149_LOCK.json'))==camera['algorithmLockSha256']
 for name,h in camera['sourceHashes'].items():assert digest(text(name).encode())==h,name
 if 'webcamLock149Sha256' in sources:assert digest(blob('docs/publication149/webcam-lock149.json'))==sources['webcamLock149Sha256']
 verification=json.loads(text('docs/readmes/149/verification.json'))
 for group,receipt in verification['studies'].items():
  study_commit=receipt['source'];subprocess.run(['git','merge-base','--is-ancestor',study_commit,ref],check=True)
  then=json.loads(subprocess.check_output(['git','show',study_commit+':docs/publication149/source149.json']))
  assert [x for x in then['records'] if x['group']==group]==[x for x in sources['records'] if x['group']==group],group
  assert then['algorithmLockSha256']==sources['algorithmLockSha256']
 inventory=json.loads(text('docs/readmes/149/file-inventory.json'))
 for row in inventory:
  b=blob(row['path'])
  if row['hashKind']=='utf8-lf':b=b.decode('utf-8-sig').replace('\r\n','\n').encode()
  assert digest(b)==row['sha256'] and len(b)==row['bytes'],row['path']
 manifest=json.loads(text('docs/readmes/editions.json'));assert manifest['latest']==149;frozen=0
 prior=json.loads(subprocess.check_output(['git','show','d3012d4bf02ce3c2396439099e9011d391ff8780:docs/readmes/editions.json']));assert manifest['editions'][:-1]==prior['editions']
 for e in manifest['editions']:
  assert digest(text(e['markdown']).encode())==e['markdownSha256'],e['markdown'];frozen+=1
  for row in e['snapshotFiles']:
   b=blob(row['path']) if row['kind']=='binary' else text(row['path']).encode();assert digest(b)==row['sha256'],row['path'];frozen+=1
 original=json.loads(text('docs/readmes/149/original-evidence.json'));restart=json.loads(text('docs/readmes/149/restart-check.json'))
 executed=next(x for x in original['files'] if x['logicalPath']=='docs/publication149/restart149.cjs');assert executed['rawSha256']==restart['harnessSha256'];assert gzip.decompress(blob(executed['path'])).decode('utf8').replace('\r\n','\n')==text(executed['logicalPath'])
 checked={}
 for row in original['files']:
  if row['path'] not in checked:
   b=blob(row['path']);raw=gzip.decompress(b);checked[row['path']]=(len(b),digest(b),len(raw),digest(raw))
  assert checked[row['path']]==(row['bytes'],row['sha256'],row['rawBytes'],row['rawSha256']),row['logicalPath']
  if row.get('transformation')=='public-view-149':
   private_original=(r/row['logicalPath']).read_bytes();assert len(private_original)==row['privateOriginalBytes'] and digest(private_original)==row['privateOriginalSha256']
   public,omitted=public_bytes(private_original);assert len(omitted)==row['omittedFeatureArrays'] and public==gzip.decompress(blob(row['path']))
   check_public(json.loads(public))
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','fetchedRef':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'rawLabelInputsVerified':len(sources['records']),'rawMethodLockVerified':True,'sourceScope':'Exposed148 regression and separately declared exploratory149 camera source; primary eligibility and raw label hashes preserved','canonicalSourceFiles':len(inventory),'editions':len(manifest['editions']),'frozenHashes':frozen,'original149EvidenceFiles':len(original['files']),'distinctOriginalPayloads':len(checked),'privateReceiptViews':sum(row.get('transformation')=='public-view-149' for row in original['files']),'productionScorerSourceLabelsUnchangedSincePreEvaluation':True,'privateSourceImagesPublished':False,'method':'Every enumerated Git blob checked for length and canonical LF or binary SHA-256. Frozen editions unchanged; every compressed original checked. Fresh fetched refs distinguish remote publication from local validation. Private camera pixels excluded.'}
 (r/'build/publication-audit149.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
finally:p.stdin.close();p.wait()
