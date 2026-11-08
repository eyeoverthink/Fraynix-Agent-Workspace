from pathlib import Path
import json,hashlib,gzip,base64,subprocess,shutil,xml.etree.ElementTree as ET,os,re
r=Path.cwd();d=r/'docs/readmes/095';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
def sec(x):return 'not comparable' if x is None else f'{x/1000:.2f} s'
a=load(r/'build/analysis095.json');rec=load(r/'build/ui095-study/result.json');assert a['passed'] and rec['passed'];summary=a['summary']
xml=list((r/'build/regression095').glob('TEST-*.xml'));assert xml
tests={'cases':0,'failures':0,'errors':0,'skipped':0,'classes':len(xml),'skips':[]}
for f in xml:
 t=ET.parse(f).getroot();tests['cases']+=int(t.attrib['tests'])
 for k in ['failures','errors','skipped']:tests[k]+=int(t.attrib.get(k,0))
 for c in t.findall('testcase'):
  if c.find('skipped') is not None:tests['skips'].append(c.attrib)
assert tests['failures']==tests['errors']==0
assert 'pass 135' in (r/'build/desktop095.log').read_text(encoding='utf-8-sig')
accounting_log=(r/'build/cost-tests095.log').read_text(encoding='utf-8-sig');accounting_count=int(re.search(r'pass\s+(\d+)',accounting_log).group(1));assert accounting_count>=6 and 'fail 0' in accounting_log
identity=load(r/'build/source-comparison095.json');assert identity['identical']==334 and not identity['changed']
mutation=load(r/'build/mutations095.json');assert mutation['passed']
parent_ci=load(r/'build/parent-ci095.json');assert len(parent_ci['failedClasses'])==4
def comparison(x):return 'an unavailable comparison' if x is None else sec(abs(x))+(' less' if x>=0 else ' more')
finding=f"{summary['completed']} of {summary['goals']} mixed tasks completed. Fixed memory took {comparison(summary['taskSavingMillis'])} time across complete goal calls and {comparison(summary['totalSavingMillis'])} across whole study iterations than simple control. Charging preparation once, the first batch took {comparison(summary['firstBatchNetMillis'])} time."
test_summary=f"{tests['cases']-tests['skipped']} Java checks passed across {tests['classes']} focused classes; {tests['skipped']} optional host-dependent cases skipped. 135 desktop checks and {accounting_count} new interval/accounting tests passed. The independent inspector verified 24 evaluated goals, preparation, eight abrupt interruptions and two saved-skill executions, with {mutation['refusals']} altered-accounting/provenance refusals. The unchanged recovery regression also reused source-identical 094 originals; those are not counted as new motion."
replays=[]
for row in rec['rows']:
 x=load(r/'build/ui095-study'/row['originalFile']);j=x['job']
 if not j['arm']['success']:continue
 last=j['arm']['programs'][j['arm']['selected']]['actual'][-1]
 for packed in x['originals']:
  run=json.loads(gzip.decompress(base64.b64decode(packed['gzipBase64'])))
  if run['id']!=last:continue
  replays.append({'label':row['id']+' / '+row['condition']+' / '+row['mode'],'id':run['id'],'objects':run['definition']['objects'],'samples':[{'tick':s['tick'],'timeS':s['timeS'],'bodies':s['bodies']} for s in run['samples']]})
archive=[];local={}
def pack(p,logical):
 raw=(Path('\\\\?\\'+str(p)) if os.name=='nt' else p).read_bytes();h=sha(raw)
 if h not in local:
  dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);z=gzip.compress(raw,mtime=0);dest.write_bytes(z);local[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(local[h],logicalPath=logical))
for folder in sorted((r/'build').glob('ui095-*')):
 for p in folder.rglob('*'):
  if (Path('\\\\?\\'+str(p)) if os.name=='nt' else p).is_file() and 'desktop-profile' not in p.parts and p.name!='host.json' and p.suffix in ['.json','.jsonl','.log','.png','.html','.css','.js','.txt','.csv','.previous','.pending','.fworld']:pack(p,folder.name+'/'+p.relative_to(folder).as_posix())
for f in xml:pack(f,'regression095/'+f.name)
for p in (r/'build').glob('*095*'):
 if p.is_file() and not p.name.startswith('document095.log') and p.suffix in ['.json','.log','.ps1','.py','.cjs','.html','.md','.txt']:pack(p,'validation/'+p.name)
for p in (r/'build/ui095-study').glob('*.png'):shutil.copyfile(p,d/p.name)
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(local),'note':'Exact synthetic originals, persisted checkpoints, measured intervals, saved projects and logs. Raw/compressed hashes. Desktop profiles and host credentials excluded. No generated replacement observations.'})
old=load(r/'docs/readmes/094/file-inventory.json');paths={x['path'] for x in old};paths.update(x for x in subprocess.check_output(['git','diff','--name-only','--diff-filter=A','88b60a6dd6865c0e9d83115401f93a03d6c4ac14','HEAD'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/','scripts/','fraynix-modules/')))
kinds={x['path']:x['hashKind'] for x in old};inventory=[]
for name in sorted(paths):
 raw=(r/name).read_bytes();kind=kinds.get(name,'utf8-lf');raw=raw.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else raw;inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
js(d/'file-inventory.json',inventory);write(d/'FILE_TREE.txt','Edition095 canonical source inventory\n'+'\n'.join(x['path'] for x in inventory)+'\n')
note='docs/obsidian/103-mixed-task-costs.md';modules=[{'id':'runner','name':'Java goal, verification, commit and skill saving','source':'workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'},{'id':'world','name':'Original native procedure and recovery','source':'workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java'},{'id':'accounting','name':'Disjoint complete-cost arithmetic','source':'scripts/Costs095.cjs'},{'id':'inspection','name':'Independent native/provenance/cost inspection','source':'scripts/Inspect-Mixed095.cjs'}]
for module in modules:module['note']=note
system={'modules':modules,'screenshots':[{'path':'docs/readmes/095/'+x['file'],'caption':'Actual095 integrated app capture · 2026-11-08'} for x in rec['captures']],'evidence':[{'path':'docs/readmes/095/verification.json','caption':'Results and limits'},{'path':'docs/readmes/095/original-evidence.json','caption':'Original bytes'}]};js(d/'system-map.json',system)
verification={'passed':True,'analysis':a,'java':tests,'desktopPassed':135,'accountingTestsPassed':accounting_count,'mutationChecks':mutation,'classes':identity,'screenshots':rec['captures'],'parentFullCi':{'commit':parent_ci['commit'],'url':parent_ci['url'],'unresolvedFailures':4,'source':'validation/parent-ci095.json'},'finding':finding,'source':rec['source'],'limits':[a['scope'],'Saving comparisons combine memory retention and procedural skill creation. Counterbalanced two-observation differences do not isolate individual writes or establish confidence intervals.','Resumed totalMillis=0 remains unavailable and excluded from calibration. The whole external iteration includes experimental copying and additional audits, shown separately from the app call.']};js(d/'verification.json',verification);js(d/'comparison-data.json',a)
data={'analysis':a,'finding':finding,'tests':test_summary,'replays':replays};html=(r/'build/guide095.html').read_text(encoding='utf8').replace('__DATA__',json.dumps(data,separators=(',',':')).replace('</','<\\/'))
extra=''.join('<script id="'+name+'" type="application/json">'+json.dumps(value,separators=(',',':')).replace('</','<\\/')+'</script>' for name,value in [('system-data',system),('inventory-data',inventory)])
write(d/'index.html',html.replace('</body>',extra+'</body>'))
table='\n'.join(f"| {p['condition']} | {p['block']+1} / {p['order'][0]} | {sec(p['simple']['taskMillis'])} | {sec(p['memory']['taskMillis'])} | {sec(p['taskSavingMillis'])} | {sec(p['simple']['totalMillis'])} | {sec(p['memory']['totalMillis'])} | {sec(p['totalSavingMillis'])} |" for p in summary['pairs'])
effects_table='\n'.join(f"| {x['geometry']} | {x['block']+1} | {x['mode']} | {sec(x['savingAddedTaskMillis'])} | {sec(x['recoveryAddedTaskMillis'])} |" for x in a['effects'])
demonstration_table='\n'.join(f"| {next(x['condition'] for x in rec['rows'] if x['goalId']==d['sourceGoal'])} | {sec(d['end']-d['start'])} |" for d in rec['demonstrations'])
wins=sum(p['totalSavingMillis'] is not None and p['totalSavingMillis']>0 for p in summary['pairs']);losses=sum(p['totalSavingMillis'] is not None and p['totalSavingMillis']<0 for p in summary['pairs'])
results_summary=f"The final matrix completed {summary['completed']}/{summary['goals']} goals. Memory was faster over the whole iteration in {wins} of 12 paired comparisons and slower in {losses}. Eight abrupt Java terminations were recovered. Both additional saved-skill executions completed after reopening on fresh worlds. These successes required the original native and report checks."
tick_totals={mode:sum(x['job']['arm']['chargedTicks'] for x in rec['rows'] if x['mode']==mode) for mode in ['simple','memory']}
results_summary+=f" Across compared tasks, simple control charged {tick_totals['simple']:,} native ticks and memory charged {tick_totals['memory']:,}: {tick_totals['simple']-tick_totals['memory']:,} fewer with memory. Earning the preparation separately charged {rec['preparation']['job']['arm']['chargedTicks']:,} ticks. Lower native work did not produce lower complete elapsed cost in this cold-start cohort."
effects_summary='The following differences keep geometry, control and block visible. Negative values can occur from ordinary timing variation; they do not imply a free or negative-cost save operation.'
next_steps='Resolve the inherited full-CI schema/source-lock and long-test resource failures with explicit contracts and preserved originals. Then repeat complete mixed sequences in a continuing warm workspace with changing bank contents, keeping cold-start costs visible separately. Compare a cost-aware selector against these fixed controls only after matched calibration coverage exists. Cross-boot timing needs its own durable clock contract before recovered tasks can train cost estimates. Keep useful browser/creation/game goals in view; a faster failed task or an unverified saved skill is not progress.'
failures=(r/'build/failures095.md').read_text(encoding='utf8') if (r/'build/failures095.md').exists() else 'No preliminary native cohort was discarded. The complete original records remain available for independent inspection.'
readme=(r/'build/README095-template.md').read_text(encoding='utf8')
for k,v in {'finding':finding,'results_summary':results_summary,'table':table,'preparation':sec(summary['preparationMillis']),'effects_summary':effects_summary,'effects_table':effects_table,'demonstration_table':demonstration_table,'test_summary':test_summary,'failures':failures,'next_steps':next_steps}.items():readme=readme.replace('{'+k+'}',v)
write(r/'README-095.md',readme)
write(r/note,f'# Edition095 — complete mixed-task costs\n\n[[README-095.md|Full edition]] · [[docs/readmes/095/index.html|Offline evidence]] · [[docs/obsidian/102-three-stage-recovery.md|Prior edition]] · [[README-CONTRIBUTORS.md|Contributor direction]]\n\n{finding}\n\n{test_summary}\n\n{next_steps}\n')
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*390,'y':0,'width':350,'height':260} for i,m in enumerate(modules)];nodes.append({'id':'evidence','type':'file','file':'README-095.md','x':390,'y':330,'width':740,'height':340});edges=[{'id':'e'+str(i),'fromNode':modules[i]['id'],'fromSide':'right','toNode':modules[i+1]['id'],'toSide':'left'} for i in range(3)];js(r/'docs/obsidian/103-mixed-task-costs.canvas',{'nodes':nodes,'edges':edges})
continuity=f'## Edition095 — locally verified; publication pending\n\n{finding}\n\n{test_summary} All 334 production engine classes match 094. Source {rec["source"]}. Fixed one-memory cold copies, six conditions, two opposite orders. No new fit or runtime policy. Parent full GitHub CI still has four failures (construction schema, historical WorldLedger lock and two heap exhaustions); original failed XML retained separately. Focused095 checks do not establish full-suite health. Saved skill executions are separate from the 24 comparison goals. Resumed Java clocks stay unavailable/ineligible; external interval accounting never grants motion authority.\n\nIsolated D:/Codex-Work/fraynix-workspace-edition095; public eyeoverthink/Fraynix-Agent-Workspace. Preserve live045/qa046/private projects and frozen001–094. No subagents. {next_steps}\n\n'
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:write(r/name,continuity+(r/name).read_text(encoding='utf8').replace('Edition094 is published and fetched back from its isolated D: checkout; the release, contract and original evidence record actual results.','Use the newest edition entry above for current status; each release, contract and original evidence records its actual results.'))
root=(r/'README.md').read_text(encoding='utf8').replace('**Latest: [edition094','**Earlier edition: [edition094',1);root=root.replace('# Fraynix Agent Workspace\n','# Fraynix Agent Workspace\n\n**Latest: [edition095 — complete mixed-task costs](README-095.md).**\n\n'+finding+' [Offline evidence](docs/readmes/095/index.html).\n',1);write(r/'README.md',root)
for name,prefix in [('docs/ARCHITECTURE.md','Edition095 measures complete mixed tasks, recovery and saved skills without changing production Java classes. [Results](../README-095.md) · [Contributor contract](../README-CONTRIBUTORS.md).\n\n'),('docs/obsidian/00-home.md','[[README-095.md|Edition095 — complete mixed-task costs]] · [[docs/obsidian/103-mixed-task-costs.md]]\n\n'),('docs/readmes/INDEX.md','[Edition095 — complete mixed-task costs](../../README-095.md) · [Offline evidence](095/index.html)\n\n')]:write(r/name,prefix+(r/name).read_text(encoding='utf8'))
p=r/'README-CONTRIBUTORS.md';s=p.read_text(encoding='utf8');s=s.replace('For edition094, the isolated source checkout is `D:/Codex-Work/fraynix-workspace-edition094`, branch `edition094`, based on verified093 main `24052edef70eba152858dd15f93425c106db39df`.','For edition095, the isolated source checkout is `D:/Codex-Work/fraynix-workspace-edition095`, branch `edition095`, based on verified094 main `88b60a6dd6865c0e9d83115401f93a03d6c4ac14`.');s=s.replace('(docs/readmes/094/file-inventory.json)','(docs/readmes/095/file-inventory.json)');s=s.replace('The immediate priority is reliable continuation with evidence, then complete mixed-task cost comparisons that show when memory earns its overhead.','Edition095 measures complete mixed-task costs, recovery and skill saving. The next priorities are resolving the four inherited full-CI failures with evidence, then continuing warm-workspace sequences with changing memory contents and matched cost coverage.');s=s.replace('| Nov 8 | 094|','| Nov 8 | 095 | Complete mixed-task cost comparison, recovery, skill saving and fresh execution of saved procedures. See README095 for observed results and limitations. |\n| Nov 8 | 094|');write(p,s)
print(json.dumps({'finding':finding,'tests':tests,'logicalOriginals':len(archive),'uniquePayloads':len(local),'sources':len(inventory),'replays':len(replays)},indent=2))
