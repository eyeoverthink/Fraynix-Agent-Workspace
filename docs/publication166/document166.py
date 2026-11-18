"""Build the small snapshot only from completed checks and retained originals."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess

root = Path.cwd()
out = root/'docs/readmes/166'
out.mkdir(exist_ok=True)
sha = lambda data: hashlib.sha256(data).hexdigest()
def read(name): return json.loads((root/name).read_bytes())
def write(path, value): path.write_text(json.dumps(value, indent=2, ensure_ascii=False)+'\n', encoding='utf8')
def text(path, value): path.write_text(value, encoding='utf8')

checks = read('build/validation166/summary.json')
comparison = read('build/comparison166.json')
compatibility = read('build/compatibility166/result.json')
prepare = read('build/migration166/preparation.json')
candidate = read('build/migration166/candidate.json')
assets = read('build/migration166/assets.json')
upload = read('build/evidence166-upload.json')
study = read('build/study166-on-c/result.json')
for value in [checks, comparison, compatibility, prepare, candidate, study]: assert value['passed']
assert upload['download']['passed'] and not upload['draft']
assert checks['java'] == 95
desktop = (root/'build/desktop166-final.log').read_text(encoding='utf8')
assert 'tests 181' in desktop and 'fail 0' in desktop
assert 'Ran 9 tests' in (root/'build/archive166-final.log').read_text()
assert read('build/render163.json')['passed']
lock = read('docs/DOMAIN_EXTRACTION_166_LOCK.json')
for name, checksum in lock['sourceHashes'].items():
    assert sha((root/name).read_bytes().replace(b'\r\n', b'\n')) == checksum, name

validation = ('95 selected Java tests, 181 desktop tests and nine archive tests passed. '
              'Three actual Chromium midpoint/pose checks passed. Four direct boundary/ownership '
              'tests also passed in the small candidate clone. Ten completed165 workflows were '
              'reverified; ten fresh166 workflows completed with eight accepted native revisions, '
              'two process interruptions and four saved procedure versions. All ten fresh '
              'source/numerical/report comparisons match165 exactly.')
migration = (f"The original tracked tree contains {prepare['trackedTreeBytes']:,} bytes, including "
             f"{prepare['snapshotTreeBytes']:,} in docs/readmes. The local candidate contains "
             f"{candidate['treeBytes']:,} tracked bytes including its compressed historical catalog. "
             f"Its fresh local clone has {candidate['cloneGitBytes']:,} bytes of Git metadata/objects "
             f"and took {candidate['cloneSeconds']:.2f} seconds on this machine. "
             f"{prepare['verifiedFrozenFiles']:,} prior frozen files were verified; "
             f"{prepare['restoredFromArchive']['restored']} edition164 originals were restored from "
             f"the archive ({prepare['restoredFromArchive']['bytes']:,} original bytes).")
asset_text = (f"The reproducible inventory covers {len(assets['files']):,} tracked delivery/media "
              f"and imported-source files. {assets['counts'].get('game-content-rights-unresolved',0)} "
              'game-content files are explicitly marked rights-unresolved. Other entries require '
              'their own provenance review; they are not automatically marked cleared.')
ci = read('build/ci166-before-publication.json')
latest = ci['runs'][0]
ci_text = (f"CI was checked first and again before publication: run{latest['id']} is "
           f"{latest['status']} / {latest.get('conclusion') or 'no final conclusion'}. "
           'Local validation and hosted CI status are reported separately.')
finding = 'Eighteen guarded routes now have one named owner; old evidence and saved results remain verifiable. '+migration
for source, destination in [
        ('build/evidence166-manifest.json', 'original-evidence.json'),
        ('build/evidence166-upload.json', 'evidence-publication.json'),
        ('build/comparison166.json', 'comparison.json'),
        ('build/native-identity166.json', 'native-identity.json'),
        ('build/compatibility166/result.json', 'compatibility.json')]:
    write(out/destination, read(source))
write(out/'migration.json', {'preparation': prepare, 'candidate': candidate})
write(out/'asset-summary.json', {key: value for key, value in assets.items() if key != 'files'})
write(out/'verification.json', {'passed': True, 'java': 95, 'desktop': 181, 'archiveTests': 9,
    'actualRenderedChecks': 3, 'candidateBoundaryTests': 4, 'priorWorkflowsReverified': 10,
    'freshWorkflows': 10, 'interruptions': 2, 'nativeRevisions': 8, 'savedProcedures': 4,
    'exactComparison': True, 'scope': validation, 'physicalAccuracyPromoted': False,
    'modelCalls': 0, 'defaultTracker': 'legacy-flow', 'checks': study['checks'],
    'externalEvidenceVerified': upload['download'], 'ci': ci})
shots = []
for capture in study['captures']:
    source = root/'build/study166-on-c'/capture['name']
    assert sha(source.read_bytes()) == capture['sha256']
    shutil.copy2(source, out/capture['name'])
    shots.append({'path': 'docs/readmes/166/'+capture['name'],
                  'title': '2026-11-18 · actual app · '+capture['name']})
modules = [
    {'id': 'routes', 'name': 'Recorded-object domain',
     'source': 'workspace-engine/src/main/java/fraymus/workspace/SmallObjectRoutes.java',
     'description': 'Eighteen unchanged URLs delegate to original stores and workflows after shared admission.'},
    {'id': 'server', 'name': 'Shared admission boundary',
     'source': 'workspace-engine/src/main/java/fraymus/workspace/WorkspaceServer.java',
     'description': 'Authentication, body limits, host ownership and busy checks remain before domain dispatch.'},
    {'id': 'archive', 'name': 'Historical evidence retrieval',
     'source': 'scripts/history_archive.py',
     'description': 'Pinned Git objects or a verified archive restore exact originals into new paths. No fallback masks corruption.'},
    {'id': 'inventory', 'name': 'Asset provenance inventory',
     'source': 'scripts/asset_inventory.py',
     'description': 'Records actual hashes and supporting origin records; does not infer redistribution rights.'},
    {'id': 'rehearsal', 'name': 'Local clone rehearsal',
     'source': 'docs/publication166/migration166.py',
     'description': 'A small disposable source clone builds and tests; the original repository remains intact.'},
    {'id': 'tasks', 'name': 'Actual durable workflow checks',
     'source': 'docs/publication166/study166.cjs',
     'description': 'Eight motions, two fresh-data reports, two interruptions and four saved procedures; original verifiers remain authoritative.'}]
system = {'modules': modules, 'screenshots': shots, 'comparison': comparison['exactComparisons'],
          'finding': finding, 'validation': validation}
write(out/'system-map.json', system)
md = (root/'docs/publication166/README-template.md').read_text()
for key, value in {'VALIDATION': validation, 'MIGRATION': migration, 'ASSETS': asset_text,
                   'ARCHIVE_URL': upload['url'], 'CI_STATUS': ci_text}.items():
    md = md.replace('{{'+key+'}}', value)
assert '{{' not in md
text(root/'README-166.md', md)
text(root/'README.md', md)
progress = ('## Edition 166 — locally verified; publication pending\n\n2026-11-18 UTC. '
            'Canonical Java repository: eyeoverthink/Fraynix-Agent-Workspace. Working checkout remains '
            'J:/Codex-Work/fraynix-workspace-edition164, branch edition166. No subagents; one heavy job '
            'at a time. Original private projects and001–165 snapshots preserved.\n\n'+finding+
            '\n\n'+validation+'\n\n'+asset_text+'\n\nThe candidate has no remote and is not a '
            'rights-cleared release or standalone history backup. No old evidence was deleted; no '
            'remote history rewritten. Native/controller/report/parent classes and defaults remain '
            'unchanged. No new physical validation, model inference, speed improvement, training, '
            'MCP or signed installer is claimed. Next: original replacement content, another bounded '
            'verified archival batch with a second durable copy, and the next guarded domain '
            'extraction. Do not treat edition numbers as architecture. See README-166.md and '
            'docs/ARCHITECTURE_RENEWAL_PLAN.md.\n\n'+ci_text+'\n\n')
for name in ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md',
             'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md']:
    path = root/name
    prior = path.read_text(encoding='utf-8-sig')
    assert not prior.startswith('## Edition 166')
    text(path, progress+prior)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md', 'docs/PLAYABLE_LEVEL_CREATION_PLAN.md',
             'docs/ARCHITECTURE_RENEWAL_PLAN.md']:
    path = root/name
    text(path, '## Edition 166 progress — 2026-11-18 UTC\n\n'+finding+
         '\n\nOne domain is extracted; wider consolidation remains planned. The candidate retains '
         'uncleared content and original-Git dependencies. Next: replacements, another bounded '
         'archive batch with redundant storage, then a second guarded domain. '
         '[Evidence and scope](../README-166.md).\n\n'+path.read_text(encoding='utf-8-sig'))
for name, prefix in [('docs/readmes/INDEX.md', '[Edition166 — guarded domain and archive rehearsal](../../README-166.md) | [Offline guide](166/index.html)'),
                     ('docs/obsidian/00-home.md', 'Current: [[docs/obsidian/174-domain-and-archive.md]] | [[README-166.md]]')]:
    path = root/name
    text(path, prefix+'\n\n'+path.read_text(encoding='utf-8-sig'))
text(root/'docs/obsidian/174-domain-and-archive.md', '# Guarded domain and archive rehearsal\n\n'
     '[[README-166.md]]\n\n'+finding+'\n\n'+validation+'\n\n'+asset_text+
     '\n\n[[docs/ARCHITECTURE_RENEWAL_PLAN.md]]\n\nA hash preserves identity, not missing bytes. '
     'A planner proposes; original outcome checks authorize acceptance.\n')
write(root/'docs/obsidian/174-domain-and-archive.canvas', {'nodes': [
    {'id': module['id'], 'type': 'file', 'file': module['source'], 'x': i*430,
     'y': 0, 'width': 390, 'height': 200} for i, module in enumerate(modules)],
    'edges': [{'id': str(i), 'fromNode': modules[i]['id'], 'toNode': modules[i+1]['id']}
              for i in range(len(modules)-1)]})
names = {row['path'] for row in read('docs/readmes/165/file-inventory.json')}
names.update(lock['sourceHashes'])
names.add('docs/DOMAIN_EXTRACTION_166_LOCK.json')
names.update(path.relative_to(root).as_posix() for path in (root/'docs/publication166').glob('*') if path.is_file())
inventory = []
for name in sorted(names):
    raw = (root/name).read_bytes()
    try:
        raw = raw.decode('utf-8-sig').replace('\r\n', '\n').encode()
        kind = 'utf8-lf'
    except UnicodeDecodeError:
        kind = 'binary'
    inventory.append({'path': name, 'bytes': len(raw), 'sha256': sha(raw), 'hashKind': kind})
write(out/'file-inventory.json', inventory)
html = (root/'docs/publication166/guide-template.html').read_text()
html += '<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script>'
html += '<script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script>'
html += '<script src="guide.js"></script></body></html>'
text(out/'index.html', html)
shutil.copy2(root/'docs/publication166/guide.js', out/'guide.js')
print(json.dumps({'sources': len(inventory), 'screenshots': len(shots), 'java': checks['java']}))
