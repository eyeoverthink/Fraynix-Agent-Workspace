"""Author an original beacon and validate the world poses after a Fraynix round trip."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector


def tagged_box(label, key, location, size, mass):
    bpy.ops.mesh.primitive_cube_add(size=1, location=location)
    box = bpy.context.object
    box.name = label
    box.dimensions = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    box["fraynixBody"] = json.dumps({
        "sourceId": key, "label": label, "massKg": mass, "friction": 0.6, "restitution": 0.0
    })
    return box


def create(output):
    tagged_box("Workshop floor", "floor", (0, 0, -.15), (8, 6, .3), 0)
    collider = tagged_box("Signal beacon", "beacon", (-1, 0, .25), (.8, .6, .5), 1.4)
    ring = [(.19, -.24), (.39, -.17), (.38, -.10), (.19, -.01), (.19, .15),
            (.59, .22), (.69, .28), (.64, .36), (.48, .45), (.12, .55)]
    points = [(r*math.cos(t*2*math.pi/32), r*math.sin(t*2*math.pi/32), z) for r, z in ring for t in range(32)]
    faces = [(j*32+i, j*32+(i+1)%32, (j+1)*32+(i+1)%32, (j+1)*32+i)
             for j in range(len(ring)-1) for i in range(32)]
    faces.extend([tuple(reversed(range(32))), tuple(range(32*(len(ring)-1), 32*len(ring)))])
    mesh = bpy.data.meshes.new("Original turned beacon")
    mesh.from_pydata(points, [], faces)
    mesh.update()
    visual = bpy.data.objects.new("Signal beacon sculpted surface", mesh)
    bpy.context.collection.objects.link(visual)
    visual.parent = collider
    visual["fraynixVisual"] = "triangle-surface"
    info = bpy.data.objects.new("Fraynix scene metadata", None)
    bpy.context.collection.objects.link(info)
    info["fraynixScene"] = json.dumps({
        "title": "Original signal beacon workshop",
        "gravityMps2": 9.81,
        "source": {"kind": "synthetic", "physicalProperties": "declared", "note": "Original Blender-authored edition 217 prop."},
        "scaleReference": {"a": [0,0,0], "b": [1,0,0], "knownLengthM": 1, "toleranceM": .00001, "basis": "declared-test"}
    })
    bpy.ops.export_scene.gltf(filepath=output, export_format="GLB", export_extras=True)


def examine(source, receipt):
    bpy.ops.import_scene.gltf(filepath=source)
    bodies = [(obj, json.loads(obj["fraynixBody"])) for obj in bpy.context.scene.objects if "fraynixBody" in obj]
    original = next(obj for obj, role in bodies if role["sourceId"] == "beacon")
    copied = next(obj for obj, role in bodies if role["sourceId"].startswith("prop-copy-"))
    assert len(bodies) == 3
    delta = copied.matrix_world.translation - original.matrix_world.translation
    assert (delta - Vector((1.8, .9, 0))).length < 1e-5, delta
    quaternion = copied.matrix_world.to_quaternion() @ original.matrix_world.to_quaternion().inverted()
    angle = math.degrees(quaternion.to_euler().z)
    assert abs(angle - 35) < 1e-4, angle
    visual = [next(child for child in root.children if child.get("fraynixVisual") == "triangle-surface")
              for root in (original, copied)]
    rotations = [root.matrix_world.to_quaternion() for root in (original, copied)]
    data = [[rotation.inverted() @ (item.matrix_world @ vertex.co - root.matrix_world.translation)
              for vertex in item.data.vertices] for rotation, item, root in zip(rotations, visual, (original, copied))]
    assert len(data[0]) == len(data[1]) and len(data[0]) >= 300
    error = max((a-b).length for a, b in zip(*data))
    assert error < 1e-5
    Path(receipt).write_text(json.dumps({
        "passed": True, "version": bpy.app.version_string, "meshVertices": len(data[0]), "localErrorM": error,
        "originalColliderVertices": len(original.data.vertices), "copiedColliderVertices": len(copied.data.vertices),
        "offsetBlenderM": list(delta), "yawDeg": angle
    }, indent=2) + "\n", encoding="utf-8")


mode, *files = sys.argv[sys.argv.index("--")+1:]
bpy.ops.wm.read_factory_settings(use_empty=True)
if mode == "create":
    create(*files)
elif mode == "verify":
    examine(*files)
else:
    raise ValueError(mode)
