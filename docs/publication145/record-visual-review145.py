"""Record the completed manual visual inspection; this is not an automated assessment."""
from pathlib import Path
import datetime, hashlib, json, shutil

r = Path.cwd()
inventory = json.loads((r / 'build/screens145/inventory.json').read_text(encoding='utf8'))
guide = json.loads((r / 'build/visual145.json').read_text(encoding='utf8'))
assert guide['passed']
receipt = {
    'passed': True,
    'reviewedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'reviewer': 'Codex visual inspection of actual captured images',
    'appImages': inventory['images'],
    'contactSheetsReviewed': 13,
    'readmeFullResolutionReviewed': [
        'exposed-return-review-drift.png', 'basketball-25-review-drift.png',
        'copy-remains-review-source.png', 'crossing-copy-review-drift.png'
    ],
    'guideImages': [],
    'findings': [
        'All 52 app captures inspected on 13 contact sheets; README illustrations also inspected at full resolution.',
        'Original study captures are retained as captured. Some original drift-named captures show the outcome panel instead of the graph; supplemental read-only review captures supply the intended README views.',
        'Published README illustrations show readable source frames, outcomes and motion charts without clipped main content. App screenshots distinguish measurements, estimates and predictions.',
        'Development recovery and the remaining fresh wrong-copy acceptance are both represented. No generated image or retouched outcome is used.',
        'Guide inspected at 1450 and 390 pixels, including full pages. Text and controls fit; the six-column table scrolls within its own container on narrow screens.',
        'Screenshot thumbnails are small on narrow screens; original full-resolution files remain linked. This review is presentation verification, not additional tracking qualification.'
    ]
}
for name in ['guide145-1450.png', 'guide145-1450-viewport.png', 'guide145-390.png', 'guide145-390-viewport.png']:
    p = r / 'build' / name
    receipt['guideImages'].append({'path': 'docs/readmes/145/' + name, 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()})
    shutil.copy2(p, r / 'docs/readmes/145' / name)
text = json.dumps(receipt, indent=2) + '\n'
(r / 'build/visual-review145.json').write_text(text, encoding='utf8')
(r / 'docs/readmes/145/visual-review.json').write_text(text, encoding='utf8')
shutil.copy2(r / 'build/visual145.json', r / 'docs/readmes/145/guide-validation.json')
print(json.dumps({'passed': True, 'appImagesReviewed': len(inventory['images']), 'guideImagesReviewed': len(receipt['guideImages'])}))
