
"""Render a new registered prop, then verify and edit the separate visual in Blender."""
import bmesh
import json
import math
from pathlib import Path
import runpy
import sys

import bpy

helpers = runpy.run_path(str(Path(__file__).with_name("browser-top-blender.py")))
box = helpers["box"]
render = helpers["render"]
boolean = helpers["tools"]["boolean"]
original_volume = .0155


def create(directory):
    directory.mkdir(parents=True, exist_ok=True)
    scene, camera = helpers["camera_setup"]()
    plate = box((-.25, -.05, -.2), (.25, .05, .2))
    for low, high in (
        ((-.15, -.08, -.15), (-.05, .08, .10)),
        ((.05, -.08, -.05), (.15, .08, .15)),
    ):
        boolean(plate, box(low, high), "DIFFERENCE")
    actual = helpers["volume"](plate)
    assert math.isclose(actual, original_volume, abs_tol=1e-7), actual
    for role in ("front", "side", "top", "elevated"):
        render(scene, camera, role, directory / f"{role}.png")
    bpy.data.objects.remove(plate, do_unlink=True)
    box((-.08, -.05, -.15), (.08, .05, .15))
    render(scene, camera, "elevated", directory / "wrong.png")


def measure(directory):
    body = next(obj for obj in bpy.context.scene.objects if obj.get("fraynix_source_id") == "surface")
    visual = next(obj for obj in body.children if obj.get("fraynix_visual") == "triangle-surface")
    assert body.rigid_body.collision_shape == "BOX"
    assert visual.rigid_body is None and visual.data is not body.data
    expected_dims = (.50, .40, .10)
    dimensions = tuple(body.dimensions)
    assert all(math.isclose(a, b, abs_tol=2e-6) for a, b in zip(dimensions, expected_dims)), dimensions
    actual = helpers["volume"](visual)
    assert abs(actual - original_volume) / original_volume < .05, actual
    mesh = bmesh.new()
    mesh.from_mesh(visual.data)
    assert all(edge.is_manifold for edge in mesh.edges)
    mesh.free()

    top = max(vertex.co.z for vertex in visual.data.vertices)
    count = 0
    for vertex in visual.data.vertices:
        if abs(vertex.co.z - top) < 1e-6:
            vertex.co.z += .005
            count += 1
    visual.data.update()
    edited_top = max(vertex.co.z for vertex in visual.data.vertices)
    assert count > 0 and math.isclose(edited_top - top, .005, abs_tol=2e-6)
    assert tuple(body.dimensions) == dimensions
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(directory / "library-prop-edited.blend"), compress=True)
    (directory / "blender-library-proof.json").write_text(json.dumps({
        "version": bpy.app.version_string,
        "expectedVolumeM3": original_volume,
        "measuredVolumeM3": actual,
        "colliderShape": body.rigid_body.collision_shape,
        "colliderDimensionsBlenderM": dimensions,
        "separateVisual": True,
        "visualManifold": True,
        "triangles": len(visual.data.polygons),
        "editedVertices": count,
        "originalVisualTopM": top,
        "editedVisualTopM": edited_top,
    }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    mode, path = sys.argv[sys.argv.index("--") + 1:]
    {"create": create, "measure": measure}[mode](Path(path).resolve())
