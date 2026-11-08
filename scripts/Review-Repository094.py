"""Read/index every tracked file; separately expose text for architecture review.
This inventory is not a claim of semantic line review of every byte or binary.
"""
from pathlib import Path
import subprocess,json,hashlib,re,collections,datetime
r=Path(__file__).resolve().parent.parent;out=r/'build';out.mkdir(exist_ok=True)
def git(*args):return subprocess.check_output(['git',*args],cwd=r).decode('utf8')
paths=git('ls-files','-z').split('\0');rows=[];groups=collections.Counter();index=[];docs=[]
text_ext={'.java','.c','.cpp','.h','.hpp','.js','.cjs','.mjs','.json','.gradle','.md','.html','.css','.py','.ps1','.yml','.yaml','.txt','.xml','.properties','.bat','.sh'}
for name in paths:
 if not name:continue
 p=r/name;digest=hashlib.sha256();size=0
 with p.open('rb') as f:
  while chunk:=f.read(1048576):digest.update(chunk);size+=len(chunk)
 category=name.split('/')[0] if '/' in name else '(root)';groups[category]+=1
 row={'path':name,'bytes':size,'sha256':digest.hexdigest(),'group':category,'textIndexed':False}
 if p.suffix in text_ext and not name.startswith('docs/readmes/') and size<20_000_000:
  try:text=p.read_text(encoding='utf-8-sig');row['lines']=text.count('\n')+1;row['textIndexed']=True
  except UnicodeDecodeError:pass
  else:
   if p.suffix in {'.java','.c','.cpp','.h','.hpp','.js','.cjs','.mjs','.gradle','.py','.ps1','.yml','.yaml'}:
    declarations=[line.strip()[:500] for line in text.splitlines() if re.search(r'^\s*(?:import |(?:public |private |protected |static |final |abstract |synchronized )*(?:class |interface |enum |record )|(?:async )?function |export |(?:const|let) .*require\(|def |function )',line)]
    markers=[{'line':i,'text':line.strip()[:300]} for i,line in enumerate(text.splitlines(),1) if re.search(r'\b(TODO|FIXME|stub|placeholder|UnsupportedOperationException)\b',line,re.I)]
    index.append({'path':name,'lines':row['lines'],'declarations':declarations,'reviewMarkers':markers})
   if re.fullmatch(r'README-\d{3}\.md',name):
    paragraphs=re.split(r'\n\s*\n',text);selected=[p for p in paragraphs if re.search(r'NOT PASSED|NOT VALIDATED|not validated|Next:|next step|measured|implemented|limitation|scope|What changed',p,re.I) and not p.startswith('|')]
    docs.append({'path':name,'heading':text.splitlines()[0],'opening':'\n\n'.join(paragraphs[:4]),'selectedPassages':selected[:3]+selected[-2:]})
 rows.append(row)
refs=git('for-each-ref','--format=%(refname:short) %(objectname) %(committerdate:iso8601)','refs/remotes/origin').splitlines()
summary={'reviewedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source':git('rev-parse','HEAD').strip(),'remoteMain':git('rev-parse','origin/main').strip(),'files':len(rows),'bytes':sum(x['bytes'] for x in rows),'textIndexed':sum(x['textIndexed'] for x in rows),'groups':dict(groups),'remoteRefs':refs,'scope':'Every tracked file read and SHA-256 indexed. Source declarations/markers and all numbered README text indexed. Detailed human/agent semantic review is recorded separately; binary hashing is not visual/semantic inspection.'}
for name,value in [('repository094-review.json',{'summary':summary,'files':rows}),('repository094-source-index.json',index),('repository094-editions.json',docs)]: (out/name).write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
(out/'repository094-history.txt').write_text(git('log','--format=%H %ad %an %s','--date=iso','origin/main'),encoding='utf8')
print(json.dumps(summary,indent=2))
