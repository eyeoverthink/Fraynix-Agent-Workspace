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
 sources=json.loads(text('docs/publication145/source145.json'))
 tail=json.loads(text('docs/publication145/source145-tail.json'));initial=json.loads(text('docs/publication145/initial-source145.json'));amendment=json.loads(text('docs/publication145/capacity-amendment145.json'))
 originalManifest=subprocess.check_output(['git','show','d091cdce:docs/publication145/source145.json']);assert digest(originalManifest)==amendment['originalManifestSha256'];assert json.loads(originalManifest)==initial
 subprocess.run(['git','merge-base','--is-ancestor','d091cdce',ref],check=True)
 for original in initial['records']:
  assert digest(blob(original['labels']))==original['labelsSha256']
  if original['role']!='evaluation' or original['domain']!='real-camera':continue
  pieces=[x for x in sources['records']+tail['records'] if x.get('partitionOf')==original['case']];assert len(pieces)==2
  restored=[]
  for part in sorted(pieces,key=lambda x:x['start']):
   assert part['frames']<=96 and part['originalPackSha256']==original['packSha256'] and part['originalLabelsSha256']==original['labelsSha256']
   for row in json.loads(text(part['labels']))['rows']:restored.append(dict(row,ordinal=row['ordinal']+part['start']-original['start']))
  assert restored==json.loads(text(original['labels']))['rows']
 sources['records']+=tail['records']
 exposedSources=json.loads(text('docs/publication145/source145-exposed.json'));assert sources['algorithmLockSha256']==tail['algorithmLockSha256']==exposedSources['algorithmLockSha256'];sources['records']+=exposedSources['records']
 exportAmendment=json.loads(text('docs/publication145/export-amendment145.json'))
 for name,h in exportAmendment['harnesses'].items():assert digest(text(name).encode())==h,name
 lock=json.loads(text('docs/OBSERVATION_145_LOCK.json'))
 assert digest(blob('docs/OBSERVATION_145_LOCK.json'))==sources['algorithmLockSha256']
 for name,h in lock['sourceHashes'].items():assert digest(text(name).encode())==h,name
 for source in sources['records']:assert digest(blob(source['labels']))==source['labelsSha256'],source['labels']
 annotations=json.loads(text('docs/publication145/annotation-integrity145.json'))
 for row in annotations:
  original=blob(row['path']);assert len(original)==row['bytes'] and digest(original)==row['sha256'],row['path']
 for source in sources['records']:
  if 'originalGroundTruth' in source:assert digest(blob(source['originalGroundTruth']))==source['originalGroundTruthSha256'],source['originalGroundTruth']
 inventory=json.loads(text('docs/readmes/145/file-inventory.json'))
 for row in inventory:
  b=blob(row['path'])
  if row['hashKind']=='utf8-lf':b=b.decode('utf8').replace('\r\n','\n').encode()
  assert digest(b)==row['sha256'] and len(b)==row['bytes'],row['path']
 manifest=json.loads(text('docs/readmes/editions.json'));frozen=0
 for e in manifest['editions']:
  assert digest(text(e['markdown']).encode())==e['markdownSha256'],e['markdown'];frozen+=1
  for row in e['snapshotFiles']:
   b=blob(row['path']) if row['kind']=='binary' else text(row['path']).encode();assert digest(b)==row['sha256'],row['path'];frozen+=1
 original=json.loads(text('docs/readmes/145/original-evidence.json'))
 restartReceipt=json.loads(text('docs/readmes/145/restart-check.json'));executed=next(x for x in original['files'] if x['logicalPath']=='docs/publication145/restart-exposed145.cjs');assert executed['rawSha256']==restartReceipt['harnessSha256'];executedBytes=gzip.decompress(blob(executed['path']));assert executedBytes.decode('utf8').replace('\r\n','\n')==text(executed['logicalPath'])
 archiveChecks={}
 for row in original['files']:
  key=row['path']
  if key not in archiveChecks:
   b=blob(key);raw=gzip.decompress(b);archiveChecks[key]=(len(b),digest(b),len(raw),digest(raw))
  assert archiveChecks[key]==(row['bytes'],row['sha256'],row['rawBytes'],row['rawSha256']),row['logicalPath']
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','fetchedRef':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'rawLabelInputsVerified':len(sources['records']),'rawMethodLockVerified':True,'canonicalSourceFiles':len(inventory),'editions':len(manifest['editions']),'frozenHashes':frozen,'original145EvidenceFiles':len(original['files']),'distinctOriginalPayloads':len(archiveChecks),'method':'Every enumerated blob read from the specified Git tree; remoteFetched identifies a fresh verification ref. Source byte counts and canonical UTF-8 LF/binary hashes checked. All frozen edition hashes checked. Each unique immutable Git archive blob is decompressed once; every logical original entry is compared to its verified compressed/raw byte counts and SHA-256. No mutable-file timestamp or runtime-proof shortcut.'}
 result['rawOriginalAnnotationsVerified']=len(annotations)
 result['productionScorerSourceLabelsUnchangedSincePreEvaluation']=True
 (r/'build/publication-audit145.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
finally:p.stdin.close();p.wait()
