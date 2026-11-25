"""Render two full-pose fitting views, one held-out view, and isolated negative controls."""
import json
import math
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector

source = runpy.run_path(str(Path(__file__).with_name("image-yaw-fixtures.py")))
out = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
out.mkdir(parents=True, exist_ok=True)
scene, camera = source["helper"]["camera_setup"]()
scene.render.resolution_x, scene.render.resolution_y = 480, 360
scene.render.film_transparent = False
scene.render.image_settings.color_mode = "RGB"
scene.display.shading.light = "FLAT"
scene.display.shading.background_type = "WORLD"
scene.world = bpy.data.worlds.new("Plain opaque background")
scene.world.color = (.012, .012, .012)
camera.data.ortho_scale = 3.2

pose_rows = [((-.813,.350,-.419),(33,-17,21)), ((.751,.393,.743),(-53,17,-13))]
basis = Matrix.Rotation(math.pi/2,3,"X")


def rotation(obj, angles):
    yaw,pitch,roll=map(math.radians,angles)
    q=Quaternion((0,1,0),yaw) @ Quaternion((1,0,0),pitch) @ Quaternion((0,0,1),roll)
    obj.rotation_mode="QUATERNION"
    obj.rotation_quaternion=(basis @ q.to_matrix() @ basis.inverted()).to_quaternion()
    return (q.x,q.y,q.z,q.w)


props=[]
truth=[]
for (name,outline,height,_,_), (position,angles) in zip(source["specs"],pose_rows):
    body=source["prism"](name,outline,height)
    body.location=basis @ Vector(position)
    quaternion=rotation(body,angles)
    props.append(body)
    truth.append({"model":name,"positionM":position,"anglesDeg":angles,"quaternion":quaternion})

d=math.sqrt(.5)
guide=((-d/2,-d/2,math.sqrt(.75)),(d,-d,0))
heldout=((.5,-.5,d),(d,d,0))


def render(name, frame):
    source["render"](scene,camera,out/name,*frame)


render("overhead.png",((0,0,1),(1,0,0)))
render("fitting-side.png",guide)
render("held-out.png",heldout)
yaw,pitch,roll=pose_rows[1][1]
pitch_change=-16
rotation(props[1],(yaw,pitch+pitch_change,roll))
render("wrong-tilt.png",heldout)
rotation(props[1],(yaw,pitch,roll))
props[1].location.z+=.09
render("wrong-height.png",heldout)
(out/"truth.json").write_text(json.dumps({
    "blenderVersion":bpy.app.version_string,"widthM":3.2,"imagePixels":[480,360],"samples":truth,
    "pitchOnlyControlDeltaDeg":pitch_change,"heightOnlyControlDeltaM":.09
}, indent=2)+"\n",encoding="utf8")
