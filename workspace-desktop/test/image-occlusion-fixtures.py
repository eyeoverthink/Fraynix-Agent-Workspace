"""Render a partial side occlusion, an extra fitting angle and independent controls."""
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
top = ((0, 0, 1), (1, 0, 0))

def camera_frame(heading, elevation):
    angle, height = map(math.radians, (heading, elevation))
    return ((math.sin(angle)*math.cos(height), -math.cos(angle)*math.cos(height), math.sin(height)),
            (math.cos(angle), math.sin(angle), 0))

def render(file, frame):
    source["render"](scene, camera, file, *frame)

def orient(body, angles):
    yaw, pitch, roll = map(math.radians, angles)
    q = Quaternion((0, 1, 0), yaw) @ Quaternion((1, 0, 0), pitch) @ Quaternion((0, 0, 1), roll)
    body.rotation_mode = "QUATERNION"
    body.rotation_quaternion = (basis @ q.to_matrix() @ basis.inverted()).to_quaternion()
    return [q.x, q.y, q.z, q.w]

def mask(file):
    img = bpy.data.images.load(str(file), check_existing=False)
    rgba = list(img.pixels)
    bpy.data.images.remove(img)
    bg = rgba[:3]
    return {i//4 for i in range(0,len(rgba),4) if max(abs(rgba[i+c]-bg[c]) for c in range(3)) > .25}

width, pixels = 4, [600,450]
scene.render.resolution_x, scene.render.resolution_y = pixels
camera.data.ortho_scale = width
known = {"sideHeadingDeg": -45, "sideElevationDeg": 30, "witnessHeadingDeg": 75, "witnessElevationDeg": 38}
extra = {"headingDeg": 4, "elevationDeg": 55}
occluded = camera_frame(known["sideHeadingDeg"],known["sideElevationDeg"])
addition = camera_frame(extra["headingDeg"],extra["elevationDeg"])
witness = camera_frame(known["witnessHeadingDeg"],known["witnessElevationDeg"])
rows = [((-.487,1.081,.613),(-31,-13,19)), ((.313,.443,-.407),(67,15,-21)), ((.874,.408,.659),(119,-9,15))]
fork = [(-.25,-.20), (.25,-.20), (.25,.00), (.10,.00), (.10,.20), (-.10,.20), (-.10,.00), (-.25,.00)]
models = [(name,outline,height) for name,outline,height,_,_ in source["specs"]] + [("fork",fork,.20)]
props, samples = [], []
for (name, outline, height), (center, rotation) in zip(models,rows):
    body = source["prism"](name,outline,height)
    body.location = basis @ Vector(center)
    quaternion = orient(body,rotation)
    props.append(body)
    samples.append({"model":name,"positionM":center,"anglesDeg":rotation,"quaternion":quaternion})
for name, frame in [("overhead",top),("occluded-side",occluded),("extra-fitting",addition),("held-out",witness)]:
    render(out / (name + ".png"), frame)

# Isolate each authored mesh in turn to independently quantify the projected overlap.
masks = []
for i, body in enumerate(props):
    for candidate in props:
        candidate.hide_render = candidate != body
    file = out / f"isolated-{i}.png"
    render(file,occluded)
    masks.append(mask(file))
    file.unlink()
for body in props:
    body.hide_render = False
observed = mask(out/"occluded-side.png")
shared = masks[0] & masks[1]
assert len(shared) > .12*min(len(masks[0]),len(masks[1])), "The two silhouettes did not overlap sufficiently"
assert len(shared) < .60*min(len(masks[0]),len(masks[1])), "The fixture must depict partial visibility"
assert not ((masks[0] | masks[1]) & masks[2]), "The third prop must remain clear"
assert len(observed & shared) > .97*len(shared)
visibility = {
    "frontProp": "elbow", "partiallyOccludedProp": "step", "overlapPixels":len(shared),
    "rearHiddenFraction":len(shared)/len(masks[1]),
    "isolatedSilhouettePixels":[len(m) for m in masks]
}
props[1].hide_render = True
render(out / "missing-rear.png", occluded)
props[1].hide_render = False
props[1].location.x += .14
render(out / "wrong-held-out.png", witness)

truth = {"blenderVersion":bpy.app.version_string,"widthM":width,"imagePixels":pixels,
         "cameraAngles":known,"additionalCamera":extra,"samples":samples,
         "visibility":visibility,"heldOutTranslationControlM":.14}
(out/"truth.json").write_text(json.dumps(truth,indent=2)+"\n",encoding="utf8")
