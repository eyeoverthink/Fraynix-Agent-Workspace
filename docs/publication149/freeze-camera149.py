from pathlib import Path
import hashlib,json,datetime,subprocess
r=Path.cwd();out=r/'docs/publication149';sha=lambda b:hashlib.sha256(b).hexdigest()
for name in ['webcam149-selftest','preflight149-tests']:
 assert json.loads((r/'build'/f'{name}.json').read_bytes())['passed'],name
assert json.loads((r/'build/webcam149-selftest.json').read_bytes())['recorderSha256']==sha((r/'workspace-desktop/test/webcam149-recorder.cjs').read_bytes().replace(b'\r\n',b'\n'))
assert json.loads((r/'build/preflight149-tests.json').read_bytes())['preflightSourceSha256']==sha((r/'docs/publication149/preflight149.py').read_bytes().replace(b'\r\n',b'\n'))
for p in (r/'build/webcam149-private').glob('attempt-*/capture.json'):
 assert json.loads(p.read_bytes())['syntheticRecorderCheck'],'Freeze source protocol before real camera pixels'
files=['workspace-desktop/test/webcam149-recorder.cjs']+['docs/publication149/'+n for n in ['CAMERA_PROTOCOL_149.md','prepare-webcam149.py','review-webcam149.py','preflight149.py','test-preflight149.py','freeze-camera149.py']]
lock={'edition':149,'algorithmLockSha256':sha((r/'docs/OBSERVATION_149_LOCK.json').read_bytes()),'lockedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'productionCommit':'9090c62d','sourceHashes':{name:sha((r/name).read_bytes().replace(b'\r\n',b'\n')) for name in files},'scope':'User-authorized fresh long-cover displaced-return camera source, frozen before real preview. Recorder self-test uses synthetic camera only; no tracking during collection.'}
p=out/'webcam-lock149.json';assert not p.exists();p.write_bytes((json.dumps(lock,indent=2)+'\n').encode())
subprocess.run(['git','add',*files,'docs/publication149/webcam-lock149.json'],check=True)
subprocess.run(['git','diff','--cached','--check'],check=True)
subprocess.run(['git','-c','gc.auto=0','commit','-m','Freeze edition 149 fresh long-occlusion camera source protocol'],check=True)
subprocess.run(['node','scripts/Study-Build095.cjs','--stamp'],check=True)
print(json.dumps({'cameraSourceFrozen':True,'commit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()}))
