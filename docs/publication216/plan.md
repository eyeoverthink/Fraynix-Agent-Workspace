# Edition 216 scene exchange and reconstruction path

## What ships

Open **Spatial → Scene exchange and image blueprints**. A labelled PNG already imported in Maps can become a 3D scene. Select its reference, enter the tile width in metres, and choose **Build 3D copy**. The map compiler preserves the original floor and wall cell positions. It merges wall runs into boxes, uses an authored wall height of 1.1 m, and adds one movable marker. Top down displays the same scene with the image’s column direction on X and row direction on Z.

**Export GLB** writes a self-contained glTF 2.0 file from the selected authored scene, with the file’s hash logged beside the source. In Blender, import the GLB, keep the metadata empty and box custom properties, then export as GLB with **Custom Properties** enabled. The **Import Fraynix GLB** control opens the result as a new editable scene. In the current format Java checks every box’s triangles, world transform and body properties. Blender hierarchy is composed into world poses. Original source identities travel with the boxes and each imported scene gets its own object IDs. Fraynix retains the imported GLB bytes and rechecks them when the scene is opened or bound to a task. [glTF 2.0 specification](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html) · [Blender glTF manual](https://docs.blender.org/manual/en/5.1/addons/import_export/scene_gltf2.html)

Select the dynamic marker to bind it to the existing Spatial agent tool. The same Java physics preview and native grab, move, release, endpoint and browser report checks apply on the new copy.

## What the other engines teach us

| Reference | Design we can bring into Fraynix | Next measurable check |
| --- | --- | --- |
| Blender | Keep visual assets and editable transforms together in an open exchange file. Use Blender to edit and independently inspect a result. [Blender glTF manual](https://docs.blender.org/manual/en/5.1/addons/import_export/scene_gltf2.html) | Move an object through a rotated parent, reimport it and compare the world-space pose and bounds. |
| Unreal and Godot | Treat authored geometry and collision as separate representations. Import the concave visual mesh alongside a declared simple collider. [Unreal Interchange](https://dev.epicgames.com/documentation/unreal-engine/interchange-import-reference-in-unreal-engine) · [Godot import conventions](https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/node_type_customization.html) | Run a contact test and prove it used the reviewed collider even when the visual shell extends beyond it. |
| OpenUSD | Keep original scene data and store reviewed edits as overrides with clear provenance. [OpenUSD composition](https://openusd.org/files/Siggraph2019_USD%20Composition.pdf) | Duplicate a scene, save one pose edit, and verify that discard and replay agree with the original and edited state respectively. |
| COLMAP | Use overlapping viewpoints, solve camera poses and match shared observations before adding measured surfaces. [COLMAP reconstruction tutorial](https://colmap.github.io/tutorial.html) | Reconstruct a calibrated tabletop from several views, compare held-out projections and known dimensions, then approve a limited collision region. |

Use recognizable scene and game workflows as quality targets while retaining Fraynix’s original content and UI. Java owns object identity, physics authority, source checks and agent actions. The portable exchange path uses no added codec dependency. Blender is an independent editing and verification tool for this release.

## Reproduce

After installing the workspace prerequisites, run the desktop integration. Set FRAYNIX_TEST_BLENDER to an installed Blender executable to include the external edit and reimport. The test generates its own scene and leaves your Blender projects alone.

~~~sh
node workspace-desktop/test/spatial-exchange-integration.cjs build/spatial-exchange
~~~

## Boundary of this edition

The GLB importer handles closed oriented boxes with Fraynix metadata, up to 32 bodies and 512 KiB. Materials, cameras and animation are outside the editable physics copy. Image conversion takes an explicitly labelled top-down layout with declared dimensions. Floor and wall coverage can be verified against that image. Reconstruction from ordinary pictures and inference about hidden geometry are separate measurement problems.

## Next acceptance test

Bring in one original Blender prop with a detailed visual mesh and a separately authored simple collider. Render both in the same scene with the same stable object ID. Let the agent copy and transform the prop, verify contacts against the declared collider, and export the edited copy to Blender. After that, use calibrated multi-view observations to place and validate a real object in that same scene path.
