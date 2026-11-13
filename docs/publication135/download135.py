from pathlib import Path
import urllib.request,json,hashlib,zipfile,datetime
r=Path.cwd();folder=r/'build/otb135';folder.mkdir(parents=True,exist_ok=True);records=[]
for name,key in [('Tiger2','1dZsl1xjXcOS18Pxgla-LSCWfee7wykgq'),('Box','1C75IzHEDuMBasLdZrdplLzUxodJHU2Ir')]:
 url='https://drive.usercontent.google.com/download?id='+key+'&export=download&confirm=t';p=folder/(name+'.zip');assert not p.exists()
 req=urllib.request.Request(url,headers={'User-Agent':'Fraynix research evaluation'})
 with urllib.request.urlopen(req,timeout=90) as response:p.write_bytes(response.read())
 raw=p.read_bytes();z=zipfile.ZipFile(p);images=sorted(n for n in z.namelist() if n.lower().endswith('.jpg') and not n.startswith('__MACOSX/') and not Path(n).name.startswith('._'));gt=[n for n in z.namelist() if 'groundtruth' in n]
 record={'name':name,'url':url,'sourcePage':'https://rvlab.snu.ac.kr/research/tracker_benchmark','sha256':hashlib.sha256(raw).hexdigest(),'bytes':len(raw),'images':len(images),'groundtruthFiles':gt,'downloadedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat()};records.append(record)
 print(json.dumps(record),flush=True)
 (folder/'download.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf8')
