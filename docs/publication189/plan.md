# Edition 189 implementation and qualification

## Use Blender as an authoring and inspection tool

Blender owns loading and writing its native projects through its documented background API. Fraynix owns the data contract, scene registration, actions and outcome verification. Electron opens a local file picker and runs the checked-in adapter with Blender's embedded Python and automatic scripts disabled. The bridge accepts one blend file up to 32 MiB, uses a private temporary copy, and bounds the execution to one minute. A configured or installed Blender is used only when the user invokes the exchange. [Blender command line](https://docs.blender.org/manual/en/dev/advanced/command_line/arguments.html) · [Loader source](https://github.com/blender/blender/blob/main/source/blender/blenloader/intern/readfile.cc)

The Spatial contract carries rigid box identities, labels, world-space position and rotation, dimensions, mass, friction, restitution, gravity and the declared scale reference. Blender's world transforms map to Fraynix with (x, y, z) → (x, z, −y). The adapter preserves the physical handedness, converts scene units to metres, and passes the exact data through Java's existing scale and shape checks. Source labels and the SHA-256 of the copy used for exchange stay in the registered scene. Fraynix can rebuild those values into an editable Blender project. Java assigns a new runtime identity for a new import and retains the original source IDs. [Blender object API](https://docs.blender.org/api/current/bpy.types.Object.html)

Game Lab exports its paused runtime through the existing owner-thread spatial snapshot. Java retains the tick, session, component names, stable IDs, drawable geometry and raster checksum. The Blender file contains editable planar meshes and the original Java package as a text block. Game coordinates retain their game-unit designation. Java checks the text block's snapshot checksum after Blender read-back.

## Qualify the whole path

Author an original Blender fixture independently of the adapter. Import it through Spatial, test centimetres and a rotated obstacle, re-export it, and compare its reimported transforms with the original. Refuse the same fixture with either a mismatched scale reference or a sphere collider. Run a native grab, move and release on the reimported scene, independently check the run and open its eight-check browser report. Restart and confirm the source and procedure are durable. Export Relay Garden after moving the original game player and compare every generated mesh against the source object ID, position, geometry and component names. Refuse changed snapshot evidence and refuse the game-unit layout as a metre scene.

## Continue toward the original goal

1. Register a Blender-authored fixture against independent mat or camera measurements, with a scale residual and an error bound carried into Java.
2. Export a series of Game Lab or native world frames as a labelled Blender animation. Keep camera observations, simulated outcomes and rendered labels traceable to their original sources.
3. Import edited, explicitly tagged Game Lab geometry as a new candidate. Compile only supported gameplay components, then require actual game playtests before accepting the candidate.
4. Give the agent an explicit scene and replay selection so a durable task can construct, test, revise and return an editable artifact. Keep episode evidence attached when a procedure is retained.

## Reproduce locally

Set FRAYNIX_BLENDER to the full executable path, configure JDK 21, and install desktop dependencies using the normal setup. Use a fresh output directory.

~~~powershell
.\gradlew.bat --offline :workspace-engine:installDist :prepareGameRuntime115
node docs/publication189/validate-desktop.cjs build/edition189-check
python docs/publication189/validate.py
~~~
