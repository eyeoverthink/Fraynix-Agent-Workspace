
# Build editable assets across the production workbench

2026-11-24 · Edition 225

The user expanded the requirements to include sculpting, procedural modeling, node materials, more simulation types, game asset export, print preparation and Python scripting. The workbench should let a person or an agent use the same editing operations, inspect the result in the app, and exchange supported fields with Blender. Edition 225 added native face-region extrusion and metric STL export. This plan extends the earlier [nine-workflow roadmap](../../README-224.md).

## Capability milestones

| Area | Implemented in Fraynix | Next acceptance test |
| --- | --- | --- |
| Hard-surface modeling, environments and characters | Reconstructed meshes, reviewed transforms, editable .blend round trips and planar region extrusion. | Edit vertex, edge and face selections with undo, normals and modifier order. Verify bevels, topology and a collider update after a Blender edit. |
| Sculpting and procedural modeling | The reconstruction lattice builds a surface from observations. | Add brush strokes with symmetry and undo, then dynamic topology and voxel remeshing. Verify surface deviation and component preservation. Evaluate a supported, typed Geometry Nodes subset with repeatable seeds and editable parameters. |
| Rigging and animation | Authored rigid-translation tracks and Blender location keys. | Add rotation, scale and graph curves. Round-trip a character rig with bones, weights, IK targets, shape keys, pose tools and non-linear clips. Verify sampled deformed geometry and specific joint poses. |
| Shading, UVs and texturing | Interactive shaded viewport. | Carry a bounded material node graph, UV seams and a UV or 3D paint edit through Blender. Confirm texture identity, color space and UV placement on the returned mesh. |
| Rendering | Interactive viewport and physics replay video. | Add in-app camera, lights and render controls. Render one production scene locally and cross-check image dimensions, framing and lighting in Blender. Provide explicit Eevee and Cycles rendering routes through the installed runtime before claiming support. |
| Physics and simulation | Native rigid contacts, manipulation, trajectories and checks. | Add editable bakes and constraints. Introduce cloth and soft-body calibration before moving to hair or fur, particles, smoke, fire and fluids. For each solver compare a bounded outcome against an independent run using declared tolerances. |
| VFX, tracking and compositing | Recorded object tracking, occlusion recovery and calibrated planar observations. | Solve a camera with held-out feature tracks. Composite at least two real layers and compare alpha, color and image pixels after a Blender round trip. |
| 2D and video editing | Editable Game Lab scenes, video review and replay export. | Author Grease Pencil-compatible stroke layers in 3D, with frame edits and ordering checks. Add multitrack video cuts and audio, then round-trip the supported Blender sequencer edits with timestamps and frame-order checks. |
| Game assets and print preparation | Static GLB for supported scenes and metric STL of a checked visual shell. | Prove asset import in Unreal and Unity, including pivots, scale, materials, animation, separate colliders and level of detail. Validate thickness and overhangs, export 3MF, and compare a real slicer's layer dimensions. |
| Python scripting and extensibility | The local Blender adapter uses Python for exchange. | Let a user script call the same typed editing commands as the UI, with permission for the selected files, deterministic inputs and a change log. Verify a batch produces the same saved objects and receipts as manual editing. |

## Edition 225 contract

The editor starts from a triangle on a supporting plane. Java expands through edge-connected coplanar faces, lifts the cap by 1–1000 mm, and adds walls along the boundary. It checks degenerate faces, opposite edge incidence, connected vertex fans and signed volume gain. Saving requires the exact preview receipt and creates a separate scene. An old preview is refused if the distance changes. The source triangle asset, simulation state and enclosing box remain independent of the edited visual shell. A Blender round trip in this edition covers visual geometry and supported collider properties. Shading, materials and modifier graphs each require their own exchange checks.

The print export converts the selected local shell to Z-up millimetre coordinates and puts its lowest point at zero. The exporter writes binary STL and the desktop checks the bytes it saves. The test imported the export through Blender's STL reader at metre scale, measured a 600 mm extent, and checked closure and volume. Printability still needs thickness, self-intersection and slicer tests.

[Blender region extrusion reference](https://docs.blender.org/manual/en/4.5/modeling/meshes/tools/extrude_region.html) · [Blender STL import reference](https://docs.blender.org/manual/en/4.1/files/import_export/stl.html) · [Desktop and Blender verification](../readmes/225/mesh-edit-evidence.json)

## Reproduce

Run the Java test and build the engine distribution, then run the desktop integration. Set FRAYNIX_TEST_BLENDER to the Blender executable to check the native file and STL. Set FRAYNIX_TEST_BLENDER_OLDER to Blender 4.5 LTS to run the independent measurements and edit in that version.

    ./gradlew :workspace-engine:installDist :workspace-engine:test --tests '*SpatialFaceExtrusionTest'
    node workspace-desktop/test/spatial-mesh-edit-integration.cjs build/visual-mesh-edit225
