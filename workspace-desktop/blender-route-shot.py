"""Bake a verified route shot as editable geometry, cameras, labels and timing in Blender."""
import json
import math
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector

TO_BLENDER = runpy.run_path(str(Path(__file__).with_name("blender-exchange.py")))["TO_BLENDER"]


def vec(value):
    return TO_BLENDER @ Vector(value)


def material(name, rgba):
    item = bpy.data.materials.new(name)
    item.diffuse_color = rgba
    return item


def color(hex_value):
    return tuple(int(hex_value[i:i+2], 16)/255 for i in (1,3,5)) + (1,)


def mesh_object(scene, name, vertices, faces):
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    scene.collection.objects.link(obj)
    return obj


def geometry(scene, rows, crate, frames):
    objects = {}
    faces = [(0,2,6,4),(1,5,7,3),(0,4,5,1),(2,3,7,6),(0,1,3,2),(4,6,7,5)]
    for row in rows:
        key = row["sourceId"]
        sx, sy, sz = (axis/2 for axis in row["size"])
        vertices = [vec((sx if i&1 else -sx, sy if i&2 else -sy, sz if i&4 else -sz)) for i in range(8)]
        obj = mesh_object(scene,row["label"],vertices,faces)
        objects[key] = obj
        obj["fraynix_box_id"] = key
        obj.location = vec(row["position"])
        x,y,z,w = row["rotation"]
        obj.rotation_mode = "QUATERNION"
        obj.rotation_quaternion = (TO_BLENDER @ Quaternion((w,x,y,z)).to_matrix() @ TO_BLENDER.transposed()).to_quaternion()
        hex_value = "#e7a44e" if key==crate else "#d97263" if key=="spare" else "#7886a0"
        if key=="floor":
            hex_value="#40566b"
        obj.data.materials.append(material(row["label"],color(hex_value)))
        if key==crate:
            for frame in frames:
                obj.location=vec(frame["cratePositionM"])
                obj.keyframe_insert(data_path="location",frame=frame["frame"]+1,group="Verified crate position")
    return objects


def animate_camera(scene, name, frames):
    lens = bpy.data.cameras.new(name)
    lens.sensor_fit="VERTICAL"
    lens.sensor_height=36
    lens.clip_start=.05
    camera=bpy.data.objects.new(name,lens)
    scene.collection.objects.link(camera)
    scene.camera=camera
    camera.rotation_mode="QUATERNION"
    last=None
    for frame in frames:
        at=frame["frame"]+1
        camera.location=vec(frame["cameraPositionM"])
        basis=Matrix((vec(frame["cameraRightM"]),vec(frame["cameraUpM"]),-vec(frame["cameraForwardM"]))).transposed()
        rotation=basis.to_quaternion()
        if last is not None and last.dot(rotation)<0:
            rotation.negate()
        last=rotation.copy()
        camera.rotation_quaternion=rotation
        lens.lens=lens.sensor_height/(2*math.tan(math.radians(frame["cameraFovDeg"])/2))
        for key in ("location","rotation_quaternion"):
            camera.keyframe_insert(data_path=key, frame=at, group="Verified camera")
        lens.keyframe_insert(data_path="lens",frame=at,group="Vertical field of view")
    return camera


def route_line(scene, definition, floor):
    style=definition["overlays"]["routeLine"]
    if not style["enabled"]:
        return
    floor += style["elevationM"]
    vertices,faces=[],[]
    for a,b in zip(definition["pathM"],definition["pathM"][1:]):
        start,end=Vector((a[0],floor,a[2])),Vector((b[0],floor,b[2]))
        delta=end-start
        if delta.length<1e-8:
            continue
        edge=Vector((-delta.z,0,delta.x)).normalized()*style["widthM"]/2
        index=len(vertices)
        vertices.extend(vec(corner) for corner in (start+edge,end+edge,end-edge,start-edge))
        faces.append(tuple(range(index,index+4)))
    line=mesh_object(scene,"Floor route",vertices,faces)
    line.data.materials.append(material("Route guide",color(style["color"])))


def visibility(obj, cue, end):
    start,stop=cue["firstFrame"]+1,cue["endExclusive"]+1
    for at in sorted({1,start,stop,end+1}):
        hidden=not cue["style"]["enabled"] or not start<=at<stop
        for field in ("hide_render","hide_viewport"):
            setattr(obj,field,hidden)
            obj.keyframe_insert(data_path=field,frame=at,group="Label timing")


def label(scene, cue, objects, camera, end):
    key,style=cue["id"],cue["style"]
    width,height=style["widthM"],style["heightM"]
    anchor=bpy.data.objects.new("Anchor · "+key,None)
    scene.collection.objects.link(anchor)
    anchor.parent=objects[cue["target"]]
    anchor.location=vec(style["localOffsetM"])
    anchor["fraynix_label_anchor"]=key
    anchor["fraynix_target"]=cue["target"]

    x,y=width/2,height/2
    inset=min(width,height)*.058
    vertices=[(-x,-y,0),(x,-y,0),(x,y,0),(-x,y,0),
              (-x+inset,-y+inset,0),(x-inset,-y+inset,0),(x-inset,y-inset,0),(-x+inset,y-inset,0)]
    plate=mesh_object(scene,"Label · "+key,vertices,[(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7),(4,5,6,7)])
    plate.parent=anchor
    plate["fraynix_label_plate"]=key
    rotation=plate.constraints.new("COPY_ROTATION")
    rotation.target=camera
    rotation.owner_space="WORLD"
    rotation.target_space="WORLD"
    accent=material("Label ink · "+key,color(style["color"]))
    plate.data.materials.append(accent)
    plate.data.materials.append(material("Label background",(0.017,0.027,0.055,1)))
    plate.data.polygons[4].material_index=1

    curve=bpy.data.curves.new("Editable text · "+key,"FONT")
    curve.body=style["text"]
    curve.align_x="CENTER"
    curve.align_y="CENTER"
    curve.size=min(height*.65,width/(max(3,len(curve.body))*.72))
    curve.materials.append(accent)
    text=bpy.data.objects.new("Text · "+key,curve)
    scene.collection.objects.link(text)
    text.parent=plate
    text.location.z=.002
    text["fraynix_label_text"]=key
    visibility(plate,cue,end)
    visibility(text,cue,end)


def bake(data, output):
    if data["format"]!="fraynix.blender-route-shot-v1":
        raise ValueError("Choose verified timed route-shot data.")
    shot=data["shot"]
    definition=shot["definition"]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    first=bpy.context.scene
    for role,frames in (("Primary",shot["audit"]["frames"]),("Reserved",data["reserved"]["frames"])):
        scene=first if role=="Primary" else bpy.data.scenes.new("Reserved camera")
        scene.name="Primary shot" if role=="Primary" else "Reserved camera"
        scene["fraynix_shot_id"]=shot["id"]
        scene["fraynix_route_evidence_sha256"]=definition["routeEvidenceSha256"]
        scene["fraynix_angle_role"]=role.lower()
        scene.frame_start=1
        scene.frame_end=definition["frameCount"]
        scene.render.fps=definition["fps"]
        scene.render.fps_base=1
        scene.render.resolution_x=definition["width"]
        scene.render.resolution_y=definition["height"]
        scene.render.resolution_percentage=100
        scene.render.engine="BLENDER_WORKBENCH"
        shading=scene.display.shading
        shading.light="STUDIO"
        shading.color_type="MATERIAL"
        shading.show_shadows=True
        shading.show_object_outline=False
        shading.background_type="WORLD"
        if scene.world is None:
            scene.world=bpy.data.worlds.new(role+" world")
        scene.world.color=(.045,.065,.095)
        objects=geometry(scene,data["sceneObjects"],definition["crate"],frames)
        camera=animate_camera(scene,role+" camera",frames)
        route_line(scene,definition,data["floorY"])
        for cue in definition["overlays"]["labels"]:
            label(scene,cue,objects,camera,scene.frame_end)
        scene.frame_set(1)
    bpy.context.window.scene=first
    first.frame_set(1+data["reserved"]["witnessFrame"] if data["reserved"]["witnessFrame"]>=0 else 1)
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(output),compress=True)


if __name__=="__main__":
    mode,source,output=sys.argv[sys.argv.index("--")+1:]
    if mode!="export-shot":
        raise ValueError("Expected route-shot export.")
    bake(json.loads(Path(source).read_text(encoding="utf-8")),Path(output))
