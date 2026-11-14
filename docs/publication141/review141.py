"""Record the completed source-only visual review; never reads predictions."""
from pathlib import Path
import json,datetime
r=Path.cwd();index=json.loads((r/'build/vot141/source-index.json').read_text(encoding='utf8'));out=r/'docs/publication141/visibility141.json';assert not out.exists()
v={'reviewedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'reviewer':'Codex source-only AI; not independent human adjudication','summary':'All148 selected source images reviewed on seven contact sheets before predictions, with original-resolution boundary checks. Following255–306 shows a white car approaching the right edge and returning into view; part of the car remains visible throughout the external NaN interval270–286, so those17 images are uncertain/unscorable, not confirmed hidden. Finite frames259–269 and287–306 have a clipped target and are labelled partial. Person2 frames1–96 show an unobstructed person walking on grass, a visible-motion control. Car3 is excluded by the frozen size rule: its first rectangle is16px wide in1280px, below.025, with no qualifying annotated disappearance interval. No replacement source, full-occlusion success claim, human adjudication or independent distractor geometry.','sources':{}}
for s in index:
 name=s['name'];entry={'reviewedIndices':s['selection']['indices'],'reviewedSheets':s['sheets'],'partial':[],'occluded':[],'out-of-view':[],'uncertain':[]}
 if name=='following':entry.update(partial=[[259,269],[287,306]],uncertain=[[270,286]],originalResolutionChecks=[259,270,278,286,287,306],note='NaN is not evidence of complete absence; the car fragment remains visible. No independent rectangle is invented for those images.')
 else:entry.update(originalResolutionChecks=[1,48,96],note='Target remains unobstructed throughout the selected96 frames. Contact with the ground is not independently calibrated here.')
 v['sources'][name]=entry
out.write_bytes((json.dumps(v,indent=2)+'\n').encode());print(v['summary'])
