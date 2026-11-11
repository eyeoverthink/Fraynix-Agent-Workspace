from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
r=Path.cwd()
baseline=json.loads((r/'workspace-engine/build/controller119-baseline/result.json').read_text(encoding='utf8'))
final=json.loads((r/'workspace-engine/build/controller119-final-development/result.json').read_text(encoding='utf8'))
def trial(d,name,policy):return next(t for c in d['cases'] if c['name']==name for t in c['trials'] if t['policy']==policy)
def states(t):return [t['initial']]+[s['after'] for s in t['steps']]
fig,ax=plt.subplots(2,2,figsize=(13,8),layout='constrained')
for data,policy,color in [(baseline,'risk118','#b3434b'),(final,'recover119','#087966')]:
 ss=states(trial(data,'retained-step',policy));ax[0,0].plot([s['tick']/60 for s in ss],[s['player']['x'] for s in ss],label=policy,color=color)
 ss=states(trial(data,'retained-two-enemies',policy));ax[1,0].plot([s['tick']/60 for s in ss],[s['player']['x'] for s in ss],label=policy,color=color)
ss=states(trial(baseline,'retained-step','risk118'))
ax[0,1].plot([s['tick']/60 for s in ss],[s['player']['vy'] for s in ss],label='Reported vertical velocity',color='#b3434b')
ax[0,1].plot([s['tick']/60 for s in ss[1:]],[(b['player']['y']-a['player']['y'])*60/(b['tick']-a['tick']) for a,b in zip(ss,ss[1:])],label='Measured displacement / time',color='#087966')
ss=states(trial(final,'retained-two-enemies','recover119'))
ax[1,1].step([s['tick']/60 for s in ss],[s['controller'].get('pickupDeferrals',0) for s in ss],label='Temporary deferrals',color='#b48427',where='post')
ax[1,1].step([s['tick']/60 for s in ss],[s['controller'].get('pickups',0) for s in ss],label='Actual power-ups collected',color='#087966',where='post')
ax[0,0].set(title='Retained gap-to-step stall',xlabel='Simulation time (s)',ylabel='Player X (original units)')
ax[0,1].set(title='Original jam: velocity is not displacement',xlabel='Simulation time (s)',ylabel='Vertical units / second',xlim=(3,7))
ax[1,0].set(title='Retained pickup-skipping regression',xlabel='Simulation time (s)',ylabel='Player X (original units)')
ax[1,1].set(title='Observe again, then verify collection',xlabel='Simulation time (s)',ylabel='Count',yticks=[0,1,2,3])
for a in ax.flat:a.grid(alpha=.2);a.legend(fontsize=8);a.spines[['top','right']].set_visible(False)
fig.suptitle('Edition119 — actual native observations; ordinary direction and jump inputs',fontsize=15)
fig.savefig(r/'build/controller119-motion.png',dpi=150)
