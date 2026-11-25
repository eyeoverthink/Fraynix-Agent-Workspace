"""Render varied depth orders and add labelled, deterministic mask noise."""
import json
import math
from pathlib import Path
import random
import runpy
import struct
import sys
import zlib

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
width_m, cols, rows = 4, 600, 450
scene.render.resolution_x, scene.render.resolution_y = cols, rows
camera.data.ortho_scale = width_m
top = ((0, 0, 1), (1, 0, 0))
extra = {"headingDeg": 4, "elevationDeg": 55}
fork = [(-.25,-.20), (.25,-.20), (.25,.00), (.10,.00), (.10,.20), (-.10,.20), (-.10,.00), (-.25,.00)]
models = {name: (outline,height) for name,outline,height,_,_ in source["specs"]}
models["fork"] = (fork,.20)

def camera_frame(heading,elevation):
    azimuth,height = map(math.radians,(heading,elevation))
    return ((math.sin(azimuth)*math.cos(height),-math.cos(azimuth)*math.cos(height),math.sin(height)),
            (math.cos(azimuth),math.sin(azimuth),0))

def render(directory,name,frame):
    source["render"](scene,camera,directory/(name+".png"),*frame)

def orient(body,angles):
    yaw,pitch,roll = map(math.radians,angles)
    q = Quaternion((0,1,0),yaw) @ Quaternion((1,0,0),pitch) @ Quaternion((0,0,1),roll)
    body.rotation_mode = "QUATERNION"
    body.rotation_quaternion = (basis @ q.to_matrix() @ basis.inverted()).to_quaternion()
    return [q.x,q.y,q.z,q.w]

def read_mask(file):
    image = bpy.data.images.load(str(file),check_existing=False)
    rgba = list(image.pixels)
    bpy.data.images.remove(image)
    bg = rgba[:3]
    return {i//4 for i in range(0,len(rgba),4)
            if max(abs(rgba[i+c]-bg[c]) for c in range(3))>.25}

def png_chunk(kind,data):
    return struct.pack(">I",len(data))+kind+data+struct.pack(">I",zlib.crc32(kind+data))

def write_mask(file,mask):
    pixels = bytearray([28])*(cols*rows)
    for pixel in mask:
        pixels[pixel] = 230
    scanlines = b"".join(b"\0"+pixels[y*cols:(y+1)*cols] for y in range(rows-1,-1,-1))
    header = struct.pack(">2I5B",cols,rows,8,0,0,0,0)
    file.write_bytes(b"\x89PNG\r\n\x1a\n"+png_chunk(b"IHDR",header)
                     +png_chunk(b"IDAT",zlib.compress(scanlines))+png_chunk(b"IEND",b""))

def add_noise(mask,seed,salt_fraction,pepper_fraction):
    rng = random.Random(seed)
    neighbors = (-cols-1,-cols,-cols+1,-1,1,cols-1,cols,cols+1)
    interior = (y*cols+x for y in range(3,rows-3) for x in range(3,cols-3))
    candidates = [[],[]]
    for pixel in interior:
        inside = pixel in mask
        if all((pixel+delta in mask)==inside for delta in neighbors):
            candidates[int(inside)].append(pixel)
    changed = set()
    counts = []
    for points,fraction in zip(candidates,(salt_fraction,pepper_fraction)):
        rng.shuffle(points)
        amount = math.floor(len(mask)*fraction)
        selected = set()
        for pixel in points:
            if any(pixel+delta in changed for delta in neighbors):
                continue
            selected.add(pixel)
            changed.add(pixel)
            if len(selected)==amount:
                break
        assert len(selected)==amount
        counts.append(amount)
    return mask ^ changed, {"injectedSpecks":counts[0],"injectedHoles":counts[1]}

def foreground_y(front,rear,heading,elevation,screen_offset):
    azimuth,height = map(math.radians,(heading,elevation))
    horizontal_depth = math.sin(azimuth)*(front[0]-rear[0])+math.cos(azimuth)*(front[2]-rear[2])
    return rear[1]+(math.sin(height)*horizontal_depth-screen_offset)/math.cos(height)

def make_case(index,name,order,side,initial,rotations):
    folder = out/name
    folder.mkdir(exist_ok=True)
    heading,elevation = side
    fitting = camera_frame(extra["headingDeg"],extra["elevationDeg"])
    occluded = camera_frame(heading,elevation)
    witness_heading,witness_elevation = (75+index,38+index)
    witness = camera_frame(witness_heading,witness_elevation)
    front_xyz, rear_xyz, clear_xyz = initial
    front_y = foreground_y(front_xyz,rear_xyz,heading,elevation,.08+index*.008)
    poses = [(front_xyz[0],front_y,front_xyz[2]),rear_xyz,clear_xyz]
    objects,samples = [],[]
    for model,position,angles in zip(order,poses,rotations):
        outline,height = models[model]
        body = source["prism"](model,outline,height)
        body.location = basis @ Vector(position)
        quaternion = orient(body,angles)
        objects.append(body)
        samples.append({"model":model,"positionM":position,"anglesDeg":angles,"quaternion":quaternion})
    eye = Vector(occluded[0])
    assert (objects[0].location-objects[1].location).dot(eye)>.65
    for label,frame in [("overhead",top),("occluded-clean",occluded),("extra-fitting",fitting),("held-out",witness)]:
        render(folder,label,frame)

    masks = []
    for i,body in enumerate(objects):
        for other in objects:
            other.hide_render = other!=body
        render(folder,"isolated",occluded)
        masks.append(read_mask(folder/"isolated.png"))
        (folder/"isolated.png").unlink()
    for body in objects:
        body.hide_render = False
    shared = masks[0] & masks[1]
    assert .15*min(len(masks[0]),len(masks[1]))<len(shared)<.60*min(len(masks[0]),len(masks[1])), (name,len(shared),[len(m) for m in masks])
    assert not ((masks[0] | masks[1]) & masks[2]), name
    mask = read_mask(folder/"occluded-clean.png")
    noisy,counts = add_noise(mask,319+index,.062+index*.001,.020)
    write_mask(folder/"occluded-noisy.png",noisy)
    if index==0:
        heavy,_ = add_noise(mask,722,.095,.035)
        write_mask(folder/"excess-noise.png",heavy)
        witness_mask = read_mask(folder/"held-out.png")
        pixel = 50*cols+45
        assert not any(i in witness_mask for i in range(pixel-2,pixel+3))
        write_mask(folder/"held-out-isolated-speck.png",witness_mask | {pixel})
        objects[1].hide_render = True
        render(folder,"missing-rear",occluded)
        objects[1].hide_render = False
    objects[1].location.x += .14
    render(folder,"wrong-held-out",witness)
    for body in objects:
        bpy.data.objects.remove(body,do_unlink=True)
    return {"id":name,"order":order,"samples":samples,
            "cameraAngles":{"sideHeadingDeg":heading,"sideElevationDeg":elevation,
                            "witnessHeadingDeg":witness_heading,"witnessElevationDeg":witness_elevation},
            "additionalCamera":extra,"rearHiddenFraction":len(shared)/len(masks[1]),
            "overlapPixels":len(shared),"cleanForegroundPixels":len(mask),
            "noise":counts,"heldOutTranslationControlM":.14}

cases = [
    ("step-behind-elbow",("elbow","step","fork"),(-45,30),
     ((-.475,0,.628),(.321,.443,-.391),(.887,.409,.664)),
     ((-32,-13,19),(68,15,-21),(120,-9,15))),
    ("elbow-behind-step",("step","elbow","fork"),(-46,31),
     ((-.493,0,.629),(.319,.430,-.389),(.868,.395,.681)),
     ((68,15,-20),(-32,-12,19),(122,-10,15))),
    ("step-behind-fork",("fork","step","elbow"),(-43,32),
     ((-.475,0,.636),(.340,.431,-.375),(.878,.402,.657)),
     ((122,-11,16),(69,14,-21),(-32,-13,19))),
    ("fork-behind-elbow",("elbow","fork","step"),(-47,29),
     ((-.468,0,.646),(.325,.421,-.369),(.886,.420,.671)),
     ((-33,-14,19),(120,-10,15),(68,16,-21))),
]
report = {"blenderVersion":bpy.app.version_string,"widthM":width_m,"imagePixels":[cols,rows],
          "cases":[make_case(i,*case) for i,case in enumerate(cases)]}
(out/"truth.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf8")
