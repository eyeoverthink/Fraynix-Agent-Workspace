"""Download only the predeclared edition024 research members. Original data stays private.
Uses the existing verified range ZIP reader; no image transformations or new dependencies.
Fetch evaluation only AFTER the candidate source freeze, recorded in profile-024.json.
"""
import argparse,base64,concurrent.futures,hashlib,json,pathlib,runpy

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--split',choices=['development','evaluation'],required=True);ap.add_argument('--output',type=pathlib.Path,required=True);args=ap.parse_args()
 root=pathlib.Path(__file__).resolve().parent.parent
 protocol=json.loads((root/'workspace-engine/src/main/resources/perception/acceptance-024.json').read_text(encoding='utf-8'))
 if args.split=='evaluation' and not (root/'workspace-engine/src/main/resources/perception/profile-024.json').exists():raise ValueError('Freeze candidate and record source identities before fetching reserved images')
 old=runpy.run_path(str(root/'scripts/Prepare-TrackingResearch.py'));entries=old['index']();member=lambda n:old['member'](entries,n)
 args.output.mkdir(parents=True,exist_ok=True)
 for name in ['README.md','SOURCES.md']:
  p=args.output/('DAVIS-'+name)
  if not p.exists():p.write_bytes(member('DAVIS/'+name))
 exceptions=(args.output/'DAVIS-SOURCES.md').read_text(encoding='utf-8')
 for case in protocol['cases']:
  if case['split']!=args.split:continue
  seq=case['sequence']
  if any(line.split()[1]==seq for line in exceptions.splitlines() if line.startswith('* ')):raise ValueError('External-source exception needs its own license review')
  names=sorted(n for n in entries if n.startswith('DAVIS/JPEGImages/480p/'+seq+'/') and n.endswith('.jpg'))[:case['frames']]
  if len(names)<5:raise ValueError('Declared source has fewer than five frames')
  folder=args.output/seq;folder.mkdir(exist_ok=True)
  def fetch(name):
   mask=name.replace('/JPEGImages/','/Annotations/').replace('.jpg','.png');stem=pathlib.PurePosixPath(name).stem
   def obtain(suffix,source):
    p=folder/(stem+suffix)
    data=p.read_bytes() if p.exists() else member(source)
    # Match central-directory size and CRC even when resuming existing bytes.
    import binascii
    if len(data)!=entries[source]['bytes'] or binascii.crc32(data)!=entries[source]['crc']:raise ValueError('Existing/source member integrity changed')
    if not p.exists():p.write_bytes(data)
    return data
   image=obtain('.jpg',name);annotation=obtain('.png',mask)
   return {'sourceFrame':int(stem),'imageName':name,'maskName':mask,'imageSha256':hashlib.sha256(image).hexdigest(),'maskSha256':hashlib.sha256(annotation).hexdigest(),'image':base64.b64encode(image).decode(),'mask':base64.b64encode(annotation).decode()}
  with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:frames=list(pool.map(fetch,names))
  pack={'schema':'fraynix-tracking-sequence-v1','title':'DAVIS '+seq+' · consecutive024','sequence':seq,'split':case['split'],'objectId':case['objectId'],'source':old['URL'],'license':'CC BY-NC4.0; DAVIS authors; research use','notes':'Original consecutive frames and indexed masks. First-frame human-guided box; no official mask score. Capture FPS is not asserted.','frames':frames}
  p=args.output/(seq+'.ftrack.json');text=json.dumps(pack,separators=(',',':'))
  if p.exists() and p.read_text(encoding='utf-8')!=text:raise ValueError('Existing pack differs; preserve it and choose a new output folder')
  p.write_text(text,encoding='utf-8');print(json.dumps({'path':str(p.resolve()),'frames':len(frames),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}),flush=True)
if __name__=='__main__':main()
