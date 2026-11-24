
"""Generate independent views of a perforated plate and measure the two exported controls."""
import json
import math
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Vector

helpers = runpy.run_path(str(Path(__file__).with_name("browser-views-blender.py")))
setup, box, volume = (helpers[name] for name in ("setup", "box", "volume"))
tools = helpers["helpers"]
import_mesh = helpers["exchange"]["import_mesh"]

def plate(opening=True):
    body = box((-.25,-.05,-.2),(.25,.05,.2))
    if opening:
        cutter = box((-.15,-.1,-.10),(.15,.1,.15))
        tools["boolean"](body,cutter,"DIFFERENCE")
    return body

def camera_setup():
    scene,camera = setup()
    camera.data.ortho_scale = 1
    scene.render.resolution_x = scene.render.resolution_y = 240
    return scene,camera

def render(scene,camera,role,target):
    d = math.sqrt(.5)
    eye,right = {
        "front": ((0,-1,0),(1,0,0)),
        "side": ((-1,0,0),(0,-1,0)),
        "top": ((0,0,1),(1,0,0)),
        "level": ((-d,-d,0),(d,-d,0)),
        "elevated": ((-.5*d,-.5*d,math.sqrt(.75)),(d,-d,0))
    }[role]
    eye,right = Vector(eye).normalized(),Vector(right).normalized()
    up = eye.cross(right)
    camera.location = eye*4
    camera.rotation_euler = Matrix((right,up,eye)).transposed().to_euler()
    scene.render.filepath = str(target)
    bpy.ops.render.render(write_still=True)

def create(out):
    out.mkdir(exist_ok=True,parents=True)
    scene,camera = camera_setup()
    plate()
    for role in ("front","side","top","level","elevated"):
        render(scene,camera,role,out/f"{role}.png")

def iou(a,b):
    x,y = map(tools["silica"],(a,b))
    return sum(i and j for i,j in zip(x,y))/sum(i or j for i,j in zip(x,y))

def verify(out):
    scene,camera = camera_setup()
    truth = plate()
    actual = volume(truth)
    for role in ("front","side","elevated"):
        render(scene,camera,role,out/f"original-{role}.png")
    bpy.data.objects.remove(truth,do_unlink=True)
    measured = {}
    for name in ("two-view", "with-top"):
        model,group = import_mesh(out/f"{name}.glb")
        bpy.context.view_layer.update()
        dims = list(model.dimensions)
        assert all(abs(a-b)<1e-6 for a,b in zip(dims,(.5,.4,.1))), dims
        fits = {}
        for role in ("front","side","elevated"):
            image = out/f"{name}-{role}.png"
            render(scene,camera,role,image)
            fits[role] = iou(out/f"original-{role}.png",image)
        measured[name] = {"volumeM3":volume(model),"viewIou":fits}
        for obj in group:
            bpy.data.objects.remove(obj,do_unlink=True)
    baseline,improved = (measured[name] for name in ("two-view","with-top"))
    assert abs(actual-.0125)<1e-7 and abs(improved["volumeM3"]-actual)<1e-7
    assert abs(baseline["volumeM3"]-.02)<1e-7
    assert baseline["viewIou"]["front"]>.99 and baseline["viewIou"]["side"]>.99
    assert baseline["viewIou"]["elevated"]<.86 and improved["viewIou"]["elevated"]>.99
    (out/"blender-top-evidence.json").write_text(json.dumps({
        "version":bpy.app.version_string,"originalVolumeM3":actual,"models":measured
    },indent=2),encoding="utf-8")

if __name__=="__main__":
    mode,path=sys.argv[sys.argv.index("--")+1:]
    (create if mode=="create" else verify)(Path(path).resolve())
