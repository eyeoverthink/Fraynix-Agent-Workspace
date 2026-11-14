from pathlib import Path
import json,zipfile
from select141 import rows,finite,measurable
r=Path.cwd();folder=r/'build/vot141';meta=json.loads((r/'docs/publication141/vot-metadata-before-lock.json').read_text(encoding='utf8'));result=[]
for spec in meta['sequences']:
 if spec['name'] not in ['car3','following','person2']:continue
 z=zipfile.ZipFile(folder/(spec['name']+'-annotations.zip'));g=rows(z.read(next(n for n in z.namelist() if n.endswith('groundtruth.txt'))));events=[];i=0
 while i<len(g):
  if finite(g[i]):i+=1;continue
  a=i
  while i<len(g) and not finite(g[i]):i+=1
  b=i-1
  if b-a+1>=3 and i+10<=len(g) and all(finite(x) for x in g[i:i+10]):events.append({'firstAbsent':a+1,'lastAbsent':b+1,'seed':max(1,a+1-15),'seedMeasurable':measurable(g[max(0,a-15)],spec['width'],spec['height'])})
 result.append({'name':spec['name'],'events':events,'firstSeedMeasurable':measurable(g[0],spec['width'],spec['height'])})
(r/'build/annotation141-check.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
