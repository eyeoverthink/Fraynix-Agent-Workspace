from pathlib import Path
import json,hashlib,zipfile,base64,math,re,datetime
r=Path.cwd();folder=r/'build/vot139';out=r/'docs/publication139';sha=lambda b:hashlib.sha256(b).hexdigest();lock=json.loads((r/'docs/OBSERVATION_139_LOCK.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
records=[];old_hashes=set();old_files=0
for p in (r/'docs').glob('publication*/*.json'):
 if p.parent==out:continue
 text=p.read_text(encoding='utf-8-sig');found=re.findall(r'"imageSha256"\s*:\s*"([a-f0-9]{64})"',text)
 if found:old_files+=1;old_hashes.update(found)
def visibility(name,n,finite):
 if name=='ballet':
  if not finite:return 'out-of-view' if 119<=n<=129 else 'uncertain'
  return 'partial' if 109<=n<=114 or 142<=n<=151 else 'visible'
 if name=='bicycle':
  if not finite:return 'out-of-view' if 116<=n<=130 else 'uncertain'
  return 'partial' if n>=106 else 'visible'
 if name=='bird1':
  if not finite:return 'out-of-view' if 659<=n<=767 else 'uncertain'
  return 'partial' if 649<=n<=655 or 772<=n<=779 else 'visible'
 if name=='car1':
  if not finite:return 'occluded' if 1533<=n<=1536 else 'uncertain'
  return 'partial' if 1518<=n<=1522 or 1546<=n<=1553 else 'visible'
 return 'visible'
for spec in json.loads((folder/'source-index.json').read_text(encoding='utf8')):
 name=spec['name'];z=zipfile.ZipFile(folder/(name+'-color.zip'));files=sorted(n for n in z.namelist() if n.endswith('.jpg'));ground=[list(map(float,s.split(','))) for s in (out/(name+'-groundtruth.txt')).read_text(encoding='utf8').splitlines()];w,h=spec['width'],spec['height'];selection=spec['selection'];frames=[];rows=[]
 def box(n):
  x,y,bw,bh=ground[n-1];return [max(0,x/w),max(0,y/h),min(1,(x+bw)/w),min(1,(y+bh)/h)]
 for ordinal,n in enumerate(selection['indices']):
  raw=z.read(files[n-1]);digest=sha(raw);assert digest not in old_hashes,(name,n,'Already exposed image');frames.append({'sourceFrame':n,'image':base64.b64encode(raw).decode(),'imageSha256':digest})
  if not ordinal:continue
  finite=all(math.isfinite(v) for v in ground[n-1]);v=visibility(name,n,finite)
  note=('External VOT finite rectangle; source-only AI review confirms '+v+'.' if finite else 'External VOT NaN region; source-only AI review '+('confirms target fully outside active image.' if v=='out-of-view' else 'confirms target fully behind central structure.' if v=='occluded' else 'finds visible/ambiguous boundary fragments without an external box; no fabricated rectangle or absence.'))
  row={'ordinal':ordinal,'sourceFrame':n,'imageSha256':digest,'visibility':v,'externalRegion':'finite-rectangle' if finite else 'nan-region','note':note}
  if v in ['visible','partial']:row['targetBox']=box(n)
  rows.append(row)
 case=name+'-'+str(selection['windowStart']);pack={'schema':'fraynix-observation-frames-036-v1','title':'Fresh139 '+case+' public source','source':'VOT-LT2018 / Visual Object Tracking Challenge; https://www.votchallenge.net/vot2018/dataset.html ; original JPEGs, sampled source indices retained, nominal stride '+str(selection['stride']),'license':'Public research benchmark; authors retain rights. Local evaluation and limited attributed screenshots; no raw archive redistribution.','clock':'source-frame','objects':[{'key':'subject','label':'VOT '+name+' target','box':box(selection['windowStart'])}],'frames':frames};p=folder/(case+'.fobs.json');assert not p.exists();p.write_text(json.dumps(pack),encoding='utf8');assert p.stat().st_size<=20_000_000
 labels={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'VOT-LT2018 authors (rectangles/NaN); Codex source-only AI (visibility/cause/boundary uncertainty)','method':'external-annotations','exposure':'before-tracking','provenance':'Original VOT XYWH floats converted directly to clipped normalized corners, matching official parser (no MATLAB subtraction). NaN regions are not automatically treated as fully hidden: source-only AI review marks ambiguous/part-visible boundaries uncertain. No independent human adjudication of AI additions and no distractor boxes. All source images reviewed before predictions; labels never enter tracker.','rows':rows};lp=out/(case+'-labels.json');assert not lp.exists();lp.write_text(json.dumps(labels,indent=2)+'\n',encoding='utf8');counts={v:sum(a['visibility']==v for a in rows) for v in ['visible','partial','occluded','out-of-view','uncertain']}
 records.append({'case':case,'name':name,'target':1,'start':selection['windowStart'],'end':selection['windowEnd'],'stride':selection['stride'],'sourceFrames':selection['indices'],'frames':len(frames),'width':w,'height':h,'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':counts,'distractorLabelledSamples':0,'originalGroundTruth':'docs/publication139/'+name+'-groundtruth.txt','originalGroundTruthSha256':spec['groundtruthSha256'],'domain':'real-camera','selection':selection,'skippedEvents':spec['skippedEvents'],'exposure':'New source imagery for this project, reviewed after method freeze and before predictions.'})
review='All362 selected source images reviewed in source-only contact sheets before predictions. Ballet/bicycle edge fragments inspected in detail; their absence labels begin before full visual disappearance and end after partial return, so ambiguous boundaries remain uncertain. VOT bird1 is a real onboard flight camera with black borders, distinct from rendered OTB Bird1; its selected event is another active-image exit. Car1 enters behind a central vertical structure and returns; detailed source crops support four confidently hidden frames1533–1536, with19 NaN boundary frames uncertain. Bike1 first96 images remain visible. Bird1 stride2 with explicit interval endpoints gives78 sampled images and skips intervening frames. External boxes plus separate AI visibility/cause review, no independent human adjudication and no other-object geometry. Frozen scorer breaks recovery episodes at uncertainty; descriptive boundary return tables must not invent recovery timing across those gaps.'
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha((r/'docs/OBSERVATION_139_LOCK.json').read_bytes()),'downloads':json.loads((folder/'download.json').read_text(encoding='utf8')),'annotationSearch':json.loads((folder/'bird-occlusion-search.json').read_text(encoding='utf8'))+json.loads((folder/'occlusion-search.json').read_text(encoding='utf8')),'records':records,'sourceReview':review,'priorImageOverlap':{'priorPublicationFiles':old_files,'uniquePriorImageHashes':len(old_hashes),'matchedSelectedHashes':0,'scope':'Exact JPEG-byte overlap against prior retained publication JSON image hashes; different encodings are not ruled out by byte identity alone. Source scenes were also visually compared.'},'limitations':['Five bounded windows, not a full VOT benchmark or real-time camera validation.','One physical obstruction episode with four confidently hidden samples; three exits/re-entries and a visible-motion control.','Independent external rectangles/NaN data; AI source visibility is not independently human adjudicated.','No independent distractor boxes, so zero confirmed switches is not proof of identity continuity.','Preserved uncertain boundaries limit frozen scorer recovery-episode counts.','Official SHA1, CRC and uncompressed size match; listed compressed sizes differ from exact ZIP lengths.']};(out/'source139.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8');print(json.dumps({'records':[{k:x[k] for k in ['case','frames','visibilityCounts']} for x in records],'overlap':manifest['priorImageOverlap']}))
