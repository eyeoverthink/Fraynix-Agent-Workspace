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
 lock=read('docs/PAIRED_RAMP_174_LOCK.json')
 for n,h in lock['sourceHashes'].items():
  b=source.read(n);assert sha(b if n in lock['binaryPaths'] else history.canonical(b,'text'))==h,n
 inventory=read('docs/readmes/174/file-inventory.json')
 for row in inventory:
  b=source.read(row['path'])
  if row['hashKind']=='utf8-lf':b=history.canonical(b,'text')
  assert len(b)==row['bytes'] and sha(b)==row['sha256'],row['path']
 frozen=history.catalog(source)
 prior=json.loads(subprocess.check_output(['git','show','d3b207c3ea6b0bbaa758441de23c194d62b1177c:docs/readmes/editions.json']))
 current=read('docs/readmes/editions.json');assert current['latest']==174 and current['editions'][:-1]==prior['editions']
 manifest=read('docs/readmes/174/original-evidence.json');download=r/'build/downloaded-evidence174.zip'
 archive.verify(manifest,download)
 for row in manifest['files']:assert archive.original(manifest,download,row['logicalPath'])==(r/row['logicalPath']).read_bytes(),row['logicalPath']
 for path in ['build/ramp174-release/result.json','build/validation174-final/summary.json','build/ramp174-legacy.json','build/ramp174-planned-resume.json']:
  assert json.loads(archive.original(manifest,download,path))['passed']
 assert read('docs/readmes/174/verification.json')['passed']
 assert read('docs/readmes/174/verification.json')['correctRoutes'] == 6
 assert read('docs/readmes/174/verification.json')['correctModelRoutes'] == 5
 trial=json.loads(archive.original(manifest,download,'build/ramp174-release/result.json'))
 subprocess.run(['git','merge-base','--is-ancestor',trial['source'],commit],check=True)
 result={'passed':True,'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'ref':ref,'commit':commit,
  'remoteFetched':ref.startswith('refs/verify/'),'editions':174,'frozenHashes':len(frozen['files']),
  'canonicalSourceFiles':len(inventory),'evidenceFiles':len(manifest['files']),'allOriginalReceiptsCompared':True,
  'historicalSnapshotsUnchanged':True,'privateCameraPixelsPublished':False}
(r/'build/publication-audit174.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
