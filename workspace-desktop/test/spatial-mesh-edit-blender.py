
"""Independently measure the edited shell, then move its cap in Blender."""
import json
from pathlib import Path
import sys

import bmesh
import bpy

out = Path(sys.argv[sys.argv.index("--") + 1])
scene = bpy.context.scene
prop = next(obj for obj in scene.objects if obj.get("fraynix_source_id") == "surface")
surface = next(obj for obj in prop.children if obj.get("fraynix_visual") == "triangle-surface")

def measure():
    mesh = bmesh.new()
    mesh.from_mesh(surface.data)
    vertices = len(mesh.verts)
    triangles = len(mesh.faces)
    volume = mesh.calc_volume(signed=True)
    all_manifold = all(v.is_manifold for v in mesh.verts) and all(e.is_manifold and e.is_contiguous for e in mesh.edges)
    mesh.free()
    return {"vertices": vertices, "triangles": triangles, "signedVolumeM3": volume, "closedOriented": all_manifold}

original = measure()
assert original["closedOriented"]
assert abs(original["signedVolumeM3"] - .08625) < 1e-7, original
assert prop.rigid_body.collision_shape == "BOX"
assert abs(prop.dimensions.z - .5) < 1e-7

top = max(vertex.co.z for vertex in surface.data.vertices)
moved = 0
for vertex in surface.data.vertices:
    if abs(vertex.co.z - top) < 1e-6:
        vertex.co.z += .025
        moved += 1
surface.data.update()
changed = measure()
assert changed["closedOriented"]
assert abs(changed["signedVolumeM3"] - original["signedVolumeM3"] - .002625) < 1e-7, changed
assert moved > 4
bpy.context.preferences.filepaths.save_version = 0
bpy.ops.wm.save_as_mainfile(filepath=str(out / "mesh-blender-edit.blend"), compress=True)
existing = set(scene.objects)
bpy.ops.wm.stl_import(filepath=str(out / "extruded-shell-mm.stl"), global_scale=.001,
                      forward_axis="Y", up_axis="Z")
stl, = set(scene.objects) - existing
stl_mesh = bmesh.new()
stl_mesh.from_mesh(stl.data)
stl_mesh.transform(stl.matrix_world)
stl_volume = stl_mesh.calc_volume(signed=True)
assert all(edge.is_manifold for edge in stl_mesh.edges)
assert abs(stl_volume - original["signedVolumeM3"]) < 1e-7, (stl_volume, original)
z_values = [vertex.co.z for vertex in stl_mesh.verts]
assert abs(min(z_values)) < 1e-6 and abs(max(z_values) - .6) < 1e-6
stl_mesh.free()
(out / "blender-mesh-evidence.json").write_text(json.dumps({
    "version": bpy.app.version_string, "original": original, "changed": changed,
    "capMoveM": .025, "editedVertices": moved, "colliderZMetres": prop.dimensions.z,
    "stlReadback": {"heightM": max(z_values), "bottomM": min(z_values), "closed": True, "signedVolumeM3": stl_volume}
}, indent=2), encoding="utf-8")
