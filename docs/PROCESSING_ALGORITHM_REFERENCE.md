# Processing, space colonization and procedural mapping

Research date: 2026-10-31. [Edition027](../README-027.md) implements the first two mathematical kernels below. The third is a separate future gate.

## What was actually reviewed

The supplied [Programming Chaos Processing/Java playlist](https://www.youtube.com/playlist?list=PLjVkBHDr230rZsqIGlhO6mmQJw8KmoCPa) exposed32 entries. We read the full available automatic captions, chapters and descriptions for the first three entries in the browser. Automatic captions can contain transcription errors. This is not a claim to have watched every frame of all32 videos. Raw captions remain private research, not repository content.

1. [The Space Colonization Algorithm: Roads, Rivers, Trees & More](https://www.youtube.com/watch?v=Y6QLhjvytXk). Attraction points influence nearby branches; the nearest eligible node accumulates directions, grows a fixed-length segment, and removes points it reaches. Influence, kill distance, envelope and step length change the shape. The tutorial uses inverse-distance weighting. Our independently written implementation uses sums of unit directions, a closest-target bootstrap when no influence is available, seeded canopy samples and explicit finite budgets. It is an adaptation of the algorithm, not a Processing source port or identical tutorial output.
2. [Procedural Dungeon Generation with Binary Space Partitioning!](https://www.youtube.com/watch?v=Pj4owFPH1Hw). Recursively split spatial cells, place rooms within leaves, then connect them. Our independent implementation chooses a longest-axis split, retains disjoint room bounds and connects room centres with a minimum spanning tree and Manhattan corridors. A grid flood and a separately implemented shortest-path verifier check the actual floor. These connector and verifier choices are ours; they are not attributed to the video.
3. [Procedural Map Generation](https://www.youtube.com/watch?v=gsG731XItNU). This tutorial uses local adjacency constraints learned from a reference image and repairs conflicts, with a rough sketch or pinned regions guiding the result. It is not simply a Perlin-noise height map. Edition027 does **not** implement reference-image grammar extraction, minimum-conflicts synthesis or automatic image-to-playable-world conversion. Its BSP maps should not be presented as proof of those capabilities.

## Processing as a reference, not an installed backbone

[Processing's official reference](https://processing.org/reference/) demonstrates a useful separation of vectors, drawing, input and a repeated update loop. [PVector](https://processing.org/reference/PVector) specifically represents2D/3D vectors; it is not a ready-made16D physical simulator. The [noise reference](https://processing.org/reference/noise_) describes1D/2D/3D noise. Extending dimensionality requires an explicit algorithm and evaluation rather than relabelling a3D API.

We inspected the [Processing4 source repository](https://github.com/processing/processing4), including the4.x PVector implementation, as API/design context. No Processing code, binaries or runtime dependency were copied into this edition. The new geometry-core runtime uses only the JDK. Existing application dependencies, including graphics, native physics and the desktop browser host, remain; the whole application is not dependency-free.

The primary algorithm reference is Runions, Lane and Prusinkiewicz, [Modeling Trees with a Space Colonization Algorithm](https://www.algorithmicbotany.org/papers/colonization.egwnp2007.html), Eurographics Workshop on Natural Phenomena2007, pp.63–70. Attribution here identifies the mathematical approach; it does not imply those authors supplied or validated this application.

## How the implementation fits the agent

The model selects typed semantic options. Java freezes the definition and implementation identity, computes the original data, checks invariants, writes an interactive artifact, and requires actual browser control and coordinate observations before completing the goal or retaining a procedure. The existing goal runner owns restart, cancellation and source identity. No new privileged code can arrive in imported JSON.

Space colonization accepts2–16 mathematical coordinates per point. Rotation operates in a selected coordinate plane; the report displays four recorded orthographic projections and last-axis slices. Growth replay uses actual recorded generations. A16D result remains a16-component dataset viewed on a2D display. It does not establish sixteen physical dimensions, calibrated real-world units, learned biological development or a general neural world model.

Dungeon generation accepts2–4 dimensions with bounded grid volume. Rooms and floor cells retain their original integer coordinates. The report can step through a checked grid path and show a higher-axis slice. A path may leave the displayed slice; the viewer omits cross-layer connections instead of drawing a false visible shortcut. This is grid navigation, not a collision-tested embodied player in the existing Bullet scene.

## Next gates, in order

1. **Reference-image mapping:** extract an explicit adjacency table, preserve pinned cells, use a bounded deterministic conflict-repair loop, report unresolved conflicts, and test new references. Keep generated maps separate from observations.
2. **Spatial acceleration:** benchmark nearest-neighbour indexing against the current exact search on fixed seeds. Preserve source results or document approximation error before promoting an optimisation.
3. **Scene compilation:** map an explicit slice into existing scene entities with declared units, colliders and a spawn/goal; test actual physics traversal before claiming playable imported maps.
4. **Measured adaptation:** compare parameter proposals on separate declared tasks, retain both failures and successes, and promote a policy only after independent evaluation. Saving a procedure or changing a seed is not LLM-weight training.

Other playlist ideas—flocking, constraint solving, cellular automata and growth systems—remain research candidates. They are not registered capabilities merely because a tutorial exists. Each needs its own complete task, finite inputs, original output, verification and recovery.
