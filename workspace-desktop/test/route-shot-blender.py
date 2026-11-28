"""Check an exported recipient shot from a fresh Blender process using evaluated scene geometry."""
import hashlib
import json
import math
from pathlib import Path
import sys

import bpy
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector
from mathutils.bvhtree import BVHTree

source, output = (Path(arg).resolve() for arg in sys.argv[sys.argv.index("--")+1:])
data = json.loads(source.read_text(encoding="utf-8"))
shot = data["shot"]
definition = shot["definition"]
cues = definition["overlays"]["labels"]
width, height = definition["width"], definition["height"]


def vector(value):
    x, y, z = value
    return Vector((x, -z, y))


def scene_boxes(boxes):
    vertices, faces, ids = [], [], []
    for obj in boxes:
        start = len(vertices)
        vertices.extend(obj.matrix_world @ vert.co for vert in obj.data.vertices)
        faces.extend(tuple(index+start for index in face.vertices) for face in obj.data.polygons)
        ids.extend([obj["fraynix_box_id"]] * len(obj.data.polygons))
    return BVHTree.FromPolygons(vertices, faces), ids


def indexed(scene, key):
    return {obj[key]:obj for obj in scene.objects if key in obj}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def check_scene(scene, role, frames):
    bpy.context.window.scene = scene
    require(scene["fraynix_shot_id"] == shot["id"], "Blender opened a different shot.")
    require(scene["fraynix_route_evidence_sha256"] == definition["routeEvidenceSha256"], "The route binding changed.")
    require(scene.render.fps == 24 and scene.render.fps_base == 1 and scene.frame_end == 144, "The Blender clock changed.")
    require((scene.render.resolution_x,scene.render.resolution_y)==(width,height), "The Blender frame geometry changed.")
    anchors, plates, text = (indexed(scene, key) for key in
        ("fraynix_label_anchor","fraynix_label_plate","fraynix_label_text"))
    boxes = indexed(scene,"fraynix_box_id")
    require(set(boxes)=={box["sourceId"] for box in data["sceneObjects"]}, "Blender box identities changed.")
    want = {cue["id"] for cue in cues}
    require(set(anchors)==set(plates)==set(text)==want, "The Blender label tracks changed.")
    for cue in cues:
        key=cue["id"]
        require(text[key].type=="FONT" and text[key].data.body==cue["style"]["text"], "The label is missing editable text.")
        require(anchors[key].parent==boxes[cue["target"]], "The Blender label lost its object attachment.")
        require((anchors[key].location-vector(cue["style"]["localOffsetM"])).length<1e-6, "The Blender label's local offset changed.")
        require(plates[key].animation_data.action is not None, "The Blender label has no editable timing keys.")

    counters={cue["id"]:{"activeFrames":0,"rayChecks":0,"blocked":0,"offFrame":0} for cue in cues}
    max_gap=0
    for frame in frames:
        at=frame["frame"]
        scene.frame_set(at+1)
        graph=bpy.context.evaluated_depsgraph_get()
        camera=scene.camera.evaluated_get(graph)
        body=boxes[definition["crate"]].evaluated_get(graph)
        require((body.matrix_world.translation-vector(frame["cratePositionM"])).length<4e-6, "Crate playback moved off the route.")
        matrix=camera.matrix_world
        eye=matrix.translation
        forward=matrix.to_quaternion() @ Vector((0,0,-1))
        require((eye-vector(frame["cameraPositionM"])).length<5e-6 and (forward-vector(frame["cameraForwardM"])).length<3e-6,
            "The camera bake diverged.")
        tree, owners = scene_boxes([box.evaluated_get(graph) for box in boxes.values()])
        for cue,row in zip(cues,frame["labels"]):
            key=cue["id"]
            active=cue["style"]["enabled"] and cue["firstFrame"]<=at<cue["endExclusive"]
            plate=plates[key].evaluated_get(graph)
            title=text[key].evaluated_get(graph)
            require(row["id"]==key and row["active"]==active and plate.hide_render==title.hide_render== (not active),
                f"The reopened {role} label {key} disagrees at its timing boundary {at}.")
            anchor=anchors[key].evaluated_get(graph).matrix_world.translation
            require((anchor-vector(row["anchorM"])).length<5e-6,"The moving label anchor drifted.")
            if not active:
                continue
            counters[key]["activeFrames"]+=1
            facing=plate.matrix_world.to_quaternion() @ Vector((0,0,-1))
            require((forward-facing).length<4e-6, "A label plate failed to face the camera.")
            projected=world_to_camera_view(scene,camera,anchor)
            px=(projected.x*width, (1-projected.y)*height)
            in_frame=projected.z>=camera.data.clip_start and 0<=px[0]<width and 0<=px[1]<height
            require(row["inFrame"]==in_frame, f"View bounds disagree for {role} frame {at} label {key}.")
            if not in_frame:
                counters[key]["offFrame"]+=1
                continue
            x,y=row["samplePx"]
            gap=max(abs(x+.5-px[0]),abs(y+.5-px[1]))
            max_gap=max(max_gap,gap)
            require(gap<.502,"The held camera projection exceeds half a pixel from the recorded sample.")
            focal=height*camera.data.lens/camera.data.sensor_height
            ray=matrix.to_3x3() @ Vector(((x+.5-width/2)/focal,(height/2-y-.5)/focal,-1))
            plane=(anchor-eye).dot(forward)
            hit, _, face, distance=tree.ray_cast(eye,ray,plane*ray.length)
            blocker=owners[face] if hit is not None and distance>=camera.data.clip_start else ""
            require(blocker==row["sceneOccluder"],
                f"Blender ray disagreed for {role} frame {at} label {key}: {blocker!r} vs {row['sceneOccluder']!r}.")
            counters[key]["rayChecks"]+=1
            counters[key]["blocked"]+=bool(blocker)
    return {"role":role,"frames":len(frames),"maximumSampleCenterDeltaPx":max_gap,"labels":counters}


def render_sample(scene, at, name):
    bpy.context.window.scene=scene
    scene.frame_set(at+1)
    scene.render.filepath=str(output/name)
    scene.render.image_settings.file_format="PNG"
    bpy.ops.render.render(write_still=True)
    path=output/name
    require(path.is_file(),"Blender did not produce the render.")
    return {"frame":at,"file":name,"sha256":hashlib.sha256(path.read_bytes()).hexdigest()}


scenes={scene.get("fraynix_angle_role"):scene for scene in bpy.data.scenes}
require(set(scenes)=={"primary","reserved"},"The Blender export needs two editable scenes.")
primary=shot["audit"]["frames"]
reserved=data["reserved"]["frames"]
results=[check_scene(scenes["primary"],"primary",primary),check_scene(scenes["reserved"],"reserved",reserved)]
witness=data["reserved"]["witnessFrame"]
require(witness>=0 and primary[witness]["labels"][0]["fullyHiddenByShelf"] and
        reserved[witness]["labels"][0]["centerVisible"],"The unseen shelf case was not recovered by the reserved camera.")
samples=[render_sample(scenes["primary"],witness,"blender-primary-occluded.png"),
         render_sample(scenes["reserved"],witness,"blender-reserved-reveal.png")]
report={"passed":True,"version":bpy.app.version_string,"reopenedBlend":True,"shotId":shot["id"],
        "minimumAngleDeg":data["reserved"]["minimumAngleDeg"],"witnessFrame":witness,"views":results,"renders":samples}
(output/"blender-proof.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
