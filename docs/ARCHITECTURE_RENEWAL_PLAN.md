## Edition 169 — measured rail clearance

2026-11-18. 125 root Java tests, 21 selected workspace Java tests, 181 desktop tests and 10 archive-verification tests passed. The workspace selection includes original courses, legacy reference reconstruction, durable level tasks and server boundaries. The visible AI completed Relay Circuit with 2/2 cells and 0.666 game units of minimum measured clearance. The visible AI completed Relay Terraces with 2/3 cells and 0.665 game units of minimum measured clearance. The exact published edition 168 app trial confirms rail contact on both courses. A sensor-based jump forecast and independent native contact hold the comparison together. All 24 course files and 59 other tracked compiled control classes match. The prior hosted chat timeout is documented with its XML and the test fixture was improved. Read README-169.md and docs/publication169/plan.md. Canonical Java workspace remains separate from private review data. Next, test multiple nearby rails and varied approaches, then connect this evidence to durable playtesting.

## Edition 168 — two original courses and archived evidence

2026-11-18. 123 root Java tests, 20 selected workspace Java tests, 181 desktop tests and 10 archive tests passed. The workspace selection covers Game Lab, reference import, durable level tasks and route boundaries. Relay Circuit finished in 374 ticks with 2 cells. Existing AI outcome was hit live rail. Relay Terraces finished in 402 ticks with 3 cells. Existing AI outcome was hit high rail. The native loss, reset and restart checks passed. We restored 74 edition 162 files from a downloaded archive and compared them with Git. The previous 19 original assets and 60 compiled control classes match exactly. Next, expose typed rail and pickup observations for a measured AI comparison. Read README-168.md and docs/publication168/plan.md. Keep the Java workspace separate from private review data.

## Edition 167 progress — 2026-11-18

Relay Garden adds an explicit original-content course, actual player/controller playtests, reset and loss handling through existing Java physics. Sixty compiled control/physics classes remain identical to edition166. The next76 frozen files (edition163) were archived, downloaded, restored and compared to original Git bytes; a second local-volume copy was verified. Initial CI archive-parent rejection is corrected with an actual junction regression. Legacy content, defaults and all frozen history remain. Full inherited-content replacement, licensing, complete archival cutover and further capability consolidation remain pending. [Evidence and scope](../README-167.md).

## Edition 166 progress — 2026-11-18 UTC

Eighteen guarded routes now have one named owner; old evidence and saved results remain verifiable. The original tracked tree contains 17,965,515,667 bytes, including 17,889,852,146 in docs/readmes. The local candidate contains 97,166,929 tracked bytes including its compressed historical catalog. Its fresh local clone has 47,007,859 bytes of Git metadata/objects and took 16.06 seconds on this machine. 52,362 prior frozen files were verified; 69 edition164 originals were restored from the archive (78,221,935 original bytes).

One domain is extracted; wider consolidation remains planned. The candidate retains uncleared content and original-Git dependencies. Next: replacements, another bounded archive batch with redundant storage, then a second guarded domain. [Evidence and scope](../README-166.md).

# From accumulated experiments to a maintainable local agent

2026-11-17 · Measured against published edition164 (`100518181`) · Implementation begins in edition165

The destination stays the same: one local application in which Java owns durable goals, policies, observations, execution and verification. The world, browser and creation surfaces should serve that owner. The next architectural work should make those capabilities easier to review, distribute and extend, rather than adding another live subsystem for every release.

## What the review establishes

The tracked tree contains **17,962,458,906 bytes**, of which **17,887,002,086 bytes** are under `docs/readmes`. That directory has 52,171 tracked files, including 46,064 gzip files. The largest tracked file is a 48,561,531-byte integration receipt. These are uncompressed tracked-tree sizes, not measurements of the packed Git object database or installer.

There are 384 engine Java source files, including **272 names ending in a number**. Their presence does not prove all are instantiated on every launch, but the server's construction, routes and shutdown paths demonstrate substantial coexistence of older owners and procedures. The longest Java source line is 8,134 characters and includes generated report text; `GoalRunner` has a 3,807-character line. The maintainability concern is real even when some long text is a template rather than executable statements.

Some foundations already exist. `tools-044.json` contains **110 typed tool definitions**, `ToolSchema043` validates the supported schema subset, and `Agency043.Planner` already provides a model-planning interface. `ChatPolicy044` uses substantial regular-expression routing. The work is to consolidate and expose these foundations consistently, not pretend there is no planner or schema today. Counting direct test files is not a branch-coverage measurement; boundary and recovery coverage must be measured before splitting the owners.

No top-level project license was found. The Game Lab's inherited game content needs a provenance audit. A public source repository alone does not establish that every included asset can be redistributed under a new project license.

## Phase A — stop growing the problem

Edition165 adds capability-named `VerifiedComputationCache`, `EvidenceFingerprint` and `VerifiedHistory`. Existing numbered entry points remain compatibility adapters. Cache results are never acceptance authority: fresh witnesses and original verifiers determine whether computation can be reused, and actual action/outcome checks remain separate. The ordinary recorded-object heading loses its edition suffix. No new numbered runtime family or public tool is added.

The same phase measures complete workflows with reuse enabled and disabled and compares them with the untouched published build. It adds a Windows CI long-path setting before checkout. New raw publication evidence moves to a release download, while Git retains hashes, manifests, compact comparisons and screenshots. This is a working migration pilot for new evidence; it does not shrink existing Git history.

Exit: all mutation/recovery checks pass; published results match exactly; full request costs are reported against both controls; release evidence is downloaded and checked; the repository has no new raw-study copies or per-file gzip evidence for edition165.

## Phase B — a distributable, auditable baseline

### Content and licensing

Inventory game sprites, audio, levels, fonts, copied code and model-related material with origin and applicable terms. Replace inherited Nintendo characters/level layouts in the distributable sample with an original course, original generated sprites or individually documented CC0 assets. Preserve the editor, physics and controller tests using neutral gameplay equivalents, including growth, hazards, gaps, pickups and checkpoints. Do not label a substitute as CC0 without provenance.

The owner chooses a project license after that inventory. MIT and Apache-2.0 are candidates to assess, not licenses already granted. Preserve third-party notices and exclusions. Exit: a fresh package contains only assets with documented redistribution rights; the replacement level builds, plays and passes the original gameplay regression cases.

### Evidence and history migration

Build a content-addressed catalog that records original raw hashes, archive hashes, byte counts, source commit and implementation identity, retrieval location and the verifier required by each edition. Retain an offline export path and a second recoverable copy. Verify every upload by downloading it and reconstructing its original bytes. GitHub supports release-asset upload/download APIs; see its [official asset documentation](https://docs.github.com/en/rest/releases/assets).

Only then prepare a disposable cleaned clone using [git-filter-repo](https://github.com/newren/git-filter-repo). Preserve an immutable original repository/bundle and map old commits, trees and evidence references to the new representation. Current audits check Git ancestry as well as content hashes: a rewrite changes that ancestry. The migration must teach the audit how to verify the old source through the retained archive, rather than claiming hashes alone solve the change.

A candidate tree excluding `docs/readmes` is about 75.5 MB before adding the replacement catalogs. A sub-100-MB source tree is a useful target, not an achieved fresh-clone result. Measure actual download size, checkout time, path lengths, build and first launch on clean Windows. Keep current main intact until the cleaned repository, original-evidence retrieval and cutover plan are concrete and reviewed. Do not force-push rewritten history as an incidental optimization.

### Readability and implementation identity

Introduce pinned Java/JavaScript formatting as a development tool, in bounded domain slices. Formatting can change line-number/debug attributes, class bytes and source fingerprints even when behavior is unchanged. Do not promise byte-identical class files. Compare normalized executable behavior, run regression cases and explicitly version changed implementation identities. Keep old evaluators available for their old proofs; do not stamp historical results with the new identity.

Exit: a clean clone builds and opens without long-path surprises, rights are documented, raw evidence is retrievable and verifiable, and ordinary edits produce reviewable diffs.

## Phase C — consolidate owners and capabilities

Define capability modules around observation/tracking, worlds/physics, data, creation, browser, memory, skills and model transport. A stable interface and explicit policy identifier describe a capability. For example, a tracker may report `rotation-search@153` as a policy identity while its UI presents a readable name. A version label does not replace the algorithm or its implementation digest.

First extract the recorded-object/small-world domain from `WorkspaceServer` into a route table and handler, retaining its existing route names as compatibility adapters. Preserve method, authentication, schema, busy-state, cancellation and error behavior. Add direct tests at those boundaries, including unknown fields, stale revisions and duplicate requests. Scope the UI's world status to this domain: the current generic linked-game-world notice can appear beside a successful recorded task and should not obscure its actual result. Expand only after the extracted domain passes actual-app recovery and refusal tests.

Extract the durable-task kernel from `GoalRunner`/`WorkflowRunner`: admission, ownership, checkpoint, idempotent effects, cancellation, interruption and completion. Procedure plugins supply typed inputs, stages, prerequisites, verification and recovery. A plugin cannot declare success by returning prose. Java commits completion only after the corresponding verifier accepts the linked evidence.

Maintain a compatibility registry with states such as active, replay-only and retired-from-new-tasks. Retire one proven superseded procedure at a time: new tasks stop selecting it, old tasks still resume or explicitly migrate, and old evidence remains replayable with its original evaluator. Do not simply delete classes whose edition number is old.

Exit: at least one end-to-end domain uses the shared router/kernel, old requests still behave as documented, dependency edges and route ownership are explicit, and a superseded procedure is demonstrably replay-only rather than another permanent default.

## Phase D — one typed external interface

Make the existing tool catalog the single source of input/output schemas, effect categories, ownership requirements and evidence results. Eliminate duplicated route/schema definitions before adding an external adapter. Inventory actual supported capabilities instead of advertising every historical helper as a user-facing tool.

Add an MCP adapter, initially local stdio, that lists only available registered tools and forwards validated requests to the same Java owner. It must implement protocol initialization, tool discovery/calls, cancellation and errors; browser/canvas tools require an available host and must report absence explicitly. Exposing a schema is not enough. See the [MCP tool specification](https://modelcontextprotocol.io/specification/2025-06-18/server/tools).

Exit: two independent clients complete the same bounded procedure; unknown tools, malformed arguments, stale world state and missing host capabilities are refused; a client disconnect/restart cannot duplicate an accepted action. Transport availability does not expand tool authority.

## Phase E — better planning and useful learning

Preserve deterministic shortcuts for explicit, fully bound procedure requests. Add a consistent model planner for broader requests, restricted to registered capabilities and schemas. The planner may propose tools, prerequisites and recovery steps; it cannot mint world revisions, invent witnesses or mark a result passed. Use uncertainty and budget constraints already represented by the runtime.

Put Ollama behind a model-provider contract and keep it the default. Additional providers need cancellation, timeouts, streaming, schema handling, context bounds, budget accounting and real integration tests. Cloud providers are explicit choices with data/cost controls; offline work continues to be supported. Provider support is not established by a dropdown or an uncalled adapter.

Export verified `(task, proposed plan, actual actions, checked result)` episodes with provenance and negative examples. Separate training from held-out evaluation by source episode and task family. Measure success, refusal quality, cost, recovery and wrong-action rate. Promote a policy or trained model only after held-out improvements and rollback checks. A changing counter or repeated success on an exposed example is not evidence of general learning.

Exit: a real chosen local model completes varied world/browser/creation requests through the same kernel, refused plans cannot bypass verification, and any claimed learned improvement survives restart and an unfamiliar evaluation set.

## Phase F — a product surface

Use Chat, Mission timeline and Evidence as the everyday workspace. Keep expert panels accessible behind a clear toggle. Show measured/predicted/unknown state distinctly, explain refusals in plain language and offer an actual recoverable next action. Remove internal release numbers from ordinary labels while retaining precise policy identity in evidence inspection.

Guide first run through environment detection, a confirmed model download when necessary, and one complete verified task. Do not automatically consume a large model download or assume a GPU is present. Package a Windows installer only after a clean-machine test covers bundled runtime, permissions, updates, uninstall and preservation of private projects. Signing requires an actual signing identity and verified CI secrets; the existing `distZip` is not a signed installer.

Exit: a new Windows user can install, finish a task, inspect the result, restart and recover without knowing the internal edition history.

## Order and progress rules

Finish and measure the narrow edition165 change first. Then prioritize content/license provenance and the new-evidence publication pilot, followed by a verified archive migration and one domain extraction. Advance in small complete slices with tests and actual workflows. Keep the original app functioning, and record incomplete work as pending rather than adding a clickable placeholder.

The one-to-two-week estimate for a shippable baseline is a planning hypothesis. The archive, licensing and compatibility inventory will determine a credible schedule. The project is promising because observation, execution and verified consequences already connect; maintainability and distribution now need the same evidence discipline as physics.
