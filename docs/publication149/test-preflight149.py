from pathlib import Path
import copy,json,hashlib,sys
sys.path.insert(0,str(Path(__file__).parent))
from preflight149 import validate
idx={'frames':[{'sourceFrame':i,'imageSha256':hashlib.sha256(str(i).encode()).hexdigest(),'originalBox':[40+(40 if i>=58 else 0),60,80,80],'width':320,'height':240} for i in range(1,101)]}
s={'eligible':True,'donor':[1,24],'evaluation':[30,78],'event':{'hiddenStart':40,'returnFrame':58},'ranges':[{'start':1,'end':24,'visibility':'visible'},{'start':30,'end':39,'visibility':'visible'},{'start':40,'end':57,'visibility':'occluded'},{'start':58,'end':78,'visibility':'visible'}],'selectedReviewedSourceFrames':list(range(1,25))+list(range(30,79)),'otherBoxes':{str(i):[[.65,.25,.9,.6]] for i in range(38,64)},'lookalike':{'distinctPhysicalObject':True,'similarityRationale':'Analytic separate rectangle with similar appearance; unit check only'}}
checks=[];assert validate(s,idx)['fullyHiddenFrames']==18;checks.append('Eligible analytic source passes structural requirements; not camera evidence')
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
rejected('Lookalike absent during cover refused',lambda a,b:a.update(otherBoxes={str(i):[[.65,.25,.9,.6]] for i in range(58,64)}))
rejected('Copied target annotation is not a distinct competitor',lambda a,b:a.update(otherBoxes={str(i):[[(40+(40 if i>=58 else 0))/320,.25,(120+(40 if i>=58 else 0))/320,140/240]] for i in range(38,64)}))
rejected('Incomplete full-resolution review refused',lambda a,b:a['selectedReviewedSourceFrames'].pop())
rejected('Incomplete visibility labels refused',lambda a,b:a['ranges'][-1].update(end=77))
rejected('Invalid competitor geometry refused',lambda a,b:a['otherBoxes']['58'][0].__setitem__(0,float('nan')))
rejected('Changed event window refused',lambda a,b:a['evaluation'].__setitem__(1,77))
rejected('Stationary source does not qualify as fresh movement',lambda a,b:[f.update(originalBox=[40,60,80,80]) for f in b['frames']])
rejected('Partially obscured evaluation seed refused',lambda a,b:a['ranges'][1].update(visibility='partial'))
rejected('Short cover cannot qualify as a long occlusion',lambda a,b:a['event'].update(returnFrame=57))
rejected('Movement only after first visible return does not qualify as displaced re-entry',lambda a,b:b['frames'][57].update(originalBox=[40,60,80,80]))
rejected('All-partial return cannot establish full-object evidence',lambda a,b:a['ranges'][-1].update(visibility='partial'))
r={'passed':True,'preflightSourceSha256':hashlib.sha256((Path(__file__).parent/'preflight149.py').read_bytes().replace(b'\r\n',b'\n')).hexdigest(),'checks':checks,'scope':'Analytic preflight validation only; no fresh pixels or tracker predictions.'};Path('build/preflight149-tests.json').write_bytes((json.dumps(r,indent=2)+'\n').encode());print(json.dumps(r))
