"""Open both independently edited exports as synchronized Blender sequencer strips."""
import json
from pathlib import Path
import sys

import bpy

root = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
scene = bpy.context.scene
scene.render.fps = 24
scene.render.fps_base = 1
editor = scene.sequence_editor_create()
results = []
for index, version in enumerate(("original","revised")):
    directory = root/version
    proof = json.loads((directory/"receipt.json").read_text(encoding="utf8"))
    start = 1 + index*30
    video = editor.strips.new_movie(version+" picture",str(directory/"edit.avi"),1,start)
    audio = editor.strips.new_sound(version+" audio",str(directory/"edit.wav"),2,start)
    assert video.frame_final_start == audio.frame_final_start == start
    frames = proof["metrics"]["frames"]
    assert video.frame_final_duration == audio.frame_final_duration == frames
    results.append({"version":version,"start":start,"videoFrames":frames,"audioFrames":frames})
(root/"blender-edit.json").write_text(json.dumps({
    "blenderVersion":bpy.app.version_string,"fps":24,"results":results
},indent=2)+"\n",encoding="utf8")
