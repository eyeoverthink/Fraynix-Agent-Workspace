from pathlib import Path
import os
import json
import re
import shutil
import subprocess
import xml.etree.ElementTree as element_tree

root = Path.cwd()
env = dict(os.environ, JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot')
env['PATH'] = env['JAVA_HOME'] + '/bin;' + env['PATH']
out = root / 'build/validation180-final'
out.mkdir(exist_ok=True)
classes = [
    'RequestWorkbenchTest', 'ChainRecommendationTest', 'RelayPlaytestTest', 'RampTraceChainTest', 'LocalWorkflowSelectionTest', 'RampComparisonTest', 'RampProcedureTest', 'RampMissionTest',
    'Agency043Test', 'Domain044Test', 'ChatArchive112Test', 'DataSummary104Test',
    'CreationTest', 'CreationPlanner044Test', 'CreationRetry102Test', 'WorkflowTest',
    'MeasurementStoreTest', 'WorkspaceServerBoundaryTest'
]
gradle = [str(root / 'gradlew.bat'), '--offline', '--max-workers=1']

def run(args, name, cwd=root):
    log = root / 'build' / (name + '180-tests.log')
    with log.open('wb') as captured:
        subprocess.run(args, cwd=cwd, env=env, stdout=captured, stderr=subprocess.STDOUT, check=True)
    return log.read_text(encoding='utf8')

def collect(folder, destination):
    destination.mkdir(parents=True, exist_ok=True)
    count = 0
    for path in folder.glob('*.xml'):
        suite = element_tree.parse(path).getroot()
        assert not any(int(suite.get(k, 0)) for k in ('failures', 'errors', 'skipped')), path
        count += int(suite.get('tests'))
        shutil.copy2(path, destination / path.name)
    assert count
    return count

run([*gradle, ':test'], 'root')
root_count = collect(root / 'build/test-results/test', out / 'root')
args = [*gradle, ':workspace-engine:test']
for name in classes:
    args.extend(['--tests', 'fraymus.workspace.' + name])
run(args, 'workspace')
workspace_count = collect(root / 'workspace-engine/build/test-results/test', out / 'workspace')
desktop = run(['npm.cmd', 'test'], 'desktop', root / 'workspace-desktop')
assert re.search(r'^. fail 0$', desktop, re.M) and re.search(r'^. skipped 0$', desktop, re.M)
archive = run(['python', '-m', 'unittest', 'discover', '-s', 'scripts', '-p', 'test_*archive.py', '-v'], 'archive')
assert archive.strip().endswith('OK')
result = {'passed': True, 'rootJava': root_count, 'workspaceJava': workspace_count, 'classes': classes,
          'desktop': int(re.search(r'^. tests (\d+)$', desktop, re.M)[1]),
          'archiveTests': int(re.search(r'Ran (\d+) tests', archive)[1])}
(out / 'summary.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf8')
print(json.dumps(result))
