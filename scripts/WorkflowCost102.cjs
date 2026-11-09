'use strict';
const assert=require('node:assert/strict');
// Exclusive wall-time accounting. Nested host calls are never added twice.
function partition(start,end,spans){
 assert(Number.isFinite(start)&&Number.isFinite(end)&&end>start,'Invalid task interval');
 const ranks=['pause-restart','generation','browser-verification'];
 const valid=spans.map(s=>{assert(ranks.includes(s.kind));assert(Number.isFinite(s.start)&&Number.isFinite(s.end)&&s.end>=s.start,'Invalid measurement span');assert(s.start>=start-1000&&s.end<=end+1000,'Span is outside the task');return {...s,start:Math.max(start,s.start),end:Math.min(end,s.end)};}).filter(s=>s.end>s.start);
 const points=[...new Set([start,end,...valid.flatMap(s=>[s.start,s.end])])].sort((a,b)=>a-b),costs=Object.fromEntries([...ranks,'coordination-and-other'].map(x=>[x,0]));let overlap=0;
 for(let i=1;i<points.length;i++){const a=points[i-1],b=points[i],active=valid.filter(s=>s.start<b&&s.end>a);const kinds=new Set(active.map(s=>s.kind));const chosen=ranks.find(x=>kinds.has(x))||'coordination-and-other';costs[chosen]+=b-a;if(kinds.size>1)overlap+=b-a;}
 assert.equal(Object.values(costs).reduce((a,b)=>a+b,0),end-start);return {elapsedMs:end-start,exclusiveMs:costs,crossCategoryOverlapMs:overlap,scope:'Priority pause/restart, then generation, then browser verification; remaining time includes Java computation, persistence, scheduling and harness polling. Stage durations overlap these categories and are not added.'};
}
function taskCosts(row,hostSpans){
 const spans=[],attempts=[];for(const job of row.generation){assert(job.timings102?.length,'Missing model timing');assert.equal(job.timings102.length,job.attempts,'Missing admitted attempt cost');for(const t of job.timings102){assert(['returned','failed'].includes(t.status)&&t.endedAt,'Incomplete call is unknown, not zero');const start=Date.parse(t.startedAt),end=Date.parse(t.endedAt);assert(Number.isFinite(t.elapsedNanos)&&t.elapsedNanos>=0);assert(Math.abs(t.elapsedNanos/1e6-(end-start))<500,'Wall/monotonic timing disagreement');spans.push({kind:'generation',start,end});attempts.push({jobId:job.id,attempt:t.attempt,status:t.status,elapsedMs:t.elapsedNanos/1e6,rejected:!!job.rejections?.some(r=>r.attempt===t.attempt)});}}
 for(const t of hostSpans)if(t.start>=row.start&&t.end<=row.end){assert(BigInt(t.nanos)>=0n);assert(Math.abs(Number(t.nanos)/1e6-(t.end-t.start))<500);spans.push({kind:'browser-verification',start:t.start,end:t.end});}
 if(row.restart){assert(row.pauseEnd>row.pauseStart);spans.push({kind:'pause-restart',start:row.pauseStart,end:row.pauseEnd});}
 const stages={};const w=row.workflow;for(const child of row.children){const start=Date.parse(child.createdAt),end=Date.parse(child.updatedAt);assert(Number.isFinite(start)&&end>=start,'Missing child boundaries');stages[child.workflowStage]={wallMs:end-start,scope:'Includes waits, generation, checks and recovery within this child; overlaps exclusive task costs.'};}
 return {...partition(row.start,row.end,spans),attempts,stageWall:stages,drafts:row.children[1].draftNumber,failedAudits:w.auditHistory.filter(a=>!a.passed).length,resumes:row.task.resumes};
}
module.exports={partition,taskCosts};
