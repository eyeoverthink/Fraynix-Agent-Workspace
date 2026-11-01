# Measured worlds · edition037 runtime contract

See [README037](../README-037.md) for workflow, screenshots and first failures; [036](../README-036.md) retains original image-plane evidence.

Java PlaneCalibration solves a convex four-corner plane or an aspect-corrected front-facing ruler. Optional Tsai ground-plane projection supports explicit camera parameters. Units/source notes are declarations; no3D height is inferred. The ground-contact point is filtered box bottom centre, retaining last accepted height on loss. Outside-plane/horizon projection fails explicitly.

MetricObservationStore retains forced atomic/checksummed sidecar records: source-clock bindings, geometry, immutable first uncertainty profiles and separate first evaluations. These do not alter036 pixels/traces or035 native world revisions. Camera key, dataset hash, trace hash, metric/runtime code identity and geometry remain pinned. Stale native binding is displayed. No metric observation can command native physics.

API: /api/metrics/state, clock, geometry, view, fit, evaluate, report and export. All use the existing private Java capability; renderers do not receive it. Reference fitting requires a completed run,12–96 distinct later frame/body references and a declared independent source. Empirical90% residual quantile; coverage/mean radius gates and correlated-sample qualification stay explicit. Repeated first fits/evaluations and same-run held evaluation are refused. Geometry/tracker/metric implementation mismatch refuses profile reuse.

37 pixel imports validate timing before delegating the unchanged036 image import, then persist the immutable clock sidecar. Every source sample has a strict increasing timestamp; allowed gaps<=5s. Older/long-video image-coordinate workflows remain readable. Local camera capture uses Chromium mediaTime, up to40 samples/20s; no invented hardware FPS.

Camera permissions: only the trusted app top frame may request video. Audio and untrusted browser/canvas requests are denied. Preview is transient, Capture is explicit, sampled images require a human seed and explicit local Import. Stop/unload/hidden document releases tracks. Camera images stay private; exports contain originals and should be shared intentionally. No webcam pixels are documentation assets.

Typed chat observation read/report includes a selected metric interpretation with qualification. Models cannot supply calibration IDs/measurements/reference data or request camera capture through those tools. Reports expose every row and four views. Export includes original036 gzip evidence plus metric state/history; scripts/Inspect-MetricWorld.cjs independently verifies numeric projections/radii, clocks/source links and original hashes, not physical accuracy or authenticity.

First declared EPFL held results reject the experimental profile. Later A:12.5% coverage,2.716537518m mean error,1.628467199m mean radius. Later B: invalid offscreen first box/import refusal, retained without substitution. No noise/tracker/gate tuning after reserved results. Millimetre convention, ground-contact assumption and0.25m grid references limit physical claims. Next gates: actual cutting-mat references, new small-object candidate and untouched clips, camera-motion invalidation and tighter application-specific tolerances.
