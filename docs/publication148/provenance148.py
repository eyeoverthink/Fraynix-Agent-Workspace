"""Bind the resumed study to its corrected input revision without rewriting originals."""
from pathlib import Path
import datetime, hashlib, json, subprocess

r = Path.cwd()
sha = lambda b: hashlib.sha256(b).hexdigest()
git = lambda *args: subprocess.check_output(['git', *args], cwd=r)
read = lambda name: json.loads((r / name).read_bytes())
revision = git('rev-parse', 'aa35463e').decode().strip()
committed = datetime.datetime.fromisoformat(git('show', '-s', '--format=%cI', revision).decode().strip())
study_path = r / 'build/occlusion148-black-lighter-movement-app/result.json'
study = json.loads(study_path.read_bytes())
assert study['passed']
manifest_bytes = (r / 'docs/publication148/source148.json').read_bytes()
assert manifest_bytes == git('show', revision + ':docs/publication148/source148.json')
manifest = json.loads(manifest_bytes)
inputs = []
for source in manifest['records']:
    pack = r / source['pack']
    assert sha(pack.read_bytes()) == source['packSha256']
    current = json.loads(pack.read_bytes())
    original = read('build/webcam148-private/before-short-label/' + pack.name)
    changed_labels = []
    for old, new in zip(original['objects'], current['objects'], strict=True):
        changed_labels.append({'before': old['label'], 'after': new['label']})
        old['label'] = new['label']
    assert original == current, 'Display label must be the only private pack change'
    labels_bytes = (r / source['labels']).read_bytes()
    assert sha(labels_bytes) == source['labelsSha256']
    assert labels_bytes == git('show', study['source'] + ':' + source['labels'])
    runs = [x for x in study['runs'] if x['caseName'] == source['case']]
    assert runs
    expected_frames = [{k: frame[k] for k in ['sourceFrame', 'imageSha256']} for frame in current['frames']]
    for run in runs:
        assert run['dataset']['frames'] == expected_frames
        assert run['dataset']['objects'] == current['objects']
        assert committed <= datetime.datetime.fromisoformat(run['dataset']['importedAt'].replace('Z', '+00:00'))
    inputs.append({'case': source['case'], 'packSha256': source['packSha256'],
                   'labelsSha256': source['labelsSha256'], 'labelsUnchangedSinceFirstAttempt': True,
                   'framesAndObjectsMatchEveryExecutedRun': True, 'labelChanges': changed_labels,
                   'runIds': [x['run']['id'] for x in runs]})
result = {'passed': True, 'initialAttemptSourceHeader': study['source'],
          'effectiveInputRevision': revision, 'inputCommittedAt': committed.isoformat(),
          'productionBuildRevision': study['buildReceipt']['source'],
          'originalStudySha256': sha(study_path.read_bytes()), 'manifestSha256': sha(manifest_bytes),
          'inputs': inputs, 'scope': 'Original resumed receipt retains the initial failed import source header. '
          'This separate receipt binds the actual revised packs to the committed manifest and every executed run. '
          'Only the object display label changed; image bytes, sample times, boxes and independent labels did not. '
          'The input revision precedes every successful import. No original receipt or method was relabelled.'}
(r / 'docs/publication148/study-provenance148.json').write_bytes((json.dumps(result, indent=2) + '\n').encode())
print(json.dumps(result))
