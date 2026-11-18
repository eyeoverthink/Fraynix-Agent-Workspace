"""One reversible, local-only archive and small-checkout rehearsal."""
from pathlib import Path
import json
import gzip
import os
import shutil
import subprocess
import sys
import time

ROOT = Path.cwd()
sys.path.insert(0, str(ROOT/'scripts'))
import history_archive as history
import evidence_archive as archive
import asset_inventory

BASE = '671a3605d7481e7e5994b75265bc7fab8ce70578'
OUT = ROOT/'build/migration166'
OUT.mkdir(exist_ok=True)


def write(path, value):
    path.write_text(json.dumps(value, indent=2)+'\n', encoding='utf8')


def prepare():
    assert not (OUT/'catalog.json').exists(), 'Never overwrite a rehearsal'
    start = time.perf_counter()
    with history.GitEvidence(ROOT, BASE) as source:
        value = history.catalog(source)
        write(OUT/'catalog.json', value)
        (OUT/'catalog.json.gz').write_bytes(gzip.compress((OUT/'catalog.json').read_bytes(), mtime=0))
        write(OUT/'assets.json', asset_inventory.inventory(source))
        names = [row['path'] for row in value['files'] if row['edition'] == 164]
        restored = history.restore(value, names, OUT/'original164', git=source)
        manifest = archive.pack([{'logicalPath': n, 'source': OUT/'original164'/n} for n in names],
                                OUT/'legacy164.zip', OUT/'legacy164-manifest.json')
        back = history.restore(value, names, OUT/'retrieved164',
                               archive=OUT/'legacy164.zip', manifest=manifest)
        for name in names:
            assert (OUT/'retrieved164'/name).read_bytes() == source.read(name)
        tree_bytes = sum(row['bytes'] for row in source.files.values())
        snapshot_bytes = sum(row['bytes'] for name, row in source.files.items()
                             if name.startswith('docs/readmes/'))
        write(OUT/'preparation.json', {'passed': True, 'sourceCommit': BASE,
              'catalogSha256': history.digest((OUT/'catalog.json').read_bytes()),
              'compressedCatalogSha256': history.digest((OUT/'catalog.json.gz').read_bytes()),
              'verifiedFrozenFiles': len(value['files']), 'trackedTreeBytes': tree_bytes,
              'snapshotTreeBytes': snapshot_bytes, 'restoredFromGit': restored,
              'restoredFromArchive': back, 'seconds': time.perf_counter()-start,
              'scope': 'All old frozen hashes verified. One legacy edition archived and restored. '
                       'Other history still depends on original Git; no deletion or remote rewrite.'})
    print('Historical catalog and legacy164 round trip passed', flush=True)


def candidate():
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    clean, clone = OUT/'clean', OUT/'clone'
    assert not clean.exists() and not clone.exists()
    clean.mkdir()
    begin = time.perf_counter()
    with history.GitEvidence(ROOT, commit) as source:
        kept = []
        for name, row in source.files.items():
            if name.startswith('docs/readmes/') and not (
                    name.startswith('docs/readmes/165/') or name in (
                        'docs/readmes/editions.json', 'docs/readmes/INDEX.md')):
                continue
            target = clean/name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read(name))
            kept.append({'path': name, 'bytes': row['bytes'], 'blob': row['blob']})
    # The catalog is evidence outside Git in the main repository, included here for rehearsal.
    shutil.copy2(OUT/'catalog.json.gz', clean/'history-catalog.json.gz')
    prep = json.loads((OUT/'preparation.json').read_bytes())
    note = ('# Local migration rehearsal\n\nNot a distribution or rights-cleared release. '
            'Older evidence payloads are omitted from this candidate only. Original history remains '
            'in Fraynix-Agent-Workspace at '+BASE+'. The compressed catalog SHA-256 is '+prep['compressedCatalogSha256']+
            '. Use scripts/history_archive.py with this pinned catalog and either the original '
            'repository or a verified archive to restore requested paths into a NEW directory. '
            'Do not discard original history. Historical README image links are not offline until '
            'their payloads are restored. No remote is configured here.\n')
    (clean/'HISTORY-RETRIEVAL.md').write_text(note)
    def git(*args, cwd=clean):
        return subprocess.check_output(['git', *args], cwd=cwd, stderr=subprocess.STDOUT)
    git('init', '-b', 'rehearsal')
    git('config', 'core.longpaths', 'true')
    git('config', 'core.autocrlf', 'false')
    git('config', 'user.name', 'Fraynix local migration rehearsal')
    git('config', 'user.email', 'rehearsal@example.invalid')
    git('add', '.')
    git('-c', 'gc.auto=0', 'commit', '-m', 'Local candidate; original evidence retained separately')
    assert git('rev-list', '--count', 'HEAD').strip() == b'1'
    assert not git('remote').strip()
    start = time.perf_counter()
    git('clone', '--no-local', str(clean), str(clone), cwd=OUT)
    clone_seconds = time.perf_counter()-start
    clone_bytes = sum(p.stat().st_size for p in (clone/'.git').rglob('*') if p.is_file())
    tree_bytes = sum(row['bytes'] for row in kept) + (clean/'history-catalog.json.gz').stat().st_size + len(note.encode())
    write(OUT/'candidate-files.json', kept)
    env = dict(os.environ, JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot')
    command = [str(clone/'gradlew.bat'), '--no-daemon', '--max-workers=1',
               'verifySourceIsolation', ':workspace-engine:installDist', ':workspace-engine:test',
               '--tests', 'fraymus.workspace.WorkspaceServerBoundaryTest',
               '--tests', 'fraymus.workspace.SmallObjectRoutesTest']
    with (OUT/'candidate-build.log').open('wb') as log:
        subprocess.run(command, cwd=clone, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
    assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip() == commit
    write(OUT/'candidate.json', {'passed': True, 'sourceCommit': commit,
          'candidateCommit': git('rev-parse', 'HEAD').decode().strip(),
          'trackedSourceFiles': len(kept), 'treeBytes': tree_bytes, 'cloneGitBytes': clone_bytes,
          'cloneSeconds': clone_seconds, 'totalSeconds': time.perf_counter()-begin,
          'longestRelativePath': max(len(row['path']) for row in kept),
          'oneCommit': True, 'remoteConfigured': False, 'sourceHeadUnchanged': True,
          'build': 'Source isolation, installed Java engine and four boundary/ownership checks passed.',
          'scope': 'Local clone using installed JDK and dependency caches; not clean-machine installation. '
                   'Uncleared assets retained. Historical media needs retrieval; catalog is not backup.'})
    print('Local clean candidate built and tested', flush=True)


if __name__ == '__main__':
    {'prepare': prepare, 'candidate': candidate}[sys.argv[1]]()
