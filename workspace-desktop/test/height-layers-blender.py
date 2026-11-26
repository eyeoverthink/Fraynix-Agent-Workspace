"""Compare six authored aperture checks with the source and rebuilt GLBs in Blender."""

import json
import sys
from pathlib import Path

import bpy
from mathutils import Vector


def inspect(glb):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(glb))
    scene = bpy.context.scene
    graph = bpy.context.evaluated_depsgraph_get()
    cases = [
        ("door", -1.25, 1.37, False),
        ("door header", -1.25, 2.33, True),
        ("window sill", 1.5, .41, True),
        ("window", 1.5, 1.27, False),
        ("window header", 1.5, 2.37, True),
        ("centre pier", 0, 1.31, True),
    ]
    rays = []
    for name, x, height, expected in cases:
        hit, location, *_ = scene.ray_cast(graph, Vector((x, -1, height)), Vector((0, 1, 0)), distance=2)
        distance = location.y + 1 if hit else None
        assert hit == expected and (not hit or abs(distance - .9) < .0001), (name, hit, distance)
        rays.append({"probe": name, "heightM": height, "hit": hit, "distanceM": distance})
    return rays


def main(folder):
    source = inspect(folder / "door-window-source.glb")
    rebuilt = inspect(folder / "door-window-rebuilt.glb")
    report = {"application": "Blender " + bpy.app.version_string, "heldOutRaysPerModel": 6,
              "source": source, "rebuilt": rebuilt, "passed": True}
    (folder / "blender-height-readback.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("Blender independently verified six held-out aperture rays on both models.")


if __name__ == "__main__":
    main(Path(sys.argv[sys.argv.index("--") + 1]).resolve())
