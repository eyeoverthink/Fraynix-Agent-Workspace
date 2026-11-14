from pathlib import Path
import shutil
r=Path.cwd();old=r/'docs/publication140';new=r/'docs/publication141'
if (new/'document141.py').exists():raise SystemExit('Already prepared; preserve work')
for name in ['audit-publication140.py','freeze140.py','release140.py','finish140.py','finalize-inventory140.py','visual140.cjs','guide-template.html','guide.js','document140.py','notes140.py','README-template.md','validate140.py','download140.py','select140.py','prepare140.py']:
 s=(old/name).read_text(encoding='utf8').replace('140','141').replace('86e0e5a3604df80a50699db762474240fa759851','d27846b206c73b8dc3326b398f746d0212a01fd9')
 s=s.replace('148-corroborated-restoration','149-loss-recovery').replace('corroborated-guard-141','recovery-guard-141').replace('monitor-only-141','recovery-monitor-141').replace('corroborated141-development','recovery141-development').replace('drift141-app','recovery141-app')
 if name=='freeze140.py':s=s.replace("prior['latest']==139","prior['latest']==140").replace('Corroborated warnings and bounded source restoration','Verified recovery after tracking loss')
 if name=='document140.py':s=s.replace('docs/readmes/139/file-inventory.json','docs/readmes/140/file-inventory.json')
 if name=='validate140.py':s=s.replace("tests=['CorroboratedGuard141Test'","tests=['LossRecovery141Test','CorroboratedGuard140Test'")
 if name in ['download140.py','select140.py']:s=s.replace("['cat1','cat2','group1']","['car3','following','person2']")
 if name=='select140.py':s=s.replace("result=[]","result=[];excluded=[]").replace("assert measurable(ground[0],w,h),(name,'No qualifying measurable event or control seed')","if not measurable(ground[0],w,h):\n    excluded.append({'name':name,'reason':'No qualifying measurable event or control seed','events':events,'skippedEvents':skipped});continue")
 if name=='select140.py':s=s.replace(" (folder/'source-index.json')"," (folder/'excluded.json').write_text(json.dumps(excluded,indent=2)+'\\n',encoding='utf8')\n (folder/'source-index.json')")
 if name=='prepare140.py':
  a=s.index("'excludedSources':");b=s.index(",'records':records",a);s=s[:a]+"'excludedSources':json.loads((folder/'excluded.json').read_text(encoding='utf8'))"+s[b:]
 (new/name.replace('140','141')).write_text(s,encoding='utf8')
shutil.copy2(old/'vot-metadata-before-lock.json',new/'vot-metadata-before-lock.json')
shutil.copytree(r/'workspace-engine/build/test-results/test',r/'build/validation141/fifth')
p=r/'workspace-desktop/renderer/observation-panel.js';s=p.read_text(encoding='utf8')
s=s.replace('<option value="corroborated-guard-140">','<option value="recovery-guard-141">Experimental 141 · source-verified recovery after loss</option><option value="recovery-monitor-141">Control 141 · monitor without changing guard 139</option><option value="corroborated-guard-140">')
s=s.replace("if(suspect?.withheld&&suspect.proposal.box)","if((row.tracker.recovery141?.withheld??suspect?.withheld)&&suspect.proposal.box)")
s=s.replace("o.tracker.status+' / '+o.estimate.status", "o.tracker.status+' / '+o.estimate.status+(o.tracker.recovery141?' · Recovery: '+(o.tracker.recovery141.recovery.accepted?'source and motion confirmed':o.tracker.recovery141.recovery.attempted?'checking '+o.tracker.recovery141.recovery.confirmations+'/3 adjacent images':'inactive'):'')")
p.write_text(s,encoding='utf8')
p=r/'workspace-desktop/renderer/association134-panel.js';s=p.read_text(encoding='utf8');marker='if(a.appearance135?.available)'
s=s.replace(marker,"if(a.recovery141){const d=a.recovery141,p=document.createElement('p');p.id='recovery141-summary';p.textContent=`Recovery after loss: ${d.additional.correct} correct additional boxes, ${d.additional.incorrect} incorrect, ${d.additional.unknown} unscored. Filter accepted: ${d.filtered.correct} correct, ${d.filtered.incorrect} incorrect, ${d.filtered.unknown} unscored. ${d.baselineAcceptedPreserved} baseline tracker proposals preserved; ${d.replayedFrames} original images replayed. No automatic appearance memory.`;host.append(p);}"+marker);p.write_text(s,encoding='utf8')
print('Prepared helpers; reports and fresh evaluation not yet run')
