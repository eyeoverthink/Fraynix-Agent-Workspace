from pathlib import Path
r=Path.cwd();p=r/'docs/publication141/document141.py';s=p.read_text(encoding='utf8')
s=s.replace("'drift141':(x['audit'].get('drift141') or x['audit'].get('drift139') or x['audit'].get('drift138'))","'recovery141':x['audit'].get('recovery141')")
a=s.index("sums={}");b=s.index("comparison=",a)
s=s[:a]+'''sums={}
for policy in lock['methods']:
 selected=[x for x in rows if x['policy']==policy]
 sums[policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain','confirmedDistractorMatches']},'additional':{k:sum((x['recovery141'] or {}).get('additional',{}).get(k,0) for x in selected) for k in ['returned','correct','incorrect','unknown']},'filtered':{k:sum((x['recovery141'] or {}).get('filtered',{}).get(k,0) for x in selected) for k in ['accepted','correct','incorrect','unknown']}}
candidate=sums['recovery-guard-141']['tracking'];base=sums['legacy-flow']['tracking'];earlier=sums['foreground-guard-139']['tracking'];extra=sums['recovery-guard-141']['additional'];filtered=sums['recovery-guard-141']['filtered']
finding=f"Fresh candidate versus unchanged139: **{candidate['correct']} vs {earlier['correct']} correct**, **{candidate['overlapFailures']} vs {earlier['overlapFailures']} wrong**, **{candidate['missing']} vs {earlier['missing']} missing**, and **{candidate['unsupported']} vs {earlier['unsupported']} hidden outputs**. There are {candidate['visible']} visible/partial references, {candidate['hidden']} confirmed absent and {candidate['uncertain']} uncertain per method. Additional source recoveries: **{extra['correct']} correct, {extra['incorrect']} incorrect, {extra['unknown']} unknown**. The filter accepted **{filtered['correct']} correct, {filtered['incorrect']} incorrect, {filtered['unknown']} unknown** of these. Default flow: {base['correct']} correct, {base['overlapFailures']} wrong, {base['missing']} missing and {base['unsupported']} hidden outputs."
finding+=" The candidate stays an explicit experiment. A positive controlled return test proves the implemented mechanism; fresh camera outcomes determine whether it is useful. No semantic identity or universal switch-free claim follows from these bounded cases."
timing=[]
'''+s[b:]
s=s.replace('Twelve actual-app fresh comparisons use',"{len(study['runs'])} actual-app fresh comparisons use")
s=s.replace("'docs/EDITION_141_SOURCE_NOTE.md',",'')
a=s.index("warn='| Method / source");b=s.index("cost='|",a);s=s[:a]+"warn='Baseline139 accepted tracker proposals are preserved exactly. Supplemental measurements may change later filter estimates; compare complete final outcomes as well as additional raw boxes. The monitor preserves139 tracker and filter outputs.\\n'\n"+s[b:]
a=s.index(" if x['drift141']:");b=s.index(" cost+=",a);s=s[:a]+s[b:]
a=s.index("warn+='\\nFirst wrong");b=s.index("elapsed=",a);s=s[:a]+s[b:]
a=s.index("restorations='");b=s.index("next_steps=",a)
s=s[:a]+'''restorations='| Source / method | Additional boxes: correct / incorrect / unknown | Filter accepted: correct / incorrect / unknown |\\n| --- | ---: | ---: |\\n'
for x in rows:
 d=x['recovery141']
 if d:
  a=d['additional'];f=d['filtered'];restorations+=f"| {x['case']} / {x['policy']} | {a['correct']} / {a['incorrect']} / {a['unknown']} | {f['correct']} / {f['incorrect']} / {f['unknown']} |\\n"
'''+s[b:]
s=s.replace('CorroboratedGuard141.java','LossRecovery141.java').replace('CorroboratedAudit141.java','LossRecoveryAudit141.java').replace('RestorationGate141.java','RecoverySearch141.java').replace('Corroborated guard and monitor','Loss recovery and monitor').replace('Bounded restoration','Multi-location source search').replace('Corroborated warnings and bounded restoration','Recovery after loss').replace('corroborated warnings and bounded restoration','recovery after loss')
p.write_text(s,encoding='utf8')
p=r/'docs/publication141/guide-template.html';s=p.read_text(encoding='utf8').replace('Corroborated warnings and restoration','Recovery after loss').replace('Detect suspect motion.<br>Keep the evidence.','Recover after loss.<br>Keep identity in question.').replace('An explicit Java guard separates advisory disagreement from corroborated warnings. Bounded restoration requires repeated original-source support and uninterrupted motion; time alone never releases a box. A separate monitor preserves baseline outputs.','An explicit Java recovery method searches multiple locations and scales after genuine flow loss. Immutable source appearance, spatial support, competitor rejection and three adjacent motion confirmations qualify supplemental boxes. Existing139 accepted tracker proposals are preserved; a separate monitor preserves its filter outputs too.').replace('README-139','README-140').replace('Three fixed fresh public recordings','Eligible windows from three prespecified public recordings').replace('cat1, cat2 and group1','car3, following and person2').replace('Warnings only</option>','Recovery monitor</option>').replace('Corroborated guard</option>','Loss recovery candidate</option>');p.write_text(s,encoding='utf8')
p=r/'docs/publication141/visual141.cjs';s=p.read_text(encoding='utf8').replace("count(),12","count(),data.comparison.rows.length").replace("count(),3","count(),data.comparison.sources.records.length").replace('CorroboratedGuard141','LossRecovery141');s=s.replace("const browser=await", "const data=JSON.parse(await fs.readFile('docs/readmes/141/system-map.json','utf8'));const browser=await");p.write_text(s,encoding='utf8')
for name in ['release141.py','finish141.py']:
 p=r/'docs/publication141'/name;s=p.read_text(encoding='utf8').replace('corroborated warnings and bounded restoration','recovery after loss');p.write_text(s,encoding='utf8')
print('Adapted report to supplemental-box and filter outcomes; no results invented')
