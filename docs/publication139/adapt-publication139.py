from pathlib import Path
r=Path.cwd();old='5ee114d6f25ad9b7625cf5bd4f4161dbdd9ad89f';parent='7e66b0c86d3a665b5793ba795298a4eeb2c81cfa'
for name in ['document','freeze','release','audit-publication','finish']:
 p=r/f'docs/publication138/{name}138.py';s=p.read_text(encoding='utf8').replace('138','139').replace(old,parent)
 s=s.replace("['latest']==137","['latest']==138").replace("readmes/137/file-inventory.json","readmes/138/file-inventory.json")
 s=s.replace('146-drift-evidence','147-foreground-structure').replace('DriftGuard139','ForegroundGuard139').replace('DriftAudit139','ForegroundAudit139').replace('AppearanceEvidence139','SpatialEvidence139').replace('drift-guard-139','foreground-guard-139')
 if name=='document':
  s=s.replace("x['audit'].get('drift139')","(x['audit'].get('drift139') or x['audit'].get('drift138'))")
  s=s.replace('Twelve actual-app fresh comparisons','Sixteen actual-app fresh comparisons').replace('Visibility and bottle distractor labels are source-only AI, not independently human adjudicated.','External VOT rectangles/absence with separate source-only AI visibility/cause review; no independent human adjudication of added labels.')
  s=s.replace('build/otb139/','build/vot139/').replace("'ATTEMPTS':", "'SOURCE_REVIEW':sources['sourceReview'],'ATTEMPTS':")
  s=s.replace("'Drift guard and monitor'","'Foreground guard and monitor'").replace("'Immutable source appearance'","'Immutable spatial parts'")
  s=s.replace("# Drift evidence before loss — edition139", "# Foreground motion and spatial structure — edition139")
  s=s.replace('Edition139 — drift evidence before loss','Edition139 — foreground motion and spatial structure')
 if name=='freeze':s=s.replace('Drift evidence before tracking loss','Foreground motion, spatial structure and disappearance')
 if name=='release':s=s.replace('drift detection and independent original-pixel evidence','foreground motion and spatial structure with independent evidence')
 (r/f'docs/publication139/{name}139.py').write_text(s,encoding='utf8')
# Keep static guide behavior; adapt labels and sources only.
s=(r/'docs/publication138/guide-template.html').read_text(encoding='utf8').replace('138','139').replace('README-137','README-138')
s=s.replace('Drift evidence before loss','Foreground motion and spatial structure').replace('An explicit Java drift guard compares proposed motion with immutable original pixels.','An explicit Java guard compares proposed motion with immutable textured parts, their spatial arrangement and nearby background motion.')
s=s.replace('Three fresh public recordings, four target windows. External benchmark boxes and source-only AI visibility; no human adjudication.','Four fresh public recordings: three disappearance/return windows and one visible-motion control. External rectangles/absence plus separate source-only AI review; no independent human adjudication of added labels.')
s=s.replace('Public OTB Skating2, Suv and Liquor pixels','Public VOT-LT2018 ballet, bicycle, bike1 and bird1 pixels').replace('All three methods','All four methods').replace('<option value="monitor-only-139">','<option value="drift-guard-138">Earlier appearance guard</option><option value="monitor-only-139">').replace('drift-guard-139','foreground-guard-139')
s=s.replace('Distractor labels cover both skaters and ten bottle samples; other unlabelled frames cannot prove switch-free tracking. Only one confidently fully hidden frame.','Distractor geometry is not independently labelled. Unlabelled distractors cannot prove switch-free tracking. Missing measurements, boundary uncertainty and full costs are reported.')
(r/'docs/publication139/guide-template.html').write_text(s,encoding='utf8')
s=(r/'docs/publication138/visual138.cjs').read_text(encoding='utf8').replace('138','139').replace('count(),12','count(),16').replace('drift-guard-139','foreground-guard-139').replace('DriftGuard139','ForegroundGuard139')
(r/'docs/publication139/visual139.cjs').write_text(s,encoding='utf8')
