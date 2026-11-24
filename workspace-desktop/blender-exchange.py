"""Exchange declared rigid props, triangle surfaces and linear translations with Blender."""
import base64
import hashlib
import json
import math
from pathlib import Path
import re
import struct
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector


TO_WORKSPACE = Matrix(((1, 0, 0), (0, 0, 1), (0, -1, 0)))
TO_BLENDER = TO_WORKSPACE.transposed()
METRES_PER_UNIT = {"m": 1, "cm": .01, "mm": .001}
REFERENCE = ("Fraynix Scale A", "Fraynix Scale B")


def box_bounds(mesh, label):
    if len(mesh.vertices) != 8 or len(mesh.polygons) != 6 or any(len(face.vertices) != 4 for face in mesh.polygons):
        raise ValueError(f"{label}: collider must be a six-face box")
    points = [vertex.co for vertex in mesh.vertices]
    low = Vector(tuple(min(point[axis] for point in points) for axis in range(3)))
    high = Vector(tuple(max(point[axis] for point in points) for axis in range(3)))
    size = high - low
    if min(size) <= 0:
        raise ValueError(f"{label}: collider has a collapsed axis")
    corners = set()
    for point in points:
        corner = tuple(int(abs(point[i] - high[i]) < abs(point[i] - low[i])) for i in range(3))
        if any(abs(point[i] - (high[i] if corner[i] else low[i])) > 1e-5 * size[i] for i in range(3)):
            raise ValueError(f"{label}: collider vertices do not form a local box")
        corners.add(corner)
    if len(corners) != 8:
        raise ValueError(f"{label}: collider has duplicated corners")
    faces = {frozenset(face.vertices) for face in mesh.polygons}
    for axis in range(3):
        for coord in (low[axis], high[axis]):
            plane = frozenset(i for i, point in enumerate(points) if abs(point[axis] - coord) <= 1e-5 * size[axis])
            if plane not in faces:
                raise ValueError(f"{label}: collider is missing a box face")
    return (low + high) / 2, size


def read_body(obj, evaluated, metres, source_id):
    rigid = obj.rigid_body
    if not rigid or rigid.collision_shape != "BOX" or not rigid.enabled or rigid.kinematic:
        raise ValueError(f"{obj.name}: use an enabled non-kinematic BOX rigid body")
    if abs(rigid.collision_margin) > 1e-7 and rigid.use_margin:
        raise ValueError(f"{obj.name}: custom collision margin has no spatial v1 field")
    center, size = box_bounds(evaluated.data, obj.name)
    matrix = evaluated.matrix_world
    location, rotation, scale = matrix.decompose()
    reconstructed = Matrix.LocRotScale(location, rotation, scale)
    if min(scale) <= 0 or any(
        abs(matrix[row][column] - reconstructed[row][column]) > 1e-5
        for row in range(3) for column in range(3)
    ):
        raise ValueError(f"{obj.name}: reflected or sheared collider transform")
    mapped = (TO_WORKSPACE @ rotation.to_matrix() @ TO_BLENDER).to_quaternion()
    return {
        "sourceId": source_id, "label": obj.get("fraynix_label", obj.name)[:100], "shape": "box",
        "position": [round(coordinate, 6) for coordinate in metres * (TO_WORKSPACE @ (matrix @ center))],
        "rotation": [mapped.x, mapped.y, mapped.z, mapped.w],
        "size": [round(metres * size[i] * scale[i], 6) for i in (0, 2, 1)],
        "massKg": round(rigid.mass, 6) if rigid.type == "ACTIVE" else 0,
        "friction": round(rigid.friction, 6), "restitution": round(rigid.restitution, 6),
    }


def read_translation(obj, center, metres):
    anim = obj.animation_data
    if not anim:
        return None
    if anim.drivers or anim.nla_tracks:
        raise ValueError(f"{obj.name}: drivers and NLA are outside translation exchange")
    if anim.action is None:
        return None
    if obj.parent or obj.constraints or obj.modifiers or center.length > 1e-6:
        raise ValueError(f"{obj.name}: animate an unparented, centered box without constraints or modifiers")
    if anim.action.is_action_legacy:
        curves = list(anim.action.fcurves)
    else:
        if len(anim.action.layers) != 1 or len(anim.action.layers[0].strips) != 1 or not anim.action_slot:
            raise ValueError(f"{obj.name}: use one active action slot and keyframe strip")
        bag = anim.action.layers[0].strips[0].channelbag(anim.action_slot)
        curves = list(bag.fcurves) if bag is not None else []
    if len(curves) != 3 or {curve.array_index for curve in curves} != {0, 1, 2}:
        raise ValueError(f"{obj.name}: use three synchronized location curves")
    by_axis = []
    for curve in sorted(curves, key=lambda fcurve: fcurve.array_index):
        if (curve.data_path != "location" or curve.mute or curve.modifiers or curve.sampled_points
                or curve.extrapolation != "CONSTANT"):
            raise ValueError(f"{obj.name}: use unmodified location keyframes")
        values = {}
        for key in curve.keyframe_points:
            frame, value = key.co
            if frame != int(frame) or frame in values or key.interpolation != "LINEAR":
                raise ValueError(f"{obj.name}: use linear keys on whole frames")
            values[int(frame)] = value
        by_axis.append(values)
    if not all(values.keys() == by_axis[0].keys() for values in by_axis):
        raise ValueError(f"{obj.name}: key X, Y and Z on the same frames")
    return [{"frame": frame, "positionM": [round(v, 6) for v in
             metres * (TO_WORKSPACE @ Vector([values[frame] for values in by_axis]))]}
            for frame in sorted(by_axis[0])]


def read_visual(obj, parent, graph, metres):
    if (obj.type != "MESH" or obj.rigid_body or obj.animation_data or obj.constraints or obj.modifiers
            or obj.data.shape_keys or metres != 1):
        raise ValueError(f"{obj.name}: use an unanimated triangle surface in a metre scene")
    if any(len(face.vertices) != 3 for face in obj.data.polygons):
        raise ValueError(f"{obj.name}: triangulate the visual mesh before exchange")
    center, _ = box_bounds(parent.evaluated_get(graph).data, parent.name)
    _, _, scale = parent.evaluated_get(graph).matrix_world.decompose()
    local = obj.matrix_parent_inverse @ obj.matrix_basis
    points = []
    for vertex in obj.data.vertices:
        point = (local @ vertex.co) - center
        mapped = TO_WORKSPACE @ Vector(tuple(point[i] * scale[i] for i in range(3)))
        points.extend(v if v else 0.0 for v in mapped)
    indices = [index for face in obj.data.polygons for index in face.vertices]
    vertices = len(obj.data.vertices)
    if not 3 <= vertices <= 4096 or not 3 <= len(indices) <= 12288:
        raise ValueError(f"{obj.name}: visual mesh exceeds the triangle budget")
    packed = struct.pack("<II", vertices, len(indices))
    packed += struct.pack(f"<{len(points)}f{len(indices)}H", *points, *indices)
    return hashlib.sha256(packed).hexdigest(), base64.b64encode(packed).decode("ascii")


def write_visual(parent, encoded):
    packed = base64.b64decode(encoded)
    count, index_count = struct.unpack_from("<II", packed)
    values = struct.unpack_from(f"<{count * 3}f{index_count}H", packed, 8)
    vertices = [TO_BLENDER @ Vector(values[i:i + 3]) for i in range(0, count * 3, 3)]
    faces = [values[i:i + 3] for i in range(count * 3, len(values), 3)]
    mesh = bpy.data.meshes.new(parent.name + " surface")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(parent.name + " surface", mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj.parent = parent
    obj["fraynix_visual"] = "triangle-surface"
    parent.display_type = "WIRE"
    parent.hide_render = True

def read_scene(output):
    scene = bpy.context.scene
    if bpy.data.libraries:
        raise ValueError("Packaged scene must use local objects without linked Blender libraries")
    if any(obj.rigid_body_constraint for obj in scene.objects):
        raise ValueError("Rigid-body joints have no spatial v1 representation")
    if scene.unit_settings.system not in {"METRIC", "IMPERIAL"}:
        raise ValueError("Set Metric or Imperial scene units before exchange")
    metres = scene.unit_settings.scale_length
    if not math.isfinite(metres) or metres <= 0:
        raise ValueError("Scene scale must be positive")
    if not scene.use_gravity or abs(scene.gravity.x) > 1e-6 or abs(scene.gravity.y) > 1e-6:
        raise ValueError("Use vertical gravity in the Blender scene")
    refs = [scene.objects.get(name) for name in REFERENCE]
    if any(obj is None or obj.type != "EMPTY" for obj in refs):
        raise ValueError("Add Fraynix Scale A and Fraynix Scale B empties at the measured endpoints")
    known = scene.get("fraynix_known_length_m")
    tolerance = scene.get("fraynix_tolerance_m")
    if type(known) not in (int, float) or type(tolerance) not in (int, float):
        raise ValueError("Set scene properties fraynix_known_length_m and fraynix_tolerance_m")
    visuals = [obj for obj in scene.objects if obj.get("fraynix_visual") == "triangle-surface"]
    bodies = [obj for obj in scene.objects if obj.type not in {"EMPTY", "LIGHT", "CAMERA"} and obj not in visuals]
    if not 2 <= len(bodies) <= 32 or any(obj.type != "MESH" for obj in bodies):
        raise ValueError("Use 2–32 rigid box meshes, plus the scale reference empties")
    scene.frame_set(scene.frame_start)
    graph = bpy.context.evaluated_depsgraph_get()
    objects = []
    tracks = {}
    meshes = {}
    by_parent = {}
    for obj in visuals:
        if obj.parent not in bodies or obj.parent in by_parent:
            raise ValueError("Attach one marked visual surface to its collider")
        by_parent[obj.parent] = obj
    for obj in sorted(bodies, key=lambda item: item.get("fraynix_source_id", item.name)):
        # Names are stable for externally authored files that supply a unique ASCII identifier.
        source_id = obj.get("fraynix_source_id", obj.name)
        if not isinstance(source_id, str) or not re.fullmatch(r"[A-Za-z0-9_.-]{1,80}", source_id):
            raise ValueError(f"{obj.name}: set a unique ASCII fraynix_source_id or rename the mesh")
        evaluated = obj.evaluated_get(graph)
        body = read_body(obj, evaluated, metres, source_id)
        surfaces = by_parent.get(obj)
        if surfaces:
            key, encoded = read_visual(surfaces, obj, graph, metres)
            meshes[key] = encoded
            body["visualMesh"] = key
        center, _ = box_bounds(evaluated.data, obj.name)
        keys = read_translation(obj, center, metres)
        if keys is not None:
            tracks[source_id] = keys
        objects.append(body)
    a, b = ([round(coordinate, 6) for coordinate in metres * (TO_WORKSPACE @ obj.evaluated_get(graph).matrix_world.translation)] for obj in refs)
    data = {
        "format": "fraynix.spatial-scene", "version": 1,
        "title": scene.get("fraynix_title", scene.name)[:120], "units": "m", "frame": "right-handed-y-up",
        "source": {"kind": scene.get("fraynix_source_kind", "unverified"),
                   "physicalProperties": scene.get("fraynix_physical_properties", "declared"),
                   "note": scene.get("fraynix_source_note", "Authored Blender scene. Scale and physical properties are provided by the author.")},
        "scaleReference": {"a": a, "b": b, "knownLengthM": known, "toleranceM": tolerance,
                           "basis": scene.get("fraynix_scale_basis", "declared-test")},
        "gravityMps2": round(-scene.gravity.z, 6), "objects": objects,
    }
    if meshes:
        data["visualMeshes"] = meshes
    if tracks:
        for obj in bodies:
            if obj.data.shape_keys or obj.data.animation_data:
                raise ValueError(f"{obj.name}: collider shape keys and mesh animation are outside translation exchange")
        if scene.frame_start != 1 or scene.render.fps != 24 or scene.render.fps_base != 1:
            raise ValueError("Use a 24 fps translation timeline starting at frame 1")
        if scene.rigidbody_world and scene.rigidbody_world.enabled:
            raise ValueError("Disable Blender rigid-body playback before importing authored translation keys")
        data["translationAnimation"] = {"fps": 24, "tracks": tracks}
    output.write_text(json.dumps({"blenderVersion": bpy.app.version_string, "scene": data},
                                 allow_nan=False), encoding="utf-8")


def add_body(spec):
    bpy.ops.mesh.primitive_cube_add(size=1, location=TO_BLENDER @ Vector(spec["position"]))
    obj = bpy.context.object
    obj.name = spec["sourceId"]
    obj["fraynix_source_id"] = spec["sourceId"]
    obj["fraynix_label"] = spec["label"]
    x, y, z, w = spec["rotation"]
    obj.rotation_mode = "QUATERNION"
    obj.rotation_quaternion = (TO_BLENDER @ Quaternion((w, x, y, z)).to_matrix() @ TO_WORKSPACE).to_quaternion()
    scale = [spec["size"][i] for i in (0, 2, 1)]
    if "visualMesh" in spec:
        obj.data.transform(Matrix.Diagonal((*scale, 1)))
    else:
        obj.scale = scale
    bpy.ops.rigidbody.object_add()
    obj.rigid_body.collision_shape = "BOX"
    obj.rigid_body.type = "ACTIVE" if spec["massKg"] else "PASSIVE"
    if spec["massKg"]:
        obj.rigid_body.mass = spec["massKg"]
    obj.rigid_body.friction = spec["friction"]
    obj.rigid_body.restitution = spec["restitution"]
    return obj


def write_translation(scene, bodies, animation, metres):
    scene.render.fps = animation["fps"]
    scene.render.fps_base = 1
    scene.frame_start = 1
    scene.frame_end = max(track[-1]["frame"] for track in animation["tracks"].values())
    if scene.rigidbody_world:
        scene.rigidbody_world.enabled = False
    for source_id, keys in animation["tracks"].items():
        obj = bodies[source_id]
        for key in keys:
            obj.location = TO_BLENDER @ Vector(key["positionM"]) / metres
            obj.keyframe_insert("location", frame=key["frame"], group="Location")
        anim = obj.animation_data
        curves = anim.action.layers[0].strips[0].channelbag(anim.action_slot).fcurves
        for curve in curves:
            for key in curve.keyframe_points:
                key.interpolation = "LINEAR"
    scene.frame_set(scene.frame_start)


def write_scene(source, output):
    data = json.loads(source.read_text(encoding="utf-8"))
    scene = bpy.context.scene
    for obj in list(scene.objects):
        bpy.data.objects.remove(obj, do_unlink=True)
    scene.name = data["title"][:63]
    scene["fraynix_title"] = data["title"]
    scene.unit_settings.system = "METRIC"
    metres = METRES_PER_UNIT[data["units"]]
    scene.unit_settings.scale_length = metres
    scene.gravity = (0, 0, -data["gravityMps2"])
    metadata = {"kind": "source_kind", "physicalProperties": "physical_properties", "note": "source_note"}
    for key, field in metadata.items():
        scene["fraynix_" + field] = data["source"][key]
    reference = data["scaleReference"]
    scene["fraynix_known_length_m"] = reference["knownLengthM"]
    scene["fraynix_tolerance_m"] = reference["toleranceM"]
    scene["fraynix_scale_basis"] = reference["basis"]
    for key, name in zip(("a", "b"), REFERENCE):
        obj = bpy.data.objects.new(name, None)
        scene.collection.objects.link(obj)
        obj.location = TO_BLENDER @ Vector(reference[key])
    bodies = {}
    for spec in data["objects"]:
        body = add_body(spec)
        bodies[spec["sourceId"]] = body
        if "visualMesh" in spec:
            write_visual(body, data["visualMeshes"][spec["visualMesh"]])
    if "translationAnimation" in data:
        write_translation(scene, bodies, data["translationAnimation"], metres)
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(output), compress=True)


def write_game_layout(source, output):
    package = source.read_text(encoding="utf-8")
    data = json.loads(package)["frame"]
    source_info = data["source"]
    if source_info["kind"] != "game" or source_info["continuous"]:
        raise ValueError("Choose a paused Game Lab frame")
    scene = bpy.context.scene
    for obj in list(scene.objects):
        bpy.data.objects.remove(obj, do_unlink=True)
    scene.name = "Game Lab " + source_info["levelId"]
    scene.unit_settings.system = "NONE"
    scene["fraynix_snapshot_sha256"] = data["snapshotHash"]
    scene["fraynix_session_id"] = source_info["sessionId"]
    scene["fraynix_tick"] = str(source_info["tick"])
    scene["fraynix_units"] = source_info["units"]
    scene["fraynix_scope"] = "Game layout in game units; render ordering and component metadata are retained."
    bpy.data.texts.new("Fraynix Game snapshot.json").write(package)
    for entry in data["entities"]:
        if entry.get("shape") != "quad":
            continue
        width, height, _ = entry["size"]
        vertices = [TO_BLENDER @ Vector((x*width/2, y*height/2, 0)) for x, y in
                    ((-1, -1), (1, -1), (1, 1), (-1, 1))]
        mesh = bpy.data.meshes.new(entry["id"])
        mesh.from_pydata(vertices, [], [(0, 1, 2, 3)])
        mesh.update()
        name = entry["label"] or "Game object"
        obj = bpy.data.objects.new(name, mesh)
        scene.collection.objects.link(obj)
        obj.location = TO_BLENDER @ Vector(entry["position"])
        x, y, z, w = entry["rotation"]
        obj.rotation_mode = "QUATERNION"
        obj.rotation_quaternion = (TO_BLENDER @ Quaternion((w, x, y, z)).to_matrix() @ TO_WORKSPACE).to_quaternion()
        obj.color = entry["source"]["sprite"]["color"]
        obj["fraynix_source_id"] = entry["id"]
        obj["fraynix_render_layer"] = entry["renderLayer"]
        obj["fraynix_components"] = json.dumps(entry["components"])
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(output), compress=True)


def read_game_layout(output):
    scene = bpy.context.scene
    if bpy.data.libraries or scene.unit_settings.system != "NONE":
        raise ValueError("Use an exported local Game Lab layout in game units")
    block = bpy.data.texts.get("Fraynix Game snapshot.json")
    if block is None:
        raise ValueError("Game Lab snapshot is missing")
    original = block.as_string()
    data = json.loads(original)["frame"]
    if scene.get("fraynix_snapshot_sha256") != data["snapshotHash"]:
        raise ValueError("Game Lab snapshot metadata changed")
    if not bpy.context.scene.objects or len(bpy.context.scene.objects) > 4096:
        raise ValueError("Unexpected layout object count")
    expected = {entry["id"]: entry for entry in data["entities"] if entry.get("shape") == "quad"}
    graph = bpy.context.evaluated_depsgraph_get()
    seen = set()
    moves = []
    for obj in scene.objects:
        source_id = obj.get("fraynix_source_id")
        if obj.type != "MESH" or source_id not in expected or source_id in seen:
            raise ValueError("Layout contains an added, duplicated or unidentified object")
        seen.add(source_id)
        entry = expected[source_id]
        item = obj.evaluated_get(graph)
        if len(item.data.vertices) != 4 or len(item.data.polygons) != 1 or len(item.data.polygons[0].vertices) != 4:
            raise ValueError(f"{obj.name}: only existing game quads can be edited")
        center = item.matrix_world.translation
        location = TO_WORKSPACE @ center
        if not all(math.isfinite(value) for value in location) or abs(location.z) > 1e-5:
            raise ValueError(f"{obj.name}: keep the layout on the game plane")
        width, height, _ = entry["size"]
        x, y, z, w = entry["rotation"]
        rotation = Quaternion((w, x, y, z))
        corners = [rotation @ Vector((dx*width/2, dy*height/2, 0)) for dx, dy in
                   ((-1, -1), (1, -1), (1, 1), (-1, 1))]
        for vertex in item.data.vertices:
            actual = TO_WORKSPACE @ (item.matrix_world @ vertex.co - center)
            closest = min(range(len(corners)), key=lambda i: (corners[i] - actual).length)
            if (corners[closest] - actual).length > 1e-5:
                raise ValueError(f"{obj.name}: keep the source quad dimensions and orientation")
            corners.pop(closest)
        if (location - Vector(entry["position"])).length > 1e-5:
            if "components.Ground" not in entry["components"]:
                raise ValueError(f"{obj.name}: only existing ground tiles may move")
            moves.append({"sourceId": source_id, "to": [round(location.x, 6), round(location.y, 6)]})
    if seen != expected.keys():
        raise ValueError("One or more exported Game Lab objects are missing")
    if not 1 <= len(moves) <= 16:
        raise ValueError("Move between one and sixteen existing ground tiles")
    moves.sort(key=lambda item: item["sourceId"])
    output.write_text(json.dumps({"snapshot": original, "moves": moves}, allow_nan=False), encoding="utf-8")


def main():
    mode, source, output = sys.argv[sys.argv.index("--") + 1:]
    if mode == "import":
        read_scene(Path(output))
    elif mode == "export":
        write_scene(Path(source), Path(output))
    elif mode == "import-game":
        read_game_layout(Path(output))
    elif mode == "export-game":
        write_game_layout(Path(source), Path(output))
    else:
        raise ValueError("Choose import or export")


if __name__ == "__main__":
    main()
