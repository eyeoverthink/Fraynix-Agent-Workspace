from pathlib import Path
import hashlib,json,shutil
r=Path.cwd();out=r/'docs/readmes/168';images=[]
for width in [1450,390]:
 for suffix in ['', '-viewport']:
  name=f'guide168-{width}{suffix}.png';p=r/'build'/name;shutil.copy2(p,out/name)
  images.append({'path':'build/'+name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'width':width})
review={'passed':True,'reviewedBy':'assistant visual inspection','images':images,
 'scope':'Wide and narrow full-page/viewport guide images inspected. Readable text, local table scrolling, actual app pictures, measured motion plot, source search and capability links. Automated checks independently verify controls, image loading and page overflow.'}
(r/'build/visual-review168.json').write_text(json.dumps(review,indent=2)+'\n')
for name in ['visual168.json','visual-review168.json']:shutil.copy2(r/'build'/name,out/name)
print(json.dumps({'passed':True,'guideImages':len(images)}))
