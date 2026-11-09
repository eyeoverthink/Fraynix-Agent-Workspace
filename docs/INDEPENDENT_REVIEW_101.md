# Independent review — edition 101

2026-11-09. The user explicitly requested an independent agent review. One reviewer inspected published edition100 (`6671e3d2`) and the developing edition101 diff, read-only. The reviewer ran no tests, model calls, applications or pushes. This report separates review findings from the primary agent's subsequent execution evidence.

| Finding | Correction in edition101 | Verification |
|---|---|---|
| Declared counters can pass while actual filtered rows are wrong | Java owns a source-bound, four-state cell/header/quality audit for the new data-to-creation workflow | Java adversarial controls and actual Chromium controls; real-model cohort recorded separately |
| Skill exposure could precede durable parent completion | New whole skills begin pending; exact completed-parent linkage is persisted before publication; pending cleanup reconciles on restart | Crash-boundary controls, fresh-parent validation on reuse and actual post-completion log-write failure |
| Receipt cache updated before durable receipt write | Cache update follows successful atomic write | Forced receipt-write failure preserves the previous cached and restarted receipt |
| New audit sent action-only browser test batches | Each action batch now includes an assertion | Surface-contract unit and real Chromium actions |
| Restart could reuse an old passing behavior receipt despite a later independent audit failure | Persist independent repair pending; reconcile pending files first; otherwise generate replacement within the same draft budget | Lost child pointer plus restart during pending repair |
| Pending publication could be described as “saved” | Chat distinguishes verified execution from pending publication | Both Java and desktop summaries check the pending marker first |

The reviewer confirmed the overall separation: existing Java supervisor, stable owned children, immutable input lineage, actual browser observations, Java verdicts and fresh-input checks on reuse. The final test/readme records identify which executions actually passed and retain failed attempts.

The subsequent real app run reached the requested data/creation pause and preserved the completed data child across an abrupt Java restart. It then exposed a separate integration defect: the chat resume guard treated its own workflow reservation as unrelated busy work. The correction exempts only the interrupted workflow recorded in the task's trusted Java context; other goals, workflows, studies, inference and brain work remain blocking. Stopping an interrupted chat task now also cancels its owned workflow. This finding came from execution, not from the independent reviewer. The original failed recording is retained alongside recovery results.

## Boundaries and follow-up

The visibility check uses DOM geometry and ancestor styles. It is not pixel-level proof against opaque overlays, clipped ancestors, deceptive contrast or every accessibility defect. Screenshots receive a separate human-readable review. This release does not claim unrestricted visual verification.

The stricter four-state audit and pending publication protocol apply to the registered edition101 workflow. Historical three-stage workflow semantics are preserved; this is not a claim that every older creation procedure gained the new contract. The generic pending-repair and receipt-persistence fixes also improve the shared implementation.

Existing history limits (20 workflows, 64 goals, 32 skills) remain. Active indexes and bounded archives are a future scaling task, not implemented here. Model weight training, broad autonomous pipeline synthesis and physical accuracy are also outside this release's evidence.
