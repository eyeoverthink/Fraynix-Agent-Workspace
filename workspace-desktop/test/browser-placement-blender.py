"""Open the exact level GLB in Blender and test its scene and editable prop against independent inputs."""
import hashlib
import json
import math
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector

REFERENCE = runpy.run_path(str(Path(__file__).with_name("browser-views-blender.py")))
HELPERS = REFERENCE["helpers"]
BASIS = Matrix.Rotation(math.pi / 2, 3, "X")
TOLERANCE = 1e-5


def vector(values):
    return BASIS @ Vector(values)


def size(points):
    return Vector(tuple(max(p[i] for p in points) - min(p[i] for p in points) for i in range(3)))


def centered_mesh(obj, origin, orientation):
    return [orientation.inverted() @ (obj.matrix_world @ vertex.co - origin) for vertex in obj.data.vertices]


def check_scene(expected):
    bpy.context.view_layer.update()
    tagged = {}
    for obj in bpy.context.scene.objects:
        if obj.get("fraynixBody"):
            body = json.loads(obj["fraynixBody"])
            assert body["sourceId"] not in tagged, body
            tagged[body["sourceId"]] = (obj, body)
    placed_keys = [key for key in tagged if key.startswith("level-prop-")]
    originals = expected["destination"]["objects"]
    assert len(placed_keys) == 1 and len(tagged) == len(originals) + 1, tagged.keys()

    meta, = [json.loads(obj["fraynixScene"]) for obj in bpy.context.scene.objects if obj.get("fraynixScene")]
    for key in ("title", "gravityMps2", "scaleReference", "source"):
        assert meta[key] == expected["destination"][key], key
    max_position_error = 0
    for fixture in originals:
        obj, body = tagged[fixture["sourceId"]]
        for key in ("label", "massKg", "friction", "restitution"):
            assert body[key] == fixture[key], (key, body[key])
        origin = obj.matrix_world.translation
        position_error = (origin - vector(fixture["position"])).length
        max_position_error = max(max_position_error, position_error)
        assert position_error < TOLERANCE
        qx, qy, qz, qw = fixture["rotation"]
        wanted = BASIS @ Quaternion((qw, qx, qy, qz)).to_matrix() @ BASIS.inverted()
        rotation = obj.matrix_world.to_quaternion()
        assert abs(rotation.dot(wanted.to_quaternion())) > 1 - TOLERANCE
        dims = size(centered_mesh(obj, origin, rotation))
        assert (dims - Vector((fixture["size"][0], fixture["size"][2], fixture["size"][1]))).length < TOLERANCE
        assert len(obj.data.vertices) == 8

    collider, role = tagged[placed_keys[0]]
    position = collider.matrix_world.translation
    rotation = collider.matrix_world.to_quaternion()
    target = expected["placement"]
    prop_error = (position - vector((target["x"], .20, target["z"]))).length
    assert prop_error < TOLERANCE, (position, prop_error)
    assert abs(rotation.dot(Quaternion((0,0,1), math.radians(target["yaw"])))) > 1 - TOLERANCE
    collider_size = size(centered_mesh(collider, position, rotation))
    assert (collider_size - Vector((.5,.3,.4))).length < TOLERANCE
    assert len(collider.data.vertices) == 8 and role["massKg"] > 0
    visual, = [obj for obj in collider.children if obj.get("fraynixVisual") == "triangle-surface"]
    assert visual.type == "MESH" and visual.data != collider.data
    points = centered_mesh(visual, position, rotation)
    faces = [tuple(face.vertices) for face in visual.data.polygons]
    assert len(faces) > 100 and all(len(face) == 3 for face in faces)
    assert (size(points) - collider_size).length < TOLERANCE
    return points, faces, dict(
        originalBodies=len(originals), bodyCount=len(tagged), originalMaxPositionErrorM=max_position_error,
        propPositionBlenderM=list(position), propPositionErrorM=prop_error,
        propYawDeg=math.degrees(rotation.to_euler().z), colliderSizeBlenderM=list(collider_size),
        colliderVertices=len(collider.data.vertices), visualTriangles=len(faces),
    )


def render(scene, camera, path):
    eye = Vector((3,-4,2)).normalized()
    camera.location = eye * 4
    camera.rotation_euler = eye.to_track_quat('Z','Y').to_euler()
    scene.render.filepath = str(path)
    bpy.ops.render.render(write_still=True)


def compare_visual(out, points, faces):
    scene, camera = HELPERS["setup"]()
    camera.data.ortho_scale = .9
    truth = REFERENCE["solid"]()
    bpy.context.view_layer.update()
    truth_volume = HELPERS["volume"](truth)
    render(scene,camera,out/"oracle-original.png")
    bpy.data.objects.remove(truth,do_unlink=True)
    mesh = bpy.data.meshes.new("Measured editable visual mesh")
    mesh.from_pydata(points,[],faces)
    mesh.update()
    model = bpy.data.objects.new("Fraynix visual surface",mesh)
    scene.collection.objects.link(model)
    bpy.context.view_layer.update()
    measured = HELPERS["volume"](model)
    relative = abs(measured-truth_volume)/truth_volume
    assert relative < .005, relative
    render(scene,camera,out/"oracle-export.png")
    a, b = (HELPERS["silica"](out/name) for name in ("oracle-original.png","oracle-export.png"))
    iou = sum(x and y for x,y in zip(a,b)) / sum(x or y for x,y in zip(a,b))
    assert iou > .99, iou
    return dict(originalVolumeM3=truth_volume,importedVolumeM3=measured,
                volumeRelativeError=relative,independentViewIou=iou)


def main(out):
    expected = json.loads((out/"expected-level.json").read_text(encoding="utf-8"))
    glb = out/"browser-level.glb"
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(glb),import_scene_extras=True)
    points, faces, checks = check_scene(expected)
    blend = out/"browser-level.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend))
    bpy.ops.wm.open_mainfile(filepath=str(blend))
    _, _, reopened = check_scene(expected)
    assert reopened == checks
    report = {
        **checks, **compare_visual(out,points,faces),
        "passed": True, "version": bpy.app.version_string, "blendReopened": True,
        "glbSha256": hashlib.sha256(glb.read_bytes()).hexdigest()
    }
    (out/"blender-level-proof.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8",newline="\n")


if __name__ == "__main__":
    main(Path(sys.argv[sys.argv.index("--")+1]).resolve())
