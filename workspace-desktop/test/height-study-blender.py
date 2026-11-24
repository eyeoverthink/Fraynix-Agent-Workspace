"""Render an original pair of registered props and independent calibration targets."""
import base64
import json
from pathlib import Path
import sys
import tempfile

import bpy
from mathutils import Vector

PALETTE = [(1, .025, .025), (.025, 1, .025), (.025, .025, 1), (1, 1, .025),
           (.025, 1, 1), (1, .27, .025)]
LANDMARKS = [[-1.05,.14,-.62], [.98,.33,-.61], [-.78,.69,.52], [.82,.05,.60],
             [-1.10,.50,.1], [.56,.80,.35]]
VIEWS = {
    "overhead": ((0,0,6),(0,0,0),"initial"),
    "repeat-overhead": ((.06,.04,6),(.06,.04,0),"candidate"),
    "inclined": ((1.4,-2.2,5),(0,0,.23),"candidate"),
    "side": ((0,-5,1.22),(0,0,.32),"candidate"),
    "withheld": ((4,-3,3.5),(0,0,.30),"witness")
}


def ball(center, color, radius):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=40, ring_count=24, radius=radius, location=center)
    obj = bpy.context.object
    obj.color = (*color, 1)
    return obj


def point_in_blender(point):
    return point[0],-point[2],point[1]


def box(center, dimensions, color):
    bpy.ops.mesh.primitive_cube_add(size=1, location=center)
    obj = bpy.context.object
    obj.dimensions = dimensions
    obj.color = (*color,1)
    return obj


def render(scene, camera, temporary, key):
    eye,target,_ = VIEWS[key]
    camera.location = eye
    camera.rotation_euler = (Vector(target)-Vector(eye)).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(temporary / "frame.png")
    bpy.ops.render.render(write_still=True)
    return base64.b64encode(Path(scene.render.filepath).read_bytes()).decode("ascii")


def create_study(folder):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.render.resolution_x = 900
    scene.render.resolution_y = 650
    scene.render.resolution_percentage = 100
    scene.render.dither_intensity = 0
    scene.render.image_settings.compression = 80
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGB"
    scene.view_settings.view_transform = "Standard"
    shading = scene.display.shading
    shading.light = "FLAT"
    shading.color_type = "OBJECT"
    shading.show_shadows = False
    shading.show_cavity = False
    camera_data = bpy.data.cameras.new("Fixed orthographic calibration camera")
    camera_data.type = "ORTHO"
    camera_data.ortho_scale = 2.8
    camera = bpy.data.objects.new("Fixed orthographic calibration camera",camera_data)
    scene.collection.objects.link(camera)
    scene.camera = camera
    # Non-coplanar scale targets and two reserved fit checks stay fixed.
    for p,color in zip(LANDMARKS,PALETTE):
        ball(point_in_blender(p),color,.025)
    deck = box((0,0,-.05),(2.6,1.8,.10),(.20,.24,.28))
    folder.mkdir(parents=True,exist_ok=True)
    witness = {}
    with tempfile.TemporaryDirectory() as tmp:
        temporary = Path(tmp)
        calibrations = {key:render(scene,camera,temporary,key) for key in VIEWS}
        for label,height in (("low",.28),("tall",.62),("occluded",.28)):
            prop = box((0,0,height/2),(.26,.32,height),(.36,.48,.52))
            bpy.context.view_layer.update()
            # A separate mesh measurement is withheld from Java and used by the test only.
            world_corners = [prop.matrix_world @ Vector(corner) for corner in prop.bound_box]
            measured_height = max(p.z for p in world_corners)-max((deck.matrix_world @ Vector(c)).z for c in deck.bound_box)
            witness[label] = {"meshHeightM":measured_height, "blender":bpy.app.version_string}
            # The marker centroid is 35 mm above the registered top. Its
            # diameter is excluded from the independent mesh measurement.
            marker = ball((0,0,height+.035),(1,.025,1),.024)
            observations = {}
            for key in VIEWS:
                marker.hide_render = label == "occluded" and key == "side"
                observations[key] = render(scene,camera,temporary,key)
            bpy.data.objects.remove(marker,do_unlink=True)
            bpy.data.objects.remove(prop,do_unlink=True)
            study = {
                "format":"fraynix.metric-height-study-v1",
                "title":"Registered height experiment",
                "sourceNote":"Original Blender orthographic frames with registered color markers and declared metric dimensions.",
                "baseAnchorM":[0,0,0], "footprintM":[.26,.32], "hypothesesM":[.28,.62],
                "landmarksM":LANDMARKS,
                "views":[{"id":key,"role":VIEWS[key][2],"calibrationPng":calibrations[key],
                          "observationPng":observations[key]} for key in VIEWS]
            }
            (folder / f"{label}.json").write_text(json.dumps(study,separators=(",",":"))+"\n",encoding="utf-8",newline="
")
    (folder / "independent-mesh-measurements.json").write_text(json.dumps(witness,indent=2)+"\n",encoding="utf-8",newline="
")


if __name__ == "__main__":
    create_study(Path(sys.argv[sys.argv.index("--")+1]))
