# FRAYMUS Logic Chain · fresh source review

Reviewed November 3, 2026 against private [FRAYMUS-Logic-Chain](https://github.com/eyeoverthink/FRAYMUS-Logic-Chain), main commit `7c15c783c784417977b0445eb086b2915025472d`. This is newer than the `d7e5537` source reviewed in [edition046](REFERENCE_LOGIC_046.md). The reference was cloned separately; no providers were configured, server launched, model downloaded or reference changes pushed.

The strongest contribution is an explicit separation between an objective, a proposal, a criticism, a supporting record and a proven execution. The application can use that structure without making every physical task wait for sixteen language-model calls.

| Reference mechanism | Source | Assessment and disposition |
| --- | --- | --- |
| Original objective supplied at every stage | `app/engine.py` | Useful continuity invariant. New Java tasks bind an objective hash into the existing checked history and check it before proceeding. The exact request still reaches the planner. |
| Seven ledger fields for new/improved logic, contradictions, implementation steps, tests, unsupported claims and dependencies | `app/engine.py` | Useful for design deliberation. Chat052 records proposal, schema admission, execution and completion as distinct events in its existing history; it does not label a proposed test as a passing test. |
| Warning preservation and normalized duplicate detection | `app/engine.py`, `app/quality.py` | Ported as a Java projection of repair/rejection/interruption records. Case/whitespace variants share a display group; every original occurrence and history hash remains. Domain planning includes these diagnostics even after the short recent-action list advances. |
| Bounded earliest/latest ledger selection, omission counts and valid JSON | `app/engine.py` | Adapted only to the optional discovery catalog. Exact selected bindings and semantic constraints remain intact. Oversized essential domain context refuses explicitly instead of sending a broken JSON prefix. |
| Eight roles in two passes, followed by further reviews | `app/engine.py` | Appropriate as an optional future design/research procedure; not added to the real-time chat path. Calling multiple roles does not create independent physical evidence. |
| JEV bounded votes, author exclusion, independent-provider identities, minimum membership, disagreement fallback | `app/jev.py` | Useful future advisory review machinery. The code explicitly calls agreement a stability signal, not correctness. Not activated in052. Native verification remains decisive. |
| Sentence-level claim tracing with cited ledger IDs and hidden false claims in evaluation | `app/claims.py` | Stronger than an ungrounded final summary, but the source itself distinguishes “the ledger proposed it” from truth.052 uses direct Java receipt fields for its concise result instead of another generative summary. |
| Candidate skills remain unverified; execution is separate | README and engine | Preserve this distinction. Only the existing actual execution/report gates can retain a verified workspace procedure. |
| Demo/provider fallback and paid-provider adapters | Provider/configuration code | Not imported. The actual workspace tests require the installed local model; unavailable or malformed inference remains an error. |

## What was run

`python -m unittest discover -s tests` completed with exit0 and reported **269 tests, OK, 4.919 seconds**. The same original output also contains **five Windows WinError5 save failures** while worker runs renamed temporary JSON files. The suite uses test providers and local HTTP boundaries. It is not a successful production run of all configured models, and its green exit does not establish error-free background persistence. The complete output is retained in [the052 snapshot](readmes/052/reference-tests.txt). No cause beyond the reported rename failures is asserted here.

## What this adds to intelligence

It gives the agent more reliable working context: the objective cannot quietly drift, earlier repair evidence is visible, a decision can be traced to its action, and an outcome comes from execution evidence. These are practical improvements to coordination and error recovery. They do not train model weights or establish that the system can solve unfamiliar tasks better. A future measured experiment should compare the same frozen tasks with and without retained diagnostics, keep all failures, and promote a policy only if it improves completion and recovery without weakening checks.

The next useful chain would be a bounded design skill: propose alternatives, identify constraints, run separate physical trials, compare witnessed outcomes and retain the successful procedure. It should reuse the existing Java workflow and world runner. The present work deliberately measures the narrower improvement: remove an unnecessary terminal model request while preserving the original completion authority.
