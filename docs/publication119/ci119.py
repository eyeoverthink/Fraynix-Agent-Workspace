"""Read push-triggered CI runs; the connector's commit lookup only returns PR runs."""
from pathlib import Path
import subprocess,urllib.request,json,sys,datetime
repo='eyeoverthink/Fraynix-Agent-Workspace'
p=subprocess.run(['git','credential','fill'],input='protocol=https\nhost=github.com\npath='+repo+'.git\n\n',text=True,capture_output=True,timeout=25,check=True)
credential=dict(line.split('=',1) for line in p.stdout.splitlines() if '=' in line)
request=urllib.request.Request('https://api.github.com/repos/'+repo+'/actions/runs?per_page=4',headers={'Authorization':'Bearer '+credential['password'],'Accept':'application/vnd.github+json','User-Agent':'Fraynix-local-verification'})
with urllib.request.urlopen(request,timeout=30) as response:data=json.load(response)
result={'checkedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'runs':[{k:run.get(k) for k in ['id','head_sha','event','status','conclusion','html_url','created_at','updated_at']} for run in data['workflow_runs']]}
Path(sys.argv[1]).write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
