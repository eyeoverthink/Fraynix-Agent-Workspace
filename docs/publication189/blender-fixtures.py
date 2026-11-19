"""Build independent Blender test inputs and inspect exported game layout data."""
import json
import math
from pathlib import Path
import sys

import bpy


def save(file):
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(file), compress=True)


def fixtures(folder):
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete()
    scene = bpy.context.scene
    scene.name = "Blender bench with a declared one metre ruler"
    scene.unit_settings.system = "METRIC"
    scene.unit_settings.scale_length = .01
    scene["fraynix_known_length_m"] = 1
    scene["fraynix_tolerance_m"] = .001
    scene["fraynix_source_kind"] = "synthetic"
    for index, name in enumerate(("Fraynix Scale A", "Fraynix Scale B")):
        marker = bpy.data.objects.new(name, None)
        scene.collection.objects.link(marker)
        marker.location = (0, 100 * index, 0)

    specs = [
        ("floor", (0, 0, -20), (800, 600, 40), 0, 0, .6),
        ("obstacle", (0, 0, 50), (60, 150, 100), math.pi/9, 0, .5),
        ("crate", (-200, -25, 25), (50, 50, 50), 0, 1.25, .4),
    ]
    for name, location, size, turn, mass, friction in specs:
        bpy.ops.mesh.primitive_cube_add(size=1, location=location)
        obj = bpy.context.object
        obj.name = name
        obj.rotation_euler.z = turn
        obj.scale = size
        bpy.ops.rigidbody.object_add()
        obj.rigid_body.type = "ACTIVE" if mass else "PASSIVE"
        obj.rigid_body.collision_shape = "BOX"
        obj.rigid_body.friction = friction
        if mass:
            obj.rigid_body.mass = mass

    save(folder / "authored-bench.blend")
    scene.objects["Fraynix Scale B"].location.y = 110
    save(folder / "wrong-scale.blend")
    scene.objects["Fraynix Scale B"].location.y = 100
    scene.objects["obstacle"].rigid_body.collision_shape = "SPHERE"
    save(folder / "unsupported.blend")


def inspect_game(output):
    scene = bpy.context.scene
    text = bpy.data.texts["Fraynix Game snapshot.json"].as_string()
    package = json.loads(text)
    meshes = {obj["fraynix_source_id"]: obj for obj in scene.objects if obj.type == "MESH"}
    expected = {obj["id"]: obj for obj in package["frame"]["entities"] if obj.get("shape") == "quad"}
    assert meshes.keys() == expected.keys()
    assert scene["fraynix_snapshot_sha256"] == package["frame"]["snapshotHash"]
    assert scene.unit_settings.system == "NONE"
    for source_id, obj in meshes.items():
        entry = expected[source_id]
        x, y, _ = entry["position"]
        width, height, _ = entry["size"]
        assert math.dist(obj.location, (x, 0, y)) <= 1e-5
        assert len(obj.data.vertices) == 4 and len(obj.data.polygons) == 1
        assert all(abs(v.co.y) <= 1e-7 for v in obj.data.vertices)
        assert math.isclose(obj.dimensions.x, width, abs_tol=1e-6)
        assert math.isclose(obj.dimensions.z, height, abs_tol=1e-6)
        assert obj["fraynix_render_layer"] == entry["renderLayer"]
        assert json.loads(obj["fraynix_components"]) == entry["components"]
        assert obj.rigid_body is None
    output.write_text(json.dumps({"text": text, "quads": len(meshes)}), encoding="utf-8")


mode, location = sys.argv[sys.argv.index("--") + 1:]
if mode == "fixture":
    fixtures(Path(location))
elif mode == "inspect-game":
    inspect_game(Path(location))
else:
    raise ValueError("Use fixture or inspect-game")
