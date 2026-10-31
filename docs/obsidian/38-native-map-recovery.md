# Native map recovery and materials

[[docs/obsidian/37-global-map-topology.md|Previous: global topology]]

The same Java-owned run now persists body pose/velocities, waypoint, settling,
disturbance, work and actual trace. Explicit recovery records the actual newly
initialized native body and preserves the committed trace prefix.

Declared palette floor materials drive native friction/restitution and Java
viscous drag. Work in joules and force effort in N·s stay separate. The native
feedback controller is unchanged; this is measured execution and procedural
retention, not model-weight training or calibrated photographic material inference.

- [[docs/NATIVE_MAP_RECOVERY.md|State, units, recovery and material contracts]]
- [[README-030.md|Edition030 functions, images, evidence and next gates]]
- [[docs/readmes/030/verification.json|Actual verification scope]]
- [[workspace-engine/src/main/java/fraymus/workspace/MapCheckpoint.java|Atomic native checkpoint]]
- [[workspace-engine/src/main/java/fraymus/workspace/MapMaterials.java|Declared material compiler]]
- [[workspace-engine/src/main/java/fraymus/workspace/MapWork.java|Independent work reconstruction]]
- [[workspace-engine/src/main/java/fraymus/workspace/MapRecovery.java|Native initialization audit]]

Next proof: push a real dynamic object through a constrained native scene,
detect a jam, recover and verify the object's final state. Each further physical
body and constraint must gain its own complete recovery contract.
