// Read-only presentation of Java-owned records. No outcome, eligibility or authority is created here.
export const kinds={chat:{label:'Chat',operation:'agency043',history:'history',read:'read',verify:'verify',archive:'archive'},workflow:{label:'Workflow',operation:'workflow',history:'history',read:'read',verify:'verify-archive',archive:'archive'},child:{label:'Step',operation:'workflow',history:'child-history',read:'child-read',verify:'verify-child',archive:'archive-children'}};
export function rowsFrom(snapshots){return Object.entries(kinds).flatMap(([kind])=>(snapshots[kind]?.records||[]).map(r=>({...r,kind,key:kind+':'+r.id,title:r.goal||r.title||r.stage||r.id})));}
export function pageRows(rows,{query='',kind='all',state='all',page=0,size=12}={}){
 const terms=String(query).toLocaleLowerCase().trim().split(/\s+/).filter(Boolean),filtered=rows.filter(r=>(kind==='all'||r.kind===kind)&&(state==='all'||state==='archived'&&r.archived||state==='active'&&!r.archived||state==='attention'&&['interrupted','cancelled'].includes(r.status)||state==='completed'&&r.status==='completed')&&terms.every(t=>[r.title,r.id,r.status,r.outcome||'',r.stage||'',r.workflowId||''].join(' ').toLocaleLowerCase().includes(t)));
 const total=filtered.length,pages=Math.max(1,Math.ceil(total/size)),index=Math.min(pages-1,Math.max(0,Number.isInteger(page)?page:0));return{rows:filtered.slice(index*size,(index+1)*size),total,pages,page:index};
}
export function related(kind,record){
 const links=[];const add=(kind,id,label)=>{if(id&&!links.some(r=>r.kind===kind&&r.id===id))links.push({kind,id,label});};
 if(kind==='chat'){add('workflow',record.context044?.ownedWorkflowId||record.completion044?.javaWorkflow?.id,'Underlying workflow');add('child',record.context044?.ownedGoalId,'Underlying step');}
 if(kind==='workflow')for(const s of record.stages||[]){add('workflow',s.workflowId,s.name||'Workflow stage');add('child',s.goalId,s.name||s.stage||'Child step');}
 if(kind==='child')add('workflow',record.workflowId,'Owning workflow');return links;
}
export function reportOf(kind,record){return kind==='chat'?record.completion044?.javaWorkflow?.report:record.report;}
export function outcome(record){if(record.outcome==='completed-unverified')return'Completed without verified task success';if(record.outcome==='verified')return'Original outcome: verified';if(record.status==='completed')return'Completed — stored outcome; use a fresh recheck for current evidence';if(record.refusal106)return'Refused — original reason retained';return String(record.status||'Unknown').replaceAll('-',' ');}
export class SelectionEpoch{constructor(){this.value=0;}next(){return ++this.value;}current(value){return this.value===value;}}
