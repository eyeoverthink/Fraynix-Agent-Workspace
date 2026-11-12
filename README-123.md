# Fraynix Agent Workspace · Edition 123

**Measure the pickup benefit. Count the whole workflow. Keep safety ahead of speed.**

2026-11-12 · Local Java decisions, original native physics, durable checked tasks.

[Previous edition](README-122.md) · [All editions](docs/readmes/INDEX.md) · [Interactive cost guide](docs/readmes/123/index.html) · [Acceptance contract](docs/EDITION_123_CONTRACT.md) · [Phased plan](docs/PLAYABLE_LEVEL_CREATION_PLAN.md)

![Pickup approaches in the actual app](docs/readmes/123/pickup-controls.png)

## What changed

Edition 122 fixed two recorded enemy-contact deaths and a supported-edge stall. Edition 123 asks a different question: when does waiting or returning for a power-up earn its extra work? It adds four explicit pickup approaches to the same controller, makes them available through durable chat requests, and measures repeated complete workflows. The default remains the edition 122 behavior.

| Pickup approach | Optional waiting | Remembered returns | Preserved behavior |
| --- | --- | --- | --- |
| `full` | Enabled | Enabled | Existing default |
| `no-wait` | Disabled | Enabled | Contact, terrain, risk and recovery checks |
| `no-return` | Enabled | Disabled | Contact, terrain, risk and recovery checks |
| `direct` | Disabled | Disabled | Safe forward pickups and every safety check |

These are controlled feature comparisons. `direct` does not mean blind forward motion, and disabling remembered returns does not prohibit every backward movement used by normal collision recovery. No physics, body transform, collectible counter or finish condition is overwritten.

## Measured findings

On development / supported-return, full versus no-return added 1.00–1.00 pickups per run; paired wall-time change was 7.65 to 8.28 seconds. On development / supported-wait, full versus no-wait added 1.00–1.00 pickups per run; paired wall-time change was 4.06 to 6.23 seconds. On reserved / fresh-return, full versus no-return added 0.00–0.00 pickups per run; paired wall-time change was -0.16 to 0.51 seconds. On reserved / fresh-wait, full versus no-wait added 1.00–1.00 pickups per run; paired wall-time change was 3.92 to 6.18 seconds. On software / fresh-return, full versus no-return added 0.00–0.00 pickups per run; paired wall-time change was -0.20 to 1.66 seconds. All negative and mixed comparisons remain below. Costs are measured on this host, not promised for another machine.

The table reports totals across repetitions. A pickup count of three means one pickup in each of three runs, not three objects in one course. Times are elapsed **complete Java workflow** seconds.

| Partition / case | Approach | Finishes | Pickups total | Damage / deaths | Median wall seconds | Observed range |
| --- | --- | --- | --- | --- | --- | --- |
| development / supported-return | direct | 3/3 | 0 | 0 / 0 | 23.98 | 23.71–24.18 |
| development / supported-return | full | 3/3 | 3 | 0 / 0 | 31.97 | 31.54–32.23 |
| development / supported-return | no-return | 3/3 | 0 | 0 / 0 | 23.95 | 23.89–24.01 |
| development / supported-return | no-wait | 3/3 | 3 | 0 / 0 | 32.22 | 31.77–32.43 |
| development / supported-wait | direct | 3/3 | 0 | 0 / 0 | 18.00 | 17.91–18.36 |
| development / supported-wait | full | 3/3 | 3 | 0 / 0 | 22.26 | 22.19–24.71 |
| development / supported-wait | no-return | 3/3 | 3 | 0 / 0 | 22.23 | 21.81–22.28 |
| development / supported-wait | no-wait | 3/3 | 0 | 0 / 0 | 18.13 | 18.08–18.48 |
| development / unsupported-pickup | direct | 3/3 | 0 | 0 / 0 | 23.07 | 22.75–23.18 |
| development / unsupported-pickup | full | 3/3 | 0 | 0 / 0 | 22.91 | 22.64–22.99 |
| development / unsupported-pickup | no-return | 3/3 | 0 | 0 / 0 | 22.92 | 22.62–23.05 |
| development / unsupported-pickup | no-wait | 3/3 | 0 | 0 / 0 | 23.00 | 22.89–23.24 |
| reserved / fresh-return | direct | 3/3 | 3 | 0 / 0 | 25.29 | 25.06–25.56 |
| reserved / fresh-return | full | 3/3 | 3 | 0 / 0 | 25.50 | 25.42–26.17 |
| reserved / fresh-return | no-return | 3/3 | 3 | 0 / 0 | 25.57 | 25.47–25.66 |
| reserved / fresh-return | no-wait | 3/3 | 3 | 0 / 0 | 25.42 | 25.39–25.66 |
| reserved / fresh-wait | direct | 3/3 | 0 | 0 / 0 | 18.01 | 17.87–18.13 |
| reserved / fresh-wait | full | 3/3 | 3 | 0 / 0 | 22.81 | 22.13–24.12 |
| reserved / fresh-wait | no-return | 3/3 | 3 | 0 / 0 | 22.60 | 22.41–22.90 |
| reserved / fresh-wait | no-wait | 3/3 | 0 | 0 / 0 | 18.11 | 17.94–18.21 |
| software / fresh-return | direct | 3/3 | 3 | 0 / 0 | 25.97 | 25.67–26.34 |
| software / fresh-return | full | 3/3 | 3 | 0 / 0 | 25.89 | 25.77–27.48 |
| software / fresh-return | no-return | 3/3 | 3 | 0 / 0 | 25.88 | 25.82–26.10 |
| software / fresh-return | no-wait | 3/3 | 3 | 0 / 0 | 25.94 | 25.92–26.38 |

![Full workflow time and observed pickup benefit](docs/readmes/123/pickup-costs.png)

## When is it worth it?

A slower approach can be useful if it adds a pickup the user values enough. For comparisons with three safe paired finishes and an extra pickup in every pair, Java reports:

`observed conservative threshold = max(0, largest measured elapsed-time increase) / smallest measured extra-pickup count`

The [interactive guide](docs/readmes/123/index.html) lets you enter how many extra seconds you would allow per additional pickup and compare that allowance with the measured threshold. It does not change the controller. Three observations are not a statistical confidence bound or a prediction guarantee for another machine or level.

| Partition / case | Enabled approach vs comparison | Extra pickups per pair (range) | Extra wall seconds (range) | Observed seconds per extra pickup | Assessment |
| --- | --- | --- | --- | --- | --- |
| development / supported-return | full vs no-wait | 0.00–0.00 | -0.69 to 0.46 | not established | No additional pickup benefit demonstrated |
| development / supported-return | full vs no-return | 1.00–1.00 | 7.65 to 8.28 | 8.28 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| development / supported-return | no-return vs direct | 0.00–0.00 | -0.29 to 0.24 | not established | No additional pickup benefit demonstrated |
| development / supported-return | no-wait vs direct | 1.00–1.00 | 8.04 to 8.45 | 8.45 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| development / supported-return | full vs direct | 1.00–1.00 | 7.36 to 8.53 | 8.53 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| development / supported-wait | full vs no-wait | 1.00–1.00 | 4.06 to 6.23 | 6.23 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| development / supported-wait | full vs no-return | 0.00–0.00 | -0.09 to 2.48 | not established | No additional pickup benefit demonstrated |
| development / supported-wait | no-return vs direct | 1.00–1.00 | 3.90 to 4.23 | 4.23 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| development / supported-wait | no-wait vs direct | 0.00–0.00 | -0.23 to 0.48 | not established | No additional pickup benefit demonstrated |
| development / supported-wait | full vs direct | 1.00–1.00 | 3.83 to 6.71 | 6.71 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| development / unsupported-pickup | full vs no-wait | 0.00–0.00 | -0.36 to 0.09 | not established | No additional pickup benefit demonstrated |
| development / unsupported-pickup | full vs no-return | 0.00–0.00 | -0.40 to 0.37 | not established | No additional pickup benefit demonstrated |
| development / unsupported-pickup | no-return vs direct | 0.00–0.00 | -0.45 to 0.17 | not established | No additional pickup benefit demonstrated |
| development / unsupported-pickup | no-wait vs direct | 0.00–0.00 | -0.18 to 0.49 | not established | No additional pickup benefit demonstrated |
| development / unsupported-pickup | full vs direct | 0.00–0.00 | -0.54 to 0.16 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-return | full vs no-wait | 0.00–0.00 | -0.01 to 0.51 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-return | full vs no-return | 0.00–0.00 | -0.16 to 0.51 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-return | no-return vs direct | 0.00–0.00 | 0.01 to 0.42 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-return | no-wait vs direct | 0.00–0.00 | -0.13 to 0.37 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-return | full vs direct | 0.00–0.00 | -0.14 to 0.88 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-wait | full vs no-wait | 1.00–1.00 | 3.92 to 6.18 | 6.18 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| reserved / fresh-wait | full vs no-return | 0.00–0.00 | -0.47 to 1.21 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-wait | no-return vs direct | 1.00–1.00 | 4.28 to 5.03 | 5.03 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| reserved / fresh-wait | no-wait vs direct | 0.00–0.00 | -0.02 to 0.20 | not established | No additional pickup benefit demonstrated |
| reserved / fresh-wait | full vs direct | 1.00–1.00 | 4.12 to 6.25 | 6.25 | Extra pickup in every repeat: compare its value with the measured cost threshold |
| software / fresh-return | full vs no-wait | 0.00–0.00 | -0.18 to 1.10 | not established | No additional pickup benefit demonstrated |
| software / fresh-return | full vs no-return | 0.00–0.00 | -0.20 to 1.66 | not established | No additional pickup benefit demonstrated |
| software / fresh-return | no-return vs direct | 0.00–0.00 | -0.52 to 0.43 | not established | No additional pickup benefit demonstrated |
| software / fresh-return | no-wait vs direct | 0.00–0.00 | -0.03 to 0.26 | not established | No additional pickup benefit demonstrated |
| software / fresh-return | full vs direct | 0.00–0.00 | -0.20 to 1.14 | not established | No additional pickup benefit demonstrated |

If either approach fails to finish safely, the comparison refuses an economic recommendation. If no additional pickup is demonstrated, it says so. A consistently faster approach with no worse pickup count can still be useful. Mixed outcomes and overlapping timings remain visible; there is no invented single utility score.

## What the clock includes

Each measured row starts before creating its fresh project and task owner. It includes source checks, candidate construction, a fresh simple-control comparison, the AI trial, native frame capture, all durable task saves, final source/outcome verification, and closing the owned runtime. The phases are setup, task advancement, verification and shutdown. Advancement includes native execution **and** its surrounding validation, persistence and response construction.

Simulation ticks are reported separately. The existing `workNanos` field is partial engine work and is not used as full wall time. UI scheduling, chat presentation, post-study cost analysis and study-report writing are outside the Java-workflow clock. Each study records its analysis time separately in `analysisNanos`. The Java workspace is already running when a request starts; outer Gradle/test-JVM startup is excluded, while the task-owned native game startup is included. Actual visible request timings are recorded separately below, including the request interrupted for the restart test. Explicit Java routes make zero model calls; these measurements are not LLM inference benchmarks.

Measured native renderer: NVIDIA GeForce GTX 1660 Ti/PCIe/SSE2; Java 21.0.10. The test owner reports 4 processors and a 1024 MiB heap limit. The native child uses four processors and a 512 MiB heap. These are process limits, not a claim about total machine capacity. Software rendering is reported separately.

All strategies use fresh memory and the same declared geometry. Order rotates over three repetitions; each strategy occupies three of four positions, not complete counterbalancing. File-system caches and unrelated machine load can still affect elapsed time. We retain ranges and individual measurements instead of claiming statistical certainty or removing inconvenient samples.

## Cases and verification

Development contains a supported wait, a supported return, and an unsupported pickup. The first pilot is retained separately and excluded from repeated statistics. Two new measurement cases were declared before running: seed 251 with a shifted turtle approach, and seed 257 with a supported remembered return. Software rendering repeats the seed 257 case on the same geometry; it is a portability repeat, not additional unseen evidence. The new wait case shifts the turtle; the new return case changes the seed while retaining the same return geometry. These are modest checks, not proof of generalization to arbitrary courses. No controller tuning follows their outcomes.

Local checks passed: 195 distinct Java test methods, 168 desktop checks, 72 complete repeated workflows, 18 prior course executions with no damage, six transformed reference images and four refusal cases per renderer, plus the original reference regression. The actual app completed five requests, recovered its own interrupted Java process, and retained/refused an impossible sixth course. These edition123 regression counts are separate from the 85 executions documented in edition122.

`GameEvidence123` checks that disabled actions stayed disabled. All previous source, asset, component, engine, owner, session, action, image, motion and outcome checks remain. Strategy identity is tied to the original request. Old proofs remain bound to their original engine identity. A failed trial can now be verified for **measurement**, with `completionCertified=false`; the ordinary completion verifier still refuses it. This does not promote failed tasks or bypass the task history limit of 24.

CI was checked first: edition 122 main and release were still running, while the latest completed edition 121 runs had succeeded. Hosted CI status is recorded separately from local validation and the fetched publication audit.

## Try it in the app

1. Open the integrated **Game Lab**, then expand **Create a level from the original components**.
2. Choose **Return to a cleared pickup** or the turtle/pickup course.
3. Choose a **Pickup approach**, prepare the request, and send it through chat.
4. Inspect the verified completion, actual pickups and the wait/return counters in task history. A completed candidate reopens at its start for review.
5. Repeat the same course with another approach. Keep full timing separate from game ticks; use the measured guide for the edition's controlled comparisons.

For example:

```text
Create a playable Mario level seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1 repairLimit=0 pickupStrategy=no-return
```

Unknown, empty or duplicate strategy choices are refused. Non-default strategies require explicitly declared challenge geometry. General AI Play and requests without a strategy retain the existing default.

![Verified task result with returning enabled](docs/readmes/123/return-full.png)
![Native frame after confirmed return collection](docs/readmes/123/collected-native.png)
![The same course with remembered returns disabled](docs/readmes/123/return-disabled.png)
![Actual waiting in the integrated game](docs/readmes/123/live-wait.png)
![Verified strategy and retained attempts](docs/readmes/123/verified-strategy.png)

## Actual visible requests

These are separate end-to-end app observations, not pooled into the repeated Java timings. All screenshots are actual captures from an isolated review project.

| Request | Elapsed seconds | Final chat status | Verified AI pickups |
| --- | --- | --- | --- |
| Create a playable Mario level seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1 repairLimit=0 pickupStrategy=full; compare controls and report skipped pickups. | 61.884 | completed | 1 |
| Create a playable Mario level seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1 repairLimit=0 pickupStrategy=no-return; compare controls and report skipped pickups. | 42.047 | completed | 0 |
| Create a playable Mario level seed=53 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=15 obstacleTile=26 obstacleHeight=2 repairLimit=0 pickupStrategy=full; compare controls and report skipped pickups. | 37.779 | completed | 1 |
| Create a playable Mario level seed=53 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=15 obstacleTile=26 obstacleHeight=2 repairLimit=0 pickupStrategy=direct; compare controls and report skipped pickups. | 29.693 | completed | 0 |
| Create a playable Mario level seed=50 lengthTiles=72 gapTiles=3 secondGapTiles=2 pickupTile=18 goombaTile=28 obstacleTile=25 obstacleHeight=2 repairLimit=0 pickupStrategy=full | 41.149 | completed | 0 |
| Create a playable Mario level gapTiles=10 pickupTile=17 goombaTile=45 pickupStrategy=direct. | 19.918 | interrupted | not certified |

![An impossible course remains refused](docs/readmes/123/failed-course.png)
![Compact integrated layout](docs/readmes/123/pickup-1100.png)

## Implementation and evidence

```mermaid
flowchart LR
 Request[Chat request and explicit approach] --> Task[Durable Java task]
 Task --> Source[Source and candidate checks]
 Source --> Compare[Fresh simple comparison and AI trial]
 Compare --> Native[Original native physics]
 Native --> Verify[Actual outcome and action checks]
 Verify --> Measure[Complete workflow timing]
 Measure --> Analysis[Repeated cost and pickup comparison]
 Analysis --> Value[Observed threshold for a user allowance]
```

```text
src/main/java/agent/PickupStrategy123.java          native feature choices
src/main/java/agent/PickupOpportunity120.java      bounded wait/return behavior
workspace-engine/.../GameStrategy123.java         request and policy binding
workspace-engine/.../GameEvidence123.java         disabled-action checks
workspace-engine/.../GameTask115.java             durable execution and measurement verification
workspace-engine/.../GameCost123.java             repeated cost and benefit analysis
workspace-engine/.../GamePickupStudy123Test.java  complete native study
workspace-desktop/renderer/brain-game.js          integrated pickup selector
```

[Measurements](docs/readmes/123/measurements.json) · [Verification](docs/readmes/123/verification.json) · [Original evidence index](docs/readmes/123/original-evidence.json) · [Source inventory](docs/readmes/123/file-inventory.json)

No new external framework, automatic policy promotion, neural weight training or saved-skill admission is introduced. The app remains a local Java-controlled system with one integrated presentation. The evidence establishes the measured trade-offs, not a general valuation of health, survival or future levels.

## Next phases

**124: preference-aware optional actions.** Use explicit user time allowances and an uncertainty-aware cost estimate, then test on new scenes before changing the default. Include cases where the estimate should abstain. Count the cost of estimating and verifying the decision itself.

**125: broader contact uncertainty.** Add declared moving-shell, enemy-reversal and changing-support cases. Keep actual contact, false alarms, missed hazards and bounded recovery visible. Earlier plans for broader image components and reusable verified procedures remain on the roadmap.

[Architecture](docs/ARCHITECTURE.md) · [Project memory](docs/PROJECT_MEMORY.md) · [Development log](docs/DEVELOPMENT_LOG.md) · [Contributor handoff](README-CONTRIBUTORS.md) · [Obsidian map](docs/obsidian/131-full-cost-pickup-choices.canvas)
