from pathlib import Path
import json,hashlib,shutil
r=Path.cwd();old=Path('J:/Codex-Work/fraynix-workspace-edition149');sha=lambda b:hashlib.sha256(b).hexdigest()
records=[]
for name in ['donor','return','wrong-pattern']:
 p=r/'workspace-engine/build/starting150-generated'/(name+'.json');labels=r/'workspace-engine/build/starting150-generated'/(name+'-labels.json')
 public=r/'docs/publication150'/(name+'-generated-labels.json');shutil.copy2(labels,public)
 data=json.loads(p.read_text());rows=json.loads(labels.read_text())['rows'];n=len(data['frames'])
 records.append({'case':'generated150-'+name,'group':'generated150','name':'Declared generated '+name,'role':'reference-source' if name=='donor' else 'evaluation','domain':'exposed-generated-control','privateImages':False,'primaryEligible':False,'start':1,'end':n,'stride':1,'frames':n,'sourceFrames':list(range(1,n+1)),'width':320,'height':240,'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':public.relative_to(r).as_posix(),'labelsSha256':sha(public.read_bytes()),'visibilityCounts':{v:sum(x['visibility']==v for x in rows) for v in ['visible','partial','occluded','out-of-view','uncertain']}})
prior=json.loads((r/'docs/publication149/source149.json').read_text())
for row in prior['records']:
 if row['group']!='black-lighter-long-cover-exploratory':continue
 row=dict(row);source=old/row['pack'];assert sha(source.read_bytes())==row['packSha256']
 target=r/'build/exposed149-private'/source.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
 row['pack']=target.relative_to(r).as_posix();row['domain']='exposed-real-camera';row['exposure150']='Previously evaluated in149; source and primary refusal unchanged.';records.append(row)
manifest={'edition':150,'baseline':'e5479b7193df57eb0fdfbc930bd87fe9a0e2cc70','algorithmLockSha256':None,'records':records,'scope':'Declared generated controls and exposed149 regression. Fresh150 recording added only after source-only review and label freeze.'}
(r/'docs/publication150/source150.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8',newline='\n')
print(json.dumps({'records':len(records),'privatePixelsCopiedLocally':2,'freshCameraRecords':0}))
