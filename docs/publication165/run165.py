"""Execute the locked final sessions serially; preserve and stop at any failure."""
from pathlib import Path
import json, os, subprocess

root = Path.cwd()
baseline = Path('J:/Codex-Work/fraynix-baseline-164')
assert json.loads((baseline/'build/study164-on-e/result.json').read_bytes())['passed']
env = dict(os.environ, JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot')
for name in ['on-a', 'off-a', 'off-b', 'on-b']:
    env.update(STUDY165_NAME=name, STUDY165_MODE=name.split('-')[0])
    with (root/f'build/study165-{name}.log').open('x', encoding='utf8') as log:
        subprocess.run(['node', 'docs/publication165/study165.cjs'], env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
    result = json.loads((root/f'build/study165-{name}/result.json').read_bytes())
    assert result['passed']
    print(json.dumps({'run': name, 'passed': True, 'totalTaskMs': sum(t['totalMs'] for t in result['tasks'])}), flush=True)
env.update(STUDY164_NAME='on-d', STUDY164_MODE='on')
with (baseline/'build/study164-on-d.log').open('x', encoding='utf8') as log:
    subprocess.run(['node', 'docs/publication164/study164.cjs'], cwd=baseline, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
assert json.loads((baseline/'build/study164-on-d/result.json').read_bytes())['passed']
subprocess.run(['python', '-X', 'utf8', 'docs/publication165/compare165.py'], check=True)
