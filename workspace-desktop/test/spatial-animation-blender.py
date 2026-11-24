
"""Independently sample an authored scene and change its last location key."""
import json
from pathlib import Path
import sys

import bpy

target = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
scene = bpy.context.scene
prop = next(obj for obj in scene.objects if obj.get("fraynix_source_id") == "surface")
surface = next(obj for obj in prop.children if obj.get("fraynix_visual") == "triangle-surface")
assert (scene.frame_start, scene.frame_end, scene.render.fps) == (1, 25, 24)
assert prop.rigid_body.collision_shape == "BOX"
assert not scene.rigidbody_world.enabled

def position(frame):
    scene.frame_set(frame)
    point = prop.evaluated_get(bpy.context.evaluated_depsgraph_get()).matrix_world.translation
    return [point.x, point.z, -point.y]

points = {str(frame): position(frame) for frame in (1, 7, 13, 19, 25)}
anchor = points["1"]
for frame, point in points.items():
    ratio = (int(frame) - 1) / 24
    expected = [anchor[0] + .4 * ratio, anchor[1] + .2 * ratio, anchor[2] - .1 * ratio]
    assert all(abs(a - b) < 1e-6 for a, b in zip(point, expected)), (frame, point, expected)

scene.frame_set(25)
prop.location.x += .25
prop.keyframe_insert(data_path="location", frame=25, group="Location")
anim = prop.animation_data
channelbag = anim.action.layers[0].strips[0].channelbag(anim.action_slot)
for curve in channelbag.fcurves:
    for key in curve.keyframe_points:
        key.interpolation = "LINEAR"

after = position(25)
assert abs(after[0] - points["25"][0] - .25) < 1e-6
scene.frame_set(1)
bpy.context.preferences.filepaths.save_version = 0
bpy.ops.wm.save_as_mainfile(filepath=str(target / "edited-in-blender.blend"), compress=True)
prop.shape_key_add(name="Basis")
shape = prop.shape_key_add(name="Unsupported collider deformation")
shape.data[0].co.x += .1
shape.value = 0
shape.keyframe_insert("value", frame=1)
shape.value = 1
shape.keyframe_insert("value", frame=25)
scene.frame_set(1)
bpy.ops.wm.save_as_mainfile(filepath=str(target / "unsupported-collider.blend"), compress=True)
target.joinpath("blender-animation-evidence.json").write_text(json.dumps({
    "version": bpy.app.version_string,
    "samplesM": points,
    "editedEndM": after,
    "vertices": len(surface.data.vertices),
    "triangles": len(surface.data.polygons)
}, indent=2), encoding="utf-8")
