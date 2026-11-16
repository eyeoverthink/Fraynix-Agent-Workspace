# Edition 151 contract — exposed return diagnosis and replay cost

Baseline: affcf65a94873f6630b2f076afe8a6791e59fd50. Client date 2026-11-16.

CI checked first; edition150 run43836422960 was still in progress. Preserve its receipt and report hosted status separately.

The edition150 displaced-camera recording and generated return/wrong-pattern controls are already exposed. Reuse them only as labelled diagnostics and regression evidence. No fresh accuracy or promotion claim. The initial camera boxes, returned boxes and red-object boxes remain the independent edition150 annotations; none become tracker input.

Implement a read-only Java diagnostic that scores the independently labelled visible crop, compares original saved search hypotheses and probes ±20 degrees in 2-degree increments, ±2 pixels in each axis, and two crop geometries (label box and original seed dimensions at the labelled centre). These bounds are diagnostic choices, not a calibrated rotation estimator or live recovery proposal. Leave all historical tracker, admission and audit implementations unchanged. No source pixels or descriptors in public outputs.

Memoize successful audit openings only, in a process-local LRU of eight entries. On every use reread bytes of the complete observation, label/audit and retained-reference namespaces and the full workspace/JSON code containers before and after. Missing/changed files, force replay or cold restart must run the original verifier. Oversized evidence falls back to the original verifier. Do not memoize failures, return mutable internal objects or persist trusted memo state. Keep original tracking, export and reference checks intact. This reduces repeated opening costs, not first replay or live tracking cost.

Tests: exact agreement with original149 scoring; known translated/rotated rendered controls; flat/wrong/out-of-frame refusals; cold/warm/forced results; caller mutation; changed pixels with same size/time; forged outcomes; altered labels; namespace additions/removal; capacity eviction. Actual Electron/Java studies compare every returned audit with the old unchanged result. Record warm timings, an explicit full replay, an actual process restart, source-corruption refusal and restoration, untouched source project, untouched observation namespaces and native world. No benchmark speedup assumed before measurement.

Publish README151, real privacy-preserving app captures, offline companion, source inventory, linked evidence, Obsidian map and continuity notes; preserve editions001–150. Verify documentation and push main/edition151/readme-151, then fetch and verify. Failures and limitations remain visible.
