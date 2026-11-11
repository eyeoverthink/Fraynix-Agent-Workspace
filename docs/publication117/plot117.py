"""Publication-only plot from retained native observations; not an app dependency."""
from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
r=Path.cwd();data=json.loads((r/'workspace-engine/build/controller117-final-wider/result.json').read_text(encoding='utf8'));assert data['passed']
case=next(c for c in data['cases'] if c['parameters']['seed']==15)
fig,axes=plt.subplots(1,2,figsize=(13,4.5),layout='constrained');colors={'simple':'#5c6b81','landing116':'#b85421','safe117':'#167566'}
for t in case['trials']:
 policy=t['policy']
 if policy not in colors:continue
 states=[t['initial']]+[s['after'] for s in t['steps']];color=colors[policy]
 axes[0].plot([s['tick']/60 for s in states],[s['player']['x'] for s in states],label=policy,color=color,lw=2)
 if policy=='simple':continue
 focus=[s for s in states if 120<=s['tick']<=240]
 axes[1].plot([s['tick']/60 for s in focus],[s['player']['y'] for s in focus],label=policy+' player',color=color,lw=2)
 item=[s for s in focus if s['controller']['goal']=='COLLECT' and 'goalY' in s['controller']]
 axes[1].plot([s['tick']/60 for s in item],[s['controller']['goalY'] for s in item],label=policy+' item',color=color,ls='--',lw=1.8)
axes[0].set(title='Actual progress: wider seed15 course',xlabel='Simulation time (seconds)',ylabel='Player x (world units)',xlim=(0,8));axes[0].axhline(5.142,color=colors['landing116'],ls=':',alpha=.6);axes[0].text(7.9,5.4,'Old policy remains near x5.142\nthrough the 36-second trial',ha='right',fontsize=9)
axes[1].set(title='Collection approach: player and item height',xlabel='Simulation time (seconds)',ylabel='Observed y (world units)',xlim=(2,4))
for ax in axes:ax.grid(alpha=.18);ax.legend(fontsize=8,loc='upper left');ax.spines[['top','right']].set_visible(False)
fig.suptitle('Edition117 | Ordinary inputs; unchanged native physics',fontsize=15)
fig.savefig(r/'build/controller117-motion.png',dpi=150)
print('Saved measured native-position plot; samples every 12 ticks. Not wall-clock timing.')
