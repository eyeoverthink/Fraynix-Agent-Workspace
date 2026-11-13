from pathlib import Path
import urllib.request,json,hashlib,zipfile,datetime
r=Path.cwd();folder=r/'build/otb138';folder.mkdir(exist_ok=False);records=[];sha=lambda b:hashlib.sha256(b).hexdigest()
lock=json.loads((r/'docs/OBSERVATION_138_LOCK.json').read_text())
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
for name,key in [('Skating2','1zMqJRHdrf_MnMKH6Rw7mmCzxMxObhzhw'),('Suv','17ZudGJuqc4VwNX85Ys81SjHOg5zAMu3i')]:
 url='https://drive.usercontent.google.com/download?id='+key+'&export=download&confirm=t';p=folder/(name+'.zip')
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Fraynix research evaluation'}),timeout=90) as response:p.write_bytes(response.read())
 raw=p.read_bytes();z=zipfile.ZipFile(p);names=[n for n in z.namelist() if not n.startswith('__MACOSX/') and not Path(n).name.startswith('._')]
 record={'name':name,'url':url,'sourcePage':'https://rvlab.snu.ac.kr/research/tracker_benchmark','sha256':sha(raw),'bytes':len(raw),'images':sum(n.lower().endswith('.jpg') for n in names),'groundtruthFiles':[n for n in names if 'groundtruth' in n],'downloadedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat()};records.append(record);print(json.dumps(record),flush=True)
 (folder/'download.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf8')
