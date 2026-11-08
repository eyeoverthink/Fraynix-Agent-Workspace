from pathlib import Path
r=Path.cwd();p=r/'scripts/Inspect-Procedures093.cjs'
s=(r/'scripts/Inspect-Procedures090.cjs').read_text(encoding='utf8')
s=s.replace("['forecast','actual'].includes(phase)","['forecast','actual','reforecast'].includes(phase)")
s=s.replace("steps(t.family)-i+(phase==='forecast'?steps(t.family):0)","phase==='reforecast'?2*(steps(t.family)-i):steps(t.family)-i+(phase==='forecast'?steps(t.family):0)")
s=s.replace("let ticks=0,observed=0,last=-1;","let ticks=0,observed=0,last=-1;const uniqueStages=new Set();")
s=s.replace("const [phase,num]=t.stage.split('/'),i=+num;","assert(!uniqueStages.has(t.family+'/'+t.stage));uniqueStages.add(t.family+'/'+t.stage);const [phase,num]=t.stage.split('/'),i=+num;assert(phase!=='reforecast'||i>0);")
s=s.replace("frame=clone(r.definition.worldFrame);frame.bodies=", "const prediction=phase==='actual'?recoveredPrediction(j,p,i,r,runs,verified,bound):null;frame=clone(r.definition.worldFrame);frame.bodies=")
s=s.replace("const c=compare(runs.get(p.forecast[i]),r)","const c=compare(prediction,r)")
s=s.replace("hash(runs.get(p.forecast[i]))","hash(prediction)")
s=s.replace("const passed=completed.every(Boolean);", "for(const [key,v]of Object.entries(p.reforecasts093||{})){assert(+key>0&&+key<steps(family)&&String(+key)===key);assert(bound.has(v.forecastId));}const passed=completed.every(Boolean);")
s += '''
// Independent reconstruction: hashes, exact state, provenance and nanosecond chronology.
function instant093(s){const m=/^(.*T\\d\\d:\\d\\d:\\d\\d)(?:\\.(\\d{1,9}))?Z$/.exec(s);assert(m);return BigInt(Date.parse(m[1]+'Z'))*1000000n+BigInt((m[2]||'').padEnd(9,'0'));}
function recoveredPrediction(j,p,i,actual,runs,verified,bound){
 const original=runs.get(p.forecast[i]),saved=p.reforecasts093?.[i],frame=actual.definition.worldFrame;
 if(canonical(original.definition)===canonical(actual.definition)&&canonical(original.plan)===canonical(actual.plan)){assert(!saved);return original;}
 assert(i>=1&&j.resumes>=1);let anchor;
 for(let k=0;k<i;k++){const a=j.arm.attempts.find(x=>x.id===p.actual[k]),r=runs.get(a.id);if(a.unknownTail&&r.resumes>0&&verified.get(a.id))anchor=r;}
 assert(anchor&&anchor.resumes<=2);assert(saved);
 const previous=runs.get(p.actual[i-1]),priorFrame=clone(previous.definition.worldFrame);priorFrame.bodies=previous.final.bodies.map(b=>Object.fromEntries(['id','position','rotation','velocity','angularVelocity'].map(k=>[k,b[k]])));priorFrame.elapsedTicks+=previous.final.tick;assert.deepEqual(priorFrame,frame);
 const expected={format:'recovered-stage-forecast-093-v1',step:i,originalForecastId:original.id,originalForecastSha256:hash(original),previousActualId:previous.id,previousActualSha256:hash(previous),resumedAncestorId:anchor.id,resumedAncestorSha256:hash(anchor),frameSha256:hash(frame),planSha256:hash(actual.plan)};
 const given=clone(saved);delete given.forecastId;assert.deepEqual(given,expected);
 const forecast=runs.get(saved.forecastId),a=j.arm.attempts.find(x=>x.id===saved.forecastId);assert(a&&forecast&&verified.get(a.id));assert(protectedTrace(j.spec,forecast,j.initialFrame));assert.equal(a.family,p.family);assert.equal(a.stage,'reforecast/'+i);assert.equal(a.admissionRemaining,2*(steps(p.family)-i));assert.deepEqual(forecast.definition,actual.definition);assert.deepEqual(forecast.plan,actual.plan);assert.deepEqual(p.waypoints[a.stage],p.waypoints['actual/'+i]);
 const attempts=j.arm.attempts,pi=attempts.findIndex(x=>x.id===previous.id),fi=attempts.findIndex(x=>x.id===a.id),ai=attempts.findIndex(x=>x.id===actual.id);assert(pi>=0&&pi<fi&&fi<ai);
 assert(instant093(attempts[pi].settledAt)<=instant093(a.plannedAt));assert(instant093(a.settledAt)<=instant093(attempts[ai].plannedAt));assert(instant093(a.plannedAt)<=instant093(forecast.createdAt));bound.add(a.id);return forecast;
}
'''
p.write_text(s,encoding='utf8',newline='\n')
