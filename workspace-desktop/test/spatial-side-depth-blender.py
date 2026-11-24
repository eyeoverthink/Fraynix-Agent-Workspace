
"""Capture six Blender faces and check how a perpendicular observation changes the mesh."""
import base64
import json
from pathlib import Path
import runpy
import sys

import bpy
from mathutils import Vector

opposed = runpy.run_path(str(Path(__file__).with_name("spatial-multidepth-blender.py")))
tools = opposed["tools"]
GRID, STEP = opposed["GRID"], opposed["STEP"]
setup, render, volume, surface, distance, mask = (
    tools[name] for name in ("setup", "render", "volume", "sample_surface", "distance", "silica")
)
cube, boolean, vec = (tools[name] for name in ("cube", "boolean", "vec"))
DIRECTIONS = ("+x", "-x", "+y", "-y", "+z", "-z")


def bracket(side=True, cavity=False):
    model = opposed["fixture"]()
    if side:
        boolean(model, cube((11, 8, 7), (14, 10, 9), "Perpendicular recess"), "DIFFERENCE")
    if cavity:
        boolean(model, cube((7, 4, 8), (8, 5, 9), "Enclosed cavity"), "DIFFERENCE")
    bpy.context.view_layer.update()
    return model


def ray(face, column, row):
    """Convert a registered row and column to Blender world coordinates."""
    near = GRID if face[0] == "+" else 0
    far = GRID - near
    if face[1] == "x":
        start, end = (near, GRID - row, GRID - column), (far, GRID - row, GRID - column)
    elif face[1] == "y":
        start, end = (column, near, row), (column, far, row)
    else:
        start, end = (column, GRID - row, near), (column, GRID - row, far)
    return vec(start), (vec(end) - vec(start)).normalized()


def grids(scene):
    graph = bpy.context.evaluated_depsgraph_get()
    result = {}
    for face in DIRECTIONS:
        values = []
        for row in range(GRID):
            for column in range(GRID):
                start, direction = ray(face, column + .5, row + .5)
                hit, position, *_ = scene.ray_cast(graph, start, direction, distance=GRID * STEP)
                values.append(round((position - start).dot(direction) * 1e6) if hit else -1)
        result[face] = values
    return result


def create(output):
    output.mkdir(parents=True, exist_ok=True)
    scene, camera = setup()
    bracket()
    views = {}
    for axis in "xyz":
        target = output / f"side-depth-{axis}.png"
        render(scene, camera, axis, target)
        views[axis] = base64.b64encode(target.read_bytes()).decode("ascii")
    samples = grids(scene)
    package = {
        "format": "fraynix.orthographic-silhouettes-axis-depth-v4",
        "title": "Bracket with three recesses",
        "grid": GRID, "cellM": STEP, "views": views,
        "depthViews": [
            {"axis": face, "origin": ("positive" if face[0] == "+" else "negative") + "-grid-face",
             "unit": "um", "samples": samples[face]}
            for face in DIRECTIONS
        ]
    }
    (output / "side-depth.json").write_text(
        json.dumps(package, separators=(",", ":")) + "\n", encoding="utf-8", newline="\n"
    )


def first(tree, face, column, row):
    start, direction = ray(face, column, row)
    return tree.ray_cast(start, direction, GRID * STEP)[3]


def side_probe(tree):
    # Probe off-center inside the new recess and its right-hand neighbor.
    return first(tree, "+x", 7.41, 6.63) - first(tree, "+x", 6.43, 6.63)


def oblique(scene, camera, target):
    eye = Vector((4, -.75, 1.5)).normalized()
    camera.location = eye * 4
    camera.rotation_euler = (-eye).to_track_quat('-Z', 'Y').to_euler()
    scene.render.filepath = str(target)
    bpy.ops.render.render(write_still=True)


def compare_rays(original, recovered):
    measured = {}
    for face in DIRECTIONS:
        hits = mismatches = 0
        error = 0
        for row in range(1, 15):
            for col in range(1, 15):
                a = first(original, face, col + .31, row + .68)
                b = first(recovered, face, col + .31, row + .68)
                hits += a is not None
                if (a is None) != (b is None):
                    mismatches += 1
                elif a is not None:
                    error = max(error, abs(a - b))
        measured[face] = {"rays": 196, "sourceHits": hits, "missDisagreements": mismatches, "maxHitErrorM": error}
    return measured


def finish(scene, model, output):
    for obj in scene.objects:
        obj.select_set(False)
    model.select_set(True)
    bpy.context.view_layer.objects.active = model
    bpy.ops.wm.save_as_mainfile(filepath=str(output / "editable-three-recesses.blend"))


def verify(output):
    scene, camera = setup()
    original = bracket()
    original_tree, original_points = surface(original)
    original_volume = volume(original)
    source_depth = grids(scene)
    truth_depth = side_probe(original_tree)
    oblique(scene, camera, output / "withheld-side-original.png")
    bpy.data.objects.remove(original, do_unlink=True)

    control = bracket(side=False)
    control_depth = grids(scene)
    bpy.data.objects.remove(control, do_unlink=True)
    cavity = bracket(cavity=True)
    cavity_equal = grids(scene) == source_depth
    cavity_delta = original_volume - volume(cavity)
    bpy.data.objects.remove(cavity, do_unlink=True)

    opposed_model, group = opposed["import_mesh"](output / "opposed-only.glb")
    opposed_tree, _ = surface(opposed_model)
    opposed_depth = side_probe(opposed_tree)
    opposed_volume = volume(opposed_model)
    oblique(scene, camera, output / "withheld-side-opposed.png")
    for obj in group:
        bpy.data.objects.remove(obj, do_unlink=True)

    recovered, _ = opposed["import_mesh"](output / "three-recesses.glb")
    recovered_tree, recovered_points = surface(recovered)
    side_depth = side_probe(recovered_tree)
    recovered_volume = volume(recovered)
    max_surface_error = max(distance(original_tree, recovered_points), distance(recovered_tree, original_points))
    rays = compare_rays(original_tree, recovered_tree)
    oblique(scene, camera, output / "withheld-side-recovered.png")
    a, b = (mask(output / f"withheld-side-{name}.png") for name in ("original", "recovered"))
    iou = sum(x and y for x,y in zip(a,b)) / sum(x or y for x,y in zip(a,b))
    view_changes = {
        face: sum(a != b for a,b in zip(source_depth[face],control_depth[face]))
        for face in DIRECTIONS
    }

    passed = (
        view_changes["+x"] == 4 and sum(view_changes.values()) == 4
        and abs(truth_depth - .10) <= 1e-6 and abs(side_depth - truth_depth) <= 1e-6
        and abs(opposed_depth) <= 1e-6 and abs(opposed_volume - recovered_volume - .001) <= 1e-6
        and abs(recovered_volume - original_volume) <= 1e-6 and max_surface_error <= 1e-5
        and all(item["missDisagreements"] == 0 and item["maxHitErrorM"] <= 1e-6 for item in rays.values())
        and iou >= .995 and cavity_equal and abs(cavity_delta - STEP ** 3) <= 1e-6
    )
    evidence = {
        "blender": bpy.app.version_string, "inputGridChangesFromSideRecess": view_changes,
        "originalVolumeM3": original_volume, "opposedOnlyVolumeM3": opposed_volume, "recoveredVolumeM3": recovered_volume,
        "sideRecessM": {"original": truth_depth, "opposedOnly": opposed_depth, "recovered": side_depth},
        "withheldRays": rays, "withheldObliqueSilhouetteIou": iou,
        "maxSampledSurfaceErrorM": max_surface_error, "enclosedCavityVolumeM3": cavity_delta,
        "enclosedCavityPreservesAllSixGrids": cavity_equal, "passed": passed
    }
    (output / "side-depth-blender-evidence.json").write_text(
        json.dumps(evidence, indent=2) + "\n", encoding="utf-8", newline="\n"
    )
    if not passed:
        raise ValueError(evidence)
    finish(scene, recovered, output)


if __name__ == "__main__":
    mode, target = sys.argv[sys.argv.index("--") + 1:]
    output = Path(target).resolve()
    if mode == "create":
        create(output)
    elif mode == "verify":
        verify(output)
    else:
        raise ValueError(mode)
