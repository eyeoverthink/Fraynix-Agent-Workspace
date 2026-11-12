from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
r=Path.cwd();d=json.loads((r/'workspace-engine/build/controller122-development/result.json').read_text(encoding='utf-8'))

def trial(name,policy):return next(t for c in d['cases'] if c['name']==name for t in c['trials'] if t['policy']==policy)
def states(t):return [t['initial']]+[s['after'] for s in t['steps']]
fixes=[c['name'] for c in d['cases'] if c['name'].startswith('retained-')];assert len(fixes)==3
fig,axes=plt.subplots(2,2,figsize=(13,8),layout='constrained')
colors={'revisit121':'#b3434b','contact122':'#087966'}
for a,name in zip(axes.flat,fixes):
 for policy,color in colors.items():
  t=trial(name,policy);ss=states(t)
  a.plot([s['tick']/60 for s in ss],[s['player']['x'] for s in ss],color=color,label=policy+(' finished' if t['won'] else ' failed'))
 a.set(title=name.replace('retained-','').replace('-',' '),xlabel='Simulation time (s)',ylabel='Player X (original units)');a.legend(fontsize=8)
a=axes[1,1]
for j,(policy,color) in enumerate(colors.items()):a.bar([i+j*.35 for i in range(3)],[trial(n,policy)['elapsedNanos']/1e9 for n in fixes],width=.35,color=color,label=policy)
a.set_xticks([i+.175 for i in range(3)],['Near guard','Late turtle','Supported edge']);a.set(title='Complete native trial cost (single runs)',ylabel='Elapsed wall seconds');a.legend(fontsize=8)
for a in axes.flat:a.grid(alpha=.2);a.spines[['top','right']].set_visible(False)
fig.suptitle('Edition 122: actual movement and completion, unchanged game physics',fontsize=15)
fig.savefig(r/'build/controller122-motion.png',dpi=150)
