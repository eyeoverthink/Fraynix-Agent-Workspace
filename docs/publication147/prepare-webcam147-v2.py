"""Additional acquisition checks; delegates to the frozen source-only preparer."""
from pathlib import Path
import json,hashlib,runpy,sys
r=Path.cwd();out=r/'docs/publication147';root=r/'build/webcam147-private';sha=lambda b:hashlib.sha256(b).hexdigest()
lock=json.loads((out/'webcam-v2-lock147.json').read_bytes())
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
for source in json.loads((out/'webcam-labels147.json').read_bytes())['sources']:
 p=(root/source['attempt']).resolve();assert p.parent==root.resolve()
 capture=json.loads((p/'capture.json').read_bytes())
 assert capture.get('recorderRevision')==2 and capture.get('sampleIntervalS')==1
 assert capture.get('syntheticRecorderCheck') is False,'Synthetic recorder tests are not physical camera evidence'
 assert capture['complete'] and capture['closed']
 assert capture['events'][-1]['action']=='finished' and capture['events'][-1]['phase']=='returned'
 frames=capture['frames'];assert len(frames)>=62
 for a,b in zip(frames,frames[1:]):assert b['seconds']-a['seconds']>=1-1e-6,'Acquisition cadence changed'
sys.path.insert(0,str(out));runpy.run_path(str(out/'prepare-webcam147.py'),run_name='__main__')
p=out/'source147.json';manifest=json.loads(p.read_bytes());manifest['webcamRevision2LockSha256']=sha((out/'webcam-v2-lock147.json').read_bytes());manifest['rejectedPriorAttempts']='docs/publication147/webcam-attempts147.json';manifest['limitations'].append('Revision2 acquired about1sample/second, unlike the rejected original2Hz attempt; temporal difficulty differs.');p.write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
