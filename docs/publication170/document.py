"""Generate the guide, snapshots and continuity notes from the two trial sets."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess

root=Path.cwd()
out=root/'docs/readmes/170'
source_dir=root/'docs/publication170'
out.mkdir(exist_ok=True)
sha=lambda data:hashlib.sha256(data).hexdigest()

def read(name):
    return json.loads((root/name).read_bytes())

def write(path,value):
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf8')

def prepend(name,text):
    path=root/name
    content=path.read_text(encoding='utf-8-sig')
    if not content.startswith(text):
        path.write_text(text+content,encoding='utf8')

app=read('build/game170/result.json')
native=read('workspace-engine/build/multi-rail-navigation/result.json')
tests=read('build/validation170/summary.json')
baseline=read('build/baseline170.json')
identity=read('build/controller-identity170.json')
ci=read('build/ci170-first.json')
publication=read('build/evidence170-upload.json')
assert all(row['passed'] for row in [app,native,tests,baseline,identity])
assert baseline['existingRailWitnessesMatched'] and publication['download']['passed']
desktop=(root/'build/desktop170-tests.log').read_text(encoding='utf8')
archive=(root/'build/archive170-tests.log').read_text(encoding='utf8')
assert re.search(r'fail 0\b',desktop) and re.search(r'skipped 0\b',desktop)
assert archive.strip().endswith('OK')
desktop_count=int(re.search(r'tests (\d+)',desktop)[1])
archive_count=int(re.search(r'Ran (\d+) tests',archive)[1])
validation=(f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} selected workspace Java tests, "
            f"{desktop_count} desktop tests and {archive_count} archive tests passed. The workspace selection "
            "covers both trial sets, previous courses, reference reconstruction, durable level tasks and route boundaries.")
course_titles={'relay-switchyard-v1':'Relay Switchyard','relay-ridge-v1':'Relay Ridge'}
paces=['normal','steady','careful']
native_by_pace={(run['course'],run['pace']):run for run in native['runs']}
table=[]
summaries=[]
measurements=[]
charts=[]
checks=[]
for course in app['courses']:
    course_id=course['id']
    title=course_titles[course_id]
    assert [run['pace'] for run in course['runs']]==paces
    trials=[]
    chart_runs=[]
    visible=[run['final']['rails']['crossings'] for run in course['runs']]
    for i,row in enumerate(visible[0]):
        per_rail=[items[i] for items in visible]
        assert all(test['id']==row['id'] and test['measuredClearance']>.035 for test in per_rail)
        assert per_rail[0]['entrySpeed']-per_rail[1]['entrySpeed']>.10
        assert per_rail[1]['entrySpeed']-per_rail[2]['entrySpeed']>.07
        table.append('| '+title+' · '+row['name']+' | '+' | '.join(
            f"{test['entrySpeed']:.3f} / {test['measuredClearance']:.3f}" for test in per_rail)+' |')
    for run in course['runs']:
        old=native_by_pace[course_id,run['pace']]
        final=run['final']
        actual=final['rails']['crossings']
        bounded=old['final']['railNavigation']['crossings']
        assert [row['id'] for row in actual]==[row['id'] for row in bounded]
        assert final['player']['won'] and old['final']['player']['won']
        measurements.extend(row['measuredClearance'] for row in actual)
        trials.append({'pace':run['pace'],'nativeFinishObservedTick':old['final']['tick'],
                       'visibleFinishObservedTick':final['tick'],'crossings':actual,
                       'nativeCrossings':bounded,'launches':final['rails']['launches']})
        points=[]
        for point in old['trace']:
            player=point['player']
            points.append(dict(tick=point['tick'],x=player['x'],y=player['y'],
                onGround=player['onGround'],won=player['won'],pickups=len(point['rail']['crossings'])))
        chart_runs.append({'pace':run['pace'],'trace':points,'crossings':bounded})
    charts.append({'id':course_id,'title':title,'runs':chart_runs,'total':len(visible[0]),
                   'rails':[rail['bounds'] for rail in course['runs'][0]['final']['rails']['rails']]})
    summaries.append({'id':course_id,'title':title,'trials':trials})
    checks.append({'name':title+' visible AI Play','kind':'game','result':'3 / 3 beacon wins',
                   'evidence':f"{len(visible[0])*3} separate fixture clearances"})
assert len(measurements)==15 and len(native_by_pace)==6
minimum=min(measurements)
checks += [
    {'name':'Ridge grouped flight','kind':'game','result':'Two rails in one flight',
     'evidence':'Shared launch, separate overlap windows and minimum distances'},
    {'name':'Pace differences per rail','kind':'game','result':'Three measured entry speeds',
     'evidence':'The native and app controls pass independent comparisons'},
    {'name':'Existing course defaults','kind':'checks','result':'Exact prior crossing records matched',
     'evidence':'Circuit and Terraces replayed with their original assets and finish ticks'},
    {'name':'First below-margin attempt','kind':'checks','result':'Failed result retained',
     'evidence':'0.016 units on the second Ridge rail before the conservative correction'}
]
markdown=(source_dir/'README-template.md').read_text(encoding='utf8')
markdown=markdown.replace('@@TESTS@@',validation).replace('@@TABLE@@','\n'.join(table))
assert '@@' not in markdown
(root/'README-170.md').write_text(markdown,encoding='utf8')
for source,target in [
    ('build/evidence170-manifest.json','original-evidence.json'),
    ('build/evidence170-upload.json','evidence-publication.json'),
    ('build/baseline170.json','baseline.json'),
    ('build/controller-identity170.json','controller-identity.json')]:
    shutil.copy2(root/source,out/target)
verification={'passed':True,'source':app['source'],'rootJava':tests['rootJava'],
              'workspaceJava':tests['workspaceJava'],'desktop':desktop_count,'archiveTests':archive_count,
              'courses':summaries,'visibleCrossings':len(measurements),'nativeCrossings':15,
              'minimumVisibleClearance':minimum,'ciFirst':ci,'coldRestart':app['coldRestart'],
              'legacyDefaults':app['legacyDefaults']}
write(out/'verification.json',verification)
shots=[]
for shot in app['captures']:
    raw=root/'build/game170'/shot['name']
    assert sha(raw.read_bytes())==shot['sha256']
    shutil.copy2(raw,out/raw.name)
    shots.append({'path':'docs/readmes/170/'+raw.name,
                  'title':'18 Nov 2026 · '+raw.stem.replace('-',' ').title()})
modules=[
    {'id':'rail','name':'Rail and pace controller','source':'src/main/java/agent/RailNavigation.java',
     'description':'Reads each fixture, forecasts a grouped flight and records the actual speed and clearance.'},
    {'id':'course','name':'Original course generator','source':'src/main/java/content/RelayCourseGenerator.java',
     'description':'Reproduces the authored layouts under their original content manifests.'},
    {'id':'test','name':'Native three-pace tests',
     'source':'workspace-engine/src/test/java/fraymus/workspace/OriginalMultiRailTest.java',
     'description':'Requires separate measurements for five fixtures and real beacon contact at all paces.'}
]
system={'finding':'Two new courses finished at three distinct measured paces.','validation':validation,
        'courses':charts,'paces':[{'id':p,'label':p.title()} for p in paces],
        'checks':checks,'screenshots':shots,'modules':modules}
write(out/'system-map.json',system)
progress=(f"Switchyard and Ridge reached their beacons at all three paces. Each of the native and visible app "
          f"trial sets verified 15 crossings. The lowest visible measured clearance was {minimum:.3f} game units.")
note=('## Edition 170 — verified original multi-rail courses\n\n2026-11-18. '+progress+' '+validation+
      ' Ridge uses one jump for two independently verified rails. Entry speed differs at every rail. The first '
      'below-margin run is retained. Existing course bytes and the two prior exact native outcomes matched. '
      'Read README-170.md and docs/publication170/plan.md. The checkout is the canonical Java project and trial '
      'projects are separate from private data. Next, attach verified crossing receipts to durable playtesting.\n\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md','docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md',
             'docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
    prepend(name,note)
(root/'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 170 — two multi-rail courses at three measured paces](README-170.md) · '
    '[Previous edition](README-169.md) · [Offline trajectory explorer](docs/readmes/170/index.html) · '
    '[Full history](docs/readmes/INDEX.md).\n\n'+progress+'\n\n'+validation+'\n',encoding='utf8')
index=root/'docs/readmes/INDEX.md'
s=index.read_text(encoding='utf8')
if 'README-170.md' not in s:
    index.write_text(s.replace('\n','\n\n[Edition 170 — two original multi-rail courses](../../README-170.md) · '
                     '[Guide](170/index.html)\n',1),encoding='utf8')
(root/'docs/obsidian/178-multi-rail.md').write_text(
    '# Verified multi-rail courses\n\n'+progress+'\n\n'+validation+
    '\n\n[Edition 170](../../README-170.md) · [Playthrough explorer](../readmes/170/index.html) · '
    '[Outcomes](../readmes/170/verification.json) · [Existing-course comparison](../readmes/170/baseline.json)\n',
    encoding='utf8')
nodes=[{'id':'edition','type':'file','file':'README-170.md','x':0,'y':0,'width':480,'height':400},
       {'id':'rail','type':'file','file':modules[0]['source'],'x':520,'y':0,'width':460,'height':380},
       {'id':'receipts','type':'file','file':'docs/readmes/170/verification.json','x':520,'y':420,'width':460,'height':280}]
edges=[{'id':name,'fromNode':'edition','fromSide':'right','toNode':name,'toSide':'left'}
       for name in ['rail','receipts']]
write(root/'docs/obsidian/178-multi-rail.canvas',{'nodes':nodes,'edges':edges})
prepend('docs/obsidian/00-home.md','[Edition 170 — verified multi-rail courses](178-multi-rail.md)\n\n')

# Capture source bytes after updating the architecture and continuity notes.
paths={row['path'] for row in read('docs/readmes/169/file-inventory.json')}
paths.update(subprocess.check_output(['git','diff','--name-only','20399ecaf',app['source']],text=True).splitlines())
paths.update(file.relative_to(root).as_posix() for file in source_dir.iterdir() if file.is_file())
paths.add('docs/MULTI_RAIL_170_LOCK.json')
rows=[]
for name in sorted(paths):
    raw=(root/name).read_bytes()
    kind='binary'
    if Path(name).suffix not in {'.png','.wav','.gz','.bin','.jpg','.zip'}:
        try:
            raw=raw.decode('utf-8-sig').replace(chr(13)+chr(10),chr(10)).encode()
            kind='utf8-lf'
        except UnicodeDecodeError:
            pass
    rows.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',rows)
html=(source_dir/'guide-template.html').read_text(encoding='utf8')
for marker,data in [('SYSTEM',system),('INVENTORY',rows)]:
    html=html.replace('@@'+marker+'@@',json.dumps(data).replace('</','<\\/'))
(out/'index.html').write_text(html,encoding='utf8')
shutil.copy2(source_dir/'guide.js',out/'guide.js')
print(json.dumps({'passed':True,'inventory':len(rows),'screenshots':len(shots),
                  'crossings':len(measurements),'minimumClearance':minimum}))
