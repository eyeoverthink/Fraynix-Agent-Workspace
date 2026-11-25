"""Render three independent prop poses and visibility/ambiguity controls in Blender."""
import json
import math
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector

source = runpy.run_path(str(Path(__file__).with_name("image-yaw-fixtures.py")))
out = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
out.mkdir(parents=True, exist_ok=True)
scene, camera = source["helper"]["camera_setup"]()
scene.render.film_transparent = False
scene.render.image_settings.color_mode = "RGB"
scene.display.shading.light = "FLAT"
scene.display.shading.background_type = "WORLD"
scene.world = bpy.data.worlds.new("Plain opaque background")
scene.world.color = (.012, .012, .012)
basis = Matrix.Rotation(math.pi / 2, 3, "X")

front = ((0, -1, 0), (1, 0, 0))
side = ((-1, 0, 0), (0, -1, 0))
top = ((0, 0, 1), (1, 0, 0))

def camera_frame(heading, elevation):
    angle, height = map(math.radians, (heading, elevation))
    return ((math.sin(angle)*math.cos(height), -math.cos(angle)*math.cos(height), math.sin(height)),
            (math.cos(angle), math.sin(angle), 0))

def render(file, frame):
    source["render"](scene, camera, file, *frame)

def pose(body, center, angles):
    yaw, pitch, roll = map(math.radians, angles)
    q = Quaternion((0, 1, 0), yaw) @ Quaternion((1, 0, 0), pitch) @ Quaternion((0, 0, 1), roll)
    body.location = basis @ Vector(center)
    body.rotation_mode = "QUATERNION"
    body.rotation_quaternion = (basis @ q.to_matrix() @ basis.inverted()).to_quaternion()
    return [q.x, q.y, q.z, q.w]

third = [(-.25,-.20), (.25,-.20), (.25,.00), (.10,.00), (.10,.20), (-.10,.20), (-.10,.00), (-.25,.00)]
symmetric = [(-.25,-.2), (.25,-.2), (.25,.2), (-.25,.2)]
for name, outline, height in [("fork",third,.20),("symmetric",symmetric,.20)]:
    model = source["prism"](name, outline, height)
    dest = out / name
    dest.mkdir(exist_ok=True)
    for view, frame in [("front",front),("side",side),("top",top),("elevated",camera_frame(-45,60))]:
        render(dest / (view + ".png"), frame)
    bpy.data.objects.remove(model, do_unlink=True)

width = 3.6
pixels = [540,405]
scene.render.resolution_x, scene.render.resolution_y = pixels
camera.data.ortho_scale = width
angles = {"sideHeadingDeg": -20, "sideElevationDeg": 48, "witnessHeadingDeg": 65, "witnessElevationDeg": 37}
fit = camera_frame(angles["sideHeadingDeg"], angles["sideElevationDeg"])
held_out = camera_frame(angles["witnessHeadingDeg"], angles["witnessElevationDeg"])
rows = [((-.917,.421,-.657),(-37,13,23)), ((.973,.406,.637),(81,-21,-7)), ((.057,.381,-.852),(127,11,-17))]
bodies, samples = [], []
models = [(name,outline,height) for name,outline,height,_,_ in source["specs"]] + [("fork",third,.20)]
for (name,outline,height),(center,rotation) in zip(models,rows):
    body = source["prism"](name,outline,height)
    quaternion = pose(body,center,rotation)
    bodies.append(body)
    samples.append({"model": name, "positionM": center, "anglesDeg": rotation, "quaternion": quaternion})

for name, frame in [("overhead",top),("fitting-side",fit),("held-out",held_out)]:
    render(out / (name + ".png"), frame)

original = bodies[2].location.copy()
bodies[2].location = bodies[0].location + Vector((.17,.03,.36))
render(out / "overhead-occluded.png", top)
bodies[2].location = original
render(out / "wrong-held-out-angle.png", camera_frame(45,30))
bpy.data.objects.remove(bodies.pop(), do_unlink=True)
block = source["prism"]("symmetric",symmetric,.20)
pose(block,rows[2][0],rows[2][1])
render(out / "overhead-ambiguous.png", top)
render(out / "fitting-side-ambiguous.png", fit)
render(out / "held-out-ambiguous.png", held_out)

truth = {
    "blenderVersion": bpy.app.version_string,
    "widthM": width,
    "imagePixels": pixels,
    "cameraAngles": angles,
    "samples": samples,
    "wrongHeldOutCamera": {"headingDeg": 45, "elevationDeg": 30},
}
(out / "truth.json").write_text(json.dumps(truth, indent=2) + "\n", encoding="utf8")
