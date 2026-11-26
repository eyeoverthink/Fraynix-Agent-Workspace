"""Check the round-trip sample in Blender without loading any Fraynix modules."""

import json
import sys
from pathlib import Path

import bpy
from mathutils import Vector


def expected_wall(x, z):
    return (6 <= x < 74 and 4 <= z < 54
            and (x < 8 or x >= 72 or z < 6 or z >= 52
                 or 38 <= x < 40 and (z < 24 or z >= 34)))


def main(folder):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(folder / "two-room-generated.glb"))
    scene = bpy.context.scene
    graph = bpy.context.evaluated_depsgraph_get()
    for z in range(60):
        for x in range(80):
            # glTF Y-up maps to Blender Z-up, and glTF +Z maps to Blender -Y.
            origin = Vector(((x + .5) / 10, -(z + .5) / 10, 4))
            hit, location, *_ = scene.ray_cast(graph, origin, Vector((0, 0, -1)), distance=5)
            expected_height = 2.6 if expected_wall(x, z) else 0
            assert hit and abs(location.z - expected_height) < .00002, (x, z, location.z, expected_height)
    report = {"application": "Blender " + bpy.app.version_string, "independentRays": 4800,
              "wallHeightM": 2.6, "matchingWallAndFloorCells": 4800, "passed": True}
    (folder / "blender-readback.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report))


if __name__ == "__main__":
    main(Path(sys.argv[sys.argv.index("--") + 1]).resolve())
