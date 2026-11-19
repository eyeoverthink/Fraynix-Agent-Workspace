"""Write the edition docs from the saved desktop comparison and verified artifacts."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import shutil
from statistics import median
import subprocess

root = Path.cwd()
study = root / 'build/workbench180-final'
out = root / 'docs/readmes/180'
out.mkdir(exist_ok=True)
publication = root / 'docs/publication180'


def read(path):
    return json.loads((root / path).read_bytes())


def write(path, value):
    (root / path).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


def prepend(path, entry):
    path = root / path
    old = path.read_text(encoding='utf-8-sig')
    if not old.startswith(entry):
        path.write_text(entry + old, encoding='utf8')


app = read(study / 'result.json')
tests = read('build/validation180-final/summary.json')
upload = read('build/evidence180-upload.json')
prior = read('build/workbench180-prior.json')
assert app['passed'] and tests['passed'] and upload['download']['passed'] and prior['passed']
assert app['presentationPassed'] and app['engineJarsMatched']
assert app['source'] == prior['source'] == upload['source']
assert app['creation']['completed'] and app['creation']['test']['passed']
files = app['files']
assert files['staleReviewRefused'] and files['unsupportedMappingRefused']
assert files['learned']['choice']['by'] == 'remembered' and 'source' not in files['learned']
assert files['cells']['rows'][0][0] == '00017' and files['inspection']['preview'][0]['bits'] == '10001001'
headline = ('Fraynix can turn everyday requests into reviewed local work, learn a confirmed phrase, '
            'convert tables in both directions, inspect original file bytes and pass creation briefs to the Canvas.')
counts = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
          f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed.")
labels = {'request-review.png':'A slang command with its exact file binding',
          'round-trip-output.png':'A converted JSON file with the verified source receipt',
          'byte-inspection.png':'The selected file as offsets, hex, decimals and bits',
          'remembered-language.png':'The taught phrase recognized after restart',
          'created-tool.png':'A generated offline counter with passing browser checks'}
shots = []
for row in app['captures']:
    name = row['name']
    original = study / name
    assert sha256(original.read_bytes()).hexdigest() == row['sha256']
    shutil.copyfile(original, out / name)
    shots.append({'path': f'docs/readmes/180/{name}', 'title': labels[name]})
assert set(labels) == {Path(x['path']).name for x in shots}
screens = '\n'.join(f"- [{row['title']}]({row['path']})" for row in shots if 'request-review.png' not in row['path'])
models = []
for name, outcome in app['models'].items():
    rows = outcome['cases']
    returned = [row['ms']/1000 for row in rows if 'ms' in row]
    models.append({'name':name, 'available':outcome['available'], 'correct':sum(row['match'] for row in rows),
                   'total':len(rows), 'returned':len(returned), 'medianSeconds':median(returned) if returned else None})
model_table = '\n'.join(
    f"| {row['name']} | {row['correct']}/{row['total']} | {row['returned']} | "
    + (f"{row['medianSeconds']:.1f} s |" if row['medianSeconds'] is not None else "— |")
    for row in models)
brief = ' · '.join(f"{row['name']} {row['correct']}/{row['total']}" for row in models if row['available'])
model_scope = ('The paired study used the same four retained requests for slang, Spanish, French and ambiguity. '
               'These are interpretation trials. Completing a workflow still needs explicit review. '
               'The time is wall clock from Interpret to the saved suggestion.')
readme = f"""# Fraynix Agent Workspace · Edition 180

[Edition 179](README-179.md) · [Offline walkthrough](docs/readmes/180/index.html) · [Full edition history](docs/readmes/INDEX.md)

{headline}

![A request and its attached source before confirmation](docs/readmes/180/request-review.png)

## Try it

1. Open **Request workbench** below Chat. Examples include “Flip this CSV into JSON”, “Convertis ce JSON en CSV stp”, “Convierte este CSV a JSON” and “Show me the bytes”.
2. Select **Interpret request**. A local rule can handle familiar wording. Allow the selected local model to suggest an action when needed. Pick the intended action yourself if the suggestion needs correction.
3. Attach the exact input for file work, review its hash, then select **Confirm and verify**. Open the result or save a checked output to project exports.
4. Select **Remember this wording** after a verified file result. Fraynix can recognize that phrase again. Each new run still needs a source and confirmation.
5. For a creation request, select **Review in Creation Canvas**. Fraynix carries the original brief into the existing tool builder, where you can review the source and behavior checks before building.

## What passed

The CSV → JSON → CSV trial preserved both rows, column order, leading zeros, Unicode, multiline quoted text and an empty cell. The native file inspection matched the SHA-256 and every observed byte of the controlled sample. A changed review hash and an unsupported JSON numeric mapping were refused. After a hard Java restart with models disabled, the verified result reopened and the taught phrase resolved without inference.

The app and utility briefs reached the Creation Canvas intact. The creation worker rejected the first model response because it lacked a native tool call. The next response produced the utility, and Chromium verified Start → Add → Reset. The original Edition 179 Ramp and Relay proofs also reopened.

| Installed model | Correct suggestions | Returned native calls | Median returned time |
| --- | ---: | ---: | ---: |
{model_table}

{model_scope} {counts} The final layout was tested again with byte-identical engine builds. The study and the final screenshots are both retained.

## Conversion and inspection scope

The current exchange preserves string-valued tables in UTF-8 CSV and JSON. Flat JSON records need matching keys and string values. File reviews accept up to 64 KiB. The inspector reports the full hash and a signature hint, with a 64-byte preview. See the plan for typed tables, behavior-checked source conversion and read-only parsing of documented binary formats.

## Screenshots and evidence

{screens}

[Verification and model cases](docs/readmes/180/verification.json) · [Original test evidence](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/tag/evidence-180) · [Implementation plan and sources](docs/publication180/plan.md)
"""
(root / 'README-180.md').write_text(readme, encoding='utf8')
(root / 'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 180 — teachable requests and verified conversion](README-180.md) · '
    '[Previous edition](README-179.md) · [Offline walkthrough](docs/readmes/180/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + headline + '\n', encoding='utf8')
index = root / 'docs/readmes/INDEX.md'
old = index.read_text(encoding='utf8')
if 'README-180.md' not in old:
    index.write_text(old.replace('\n', '\n\n[Edition 180 — teachable requests and verified conversion](../../README-180.md) · '
        '[Walkthrough](180/index.html)\n', 1), encoding='utf8')
record = ('## Edition 180 — teachable requests and verified conversion\n\n2026-11-19. ' + headline + ' ' + counts
          + ' The two-direction conversion retained its cells and the byte inspector matched the original sample. '
            'A corrected phrase persisted across restart and still required approval. The utility ran its real Chromium checks. '
            'The previous Ramp and Relay receipts reopened. Observed model choices ' + brief
          + '. The first compile caught a checked SHA-256 exception. The corrected build and original failure log are retained. '
            'Read README-180.md and docs/publication180/plan.md. Next, prove typed data and small behavior-checked source ports.\n\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md','docs/INTEGRATED_AGENT_ROADMAP.md']:
    prepend(name, record)
note = 'docs/obsidian/188-request-workbench.md'
prepend('docs/obsidian/00-home.md', '[Edition 180 — teachable requests and verified conversion](188-request-workbench.md)\n\n')
(root / note).write_text('# Teachable requests and verified conversion\n\n' + headline + '\n\n'
    '[Edition 180](../../README-180.md) · [Walkthrough](../readmes/180/index.html) · '
    '[Implementation plan](../publication180/plan.md)\n', encoding='utf8')
java = 'workspace-engine/src/main/java/fraymus/workspace/'
modules = [{'title': title, 'source': java + source, 'note': note} for title, source in [
    ('Reviewable intent catalog', 'RequestLanguage.java'),
    ('Durable review and learned wording', 'RequestWorkbench.java'),
    ('Exact conversion and inspection', 'FileRepresentation.java')]]
canvas = 'docs/obsidian/188-request-workbench.canvas'
paths = [m['source'] for m in modules] + ['README-180.md']
write(canvas, {'nodes':[{'id':str(i),'type':'file','file':path,'x':i*380,'y':0,'width':350,'height':240}
                        for i,path in enumerate(paths)],
               'edges':[{'id':'step-'+str(i),'fromNode':str(i-1),'fromSide':'right','toNode':str(i),'toSide':'left'}
                        for i in range(1,len(paths))]})
verification = {'passed':True,'source':app['source'],'models':models,'cases':app['models'],'tests':tests,
                'files':files,'creation':app['creation'],'priorChains':prior,'studySource':app['studySource'],
                'engineJarsMatched':app['engineJarsMatched'],'presentationPassed':app['presentationPassed']}
write(out / 'verification.json', verification)
write(out / 'original-evidence.json', read('build/evidence180-manifest.json'))
write(out / 'evidence-publication.json', upload)
system = {'summary':headline,'modules':modules,'screenshots':shots,'evidence':[
    {'path':'docs/readmes/180/verification.json','title':'Verification and model cases'},
    {'path':'docs/readmes/180/original-evidence.json','title':'Evidence manifest'}]}
write(out / 'system-map.json', system)


def entry(path):
    data = (root / path).read_bytes()
    try:
        data = data.decode('utf-8-sig').replace('\r\n','\n').encode()
        kind = 'utf8-lf'
    except UnicodeDecodeError:
        kind = 'binary'
    return {'path':path,'bytes':len(data),'sha256':sha256(data).hexdigest(),'hashKind':kind}


base = '370fe211f820e3484a96e69ddbf972dd6a0bbdff'
changed = subprocess.check_output(['git','diff','--name-only',base,app['source']], text=True).splitlines()
assert not any(name.endswith(('ChainRecommendation.java','ReviewedChain.java','RampMission.java','RelayPlaytest.java',
                               'DataSummary104.java','CreationPlanner044.java')) for name in changed)
locked = [entry(name) for name in changed if name.startswith(('workspace-engine/', 'workspace-desktop/'))]
lock = 'docs/REQUEST_WORKBENCH_180_LOCK.json'
write(lock, {'source':app['source'],'sourceHashes':{row['path']:row['sha256'] for row in locked},
             'binaryPaths':[row['path'] for row in locked if row['hashKind']=='binary']})
paths = {row['path'] for row in read('docs/readmes/179/file-inventory.json')}
paths.update(changed)
paths.update(p.relative_to(root).as_posix() for p in publication.iterdir() if p.is_file())
paths.update([lock,note,canvas])
inventory = [entry(name) for name in sorted(paths)]
write(out / 'file-inventory.json', inventory)
images = '\n'.join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                   f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in shots)
datasets = '\n'.join(f'<script id="{key}-data" type="application/json">'
                     + json.dumps(value,ensure_ascii=True).replace('</','<\\/')+'</script>'
                     for key,value in [('system',system),('inventory',inventory)])
html = f"""<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Fraynix Edition 180</title>
<style>body{{background:#0e1821;color:#edf4f6;font:16px/1.55 system-ui;margin:0}}
main{{max-width:1000px;margin:auto;padding:clamp(16px,5vw,40px)}}a{{color:#8ce9d5}}
figure{{margin:1.5rem 0}}img{{max-width:100%;height:auto;border-radius:.3rem}}h1{{line-height:1.12}}</style>
<main><h1>Teachable requests and verified conversion</h1><p>{escape(headline)}</p><p>{escape(brief)}</p>
<nav><a href="../../../README-180.md">Run instructions</a> · <a href="verification.json">Verification</a>
 · <a href="../179/index.html">Edition 179</a></nav>{images}</main>
{datasets}</html>
"""
(out / 'index.html').write_text(html,encoding='utf8')
print(json.dumps({'passed':True,'screenshots':len(shots),'inventory':len(inventory)}))
