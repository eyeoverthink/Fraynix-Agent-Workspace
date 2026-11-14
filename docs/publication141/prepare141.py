"""Build original-byte packs and independently reviewed labels before predictions."""
from pathlib import Path
from fractions import Fraction
import json,hashlib,zipfile,base64,math,re,datetime
from select141 import rows,finite,rectangle
# Import only shared source geometry; selection runs separately before review.
r=Path.cwd();folder=r/'build/vot141';out=r/'docs/publication141';sha=lambda b:hashlib.sha256(b).hexdigest()
lock=json.loads((r/'docs/OBSERVATION_141_LOCK.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
review=json.loads((out/'visibility141.json').read_text(encoding='utf8'));prior=set();priorFiles=0
for p in (r/'docs').glob('publication*/*.json'):
 if p.parent==out:continue
 found=re.findall(r'"imageSha256"\s*:\s*"([a-f0-9]{64})"',p.read_text(encoding='utf-8-sig'))
 if found:priorFiles+=1;prior.update(found)
records=[];corrections=[]
for spec in json.loads((folder/'source-index.json').read_text(encoding='utf8')):
 name=spec['name'];v=review['sources'][name];w,h=spec['width'],spec['height'];sel=spec['selection'];assert v['reviewedIndices']==sel['indices']
 ground=rows((out/(name+'-groundtruth.txt')).read_bytes());z=zipfile.ZipFile(folder/(name+'-color.zip'));files=sorted(n for n in z.namelist() if n.lower().endswith('.jpg'));frames=[];labels=[]
 def box(n):
  exact=rectangle(ground[n-1],w,h);b=list(map(float,exact))
  for axis in [0,1]:
   if exact[axis+2]-exact[axis]>=Fraction(1,40) and b[axis+2]-b[axis]<.025:
    before=b.copy()
    if b[axis+2]<1:b[axis+2]=math.nextafter(b[axis+2],math.inf)
    else:b[axis]=math.nextafter(b[axis],-math.inf)
    corrections.append({'name':name,'frame':n,'axis':axis,'before':before,'after':b.copy(),'reason':'Exact rational side meets1/40; one outward ULP corrects representation, not geometry'})
  return b
 for ordinal,n in enumerate(sel['indices']):
  raw=z.read(files[n-1]);digest=sha(raw);assert digest not in prior,(name,n,'Previously retained image');frames.append({'sourceFrame':n,'image':base64.b64encode(raw).decode(),'imageSha256':digest})
  if ordinal==0:continue
  visibility='visible' if finite(ground[n-1]) else 'uncertain'
  for key in ['partial','occluded','out-of-view','uncertain']:
   if any(a<=n<=b for a,b in v.get(key,[])):visibility=key
  note='External VOT '+('finite rectangle' if finite(ground[n-1]) else 'NaN region')+'; separate source-only AI review: '+visibility+'.'
  row={'ordinal':ordinal,'sourceFrame':n,'imageSha256':digest,'visibility':visibility,'note':note}
  if visibility in ['visible','partial']:
   assert finite(ground[n-1]),(name,n,'Cannot invent an external rectangle')
   b=box(n)
   if b[2]-b[0]<.025 or b[3]-b[1]<.025:row['visibility']='uncertain';row['note']+=' External rectangle below existing measurable bound; unscorable, not hidden.'
   else:row['targetBox']=b
  labels.append(row)
 case=name+'-'+str(sel['windowStart']);pack={'schema':'fraynix-observation-frames-036-v1','title':'Fresh141 '+case+' public source','source':'VOT-LT2018 / https://www.votchallenge.net/vot2018/dataset.html ; exact original JPEGs; indices and nominal stride retained','license':'Public research benchmark; authors retain rights. Local evaluation and limited attributed screenshots; no raw archive redistribution.','clock':'source-frame','objects':[{'key':'subject','label':'VOT '+name+' target','box':box(sel['windowStart'])}],'frames':frames}
 p=folder/(case+'.fobs.json');assert not p.exists();p.write_bytes(json.dumps(pack).encode());assert p.stat().st_size<=20_000_000,(case,p.stat().st_size)
 lab={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'VOT-LT2018 authors (rectangles/NaN); Codex source-only AI (visibility/uncertainty)','method':'external-annotations','exposure':'before-tracking','provenance':'Original XYWH values directly normalized/clipped. Separate source-only AI review before predictions; no independent human adjudication. Unknown/fragmented absence is uncertain. Labels never enter tracking; no other-object geometry.','rows':labels}
 lp=out/(case+'-labels.json');assert not lp.exists();lp.write_bytes((json.dumps(lab,indent=2)+'\n').encode())
 counts={k:sum(x['visibility']==k for x in labels) for k in ['visible','partial','occluded','out-of-view','uncertain']}
 records.append({'case':case,'name':name,'target':1,'start':sel['windowStart'],'end':sel['windowEnd'],'stride':sel['stride'],'sourceFrames':sel['indices'],'frames':len(frames),'width':w,'height':h,'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':counts,'distractorLabelledSamples':0,'originalGroundTruth':f'docs/publication141/{name}-groundtruth.txt','originalGroundTruthSha256':spec['groundtruthSha256'],'domain':'real-camera','selection':sel,'skippedEvents':spec['skippedEvents'],'exposure':'New source images for this project, reviewed after method freeze and before predictions.'})
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha((r/'docs/OBSERVATION_141_LOCK.json').read_bytes()),'excludedSources':json.loads((folder/'excluded.json').read_text(encoding='utf8')),'records':records,'downloads':json.loads((folder/'download.json').read_text()),'annotationSearch':json.loads((folder/'source-index.json').read_text()),'sourceReview':review['summary'],'rationalNormalizationCorrections':corrections,'priorImageOverlap':{'priorPublicationFiles':priorFiles,'uniquePriorImageHashes':len(prior),'matchedSelectedHashes':0,'scope':'Exact JPEG hashes against retained publication JSON image hashes; not proof against different encodings.'},'limitations':['Bounded windows, not a full VOT benchmark or live camera validation.','External rectangles plus separately attributed AI source-only visibility; no independent human review.','No labelled distractor geometry, so zero labelled-other matches cannot establish switch-free identity.','Unknown and too-small observations stay explicit; original annotations retained.']}
(out/'source141.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode());print(json.dumps([{'case':x['case'],'counts':x['visibilityCounts']} for x in records]))
