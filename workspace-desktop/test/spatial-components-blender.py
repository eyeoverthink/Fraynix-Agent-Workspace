
"""Verify a refitted body, edit its visual cap and save for Fraynix."""
import json
from pathlib import Path
import sys

import bpy

out = Path(sys.argv[sys.argv.index("--") + 1])
body = next(obj for obj in bpy.context.scene.objects if obj.get("fraynix_source_id") == "surface")
mesh = next(obj for obj in body.children if obj.get("fraynix_visual") == "triangle-surface")
assert body.rigid_body.collision_shape == "BOX"
assert abs(body.dimensions.z - .53) < 2e-6, tuple(body.dimensions)
assert abs(body.location.z - .015) < 2e-6, tuple(body.location)
top = max(vertex.co.z for vertex in mesh.data.vertices)
assert abs(top - .265) < 2e-6, top
for vertex in mesh.data.vertices:
    if abs(vertex.co.z - top) < 1e-6:
        vertex.co.z += .005
mesh.data.update()
bpy.context.preferences.filepaths.save_version = 0
bpy.ops.wm.save_as_mainfile(filepath=str(out / "blender-component-edit.blend"), compress=True)
(out / "blender-components.json").write_text(json.dumps({
    "version": bpy.app.version_string, "fittedHeightM": body.dimensions.z,
    "fittedCenterM": body.location.z, "editedTopLocalM": max(v.co.z for v in mesh.data.vertices),
    "triangles": len(mesh.data.polygons)
}, indent=2), encoding="utf-8")
