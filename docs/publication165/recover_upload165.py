"""Resume the recorded upload by identity; never recreate or overwrite the release."""
from pathlib import Path
import importlib.util, json, subprocess, urllib.request

r = Path.cwd()
receipt_path = r/'build/evidence165-upload.json'
receipt = json.loads(receipt_path.read_bytes())
manifest_path = r/'build/evidence165-manifest.json'
manifest = json.loads(manifest_path.read_bytes())
assert receipt['releaseId'] == manifest['release']['id']
assert receipt['assetId'] == manifest['archive']['assetId']
credentials = subprocess.run(['git', 'credential', 'fill'], input='protocol=https\nhost=github.com\npath=eyeoverthink/Fraynix-Agent-Workspace.git\n\n', text=True, capture_output=True, check=True)
token = dict(line.split('=', 1) for line in credentials.stdout.splitlines() if '=' in line)['password']
base = 'https://api.github.com/repos/eyeoverthink/Fraynix-Agent-Workspace'
def get(path):
    req = urllib.request.Request(base+path, headers={'Authorization': 'Bearer '+token, 'Accept': 'application/vnd.github+json', 'User-Agent': 'Fraynix-evidence-publication', 'X-GitHub-Api-Version': '2026-03-10'})
    with urllib.request.urlopen(req, timeout=60) as response: return json.load(response)
release = get('/releases/'+str(receipt['releaseId']))
assert not release['draft'] and release['tag_name'] == 'evidence-165'
asset = get('/releases/assets/'+str(receipt['assetId']))
assert asset['state'] == 'uploaded' and asset['name'] == 'evidence165.zip' and asset['size'] == manifest['archive']['bytes']
if asset.get('digest'): assert asset['digest'] == 'sha256:'+manifest['archive']['sha256']
old_url = manifest['archive']['url']
manifest['archive']['url'] = asset['browser_download_url']
manifest_path.write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf8')
spec = importlib.util.spec_from_file_location('evidence_archive', r/'scripts/evidence_archive.py')
archive_api = importlib.util.module_from_spec(spec); spec.loader.exec_module(archive_api)
receipt.update({'draft': False, 'url': asset['browser_download_url'], 'recovery': {'originalDraftUrl': old_url, 'firstDownloadStatus': 404, 'action': 'Refetched the same asset after publication; archive bytes unchanged.'}})
receipt_path.write_text(json.dumps(receipt, indent=2)+'\n', encoding='utf8')
receipt['download'] = archive_api.download(manifest, r/'build/downloaded-evidence165.zip')
receipt_path.write_text(json.dumps(receipt, indent=2)+'\n', encoding='utf8')
print(json.dumps(receipt))
