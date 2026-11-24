
"""Render registered fixtures and independently measure the returned GLB in Blender."""
import json
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Vector

helpers = runpy.run_path(str(Path(__file__).with_name("spatial-reconstruction-blender.py")))
setup, volume = (helpers[name] for name in ("setup", "volume"))
exchange = runpy.run_path(str(Path(__file__).with_name("spatial-depth-blender.py")))

def box(low, high):
    low, high = Vector((low[0], -low[2], low[1])), Vector((high[0], -high[2], high[1]))
    bpy.ops.mesh.primitive_cube_add(size=1, location=(low+high)/2)
    obj = bpy.context.object
    obj.dimensions = tuple(abs(i) for i in high-low)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.color = (.07, .21, .38, 1)
    return obj

def solid(narrow=False):
    if narrow:
        return box((-.10,-.20,-.05),(.10,.20,.05))
    body = box((-.25,-.20,-.15),(.25,.002,.15))
    upper = box((-.15,0,-.10),(.15,.20,.10))
    helpers["boolean"](body,upper,"UNION")
    return body

def render(scene, camera, eye, right, target):
    eye, right = Vector(eye).normalized(), Vector(right).normalized()
    camera.location = eye * 4
    camera.rotation_euler = Matrix((right, Vector((0,0,1)), eye)).transposed().to_euler()
    scene.render.filepath = str(target)
    bpy.ops.render.render(write_still=True)

def create(out):
    out.mkdir(exist_ok=True, parents=True)
    scene, camera = setup()
    camera.data.ortho_scale = 1
    scene.render.resolution_x = scene.render.resolution_y = 240
    body = solid()
    render(scene,camera,(0,-1,0),(1,0,0),out/"front.png")
    render(scene,camera,(-1,0,0),(0,-1,0),out/"side.png")
    render(scene,camera,(-1,-1,0),(1,-1,0),out/"held-out.png")
    bpy.data.objects.remove(body,do_unlink=True)
    solid(narrow=True)
    render(scene,camera,(-1,-1,0),(1,-1,0),out/"wrong.png")

def verify(out):
    scene,camera = setup()
    camera.data.ortho_scale = 1
    scene.render.resolution_x = scene.render.resolution_y = 240
    target = solid()
    bpy.context.view_layer.update()
    expected = list(target.dimensions)
    truth = volume(target)
    render(scene,camera,(-1,-1,0),(1,-1,0),out/"blender-held-out-original.png")
    # The low box determines width and depth. The rise determines height.
    assert all(abs(a-b)<1e-6 for a,b in zip(expected,(.5,.3,.4))), expected
    bpy.data.objects.remove(target,do_unlink=True)
    model,_ = exchange["import_mesh"](out/"browser-verified.glb")
    bpy.context.view_layer.update()
    actual = list(model.dimensions)
    measured = volume(model)
    assert all(abs(a-b)<1e-6 for a,b in zip(actual,expected)), actual
    volume_error = abs(measured-truth)/truth
    assert volume_error < .005, (measured,truth)
    render(scene,camera,(-1,-1,0),(1,-1,0),out/"blender-held-out-export.png")
    a,b = [helpers["silica"](out/name) for name in
           ("blender-held-out-original.png","blender-held-out-export.png")]
    iou = sum(x and y for x,y in zip(a,b)) / sum(x or y for x,y in zip(a,b))
    assert iou > .99, iou
    (out/"blender-view-evidence.json").write_text(json.dumps({
        "version":bpy.app.version_string, "originalDimensionsBlenderM":expected, "modelDimensionsBlenderM":actual,
        "originalVolumeM3":truth,"modelVolumeM3":measured,"volumeRelativeError":volume_error,
        "independentObliqueIou":iou,"triangles":len(model.data.polygons)
    },indent=2),encoding="utf8")

if __name__=="__main__":
    mode,target=sys.argv[sys.argv.index("--")+1:]
    (create if mode=="create" else verify)(Path(target).resolve())
