from pathlib import Path
import hashlib,json,subprocess,zipfile
r=Path.cwd()
names=subprocess.check_output(['git','ls-files'],text=True).splitlines()
selected=[n for n in names if (n.startswith(('src/','workspace-engine/src/','assets/original/'))
    or n in ['build.gradle','workspace-engine/build.gradle','scripts/history_archive.py','scripts/test_history_archive.py',
    'docs/publication167/study167.cjs','docs/publication167/archive167.py','docs/publication167/validate167.py']
    or n.startswith('workspace-desktop/') and (n.endswith('.cjs') and '/test/' not in n or '/renderer/' in n))]
binary=[]
def canonical(n):
 b=(r/n).read_bytes()
 try:
  if Path(n).suffix in ['.png','.wav','.gz','.bin','.jpg','.zip']:raise UnicodeDecodeError('utf8',b,0,1,'binary asset')
  return b.decode('utf8').replace('\r\n','\n').encode()
 except UnicodeDecodeError:binary.append(n);return b
value={'baseline':'ded3c91febc277d217a311801b2f08835a193c3e',
 'scope':'Original content and explicit launch path; existing simulation/controller defaults preserved.',
 'sourceHashes':{n:hashlib.sha256(canonical(n)).hexdigest() for n in sorted(selected)},'binaryPaths':binary}
(r/'docs/ORIGINAL_CONTENT_167_LOCK.json').write_text(json.dumps(value,indent=2)+'\n')
# Compare actual compiled controller/physics classes with the preserved edition166 candidate.
before=r/'build/migration166/clone/build/libs/Fraynix-Agent-Workspace-0.1.0.jar'
after=r/'build/libs/Fraynix-Agent-Workspace-0.1.0.jar'
with zipfile.ZipFile(before) as old, zipfile.ZipFile(after) as new:
 classes=[n for n in old.namelist() if n.endswith('.class') and n.startswith(('agent/','physics2d/'))]
 classes+=['components/PlayerController.class','components/GameCamera.class','components/StateMachine.class']
 for n in classes:assert old.read(n)==new.read(n),n
 receipt={'passed':True,'baseline':'edition166 local candidate 63769d3d5','classes':classes,
          'hashes':{n:hashlib.sha256(new.read(n)).hexdigest() for n in classes}}
 (r/'build/controller-identity167.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({'lockedSources':len(selected),'unchangedCompiledClasses':len(classes)}))
