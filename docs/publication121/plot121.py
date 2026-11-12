from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
r=Path.cwd();d=json.loads((r/'workspace-engine/build/controller121-development/result.json').read_text())
def trial(name,policy):return next(t for c in d['cases'] if c['name']==name for t in c['trials'] if t['policy']==policy)
def states(t):return [t['initial']]+[s['after'] for s in t['steps']]
fig,ax=plt.subplots(2,2,figsize=(13,8),layout='constrained')
for policy,color in [('opportunity120','#b3434b'),('observe121','#b48427'),('revisit121','#087966')]:
 ss=states(trial('late-room',policy));times=[s['tick']/60 for s in ss];ax[0,0].plot(times,[s['player']['x'] for s in ss],label=policy,color=color);ax[0,1].step(times,[s['controller'].get('pickups',0) for s in ss],label=policy,color=color,where='post')
ss=states(trial('goomba-reversal','revisit121'));enemy=next(e['id'] for s in ss for e in s['threats118']['enemies']);points=[(s['tick']/60,e['vx']) for s in ss for e in s['threats118']['enemies'] if e['id']==enemy];ax[1,0].plot([p[0] for p in points],[p[1] for p in points],color='#265bb3',label='Observed native enemy vx')
ps=['opportunity120','observe121','revisit121'];ax[1,1].bar(ps,[trial('late-room',p)['final']['tick']/60 for p in ps],color=['#b3434b','#b48427','#087966'],label='Complete simulation seconds')
ax[0,0].set(title='Same scene: return changes the route',xlabel='Simulation time (s)',ylabel='Player X (original units)');ax[0,1].set(title='Only actual collision counts as collection',xlabel='Simulation time (s)',ylabel='Collected items');ax[1,0].set(title='Enemy reverses at original native wall',xlabel='Simulation time (s)',ylabel='Velocity X (units/s)');ax[1,1].set(title='The useful pickup costs extra time',ylabel='Complete simulation seconds')
for a in ax.flat:a.grid(alpha=.2);a.legend(fontsize=8);a.spines[['top','right']].set_visible(False)
fig.suptitle('Edition121 — actual return benefit, observed reversal and full task cost',fontsize=15);fig.savefig(r/'build/controller121-motion.png',dpi=150)
