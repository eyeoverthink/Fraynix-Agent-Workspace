"""Independently open the exported GLB and check both visual meshes and their box colliders."""
import base64
import hashlib
import json
import math
import struct
import sys
from pathlib import Path

import bpy
from mathutils import Matrix, Quaternion, Vector

folder = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
glb = folder / "image-workshop.glb"
scene = json.loads((folder / "image-workshop.json").read_bytes())
evidence = json.loads((folder / "image-assembly-evidence.json").read_bytes())
digest = hashlib.sha256(glb.read_bytes()).hexdigest()
assert digest == evidence["glbSha256"]
basis = Matrix.Rotation(math.pi / 2, 3, "X")
tolerance = 1e-5

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(glb))
bpy.context.view_layer.update()
tagged = {}
for obj in bpy.context.scene.objects:
    if obj.get("fraynixBody"):
        body = json.loads(obj["fraynixBody"])
        key = body["sourceId"]
        assert key not in tagged
        tagged[key] = obj
assert set(tagged) == {body["sourceId"] for body in scene["objects"]}

rows = []
for body in scene["objects"]:
    collider = tagged[body["sourceId"]]
    origin = collider.matrix_world.translation
    error = (origin - basis @ Vector(body["position"])).length
    assert error < tolerance
    xq, yq, zq, wq = body["rotation"]
    orientation = basis @ Quaternion((wq, xq, yq, zq)).to_matrix() @ basis.inverted()
    collider_rotation = collider.matrix_world.to_quaternion().to_matrix()
    for axis in [(1,0,0),(0,1,0),(0,0,1)]:
        assert ((collider_rotation @ Vector(axis)) - (orientation @ Vector(axis))).length < tolerance
    tangent = collider_rotation @ Vector((1,0,0))
    expected_tangent = orientation @ Vector((1,0,0))
    error_deg = math.degrees(math.atan2(tangent.cross(expected_tangent).length, tangent.dot(expected_tangent)))
    vertices = [orientation.inverted() @ (collider.matrix_world @ v.co - origin) for v in collider.data.vertices]
    dims = Vector([max(p[i] for p in vertices) - min(p[i] for p in vertices) for i in range(3)])
    x, y, z = body["size"]
    assert (dims - Vector((x, z, y))).length < tolerance
    assert len(vertices) == 8

    row = {"sourceId": body["sourceId"], "centerErrorM": error, "boxVertices": len(vertices),
           "yawDeg": math.degrees(math.atan2(tangent.y,tangent.x)), "rotationErrorDeg": error_deg}
    if "visualMesh" in body:
        visual, = [child for child in collider.children if child.get("fraynixVisual") == "triangle-surface"]
        assert visual.type == "MESH" and visual.data != collider.data
        encoded = base64.b64decode(scene["visualMeshes"][body["visualMesh"]])
        count, indices = struct.unpack_from("<II", encoded)
        positions = struct.unpack_from("<" + "f" * (count * 3), encoded, 8)
        wanted = [origin + orientation @ basis @ Vector(positions[i:i+3]) for i in range(0, len(positions), 3)]
        actual = [visual.matrix_world @ v.co for v in visual.data.vertices]
        drift = max(max(min((a-b).length for a in actual) for b in wanted),
                    max(min((a-b).length for b in wanted) for a in actual))
        assert drift < tolerance
        faces = visual.data.polygons
        assert len(faces) == indices // 3 and all(len(face.vertices) == 3 for face in faces)
        row.update(visualTriangles=len(faces), maxVertexErrorM=drift)
    rows.append(row)

proof = {"blenderVersion": bpy.app.version_string, "glbSha256": digest, "bodies": rows}
(folder / "blender-readback.json").write_text(json.dumps(proof, indent=2) + "\n", encoding="utf8")
print(json.dumps(proof))
