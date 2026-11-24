# Edition 217 detailed prop and collider exchange

## Import, review and verify

1. Use **Spatial → Scene exchange and image blueprints → Import Fraynix GLB** to open the original signal beacon fixture.
2. Select **Signal beacon**. Enable **Show prop colliders** to inspect the smaller box and the rendered surface together.
3. Ask Chat **“Copy and transform the selected prop.”** Name the copy and enter its XYZ offset and yaw. **Review copy** shows the exact target pose and test budget. The local agent workflow uses the selected native object and the saved transform.
4. Select **Approve and verify**. Java creates a fresh scene candidate. It runs two independent 240-tick native worlds, compares the retained results, and commits the edited scene only after both pass.
5. Use **Export GLB** on the edited scene. Open that file in Blender to inspect both props and their individual colliders. Each prop in the copy has a distinct identity. The edited scene holds one shared immutable visual asset.

## Author a fixture in Blender

The GLB contains a scene metadata empty called **Fraynix scene metadata**. It carries the Fraynix scene properties used by edition 216. A visual child uses the string custom property **fraynixVisual** with the value **triangle-surface**. Its parent is an explicit closed box with the **fraynixBody** custom property. Export with **Custom Properties** enabled. The example authoring script shows the metadata, surface, and collider arrangement. [Blender fixture script](../../workspace-desktop/test/spatial-prop-blender.py)

Current exchange supports one indexed surface per prop, with a common metric pose for visual and collision geometry. The surface carries triangle geometry. Fraynix shades it for the viewport. Appearance work such as materials, textures, lights, skinning and animation is a separate extension.

## Collision tests and receipts

- Before committing, Java tests the proposed collider against existing bodies. It refuses intersecting placements.
- A small native probe inside the declared box must report contact. When the source mesh contains a triangle centroid outside the box by the required clearance, a second probe at that point must report zero contacts with the prop.
- After 240 fixed ticks, the dynamic copy must be supported by a static body, with limited drift and terminal speed. The entire witness is repeated in a separate native world.
- The original scene, reviewed transform, mesh identity, committed copy and both native receipts stay bound. Reopening rechecks the retained original GLB and source chain. Repeating an identical approval opens the existing copy.
- Java reimports its exported GLB to check collision parameters, world poses and triangle geometry. In the full desktop test, stock Blender independently reopens the export and compares the two prop meshes in their local frames.

## Reproduce

Set FRAYNIX_TEST_BLENDER to Blender’s executable, then use the prepared Java and desktop distributions:

~~~sh
node workspace-desktop/test/spatial-prop-integration.cjs build/edition217
~~~

The desktop harness generates its own original prop. It imports a separate collider and visual surface, exercises the chat route without a model, tests a refused copy, and checks a real Blender export round trip. Gradle’s SpatialPropExchangeTest uses the retained Blender fixture to run the same native contact logic in CI.

## Next acceptance test

Edit one part of the copied prop and verify which asset and collider changed. Then add measured visual scale from multiple calibrated views, with an explicit collider review before the agent can act on the result.
