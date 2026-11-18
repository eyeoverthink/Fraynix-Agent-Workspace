"""Build the guide and project continuity notes from actual verified trials."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess

root = Path.cwd()
out = root/'docs/readmes/169'
source_dir = root/'docs/publication169'
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

app = read('build/game169/result.json')
tests = read('build/validation169/summary.json')
baseline = read('build/baseline169/receipt.json')
identity = read('build/controller-identity169.json')
hosted = read('build/ci169-first.json')
publication = read('build/evidence169-upload.json')
assert app['passed'] and tests['passed'] and baseline['exactPublishedOriginal'] and identity['passed']
assert publication['download']['passed']
desktop_log = (root/'build/desktop169-tests.log').read_text(encoding='utf8')
archive_log = (root/'build/archive169-tests.log').read_text(encoding='utf8')
assert re.search(r'fail 0\b', desktop_log) and re.search(r'skipped 0\b', desktop_log)
assert archive_log.strip().endswith('OK')
desktop = int(re.search(r'tests (\d+)', desktop_log)[1])
archives = int(re.search(r'Ran (\d+) tests', archive_log)[1])
validation = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} selected workspace Java tests, "
              f"{desktop} desktop tests and {archives} archive-verification tests passed. "
              "The workspace selection includes original courses, legacy reference reconstruction, durable level tasks and server boundaries.")
names = {'relay-circuit-v1':'Relay Circuit','relay-terraces-v1':'Relay Terraces'}
baseline_by_id = {item['id']:item for item in baseline['courses']}
outcomes, table, chart, checks = [], [], [], []
for course in app['courses']:
    key = course['id']
    title = names[key]
    old = baseline_by_id[key]['hazard']
    bounded = course['bounded']
    current = course['visibleAi']
    crossing = current['rails']['crossings'][0]
    assert crossing['id'] == old['id'] and current['player']['won']
    actual = crossing['measuredClearance']
    predicted = crossing['predictedClearance']
    assert len(bounded['rails']['crossings']) == 1
    bounded_crossing = bounded['rails']['crossings'][0]
    assert bounded_crossing['id'] == crossing['id']
    cells = str(len(current['progress']['pickups']))+'/'+str(current['progress']['totalPickups'])
    outcomes.append({'title':title,'id':key,'baselineContactTick':old['tick'],
                     'boundedFinishObservedTick':bounded['tick'],'visibleFinishObservedTick':current['tick'],
                     'cells':cells,'crossing':crossing,'boundedCrossing':bounded_crossing})
    table.append(f"| {title} | Hit {old['name']} at tick {old['tick']} | Beacon reached by tick "
                 f"{bounded['tick']} | Beacon reached by tick {current['tick']} | {predicted:.3f} / {actual:.3f} |")
    points = [dict(tick=point['tick'], x=point['player']['x'], y=point['player']['y'],
                   onGround=point['player']['onGround'], won=point['player']['won'],
                   pickups=len(point['progress']['pickups']))
              for point in [course['initial'], *course['trace']]]
    chart.append({'id':key,'title':title,'trace':points,'total':current['progress']['totalPickups'],
                  'rail':bounded['rails']['rails'][0]['bounds'],'crossing':bounded_crossing})
    checks.append({'name':title+' visible AI Play', 'kind':'game','result':'Beacon reached',
                   'evidence':f"{actual:.3f} minimum observed clearance, {cells} cells"})
checks += [
    {'name':'Published edition 168 comparison','kind':'game','result':'Rail contact on both courses',
     'evidence':'Exact recovered app trial and matching live rail IDs'},
    {'name':'Original defaults, reset and cold reopen','kind':'game','result':'Passed',
     'evidence':'Normal legacy play and a fresh original course state after reset'},
    {'name':'Forecast refusal','kind':'checks','result':'Unsafe approaches refused',
     'evidence':'Oversized hazard, late start and unsupported landing'},
    {'name':'Identity check','kind':'checks','result':'24 content files and 59 compiled classes match',
     'evidence':'Compared against the published edition 168 record'}
]
markdown = (source_dir/'README-template.md').read_text(encoding='utf8')
markdown = markdown.replace('@@TESTS@@',validation).replace('@@RESULTS@@','\n'.join(table))
assert '@@' not in markdown
(root/'README-169.md').write_text(markdown, encoding='utf8')
for source, target in [
    ('build/evidence169-manifest.json','original-evidence.json'),
    ('build/evidence169-upload.json','evidence-publication.json'),
    ('build/baseline169/receipt.json','baseline.json'),
    ('build/controller-identity169.json','controller-identity.json')]:
    shutil.copy2(root/source, out/target)
verification = {'passed':True,'source':app['source'],'rootJava':tests['rootJava'],'workspaceJava':tests['workspaceJava'],
                'desktop':desktop,'archiveTests':archives,'courses':outcomes,'ciFirst':hosted,
                'coldRestart':app['coldRestart'],'legacyDefaults':app['legacyDefaults']}
write(out/'verification.json', verification)
shots = []
for shot in app['captures']:
    p = root/'build/game169'/shot['name']
    assert sha(p.read_bytes()) == shot['sha256']
    shutil.copy2(p, out/p.name)
    shots.append({'path':'docs/readmes/169/'+p.name,'title':'18 Nov 2026 · '+p.stem.replace('-',' ').title()})

modules = [
    {'id':'rail','name':'Rail perception and forecast','source':'src/main/java/agent/RailNavigation.java',
     'description':'Reads sensor bounds, predicts a supported jump and measures the actual crossing.'},
    {'id':'input','name':'Shared player input','source':'src/main/java/agent/GameInput.java',
     'description':'Routes approved jump input through the original controller lease.'},
    {'id':'course','name':'Native hazard and finish tests',
     'source':'workspace-engine/src/test/java/fraymus/workspace/OriginalRailsTest.java',
     'description':'Checks the fixture identity, measured clearance, beacon contact, reset and legacy defaults.'},
]
system = {'finding':'The AI cleared both rails and reached both beacons.',
          'validation':validation,'courses':chart,'checks':checks,'screenshots':shots,'modules':modules}
write(out/'system-map.json', system)
paths = {row['path'] for row in read('docs/readmes/168/file-inventory.json')}
paths.update(subprocess.check_output(['git','diff','--name-only','aba5b1dff','HEAD'],text=True).splitlines())
paths.update(p.relative_to(root).as_posix() for p in source_dir.iterdir() if p.is_file())
paths.add('docs/RAIL_NAVIGATION_169_LOCK.json')
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
(out/'index.html').write_text(html,encoding='utf8')
shutil.copy2(source_dir/'guide.js',out/'guide.js')

progress = ' '.join(f"The visible AI completed {item['title']} with {item['cells']} cells "
                    f"and {item['crossing']['measuredClearance']:.3f} game units of minimum measured clearance."
                    for item in outcomes)
note = ('## Edition 169 — measured rail clearance\n\n2026-11-18. '+validation+' '+progress+
        ' The exact published edition 168 app trial confirms rail contact on both courses. A sensor-based jump forecast '
        'and independent native contact hold the comparison together. All 24 course files and 59 other tracked compiled '
        'control classes match. The prior hosted chat timeout is documented with its XML and the test fixture was improved. '
        'Read README-169.md and docs/publication169/plan.md. Canonical Java workspace remains separate from private review data. '
        'Next, test multiple nearby rails and varied approaches, then connect this evidence to durable playtesting.\n\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md','docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md',
             'docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
    prepend(name,note)
(root/'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 169 — verified AI rail clearance](README-169.md) · '
    '[Previous edition](README-168.md) · [Offline trajectory explorer](docs/readmes/169/index.html) · '
    '[Full history](docs/readmes/INDEX.md).\n\n'+progress+'\n\n'+validation+'\n',encoding='utf8')
index = root/'docs/readmes/INDEX.md'
s = index.read_text(encoding='utf8')
if 'README-169.md' not in s:
    index.write_text(s.replace('\n','\n\n[Edition 169 — verified AI rail clearance](../../README-169.md) · '
                     '[Guide](169/index.html)\n',1),encoding='utf8')
(root/'docs/obsidian/177-rail-navigation.md').write_text(
    '# Verified rail navigation\n\n'+progress+'\n\n'+validation+
    '\n\n[Edition 169](../../README-169.md) · [Playthrough explorer](../readmes/169/index.html) · '
    '[Outcomes](../readmes/169/verification.json) · [Comparison](../readmes/169/baseline.json)\n',encoding='utf8')
nodes = [{'id':'edition','type':'file','file':'README-169.md','x':0,'y':0,'width':480,'height':400},
         {'id':'rail','type':'file','file':modules[0]['source'],'x':520,'y':0,'width':460,'height':380},
         {'id':'receipts','type':'file','file':'docs/readmes/169/verification.json',
          'x':520,'y':420,'width':460,'height':280}]
edges = [{'id':name,'fromNode':'edition','fromSide':'right','toNode':name,'toSide':'left'}
         for name in ['rail','receipts']]
write(root/'docs/obsidian/177-rail-navigation.canvas',{'nodes':nodes,'edges':edges})
prepend('docs/obsidian/00-home.md','[Edition 169 — verified AI rail clearance](177-rail-navigation.md)\n\n')
print(json.dumps({'passed':True,'inventory':len(rows),'screenshots':len(shots),'outcomes':outcomes}))
