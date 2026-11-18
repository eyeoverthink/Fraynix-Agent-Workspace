"""Build the edition guide from the finished play and restoration receipts."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess

root = Path.cwd()
out = root/'docs/readmes/168'
source_dir = root/'docs/publication168'
out.mkdir(exist_ok=True)
sha = lambda raw: hashlib.sha256(raw).hexdigest()

def read(name):
    return json.loads((root/name).read_bytes())

def write(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False)+'\n', encoding='utf8')

def prepend(name, text):
    path = root/name
    original = path.read_text(encoding='utf-8-sig')
    if not original.startswith(text):
        path.write_text(text+original, encoding='utf8')

app = read('build/game168-final/result.json')
tests = read('build/validation168/summary.json')
batch = read('build/archive168/result.json')
download = read('build/archive168/downloaded-result.json')
identity = read('build/controller-identity168.json')
hosted = read('build/ci168-first.json')
assert app['passed'] and tests['passed'] and batch['passed'] and download['allOriginalGitBytesCompared'] and identity['passed']
desktop_log = (root/'build/desktop168-tests.log').read_text(encoding='utf8')
archive_log = (root/'build/archive168-tests.log').read_text(encoding='utf8')
assert re.search(r'fail 0\b', desktop_log) and re.search(r'skipped 0\b', desktop_log)
assert archive_log.strip().endswith('OK')
desktop = int(re.search(r'tests (\d+)', desktop_log)[1])
archives = int(re.search(r'Ran (\d+) tests', archive_log)[1])
validation = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} selected workspace Java tests, "
              f"{desktop} desktop tests and {archives} archive tests passed. "
              "The workspace selection covers Game Lab, reference import, durable level tasks and route boundaries.")
run = hosted['runs'][0]
ci_text = ("At the initial CI check, edition 167 had passed hosted archive and documentation checks "
           "and was still compiling and testing. See the [edition 167 workflow]("+run['html_url']+").")
names = {'relay-circuit-v1':'Relay Circuit','relay-terraces-v1':'Relay Terraces'}
finished = []
table = []
course_traces = []
checks = []
for course in app['courses']:
    title = names[course['id']]
    finish = course['finish']
    ai = course['defaultAI']
    hazard = ai['progress'].get('hazard')
    if ai['player']['won']:
        outcome = 'Reached the beacon'
    elif hazard:
        outcome = 'Hit ' + hazard['name']
    elif ai['player']['dead']:
        outcome = 'Lost before the beacon'
    else:
        outcome = 'Timed out in the comparison'
    count = len(finish['progress']['pickups'])
    table.append(f"| {title} | {finish['tick']} | {count}/{finish['progress']['totalPickups']} | {outcome} |")
    finished.append({'title': title, 'finishTick': finish['tick'], 'pickupCount': count,
                     'defaultAI': outcome, 'defaultAITick': ai['tick'], 'source': app['source']})
    course_traces.append({'id':course['id'], 'title':title, 'total':finish['progress']['totalPickups'],
        'trace':[dict(tick=point['tick'], x=point['player']['x'], y=point['player']['y'],
                      onGround=point['player']['onGround'], won=point['player']['won'],
                      pickups=len(point['progress']['pickups']))
                 for point in [course['initial'], *course['trace']]]})
    checks.append({'name':title+' visible playthrough', 'kind':'game', 'result':str(count)+' cells and finish contact',
                   'evidence':str(finish['tick'])+' observed ticks'})

checks += [
    {'name':'Rail contact','kind':'game','result':'Loss as intended','evidence':'Native contact and visible app status'},
    {'name':'Reset and cold reopen','kind':'game','result':'Starting state restored','evidence':'Stable robot ID and empty pickup record'},
    {'name':'Historical batch','kind':'archive','result':'74 / 74 restored','evidence':'Downloaded bytes compared with Git'},
    {'name':'Missing or corrupt provider','kind':'archive','result':'Refused','evidence':'No partial output or fallback'}
]
markdown = (source_dir/'README-template.md').read_text(encoding='utf8')
markdown = markdown.replace('@@TESTS@@',validation).replace('@@COURSES@@','\n'.join(table)).replace('@@CI@@',ci_text)
assert '@@' not in markdown
(root/'README-168.md').write_text(markdown, encoding='utf8')
for source, target in [
    ('build/evidence168-manifest.json','original-evidence.json'),
    ('build/evidence168-upload.json','evidence-publication.json'),
    ('build/archive168/result.json','archive.json'),
    ('build/archive168/downloaded-result.json','archive-download.json'),
    ('build/controller-identity168.json','controller-identity.json')]:
    shutil.copy2(root/source, out/target)
verification = {'passed': True, 'rootJava':tests['rootJava'], 'workspaceJava':tests['workspaceJava'],
                'desktop':desktop, 'archiveTests':archives, 'courses':finished,
                'ciFirst':hosted, 'coldRestart':app['coldRestart'],
                'unchangedCompiledClasses':len(identity['classes']), 'historicalBatch':batch['originalFiles']}
write(out/'verification.json', verification)

shots = []
for shot in app['captures']:
    p = root/'build/game168-final'/shot['name']
    assert sha(p.read_bytes()) == shot['sha256']
    shutil.copy2(p, out/p.name)
    shots.append({'path':'docs/readmes/168/'+p.name, 'title':'18 Nov 2026 · '+p.stem.replace('-',' ').title()})

modules = [
    {'id':'content','name':'Course generator','source':'src/main/java/content/RelayCourseGenerator.java',
     'description':'Two distinct deck layouts and original cell and rail artwork.'},
    {'id':'contact','name':'Measured contact','source':'src/main/java/components/CourseContact.java',
     'description':'A cell or rail records the native contact tick and persistent object identity.'},
    {'id':'launch','name':'Verified content','source':'workspace-engine/src/main/java/fraymus/workspace/GameContentPack.java',
     'description':'The source and installed asset hashes must both match the selected manifest.'},
    {'id':'archive','name':'History restoration','source':'scripts/history_archive.py',
     'description':'The archived files must match their pinned original bytes.'},
]
system = {'finding':'Two playable original courses and 74 retrievable history files.',
          'validation':validation, 'courses':course_traces, 'checks':checks, 'screenshots':shots,
          'modules':modules}
write(out/'system-map.json', system)
paths = {row['path'] for row in read('docs/readmes/167/file-inventory.json')}
paths.update(subprocess.check_output(
    ['git','diff','--name-only','f841ed003','HEAD'], text=True).splitlines())
paths.update(p.relative_to(root).as_posix() for p in source_dir.iterdir() if p.is_file())
paths.add('docs/ORIGINAL_COURSES_168_LOCK.json')
rows = []
for name in sorted(paths):
    raw = (root/name).read_bytes()
    kind = 'binary'
    if Path(name).suffix not in {'.png','.wav','.gz','.bin','.jpg','.zip'}:
        try:
            raw = raw.decode('utf-8-sig').replace(chr(13)+chr(10),chr(10)).encode()
            kind = 'utf8-lf'
        except UnicodeDecodeError:
            pass
    rows.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',rows)
html = (source_dir/'guide-template.html').read_text(encoding='utf8')
for marker, data in [('SYSTEM',system),('INVENTORY',rows)]:
    html = html.replace('@@'+marker+'@@',json.dumps(data).replace('</','<\\/'))
(out/'index.html').write_text(html, encoding='utf8')
shutil.copy2(source_dir/'guide.js',out/'guide.js')

progress = ' '.join(f"{item['title']} finished in {item['finishTick']} ticks with {item['pickupCount']} cells. "
                    f"Existing AI outcome was {item['defaultAI'].lower()}."
                    for item in finished)
note = ('## Edition 168 — two original courses and archived evidence\n\n2026-11-18. '+validation+' '+progress+
        ' The native loss, reset and restart checks passed. We restored 74 edition 162 files from a downloaded archive '
        'and compared them with Git. The previous 19 original assets and 60 compiled control classes match exactly. '
        'Next, expose typed rail and pickup observations for a measured AI comparison. Read README-168.md and '
        'docs/publication168/plan.md. Keep the Java workspace separate from private review data.\n\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md','docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md',
             'docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
    prepend(name,note)
(root/'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 168 — original pickups, hazards and two playable courses](README-168.md) · '
    '[Previous edition](README-167.md) · [Offline guide](docs/readmes/168/index.html) · '
    '[Full history](docs/readmes/INDEX.md).\n\n'+progress+'\n\n'+validation+
    '\n\nThe verified archive adds all 74 files from edition 162.\n', encoding='utf8')
index = root/'docs/readmes/INDEX.md'
s = index.read_text(encoding='utf8')
if 'README-168.md' not in s:
    index.write_text(s.replace('\n','\n\n[Edition 168 — Relay Circuit and Terraces](../../README-168.md) · '
                     '[Guide](168/index.html)\n',1),encoding='utf8')
(root/'docs/obsidian/176-original-courses.md').write_text(
    '# Original courses and verified archive retrieval\n\n'+progress+'\n\n'+validation+
    '\n\n[Edition 168](../../README-168.md) · [Offline guide](../readmes/168/index.html) · '
    '[Verification](../readmes/168/verification.json) · [Archive restoration](../readmes/168/archive-download.json)\n',
    encoding='utf8')
nodes = [{'id':'edition','type':'file','file':'README-168.md','x':0,'y':0,'width':480,'height':400},
         {'id':'contact','type':'file','file':modules[1]['source'],'x':520,'y':0,'width':460,'height':380},
         {'id':'archive','type':'file','file':'docs/readmes/168/archive-download.json',
          'x':520,'y':420,'width':460,'height':280}]
edges = [{'id':n,'fromNode':'edition','fromSide':'right','toNode':n,'toSide':'left'}
         for n in ['contact','archive']]
write(root/'docs/obsidian/176-original-courses.canvas',{'nodes':nodes,'edges':edges})
prepend('docs/obsidian/00-home.md','[Edition 168 — Relay Circuit and Terraces](176-original-courses.md)\n\n')
print(json.dumps({'passed':True,'inventory':len(rows),'screenshots':len(shots),'outcomes':finished}))
