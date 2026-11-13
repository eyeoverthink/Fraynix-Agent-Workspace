from pathlib import Path
import json,subprocess,urllib.parse,hashlib,zipfile
folder=Path('build/vot139');spec=json.loads((folder/'extra-source.json').read_text(encoding='utf8'))['spec'];a=spec['channels']['color'];url=urllib.parse.urljoin('https://data.votchallenge.net/vot2018/longterm/',a['url']);p=folder/(spec['name']+'-color.zip')
assert not p.exists()
subprocess.run(['curl.exe','--fail','--location','--silent','--show-error','--output',str(p),url],check=True)
with p.open('rb') as f:h1=hashlib.file_digest(f,'sha1').hexdigest()
with p.open('rb') as f:h2=hashlib.file_digest(f,'sha256').hexdigest()
assert h1==a['checksum']
with zipfile.ZipFile(p) as z:assert z.testzip() is None;uncompressed=sum(x.file_size for x in z.infolist());assert uncompressed==a['uncompressed']
rows=json.loads((folder/'download.json').read_text(encoding='utf8'));rows.append({'name':spec['name'],'kind':'color','url':url,'file':p.as_posix(),'bytes':p.stat().st_size,'sha256':h2,'publishedSha1':a['checksum'],'actualSha1':h1,'publishedCompressedBytes':a['compressed'],'uncompressedBytes':uncompressed,'sizeMetadataMatchesArchive':a['compressed']==p.stat().st_size,'sizeNote':'Hash and ZIP CRC verified; metadata compressed size differs from full archive length.'});(folder/'download.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf8');print(json.dumps(rows[-1]))
