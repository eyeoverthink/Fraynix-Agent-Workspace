from pathlib import Path
import json,urllib.request,urllib.parse,hashlib,time,datetime,subprocess,zipfile
r=Path.cwd();out=r/'build/vot139';out.mkdir(exist_ok=True);meta=json.loads((r/'docs/publication139/vot-metadata-before-lock.json').read_text(encoding='utf8'));rows=[]
assert (r/'docs/OBSERVATION_139_LOCK.json').exists()
for spec in meta['sequences'][:3]:
 for key,record in [('annotations',spec['annotations']),('color',spec['channels']['color'])]:
  url=urllib.parse.urljoin('https://data.votchallenge.net/vot2018/longterm/',record['url']);p=out/(spec['name']+'-'+key+'.zip');start=time.monotonic()
  if not p.exists():
   tmp=p.with_suffix('.download')
   subprocess.run(['curl.exe','--fail','--location','--silent','--show-error','--retry','2','--output',str(tmp),url],check=True)
   tmp.rename(p)
  b=p.read_bytes();actual=hashlib.sha1(b).hexdigest();row={'name':spec['name'],'kind':key,'url':url,'file':p.relative_to(r).as_posix(),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'publishedSha1':record['checksum'],'actualSha1':actual,'seconds':time.monotonic()-start,'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()};rows.append(row)
  (out/'download.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf8');print(json.dumps(row),flush=True)
  assert actual==record['checksum'],row
  with zipfile.ZipFile(p) as z:
   assert z.testzip() is None
   row['zipPayloadCompressedBytes']=sum(x.compress_size for x in z.infolist());row['publishedCompressedBytes']=record['compressed'];row['uncompressedBytes']=sum(x.file_size for x in z.infolist())
   assert row['uncompressedBytes']==record['uncompressed']
   row['sizeMetadataMatchesArchive']=record['compressed']==len(b)
   row['sizeNote']='Published SHA1, ZIP CRC and uncompressed size verified. Published compressed size differs from actual ZIP length when sizeMetadataMatchesArchive is false; exact bytes and both sizes retained.'
  (out/'download.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf8')
