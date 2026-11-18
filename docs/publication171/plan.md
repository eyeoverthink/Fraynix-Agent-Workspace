# Compute locally, verify independently

Edition 171 establishes a repeatable offline mission across chat, Java, the game runtime and the creation canvas. The Game Lab serves as a controlled environment where Fraynix can act, observe the physics and preserve evidence of what happened.

## Architecture assessment

- The main advantage is the control loop. A running environment exposes actual conditions and lets the agent act through controllers. Model output can help choose the next operation. Measurements, independent checks and durable state establish whether it worked.
- A local language model expands how users can phrase and combine goals. It adds no new authority. This named playtest runs with model discovery disabled. Open-ended planning still benefits from an installed local model.
- The engineering limits are shared orchestration across domains, skills that transfer across cases, and release size. Finish the cross-domain workflow before adding more specialist tools. Keep source references and compact receipts in Git and verify archived raw evidence on retrieval.

## Next release gates

| Phase | Build | Required evidence |
| --- | --- | --- |
| Offline cross-domain mission | Open a local browser fixture, extract a versioned table, run a bounded world procedure, then create a working report from the fresh observations. | Altered input produces a new report. A crash between stages restores from the correct revision. A source mismatch blocks completion. |
| Registered procedures | Give local deterministic routing and the optional offline model the same typed procedure catalog, prerequisites, cost bounds and independent verifiers. | Equivalent requests bind the same sources. An invalid or unavailable procedure is refused. Show comparative latency and actual outcomes. |
| Reuse and improvement | Compare fresh execution with retained procedures under changed inputs. Keep evaluated candidates, costs and rejection reasons. | Report full-task costs and success on reserved cases, with a restart and rollback. Gate promotion on reproducible gains. |
| Local distribution | Create a compact installation with original content, a separate evidence archive and a first-run offline mission. | A clean machine can install, open one verified result and restart the mission after an interruption using only installed components. Test model and model-free modes independently. |

A later physical-world phase can reuse this loop with calibrated sensors. Record uncertainty at entry and check the observed result before acting again. Simulated crossings measure the controller in Game Lab.
