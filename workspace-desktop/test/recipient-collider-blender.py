"""Measure the two actual GLB files imported into a clean Blender scene."""
import hashlib
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector

EPS = 1e-5
BASIS = Matrix.Rotation(math.pi / 2, 3, 'X')


def read_scene(file, fixture, expected_size):
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    bpy.ops.import_scene.gltf(filepath=str(file))
    bpy.context.view_layer.update()
    boxes = {}
    for obj in bpy.context.scene.objects:
        if obj.get('fraynixBody'):
            body = json.loads(obj['fraynixBody'])
            assert body['sourceId'] not in boxes
            boxes[body['sourceId']] = (obj, body)
    prop, properties = boxes.pop(fixture['sourceId'])
    assert len(prop.data.vertices) == 8
    origin = prop.matrix_world.translation
    expected_origin = BASIS @ Vector(fixture['position'])
    assert (origin - expected_origin).length < EPS
    rotation = prop.matrix_world.to_quaternion()
    expected_rotation = Quaternion((0, 0, 1), math.radians(fixture['yawDeg']))
    assert abs(rotation.dot(expected_rotation)) > 1 - EPS

    local = [rotation.inverted() @ (prop.matrix_world @ v.co - origin) for v in prop.data.vertices]
    size = [max(v[i] for v in local) - min(v[i] for v in local) for i in range(3)]
    wanted = [expected_size[0], expected_size[2], expected_size[1]]
    assert max(abs(a-b) for a,b in zip(size,wanted)) < EPS
    mesh, = [obj for obj in prop.children if obj.get('fraynixVisual') == 'triangle-surface']
    assert mesh.data != prop.data
    points = [mesh.matrix_world @ v.co for v in mesh.data.vertices]
    triangles = [tuple(face.vertices) for face in mesh.data.polygons]
    assert len(triangles) > 100 and all(len(face) == 3 for face in triangles)
    for point in points:
        relative = rotation.inverted() @ (point-origin)
        assert all(abs(relative[i]) <= size[i]/2+EPS for i in range(3))
    neighbors = {key:(body,[obj.matrix_world @ v.co for v in obj.data.vertices])
                 for key,(obj,body) in boxes.items()}
    return size, points, triangles, neighbors, properties


def main(folder):
    fixture = json.loads((folder/'blender-input.json').read_text(encoding='utf-8'))
    final = folder/'collider-final.glb'
    digest = hashlib.sha256(final.read_bytes()).hexdigest()
    assert digest == fixture['glbSha256']
    before_size, before_mesh, old_faces, old_neighbors, old_properties = read_scene(
        folder/'incoming-final.glb',fixture,fixture['old'])
    after_size, after_mesh, new_faces, new_neighbors, new_properties = read_scene(final,fixture,fixture['changed'])
    assert old_faces == new_faces and len(before_mesh) == len(after_mesh)
    drift = max((a-b).length for a,b in zip(before_mesh,after_mesh))
    assert drift < EPS
    assert old_properties == new_properties and old_neighbors.keys() == new_neighbors.keys()
    for key,(old_data,old_points) in old_neighbors.items():
        new_data, new_points = new_neighbors[key]
        assert old_data == new_data and len(old_points) == len(new_points)
        assert max((a-b).length for a,b in zip(old_points,new_points)) < EPS

    proof = {'blenderVersion':bpy.app.version_string,'glbSha256':digest,
             'oldColliderBlenderXYZM':before_size,'newColliderBlenderXYZM':after_size,
             'visualTriangles':len(new_faces),'maxVisualWorldVertexDifferenceM':drift,
             'otherBodiesMatched':len(new_neighbors),'visualFullyEnclosed':True}
    (folder/'blender-collider-proof.json').write_text(json.dumps(proof,indent=2)+'\n',encoding='utf-8')


main(Path(sys.argv[sys.argv.index('--') + 1]))
