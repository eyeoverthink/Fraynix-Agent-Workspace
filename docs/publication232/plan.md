
# Approve, measure and export a chat-authored prop placement

2026-11-24 · Edition 232

Chat uses a bounded local placement command. It resolves the selected dynamic prop to an original Spatial reconstruction with a passing held-out view. The review binds that scene, its visual asset hash, the separate box dimensions and the exact X/Z position and yaw. The new contact deck leaves the source scene intact.

## Approval and restart

- The review is retained before a scene is created. Confirmation binds the exact saved review hash. A run or export before its prerequisite is refused.
- The app saves approval before it runs physics. Reopening **Placement review** after restart shows the pending review, the authorized run, or the verified result. Repeating the authorized run returns the same saved scene.
- The placed scene is retained only after both independent hit/miss pairs match. The original reconstruction, saved approval and receipt are checked before replay or export. A healthy re-export does not change the bundle bytes.

## Collision witness

A 0.35 kg sphere enters the collider path at 1.8 m/s. The miss starts at the same X/Y with a lateral offset beyond the rotated collider. Each independent trial runs 180 native steps at 240 Hz. Samples record both bodies’ positions and velocities, plus the first touching or penetrating contact. The hit needs a collision and measured prop motion. The miss must make zero contacts and keep the prop below the allowed drift speed.

The test placed the twin-slot plate at X/Y/Z **0.9 / 0.05 / −0.25 m**, yaw **20°**. It measured first contact at tick **43**, first separation **−0.648 mm**, **59 contact ticks**, and a peak planar prop speed of **0.406 m/s**. The lateral miss recorded zero contacts. The fixture contains a separate visual mesh and box collider. The test uses the collider for contact and exports the authored scene alongside measured trajectories.

The Windows integration drove the chat and approval controls. It reopened a pending review and a saved approval across restarts, replayed the completed collision, reimported the exported GLB and compared its box and mesh geometry, and refused submission of an unverified source or modified library original.

## Reproduce

    ./gradlew :workspace-engine:installDist
    node workspace-desktop/test/spatial-placement-integration.cjs build/visual-placement232

[Edition 232](../../README-232.md) · [Original prop workflow](../publication231/plan.md)
