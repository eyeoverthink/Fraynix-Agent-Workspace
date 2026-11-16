"""Run remaining owned-app and regression checks serially; stop at first failure."""
from pathlib import Path
import json, os, subprocess, sys, time

r = Path.cwd()
assert json.loads((r / 'build/occlusion148-black-lighter-movement-app/result.json').read_bytes())['passed']
env = os.environ.copy()
env['RETURN148_GROUP'] = 'black-lighter-movement'
env['PYTHONUTF8'] = '1'
steps = [
    ('restart148', ['node', 'docs/publication148/restart148.cjs']),
    ('capture148', ['node', 'docs/publication148/capture148.cjs']),
    ('broad-java148', [sys.executable, '-X', 'utf8', 'docs/publication148/validate148.py', 'java']),
    ('broad-desktop148', [sys.executable, '-X', 'utf8', 'docs/publication148/validate148.py', 'desktop']),
]
for name, args in steps:
    path = r / 'build' / (name + '-serial.log')
    assert not path.exists(), 'Preserve previous execution: ' + str(path)
    print(json.dumps({'started': name}), flush=True)
    started = time.monotonic()
    with path.open('wb') as output:
        p = subprocess.run(args, cwd=r, env=env, stdout=output, stderr=subprocess.STDOUT)
    result = {'step': name, 'exit': p.returncode, 'seconds': time.monotonic() - started}
    (r / 'build' / (name + '-serial.json')).write_bytes((json.dumps(result, indent=2) + '\n').encode())
    print(json.dumps(result), flush=True)
    if p.returncode:
        raise SystemExit(p.returncode)
