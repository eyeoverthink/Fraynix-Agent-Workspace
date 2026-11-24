
# From reference images to playable media

2026-11-24 · Edition 229

The end-to-end path begins with an image you can point to. Fraynix keeps the source, turns it into an asset, lets you compose a scene, and ships a file you can open independently. The scene graph and its evidence give the agent a way to inspect and revise its work.

## First working path

- **Source.** A trusted click on a visible in-app browser graphic retains normalized captured pixels and a page URL stripped of credentials, query and fragment. A local PNG retains the filename and image bytes.
- **Sprite.** Java crops existing alpha, including semitransparency, or keys the largest connected region from a contrasting plain background. Holes and original retained color survive. The input limit is 750 KB, with sides 8–1024 px. The opaque path is intended for one connected, contrasting subject.
- **Scene.** The Media canvas renders a backdrop, a pickup over a solid raised platform, and a foreground. You can move each layer and the platform in a live draft. Saving checks the permitted geometry and anchors the version to a SHA-256 digest. Reopening checks the full stored package and rebuilds the sprite.
- **Build.** The same fixed-step game runtime drives the Media preview and the single-file HTML. The latter carries its sprite and layout and grants no network resource access. Editable JSON carries the source pixels and recipe for re-use on another install. PNG exports the derived asset alone. Export receipts bind the on-disk bytes to the level and sprite.
- **Witness.** An original graphic with an interior cutout exercises the true browser pick. The independent acceptance checks sample sprite RGBA and the moved draft platform, drive the desktop and exported games with keyboard input, and check restart, editing, transfer to a separate project, reset, and refusal of changed evidence.

## Sequence from here

| Phase | Deliverable | Acceptance check |
| --- | --- | --- |
| Sprite library and map editor | Multiple sources, sprite sheets and animation frames, layer order, drag placement, tile maps and per-object collision shapes | Reuse an asset across two scenes and replay an exported build with several moving or solid objects |
| Image-to-3D branch | Route registered front, side, top or depth captures into Spatial, retain the sprite as an alternate view or billboard, and package the selected visual mesh and collider | Check a separate view before export, then round-trip a composed scene through Blender and replay its collisions |
| Animation and compositing | Share scene and camera tracks across 2D and 3D, with masks, transitions and layered audio | Render a timed shot offline, compare selected frames and sync markers, and reopen the editable timeline |
| Agent authoring and playtest | A reviewed chat request assembles assets, edits a scene, runs bounded playtests or renders and revises after an observed failure | Use an unfamiliar brief, interrupt it between stages, and confirm the final artifact and independent witness after restart |
| Delivery | Web builds, editable bundles, image sequences, GLB scenes and rendered video using established interchange formats | Open exports independently of the workspace, reproduce receipt hashes and identify which editable source produced each build |

A single image can supply a clean 2D asset or a 3D starting hypothesis. A model with hidden geometry needs additional views or depth to support that claim. Keep the held-out evidence outside the fit. Game Lab's original Java courses, the Media preview and Spatial's native 3D tools are existing entry points for the shared asset flow.
