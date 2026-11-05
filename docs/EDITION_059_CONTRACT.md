# Edition 059 acceptance contract

Frozen before implementation or experiments. Baseline: d872b1fe810d0ec56934f69a7d9c6f4f41565bc8. Keep the independent Java app, all published 001–058 snapshots and private live installations intact. No new runtime dependencies. Run heavy native/model checks serially, Java heap 768 MB and local model CPU/four threads.

## Faster evidence, identical checks

Add a bounded in-memory cache of verified derived evidence, keyed by the complete current file bytes, source run, executable implementation and frozen protocol. Read and hash the actual bytes on every access; file timestamp and size alone are insufficient. Cached values cannot be mutated by callers. Cold restart rebuilds verification. Changed originals, metrics, source, protocol, or code must invalidate or reject; failed verification never populates the cache. Preserve every original attempt and all existing physical, report, ownership and promotion checks.

Compare cold uncached and cached reconstruction of the exact same actual native records, with warmup and at least five alternating measurements. Retain outputs, byte/hash equality, timings, cache statistics and failures. Report observed speedups only; no fixed speedup or improved end-to-end model latency is assumed. Measure actual full app elapsed time separately.

## Asymmetric supports with new payloads

An explicit asymmetricSupports059 option extends the existing payloadDesign058 workflow with a distinct versioned 059 policy. It requires direct-first design and its exact policy survives skill reuse. Do not silently turn it into the 058 suite.

Candidates, in fixed order: original symmetric support pair; same span translated left; same span translated right. Shift magnitude is min(0.30 m, remaining beam-end clearance after retaining the original 0.08 m margin). Beam position, all stock dimensions/masses, materials, gravity, actuator limits and span remain unchanged. Zero/duplicate or blocked layouts are explicitly rejected. Only a measured original design-case failure admits alternative trials. All admissible alternatives are measured, even if an earlier alternative qualifies, to expose contrary outcomes. Select the first fully qualified candidate in declared order; selection never sees reserved cases. Maximum three layouts; original attempt, time and storage bounds remain.

Development geometry: wide stock, 0.8 m original span, beam centre (0,0), 40 N / 4 N·m virtual gripper. Repeat at centre (0.2,0.2), and test compact stock at its original 1.0 m span without changing the frozen protocol.

Design suite: 16 N centre force; 16 N right force at +90% beam half-length; new 1.35 kg payload at +55%, dropped 0.22 m. Forces act during seconds 1–3. All cases last six seconds at 240 Hz, twice. Existing displacement, tilt, speed, contact, free-fall and impact checks remain unchanged.

Persist the selected plan and decision before fresh construction, repeated design cases and paired original/selected reserved tests:

| New reserved payload | Mass kg | Half-length fraction | Drop m |
| --- | ---: | ---: | ---: |
| reserved059-light-left | 0.60 | -0.25 | 0.16 |
| reserved059-inner-right | 0.90 | +0.20 | 0.12 |
| reserved059-outer-right | 1.65 | +0.60 | 0.30 |
| reserved059-heavy-right | 1.90 | +0.65 | 0.36 |

Same 0.18 m cubes, friction 0.55, restitution 0.05, gravity/contact integration and two repeats. Reserved outcomes never trigger another layout. Do not tune these conditions or gates after observing results. Preserve failure if no layout qualifies or reserved tests fail. World/skill promotion requires all selected fresh and reserved gates. These are declared synthetic experiments, not real material strength or LLM weight learning.

## Validation and publication

Test geometry bounds/unchanged stock, measured-failure admission, symmetry versus both asymmetric alternatives, decision chronology, cold continuation, changed-byte invalidation even at identical size/timestamp, caller mutation isolation, source/protocol mismatch, exact saved-policy refusal, actual local-model chat, report/charts/replay/original exports, narrow layout and visible Stop with partial evidence. Keep source-qualified failed development invocations.

Publish README-059 with actual dated screenshots, observed performance and contrary results, linked historical edition, offline guide, Obsidian map, source inventory, original evidence and updated memories/logs. Verify documentation and previous frozen hashes. Non-force push main, edition059 and annotated readme-059; fetch from GitHub and verify actual blobs before recording publication.
