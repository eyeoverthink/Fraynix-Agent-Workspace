from pathlib import Path
r=Path.cwd();old=r/'docs/publication139';new=r/'docs/publication140'
if (new/'document140.py').exists():raise SystemExit('One-time bootstrap already completed. Preserve the reviewed publication tools; do not overwrite them.')
for name in ['audit-publication139.py','freeze139.py','release139.py','finish139.py','finalize-inventory139.py','visual139.cjs','guide-template.html','guide.js','document139.py']:
 s=(old/name).read_text(encoding='utf8').replace('139','140').replace('7e66b0c86d3a665b5793ba795298a4eeb2c81cfa','86e0e5a3604df80a50699db762474240fa759851')
 if name=='freeze139.py':s=s.replace("prior['latest']==138","prior['latest']==139").replace('Foreground motion, spatial structure and disappearance','Corroborated warnings and bounded source restoration')
 if name in ['release139.py','document139.py']:s=s.replace('147-foreground-structure','148-corroborated-restoration')
 if name=='document139.py':
  s=s.replace('docs/readmes/138/file-inventory.json','docs/readmes/139/file-inventory.json').replace('build/foreground140-development','build/corroborated140-development').replace("'foreground140-development'","'corroborated140-development'")
  s=s.replace('foreground-guard-140','corroborated-guard-140').replace("x['audit'].get('drift140') or x['audit'].get('drift138')","x['audit'].get('drift140') or x['audit'].get('drift139') or x['audit'].get('drift138')")
  s=s.replace('Twenty actual-app fresh comparisons','Sixteen actual-app fresh comparisons').replace("'source-labels140-first','source-labels140-exact-before'","'source-labels140-first','source-labels140-exact-before'")
  s=s.replace('Foreground motion and spatial structure','Corroborated warnings and bounded restoration').replace('ForegroundGuard140.java','CorroboratedGuard140.java').replace('ForegroundAudit140.java','CorroboratedAudit140.java').replace('SpatialEvidence140.java','SpatialEvidence139.java')
  s=s.replace("'SOURCE_REVIEW':sources['sourceReview']","'SOURCE_REVIEW':sources['sourceReview'],'RECOVERIES':restorations")
  marker="next_steps=(r/'build/next140.md').read_text"
  replacement="restorations='| Source / method | Advisory samples / on correct proposals | Continuity restorations: correct / incorrect / unknown |\\n| --- | ---: | ---: |\\n'\nfor x in rows:\n d=x['drift140'] or {}; a=d.get('advisories'); c=d.get('continuity')\n if a and c:restorations+=f\"| {x['case']} / {x['policy']} | {a['total']} / {a['onCorrectProposals']} | {c['correct']} / {c['incorrect']} / {c['unknown']} |\\n\"\n"+marker
  s=s.replace(marker,replacement)
 if name=='visual139.cjs':s=s.replace('foreground-guard-140','corroborated-guard-140').replace('ForegroundGuard140','CorroboratedGuard140').replace('count(),20','count(),16').replace('count(),5','count(),4')
 if name=='guide-template.html':
  s=s.replace('README-138','README-139').replace('foreground-guard-140','corroborated-guard-140').replace('drift-guard-138','foreground-guard-139').replace('Earlier appearance guard','Earlier foreground guard').replace('Foreground motion and spatial structure','Corroborated warnings and restoration')
  s=s.replace('Five fresh public recordings: three exits and returns, a structure occlusion and one visible-motion control.','Four fixed fresh public recordings, with independently annotated rectangles and explicitly reviewed visibility limits.')
  s=s.replace('ballet, bicycle, bike1, bird1 and car1','cat1, cat2, group1 and group2').replace('Drift guard</option>','Corroborated guard</option>')
  s=s.replace('An explicit Java guard compares proposed motion with immutable textured parts, their spatial arrangement and nearby background motion.','An explicit Java guard separates advisory disagreement from corroborated warnings. Bounded restoration requires repeated original-source support and uninterrupted motion; time alone never releases a box.')
 (new/name.replace('139','140')).write_bytes(s.encode('utf8'))
print('Prepared publication tools; no results or publication claims generated.')
