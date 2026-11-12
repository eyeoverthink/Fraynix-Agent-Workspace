from pathlib import Path
import json,math
r=Path.cwd();d=json.loads((r/'build/model126-complete/result.json').read_text(encoding='utf8'));assert d['passed']
def angle(a,b):
 na=math.sqrt(sum(x*x for x in a));nb=math.sqrt(sum(x*x for x in b));assert na>0 and nb>0
 return math.degrees(2*math.acos(min(1,abs(sum(x*y for x,y in zip(a,b)))/(na*nb))))
assert abs(angle([0,1.001/math.sqrt(2),0,1.001/math.sqrt(2)],[0,0,0,1])-90)<1e-8
rows=[]
for row in [*d['direct'],*d['requests']]:
 if 'world' not in row['kind']:continue
 frame=row['after']['frame'];checks=[]
 for t in row['spec']['targets']:
  obj=next(x for x in frame['originalScene']['objects'] if x['sourceId']==t['sourceId']);body=next(x for x in frame['bodies'] if x['id']==obj['id']);half=math.radians(t['yawDeg'])/2
  a=angle(body['rotation'],[0,math.sin(half),0,math.cos(half)]);position=math.dist(body['position'],t['positionM']);speed=math.sqrt(sum(x*x for x in body['velocity']))
  assert a<=5 and position<=.1 and speed<=.2
  checks.append({'sourceId':t['sourceId'],'positionErrorM':position,'normalizedAngleErrorDeg':a,'terminalSpeedMps':speed})
 rows.append({'kind':row['kind'],'checks':checks})
(r/'build/independent126-world.json').write_text(json.dumps({'passed':True,'rows':rows,'scope':'Independent recomputation from retained actual world bodies, normalizing recorded floating-point quaternions before angular comparison. Original driver results remain unmodified; zero clamped angles there are not a physical precision claim.'},indent=2),encoding='utf8');print(json.dumps({'passed':True,'rows':rows}))
