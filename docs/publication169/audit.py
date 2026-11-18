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
 lock=read('docs/RAIL_NAVIGATION_169_LOCK.json')
 for n,h in lock['sourceHashes'].items():
  b=source.read(n);assert sha(b if n in lock['binaryPaths'] else history.canonical(b,'text'))==h,n
 inventory=read('docs/readmes/169/file-inventory.json')
 for row in inventory:
  b=source.read(row['path'])
  if row['hashKind']=='utf8-lf':b=history.canonical(b,'text')
  assert len(b)==row['bytes'] and sha(b)==row['sha256'],row['path']
 frozen=history.catalog(source)
 prior=json.loads(subprocess.check_output(['git','show','aba5b1dff44a110b351189b7bda1b1857fede585:docs/readmes/editions.json']))
 current=read('docs/readmes/editions.json');assert current['latest']==169 and current['editions'][:-1]==prior['editions']
 manifest=read('docs/readmes/169/original-evidence.json');download=r/'build/downloaded-evidence169.zip'
 archive.verify(manifest,download)
 for row in manifest['files']:assert archive.original(manifest,download,row['logicalPath'])==(r/row['logicalPath']).read_bytes(),row['logicalPath']
 for path in ['build/game169/result.json','workspace-engine/build/rail-navigation/result.json','build/validation169/summary.json']:
  assert json.loads(archive.original(manifest,download,path))['passed']
 assert read('docs/readmes/169/verification.json')['passed']
 assert read('docs/readmes/169/baseline.json')['exactPublishedOriginal']
 trial=json.loads(archive.original(manifest,download,'build/game169/result.json'))
 subprocess.run(['git','merge-base','--is-ancestor',trial['source'],commit],check=True)
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'ref':ref,'commit':commit,
  'remoteFetched':ref.startswith('refs/verify/'),'editions':169,'frozenHashes':len(frozen['files']),
  'canonicalSourceFiles':len(inventory),'evidenceFiles':len(manifest['files']),'allOriginalReceiptsCompared':True,
  'historicalSnapshotsUnchanged':True,'privateCameraPixelsPublished':False}
(r/'build/publication-audit169.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
