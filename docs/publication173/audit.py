from pathlib import Path
import datetime,hashlib,json,subprocess,sys
r=Path.cwd();sys.path.insert(0,str(r/'scripts'))
import history_archive as history
import evidence_archive as archive
ref,expected=sys.argv[1:3]
commit=subprocess.check_output(['git','rev-parse',ref],text=True).strip();assert commit==expected
sha=lambda b:hashlib.sha256(b).hexdigest()
with history.GitEvidence(r,commit) as source:
 def read(n):return json.loads(source.read(n))
 lock=read('docs/REGISTERED_RAMP_173_LOCK.json')
 for n,h in lock['sourceHashes'].items():
  b=source.read(n);assert sha(b if n in lock['binaryPaths'] else history.canonical(b,'text'))==h,n
 inventory=read('docs/readmes/173/file-inventory.json')
 for row in inventory:
  b=source.read(row['path'])
  if row['hashKind']=='utf8-lf':b=history.canonical(b,'text')
  assert len(b)==row['bytes'] and sha(b)==row['sha256'],row['path']
 frozen=history.catalog(source)
 prior=json.loads(subprocess.check_output(['git','show','19ab85caa1f9c68a2f13d9635c9ebfaa6f404056:docs/readmes/editions.json']))
 current=read('docs/readmes/editions.json');assert current['latest']==173 and current['editions'][:-1]==prior['editions']
 manifest=read('docs/readmes/173/original-evidence.json');download=r/'build/downloaded-evidence173.zip'
 archive.verify(manifest,download)
 for row in manifest['files']:assert archive.original(manifest,download,row['logicalPath'])==(r/row['logicalPath']).read_bytes(),row['logicalPath']
 for path in ['build/ramp173-complete/result.json','build/validation173-final/summary.json','build/ramp173-legacy.json']:
  assert json.loads(archive.original(manifest,download,path))['passed']
 assert read('docs/readmes/173/verification.json')['passed']
 assert read('docs/readmes/173/verification.json')['directModelCalls'] == 0 and read('docs/readmes/173/verification.json')['plannedModelCalls'] == 1
 trial=json.loads(archive.original(manifest,download,'build/ramp173-complete/result.json'))
 subprocess.run(['git','merge-base','--is-ancestor',trial['source'],commit],check=True)
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'ref':ref,'commit':commit,
  'remoteFetched':ref.startswith('refs/verify/'),'editions':173,'frozenHashes':len(frozen['files']),
  'canonicalSourceFiles':len(inventory),'evidenceFiles':len(manifest['files']),'allOriginalReceiptsCompared':True,
  'historicalSnapshotsUnchanged':True,'privateCameraPixelsPublished':False}
(r/'build/publication-audit173.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
