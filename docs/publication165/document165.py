"""Create the small edition snapshot after the original archive is published and verified."""
from pathlib import Path
import hashlib, json, re, shutil, xml.etree.ElementTree as E

r = Path.cwd()
out = r/'docs/readmes/165'
out.mkdir(parents=True, exist_ok=True)
sha = lambda b: hashlib.sha256(b).hexdigest()
def read(n): return json.loads((r/n).read_bytes())
def write(p, v): p.parent.mkdir(parents=True, exist_ok=True); p.write_text(json.dumps(v, indent=2, ensure_ascii=False)+'\n', encoding='utf8')
def text(p, s): p.parent.mkdir(parents=True, exist_ok=True); p.write_text(s, encoding='utf8')

comp = read('build/comparison165.json')
assert comp['passed'] and comp['totalCompletedWorkflowsIncludingBaseline'] == 60
lock = read('docs/EVIDENCE_REUSE_165_LOCK.json')
for n, h in lock['sourceHashes'].items(): assert sha((r/n).read_bytes().replace(b'\r\n', b'\n')) == h, n
studies = {n: read('build/study165-'+n+'/result.json') for n in comp['runs']}
for s in studies.values(): assert s['passed'] and s['methodLockSha256'] == sha((r/'docs/EVIDENCE_REUSE_165_LOCK.json').read_bytes())
count = 0
for p in (r/'build/validation165/final').glob('*.xml'):
    e = E.parse(p).getroot()
    assert sum(int(e.get(k, '0')) for k in ['failures', 'errors', 'skipped']) == 0
    count += int(e.get('tests'))
assert count == read('build/validation165/summary.json')['java'] == 91
desktop = (r/'build/desktop165-final.log').read_text(encoding='utf8')
assert 'fail 0' in desktop and 'tests 181' in desktop and read('build/render163.json')['passed']
upload = read('build/evidence165-upload.json')
assert not upload['draft'] and upload['download']['passed']
manifest = read('build/evidence165-manifest.json')
write(out/'original-evidence.json', manifest)
write(out/'evidence-publication.json', upload)
finding = read('build/findings165.json')['finding']
validation = '91 selected Java checks, 181 desktop checks and four archive-boundary tests passed. Three actual Chromium halfway labels and exact poses passed. Six final Java/Electron sessions completed 60 durable chat workflows: 48 simulated motions and 12 fresh data reports, with 12 abrupt interruptions and 24 saved motion-procedure versions. Exact source, numerical and actual-report outcomes match across all sessions. Original native class identities, tracker/controller defaults and acceptance tolerances remain unchanged. No model inference or new physical observation is claimed.'
study = studies['on-b']
review = read('build/app-visual-review165.json')
assert review['passed'] and {x['sha256'] for x in review['images']} == {x['sha256'] for x in study['captures']}
shots = []
for c in study['captures']:
    p = r/'build/study165-on-b'/c['name']
    assert sha(p.read_bytes()) == c['sha256'] and c['privatePixelsExcluded']
    shutil.copy2(p, out/c['name'])
    shots.append({'path': 'docs/readmes/165/'+c['name'], 'title': '2026-11-17 · actual app / '+c['name']})
for source, name in [('build/native-identity165.json', 'native-identity.json'), ('build/review165-inventory.json', 'review-inventory.json'), ('build/comparison165.json', 'comparison-evidence.json')]: write(out/name, read(source))
write(out/'comparison.json', comp['costRows'])
write(out/'verification.json', {'passed': True, 'java': count, 'desktop': 181, 'archiveTests': 4, 'completedChatWorkflows': 60, 'sessions': 6, 'historyDepth': 8, 'checks': study['checks'], 'exactComparison': True, 'scope': validation, 'physicalAccuracyPromoted': False, 'modelCalls': 0, 'defaultTracker': 'legacy-flow', 'originalSourceFilesUnchanged': study['originalFilesUnchanged'], 'externalEvidenceVerified': upload['download']})
modules = [{'id': k, 'name': n, 'source': f} for k, n, f in [
    ('context', 'Bounded verified computation reuse', 'workspace-engine/src/main/java/fraymus/workspace/VerifiedComputationCache.java'),
    ('witness', 'Fresh bytes and implementation witnesses', 'workspace-engine/src/main/java/fraymus/workspace/EvidenceFingerprint.java'),
    ('history', 'Original accepted-history verifier adapter', 'workspace-engine/src/main/java/fraymus/workspace/VerifiedHistory.java'),
    ('workflow', 'Durable stages and independent outcomes', 'workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),
    ('measurement', 'Complete six-session workflow comparison', 'docs/publication165/study165.cjs'),
    ('archive', 'Retrievable original evidence', 'scripts/evidence_archive.py'),
    ('roadmap', 'Capability and publication renewal', 'docs/ARCHITECTURE_RENEWAL_PLAN.md')]]
system = {'modules': modules, 'screenshots': shots, 'comparison': comp['costRows'], 'probes': comp['probeRows'], 'finding': finding, 'validation': validation}
write(out/'system-map.json', system)
pair_table = '| Pair | Reuse, ten tasks s | Full computation s | Time saved % |\n| --- | ---: | ---: | ---: |\n'
for x in comp['pairs']: pair_table += f"| {x['pair']} | {x['reuseMs']/1000:.2f} | {x['fullMs']/1000:.2f} | {x['savedPercent']:+.2f} |\n"
attempts = read('build/attempts165.json')['description']
md = (r/'docs/publication165/README-template.md').read_text(encoding='utf8')
for k, v in {'FINDING': finding, 'PAIR_TABLE': pair_table, 'MEASURED': (r/'build/findings165.md').read_text(encoding='utf8'), 'VALIDATION': validation, 'ATTEMPTS': attempts}.items(): md = md.replace('{{'+k+'}}', v)
assert '{{' not in md
text(r/'README-165.md', md); text(r/'README.md', md)
progress = '## Edition 165 — locally verified; publication pending\n\n2026-11-17. Java target: eyeoverthink/Fraynix-Agent-Workspace. Reused J:/Codex-Work/fraynix-workspace-edition164 on branch edition165; no second full archive copy. No subagents; one heavy check at a time. Existing recordings only; new physical validation remains pending. CI checked first and separately.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nNew capability-named Java caches use fresh complete witnesses before/after; native actions and outcome gates remain original. Parent identity changes explicitly; do not relabel old proofs. New raw evidence uses a downloaded-and-verified release archive; old001–164 records remain unchanged. No history rewrite, asset/license migration, universal consolidation, MCP or new provider is claimed. Read README-165.md and docs/ARCHITECTURE_RENEWAL_PLAN.md. Next: asset/provenance inventory, reversible archive migration and one tested domain extraction; keep witness costs visible without another numbered subsystem.\n\n'
for n in ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md']:
    p = r/n; s = p.read_text(encoding='utf-8-sig'); assert not s.startswith('## Edition 165'); text(p, progress+s)
for n in ['docs/INTEGRATED_AGENT_ROADMAP.md', 'docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
    p = r/n; text(p, '## Edition 165 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition 165](../README-165.md) · [Architecture renewal](ARCHITECTURE_RENEWAL_PLAN.md). Preserve one integrated Java agent and every source/outcome check. Physical validation remains pending.\n\n'+p.read_text(encoding='utf-8-sig'))
for n, prefix in [('docs/readmes/INDEX.md', '[Edition 165 — verified reuse and architecture renewal](../../README-165.md) | [Offline guide](165/index.html)'), ('docs/obsidian/00-home.md', 'Current: [[docs/obsidian/173-verified-evidence-reuse.md]] | [[README-165.md]]')]:
    p = r/n; text(p, prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
text(r/'docs/obsidian/173-verified-evidence-reuse.md', '# Verified evidence reuse — edition165\n\n[[README-165.md]]\n\n'+finding+'\n\n'+validation+'\n\n[[docs/ARCHITECTURE_RENEWAL_PLAN.md]]\n\nFresh witnesses authorize computation reuse; original outcome checks authorize acceptance.\n')
write(r/'docs/obsidian/173-verified-evidence-reuse.canvas', {'nodes': [{'id': m['id'], 'type': 'file', 'file': m['source'], 'x': i*430, 'y': 0, 'width': 390, 'height': 200} for i, m in enumerate(modules)], 'edges': [{'id': str(i), 'fromNode': modules[i]['id'], 'toNode': modules[i+1]['id']} for i in range(len(modules)-1)]})
names = {x['path'] for x in read('docs/readmes/164/file-inventory.json')}
names.update(lock['sourceHashes'])
names.update(['docs/EVIDENCE_REUSE_165_LOCK.json', 'docs/ARCHITECTURE_RENEWAL_PLAN.md', 'scripts/evidence_archive.py', 'scripts/test_evidence_archive.py', '.github/workflows/build.yml'])
names.update(p.relative_to(r).as_posix() for p in (r/'docs/publication165').iterdir() if p.is_file())
inventory = []
for n in sorted(names):
    raw = (r/n).read_bytes()
    try: raw = raw.decode('utf-8-sig').replace('\r\n', '\n').encode(); kind = 'utf8-lf'
    except UnicodeDecodeError: kind = 'binary'
    inventory.append({'path': n, 'bytes': len(raw), 'sha256': sha(raw), 'hashKind': kind})
write(out/'file-inventory.json', inventory)
html = (r/'docs/publication165/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</', '<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</', '<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html', html)
shutil.copy2(r/'docs/publication165/guide.js', out/'guide.js')
print(json.dumps({'sources': len(inventory), 'evidence': len(manifest['files']), 'screenshots': len(shots), 'java': count, 'desktop': 181}))
