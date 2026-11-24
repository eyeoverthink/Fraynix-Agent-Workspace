"""Generate Blender first hits and independently verify exported 3D geometry."""
import base64
import json
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Vector

tools = runpy.run_path(str(Path(__file__).with_name("spatial-reconstruction-blender.py")))
GRID, STEP = tools["GRID"], tools["STEP"]
CAMERAS = tools["CAMERAS"]
setup, original, render = [tools[key] for key in ("setup","original","render")]
volume, sample_surface, distance, silhouette = [tools[key] for key in ("volume","sample_surface","distance","silica")]
NEAR = GRID * STEP / 2
DIRECTION = Vector((0,1,0))  # Fraynix −Z maps to Blender +Y.


def ray_origin(grid_x, grid_y):
    return Vector(((grid_x-GRID/2)*STEP,-NEAR,(grid_y-GRID/2)*STEP))


def scene_depth(scene, graph, col, row):
    origin = ray_origin(col+.5,GRID-row-.5)
    hit, position, *_ = scene.ray_cast(graph,origin,DIRECTION,distance=2*NEAR)
    return round((position-origin).dot(DIRECTION)*1e6) if hit else -1


def save_depth_preview(samples, path):
    size=GRID*24
    width=24
    image=bpy.data.images.new("Rear first surface",width=size,height=size,alpha=False)
    colors=[]
    for y in range(size):
        for x in range(size):
            depth=samples[(GRID-1-y//width)*GRID+x//width]
            if depth<0:
                color=(.04,.06,.10,1)
            else:
                f=depth/(2*NEAR*1e6)
                color=(.08+1.3*f,.80-.85*f,.95-.35*f,1)
            colors.extend(color)
    image.pixels.foreach_set(colors)
    image.filepath_raw=str(path)
    image.file_format="PNG"
    image.save()
    bpy.data.images.remove(image)


def create(output):
    output.mkdir(parents=True,exist_ok=True)
    scene,camera=setup()
    original(True)
    bpy.context.view_layer.update()
    graph=bpy.context.evaluated_depsgraph_get()
    samples=[scene_depth(scene,graph,col,row) for row in range(GRID) for col in range(GRID)]
    images={}
    for axis in CAMERAS:
        file=output/f"depth-input-{axis}.png"
        render(scene,camera,axis,file)
        images[axis]=base64.b64encode(file.read_bytes()).decode("ascii")
    package={"format":"fraynix.orthographic-silhouettes-depth-v2","title":"Recessed bracket",
             "grid":GRID,"cellM":STEP,"views":images,
             "depth":{"axis":"+z","origin":"positive-grid-face","unit":"um","samples":samples}}
    (output/"recess-depth.json").write_text(json.dumps(package,separators=(",",":"))+"\n",encoding="utf-8",newline="\n")
    save_depth_preview(samples,output/"depth-input-preview.png")


def import_mesh(file):
    before=set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(file))
    imported=set(bpy.context.scene.objects)-before
    mesh=next(obj for obj in imported if obj.get("fraynixVisual")=="triangle-surface")
    for obj in imported:
        if obj.type=="MESH" and obj!=mesh:
            obj.hide_render=True
            obj.hide_set(True)
    mesh.color=(.64,.74,.85,1)
    bpy.context.view_layer.update()
    return mesh,imported


def first_hit(tree, x, y):
    _,_,_,hit=tree.ray_cast(ray_origin(x,y),DIRECTION,2*NEAR)
    return hit


def probe_values(tree):
    return {"insideM":first_hit(tree,9.27,10.24),"neighborM":first_hit(tree,11.35,10.24),
            "throughSlotM":first_hit(tree,4.35,5.24)}


def verify(baseline, reconstructed, output):
    scene,camera=setup()
    fixture=original(True)
    bpy.context.view_layer.update()
    truth_volume=volume(fixture)
    truth_tree,truth_samples=sample_surface(fixture)
    truth_probe=probe_values(truth_tree)
    render(scene,camera,"withheld",output/"withheld-recess-original.png")
    bpy.data.objects.remove(fixture,do_unlink=True)

    uncarved,group=import_mesh(baseline)
    baseline_tree,_=sample_surface(uncarved)
    baseline_probe=probe_values(baseline_tree)
    baseline_volume=volume(uncarved)
    for obj in group:
        bpy.data.objects.remove(obj,do_unlink=True)

    model,group=import_mesh(reconstructed)
    fitted_tree,fit_samples=sample_surface(model)
    fit_probe=probe_values(fitted_tree)
    fit_volume=volume(model)
    surface_error=max(distance(truth_tree,fit_samples),distance(fitted_tree,truth_samples))
    render(scene,camera,"withheld",output/"withheld-recess-reconstructed.png")
    a,b=(silhouette(output/name) for name in ("withheld-recess-original.png","withheld-recess-reconstructed.png"))
    iou=sum(x and y for x,y in zip(a,b))/sum(x or y for x,y in zip(a,b))
    ray_error=0
    mismatched=0
    truth_hits=0
    # Offset the test rays from the source grid centres.
    for row in range(1,15):
        for col in range(1,15):
            u,v=col+.27,row+.24
            p,q=first_hit(truth_tree,u,v),first_hit(fitted_tree,u,v)
            if p is not None:
                truth_hits+=1
            if (p is None)!=(q is None):
                mismatched+=1
            if p is not None and q is not None:
                ray_error=max(ray_error,abs(p-q))

    for obj in bpy.context.selected_objects:
        obj.select_set(False)
    model.select_set(True)
    bpy.context.view_layer.objects.active=model
    bpy.ops.wm.save_as_mainfile(filepath=str(output/"editable-recess.blend"))

    # Move the real imported recess back wall 25 mm deeper. Save as a separate derivative.
    inverse=model.matrix_world.inverted()
    edited=0
    for vertex in model.data.vertices:
        point=model.matrix_world @ vertex.co
        if abs(point.y+.1)<1e-6 and -.000001<=point.x<=.150001 and .049999<=point.z<=.200001:
            vertex.co=inverse @ (point+Vector((0,.025,0)))
            edited+=1
    model.data.update()
    changed_tree,_=sample_surface(model)
    changed_probe=probe_values(changed_tree)
    for obj in group:
        if obj.type=="MESH":
            obj.hide_render=False
            obj.hide_set(False)
    bpy.ops.export_scene.gltf(filepath=str(output/"blender-edited-recess.glb"),export_format="GLB",export_extras=True)

    report={"blender":bpy.app.version_string,"withheldSilhouetteIou":iou,
            "sourceVolumeM3":truth_volume,"silhouetteOnlyVolumeM3":baseline_volume,"reconstructedVolumeM3":fit_volume,
            "sourceProbe":truth_probe,"silhouetteOnlyProbe":baseline_probe,"reconstructedProbe":fit_probe,
            "withheldRayCount":196,"withheldTruthHits":truth_hits,"withheldRayMissDisagreements":mismatched,"maxWithheldDepthErrorM":ray_error,
            "sampledSurfaceErrorM":surface_error,"editedVertices":edited,"editedProbe":changed_probe}
    report["passed"]=(iou>=.995 and abs(fit_volume-truth_volume)<=1e-6 and surface_error<=1e-5
                      and abs((fit_probe["insideM"]-baseline_probe["insideM"])-.1)<=1e-6
                      and abs(fit_probe["neighborM"]-baseline_probe["neighborM"])<=1e-6
                      and fit_probe["throughSlotM"] is None and mismatched==0 and ray_error<=1e-6
                      and edited>0 and abs((changed_probe["insideM"]-fit_probe["insideM"])-.025)<=1e-6)
    (output/"depth-blender-evidence.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8",newline="\n")
    if not report["passed"]:
        raise ValueError(report)


if __name__=="__main__":
    mode,*args=sys.argv[sys.argv.index("--")+1:]
    if mode=="create":
        create(Path(args[0]).resolve())
    elif mode=="verify":
        verify(*(Path(arg).resolve() for arg in args))
    else:
        raise ValueError(mode)
