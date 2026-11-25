"""Verify that the exported video and sound can be placed as editable strips in Blender."""
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
for index, name in enumerate(("shallow","steep")):
    directory = root/name
    receipt = json.loads((directory/"receipt.json").read_text(encoding="utf8"))
    start = 1 + 50*index
    movie_file, wave_file = directory/"motion.avi", directory/"motion.wav"
    movie = editor.strips.new_movie(name+" video",str(movie_file),1,start)
    audio = editor.strips.new_sound(name+" audio",str(wave_file),2,start)
    clip = bpy.data.movieclips.load(str(movie_file))
    assert movie.frame_final_start == audio.frame_final_start == start
    assert movie.frame_final_duration == audio.frame_final_duration == receipt["frames"], (
        name,movie.frame_final_duration,audio.frame_final_duration,receipt["frames"])
    assert list(clip.size) == [receipt["width"],receipt["height"]]
    results.append({"name":name,"startFrame":start,"videoFrames":int(movie.frame_final_duration),
                    "audioFrames":int(audio.frame_final_duration),"imagePixels":list(clip.size)})
(root/"blender-timeline.json").write_text(json.dumps({
    "blenderVersion":bpy.app.version_string,"timelineFps":24,"results":results
},indent=2)+"\n",encoding="utf8")
