"""Render independent prop models, rotated scene views, and a rotation-only negative control."""
import json
import math
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Vector

helper = runpy.run_path(str(Path(__file__).with_name("browser-top-blender.py")))
out = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
out.mkdir(parents=True, exist_ok=True)

# These independently authored footprints use 50 mm steps and stand on the XY plane.
specs = [
    ("elbow", [(-.25,-.20),(.25,-.20),(.25,-.10),(-.10,-.10),(-.10,.20),(-.25,.20)],
     .20, [-.924,-.487], 31),
    ("step", [(-.25,-.20),(.25,-.20),(.25,.00),(.15,.00),(.15,.20),(-.05,.20),(-.05,.10),(-.25,.10)],
     .30, [.793,.834], -55)
]


def prism(name, boundary, height):
    n = len(boundary)
    points = [(x, -z, y) for y in (-height/2, height/2) for x,z in boundary]
    sides = [(i, i+n, (i+1)%n+n, (i+1)%n) for i in range(n)]
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(points, [], [tuple(range(n)), tuple(reversed(range(n,2*n))), *sides])
    mesh.update()
    obj = bpy.data.objects.new(name,mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj.color = (.80,.80,.80,1)
    return obj


def render(scene, camera, target, eye, right):
    eye, right = Vector(eye).normalized(), Vector(right).normalized()
    camera.location = eye * 6
    camera.rotation_euler = Matrix((right,eye.cross(right),eye)).transposed().to_euler()
    scene.render.filepath = str(target)
    bpy.ops.render.render(write_still=True)


d = math.sqrt(.5)
elevated = (-.5*d,-.5*d,math.sqrt(.75))
across = (d,-d,0)
yaw_control_deg = 10
scene, camera = helper["camera_setup"]()
scene.render.film_transparent=False
scene.render.image_settings.color_mode="RGB"
scene.display.shading.light="FLAT"
scene.display.shading.background_type="WORLD"
scene.world=bpy.data.worlds.new("Plain opaque background")
scene.world.color=(.012,.012,.012)

for name,outline,height,_,_ in specs:
    obj=prism(name,outline,height)
    target=out/name
    target.mkdir(exist_ok=True)
    for role,eye,right in (
        ("front",(0,-1,0),(1,0,0)),("side",(-1,0,0),(0,-1,0)),
        ("top",(0,0,1),(1,0,0)),("elevated",elevated,across)
    ):
        render(scene,camera,target/(role+".png"),eye,right)
    bpy.data.objects.remove(obj,do_unlink=True)

scene.render.resolution_x=400
scene.render.resolution_y=300
camera.data.ortho_scale=4
bodies=[]
for name,outline,height,(x,z),yaw in specs:
    body=prism(name,outline,height)
    body.location=(x,-z,height/2)
    body.rotation_euler.z=math.radians(yaw)
    bodies.append(body)
render(scene,camera,out/"overhead.png",(0,0,1),(1,0,0))
render(scene,camera,out/"second-angle.png",elevated,across)
bodies[1].rotation_euler.z+=math.radians(yaw_control_deg)
render(scene,camera,out/"rotated-second-angle.png",elevated,across)
bodies[1].rotation_euler.z-=math.radians(yaw_control_deg)
bodies[1].location.x+=.14
render(scene,camera,out/"shifted-second-angle.png",elevated,across)
(out/"truth.json").write_text(json.dumps({
    "blenderVersion":bpy.app.version_string,
    "widthM":4,
    "imagePixels":[400,300],
    "samples":[{"model":name,"positionXZ":position,"yawDeg":yaw,"modelWidthM":.5}
               for name,_,_,position,yaw in specs],
    "yawOnlyControlDeltaDeg":yaw_control_deg,
    "shiftOnlyControlXM":.14
},indent=2)+"\n",encoding="utf8")
