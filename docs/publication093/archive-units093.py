from pathlib import Path
import json,gzip,hashlib,re,os
r=Path.cwd();d=r/'docs/readmes/093';p=d/'original-evidence.json';a=json.loads(p.read_text(encoding='utf8'))
def sha(b):return hashlib.sha256(b).hexdigest()
count=0
for xml in (r/'build/regression093').glob('TEST-*.xml'):
 for match in re.finditer(r'(OUTCOME091_NATIVE|WORLD079_NATIVE|RECOVERY093_NATIVE_COPY) ([^\r\n<]+)',xml.read_text(encoding='utf8')):
  root=Path(match.group(2).strip());assert root.is_relative_to(r/'build')
  for f in root.rglob('*.json'):
   raw=(Path('\\\\?\\'+str(f)) if os.name=='nt' else f).read_bytes();h=sha(raw);z=gzip.compress(raw,mtime=0);dest=d/'original'/(h+'.gz');dest.write_bytes(z)
   a['files'].append({'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h,'logicalPath':'native-unit/'+match.group(1)+'/'+f.relative_to(root).as_posix()});count+=1
a['distinctNewPayloads']=len({x['path'] for x in a['files']});p.write_text(json.dumps(a,indent=2)+'\n',encoding='utf8');print('Archived native unit records:',count)
