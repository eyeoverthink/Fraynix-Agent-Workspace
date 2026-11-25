"""Author a changed part independently and verify the placed edit in stock Blender."""
import hashlib
import json
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Vector

SCRIPTS = Path(__file__).parent
BASE = runpy.run_path(str(SCRIPTS/"browser-views-blender.py"))
VIEWS = runpy.run_path(str(SCRIPTS/"browser-top-blender.py"))


def edited_shape():
    body = BASE["box"]((-.25,-.20,-.15),(.25,.002,.15))
    upper = BASE["box"]((-.15,0,-.10),(.15,.35,.10))
    BASE["helpers"]["boolean"](body,upper,"UNION")
    return body


def create(out):
    out.mkdir(exist_ok=True,parents=True)
    BASE["create"](out)
    scene, camera = VIEWS["camera_setup"]()
    body = BASE["solid"]()
    VIEWS["render"](scene,camera,"elevated",out/"unchanged-elevated.png")
    bpy.data.objects.remove(body,do_unlink=True)
    edited_shape()
    VIEWS["render"](scene,camera,"elevated",out/"edited-elevated.png")


def verify(out):
    scene,camera = VIEWS["camera_setup"]()
    truth = edited_shape()
    bpy.context.view_layer.update()
    target_volume = BASE["volume"](truth)
    bpy.data.objects.remove(truth,do_unlink=True)
    exact = out/"qualified-edit.glb"
    bpy.ops.import_scene.gltf(filepath=str(exact),import_scene_extras=True)
    bpy.context.view_layer.update()
    tagged = {json.loads(obj["fraynixBody"])["sourceId"]: obj
              for obj in bpy.context.scene.objects if obj.get("fraynixBody")}
    assert len(tagged) == 4 and set(("floor","rail","crate")).issubset(tagged), tagged
    collider, = [obj for key,obj in tagged.items() if key.startswith("level-prop-")]
    visual, = [obj for obj in collider.children if obj.get("fraynixVisual")=="triangle-surface"]
    assert len(collider.data.vertices)==8 and len(visual.data.polygons)>656
    assert (collider.matrix_world.translation-Vector((.15,-.7,.275))).length<1e-5
    rotation = collider.matrix_world.to_quaternion()
    inverse = rotation.inverted()
    points = [inverse @ (visual.matrix_world @ vertex.co - collider.matrix_world.translation) for vertex in visual.data.vertices]
    dims = [max(v[i] for v in points)-min(v[i] for v in points) for i in range(3)]
    assert (Vector(dims)-Vector((.5,.3,.55))).length<1e-5,dims
    measured = BASE["volume"](visual)
    error = abs(measured-target_volume)/target_volume
    assert error<.005,error
    bpy.ops.wm.save_as_mainfile(filepath=str(out/"qualified-edit.blend"))
    result=dict(version=bpy.app.version_string,glbSha256=hashlib.sha256(exact.read_bytes()).hexdigest(),
                originalBodies=3,colliderVertices=len(collider.data.vertices),visualTriangles=len(visual.data.polygons),
                dimensionsBlenderM=dims,targetVolumeM3=target_volume,exportVolumeM3=measured,volumeRelativeError=error)
    (out/"qualified-blender-proof.json").write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8",newline="\n")


if __name__=="__main__":
    mode,target=sys.argv[sys.argv.index("--")+1:]
    {"create":create,"verify":verify}[mode](Path(target).resolve())
