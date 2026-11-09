from pathlib import Path
import zipfile,json,hashlib,sys,datetime
r=Path.cwd();final='--final' in sys.argv
archives=list((r/'build/distributions').glob('*.zip'));assert len(archives)==1
p=archives[0]
with zipfile.ZipFile(p) as z:
 names=z.namelist();top=names[0].split('/')[0]+'/'
 if final:
  editions=json.loads((r/'docs/readmes/editions.json').read_text(encoding='utf8'));edition=next(e for e in editions['editions'] if e['number']==96)
  editionFiles={'README-096.md','docs/readmes/editions.json',*(f['path'] for f in edition['snapshotFiles'])}
  for name in sorted(editionFiles):
   assert z.read(top+name)==(r/name).read_bytes(),name
  mode=f'All {len(editionFiles)} final edition and manifest files compared byte for byte with the frozen checkout; initial complete archive CRC audit retained separately.'
 else:
  assert z.testzip() is None
  mode='Every archive entry decompressed and CRC checked; no content excluded.'
 stat=p.stat();digest=hashlib.file_digest(p.open('rb'),'sha256').hexdigest()
 record={'passed':True,'file':p.relative_to(r).as_posix(),'bytes':stat.st_size,'sha256':digest,'entries':len(names),'zip64Needed':stat.st_size>0xffffffff,'mode':mode,'checkedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat()}
 out=r/('build/final-zip096.json' if final else 'build/zip096.json');out.write_text(json.dumps(record,indent=2)+'\n',encoding='utf8');print(json.dumps(record))
