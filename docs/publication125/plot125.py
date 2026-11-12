from pathlib import Path
import json,numpy as np,matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
r=Path.cwd();rows=[]
for partition in ['development','reserved','software']:
 d=json.loads((r/f'workspace-engine/build/cost125-{partition}/result.json').read_text(encoding='utf8'));assert d['passed']
 for entry in d['reviews']:
  if entry['role']=='final':rows.append((partition,entry['case'],entry['report']))
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10,'figure.facecolor':'#101822','axes.facecolor':'#101822','text.color':'#e9edf4','axes.labelcolor':'#e9edf4','xtick.color':'#c2cfdd','ytick.color':'#c2cfdd','axes.edgecolor':'#35465a'})
fig,ax=plt.subplots(1,2,figsize=(16,8));y=np.arange(len(rows));labels=[f'{p} / {case}\n{report["metrics"]["missedOpportunity"]}' for p,case,report in rows]
for i,(_,_,report) in enumerate(rows):
 m=report['metrics'];pred=m.get('predictedSecondsPerPickup');observed=m.get('observedSecondsPerPickupUpper')
 if pred is not None:ax[0].plot(pred,i-.12,'o',color='#ffbf70',label='Original predicted threshold' if i==0 else None)
 if observed is not None:ax[0].plot(observed,i+.12,'s',color='#71e1b9',label='Independent observed upper' if i==0 else None)
 if pred is None and observed is None:ax[0].text(.5,i,'No added benefit' if m.get('benefitLower',1)<=0 else 'Inconclusive',va='center',fontsize=9,color='#ffbf70')
 for strategy,offset,color in [('direct',-.14,'#79b7ff'),('full',.14,'#71e1b9')]:
  samples=[s['seconds'] for s in report['samples'] if s['strategy']==strategy]
  if samples:ax[1].plot([min(samples),max(samples)],[i+offset,i+offset],color=color,lw=7,label=strategy+' range' if i==0 else None)
 terminal=report['sources'][0]['timing'].get('terminal',{})
 if terminal:ax[1].plot(terminal['seconds'],i,'D',color='#ffbf70',label='Original budget request' if i==0 else None)
ax[0].set_yticks(y,labels);ax[1].set_yticks(y,[])
for a in ax:a.invert_yaxis();a.grid(axis='x',alpha=.15);a.set_axisbelow(True);a.legend(loc='upper center',bbox_to_anchor=(.5,-.14),fontsize=8)
ax[0].set_xlabel('Extra seconds per additional pickup');ax[0].set_title('Prediction and independent cost comparison')
ax[1].set_xlabel('First verified outcome · elapsed seconds');ax[1].set_title('Three later explicit runs per strategy')
fig.suptitle('Edition125 · evaluate the decision against actual outcomes',fontsize=18,x=.55)
fig.text(.18,.02,'Recorded empirical ranges, not confidence intervals. Failed or recovered observations do not establish a useful optional action.',fontsize=10)
fig.tight_layout(rect=[0,.045,1,.94]);fig.savefig(r/'build/cost-review125.png',dpi=150);plt.close(fig)
