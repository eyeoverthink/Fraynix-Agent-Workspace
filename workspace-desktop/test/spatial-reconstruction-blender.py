"""Create independent Blender geometry, emit only calibrated input images and check a reconstructed GLB."""
import base64
import json
import math
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree

GRID = 16
STEP = .05
CAMERAS = {
    "x": ((1,0,0),(0,1,0),(0,0,1)),
    "y": ((0,0,1),(1,0,0),(0,1,0)),
    "z": ((0,-1,0),(1,0,0),(0,0,1)),
}


def vec(index):
    x,y,z = ((coordinate-GRID/2)*STEP for coordinate in index)
    return Vector((x,-z,y))


def cube(lo, hi, name):
    a,b = vec(lo),vec(hi)
    bpy.ops.mesh.primitive_cube_add(size=1, location=(a+b)/2)
    obj = bpy.context.object
    obj.name = name
    obj.dimensions = tuple(abs(v) for v in b-a)
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    return obj


def boolean(obj, other, kind):
    bpy.context.view_layer.objects.active = obj
    change = obj.modifiers.new(name=kind,type="BOOLEAN")
    change.operation = kind
    change.object = other
    bpy.ops.object.modifier_apply(modifier=change.name)
    bpy.data.objects.remove(other,do_unlink=True)


def original(hidden_recess):
    body = cube((3,3,3),(13,8,13),"Original slotted bracket")
    # Overlap before union so the join has a proper continuous solid.
    boolean(body,cube((7,7,5),(13,13,12),"Raised shoulder"),"UNION")
    boolean(body,cube((4,4,2),(6,7,14),"Through slot"),"DIFFERENCE")
    if hidden_recess:
        boolean(body,cube((8,9,10),(11,12,13),"Recess behind shoulder"),"DIFFERENCE")
    body.color=(.64,.74,.85,1)
    return body


def setup():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.render.resolution_x=480
    scene.render.resolution_y=480
    scene.render.resolution_percentage=100
    scene.render.film_transparent=True
    scene.render.dither_intensity=0
    scene.render.image_settings.file_format="PNG"
    scene.render.image_settings.color_mode="RGBA"
    scene.view_settings.view_transform="Standard"
    scene.display.shading.light="STUDIO"
    scene.display.shading.color_type="OBJECT"
    scene.display.shading.show_shadows=False
    scene.display.shading.show_cavity=False
    data=bpy.data.cameras.new("Registered camera")
    data.type="ORTHO"
    data.ortho_scale=GRID*STEP
    camera=bpy.data.objects.new("Registered camera",data)
    scene.collection.objects.link(camera)
    scene.camera=camera
    return scene,camera


def orient(camera, key):
    if key in CAMERAS:
        eye,right,up = map(Vector,CAMERAS[key])
        camera.rotation_euler=Matrix((right,up,eye)).transposed().to_euler()
    else:
        eye=Vector((3,-4,3)).normalized()
        camera.rotation_euler=(-eye).to_track_quat('-Z','Y').to_euler()
    camera.location=eye*4


def render(scene,camera,key,target):
    orient(camera,key)
    scene.render.filepath=str(target)
    bpy.ops.render.render(write_still=True)


def fixture(output, prefix, hidden):
    scene,camera=setup()
    original(hidden)
    views={}
    for key in CAMERAS:
        image=output/f"{prefix}-{key}.png"
        render(scene,camera,key,image)
        views[key]=base64.b64encode(image.read_bytes()).decode("ascii")
    package={"format":"fraynix.orthographic-silhouettes-v1","title":"Slotted bracket",
             "grid":GRID,"cellM":STEP,"views":views}
    (output/f"{prefix}.json").write_text(json.dumps(package,separators=(",",":"))+"\n",encoding="utf-8",newline="\n")


def volume(obj):
    mesh=bmesh.new()
    mesh.from_mesh(obj.data)
    mesh.transform(obj.matrix_world)
    result=abs(mesh.calc_volume(signed=True))
    mesh.free()
    return result


def sample_surface(obj):
    points=[obj.matrix_world @ vertex.co for vertex in obj.data.vertices]
    obj.data.calc_loop_triangles()
    triangles=[tuple(face.vertices) for face in obj.data.loop_triangles]
    centers=[sum((points[index] for index in face),Vector())/3 for face in triangles]
    return BVHTree.FromPolygons(points,triangles,all_triangles=True),points+centers


def distance(tree, points):
    return max(tree.find_nearest(point)[3] for point in points)


def silica(path):
    image=bpy.data.images.load(str(path),check_existing=False)
    values=list(image.pixels)[3::4]
    bpy.data.images.remove(image)
    return [value>=.5 for value in values]


def verify(glb, output):
    scene,camera=setup()
    truth=original(False)
    original_volume=volume(truth)
    reference_tree,reference_points=sample_surface(truth)
    truth_file=output/"withheld-original.png"
    fitted_file=output/"withheld-reconstruction.png"
    preview=output/"editable-prop.blend"
    render(scene,camera,"withheld",truth_file)
    bpy.data.objects.remove(truth,do_unlink=True)
    recess=original(True)
    recess_volume=volume(recess)
    bpy.data.objects.remove(recess,do_unlink=True)

    bpy.ops.import_scene.gltf(filepath=str(glb))
    model=next(obj for obj in scene.objects if obj.get("fraynixVisual")=="triangle-surface")
    visual_count=len(model.data.vertices)
    polygons=len(model.data.polygons)
    fitted_volume=volume(model)
    fitted_tree,fitted_points=sample_surface(model)
    surface_error=max(distance(reference_tree,fitted_points),distance(fitted_tree,reference_points))
    for obj in scene.objects:
        if obj.type=="MESH" and obj!=model:
            obj.hide_render=True
            obj.hide_set(True)
    model.color=(.64,.74,.85,1)
    render(scene,camera,"withheld",fitted_file)
    a,b=map(silica,(truth_file,fitted_file))
    intersection=sum(x and y for x,y in zip(a,b))
    union=sum(x or y for x,y in zip(a,b))
    for obj in bpy.context.selected_objects:
        obj.select_set(False)
    model.select_set(True)
    bpy.context.view_layer.objects.active=model
    bpy.ops.wm.save_as_mainfile(filepath=str(preview))
    # Edit the real imported triangles and return the asset through Blender's exporter.
    top=max(vertex.co.z for vertex in model.data.vertices)
    moved=0
    for vertex in model.data.vertices:
        if abs(vertex.co.z-top)<.000001:
            vertex.co.z+=.05
            moved+=1
    model.data.update()
    for obj in scene.objects:
        if obj.type=="MESH":
            obj.hide_render=False
            obj.hide_set(False)
    bpy.ops.export_scene.gltf(filepath=str(output/"blender-edited-reconstruction.glb"),
                              export_format="GLB",export_extras=True)
    ambiguous_pixels={}
    for key in CAMERAS:
        clean=silica(output/f"bracket-{key}.png")
        hidden=silica(output/f"hidden-recess-{key}.png")
        ambiguous_pixels[key]=sum(a!=b for a,b in zip(clean,hidden))
    report={"blender":bpy.app.version_string,"withheldSilhouetteIou":intersection/union,
            "sourceVolumeM3":original_volume,"reconstructedVolumeM3":fitted_volume,
            "hiddenRecessVolumeM3":recess_volume,"hiddenRecessInputPixelDifferences":ambiguous_pixels,"sampledSurfaceErrorM":surface_error,"surfaceLimitM":.00001,"blenderVertices":visual_count,"blenderPolygons":polygons,"editedVertices":moved,"editDeltaM":.05}
    report["passed"]=(report["withheldSilhouetteIou"]>=.995
                      and math.isclose(original_volume,fitted_volume,abs_tol=.000001)
                      and recess_volume < original_volume-.001 and surface_error<=.00001
                      and all(value==0 for value in ambiguous_pixels.values()))
    (output/"blender-reconstruction.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    if not report["passed"]:
        raise ValueError(report)


mode,*args=sys.argv[sys.argv.index("--")+1:]
if mode=="create":
    output=Path(args[0]).resolve()
    output.mkdir(parents=True,exist_ok=True)
    fixture(output,"bracket",False)
    fixture(output,"hidden-recess",True)
elif mode=="verify":
    verify(Path(args[0]).resolve(),Path(args[1]).resolve())
else:
    raise ValueError(mode)
