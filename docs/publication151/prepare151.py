from pathlib import Path
import hashlib,json,subprocess,shutil
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2)+'\n').encode())
paths=set(json.loads((r/'docs/OBSERVATION_150_LOCK.json').read_bytes())['sourceHashes'])
paths.update(['workspace-engine/src/main/java/fraymus/workspace/'+n+'.java' for n in ['ReplayEvidence151','ReturnDiagnosis151','WorkspaceServer']])
paths.update(['workspace-desktop/main.cjs','workspace-desktop/renderer/association134-panel.js','docs/publication151/study151.cjs'])
paths.update(['workspace-engine/src/test/java/fraymus/workspace/'+n+'.java' for n in ['ReplayEvidence151Test','ReturnDiagnosis151Test']])
write(r/'docs/OBSERVATION_151_LOCK.json',{'edition':151,'baseline':'affcf65a94873f6630b2f076afe8a6791e59fd50','scope':'Diagnostic and memo method freeze before exposed app measurements. Original tracker algorithms and proof identities unchanged. No fresh footage claim.','sourceHashes':{p:sha((r/p).read_bytes().replace(b'\r\n',b'\n')) for p in sorted(paths)}})
original=json.loads((r/'docs/OBSERVATION_150_LOCK.json').read_bytes())['sourceHashes'];changed=[p for p,h in original.items() if sha((r/p).read_bytes().replace(b'\r\n',b'\n'))!=h]
write(r/'build/identity151-source-check.json',{'changedFrom150Lock':changed,'trackerSourcesUnchanged':all(not p.startswith('workspace-engine/src/main/java/') or p.endswith('WorkspaceServer.java') for p in changed)})
assert all(not p.startswith('workspace-engine/src/main/java/') or p.endswith('WorkspaceServer.java') for p in changed),changed
print(json.dumps({'locked':len(paths),'changedFrom150Lock':changed}))
