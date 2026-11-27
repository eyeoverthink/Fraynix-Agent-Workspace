"""Build and read back Fraynix's original articulated Courier character."""
import json
import math
import sys
from pathlib import Path

import bpy
from mathutils import Vector


POSES = {
    "idle": (1, 0, 0, 0, 0),
    "blink": (4, 0, 0, 0, 1),
    "walk-1": (12, 28, 7, 0.08, 0),
    "walk-2": (16, 4, 26, 0.04, 0),
    "walk-3": (20, -28, 7, 0.08, 0),
    "walk-4": (24, -4, 26, 0.04, 0),
    "takeoff": (32, 10, 32, -0.08, 0),
    "airborne": (38, -23, 35, 0.27, 0),
    "landing": (44, 16, 38, -0.12, 0),
}
PALETTES = {
    "teal": ((0.025, 0.54, 0.49), (1.0, 0.43, 0.08)),
    "amber": ((0.82, 0.31, 0.05), (0.14, 0.72, 0.94)),
}


def material(name, color, metallic=0, emission=0):
    value = bpy.data.materials.new(name)
    value.diffuse_color = (*color, 1)
    value.use_nodes = True
    node = value.node_tree.nodes.get("Principled BSDF")
    node.inputs["Base Color"].default_value = (*color, 1)
    node.inputs["Metallic"].default_value = metallic
    node.inputs["Roughness"].default_value = 0.37
    node.inputs["Emission Color"].default_value = (*color, 1)
    node.inputs["Emission Strength"].default_value = emission
    return value


def build(palette, folder):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.world = bpy.data.worlds.new("Studio bounce")
    scene.world.color = (0.25, 0.25, 0.25)
    main, accent = PALETTES[palette]
    shell = material("Painted shell", main, 0.3)
    ceramic = material("Warm ceramic", (0.87, 0.8, 0.61))
    joint = material("Flexible navy joints", (0.023, 0.047, 0.075))
    glass = material("Visor glass", (0.011, 0.035, 0.072), 0.32)
    mark = material("Signal orange" if palette == "teal" else "Signal blue", accent, 0.12)
    eye = material("Ice blue eye display", (0.24, 0.93, 1.0), emission=1.3)

    skeleton = bpy.data.armatures.new("Courier joints")
    rig = bpy.data.objects.new("Courier rig", skeleton)
    scene.collection.objects.link(rig)
    bpy.context.view_layer.objects.active = rig
    rig.select_set(True)
    rig.show_in_front = True
    skeleton.display_type = "STICK"
    bpy.ops.object.mode_set(mode="EDIT")

    def bone(name, head, tail, parent=None):
        entry = skeleton.edit_bones.new(name)
        entry.head = head
        entry.tail = tail
        if parent:
            entry.parent = skeleton.edit_bones[parent]

    bone("root", (0, 0, 0), (0, 0, 0.24))
    bone("torso", (0, 0, 0.78), (0, 0, 1.39), "root")
    bone("head", (0, 0, 1.40), (0, 0, 2.03), "torso")
    for side, x in (("L", -1), ("R", 1)):
        bone("upper_arm." + side, (0.40*x, 0, 1.34), (0.52*x, 0, 1.04), "torso")
        bone("lower_arm." + side, (0.52*x, 0, 1.04), (0.54*x, 0, 0.78), "upper_arm." + side)
        bone("thigh." + side, (0.21*x, 0, 0.86), (0.21*x, 0, 0.51), "root")
        bone("shin." + side, (0.21*x, 0, 0.51), (0.21*x, 0, 0.18), "thigh." + side)
        bone("boot." + side, (0.21*x, 0, 0.18), (0.21*x, 0, 0.06), "shin." + side)
    bpy.ops.object.mode_set(mode="OBJECT")

    def part(name, point, dimensions, finish, attach, ball=False, bevel=0.045):
        if ball:
            bpy.ops.mesh.primitive_uv_sphere_add(segments=20, ring_count=12, location=point)
        else:
            bpy.ops.mesh.primitive_cube_add(size=2, location=point)
        item = bpy.context.object
        item.name = name
        item.scale = tuple(size / 2 for size in dimensions)
        bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
        if not ball:
            rounding = item.modifiers.new("Machined edges", "BEVEL")
            rounding.width, rounding.segments = bevel, 4
            item.modifiers.new("Weighted normals", "WEIGHTED_NORMAL")
        for face in item.data.polygons:
            face.use_smooth = True
        if finish is not None:
            item.data.materials.append(finish)
        if attach:
            world = item.matrix_world.copy()
            item.parent = rig
            item.parent_type = "BONE"
            item.parent_bone = attach
            bpy.context.view_layer.update()
            item.matrix_world = world
        return item

    part("Hip housing", (0, 0, 0.84), (0.55, 0.39, 0.27), joint, "root")
    part("Torso shell", (0, 0, 1.13), (0.76, 0.47, 0.56), shell, "torso", bevel=0.13)
    part("Chest plate", (0, -0.217, 1.15), (0.5, 0.13, 0.36), ceramic, "torso", bevel=0.065)
    part("Signal badge", (0.106, -0.286, 1.195), (0.15, 0.03, 0.15), mark, "torso", True)
    part("Battery pack", (0, 0.27, 1.16), (0.57, 0.24, 0.5), joint, "torso", bevel=0.08)
    part("Neck swivel", (0, 0, 1.43), (0.29, 0.3, 0.21), joint, "head")
    part("Helmet", (0, -0.008, 1.78), (0.94, 0.62, 0.66), ceramic, "head", bevel=0.18)
    part("Visor rim", (0, -0.287, 1.8), (0.84, 0.15, 0.43), shell, "head", bevel=0.075)
    part("Visor", (0, -0.365, 1.8), (0.72, 0.09, 0.36), glass, "head", bevel=0.043)
    part("Antenna", (0.26, 0.02, 2.14), (0.044, 0.044, 0.20), joint, "head", bevel=0.011)
    part("Antenna tip", (0.26, 0.02, 2.25), (0.102, 0.102, 0.104), mark, "head", True)
    blink_keys = []
    brows = []
    for side, x in (("L", -1), ("R", 1)):
        part("Ear cap " + side, (0.455*x, 0.01, 1.76), (0.13, 0.26, 0.25), shell, "head", True)
        display = part("Eye " + side, (0.155*x, -0.417, 1.826), (0.11, 0.045, 0.157), eye, "head", True)
        display.shape_key_add(name="Open")
        key = display.shape_key_add(name="Blink")
        for point in key.data:
            point.co.z *= 0.12
        blink_keys.append(key)
        brows.append(part("Brow " + side, (0.155*x, -0.421, 1.952), (0.165, 0.027, 0.036), eye, "head", True))
        part("Smile corner " + side, (0.055*x, -0.418, 1.735), (0.044, 0.022, 0.037), eye, "head", True)
        part("Shoulder " + side, (0.428*x, 0, 1.32), (0.28, 0.31, 0.25), mark, "upper_arm." + side, True)
        part("Upper arm " + side, (0.486*x, 0, 1.17), (0.18, 0.22, 0.28), shell, "upper_arm." + side)
        part("Elbow " + side, (0.52*x, 0, 1.04), (0.17, 0.2, 0.18), joint, "lower_arm." + side, True)
        part("Arm cuff " + side, (0.542*x, 0, 0.929), (0.215, 0.245, 0.23), ceramic, "lower_arm." + side)
        part("Glove " + side, (0.557*x, 0, 0.785), (0.18, 0.2, 0.18), joint, "lower_arm." + side, True)
        part("Thigh " + side, (0.21*x, 0, 0.70), (0.21, 0.26, 0.34), shell, "thigh." + side)
        part("Knee " + side, (0.21*x, -0.057, 0.5), (0.18, 0.23, 0.17), mark, "shin." + side, True)
        part("Shin " + side, (0.21*x, 0, 0.365), (0.23, 0.25, 0.32), ceramic, "shin." + side)
        part("Boot " + side, (0.21*x, -0.087, 0.128), (0.29, 0.45, 0.22), shell, "boot." + side)
        part("Sole " + side, (0.21*x, -0.087, 0.038), (0.295, 0.455, 0.071), joint, "boot." + side, bevel=0.026)
    part("Smile", (0, -0.42, 1.713), (0.13, 0.022, 0.037), eye, "head", True)

    proxy = part("Upright collision proxy", (0, 0, 1.1), (0.76, 0.64, 2.2), None, None)
    proxy.modifiers.clear()
    proxy.display_type = "WIRE"
    proxy.hide_render = True
    bpy.ops.rigidbody.object_add()
    proxy.rigid_body.collision_shape = "BOX"
    proxy.rigid_body.kinematic = True
    proxy["purpose"] = "Independent collision proxy. Tile Workshop uses its own fixed body."

    def turn(name, x=0, y=0, z=0):
        joint_pose = rig.pose.bones[name]
        joint_pose.rotation_mode = "XYZ"
        joint_pose.rotation_euler = tuple(math.radians(a) for a in (x, y, z))
        joint_pose.keyframe_insert("rotation_euler", frame=frame)

    for name, (frame, stride, flex, rise, blink) in POSES.items():
        scene.timeline_markers.new(name, frame=frame)
        root = rig.pose.bones["root"]
        # Its local Y axis points up the body.
        root.location.y = rise
        root.keyframe_insert("location", frame=frame)
        turn("torso", stride * 0.14, 0, stride * 0.12)
        turn("head", -stride * 0.13, -stride * 0.14, stride * 0.21)
        for index, side in enumerate(("L", "R")):
            sign = (-1)**index
            knee = max(0, sign * stride * 0.48) + flex
            turn("thigh." + side, sign * stride - flex * 0.44)
            turn("shin." + side, knee)
            turn("boot." + side, -min(35, knee + sign * stride * 0.62))
            turn("upper_arm." + side, -sign * stride * 0.95)
            turn("lower_arm." + side, -9 - max(0, sign * stride * 0.36))
        for index, key in enumerate(blink_keys):
            key.value = blink
            key.keyframe_insert("value", frame=frame)
            brow = brows[index]
            brow.rotation_mode = "XYZ"
            brow.rotation_euler.z = math.radians((-1)**index * (13 if name == "takeoff" else 0))
            brow.keyframe_insert("rotation_euler", frame=frame)

    def aim(obj, at):
        obj.rotation_euler = (Vector(at) - obj.location).to_track_quat("-Z", "Y").to_euler()

    bpy.ops.object.camera_add(location=(3.8, -8.4, 2.56))
    camera = bpy.context.object
    aim(camera, (0, 0, 1.26))
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 2.95
    scene.camera = camera

    for name, pos, power, size, color in (
        ("Key softbox", (-3, -5, 5.7), 525, 4, (1, 0.89, 0.74)),
        ("Fill softbox", (4, -1, 3.4), 315, 3, (0.63, 0.87, 1)),
        ("Rim light", (0, 3, 4), 490, 3, (0.55, 0.77, 1)),
    ):
        bpy.ops.object.light_add(type="AREA", location=pos)
        light = bpy.context.object
        light.name = name
        light.data.energy, light.data.size, light.data.color = power, size, color
        aim(light, (0, 0, 1))

    scene.render.engine = "CYCLES"
    scene.cycles.samples = 12
    scene.cycles.use_denoising = True
    scene.render.threads_mode = "FIXED"
    scene.render.threads = 4
    scene.render.resolution_x, scene.render.resolution_y = 96, 112
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.filepath = "//"
    scene.frame_end = 44
    scene.frame_set(1)
    bpy.ops.object.select_all(action="DESELECT")
    rig.select_set(True)
    bpy.context.view_layer.objects.active = rig
    bpy.ops.wm.save_as_mainfile(filepath=str(folder / "courier.blend"))
    for name, (frame, *_) in POSES.items():
        scene.frame_set(frame)
        scene.render.filepath = str(folder / (name + ".png"))
        bpy.ops.render.render(write_still=True)


def inspect(folder):
    scene = bpy.context.scene
    rig = bpy.data.objects["Courier rig"]
    proxy = bpy.data.objects["Upright collision proxy"]
    markers = {marker.name: marker.frame for marker in scene.timeline_markers}
    assert markers == {name: values[0] for name, values in POSES.items()}, "Pose markers changed"
    blink = bpy.data.objects["Eye L"].data.shape_keys.key_blocks["Blink"]
    scene.frame_set(markers["idle"])
    opened = blink.value
    scene.frame_set(markers["blink"])
    closed = blink.value
    assert opened == 0 and closed == 1, "Missing animated blink"
    stride = []
    for name in ("walk-1", "walk-3"):
        scene.frame_set(markers[name])
        stride.append(round(math.degrees(rig.pose.bones["thigh.L"].rotation_euler.x), 3))
    assert stride[0] > 20 and stride[1] < -20, "Missing opposite leg poses"
    assert proxy.parent is None and proxy.hide_render and proxy.rigid_body.kinematic
    assert proxy.rigid_body.collision_shape == "BOX"
    assert bpy.data.objects["Torso shell"].parent_bone == "torso"
    info = {
        "blenderVersion": bpy.app.version_string, "bones": len(rig.data.bones),
        "markers": markers, "blinkWeights": [opened, closed], "leftThighDegrees": stride,
        "kinematicBox": True,
    }
    (folder / "inspection.json").write_text(json.dumps(info), encoding="utf-8")


if __name__ == "__main__":
    where = sys.argv.index("--")
    action, target, *options = sys.argv[where + 1:]
    if action == "build":
        build(options[0], Path(target))
    elif action == "inspect":
        inspect(Path(target))
    else:
        raise ValueError("Unknown character job")
