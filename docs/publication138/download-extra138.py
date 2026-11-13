from pathlib import Path
import urllib.request,json,zipfile,hashlib,datetime
r=Path.cwd();folder=r/'build/otb138';name='Liquor';url='https://drive.usercontent.google.com/download?id=1npRoxKorlgVqnf7fYtDkPrJ4pPBLQO1u&export=download&confirm=t';p=folder/(name+'.zip');assert not p.exists()
with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Fraynix research evaluation'}),timeout=90) as response:p.write_bytes(response.read())
b=p.read_bytes();z=zipfile.ZipFile(p);names=[n for n in z.namelist() if not n.startswith('__MACOSX/') and not Path(n).name.startswith('._')]
record={'name':name,'url':url,'sourcePage':'https://rvlab.snu.ac.kr/research/tracker_benchmark','sha256':hashlib.sha256(b).hexdigest(),'bytes':len(b),'images':sum(n.lower().endswith('.jpg') for n in names),'groundtruthFiles':[n for n in names if 'groundtruth' in n],'downloadedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat()}
records=json.loads((folder/'download.json').read_text());records.append(record);(folder/'download.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf8');print(json.dumps(record))
