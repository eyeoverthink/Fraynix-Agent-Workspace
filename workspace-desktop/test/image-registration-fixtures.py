"""Render two unmarked registered scene images and one displaced second-view control."""
import json
import math
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Vector

root = Path(__file__).resolve().parent
bracket_tools = runpy.run_path(str(root / "browser-views-blender.py"))
plate_tools = runpy.run_path(str(root / "browser-top-blender.py"))
out = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
out.mkdir(parents=True, exist_ok=True)
floor_positions = [(-.83, -.53), (.74, .91)]
scene, camera = bracket_tools["setup"]()
scene.render.resolution_x = 400
scene.render.resolution_y = 300
scene.render.film_transparent = False
scene.render.image_settings.color_mode = "RGB"
scene.display.shading.light = "FLAT"
scene.display.shading.background_type = "WORLD"
scene.world = bpy.data.worlds.new("Plain opaque background")
scene.world.color = (.012, .012, .012)
camera.data.ortho_scale = 4
bracket = bracket_tools["solid"]()
plate = plate_tools["plate"]()
for obj, (x, z), height in zip((bracket, plate), floor_positions, (.4, .1)):
    obj.location += Vector((x, -z, height / 2))
    obj.color = (.8, .8, .8, 1)


def render(name, eye, right):
    eye, right = Vector(eye).normalized(), Vector(right).normalized()
    camera.location = eye * 6
    camera.rotation_euler = Matrix((right, eye.cross(right), eye)).transposed().to_euler()
    scene.render.filepath = str(out / name)
    bpy.ops.render.render(write_still=True)


d = math.sqrt(.5)
elevated = (-.5*d, -.5*d, math.sqrt(.75))
across = (d, -d, 0)
render("overhead.png", (0, 0, 1), (1, 0, 0))
render("second-angle.png", elevated, across)
plate.location += Vector((.16, .04, 0))
render("shifted-second-angle.png", elevated, across)
(out / "truth.json").write_text(json.dumps({
    "blenderVersion": bpy.app.version_string,
    "imagePixels": [400, 300],
    "widthM": 4,
    "propsXZ": floor_positions,
    "shiftInWrongViewXZ": [.16, -.04],
    "eastRightInOverhead": True,
    "secondCamera": {"azimuthFromFrontDeg": 45, "elevationDeg": 60},
}, indent=2) + "\n", encoding="utf8")
