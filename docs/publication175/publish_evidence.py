"""Publish the verified new evidence asset; existing repository history is untouched."""
from pathlib import Path
import importlib.util, json, subprocess, urllib.request

r = Path.cwd()
spec = importlib.util.spec_from_file_location('evidence_archive', r/'scripts/evidence_archive.py')
archive_api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(archive_api)
manifest_path = r/'build/evidence175-manifest.json'
manifest = json.loads(manifest_path.read_bytes())
archive = r/'build/evidence175.zip'
archive_api.verify(manifest, archive)
source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
assert subprocess.check_output(['git', 'branch', '--show-current'], text=True).strip() == 'edition175'
assert subprocess.check_output(['git', 'remote', 'get-url', '--push', 'origin'], text=True).strip().lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
# Publish the tested source branch before binding the separate evidence tag to it.
subprocess.run(['git', 'push', 'origin', 'HEAD:refs/heads/edition175'], check=True)
credentials = subprocess.run(['git', 'credential', 'fill'], input='protocol=https\nhost=github.com\npath=eyeoverthink/Fraynix-Agent-Workspace.git\n\n', text=True, capture_output=True, check=True)
token = dict(line.split('=', 1) for line in credentials.stdout.splitlines() if '=' in line)['password']


def request(url, method='GET', data=None, content_type='application/json'):
    assert url.startswith(('https://api.github.com/', 'https://uploads.github.com/'))
    body = data if isinstance(data, bytes) else json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=body, method=method, headers={
        'Authorization': 'Bearer ' + token, 'Accept': 'application/vnd.github+json',
        'Content-Type': content_type, 'User-Agent': 'Fraynix-evidence-publication',
        'X-GitHub-Api-Version': '2026-03-10'})
    with urllib.request.urlopen(req, timeout=120) as response:
        return json.load(response)


base = 'https://api.github.com/repos/eyeoverthink/Fraynix-Agent-Workspace'
assert not request(base)['private'], 'Private releases require an authenticated retrieval design'
receipt_path = r/'build/evidence175-upload.json'
assert not receipt_path.exists(), 'Inspect the retained upload receipt before attempting another publication'
release = request(base + '/releases', 'POST', {
    'tag_name': 'evidence-175', 'target_commitish': source, 'name': 'Edition 175 original evidence',
    'draft': True, 'body': 'Exact local-model selections, paired workflow timings, checked ramp, data and creation artifacts, and restart receipts for edition 175. The evidence manifest in README-175.md identifies the original files.'})
assert release['draft'] and release['tag_name'] == 'evidence-175'
receipt = {'source': source, 'releaseId': release['id'], 'draft': True}
receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf8')
upload = release['upload_url'].split('{', 1)[0] + '?name=evidence175.zip'
asset = request(upload, 'POST', archive.read_bytes(), 'application/zip')
assert asset['name'] == 'evidence175.zip' and asset['state'] == 'uploaded'
assert asset['size'] == manifest['archive']['bytes']
if asset.get('digest'):
    assert asset['digest'] == 'sha256:' + manifest['archive']['sha256']
manifest['archive']['url'] = asset['browser_download_url']
manifest['archive']['assetId'] = asset['id']
manifest['release'] = {'tag': 'evidence-175', 'source': source, 'id': release['id']}
receipt['assetId'] = asset['id']
receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf8')
published = request(base + '/releases/' + str(release['id']), 'PATCH', {'draft': False})
assert not published['draft'] and published['tag_name'] == 'evidence-175'
# A draft upload may return an untagged temporary download URL. Read it again after publication.
asset = request(base + '/releases/assets/' + str(asset['id']))
assert asset['state'] == 'uploaded' and asset['size'] == manifest['archive']['bytes']
manifest['archive']['url'] = asset['browser_download_url']
manifest_path.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf8')
receipt.update({'draft': False, 'url': asset['browser_download_url']})
receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf8')
receipt['download'] = archive_api.download(manifest, r/'build/downloaded-evidence175.zip')
receipt_path.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf8')
print(json.dumps(receipt))
