from pathlib import Path
r=Path.cwd();p=r/'workspace-desktop/test/recovery141-integration.cjs'
assert not p.exists()
s=(r/'workspace-desktop/test/corroborated140-integration.cjs').read_text(encoding='utf8').replace('140','141').replace('drift141-app','recovery141-app').replace('corroborated-guard-141','recovery-guard-141').replace('monitor-only-141','recovery-monitor-141')
s=s.replace('corroborated warnings and bounded restoration and independent evidence','recovery after loss and independent evidence')
a=s.index(' function independent(');b=s.index(' async function track(',a)
s=s[:a]+''' function independent(run,audit,label){
  const extra={returned:0,correct:0,incorrect:0,unknown:0},filtered={accepted:0,correct:0,incorrect:0,unknown:0};let preserved=0;
  for(const row of audit.scores){
   const object=run.trace[row.ordinal-1].objects[0],t=object.tracker;
   const actual=row.accepted&&row.targetBox?overlap(row.box,row.targetBox):0;assert(Math.abs(actual-row.targetOverlap)<1e-9);
   const other=row.accepted?Math.max(0,...(row.otherBoxes||[]).map(b=>overlap(row.box,b))):0;assert.equal(row.confirmedDistractorMatch,row.accepted&&other>=.5&&(!row.targetBox||actual<.2));
   const m=t.recovery141;if(!m)continue;assert.equal(m.automaticAdmissions,0);if(m.baselineAccepted)preserved++;
   if(!m.recovery.accepted)continue;
   const seen=['visible','partial'].includes(row.visibility),lost=['occluded','out-of-view'].includes(row.visibility);
   const outcome=seen&&overlap(t.box,row.targetBox)>=.5?'correct':seen||lost?'incorrect':'unknown';extra.returned++;extra[outcome]++;
   if(object.estimate.accepted){filtered.accepted++;filtered[outcome]++;}
  }
  if(audit.recovery141){const d=audit.recovery141;assert.deepEqual(d.additional,extra);assert.deepEqual(d.filtered,filtered);assert.equal(d.baselineAcceptedPreserved,preserved);assert.equal(d.replayedFrames,run.trace.length);}
 }
'''+s[b:]
s=s.replace("x.policy==='legacy-flow'","x.policy==='foreground-guard-139'").replace('tracker.drift141','tracker.recovery141').replace('audit.drift141?.summary','audit.recovery141?.additional')
needle="  if(f.spec.case===manifest.records[0].case&&policy==='recovery-guard-141'){assert.equal(run.resumes,1);"
addition="""  if(policy==='recovery-guard-141'){
   const base=record.runs.find(x=>x.caseName===f.spec.case&&x.policy==='foreground-guard-139');let preserved=0;
   for(let i=0;i<run.trace.length;i++){const b=base.run.trace[i].objects[0].tracker;if(!b.box)continue;const t=structuredClone(run.trace[i].objects[0].tracker);delete t.recovery141;assert.deepEqual(t,b);preserved++;}
   assert.equal(audit.recovery141.baselineAcceptedPreserved,preserved);
  }
"""
assert needle in s;s=s.replace(needle,addition+needle)
s=s.replace('t.objects[0].tracker.recovery141.suspect','t.objects[0].tracker.recovery141.recovery.accepted||t.objects[0].tracker.recovery141.lossObserved')
s=s.replace('Twelve actual-app fresh comparisons: three fixed target windows, four methods, identical source pixels. Independent host overlap/distractor/warning arithmetic agrees with Java. Monitor-only preserves every incumbent tracker/filter output.', 'All eligible fixed source windows compared with four methods and identical pixels. Independent host overlap/distractor/supplemental-outcome arithmetic agrees with Java. Monitor preserves every139 tracker/filter output; candidate preserves every accepted139 tracker proposal.')
s=s.replace('All12 runs/audits reverified','All recorded runs/audits reverified').replace('warning explanation','recovery explanation').replace('original-proposal overlay','original-source overlay')
p.write_text(s,encoding='utf8')
print(p)
