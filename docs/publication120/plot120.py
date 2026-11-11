from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
r=Path.cwd()
baseline=json.loads((r/'workspace-engine/build/controller120-baseline/result.json').read_text(encoding='utf8'))
final=json.loads((r/'workspace-engine/build/controller120-development-final/result.json').read_text(encoding='utf8'))
def trial(d,name,policy):return next(t for c in d['cases'] if c['name']==name for t in c['trials'] if t['policy']==policy)
def states(t):return [t['initial']]+[s['after'] for s in t['steps']]
fig,ax=plt.subplots(2,2,figsize=(13,8),layout='constrained')
for data,policy,color in [(baseline,'recover119','#b3434b'),(final,'opportunity120','#087966')]:
 ss=states(trial(data,'retained-two-enemies',policy));ax[0,0].plot([s['tick']/60 for s in ss],[s['player']['x'] for s in ss],label=policy,color=color)
 ss=states(trial(data,'retained-two-enemies',policy));ax[1,0].plot([s['tick']/60 for s in ss],[s['player']['x'] for s in ss],label=policy,color=color)
ss=states(trial(final,'retained-two-enemies','opportunity120'))
ax[0,1].step([s['tick']/60 for s in ss],[s['controller'].get('opportunity120',{}).get('waitTicks',0)/60 for s in ss],label='Actual waiting seconds',color='#b48427',where='post')
ax[0,1].step([s['tick']/60 for s in ss],[s['controller'].get('pickups',0) for s in ss],label='Actual pickups',color='#087966',where='post')
ax[1,0].clear()
for data,policy,color in [(baseline,'recover119','#b3434b'),(final,'opportunity120','#087966')]:
 ss=states(trial(data,'retained-turtle',policy));ax[1,0].plot([s['tick']/60 for s in ss],[s['player']['x'] for s in ss],label=policy,color=color)
planned=[];actual=[];labels=[]
for case in final['cases']:
 t=next(t for t in case['trials'] if t['policy']=='opportunity120');o=t['final']['controller']['opportunity120']
 if o['waitClears']:
  planned.append(next(d['plannedTicks'] for d in o['decisions'] if d['action']=='wait-started'));actual.append(o['waitTicks']);labels.append(case['name'])
x=list(range(len(labels)));ax[1,1].bar([i-.18 for i in x],planned,.36,label='Estimated ticks',color='#b48427');ax[1,1].bar([i+.18 for i in x],actual,.36,label='Actual wait ticks',color='#087966');ax[1,1].set_xticks(x,labels,rotation=12,fontsize=8)
ax[0,0].set(title='Supported two-enemy course',xlabel='Simulation time (s)',ylabel='Player X (original units)')
ax[0,1].set(title='Wait, observe clearance, then collect',xlabel='Simulation time (s)',ylabel='Seconds / count')
ax[1,0].set(title='Supported turtle course',xlabel='Simulation time (s)',ylabel='Player X (original units)')
ax[1,1].set(title='Estimate does not end the wait',ylabel='Simulation ticks')
for a in ax.flat:a.grid(alpha=.2);a.legend(fontsize=8);a.spines[['top','right']].set_visible(False)
fig.suptitle('Edition120 — actual clearance and collection; ordinary native inputs',fontsize=15)
fig.savefig(r/'build/controller120-motion.png',dpi=150)
