# Chandra's specialist architecture and the Java decision system

Source review on November 5, 2026: [chandra-fraymus e984a33685c0ed9e0c4f4c79e4cd1f094b4118de](https://github.com/eyeoverthink/chandra-fraymus/tree/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de). This supersedes the current-source assumptions in the [October29 assessment](CHANDRA_ASSESSMENT.md); that earlier report remains historical. Chandra's source and personal runtime data were not modified. Its server, autonomy loop and real model were not started.

## Judgment

Chandra contains a useful organizing idea: intelligence can be composed from specialists that share context, disagree, retain useful experience and act through a common execution layer. The best transfer into Agent Workspace is the contract between those parts: what each knows, what it may request, the evidence supporting its answer, the resources it spent, and what actually happened afterward.

This can strengthen the current Java application. It is not evidence that adding more named agents creates superintelligence. Multiple calls to the same model can repeat the same mistake; a language-model judge can prefer a persuasive explanation over a working result. Specialist diversity becomes valuable when independent checks or complementary information improve completion, reliability or cost on fresh tasks.

For this edition, the immediate application is concrete. A forecaster supplies position/speed ranges; a control calculation checks the remaining correction authority; a value calculation compares observation times and costs; the native verifier checks the actual correction and outcome. These are owned Java computation stages within the existing durable job, not a claim that three autonomous LLM agents have been installed. The chat model receives their checked results through the existing Java agent route.

## What the source actually does

| Mechanism | Implemented behavior | Value for Workspace |
|---|---|---|
| [Fleet runtime](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/fleet/runtime.js) | Loads role manifests, selects scoped tools, stores messages/runs, delegates tasks and applies execution-budget directives. | Explicit specialist capabilities, parent/child task identities and retained execution receipts. Keep the Java dispatcher as action owner. |
| [Collaboration](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/fleet/collab.js) | Implements chain, parallel synthesis and proponent/opponent/judge flows with stored steps. | Use different review functions when their contribution is measurable. Preserve disagreement and failed work; do not reduce the result to a final paragraph. |
| [Perspective and arena](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/intelligence/perspective.js) | Selects task framings, runs contestants, asks an LLM judge for scores and stores a winning approach in a playbook. | Retrieve relevant procedures and compare alternatives. Separate the judge's opinion from verified task success. |
| [Ouroboros inheritance](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/intelligence/ouroboros-cycle.js) | Absorbs source outlines, generates/reviews proposals, records lineage and passes inherited genome context to both stages. Records implemented_count=0 for these proposal cycles. | Preserve accepted knowledge across restarts, forks and templates; track proposal, tested candidate and promoted procedure as different states. |
| [Execution harness](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/fleet/harness.js) | Adds hashes, budget warning bands, permission checks, partial failure records and output summaries. | Bound work and retain complete original evidence behind concise context. A content hash establishes byte identity; it does not establish successful execution. |
| [Predictive processing](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/intelligence/predictive-processing.js) | Extrapolates internal telemetry with derivatives, compares later readings and adds fixed motor-to-sensory expectation offsets. | A general prediction/observation record with explicit units, timestamp, horizon and provenance could connect internal health, browser work and physical scenes. |
| [Active inference](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/intelligence/active-inference.js) | Maps telemetry surprise into fixed CTRNN motor-region biases and updates an effectiveness score from subsequent error reduction. | Unexpected observations can prioritize investigation. A physical action still needs reachability, permission, safety and outcome checks. |

The current predictive channels are bus, memory, emotion, consciousness-labelled telemetry, agents, health, workflows and goals. They are not measurements of object position, camera scale, mass or contact. The fixed efference map is a useful initial hypothesis about consequences, not a learned causal physical model. Likewise, a neural activation or a tier named “consciousness” does not demonstrate subjective experience.

## Fresh checks and limits

The source was fetched into an isolated review checkout. Four existing inheritance tests passed using disposable databases and mocked model responses: first generation, ordinary inheritance, forked ancestor and template inheritance. This verifies that the selected context reaches generator and reviewer beyond outline compaction. It does not measure model quality or prove that inherited proposals are correct. The first invocation failed because a local dependency junction was not created; that setup failure and the successful retry are retained separately.

The [independent probe](../scripts/Review-Chandra072.mjs) evaluates exact source modules with declared substitute boundaries: an in-memory SQLite database, controlled agent replies and a neural injection sink. It performs no real network or tool action. Six counterexamples reproduced:

| Probe | Actual result | Required Java rule |
|---|---|---|
| Agent replies with timeout | Collaboration session and both steps become done; final text is [No response]. | Propagate timeout/unknown separately from completion. |
| Every parallel agent and synthesis throws | All three steps are error, but the session is done. | A failed synthesis or all-failed contributors cannot establish task success. |
| Seal a no-executor error object | Receipt says executed=true. The runtime also has a no-executor branch leading to this sealer. | Dispatch evidence must distinguish attempted, executed, failed and verified; hashes cannot create execution facts. |
| Bound a 10,000-character single line | Output is 20,122 characters because head/tail overlap. | Enforce a byte/character cap as well as line limits; retain original bytes separately. |
| Prediction error decreases with no motor bias | Effectiveness rises from0.50 to0.54 despite zero motor-bias injections. | Error reduction alone cannot be credited to an action that did not happen. |
| RSI with zero mutation and fixed random source | Unchanged genome is reported improved; fitness rises0.500→0.549. | Re-evaluate candidates on actual tasks; do not award improvement from a random fitness offset. |

The last behavior remains in [vertiso/rsi.js](https://github.com/eyeoverthink/chandra-fraymus/blob/e984a33685c0ed9e0c4f4c79e4cd1f094b4118de/server/vertiso/rsi.js). Its revert method marks the improvement row reverted but does not restore the selected previous genome; this is a source finding, not a fresh full rollback experiment. The Ouroboros source also auto-accepts proposals omitted by the reviewer. Unreviewed work should remain unreviewed. Arena winners increase a playbook success count from the judge's selection, which is a different fact from a successful external task.

These are focused checks, not a claim to have executed or verified all1,450 tracked files, every service, commerce pipeline, model or hardware adapter. Chandra's own current README has a valuable queue of unresolved findings and persistence reviews. Its stored claims and review statuses are source material, not new validation performed by this edition.

## The architecture to carry forward

Keep one Java brain and one world state. Specialists exchange typed records referencing that state instead of maintaining competing copies of reality. Each request should contain a parent goal, permitted capability, source revision/hash, deadline, cost budget and success contract. Each answer should distinguish observation, prediction, proposed action and verified outcome. Contradictions remain explicit and can trigger a bounded new observation.

The coordinator should own one total budget for a goal and its children. Delegation must not multiply permission, model concurrency or action authority. On this machine, start with sequential specialist computation and at most one local model generation at a time. Parallelize independent bounded CPU calculations only after measuring memory, latency and usefulness. A role can be a small deterministic Java algorithm, a learned controller or a local LLM; choose the cheapest implementation that passes its task contract.

An observer cannot authorize movement by changing its own confidence number. A planner cannot certify its own success by returning a completed string. A verifier should use the native trajectory, artifact test, browser state or other independent result appropriate to the task. The system should remember which procedure, environment and resource budget produced that result.

## Ordered additions

1. **Edition072: useful observation.** Combine source-bound state uncertainty with bounded inverse control and complete sensing costs. Compare against the old scheduler, old risk guard and a simpler reachability policy on new tasks. Preserve every failure and the original gates.
2. **Typed specialist requests in the existing Java goal runner.** Start with observer, planner and verifier contracts around one existing end-to-end task. Test missing capabilities, stale state, contradictory answers, timeouts and restart. A partial child cannot silently become a successful parent.
3. **Outcome-linked procedure memory.** Store proposal lineage separately from verified procedural skills. Feed exact relevant failure/success records into both planning and review. Test inherited context and held-out usefulness after restart; count retrieval and model costs.
4. **Controlled specialist comparison.** Compare one planner with planner-plus-critic on fresh tasks using the same model/time/action budget. Keep independent execution tests as the judge. Report regressions and zero-benefit cases as well as wins.
5. **Prediction-error-directed attention across the app.** Give browser, file and scene observations typed units and freshness rules. Escalate surprising states to investigation only when a useful response remains possible. Test a matched no-action baseline before crediting error reduction to control.
6. **Measured policy evolution.** Generate bounded candidates, run training and separate selection/evaluation tasks, retain exact source/model identities and require a real rollback that restores previous behavior. LLM-weight training remains its own separately evaluated pipeline.

The useful long-term direction is a system that can explain “what I observed, what I expected, what I could still change, why I spent another look, and what the next observation proved.” Chandra supplies organizational ideas; the Java world's exact state, physics and retained outcomes supply the checks those ideas need.
