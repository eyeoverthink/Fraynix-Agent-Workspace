"""Author six original metric gate scenes with paired matching endpoints."""
import json
import math
from pathlib import Path
import sys

import bpy


def body(label, key, position, dimensions, mass=0.0):
    bpy.ops.mesh.primitive_cube_add(size=1, location=position)
    obj = bpy.context.object
    obj.name = label
    obj.dimensions = dimensions
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj["fraynixBody"] = json.dumps({
        "sourceId": key, "label": label, "massKg": mass, "friction": 0.5, "restitution": 0.0
    })
    return obj


def attach_key_surface(prop):
    outline = [(-.245,-.51), (.245,-.51), (.245,-.23), (.11,-.23),
               (.11,.36), (.17,.36), (.17,.51), (-.11,.51), (-.11,-.23), (-.245,-.23)]
    count = len(outline)
    vertices = [(x,z,y) for y in [-.12,.16] for x,z in outline]
    faces = [tuple(reversed(range(count))), tuple(range(count,2*count))]
    faces += [(i, (i+1)%count, (i+1)%count+count, i+count) for i in range(count)]
    mesh = bpy.data.meshes.new("Original gate key")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    visual = bpy.data.objects.new("Machined key surface", mesh)
    bpy.context.collection.objects.link(visual)
    visual.parent = prop
    visual["fraynixVisual"] = "triangle-surface"


def create(output, case, reachable, gap, gate_x, center, prop_z, yaw, visual_only=False):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    title = f"Gate {case} · {'Turn fits' if reachable else 'Narrow stop'}"
    body("Support deck", "deck", (0,0,-.15), (7.6,6,.3))
    width = 2.4
    for direction, side in [(1,"far"),(-1,"near")]:
        body(f"{side.capitalize()} jamb", f"{side}-jamb",
             (gate_x, -(center+direction*(gap+width)/2), .55), (.32,width,1.10))
    prop = body("Gate key", "key", (-1.65,-prop_z,.12), (.42,.96,.24), 1.2)
    prop.rotation_euler.z = math.radians(yaw)
    attach_key_surface(prop)
    info = bpy.data.objects.new("Fraynix scene metadata", None)
    bpy.context.collection.objects.link(info)
    info["fraynixScene"] = json.dumps({
        "title": title, "gravityMps2": 9.81,
        "source": {"kind": "synthetic", "physicalProperties": "declared",
                   "note": f"Original Blender-authored gate {case}, declared {gap:.3f} m interior opening."},
        "scaleReference": {"a": [0,0,0], "b": [1,0,0], "knownLengthM": 1,
                           "toleranceM": .00001, "basis": "declared-test"}
    })
    suffix = "visual" if visual_only else "turn" if reachable else "blocked"
    bpy.ops.export_scene.gltf(filepath=str(output / f"gate-{case}-{suffix}.glb"),
                              export_format="GLB", export_extras=True)


if __name__ == "__main__":
    destination = Path(sys.argv[sys.argv.index("--")+1])
    destination.mkdir(parents=True, exist_ok=True)
    cases = [
        ("a", .68, .34, 0, 0, 0, 0),
        ("b", .73, .35, -.25, .18, .198, 4),
        ("c", .69, .32, .23, -.16, -.18, -4)
    ]
    for case, open_gap, closed_gap, gate_x, center, prop_z, yaw in cases:
        create(destination, case, True, open_gap, gate_x, center, prop_z, yaw)
        create(destination, case, False, closed_gap, gate_x, center, prop_z, yaw)
    create(destination, "a", False, .46, 0, 0, 0, 0, visual_only=True)
