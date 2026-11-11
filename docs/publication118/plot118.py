from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
r=Path.cwd();d=json.loads((r/'workspace-engine/build/controller118-development/result.json').read_text(encoding='utf8'));a=json.loads((r/'workspace-engine/build/controller118-prototype-a/result.json').read_text(encoding='utf8'))
def trial(data,name,policy):return next(t for c in data['cases'] if c['name']==name for t in c['trials'] if t['policy']==policy)
def states(t):return [t['initial']]+[s['after'] for s in t['steps']]
fig,axes=plt.subplots(1,3,figsize=(15,4.7),layout='constrained');colors={'simple':'#8b96a4','safe117':'#c17c25','risk118':'#007e71'}
for policy in ['simple','safe117','risk118']:
 ss=states(trial(d,'low-clearance',policy));axes[0].plot([s['tick']/60 for s in ss],[s['player']['x'] for s in ss],label=policy,color=colors[policy])
axes[0].set(title='Cramped pickup: continue past the obstacle',xlabel='Simulation time (s)',ylabel='Player X (original world units)');axes[0].legend()
for data,label,color in [(a,'First prototype: early jump','#b9444b'),(d,'Final: observed closing motion','#007e71')]:
 ss=states(trial(data,'near-goomba','risk118'));axes[1].plot([s['tick']/60 for s in ss],[s['player']['y'] for s in ss],label=label,color=color)
axes[1].set(title='Near enemy: first 3 seconds',xlabel='Simulation time (s)',ylabel='Player Y (original world units)',xlim=(0,3));axes[1].legend(fontsize=8)
policies=['simple','safe117','risk118'];wins=[sum(t['won'] for c in d['cases'] for t in c['trials'] if t['policy']==p) for p in policies];bars=axes[2].bar(policies,wins,color=[colors[p] for p in policies]);axes[2].bar_label(bars,labels=[str(w)+'/12' for w in wins],padding=3);axes[2].set(title='Actual flag-contact finishes',ylabel='Completed development courses',ylim=(0,13.5))
for ax in axes:ax.grid(alpha=.2,axis='y');ax.spines[['top','right']].set_visible(False)
fig.suptitle('Edition118 — original native observations, ordinary input only',fontsize=15)
fig.savefig(r/'build/controller118-motion.png',dpi=150)
