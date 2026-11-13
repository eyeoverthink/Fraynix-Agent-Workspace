from pathlib import Path
import json,zipfile,subprocess,hashlib,math,urllib.parse,sys
r=Path.cwd();folder=r/'build/vot139';meta=json.loads((r/'docs/publication139/vot-metadata-before-lock.json').read_text(encoding='utf8'));rows=[]
for spec in meta['sequences'][int(sys.argv[1]) if len(sys.argv)>1 else 3:]:
 name=spec['name'];a=spec['annotations'];url=urllib.parse.urljoin('https://data.votchallenge.net/vot2018/longterm/',a['url']);p=folder/(name+'-annotations.zip')
 if not p.exists():subprocess.run(['curl.exe','--fail','--location','--silent','--show-error','--output',str(p),url],check=True)
 raw=p.read_bytes();assert hashlib.sha1(raw).hexdigest()==a['checksum'];z=zipfile.ZipFile(p);g=[list(map(float,s.split(','))) for s in z.read('groundtruth.txt').decode().splitlines()];finite=lambda i:all(math.isfinite(v) for v in g[i]);inside=lambda i:finite(i) and g[i][0]>=.02*spec['width'] and g[i][1]>=.02*spec['height'] and g[i][0]+g[i][2]<=.98*spec['width'] and g[i][1]+g[i][3]<=.98*spec['height'];events=[];i=0
 while i<len(g):
  if finite(i):i+=1;continue
  start=i
  while i<len(g) and not finite(i):i+=1
  end=i-1;following=0
  while i+following<len(g) and finite(i+following):following+=1
  seed=max(0,start-15)
  if end-start+1>=3 and following>=10 and start>0 and inside(start-1) and inside(i) and finite(seed) and min(g[seed][2]/spec['width'],g[seed][3]/spec['height'])>=.025:events.append({'start':start+1,'end':end+1,'followingPresent':following})
 rows.append({'name':name,'url':url,'sha256':hashlib.sha256(raw).hexdigest(),'publishedSha1':a['checksum'],'events':events});(folder/'occlusion-search.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf8')
 print(json.dumps(rows[-1]),flush=True)
 if events:
  (folder/'extra-source.json').write_text(json.dumps({'spec':spec,'event':events[0]},indent=2)+'\n',encoding='utf8');break
else:raise RuntimeError('No source qualified')
