"""Prepare a per-source app study, with bounded existing capacities and every original evidence check."""
from pathlib import Path
r=Path.cwd()
s=(r/'workspace-desktop/test/trajectory145-tail-integration.cjs').read_text(encoding='utf8')
s=s.replace("const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/trajectory145-tail-app')","const group=process.env.RETURN146_GROUP;assert(/^[a-z0-9-]+$/.test(group||''),'Choose a declared study group');\nconst repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/return146-'+group+'-app')")
s=s.replace('DRIFT145_TAIL_RESUME','RETURN146_RESUME').replace('docs/publication145/source145-tail.json','docs/publication146/source146.json').replace('docs/OBSERVATION_145_LOCK.json','docs/OBSERVATION_146_LOCK.json')
s=s.replace('assert.equal(sha(lockBytes),manifest.algorithmLockSha256);','manifest.records=manifest.records.filter(x=>x.group===group);assert(manifest.records.length>=2);const firstCase=manifest.records.find(x=>x.role===\'evaluation\').case,lastCase=manifest.records.filter(x=>x.role===\'evaluation\').at(-1).case;assert.equal(sha(lockBytes),manifest.algorithmLockSha256);')
s=s.replace('Edition 145 | bounded trajectory recovery and competing paths','Edition 146 | observed continuity for returning objects')
s=s.replace("const restored={returned:0","const screened={returned:0,correct:0,incorrect:0,unknown:0},restored={returned:0")
s=s.replace("if(t.trajectory145?.restored){","if(t.trajectory145?.restored&&!t.continuity146?.blocked){")
s=s.replace("if(t.trajectory145?.blocked??t.ambiguity144?.blocked)","if(t.continuity146?.blocked){screened.returned++;screened[outcome]++;}if(t.continuity146?(t.continuity146.blocked||t.continuity146.inheritedBlocked):(t.trajectory145?.blocked??t.ambiguity144?.blocked))")
s=s.replace('const d=audit.trajectory145||','const d=audit.continuity146||audit.trajectory145||')
s=s.replace('if(audit.trajectory145||audit.ambiguity144)','if(audit.continuity146||audit.trajectory145||audit.ambiguity144)').replace('if(audit.trajectory145)assert.deepEqual(d.restored,restored);','if(audit.continuity146||audit.trajectory145)assert.deepEqual(d.restored,restored);if(audit.continuity146)assert.deepEqual(d.screened,screened);')
s=s.replace("f.spec.case==='basketball-120'&&policy==='trajectory-guard-145'","f.spec.case===firstCase&&policy==='return-guard-146'")
s=s.replace("policy==='trajectory-monitor-145'","policy==='return-monitor-146'").replace("x.policy==='ambiguity-guard-144'","x.policy==='trajectory-guard-145'")
s=s.replace('delete frame.objects[0].tracker.trajectory145','delete frame.objects[0].tracker.continuity146')
s=s.replace("policy==='trajectory-guard-145'","policy==='return-guard-146'")
# Above replacement also changes comparison lookups; restore them to the actual incumbent.
s=s.replace("x.policy==='return-guard-146'","x.policy==='trajectory-guard-145'")
old="if(parent.box){const exact=structuredClone(run.trace[i].objects[0].tracker);delete exact.trajectory145;assert.deepEqual(exact,parent);}"
new="if(parent.box){const exact=structuredClone(run.trace[i].objects[0].tracker),screen=exact.continuity146;if(!screen.blocked){delete exact.continuity146;assert.deepEqual(exact,parent);}else{assert(!parent.references143.baselineAccepted);assert(!parent.ambiguity144.latched);assert(parent.trajectory145.accepted);assert(!exact.box);}}"
assert old in s;s=s.replace(old,new).replace('delete t.references143;delete t.ambiguity144;delete t.trajectory145;','delete t.references143;delete t.ambiguity144;delete t.trajectory145;delete t.continuity146;')
s=s.replace('assert.equal(audit.trajectory145.baselineAcceptedPreserved,preserved);','assert.equal(audit.continuity146.baselineAcceptedPreserved,preserved);')
s=s.replace("if(spec.case==='basketball-120')","if(spec.case===lastCase)")
s=s.replace('recovery:audit.trajectory145?.additional','screened:audit.continuity146?.screened,recovery:audit.continuity146?.additional||audit.trajectory145?.additional')
s=s.replace("Monitor preserves every144 tracker/filter output; candidate preserves every accepted144 tracker proposal, including all139 baseline proposals.","Monitor146 preserves every145 tracker/filter output; candidate preserves139 baseline and145 competed restorations, independently scoring additional146 refusals without a warning.")
s=s.replace("const target=record.runs.find(r=>r.policy==='trajectory-guard-145')","const target=record.runs.find(r=>r.policy==='return-guard-146')")
s=s.replace("r.completedFrames>=10","r.completedFrames>=3")
# Close the source audit panel before graph capture, preserving all original screenshots without hidden content.
s=s.replace("await page.evaluate(async ({id,ordinal})=>", "await page.locator('#association134-panel').evaluate(e=>e.open=false);await page.evaluate(async ({id,ordinal})=>")
old="record.checks.push('Explicit method selection, recovery explanation, original-source overlay, independent audit UI, complete bounded export and exact stopped-prefix resume passed. Native physical world unchanged.');"
new="""if(!donors[group].retained){
   const fallback=record.runs.filter(x=>x.policy==='foreground-guard-139').at(-1);assert(fallback);
   const exported=await request('observations','export',{id:fallback.run.id}),envelope=JSON.parse(await fs.readFile(exported.path,'utf8')),raw=zlib.gunzipSync(Buffer.from(envelope.data,'base64'));assert.equal(sha(raw),envelope.rawSha256);assert.equal(JSON.parse(raw).images.length,fallback.run.totalFrames+1);
   record.export={bytes:(await fs.stat(exported.path)).size,rawBytes:raw.length,sha256:sha(raw),scope:'Fallback baseline export; reference-dependent methods unavailable'};
   record.checks.push('Donor retention refusal preserved. Fixed baseline and its complete export checked; no claim of146 method selection, recovery or prefix-resume validation on this unavailable source.');
  }else{assert(record.export&&record.stoppedPrefix);record.checks.push('Explicit method selection, recovery explanation, original-source overlay, independent audit UI, complete bounded export and exact stopped-prefix resume passed. Native physical world unchanged.');}"""
assert old in s;s=s.replace(old,new)
old="const target=record.runs.find(r=>r.policy==='return-guard-146');for(const data of [target.dataset,donors[target.group].dataset]){"
new="const target=record.runs.find(r=>r.policy==='return-guard-146')||record.runs.find(r=>r.policy==='foreground-guard-139');assert(target);const donor=donors[target.group];const tamperCases=donor.retained?[{data:target.dataset,evidence:target},{data:donor.dataset,evidence:target}]:[{data:target.dataset,evidence:target},{data:donor.dataset,evidence:donor}];for(const {data,evidence} of tamperCases){"
assert old in s;s=s.replace(old,new)
tail=s.index('const tamperCases=');s=s[:tail]+s[tail:].replace('id:target.audit.id','id:evidence.audit.id').replace('id:target.run.id','id:evidence.run.id').replace('),target.audit);','),evidence.audit);')
(r/'workspace-desktop/test/return146-integration.cjs').write_text(s,encoding='utf8')
print('Prepared actual per-source integration study')
