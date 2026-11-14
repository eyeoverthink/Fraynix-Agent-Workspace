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
 sources=json.loads(text('docs/publication143/source143.json'))
 lock=json.loads(text('docs/OBSERVATION_143_LOCK.json'))
 assert digest(blob('docs/OBSERVATION_143_LOCK.json'))==sources['algorithmLockSha256']
 for name,h in lock['sourceHashes'].items():assert digest(text(name).encode())==h,name
 originalLock=json.loads(text('docs/publication143/lock143-original.json'))
 assert set(originalLock['sourceHashes'])==set(lock['sourceHashes'])
 changed=[n for n,h in originalLock['sourceHashes'].items() if lock['sourceHashes'][n]!=h]
 assert changed==['workspace-desktop/test/confirmation143-integration.cjs'],changed
 assert digest(text('docs/publication143/harness143-original.txt').encode())==originalLock['sourceHashes'][changed[0]]
 amendment=json.loads(text('docs/publication143/donor-refusal143.json'))
 assert amendment['amendedHarnessSha256']==lock['sourceHashes'][changed[0]]
 sourceBefore=json.loads(subprocess.check_output(['git','show','6951c268:docs/publication143/source143.json']).decode('utf8'))
 assert sourceBefore['records']==sources['records'] and sourceBefore['sourceReview']==sources['sourceReview']
 for source in sources['records']:assert digest(blob(source['labels']))==source['labelsSha256'],source['labels']
 annotations=json.loads(text('docs/publication143/annotation-integrity143.json'))
 for row in annotations:
  original=blob(row['path']);assert len(original)==row['bytes'] and digest(original)==row['sha256'],row['path']
 for source in sources['records']:
  if 'originalGroundTruth' in source:assert digest(blob(source['originalGroundTruth']))==source['originalGroundTruthSha256'],source['originalGroundTruth']
 inventory=json.loads(text('docs/readmes/143/file-inventory.json'))
 for row in inventory:
  b=blob(row['path'])
  if row['hashKind']=='utf8-lf':b=b.decode('utf8').replace('\r\n','\n').encode()
  assert digest(b)==row['sha256'] and len(b)==row['bytes'],row['path']
 manifest=json.loads(text('docs/readmes/editions.json'));frozen=0
 for e in manifest['editions']:
  assert digest(text(e['markdown']).encode())==e['markdownSha256'],e['markdown'];frozen+=1
  for row in e['snapshotFiles']:
   b=blob(row['path']) if row['kind']=='binary' else text(row['path']).encode();assert digest(b)==row['sha256'],row['path'];frozen+=1
 original=json.loads(text('docs/readmes/143/original-evidence.json'))
 archiveChecks={}
 for row in original['files']:
  key=row['path']
  if key not in archiveChecks:
   b=blob(key);raw=gzip.decompress(b);archiveChecks[key]=(len(b),digest(b),len(raw),digest(raw))
  assert archiveChecks[key]==(row['bytes'],row['sha256'],row['rawBytes'],row['rawSha256']),row['logicalPath']
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','fetchedRef':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'rawLabelInputsVerified':len(sources['records']),'rawMethodLockVerified':True,'canonicalSourceFiles':len(inventory),'editions':len(manifest['editions']),'frozenHashes':frozen,'original143EvidenceFiles':len(original['files']),'distinctOriginalPayloads':len(archiveChecks),'method':'Every enumerated blob read from the specified Git tree; remoteFetched identifies a fresh verification ref. Source byte counts and canonical UTF-8 LF/binary hashes checked. All frozen edition hashes checked. Each unique immutable Git archive blob is decompressed once; every logical original entry is compared to its verified compressed/raw byte counts and SHA-256. No mutable-file timestamp or runtime-proof shortcut.'}
 result['rawOriginalAnnotationsVerified']=len(annotations)
 result['originalFreezeAndTwoHarnessAmendmentsVerified']=True
 result['productionScorerSourceLabelsUnchangedSincePreEvaluation']=True
 (r/'build/publication-audit143.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
finally:p.stdin.close();p.wait()
