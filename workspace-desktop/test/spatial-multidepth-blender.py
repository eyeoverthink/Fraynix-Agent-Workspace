"""Author opposing depth grids, then verify the two recovered recesses in Blender."""
import base64
import json
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Vector

tools=runpy.run_path(str(Path(__file__).with_name("spatial-reconstruction-blender.py")))
GRID,STEP=tools["GRID"],tools["STEP"]
setup,original,render,volume,surface,distance,mask=[
    tools[key] for key in ("setup","original","render","volume","sample_surface","distance","silica")]
depth_tools=runpy.run_path(str(Path(__file__).with_name("spatial-depth-blender.py")))
import_mesh=depth_tools["import_mesh"]
NEAR=GRID*STEP/2
SIGNS={"+z":1,"-z":-1}


def fixture():
    bracket=original(True)
    second=tools["cube"]((7,4,2),(10,6,6),"Opposite lower recess")
    tools["boolean"](bracket,second,"DIFFERENCE")
    bpy.context.view_layer.update()
    return bracket


def ray(x,y,axis):
    sign=SIGNS[axis]
    start=Vector(((x-GRID/2)*STEP,-sign*NEAR,(y-GRID/2)*STEP))
    return start,Vector((0,sign,0))


def create(output):
    output.mkdir(parents=True,exist_ok=True)
    scene,camera=setup()
    fixture()
    graph=bpy.context.evaluated_depsgraph_get()
    views={}
    for axis in "xyz":
        file=output/f"input-{axis}.png"
        render(scene,camera,axis,file)
        views[axis]=base64.b64encode(file.read_bytes()).decode("ascii")
    grids=[]
    for axis in SIGNS:
        values=[]
        for row in range(GRID):
            for col in range(GRID):
                start,direction=ray(col+.5,GRID-row-.5,axis)
                hit,position,*_=scene.ray_cast(graph,start,direction,distance=2*NEAR)
                values.append(round((position-start).dot(direction)*1e6) if hit else -1)
        grids.append({"axis":axis,"origin":("positive" if axis=="+z" else "negative")+"-grid-face",
                      "unit":"um","samples":values})
    package={"format":"fraynix.orthographic-silhouettes-multidepth-v3","title":"Bracket with opposite recesses",
             "grid":GRID,"cellM":STEP,"views":views,"depthViews":grids}
    (output/"opposed-depth.json").write_text(json.dumps(package,separators=(",",":"))+"\n",encoding="utf-8",newline="\n")


def first(tree,x,y,axis):
    start,direction=ray(x,y,axis)
    _,_,_,hit=tree.ray_cast(start,direction,2*NEAR)
    return hit


def probes(tree):
    return {"positiveRecessM":first(tree,9.27,10.24,"+z"),
            "negativeRecessM":first(tree,8.27,5.24,"-z"),
            "positiveNeighborM":first(tree,11.35,10.24,"+z"),
            "negativeNeighborM":first(tree,11.35,5.24,"-z"),
            "throughSlotM":first(tree,4.36,5.24,"-z")}


def oblique(scene,camera,axis,target):
    sign=SIGNS[axis]
    eye=Vector((3*sign,-4*sign,2.6)).normalized()
    camera.location=eye*4
    camera.rotation_euler=(-eye).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(target)
    bpy.ops.render.render(write_still=True)


def verify(output):
    scene,camera=setup()
    target=fixture()
    truth_tree,truth_points=surface(target)
    truth_volume=volume(target)
    truth=probes(truth_tree)
    for axis in SIGNS:
        oblique(scene,camera,axis,output/f"withheld-{axis}-original.png")
    bpy.data.objects.remove(target,do_unlink=True)

    controls={}
    for name in ("silhouettes","positive-only","negative-only"):
        model,group=import_mesh(output/f"{name}.glb")
        tree,_=surface(model)
        controls[name]={"volumeM3":volume(model),"probes":probes(tree)}
        for obj in group:
            bpy.data.objects.remove(obj,do_unlink=True)

    model,_=import_mesh(output/"opposed-depth.glb")
    fitted,points=surface(model)
    actual=probes(fitted)
    metrics={}
    maximum=max(distance(truth_tree,points),distance(fitted,truth_points))
    for axis in SIGNS:
        hits,disagreements,maximum_ray=0,0,0
        for row in range(1,15):
            for col in range(1,15):
                u,v=col+.27,row+.24
                a,b=first(truth_tree,u,v,axis),first(fitted,u,v,axis)
                if a is not None:
                    hits+=1
                if (a is None)!=(b is None):
                    disagreements+=1
                elif a is not None:
                    maximum_ray=max(maximum_ray,abs(a-b))
        image=output/f"withheld-{axis}-reconstruction.png"
        oblique(scene,camera,axis,image)
        a,b=(mask(file) for file in (output/f"withheld-{axis}-original.png",image))
        iou=sum(x and y for x,y in zip(a,b))/sum(x or y for x,y in zip(a,b))
        metrics[axis]={"rays":196,"sourceHits":hits,"missDisagreements":disagreements,
                       "maxHitErrorM":maximum_ray,"obliqueSilhouetteIou":iou}

    for obj in bpy.context.selected_objects:
        obj.select_set(False)
    model.select_set(True)
    bpy.context.view_layer.objects.active=model
    bpy.ops.wm.save_as_mainfile(filepath=str(output/"editable-opposed-recesses.blend"))
    control=controls["silhouettes"]["probes"]
    one=controls["positive-only"]["probes"]
    other=controls["negative-only"]["probes"]

    def same(a,b):
        return abs(a-b)<=1e-6

    passed=(all(m["missDisagreements"]==0 and m["maxHitErrorM"]<=1e-6 and m["obliqueSilhouetteIou"]>=.995
                for m in metrics.values())
            and abs(volume(model)-truth_volume)<=1e-6 and maximum<=1e-5
            and same(actual["positiveRecessM"]-control["positiveRecessM"],.10)
            and same(actual["negativeRecessM"]-control["negativeRecessM"],.15)
            and same(one["positiveRecessM"],actual["positiveRecessM"])
            and same(one["negativeRecessM"],control["negativeRecessM"])
            and same(other["positiveRecessM"],control["positiveRecessM"])
            and same(other["negativeRecessM"],actual["negativeRecessM"])
            and same(actual["positiveNeighborM"],control["positiveNeighborM"])
            and same(actual["negativeNeighborM"],control["negativeNeighborM"])
            and actual["throughSlotM"] is None)
    evidence={"blender":bpy.app.version_string,"sourceVolumeM3":truth_volume,"sourceProbes":truth,
              "controls":controls,"combinedVolumeM3":volume(model),"combinedProbes":actual,
              "maxSampledSurfaceErrorM":maximum,"withheld":metrics,"passed":passed}
    (output/"multidepth-blender-evidence.json").write_text(json.dumps(evidence,indent=2)+"\n",encoding="utf-8",newline="\n")
    if not passed:
        raise ValueError(evidence)


if __name__=="__main__":
    mode,output=sys.argv[sys.argv.index("--")+1:]
    if mode=="create":
        create(Path(output).resolve())
    elif mode=="verify":
        verify(Path(output).resolve())
    else:
        raise ValueError(mode)
