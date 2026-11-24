"""Edit a Fraynix GLB fixture in stock Blender and export its authored copy."""
import bpy
import sys

source, destination = sys.argv[sys.argv.index("--") + 1:]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=source, import_scene_extras=True)
marker = next(obj for obj in bpy.context.scene.objects if obj.name.startswith("Movable blueprint marker"))
assert marker.get("fraynixBody")
group = bpy.data.objects.new("Blender group", None)
bpy.context.scene.collection.objects.link(group)
marker.parent = group
group.location.x = 0.15
bpy.context.view_layer.update()
assert any(obj.get("fraynixScene") for obj in bpy.context.scene.objects)
bpy.ops.export_scene.gltf(filepath=destination, export_format="GLB", export_extras=True)
