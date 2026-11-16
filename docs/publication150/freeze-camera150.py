from pathlib import Path
import hashlib,json,datetime,subprocess
r=Path.cwd();out=r/'docs/publication150';sha=lambda b:hashlib.sha256(b).hexdigest()
for name in ['webcam150-selftest','webcam150-timeout-selftest','preflight150-tests']:
 assert json.loads((r/'build'/f'{name}.json').read_bytes())['passed'],name
assert json.loads((r/'build/webcam150-selftest.json').read_bytes())['recorderSha256']==sha((r/'workspace-desktop/test/webcam150-recorder.cjs').read_bytes().replace(b'\r\n',b'\n'))
assert json.loads((r/'build/preflight150-tests.json').read_bytes())['preflightSourceSha256']==sha((r/'docs/publication150/preflight150.py').read_bytes().replace(b'\r\n',b'\n'))
for p in (r/'build/webcam150-private').glob('attempt-*/capture.json'):
 assert json.loads(p.read_bytes())['syntheticRecorderCheck'],'Freeze source protocol before real camera pixels'
files=['workspace-desktop/test/webcam150-recorder.cjs','workspace-desktop/recorder150.cjs','workspace-desktop/renderer/recorder150-timing.js']+['docs/publication150/'+n for n in ['CAMERA_PROTOCOL_150.md','prepare-webcam150.py','review-webcam150.py','preflight150.py','test-preflight150.py','freeze-camera150.py']]
lock={'edition':150,'algorithmLockSha256':sha((r/'docs/OBSERVATION_150_LOCK.json').read_bytes()),'lockedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'productionCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'sourceHashes':{name:sha((r/name).read_bytes().replace(b'\r\n',b'\n')) for name in files},'scope':'User-authorized fresh long-cover displaced-return camera source, frozen before real preview. Recorder self-test uses synthetic camera only; no tracking during collection.'}
p=out/'webcam-lock150.json';assert not p.exists();p.write_bytes((json.dumps(lock,indent=2)+'\n').encode())
subprocess.run(['git','add',*files,'docs/publication150/webcam-lock150.json'],check=True)
subprocess.run(['git','diff','--cached','--check'],check=True)
subprocess.run(['git','-c','gc.auto=0','commit','-m','Freeze edition 150 fresh long-occlusion camera source protocol'],check=True)
subprocess.run(['node','scripts/Study-Build095.cjs','--stamp'],check=True)
print(json.dumps({'cameraSourceFrozen':True,'commit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()}))
