from pathlib import Path
import copy,json,hashlib,sys
sys.path.insert(0,str(Path(__file__).parent))
from preflight147 import validate
idx={'frames':[{'sourceFrame':i,'imageSha256':hashlib.sha256(str(i).encode()).hexdigest(),'originalBox':[40,60,80,80],'width':320,'height':240} for i in range(1,101)]}
s={'eligible':True,'donor':[1,24],'evaluation':[30,62],'event':{'hiddenStart':40,'returnFrame':42},'ranges':[{'start':1,'end':24,'visibility':'visible'},{'start':30,'end':39,'visibility':'visible'},{'start':40,'end':41,'visibility':'occluded'},{'start':42,'end':62,'visibility':'visible'}],'selectedReviewedSourceFrames':list(range(1,25))+list(range(30,63)),'otherBoxes':{str(i):[[.65,.25,.9,.6]] for i in range(38,48)},'lookalike':{'distinctPhysicalObject':True,'similarityRationale':'Analytic separate rectangle with similar appearance; unit check only'}}
checks=[];assert validate(s,idx)['fullyHiddenFrames']==2;checks.append('Eligible analytic source passes structural requirements; not camera evidence')
def rejected(name,edit):
 a,b=copy.deepcopy(s),copy.deepcopy(idx);edit(a,b)
 try:validate(a,b)
 except (AssertionError,KeyError,ValueError):checks.append(name);return
 raise AssertionError('Accepted invalid source: '+name)
rejected('Exact donor/evaluation byte overlap refused',lambda a,b:b['frames'][40].update(imageSha256=b['frames'][0]['imageSha256']))
rejected('All-partial donor refused',lambda a,b:a['ranges'][0].update(visibility='partial'))
rejected('Future donor refused',lambda a,b:a.update(donor=[70,93]))
rejected('Out-of-view is not complete cover',lambda a,b:a['ranges'][2].update(visibility='out-of-view'))
rejected('Missing lookalike refused',lambda a,b:a.update(otherBoxes={}))
rejected('Lookalike absent during cover refused',lambda a,b:a.update(otherBoxes={str(i):[[.65,.25,.9,.6]] for i in range(42,48)}))
rejected('Copied target annotation is not a distinct competitor',lambda a,b:a.update(otherBoxes={str(i):[[.125,.25,.375,140/240]] for i in range(38,48)}))
rejected('Incomplete full-resolution review refused',lambda a,b:a['selectedReviewedSourceFrames'].pop())
rejected('Incomplete visibility labels refused',lambda a,b:a['ranges'][-1].update(end=61))
rejected('Invalid competitor geometry refused',lambda a,b:a['otherBoxes']['42'][0].__setitem__(0,float('nan')))
rejected('Changed event window refused',lambda a,b:a['evaluation'].__setitem__(1,61))
r={'passed':True,'checks':checks,'scope':'Analytic preflight validation only; no fresh pixels or tracker predictions.'};Path('build/preflight147-tests.json').write_bytes((json.dumps(r,indent=2)+'\n').encode());print(json.dumps(r))
