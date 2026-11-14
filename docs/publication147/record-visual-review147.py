"""Preserve the manually written inspection notes and exact reviewed image hashes."""
from pathlib import Path
import datetime,hashlib,json,shutil
r=Path.cwd();inventory=json.loads((r/'build/screens147/inventory.json').read_text(encoding='utf8'))
guide=json.loads((r/'build/visual147.json').read_text(encoding='utf8'));assert guide['passed']
notes=json.loads((r/'build/manual-visual147.json').read_text(encoding='utf8'));assert notes['passed'] and notes['sheetsReviewed']==inventory['sheets']
receipt={**notes,'reviewedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'reviewer':'Codex visual inspection of actual captured images','appImages':inventory['images'],'guideImages':[]}
for name in ['guide147-1450.png','guide147-1450-viewport.png','guide147-390.png','guide147-390-viewport.png']:
 p=r/'build'/name;receipt['guideImages'].append({'path':'docs/readmes/147/'+name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()});shutil.copy2(p,r/'docs/readmes/147'/name)
s=json.dumps(receipt,indent=2)+'\n';(r/'build/visual-review147.json').write_text(s,encoding='utf8');(r/'docs/readmes/147/visual-review.json').write_text(s,encoding='utf8');shutil.copy2(r/'build/visual147.json',r/'docs/readmes/147/guide-validation.json')
print(json.dumps({'passed':True,'imagesReviewed':len(inventory['images']),'guideImages':len(receipt['guideImages'])}))
