# Fraynix Agent Workspace — Edition 130

**Better object tracking on a bounded footage test, with independent physical-accuracy checks inside the app. Both existing defaults remain intact.**

2026-11-12 · Local Java agent, connected world/browser/creation surfaces

[Previous edition](README-129.md) · [All editions](docs/readmes/INDEX.md) · [Offline illustrated guide](docs/readmes/130/index.html) · [Acceptance contract](docs/EDITION_130_CONTRACT.md) · [Frozen experiment protocol](docs/OBSERVATION_130_LOCK.json)

![Actual independent reference audit in the application](docs/readmes/130/physical-accuracy.png)

## The direction stays the same

Fraynix is a continuing local agent workspace: **observe → choose → act → verify → retain useful procedures → evaluate their reuse**. Java owns persistent worlds, task continuity, typed actions, observation interpretation, procedure evidence and recovery. Chat, Game Lab, 3D physics, browser and creation canvas are connected working surfaces. A simulation can test a proposal, but a successful simulated action does not prove a camera measured reality correctly.

Edition 128 connected recorded observations to a working, independently checked artifact. Edition 129 exposed a critical distinction: narrower forecast bounds missed more independent references, despite software completing correctly. Its candidate failed qualification, and ordinary tracking/forecasting stayed unchanged. This edition follows the user's request to address that physical gap before the proposed layout phase. It does not claim to solve physical accuracy generally, learn model weights, recognize a person's identity, or operate native hardware.

## What changed

The new **object-supported feature tracker** is handwritten Java. It retains optical-flow feature identities, weighs features using the first object's colour distribution relative to surrounding pixels, checks forward/backward motion and patch agreement, and replenishes features only while enough tracked support remains. Its appearance evidence is frozen from the seed. It never receives later reference positions. Duplicate and missing observations still produce explicit losses.

It is available as **Experimental130 · object-supported features** when starting a new recorded observation. `legacy-flow` remains the default. The edition 129 experimental choice remains available. Changing the implementation changes the source identity; old incompatible proofs are preserved and refused for fresh execution, never relabelled.

The new **physical accuracy audit** accepts 1–96 independent later positions, without fitting. It separately reports:

- How many reference positions have an accepted measurement, including all missing positions in the denominator.
- Mean/max physical position error and mean returned radius.
- Coverage of the reference centre and coverage with an explicitly declared reference uncertainty allowance.
- The existing adjacent-point forecast and the explicit 1 m-budget forecast, scored against the same independent references.

References enter the scorer after predictions are built. They do not change tracking, filter state, forecast parameters, the world or defaults. The comparison is at the recorded reference timestamps: **it is not validation of every full 0.6-second future forecast**. Reference origin is declared by the user, not automatically authenticated. These are sample coverage counts, not statistical confidence.

An audit is saved with its original references, source/clock/geometry/trace identity and implementation identity. Rechecking after restart rereads the original evidence and recomputes the result. Changed source bytes, altered receipts or incompatible implementations refuse. Up to 64 distinct audit records are retained without pruning; repeated identical requests return the same checked record.

## Measurements and qualification

The first development version exhausted its features and returned only 5 of 15 reference positions. Its source and results remain in the evidence archive. The revised candidate returned all 15 development references. Both development passes used already exposed footage and are **not held-out evidence**.

Before inspecting new results, the candidate, preparation code, three comparison methods and gates were committed. Five disjoint 96-frame windows begin at source frames 700, 1900, 2700, 3700 and 4700. These are different temporal windows from the same EPFL Terrace camera, with correlated scenes and people. The first eligible subject is selected by a fixed seed rule, using only its initial annotation plus a declared 1750 mm height assumption. Later 250 mm-grid references are separate scorer input. No substitution or tuning followed held results.

{{TRACKING}}

The candidate passes the predeclared bounded gate: at least 90% of the incumbent's available references, at least 15% lower mean accepted error, strictly more positions within one metre, and no additional errors above two metres. **This does not change the default or establish general physical accuracy.** It loses two reference positions versus one for the incumbent. On the 13 positions both returned, a separately reported paired diagnostic gives 0.414 m versus 0.301 m mean error; the improvement is not entirely explained by withholding the incumbent's large error.

{{COVERAGE}}

The stricter forecast's availability, coverage and width must be read together. A withheld estimate is not a successful measurement. A tight interval that misses the reference is not precise in the physical sense. Uncertainty remains conditional on the declared planar geometry, ground-contact anchor and motion assumptions. Camera movement, occlusion, identity confusion, assumed body height and ground-grid quantisation remain limitations.

## Try it step by step

1. Launch the existing desktop workspace with Java 21 and the project's prepared desktop dependencies. The normal launcher is [Start-Agent-Workspace.ps1](scripts/Start-Agent-Workspace.ps1); see [setup and architecture](docs/ARCHITECTURE.md). No new package, model download or paid service is required by this edition.
2. In Spatial, choose a persistent world and import timestamped footage. Draw or confirm the first object rectangle and bind it to a stable body. Leave the tracking method unchanged for the default, or explicitly choose Experimental130 for a comparison run.
3. Complete the recorded run. In **Measured world**, select the matching fixed-camera calibration and click **Measure selected run**. Keep the camera/plane source and units accurate.
4. Expand **Independent references**. Import JSON containing `source` and `references`, each with a later `ordinal` and `positionM: [x, y]`. Include `bodyId` for multiple bodies; the panel supplies it for a single body. Seed-frame, duplicate and unknown-frame references refuse.
5. Declare the reference position uncertainty in metres, then choose **Audit physical accuracy**. Zero means compare against the supplied centre without an extra allowance. The allowance never alters predicted radii.
6. Read missing counts, errors and both coverage columns together. Copy the saved audit ID. **Recheck saved audit** works after a cold restart and refuses changed evidence.
7. Prepare an observation-to-artifact request from the measured source. The prepared request explicitly opts into the experimental radius budget. Java retains the source and forecasts, creates a working explorer, independently verifies its data and plotted points, and saves the procedure only after checks pass. Ordinary chat remains on the previous forecast policy.

![Actual motion explorer](docs/readmes/130/verified-explorer.png)

## Validation and retained evidence

{{VALIDATION}}

{{APP_CHECKS}}

The actual application study uses already evaluated windows to exercise UI and persistence; it is not another unseen-footage result. It includes a durable chat request, abrupt owned-Java termination, cold recovery of the same workflow, a fresh-source procedure reuse, retirement/reactivation checks and an accuracy audit recovered after another abrupt termination. Native verified world state stays unchanged during observation/auditing. Single measured workflow costs are reported as observations, not typical latency claims.

[Verification](docs/readmes/130/verification.json) · [Actual application study](docs/readmes/130/observation-study.json) · [Tracking decision](docs/readmes/130/decision.json) · [Coverage](docs/readmes/130/coverage.json) · [Original evidence index](docs/readmes/130/original-evidence.json) · [Canonical source inventory](docs/readmes/130/file-inventory.json)

CI was checked first. Hosted CI status is recorded separately from local tests and actual fetched publication; pending runs are not passes. Original public video/frame packs and private camera/project data are not committed. Numerical evidence and limited attributed screenshots are retained. Footage: [EPFL CVLab POM dataset](https://www.epfl.ch/labs/cvlab/data/data-pom-index-php/); public research footage is not newly recorded webcam evidence.

## Next steps

1. Diagnose the lost subject in the difficult window using independent association/occlusion labels; reserve a different camera or recording for the next locked evaluation.
2. Model uncertainty in the ground-contact anchor and association, then fit on separate training evidence. Test coverage **and** useful width at declared future horizons, including missing observations.
3. Connect those qualified measurement limits to durable world goals: an action should ask for another observation or refuse when its physical tolerance cannot be supported.
4. Return to coherent daily use: show the active mission, selected source, age, uncertainty, withholding reason and reusable procedure together, while preserving one continuing Java-owned workflow.

The destination is a useful, local, persistent agent with measured perception and verified actions. Faster or smoother computation is valuable only when its outcomes remain independently checkable.
