from pathlib import Path
import json,collections,hashlib
r=Path('build/profile157');raw=(r/'samples.json').read_bytes();events=json.loads(raw)['recording']['events'];leaf=collections.Counter();inclusive=collections.Counter()
for e in events:
 frames=(e['values'].get('stackTrace') or {}).get('frames',[])
 names=[f["method"]["type"]["name"].replace('/','.')+'.'+f['method']['name'] for f in frames]
 if names:leaf[names[0]]+=1;inclusive.update(set(names))
report={'scope':'Sampled CPU stacks across one exposed complete156 audit plus setup/inventory; inclusive rows overlap; profiler overhead included. Sampling is not elapsed-phase accounting. No private image data.','sampleCount':len(events),'sourceSha256':hashlib.sha256(raw).hexdigest(),'leaf':leaf.most_common(20),'inclusive':inclusive.most_common(20)}
(r/'report.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print(json.dumps(report))
