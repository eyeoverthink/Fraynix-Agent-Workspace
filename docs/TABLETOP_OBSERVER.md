# Tabletop observer038

The tabletop monitor connects one human-seeded physical target to an existing durable world/body identity. It reads private camera pixels in Java, projects the appearance centre into a measured plane and retains measured versus predicted estimates. This is an observation sidecar, not permission to move a real object or replace the native world's verified head.

## Reproduce the measured workflow

1. Start the workspace with JDK21, Node22 and installed pinned desktop dependencies. Docker, WSL, a paid provider and a downloaded vision model are unnecessary for this observer.
2. Open Spatial, create/select a persistent world, then open **Tabletop · live camera & grounded understanding**. Select an appropriate dynamic body identity. This association does not establish physical equivalence between a virtual cube and a lighter.
3. Start the camera explicitly. Keep it fixed, with the printed mat and independent tape markings visible. No microphone is requested. Freeze a setup frame.
4. Select four known mat intersections in perimeter order. The user's mat is12×12in, but the visible selected rectangle can be smaller. Enter the actual selected width/height, draw the target once, then mark two independent tape ticks and declare their units/values. Use ticks, not text-label centres.
5. Save the plane and independent check. The fixed absolute tape-error limit is5mm. A failed check does not enable recording. Perspective geometry assumes that the selected points and projected target centre belong to one plane; object height introduces unmeasured bias.
6. Start the live monitor or a guided procedure. Read the explicit accepted/prediction-only/reference-changed status. Stop invalidates the execution generation; Finish ends collection without certifying accuracy.
7. Select measurements for chat. Ask where the lighter is, whether it is moving, whether it is visible, how scale was checked or what a short forecast assumes. Read/report tools cannot supply their own measurements, change calibration or capture the camera.
8. Open the numeric report. Five chart modes retain every recorded row. Reports have snapshot timestamps and no camera bitmaps. Export private originals only when you intend to save the source images locally.

## Ownership and persistence

Java owns RigidTargetTracker, TabletopFilter, MatReferenceGuard, TabletopStore, TabletopAnalysis and TabletopUnderstanding. The Electron shell supplies explicit acquisition and pointer annotations. ImageCapture returns camera ImageBitmaps; host monotonic seconds are recorded at acquisition completion. They are not certified exposure timestamps. One source sample is in flight; processing backpressure preserves real gaps instead of inventing a fixed frame rate. ImageBitmaps are closed after copying.

The original colour template is immutable. Normalized correlation, a disjoint-runner margin, bounded scale/angle searches and global reacquisition produce appearance evidence. These are algorithms, not new neural weights. A match score is not a semantic confidence probability. The metric constant-velocity filter uses seconds, declared process/measurement noise and innovation gating. Its radius combines model covariance, anchor placement budget and relative scale error. That radius is experimental until independently evaluated.

Four reference patches are checked against the saved calibration frame. At least three usable/matched anchors and a median shift at most3pixels are required. Lighting changes, movement and covering reference patches can all invalidate the reference; the monitor does not pretend to distinguish those causes automatically. Invalid references stop metric collection. They are not silently rebased.

Project-local `tabletop-038/state.json` has a checksum envelope, cross-process file lock and atomic forced replacement. Original images and metadata are saved separately with hashes. Resume reconstructs tracker/filter/guard state by replaying the exact prefix. A complete pending source can be adopted; incomplete pending files are retained/exportable and refuse continuation. A new camera session/clock or changed runtime requires a new run. After a full app restart, the UI cannot recover the old physical camera clock; old evidence remains readable and a new live recording must be calibrated.

Bounded collection stops at600post-seed frames or12MB of originals, whichever comes first. A detailed640×480 scene may reach the byte cap much sooner. No source pruning makes room for more data. Native goals and observation ownership cannot conflict. Read-only camera chat may coexist with live observation, while native verified state stays unchanged.

## Verification contracts

The original protocol, later guided procedure and candidate sources are retained in [the initial declaration](../workspace-engine/src/main/resources/tabletop-038-contract.json), [guided contract](../workspace-engine/src/main/resources/tabletop-038-guided-contract.json) and [candidate lock](TABLETOP_038_CANDIDATE_LOCK.json). The [first reference amendment](TABLETOP_038_REFERENCE_AMENDMENT.json) records an out-of-plane tape annotation refusal before numeric scoring. No result authorizes changing its own gate.

Stationarity measures RMS jitter about the whole recording's accepted mean, not absolute physical accuracy. The fixed4-to8in slide checks the first/last eight measured samples against a known101.6mm vector, with10mm tolerance. Predictions cannot substitute for either endpoint. The guided cover trial checks declared hidden/recovery windows and return-position error; human cue confirmation requires corroborating original pixels. A completed recording alone proves neither that an object was hidden nor that it moved by the declared distance.

Independent reference fitting uses12–96 non-seed accepted measurements. A profile may be evaluated only on a different completed run with matching world, body, geometry and runtime. Finite empirical coverage is not a universal probability guarantee. `node scripts/Inspect-Tabletop.cjs <private-export.json>` independently verifies source hashes, timing/body links, planar measurements, tape arithmetic and radius arithmetic. It does not rerun the Java appearance tracker, authenticate the camera or certify real accuracy.

## Next gates

Complete new controlled slide and actual hidden-target recordings under the frozen gates, retain first failures, then compare performance on different objects and lighting. Collect independent physical position references before fitting uncertainty. Evaluate a separate later sequence and repeat after restart. Only after those measurements support the claim should planning consume stronger physical predictions. Multi-object identity, automatic semantics/OCR, height/contact sensing, force estimation and robot actuation remain separate additions.

Earlier calibrated-footage results and sources remain in [edition037](../README-037.md). This new small-object path does not relabel its rejected pedestrian profile as successful.


## Edition038 outcome and following work

The final full suite passes334Java tests/73classes and88desktop checks. Independent workflow checks pass90UI assertions plus three actual gpt-oss:20b assertions, with732canonical source/assets matching. Actual physical data retain21runs/772post-seed frames/793recording originals and777distinct hashes privately. Real tape checks pass5mm (latest3.018mm discrepancy over101.6mm), and one real cover/reveal sequence loses/reacquires the lighter, but its strict recovery window lacks a required sample. The first4to8inch displacement fails because the matcher selects the tape housing outside the plane. Revised search improves a seen replay from6to17metric accepts, yet new v2 footage still fails its start window and confuses background. No physical displacement/uncertainty certificate or model training is claimed.

Use the prominent manual-slide button, keep the target visible during preparation, wait for measured start samples, release/take your hand away at the endpoint and wait for END WINDOW. Short timed controls are under Advanced. Next candidate must improve target/background discrimination and processing latency, add explicit fresh-start/preparation recovery, then face genuinely new footage and independent position references. Preserve current first failures and gates.


## Fresh measured041 evidence

[Edition041](../README-041.md) adds a separate fixed appearance/background matcher, explicit mat/native registration and manual fixed-window recorder. Fresh first physical results pass6.377mmvectorerror for4to8inch movement and0/8hidden false measurements with8/8revealed recovery. These are source-specific new outcomes; previous038 displacement/uncertainty remainUNVALIDATED. See [operation, timing and uncertainty limits](MEASURED_ALIGNMENT.md). Original038 math/results are unchanged.
