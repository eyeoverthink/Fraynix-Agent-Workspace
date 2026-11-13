from pathlib import Path
import json,subprocess,hashlib
r=Path.cwd();folder=r/'build/diagnostic129'
java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe'
classpath='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;S:/Codex-Work/fraynix-workspace-edition128/workspace-engine/build/install/workspace-engine/lib/*'
files=sorted(folder.glob('held-*.json'))+sorted(folder.glob('confirm-*.json'))+[folder/'flow-development.json',folder/'scale-aware-second-terrace-calibration.json']
results=[];summary={}
for p in files:
 output=folder/('forecast-'+p.name)
 subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',classpath,'fraymus.workspace.ForecastStudy129',str(p),str(output)],check=True,cwd=r)
 data=json.loads(output.read_text(encoding='utf8'));assert data['inputSha256']==hashlib.sha256(p.read_bytes()).hexdigest();results.append({'path':output.relative_to(r).as_posix(),'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'input':p.name,'records':[{k:v for k,v in x.items() if k!='forecast'}|({'metrics':x['forecast']['metrics'],'future':x['forecast']['future']} if 'forecast' in x else {}) for x in data['records']]})
 if p.name.startswith(('held-','confirm-')) and p.name.endswith('-flow.json'):
  for x in data['records']:
   s=summary.setdefault(x['policy'],{'available':0,'totalRows':0,'radiusSum':0,'referenceAvailable':0,'referenceCovered':0,'referenceErrorSum':0,'refused':0})
   if 'refused' in x:s['refused']+=1;continue
   s['totalRows']+=len(x['forecast']['rows'])
   for row in x['forecast']['rows']:
    f=row['forecast']
    if f['available']:s['available']+=1;s['radiusSum']+=f['radiusM']
   for score in x['independentScores']:
    if score['available']:s['referenceAvailable']+=1;s['referenceCovered']+=int(score['coveredIncludingGrid']);s['referenceErrorSum']+=score['errorM']
for s in summary.values():s['meanRadiusM']=s['radiusSum']/s['available'] if s['available'] else None;s['meanReferenceErrorM']=s['referenceErrorSum']/s['referenceAvailable'] if s['referenceAvailable'] else None
result={'summary':summary,'results':results,'scope':'Summary uses the same ten held/confirmation incumbent traces. Other methods and development traces retained separately, including refusals. Source rows are already computed; only forecast evaluation runs. Single timings include JVM warmup and are not end-to-end speed claims. Independent coverage includes 250mm reference-grid uncertainty.'}
(folder/'forecast-comparison.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(summary),flush=True)
