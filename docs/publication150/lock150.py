from pathlib import Path
import json,hashlib
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest()
prior=json.loads((r/'docs/OBSERVATION_149_LOCK.json').read_text())
allowed={'workspace-engine/src/main/java/fraymus/workspace/ObservationMethods129.java','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java','workspace-engine/src/main/java/fraymus/workspace/VerifiedAppearances135.java','workspace-engine/src/main/java/fraymus/workspace/WorkspaceServer.java','workspace-desktop/main.cjs','workspace-desktop/renderer/association134-panel.js','workspace-desktop/renderer/observation-panel.js'}
for name,h in prior['sourceHashes'].items():
 if name not in allowed:assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
names=set(prior['sourceHashes'])|allowed|{
 'workspace-engine/src/main/java/fraymus/workspace/StartingReference150.java',
 'workspace-engine/src/test/java/fraymus/workspace/StartingReference150Test.java',
 'workspace-engine/src/test/java/fraymus/workspace/StartingReference150FixturesTest.java',
 'workspace-desktop/recorder150.cjs','workspace-desktop/renderer/recorder150-timing.js',
 'workspace-desktop/test/recorder150.test.cjs','workspace-desktop/test/webcam150-recorder.cjs',
 'workspace-desktop/test/starting150-integration.cjs','workspace-desktop/package.json',
 'docs/publication150/restart150.cjs','docs/publication150/capture150.cjs','docs/publication150/PROTOCOL_150.md'}
lock={'edition':150,'baseline':'e5479b7193df57eb0fdfbc930bd87fe9a0e2cc70','previous149LockSha256':sha((r/'docs/OBSERVATION_149_LOCK.json').read_bytes()),'methods':['foreground-guard-139','path-confirmation-148','identity-monitor-149','identity-return-149'],'sourceHashes':{n:sha((r/n).read_bytes().replace(b'\r\n',b'\n')) for n in sorted(names)},'scope':'Explicit verified-start retention and timing. Existing149 tracker mathematics/defaults unchanged. Same independently verified starting reference for reference methods; simple139 control; original source/outcome/restart checks remain.'}
p=r/'docs/OBSERVATION_150_LOCK.json';p.write_text(json.dumps(lock,indent=2)+'\n',encoding='utf8',newline='\n')
p=r/'docs/publication150/source150.json';sources=json.loads(p.read_text());sources['algorithmLockSha256']=sha((r/'docs/OBSERVATION_150_LOCK.json').read_bytes());p.write_text(json.dumps(sources,indent=2)+'\n',encoding='utf8',newline='\n')
print(json.dumps({'locked':len(names),'previousTrackerMathUnchanged':True}))
