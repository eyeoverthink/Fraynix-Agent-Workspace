# Local GPT-OSS 120B validation - October 22, 2026

The locally installed `gpt-oss:120b` was verified through actual inference. This was the weight-bearing local tag, not `gpt-oss:120b-cloud`.

The test used an isolated loopback Ollama server with cloud disabled, CPU-only execution, twelve threads, 4096-token context, temperature zero, seed 42 and low reasoning effort. The first attempt hit the default five-minute model-load timeout; a retry used a fifteen-minute load allowance.

## Results

| Test | GPT-OSS 120B | Fraynix 3B |
| --- | --- | --- |
| Negative-coordinate grid snap | Correct | Incorrect |
| Collider fixture-density mass | Correct | Incorrect |
| Ideal resting tile height | Correct | Incorrect |
| Class names without a supplied schema | Incorrect guesses | Incorrect guesses |
| Full-schema scene proposal | Accepted | Rejected for duplicate JSON fields |

The scene task supplied the actual `SceneProposal.instructions()` contract and a complete immutable observation containing a protected marker. Neither reply was hand-edited. The 120B proposal preserved the original entity and added exactly a static floor and a dynamic tile. The real engine staged and round-tripped it, then ran two seconds / 120 JBox2D steps. Two bodies registered, contact occurred, the floor stayed fixed and the tile settled at approximately y=0.14 with solver slop.

## Measurements

- Local weights: approximately 65.37 GB, 116.8B parameters, MXFP4.
- Ryzen 9 3900X, about 96 GiB system RAM; RTX 3070 was not used for inference.
- Successful cold model load: 341.05 seconds.
- Generation: 5.41 tokens/second on the calculation task and 5.25 on the scene task, including reasoning tokens.
- Warm scene request: 136.70 seconds; first generated token at 27.54 seconds, first answer content at 63.06 seconds.
- Peak sampled runner working set: approximately 61.26 GiB; minimum sampled available system RAM approximately 9.41 GiB.

The 3B runs used CPU-only execution, with a 2K context for calculations and 4K for the scene task. They ran while the large model was initially loading from disk, so observed timings are not a controlled throughput comparison. Each model received only one attempt per task. The results demonstrate specific behavior, not a general benchmark ranking.

## Integration consequences

Provide the actual component schema and state even to a large model. Keep validation and review in the loop. Add per-model load/request allowances and preloading before using 120B reliably through the desktop bridge: its current 120-second read timeout is shorter than this measured warm proposal.

The test model was unloaded and its temporary server stopped. The live editor project was not changed. The source evidence remains local; the included JSON summary removes personal machine paths and ephemeral server details.
