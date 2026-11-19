"""Edit a native Game Lab export in Blender using the same transforms as Object Mode."""
from pathlib import Path
import sys

import bpy


folder = Path(sys.argv[sys.argv.index("--") + 1])
scene = bpy.context.scene
ground = next(obj for obj in scene.objects if obj.name == "Garden deck 17/0")
player = next(obj for obj in scene.objects if obj.name == "Relay robot")
bpy.context.preferences.filepaths.save_version = 0


def save(name):
    bpy.ops.wm.save_as_mainfile(filepath=str(folder / name), compress=True)


ground.location.x += .25
ground.location.z += .25
save("edited-course.blend")
ground.location.x += .1
save("off-grid-course.blend")
ground.location.x -= .1
player.location.x += .25
save("moved-player.blend")

player.location.x -= .25
ground.location.x, ground.location.z = 4.375, .125
for column in (14, 15, 16, 17, 20, 21, 22, 23):
    for row in (0, 1):
        tile = scene.objects[f"Garden deck {column}/{row}"]
        tile.location.z += 1.75
save("wide-gap-course.blend")
