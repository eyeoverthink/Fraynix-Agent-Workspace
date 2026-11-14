from pathlib import Path
import urllib.request,json,hashlib,zipfile,datetime
r=Path.cwd();folder=r/'build/otb142';folder.mkdir(exist_ok=False);out=r/'docs/publication142';sha=lambda b:hashlib.sha256(b).hexdigest()
lock=json.loads((r/'docs/OBSERVATION_142_LOCK.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
records=[]
for name,key in [('FaceOcc2','1wMGWUtiGqnFMbFBFLb-hgnCWzezYRCgg'),('Walking2','1VomZdpWhB-3n5ZlWoM0oGITHzSGhPH9G')]:
 url='https://drive.usercontent.google.com/download?id='+key+'&export=download&confirm=t';p=folder/(name+'.zip')
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Fraynix research evaluation'}),timeout=90) as response:
  with p.open('xb') as f:
   while chunk:=response.read(1024*1024):f.write(chunk)
 z=zipfile.ZipFile(p);assert z.testzip() is None;names=[n for n in z.namelist() if not n.startswith('__MACOSX/') and not Path(n).name.startswith('._')];gts=[n for n in names if 'groundtruth' in n.lower()];assert len(gts)==1,(name,gts)
 gt=z.read(gts[0]);(out/(name+'-groundtruth.txt')).write_bytes(gt)
 records.append({'name':name,'url':url,'sourcePage':'https://rvlab.snu.ac.kr/research/tracker_benchmark/','sha256':sha(p.read_bytes()),'bytes':p.stat().st_size,'crcVerified':True,'groundtruth':gts[0],'groundtruthSha256':sha(gt),'downloadedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat()});(folder/'download.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf8');print(json.dumps(records[-1]),flush=True)
