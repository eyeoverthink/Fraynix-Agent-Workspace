
# Integrated production workbench

2026-11-24 · Edition 224

The workbench will retain an editable scene, its time-based work, and the evidence behind each operation. The same objects should be usable in modeling, animation, physics and agent tasks. Fraynix owns local authoring, evaluation and verification. Blender participates in import, export and cross-checks. Acceptance requires that people can make changes in Fraynix itself and keep supported edits through a Blender round trip.

## Scope and acceptance

| Workflow | Current Fraynix capability | Native milestone and Blender acceptance test |
| --- | --- | --- |
| Modeling | Reconstructed editable triangles, a separate collider, scene import, reviewed copy and transform. Edition 224 retains detailed meshes in .blend exchange. | Select and edit vertices, edges and faces, with undo and collider refresh. Round-trip topology, local transform, mesh identity and a material assignment. |
| Rigging | Pending. | Author a small joint hierarchy and assign weights in Spatial. Edit a pose in Blender, then recover joint identity, weights and the deformed geometry within tolerance. |
| Animation | Linear rigid-object position keys, scrub, playback, saved scene copy and native .blend round-trip. | Add rotation and scale tracks, curve interpolation, clips, and skeletal motion. Compare complete curves and evaluated frames after edits from either app. |
| Simulation | App-owned rigid-body scenes, manipulation, collisions and verification. Blender exchange carries supported box geometry and physics properties. | Connect a shared timeline to collision baking, constraints and editable playback. Compare declared properties and timed contact or motion results under uncertainty. |
| Rendering | Interactive 3D preview and an existing physics replay video export. | Author cameras, lights and materials. Compare an offline frame sequence against saved framing, color and output dimensions. Keep render settings editable. |
| Compositing | Pending. | Compose two image or render layers in-app with timing, alpha and color controls. Reopen a supported Blender node subset and compare rendered pixels. |
| Motion tracking | Existing object tracking, occlusion recovery and calibrated tabletop observations. | Add editable feature tracks and camera solve where evidence permits. Round-trip feature IDs, frame times, camera poses and reprojection errors. |
| 2D animation | Game Lab has an editable 2D world. Drawing and stroke timelines are pending. | Author frame-based layers, stroke geometry and interpolation. Round-trip a supported Grease Pencil scene and check stroke identity and display order. |
| Video editing | Reviewing footage and exporting existing replay clips. | Add clips, trimming, audio, a multitrack timeline and export. Round-trip supported Blender sequencer edit points and compare timestamps, audio and frame order. |

## Phases

1. Build the common editing foundation around stable object identity, transforms, source hashes, frame times and saved edit copies. Edition 224 uses Java to validate the timeline and generate preview samples. Actual Blender import and export run through the existing local adapter.
2. Add native mesh editing, then rotation and scale animation, followed by rigging. Capture representative Blender round trips at each step, including imports from the older supported runtime.
3. Couple authored and simulated motion through explicitly labelled tracks. Connect recorded observations to editable scene tracks and compare the physical results separately from authored motion.
4. Carry the same scene into rendering, compositing, 2D animation and video assembly. Let reviewed agent requests call the same operations that a person uses. Keep source checks and rendered outcome checks outside model control.

## Exchange contract for edition 224

The built-in .blend exchange carries supported rigid box props, an optional triangulated child surface, and linear XYZ translation. Blender uses Z-up and Fraynix uses Y-up. Authored position keys are in metres. Frame 1 matches the static authored position. To preview authored motion on an exported .blend, the bridge turns off Blender's rigid-body world playback and keeps the body properties. Native Fraynix physics continues to use the original collider and pose. The bridge refuses unsupported object curves, drivers, NLA and animated collider deformation in the translation workflow. The preview pauses when a native study starts and disables native visibility inspection until the user exits the authored view. The imported scene keeps normalized data and a hash of the .blend source in provenance. It reopens locally without requiring the original .blend. Rendering, compositing, rigging, tracking, Grease Pencil and sequencer content each require their own exchange contract.

For the tested fixture, both Blender versions independently sample frames 1, 7, 13, 19 and 25 and edit frame 25. The Java and desktop checks compare the actual centre coordinates, triangle bytes, body dimensions, mass, refusal behavior and the reopened records. The app preview uses Java's frame samples and does not simulate contacts. A static fixture exported through the prior adapter also reopened with its original crate position.

[Blender 4.5 LTS](https://www.blender.org/releases/4-5/) · [Blender glTF animation scope](https://docs.blender.org/manual/en/4.5/addons/import_export/scene_gltf2.html) · [In-app round-trip test](../../workspace-desktop/test/spatial-animation-integration.cjs) · [Measured outcomes](../readmes/224/animation-evidence.json)
