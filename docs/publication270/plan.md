# Edition 270 design review and verification

## How the asset moves through Fraynix

The original procedural Blender source makes an editable articulated model with two palettes and an isolated collision box. The eyes use shape keys and the brows and joints use keyframes. Fraynix launches Blender in background mode, reopens the saved project in a separate process and checks the armature, key markers, blink values, opposing leg rotations and the independent kinematic box. Java checks each 96 × 112 PNG for dimensions, transparency, margins and distinct decoded visible pixels. It builds the shared 288 × 336 atlas and the four-frame walk sheet.

Each saved rig binds the Blender project, inspection and nine frames by SHA-256. A restart checks the retained bytes. Re-baking may yield a new identity. A portable level carries the approved visual bytes and plays with the existing 120 Hz tile controller. Face, jump and landing poses are exposed in Avatar Studio and in the exported atlas. Tile Workshop currently displays idle and walk cells. Its jump remains a real controller action.

A stable body should own the player's position, contacts and forces. A separate skeleton should own how the character looks. Fraynix has both patterns already. Tile Workshop uses a fixed 24 × 32 collision body under direct control. The native Game Lab's Relay character uses JBox2D's dynamic pillbox with rotation locked, separate from its SpriteRenderer and StateMachine. The new Blender box is an authoring proxy. The current handoff goes to Tile Workshop. Blender shape keys can support future blinking, expression and mouth controls. [Blender rigid bodies](https://docs.blender.org/manual/en/latest/physics/rigid_body/introduction.html) · [Blender shape keys](https://docs.blender.org/manual/en/latest/animation/shape_keys/introduction.html)

## What the independent reviewer would build next

1. **One controller across art styles.** Reuse the Game Lab dynamic body with a content-addressed pose pack. Use a movable crate and an identical no-contact control to prove a reciprocal push. Save grounded, contact, impulse and displacement evidence. Reopen and replay offline.
2. **Movement from measured state.** Give new tile motion a versioned replay path. Choose takeoff, rise, fall and landing from recorded support and vertical velocity. Choose the walk phase from travel distance, and anchor the images at the feet. Verify left and right approaches and preserve old renderer identities.
3. **Faces and visual choices.** Expose eye, brow and mouth landmarks for imported images. Keep authored stylized rigs alongside those portrait controls. Use photos and voice only when provided for the project. Add a character-specific interchange for bones, pose and expression data. Blender can carry pose-bone and shape-key animation in glTF. [Blender glTF documentation](https://docs.blender.org/manual/en/5.1/addons/import_export/scene_gltf2.html)
4. **One reusable set of art and mechanics.** Grow original kits with a consistent preview, palette, material, layer and collider. Use the same assets for a courier platformer, a warehouse with pushable crates and a room navigation rehearsal. A kit should ship with at least one playable level and independent checks for scene, collisions and export.
5. **Measure creation and play separately.** Start from full bake time, reopen time and end-to-end playtest and export. Compare short and long pose packs. Cache verified derived frames if decoding or rendering dominates. Retain source, baker and result identities and test changes to each.

The reviewer was independent and read the code. The desktop tests below provide the execution evidence. [Native Relay character composition](../../src/main/java/content/RelayGardenGenerator.java#L147) · [Tile renderer](../../workspace-desktop/renderer/media-tile-play.js) · [Existing avatar replay test](../../workspace-desktop/test/avatar-player-integration.cjs)

## Results and reproduction

On Windows, Blender 5.3.0 baked and reopened Teal in **5.4 seconds** and Amber in **5.0 seconds** as measured by the desktop test on this machine. Both produced nine verified frames with distinct palettes. The test verified UI exports, refused duplicate and changed frames, reopened the rig after restart, compared exported HTML hashes and played the standalone course with networking blocked. The existing portrait player and edition 269 traversal integration also passed locally. The previous hosted matrix completed its avatar repair and traversal steps across all four targets. Its full run was affected by later spatial checks and an Intel Mac cancellation. The new Blender authoring path was exercised on Windows. [Previous portable CI run](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/46078563345)

Prepare with `./gradlew :workspace-engine:installDist` on macOS/Linux or `gradlew.bat :workspace-engine:installDist` on Windows, and `npm --prefix workspace-desktop ci`. With Blender installed, run `node workspace-desktop/test/character-rig-integration.cjs build/character-rig-test`. On Linux without a desktop, use a virtual display. For this course, use the arrow keys to collect the signal, then press Space at the hurdle.

[Prior edition](../../README-269.md) · [Downloadable files and evidence](../readmes/270/index.html)
