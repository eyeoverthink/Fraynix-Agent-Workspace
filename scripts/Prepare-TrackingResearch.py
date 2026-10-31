"""Fetch only the declared DAVIS frames through verified ZIP byte ranges.

Standard library only. Original images/masks stay outside Git. Research data is
CC BY-NC4.0; see the archive README and generated provenance, not the app license.
"""
import argparse,base64,binascii,concurrent.futures,hashlib,json,pathlib,struct,urllib.request,zlib

URL='https://data.vision.ee.ethz.ch/csergi/share/davis/DAVIS-2017-trainval-480p.zip'
ETAG='"31a3032d-54c6d0af8b4dc"';SIZE=832766765
def chunk(start,end):
 req=urllib.request.Request(URL,headers={'Range':f'bytes={start}-{end}','If-Range':ETAG})
 with urllib.request.urlopen(req,timeout=30) as response:
  if response.status!=206 or response.headers.get('Content-Range')!=f'bytes {start}-{end}/{SIZE}':raise ValueError('Archive range or version changed; do not silently use another dataset')
  data=response.read(end-start+2)
  if len(data)!=end-start+1:raise ValueError('Incomplete archive range')
  return data
def index():
 tail=chunk(SIZE-65536,SIZE-1);i=tail.rfind(b'PK\x05\x06');v=struct.unpack('<4s4H2LH',tail[i:i+22]);size,offset=v[5:7];data=chunk(offset,offset+size-1);entries={};p=0
 while p<len(data):
  v=struct.unpack('<4s6H3L5H2L',data[p:p+46])
  if v[0]!=b'PK\x01\x02':raise ValueError('Invalid central directory')
  name=data[p+46:p+46+v[10]].decode('utf-8');entries[name]={'method':v[4],'crc':v[7],'compressed':v[8],'bytes':v[9],'offset':v[16]};p+=46+v[10]+v[11]+v[12]
 return entries
def member(entries,name):
 e=entries[name];a=e['offset'];header=chunk(a,a+29);v=struct.unpack('<4s5H3L2H',header)
 if v[0]!=b'PK\x03\x04' or e['method'] not in (0,8) or e['bytes']>1000000:raise ValueError('Unsupported bounded archive member')
 start=a+30+v[9]+v[10];raw=chunk(start,start+e['compressed']-1);data=zlib.decompress(raw,-15) if e['method']==8 else raw
 if len(data)!=e['bytes'] or binascii.crc32(data)!=e['crc']:raise ValueError('Member checksum failed')
 return data
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--split',choices=['development','evaluation'],required=True);ap.add_argument('--output',type=pathlib.Path,required=True);args=ap.parse_args()
 root=pathlib.Path(__file__).resolve().parent.parent;protocol=json.loads((root/'workspace-engine/src/main/resources/perception/acceptance-023.json').read_text(encoding='utf-8'));entries=index();args.output.mkdir(parents=True,exist_ok=True)
 for name in ['README.md','SOURCES.md']:
  p=args.output/('DAVIS-'+name)
  if not p.exists():p.write_bytes(member(entries,'DAVIS/'+name))
 for case in protocol['cases']:
  if case['split']!=args.split:continue
  seq=case['sequence'];names=sorted(n for n in entries if n.startswith('DAVIS/JPEGImages/480p/'+seq+'/') and n.endswith('.jpg'));ordinals=[round(i*(len(names)-1)/(case['frames']-1)) for i in range(case['frames'])];folder=args.output/seq;folder.mkdir(exist_ok=True)
  def fetch(i):
   name=names[i];mask=name.replace('/JPEGImages/','/Annotations/').replace('.jpg','.png');image=member(entries,name);annotation=member(entries,mask);stem=pathlib.PurePosixPath(name).stem
   for suffix,data in [('.jpg',image),('.png',annotation)]:
    p=folder/(stem+suffix)
    if p.exists() and p.read_bytes()!=data:raise ValueError('Existing research bytes differ; preserve them and choose a new output folder')
    if not p.exists():p.write_bytes(data)
   return {'sourceFrame':i,'imageName':name,'maskName':mask,'imageSha256':hashlib.sha256(image).hexdigest(),'maskSha256':hashlib.sha256(annotation).hexdigest(),'image':base64.b64encode(image).decode(),'mask':base64.b64encode(annotation).decode()}
  with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:frames=list(pool.map(fetch,ordinals))
  pack={'schema':'fraynix-tracking-sequence-v1','title':'DAVIS '+seq,'sequence':seq,'split':case['split'],'objectId':case['objectId'],'source':URL,'license':'CC BY-NC4.0; DAVIS authors; research use','notes':'Original frames and masks. Box evaluation, not official mask benchmark. Original capture FPS unknown.','frames':frames}
  p=args.output/(seq+'.ftrack.json');text=json.dumps(pack,separators=(',',':'))
  if p.exists() and p.read_text(encoding='utf-8')!=text:raise ValueError('Existing pack differs; preserve it and choose a new output folder')
  p.write_text(text,encoding='utf-8');print(json.dumps({'path':str(p.resolve()),'frames':len(frames),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}),flush=True)
if __name__=='__main__':main()
