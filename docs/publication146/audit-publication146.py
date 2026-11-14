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
 sources=json.loads(text('docs/publication146/source146.json'));lock=json.loads(text('docs/OBSERVATION_146_LOCK.json'))
 assert digest(blob('docs/OBSERVATION_146_LOCK.json'))==sources['algorithmLockSha256']
 subprocess.run(['git','merge-base','--is-ancestor','236d4b2f',ref],check=True)
 for name,h in lock['sourceHashes'].items():assert digest(text(name).encode())==h,name
 for source in sources['records']:assert digest(blob(source['labels']))==source['labelsSha256'],source['labels']
 annotations=json.loads(text('docs/publication146/annotation-integrity146.json'))
 for row in annotations:
  raw=blob(row['path']);assert len(raw)==row['bytes'] and digest(raw)==row['sha256'],row['path']
 for source in sources['records']:
  if 'originalGroundTruth' in source:assert digest(blob(source['originalGroundTruth']))==source['originalGroundTruthSha256']
 inventory=json.loads(text('docs/readmes/146/file-inventory.json'))
 for row in inventory:
  b=blob(row['path'])
  if row['hashKind']=='utf8-lf':b=b.decode('utf8').replace('\r\n','\n').encode()
  assert digest(b)==row['sha256'] and len(b)==row['bytes'],row['path']
 manifest=json.loads(text('docs/readmes/editions.json'));assert manifest['latest']==146;frozen=0
 prior=json.loads(subprocess.check_output(['git','show','672894eab948f372cfa368ce7168dc667951c4eb:docs/readmes/editions.json']));assert manifest['editions'][:-1]==prior['editions']
 for e in manifest['editions']:
  assert digest(text(e['markdown']).encode())==e['markdownSha256'],e['markdown'];frozen+=1
  for row in e['snapshotFiles']:
   b=blob(row['path']) if row['kind']=='binary' else text(row['path']).encode();assert digest(b)==row['sha256'],row['path'];frozen+=1
 original=json.loads(text('docs/readmes/146/original-evidence.json'));restart=json.loads(text('docs/readmes/146/restart-check.json'))
 executed=next(x for x in original['files'] if x['logicalPath']=='docs/publication146/restart146.cjs');assert executed['rawSha256']==restart['harnessSha256'];assert gzip.decompress(blob(executed['path'])).decode('utf8').replace('\r\n','\n')==text(executed['logicalPath'])
 checked={}
 for row in original['files']:
  if row['path'] not in checked:
   b=blob(row['path']);raw=gzip.decompress(b);checked[row['path']]=(len(b),digest(b),len(raw),digest(raw))
  assert checked[row['path']]==(row['bytes'],row['sha256'],row['rawBytes'],row['rawSha256']),row['logicalPath']
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'remote':'https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git','fetchedRef':ref,'remoteFetched':ref.startswith('refs/verify/'),'commit':actual,'rawLabelInputsVerified':len(sources['records']),'rawMethodLockVerified':True,'canonicalSourceFiles':len(inventory),'editions':len(manifest['editions']),'frozenHashes':frozen,'original146EvidenceFiles':len(original['files']),'distinctOriginalPayloads':len(checked),'rawOriginalAnnotationsVerified':len(annotations),'productionScorerSourceLabelsUnchangedSincePreEvaluation':True,'method':'Every enumerated Git blob checked for byte length and canonical LF or binary SHA-256. All frozen editions unchanged; every compressed original decompressed and checked. Freshly fetched refs distinguish remote publication from local validation.'}
 (r/'build/publication-audit146.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
finally:p.stdin.close();p.wait()
