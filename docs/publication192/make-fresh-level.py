"""Restore two boundary columns in the published wide-gap Blender fixture."""
from pathlib import Path
import sys

import bpy


target = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
for column in (22, 23):
    for row in (0, 1):
        bpy.context.scene.objects[f"Garden deck {column}/{row}"].location.z -= 1.75
bpy.context.preferences.filepaths.save_version = 0
bpy.ops.wm.save_as_mainfile(filepath=str(target), compress=True)
