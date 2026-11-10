from pathlib import Path
p=Path('workspace-desktop/test/transition108-integration.cjs');s=p.read_text(encoding='utf8');old="if(state.gate&&method==='isolated'&&String(args[0]).includes(\"one('table#data-table')\")&&++state.seen===state.gate)"
new="const matched=state.gate&&method==='isolated'&&String(args[0]).includes(\"one('table#data-table')\")?(await global.workspaceTest.api('/api/workflows/list',{})).workflows.find(w=>w.qualification105&&w.status==='running'&&w.qualification105.checked===state.gate-1):null;if(matched)"
assert old in s;s=s.replace(old,new).replace("boundary:state.gate,scope:","boundary:state.gate,checked:matched.qualification105.checked,workflowId:matched.id,scope:")
s=s.replace("row.boundary=w;row.boundaryOriginal=", "assert.equal(w.qualification105.checked,restart-1);assert.equal(blocked.workflowId,w.id);row.boundary=w;row.boundaryOriginal=")
s=s.replace("path.join(repo,'build/transition108')","path.join(repo,'build/transition108-final')");p.write_text(s,encoding='utf8',newline='\n')
p=Path('scripts/Inspect-Transition108.cjs');s=p.read_text(encoding='utf8').replace('function inspect(r){','function inspect(r,{prior=false}={}){').replace("t.name==='partial-restart108'?2:5","prior?(t.name==='partial-restart108'?0:1):(t.name==='partial-restart108'?2:5)")
s=s.replace('return{passed:true,source:r.source','return{passed:true,declaredRestartBoundaries:!prior,source:r.source').replace("scope:'Actual orchestration-only", "scope:(prior?'Prior trial integrity only: interruptions after zero/one completed examples; declared boundary contract refused. ':'')+'Actual orchestration-only")
s=s.replace('function refusals(r){','function refusals(r,options={}){').replace('assert.throws(()=>inspect(x),name)','assert.throws(()=>inspect(x,options),name)')
s=s.replace("if(require.main===module){const r=", "if(require.main===module){const prior=process.argv.includes('--prior');const r=").replace("'build/transition108/result.json'","'build/transition108-final/result.json'").replace('...inspect(r),refusals:refusals(r)','...inspect(r,{prior}),refusals:refusals(r,{prior})');p.write_text(s,encoding='utf8',newline='\n')
p=Path('scripts/Document-Transition108.py');s=p.read_text(encoding='utf8').replace('build/transition108','build/transition108-final')
s=s.replace("roots=['build/transition108-final',","roots=['build/transition108','build/transition108-final',").replace("extras=['build/lineage-change108.json'","extras=['build/initial108-build.json','build/analysis108-prior.json','build/prior-boundary108.json','build/inspect108-original-contract.log','build/transition108.log','build/lineage-change108.json'")
s=s.replace("assert analysis['passed'] and", "prior=load('build/analysis108-prior.json');assert prior['passed'] and not prior['declaredRestartBoundaries'] and load('build/prior-boundary108.json')['refused']\nassert analysis['declaredRestartBoundaries']\nassert analysis['passed'] and")
s=s.replace("js(d/'analysis.json',analysis)","js(d/'analysis.json',analysis)\njs(d/'prior-analysis.json',prior)")
s=s.replace("'passed':True,'java':tests,","'passed':True,'priorContractRefusal':load('build/prior-boundary108.json'),'priorIntegrityInspection':prior,'java':tests,")
s=s.replace('## Verification and honest boundaries\n','''## Original recorder attempt retained

The initial recorder counted browser frames and interrupted the first and second examples, rather than the declared third example and selected-source audit. The independent contract check refused those boundaries. Its twelve requests and full **{prior['cohortWallMs']/1000:.3f}s** cohort cost (**{prior['requestMs']/1000:.3f}s** in requests) remain in the [prior integrity analysis](docs/readmes/108/prior-analysis.json) and lossless originals. Those early recoveries are additional evidence, not substitutes for the required tests.

The recorder was corrected to use Java's actual completed-example count and immediately assert the exact boundary. The full twelve-request study then ran again in fresh projects with unchanged application inputs and JARs. The final results above belong to that separate trial. Preparation, rejected timing, restart and refusal costs were not discarded. Twenty-eight altered-record controls also check the separately labelled prior analysis.

## Verification and honest boundaries
''')
s=s.replace('Seven actual workflow completions, including two abrupt recoveries, and five required refusals.','Seven final actual workflow completions, including two declared abrupt recoveries, and five required refusals. The earlier mistimed trial and its costs are retained separately.')
s=s.replace('Two cold clean/upgrade pairs retain all costs;', 'Initial recorder-boundary refusal retained with full costs; corrected actual-state recorder reruns all12 requests fresh. Two cold clean/upgrade pairs retain all costs;')
p.write_text(s,encoding='utf8',newline='\n')
