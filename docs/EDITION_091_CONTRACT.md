# Edition 091 prospective contract

Reduce repeated native-envelope fingerprint work without omitting source or outcome checks. The prior release is edition090 (`6d1ef698c44ca0e3bcdedf419b8f4b41dfc8238a`). No prior proof or cost model will be relabelled as current evidence.

## Implementation boundary

Each native record read still reads all bytes, checks the path and size, computes SHA-256, decodes a fresh JSON object, and checks the current goal, attempt and expected content identity. A bounded, process-local table may reuse only the canonical content digest of an identical complete envelope previously validated by this owner. It stores no JSON objects, outcome verdicts or motion proofs. Changed bytes require canonical validation again. Restart loses the table. All motion, chronology, admission, forecast comparison, protected-body, report and world commit checks remain in their original callers.

## Tests declared before measurement

- Unit checks: byte changes including unchanged size/time; changed expected identities; malformed/foreign envelope; bounded reads; escaped/redirected paths; cold owners; eviction; reference mode; returned-object mutation; independent concurrent callers.
- Existing procedure, frame, native verifier, proof, cost/freshness, goal and compatibility regression checks, serially. Keep original failures if found.
- Fresh edition091 native tasks using labelled synthetic scenes: one seed receipt, then long and short tasks with both simple and forced-memory policies. Compare optimized and reference envelope modes in alternating order. Reference mode performs full canonical verification on every read; it does not weaken checks. Both modes use the same compiled runtime and native controller.
- Independently recheck completed goals, report and world results. Compare repeated full audits of the same original records in alternating modes. Record caller time, read bytes, canonical passes, hits and failures; no guessed saved time. Exercise native-file corruption and changed expected outcomes after warm reuse, restore exact bytes, and check cold restart.
- Timing is a small local diagnostic, not a population performance claim. Preparation, exports and post-task checks are separately reported. No automatic calibration, model training or hardware accuracy claim.

Publish current source, original results, actual app screenshots, README091, offline guide, continuity notes and publication verification. Preserve published001–090, private runtime data and existing user apps.
