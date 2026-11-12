from pathlib import Path
import json,numpy as np,matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
from matplotlib.patches import Patch
r=Path.cwd();rows=[]
for partition in ['development','reserved','software']:
 d=json.loads((r/f'workspace-engine/build/budget124-{partition}/result.json').read_text(encoding='utf8'));assert d['passed']
 for x in d['rows']:
  if not x['requested'].startswith('budget'):continue
  choice=x['decision']['result'];full=next(o for o in choice['alternatives'] if o['strategy']=='full')
  rows.append((partition,x['case'],choice['allowance'],choice['selected'],full.get('requiredSecondsPerPickup'),x['measurement']['choice124']['seconds'],x['wallNanos']/1e9,x['measurement']['audit']['aiPickups']))
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10,'figure.facecolor':'#101822','axes.facecolor':'#101822','text.color':'#e9edf4','axes.labelcolor':'#e9edf4','xtick.color':'#c2cfdd','ytick.color':'#c2cfdd','axes.edgecolor':'#35465a'})
fig,ax=plt.subplots(1,2,figsize=(16,10),gridspec_kw={'width_ratios':[1.25,1]});y=np.arange(len(rows));labels=[f'{p} / {c}\nallowance {a:g} → {s}; pickups {n}' for p,c,a,s,t,clock,wall,n in rows]
ax[0].barh(y,[x[2] for x in rows],color='#344e69',label='User allowance',height=.65)
for i,x in enumerate(rows):
 if x[4] is not None:ax[0].plot(x[4],i,'o',color='#71e1b9' if x[3]!='direct' else '#ffbf70',markersize=7)
 else:ax[0].text(1,i,'No qualifying estimate',va='center',fontsize=8,color='#ffbf70')
ax[0].set_yticks(y,labels);ax[0].invert_yaxis();ax[0].set_xlabel('Extra seconds per additional pickup');ax[0].set_title('Recorded allowance and conservative estimate');ax[0].legend(handles=[Patch(color='#344e69',label='User allowance'),Line2D([],[],marker='o',linestyle='',color='#71e1b9',label='Full selected'),Line2D([],[],marker='o',linestyle='',color='#ffbf70',label='Full declined')],loc='upper center',bbox_to_anchor=(.5,-.09),ncol=3,fontsize=8)
ax[1].barh(y,[x[6] for x in rows],height=.65,color='#344e69',label='External request + re-audit')
ax[1].barh(y,[x[5] for x in rows],height=.35,color='#71e1b9',label='First verified outcome clock');ax[1].set_yticks(y,[]);ax[1].invert_yaxis();ax[1].set_xlabel('Actual elapsed seconds');ax[1].set_title('Task cost, including all original checks');ax[1].legend(loc='upper center',bbox_to_anchor=(.5,-.09),ncol=1,fontsize=8)
for a in ax:a.grid(axis='x',alpha=.15);a.set_axisbelow(True)
fig.suptitle('Edition124 · choosing optional work from evidence',fontsize=20,x=.56)
fig.text(.32,.016,'Empirical envelopes, not confidence intervals or deadline guarantees. Unknown and zero-gain cases remain direct.',fontsize=10)
fig.tight_layout(rect=[0,.045,1,.95]);fig.savefig(r/'build/budget124-costs.png',dpi=150);plt.close(fig)
