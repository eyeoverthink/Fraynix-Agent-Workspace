# Offline motion-data procedures

Edition 020 adds a second registered task family, `motion-data-report-v1`, to the Java goal runner. A selected, immutable CSV dataset becomes a checked data analysis, real canvas report and reusable procedural version. No native physics, model training, cloud provider or dynamic tool installation is required for direct execution.

## Use the app

Open **Data tasks**. Choose a local UTF-8 CSV, declare its time/displacement columns and units, source label and source note, then import it. Units are required: seconds or milliseconds, and metres, centimetres or millimetres. Optional explicit mapping supplies time offset in seconds, displacement origin in the source unit and direction ±1. Missing selected values must reject or be explicitly skipped with source row numbers retained.

Use an imported dataset for chat, or set the maximum time-gap and absolute-speed thresholds and select **Analyze selected data**. A procedure name is optional. Chat example:

> Analyze the selected motion dataset with gap limit 0.3 seconds and speed limit 2 m/s. Chart displacement and derived velocity, preserve the original CSV, and save the successful procedure as motion-data-analysis.

Later select a different imported dataset and open the saved procedural version under **Verified skills**, or ask:

> Reuse the motion-data-analysis procedure on the selected motion dataset with gap limit 0.25 seconds and speed limit 0.4 m/s. Report the quality flags.

For a uniquely named saved procedure, the host binds its actual name and exact version. An explicit `version N` in the request chooses that existing version; otherwise the highest version of that name is selected. The model receives only the options relevant to the task family. It does not have to copy unrelated physics fields or internal identifiers. Ambiguous unbound requests retain explicit name/version selection. A missing source is an actionable error, not an invented dataset.

## What Java computes

1. Freeze dataset identity/version, source CSV SHA-256, mapping SHA-256, full immutable-version hash and declared thresholds. A later import can create dataset v2 without altering a running task's v1 binding.
2. Reconstruct every retained SI row from the original CSV and explicit mapping using the existing bounded parser. Compare reconstructed rows and skipped row numbers against the stored dataset. Reject source/mapping/row corruption.
3. For each adjacent retained pair, compute `dt`, signed displacement change, segment-average velocity `dx/dt` and the segment midpoint. Derive acceleration from adjacent segment velocities divided by the interval between their midpoints. Two source points yield one velocity and no acceleration estimate.
4. Compute duration, net displacement, total travel (sum of absolute segment displacement), signed mean velocity, mean speed, peak absolute segment velocity, peak absolute derived acceleration when available and largest source time gap. Check that integrating the derived velocities reproduces net displacement and total travel.
5. Record every interval exceeding the declared time-gap or absolute-speed threshold. Explicitly skipped source rows remain a separate count and prevent a within-threshold quality result. No smoothing, resampling, fitting, fabricated points or automatic threshold changes occur.
6. Write a stable report with displacement and segment-velocity charts, source bindings and quality status. Open it in the real canvas and require eight exact assertions before verified completion or procedure retention.

Data-task verification and source quality are separate. **Data analysis verified** means the registered computation, provenance and report checks passed. **Quality flags found** means the source did not meet one or more declared checks. A correctly flagged analysis can be a successful task and a reusable procedure; it never changes flagged data into passing quality.

Finite differences describe sampled data and can amplify tracking noise. They are not direct velocity/acceleration sensor readings. Passing gap/speed thresholds does not establish source authenticity, calibrated apparatus, an accurate physical model, causality or hardware-control authority. Source labels remain user supplied. The earlier recorded-motion forecasting gate remains rejected and unchanged.

## Actual audit files

Each report is a versioned artifact with eight real files:

| File | Retained content |
|---|---|
| `index.html` | Actual charts, summary, quality result, provenance and mapped-row table |
| `original.csv` | Original source string, unchanged |
| `normalized.csv` | Source row, mapped seconds and signed displacement in metres |
| `normalized.json` | All mapped source rows |
| `intervals.json` | Source-row pairs, interval times/displacements and segment velocities |
| `accelerations.json` | Midpoint finite-difference acceleration estimates, empty for two samples |
| `quality.json` | Every time-gap/speed exceedance and its declared limit |
| `summary.json` | Metrics, source bindings, explicit mapping, thresholds and method |

Titles/notes are escaped when rendered. Imported cells and procedure descriptions remain data. Native filesystem paths, executable handlers and arbitrary scripts are not accepted as procedural arguments.

## Shared durability and separate ownership

Data goals use the same Java FactoryWorld checkpoint, finite history, nonce-bound desktop effects, actual artifact receipts and versioned procedural records introduced in edition 019. Active work becomes interrupted after process loss. Explicit resume recomputes from the same frozen immutable source or reuses its verified analysis and stable report identity. It does not silently rebind to the latest dataset version. Cancellation cannot save a successful skill. Changed/corrupt source or report content rejects rather than being overwritten to make a check pass.

A data goal does not acquire physics/model ownership. It can run while an unrelated native experiment is paused and cannot cancel or mutate that experiment. A native physical goal retains its existing ownership guard. There is still one active Java goal at a time because the registered task owns its report verification effects.

Procedure versions retain source evidence, defaults, prerequisites, actions, checks and recovery. Reuse requires an explicitly selected new dataset/version; it does not hard-code the old file or reuse old calculated values. Re-saving the same goal/name is idempotent; a different verified source result creates the next immutable version. A name cannot change between the native-world and motion-data procedure families.

Bounds remain UTF-8 CSV ≤256KiB, 2–2048 source rows and ≤32 columns, at least two retained rows, finite strictly increasing mapped time, existing source/mapping limits and artifact file budgets. Unrepresentable derived values reject instead of clipping or inventing motion. Threshold ranges are gap1µs–86400s and speed1µm/s–10⁹m/s; these are representable input limits, not physical validity claims. Existing goal/procedure/history/recovery caps remain unchanged.

## Verification

[Predeclared cases](../workspace-engine/src/main/resources/goals/data-contract-v1.json) include explicit ms/cm conversion and changed ms/mm data with reversals, a gap and a speed flag. Java tests cover unequal sampling, explicit frame mapping, source corruption, derived overflow, two-point behavior, missing rows, escaping, maximum rows/artifact budgets and native coexistence. Actual app tests import local files, inspect real charts, reuse a retained skill after restart, keep v1 frozen after v2 import, test interruption/cancellation/host receipts and exercise both registered families.

The existing frozen Delfys extracted motion file is analyzed descriptively with all49 rows and unchanged bytes. This is neither a new hidden holdout nor a replacement physical-calibration test. The original first-two-point conditioning/47-point forecasting evaluation and its failed1.5cm gate remain separate.

Use `npm run test:data-goals` for actual import/report/skills/recovery checks and `npm run test:live-data-goals` for two real local-model conversations, including named-skill reuse on changed data. Tests create isolated temporary projects. The user's edition019 review runs from its published checkout and separate `review-019` project, independent of edition020 development.

[Edition020](../README-020.md) · [Shared goals and skills](GOALS_AND_SKILLS.md) · [World-model/reality contract](WORLD_MODEL_REALITY.md)
