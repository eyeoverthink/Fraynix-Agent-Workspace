# Fraynix Agent Workspace Â· Edition 121

**Observed enemy reversals, bounded returns, and an actual pickup benefit.**

2026-11-11 Â· Java owns execution and evidence; the integrated desktop shows the world, chat and task history.

[Previous edition120](README-120.md) Â· [All editions](docs/readmes/INDEX.md) Â· [Interactive offline guide](docs/readmes/121/index.html) Â· [Development contract](docs/EDITION_121_CONTRACT.md) Â· [Phased plan](docs/PLAYABLE_LEVEL_CREATION_PLAN.md)

![Actual completed return task](docs/readmes/121/return-completed.png)

## What changed

Edition120 proved that waiting for an enemy to clear could recover two previously missed optional pickups. It did **not** establish that returning to a missed pickup was useful. Edition121 supplies that missing comparison on a declared development course: the revised controller goes back, hits the original question block, collects the released mushroom through native collision, and reaches the finish without damage. The same revised controller with returns disabled finishes without the item. The original120 policy also finishes without it.

This is a measured improvement within a bounded game task. It does not mean every return is worthwhile or every enemy encounter is solved. Two of the eight new development courses still kill the player under both120 and121. A simple controller survives one of those. Those results remain in the report and define a useful next engineering target.

All71 older regression courses finish without damage. These include the54 original courses,120's seven development and six reserved courses, and four early-return probes. The original24 collection courses retain their pickups. The controller still refuses the unsupported gap item; larger reach is not permission to cross missing ground.

## Try it in the app

1. Open the workspace and select **Game Lab**. The original Java renderer appears within the integrated view.
2. Expand the level-building controls. Choose **Return to a cleared pickup** and prepare the chat request.
3. Start the task. Java constructs a separate, source-verified candidate, runs the simple comparison and the AI trial, and checks the result. The saved personal scene is not used for these experiments.
4. Watch the return counter and the live player. A return attempt is not collection; the collision counter and final verification settle the outcome.
5. Open saved task history to inspect the retained decisions and verification. The completed candidate reopens at its start for review.
6. Try **Enemy reverses at a wall**. The enemy direction changes because of the existing collision behavior. The history records its actual velocity sign change.
7. Try the gap refusal. The optional item remains uncollected when its approach lacks observed support.

The explicit chat route uses owned Java scheduling and zero model calls. Choosing an Ollama model in the interface does not imply it authored these controller actions. Natural-language routes outside the supported task grammar retain their existing model and verification boundaries.

![Braking at the start of the verified return](docs/readmes/121/return-live.png)

## Run or update from source

The supported Windows launcher is [Launch-Agent-Workspace.bat](Launch-Agent-Workspace.bat), backed by [Start-Agent-Workspace.ps1](scripts/Start-Agent-Workspace.ps1). It requires JDK21 and Node.js LTS. Initial preparation downloads the locked Java and desktop dependencies; later prepared runs can use `-Offline`.

Before updating an existing installation, use **Stop Java & close app**. Closing only the window may leave the durable Java owner running. The launcher protects that owner rather than rebuilding its loaded installation. Edition121 also fixes the stopped-owner preparation path to refresh the native Game Lab code and assets together with the workspace engine; an existing game folder must not leave an older controller installed.

For an isolated review, choose a new project directory rather than reusing a live personal world:

```powershell
$env:FRAYNIX_WORKSPACE_PROJECT = Join-Path $env:USERPROFILE '.fraynix-agent-workspace/projects/review-edition121'
.\Launch-Agent-Workspace.bat
```

Use a distinct unused review name if that directory is already active. `-PrepareOnly` builds and checks dependencies without opening the desktop. Ollama remains optional for the explicit Java game-task grammar; general model chat uses the configured provider. This release does not install model weights.

## The experiment that proves return value

A late supported block is initially deferred because of a moving enemy. After the encounter, the player lands farther than120's1.75-unit return limit. The new policy considers up to2.75 original game units, with the same240-tick deadline, at most three return starts, once per object, continuous ground, braking room and fresh approach checks. The item remains identified by its persistent object ID.

The comparison below uses the same declared scene and fresh runtime memory for every policy. **observe121** is the same new controller with returns disabled. It includes the observation and collectible-handling improvements, so the contrast isolates return availability.

| Policy | Actual pickups | Total ticks | Return ticks | Complete trial time |
| --- | ---: | ---: | ---: | ---: |
{{BENEFIT_ROWS}}

Returning earns an item at extra time cost. It is useful for an objective that values the optional power-up; it is not a speed improvement. These are single complete-run timings through the final frame, including native simulation, rendering and bridge work; later process cleanup is excluded. Repeated timing distributions and a calibrated benefit gate remain future work.

![Measured native motion and costs](docs/readmes/121/controller-motion.png)

[All measurements](docs/readmes/121/measurements.json) Â· [Exact original evidence index](docs/readmes/121/original-evidence.json)

## What the failures taught us

The first longer-return prototype did not succeed. Braking away from forward travel caused the local recovery logic to count a stall before the player could turn. For121, recovery is reset while the returning player is still braking away from the local goal. The return's fixed deadline remains in force, so this does not permit endless retries.

The next run reached and hit the block but missed the released mushroom. The ordinary wall ray saw the collectible as an obstacle and triggered a jump over it.121 excludes mushroom/flower bodies from obstacle jumping. It still uses the original body, collision, power-up and sprite behavior; no pickup counter or transform is edited to manufacture success.

Both unsuccessful runs are retained. The final run establishes collection through actual native collision, then finish contact. Early unsupported/braking-limited returns remain refused. Two late-enemy contact deaths remain unresolved. Their player, enemy, velocity, timing and decision traces are included rather than converted into successful certificates.

## Reversals and observation

`OpportunityMotion121` remembers at most64 live enemy identities and at most16 detailed reversal events. It observes actual body velocities, removes departed identities, and requires increasing observation ticks. A nearby reversal makes a linear clearance estimate uncertain for30 ticks. A forecast whose projected enemy path intersects observed ground geometry is also refused, because a wall may reverse that enemy before the predicted clearance occurs.

The native wall courses record actual direction reversals and finish safely. Boundary tests separately demonstrate that a nearby reversal or a native wall invalidates an otherwise linear clearance forecast. These are different claims: recording reversals is proven in playable courses; a general survival advantage from the uncertainty guard is not established by those courses alone.

Waiting keeps the existing180 ticks per wait,360 total and three-start bounds. Return and waiting cannot overlap. Permanent structural refusal remains authoritative. A recent threat or missing ground does not become acceptable because the controller remembers the object.

## New development and reserved results

Eight development courses were declared before tuning. Six finish under121, with four pickups and two deaths;120 and the return-disabled comparison retain six finishes and three pickups. Simple control's results are included as a meaningful baseline, including the course it finishes while the more elaborate controllers fail.

{{RESERVED_RESULT}}

The reserved turtle-reversal course collects its optional item but then stalls for25 seconds under120,121 and the return-disabled comparison. Its item is included in the pickup total, but the course is not a successful finish and is not certified as a completed task. This is a separate remaining gap from the two development deaths.

The reserved return comparison below lists courses where both policies finish safely and enabling returns earns an extra item. The controller was fixed before these scenes ran.

| Reserved course | With returns | Returns disabled | Extra total simulation ticks |
| --- | ---: | ---: | ---: |
{{RESERVED_BENEFIT_ROWS}}

[Per-course return benefit](docs/readmes/121/reserved-return-benefit.json)


| Partition | Policy | Finishes | Pickups | Deaths | Return ticks | Median complete time |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
{{CONTROLLER_ROWS}}

The original game camera still activates and removes enemies according to its existing rules. The useful return is not evidence of avoiding a persistent off-screen enemy. Software rendering repeats the same reserved scenes; it does not create additional unseen examples. Native simulation is unchanged. {{COST_RESULT}}

## Durable chat, restart and verification

The task runner still binds the original source and assets, constructed component lineage, installed engine identity, native owner/session, exact bounded actions, frame receipts and final outcome. `GameEvidence121` adds bounds and consistency checks for motion history, reversal events, return policy settings and the return-disabled comparison. It calls the existing waiting/contact verifiers; it does not replace them.

Older proofs remain bound to their original compiled engine identity; this release does not relabel them as new evidence. Changing a counter, inventing a same-direction reversal, altering policy limits or tampering with a retained task remains refused. A completed result is recomputed from retained evidence. A process interruption does not relabel partial execution as success: explicit resume starts a fresh native trial and retains the interrupted one.

Five actual visible requests cover old terrain repair, return with abrupt process recovery, wall reversal, unsupported pickup refusal and a genuinely failed challenge. Screenshots are dated captures from those isolated runs.

| Actual request | Complete wall time | Durable status | Certified AI pickups |
| --- | ---: | --- | ---: |
{{CHAT_ROWS}}

![Saved return and motion evidence](docs/readmes/121/verified-return.png)

## CI was checked first

The edition119 main run43104121657 failed an older load interruption test. The test slept30 milliseconds and assumed cancellation would occur inside an active simulation attempt. That assumption depends on scheduling. Edition121 coordinates cancellation after a real durable native checkpoint, confirms retained partial rows, then checks interrupted status, restart, fresh completed evaluation and the existing force/gap/initial-state forgery refusals.

The production entry point retains its existing behavior. A package-local checkpoint observer enables deterministic coordination at the actual checkpoint boundary. No simulation, verification or attempt budget is relaxed. The original CI log and failed test report are archived. A prior120 continuity note mistyped the119 run ID as43104121687; this edition records the correct43104121657 without rewriting frozen history.

At implementation start,120 hosted jobs were still running. Local pass counts and a fetched Git publication do not establish hosted CI success. The publication receipt records the later observed status separately.

## Validation scope

- {{JAVA_COUNT}} distinct Java test methods, including native game trials, observed-motion boundaries, interruption coordination, task recovery, source verification and forged-evidence refusal.
- 168 desktop checks and five actual visible chat requests, including abrupt termination of this test's own Java process.
- Normal launcher preparation, replacement of an older installed game JAR, and refusal while a live Java owner uses the installation.
- 71 older courses preserved; original24 pickup successes retained.
- Six transformed reference-image cases and four refusals per renderer, plus the original reference regression.
- Real integrated game captures, narrow desktop review, and offline guide interaction at1440 and390 pixels.
- Complete source inventory, original compressed evidence, immutable numbered editions, documentation validation and fetched Git byte audits.

[Machine-readable scope](docs/readmes/121/verification.json) Â· [Actual UI record](docs/readmes/121/actual-ui.json) Â· [Hardware references](docs/readmes/121/reference-hardware.json) Â· [Software references](docs/readmes/121/reference-software.json)

## Architecture and project direction

The broader app remains a local Java-owned agent workspace with durable goals, verified procedures, persistent worlds, native physics, data/creation workflows, browser/canvas presentation and observed-camera experiments. Game Lab is one testable environment in that system. Its serialized objects expose state; their Java components provide behavior. JSON alone does not supply intelligence or physics.

```mermaid
flowchart LR
 Chat[Chat request] --> Task[Java durable task]
 Task --> Source[Checked source construction]
 Source --> Runtime[Native game and physics]
 Runtime --> Observe[Objects contacts velocity history]
 Observe --> Policy[Bounded wait or return]
 Policy --> Input[Normal player controls]
 Input --> Runtime
 Runtime --> Verify[Source and outcome checks]
 Verify --> History[Retained episode and review]
```

```text
src/main/java/agent/OpportunityMotion121.java   observed enemy history
src/main/java/agent/PickupOpportunity120.java   bounded optional goals
src/main/java/agent/GameInput.java              ordinary control boundary
workspace-engine/.../GameEvidence121.java       added receipt checks
workspace-engine/.../GameTask115.java           durable execution and audit
workspace-desktop/renderer/brain-game.js        integrated view and presets
workspace-desktop/test/game121-integration.cjs  actual visible review
```

No new external framework or model is introduced. The original native physics/rendering dependencies remain. This edition is controller engineering and evidence retention, not transformer training, general intelligence, arbitrary image understanding or automatic admission of a new reusable procedure.

## Next phases

**122: late-enemy survival and reversal-course stalls.** Use the retained deaths and turtle-course stall to study airborne landing/contact timing and when a pickup should remain abandoned. Declare new reserved courses first. Preserve the successful return and older-course gates; compare simple control again.

**123: value-aware optional goals.** Measure repeat runs and complete return cost, then let explicit task preference distinguish finishing quickly from collecting a useful power-up. Keep a return-disabled comparison and count cases where extra observation or return hurts.

**Later: broader creation and reusable procedures.** Expand supported image components with explicit unknown-region refusal, then qualify a versioned procedure only after source reconstruction, independent playtests, restart and changed-condition checks. The existing collection limits and provenance requirements remain.

See [architecture](docs/ARCHITECTURE.md), [project memory](docs/PROJECT_MEMORY.md), [development log](docs/DEVELOPMENT_LOG.md), [contributor handoff](README-CONTRIBUTORS.md), and the [Obsidian map](docs/obsidian/129-enemy-reversals-and-return-value.canvas). Published numbered editions remain historical evidence; later changes belong in later editions.
