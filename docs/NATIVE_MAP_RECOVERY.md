# Native map recovery and declared materials

Edition 030 extends the existing Maps/chat/Skills/canvas workflow. The Java
runtime owns physics and durable execution. Chromium displays the state and
report; it does not integrate a second physical world.

## Checkpoint contract

`MapStore` has one native-owner worker. Every 12 ticks it retains a measurement;
every 120 ticks (0.5 simulated seconds), and on pause/completion, it atomically
commits a checkpoint before exposing the corresponding live observation.
Uncommitted work between checkpoints is discarded after process loss. There is
no promise that an unobserved, uncommitted step survives a crash.

The envelope contains:

- Canonical SHA-256 of the actual payload; checksum integrity is not a signature.
- Original map/run binding and compiled Java implementation identity.
- Actual Minie artifact hash, declared native backend, JDK version and platform.
- Avatar position, quaternion, linear/angular velocity and native tick.
- Controller waypoint and consecutive settling count.
- Original trace, per-tick integration rows, cumulative work/effort and already
  applied disturbance, plus prior recovery events.

Resume validates the envelope, original map, implementation, trace and bounds.
It initializes a new native world from that state, independently records and
compares its actual initialized body, then continues the same run. Manual input
leases are cleared. The scheduled impulse is checked against actual before/after
velocities and occurs once. Edited/incompatible/missing checkpoints fail closed;
they do not silently respawn a new body. Legacy interrupted runs that never had
a checkpoint require an explicitly recorded new attempt. Completed runs and
their original reports are reverified without repeating physical effects or
rewriting report contents.

Only the current single dynamic map-avatar world is covered. Bullet contact
manifolds/solver caches are rebuilt. Floating-point subsequent trajectories are
compared with declared tolerances; this is not a bitwise native-world serializer
or a guarantee against storage/hardware failure. Dynamic props and constraints
need their own complete checkpoint state before they can be claimed recoverable.

## Material contract

Optional `options.materials` binds declared walkable palette indices:

```json
{"materials":[{"tile":1,"name":"Declared ice","friction":0.06,
"restitution":0,"dragNsPerM":0.02}]}
```

Tile indices must exist, be declared floor, and be unique. Friction is
0.05..1.2; restitution 0..0.95; viscous drag 0..8 N·s/m. Names are labels,
not inferred observations. Same-class rectangles compile actual static floor
patches; every original cell remains unchanged. Collider count stays bounded
at512 including the avatar. Legacy maps retain their original uniform-floor
compilation and topology contract.

The unit-mass sphere has radius0.18m. The direct horizontal force actuator is
capped at24N. Java applies drag as `-c*v` in the horizontal plane. Native Bullet
handles gravity, contacts, friction, restitution and rotation. Material-bound
avatars use friction/restitution1; floor patches carry the declared values.
The feedback navigation controller is unchanged. This phase measures surface
effects; it does not add traction-limited locomotion, energy-optimal routing,
calibrated real-world material recognition or a learned material policy.

## Measurement contract

For every actual native tick, retain `[tick,Fx,Fz,dx,dz,dragFx,dragFz,dy]`.
`MapWork` independently reconstructs position and the following integrals:

| Quantity | Definition | Unit |
| --- | --- | --- |
| Force effort | Sum of actuator force magnitude × dt | N·s |
| Signed actuator work | Sum of actuator force dot measured displacement | J |
| Positive work | Sum of positive actuator step work | J |
| Braking work | Magnitude of negative actuator step work | J |
| Drag work | Sum of drag force dot displacement | J |
| Gravitational work | Sum of -m·g·dy | J |
| Kinetic energy | Translational plus rotational sphere energy | J |

The discrete force/displacement convention is explicit. Native damping/contact
work is not individually resolved. Its reported residual is a diagnostic, not a
closed energy-conservation proof. A passing work audit means the counters,
positions, velocities and disturbance receipts reconstruct correctly.

Material reports have nine independently checked SVG charts and exact visible
work/material/checkpoint data. `native-run.json` is a lossless gzip/base64 JSON
package with original raw SHA and byte count; Maps also exports the complete
plain run JSON. Chart-sample JSON is deliberately compact, while trace CSV and
the complete native package preserve actual numeric evidence. Existing artifact
file/total-size limits still apply; oversized reports cannot retain a skill.

## Recovery and user controls

Use **Pause & save** and **Resume saved** for manual play. For an interrupted
shared task, use **Resume** in the Java goal card. Resume requires an idle owner
and matching source/backend; it never repeats stale keys. Stop/cancel does not
promote an incomplete task. Map actions queue so rapid world-selection and play
clicks are applied in order instead of being dropped.

Chat and saved procedures use the existing typed dispatcher. Exact requested
material values are checked semantically; optional zero defaults and object-key
ordering do not alter meaning. Chat reuse cannot silently replace retained
material values; explicit replacement `materials JSON` is required.

## Evidence and next acceptance

See [edition030](../README-030.md) and its original measurements, screenshots and
verification scope. Published029 reference cases are now regression data, not an
unseen evaluation set. No numbered historical README or snapshot is rewritten.

Next: actual pushable-object planning, valid pushing poses, native contacts,
jam detection/recovery, object-goal verification and complete dynamic-body state.
Persistent layer transitions and controller promotion on separate evaluation
cases follow. Parameter mutation alone will not be labelled useful learning.
