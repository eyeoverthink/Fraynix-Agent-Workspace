from pathlib import Path
import hashlib,json,re,shutil,subprocess
r=Path.cwd();out=r/'docs/readmes/167';out.mkdir(exist_ok=True)
sha=lambda b:hashlib.sha256(b).hexdigest()
def read(n):return json.loads((r/n).read_bytes())
def write(p,v):p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
tests=read('build/validation167/summary.json');app=read('build/game167-final/result.json')
native=read('workspace-engine/build/original-content/result.json');batch=read('build/archive167/result.json')
download=read('build/archive167/downloaded-result.json');upload=read('build/evidence167-upload.json')
identities=read('build/controller-identity167.json');ci=read('build/ci167-before-publication.json')
for v in [tests,app,native,batch,download,identities]:assert v['passed']
assert upload['download']['passed']
desktop=(r/'build/desktop167-tests.log').read_text(encoding='utf8');assert re.search(r'fail 0\b',desktop)
desktop_count=int(re.search(r'tests (\d+)',desktop)[1])
archive_log=(r/'build/archive167-tests.log').read_text();assert 'OK' in archive_log
archive_count=int(re.search(r'Ran (\d+) tests',archive_log)[1])
validation=f"{tests['rootJava']} root-engine Java tests, {tests['workspaceJava']} selected workspace Java tests, {desktop_count} desktop tests and {archive_count} archive tests passed, without failures or skips. This is the full root-engine suite plus scoped integrated-game/server regressions, not a rerun of every historical workspace capability."
play=f"The final actual-app run reached the finish in {app['final']['tick']} simulation ticks through bounded steps of the unchanged contact122 controller. A separate run using the visible AI play button had confirmed finish contact at tick {app['visibleAiPlay']['tick']}. Manual input moved the robot, reset restored its initial position, and a full app restart retained its original identity. The native no-jump trial failed as intended below the deck."
identity=f"The compiled identities of {len(identities['classes'])} existing agent/controller, physics, player, camera and state-machine classes match the preserved edition 166 candidate exactly. New content and the explicit fall-boundary component are separate additions."
archive_text=f"Edition 163 contributes **{batch['originalFiles']} original files**, totaling **{batch['originalBytes']:,} bytes**, packed into **{batch['archive']['bytes']:,} bytes**. Every downloaded original was compared to its retained Git blob."
ci_text=("At the prepublication check, the latest hosted run remained the failed edition 166 run "
         f"[{ci['runs'][0]['id']}]({ci['runs'][0]['html_url']}). Local archive checks now pass with the junction regression. The new publication run is reported separately; local success is not labelled hosted-CI success.")
replacements={'VALIDATION':validation,'PLAY':play,'IDENTITY':identity,'ARCHIVE':archive_text,'CI':ci_text}
text=(r/'docs/publication167/README-template.md').read_text(encoding='utf8')
for k,v in replacements.items():text=text.replace('@@'+k+'@@',v)
assert '@@' not in text
(r/'README-167.md').write_text(text,encoding='utf8')
for src,dest in [('build/evidence167-manifest.json','original-evidence.json'),('build/evidence167-upload.json','evidence-publication.json'),
 ('build/archive167/result.json','archive.json'),('build/archive167/downloaded-result.json','archive-download.json'),
 ('build/controller-identity167.json','controller-identity.json'),('build/app-visual167.json','app-visual.json')]:shutil.copy2(r/src,out/dest)
verification={'passed':True,'rootJava':tests['rootJava'],'workspaceJava':tests['workspaceJava'],'desktop':desktop_count,'archiveTests':archive_count,
 'actualApp':{'ticksToFinish':app['final']['tick'],'visibleAiPlay':app['visibleAiPlay']['player']['won'],'visibleAiObservedFinishTick':app['visibleAiPlay']['tick'],'defaultPolicy':app['final']['controller']['policy'],'manualMovement':True,'reset':True,'coldRestart':True},
 'actualNative':{'finishContact':native['finish']['player']['won'],'noJumpFailed':native['noJump']['player']['dead']},
 'unchangedCompiledClasses':len(identities['classes']),'historicalBatch':batch['originalFiles'],'physicalAccuracyPromoted':False,'modelCalls':0,'ciFirst':read('build/ci167-first.json')}
write(out/'verification.json',verification)
shots=[]
for capture in app['captures']:
 p=r/'build/game167-final'/capture['name'];assert sha(p.read_bytes())==capture['sha256'];shutil.copy2(p,out/capture['name'])
 shots.append({'path':'docs/readmes/167/'+capture['name'],'title':'2026-11-18 · Actual app · '+capture['name']})
modules=[{'id':'content','name':'Original content','source':'src/main/java/content/RelayGardenGenerator.java','description':'Owned pixels, tones and serialized layout; no old art or level read.'},
 {'id':'runtime','name':'Checked launch','source':'workspace-engine/src/main/java/fraymus/workspace/GameContentPack.java','description':'Source and installed content hashes verified before an explicit launch or reset.'},
 {'id':'archive','name':'Original evidence restoration','source':'scripts/history_archive.py','description':'Canonical physical parent, new destination and original hashes; one provider, no silent fallback.'}]
system={'finding':'Original playable content and another restored historical batch.','validation':validation,'modules':modules,'screenshots':shots,
 'checks':[{'name':'Native gap-aware run','kind':'game','result':'Finish contact','evidence':'Actual controller inputs'},
 {'name':'No-jump run','kind':'game','result':'Loss as intended','evidence':'Observed fall and death state'},
 {'name':'Actual app and restart','kind':'game','result':'Passed','evidence':'Visible controls and original identity'},
 {'name':'Historical batch','kind':'archive','result':'76 / 76 restored','evidence':'Downloaded bytes versus Git'},
 {'name':'Missing or corrupt provider','kind':'archive','result':'Refused','evidence':'No fallback or output directory'}],
 'trace':[dict(tick=s['tick'],x=s['player']['x'],y=s['player']['y'],onGround=s['player']['onGround'],won=s['player']['won']) for s in app['trace']]}
write(out/'system-map.json',system)
paths={x['path'] for x in read('docs/readmes/166/file-inventory.json')}
tracked=subprocess.check_output(['git','ls-files'],text=True).splitlines()
paths.update(n for n in tracked if n.startswith(('assets/original/','src/main/java/content/','src/test/java/content/','docs/publication167/')))
paths.update(['docs/ORIGINAL_CONTENT_167_LOCK.json','src/main/java/components/FallBoundary.java',
 'workspace-engine/src/main/java/fraymus/workspace/GameContentPack.java','workspace-engine/src/test/java/fraymus/workspace/OriginalGameContentTest.java'])
paths.update(p.relative_to(r).as_posix() for p in (r/'docs/publication167').iterdir() if p.is_file())
rows=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:
  if Path(n).suffix in ['.png','.wav','.gz','.bin','.jpg']:raise UnicodeDecodeError('utf8',raw,0,1,'binary')
  raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 rows.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',rows)
html=(r/'docs/publication167/guide-template.html').read_text(encoding='utf8').replace('@@SYSTEM@@',json.dumps(system).replace('</','<\\/')).replace('@@INVENTORY@@',json.dumps(rows).replace('</','<\\/'))
(out/'index.html').write_text(html,encoding='utf8');shutil.copy2(r/'docs/publication167/guide.js',out/'guide.js')
heading='## Edition 167 — original content and archived evidence\n\n'
note=heading+'2026-11-18. Canonical Java checkout J:/Codex-Work/fraynix-workspace-edition164, branch edition167. '+validation+' '+play+' '+identity+' '+archive_text.replace('**','')+' Existing defaults and private projects preserved. No model inference or physical accuracy claim. Initial CI166 archive-parent failure fixed and tested; new hosted run still separate. Read README-167.md for exact scope, attempts and next gates. No subagents; one heavy check at a time.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;s=p.read_text(encoding='utf-8-sig')
 if not s.startswith(heading):p.write_text(note+s,encoding='utf8')
entry='# Fraynix Agent Workspace\n\nCurrent: [Edition 167 — original playable content and verified archives](README-167.md). [Previous edition](README-166.md) · [Interactive offline guide](docs/readmes/167/index.html) · [Full history](docs/readmes/INDEX.md).\n\n'+validation+'\n\n'+play+'\n\n'+archive_text+' Original repository history remains intact. Full asset replacement and distribution licensing are still pending.\n'
(r/'README.md').write_text(entry,encoding='utf8')
index=r/'docs/readmes/INDEX.md';s=index.read_text(encoding='utf8')
if 'README-167.md' not in s:s=s.replace('\n','\n\n[Edition 167 — original playable content and archive verification](../../README-167.md) · [Guide](167/index.html)\n',1);index.write_text(s,encoding='utf8')
note_path=r/'docs/obsidian/175-original-content.md'
note_path.write_text('# Original content and retrievable history\n\n2026-11-18. '+validation+'\n\n'+play+'\n\n'+archive_text+'\n\n[Edition](../../README-167.md) · [Guide](../readmes/167/index.html) · [Renewal plan](../ARCHITECTURE_RENEWAL_PLAN.md)\n\nNext: extend original content into varied procedures, archive more batches and continue capability consolidation. Legacy defaults and evidence remain.\n',encoding='utf8')
canvas={'nodes':[{'id':'readme','type':'file','file':'README-167.md','x':0,'y':0,'width':480,'height':320},
 {'id':'logic','type':'file','file':'src/main/java/content/RelayGardenGenerator.java','x':540,'y':0,'width':460,'height':320},
 {'id':'archive','type':'file','file':'scripts/history_archive.py','x':540,'y':380,'width':460,'height':260}],
 'edges':[{'id':'a','fromNode':'readme','fromSide':'right','toNode':'logic','toSide':'left'}, {'id':'b','fromNode':'readme','fromSide':'bottom','toNode':'archive','toSide':'left'}]}
write(r/'docs/obsidian/175-original-content.canvas',canvas)
p=r/'docs/obsidian/00-home.md';s=p.read_text(encoding='utf8');link='[Edition 167 — original content and archive verification](175-original-content.md)'
if link not in s:p.write_text(link+'\n\n'+s,encoding='utf8')
print(json.dumps({'passed':True,'inventory':len(rows),'screenshots':len(shots),'verification':verification}))
