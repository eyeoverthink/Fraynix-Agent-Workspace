# Models, agency, Dreamer and Base44 source review

Review date: October 22, 2026 (Pacific). Owner: Vaughn Scott / eyeoverthink.

This report reviews supplied files and selected GitHub implementations as evidence for the Java-first Fraynix Studio integration. It records observations, not a claim that every feature is integrated. The implementation sequence is in [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

Implementation follow-up: the October 23 [live-chat repair](LIVE_MODEL_CONTROLS.md) addresses the editor identity override, visible JSON dumps and client/runtime controls identified here. Findings below describe the audited baseline unless explicitly updated.

## Scope and method

The complete supplied folder inventory contains **139 files / 3,338,767 bytes**:

| Supplied folder under `F:\Full-AI-System` | Files | Bytes | Review |
| --- | ---: | ---: | --- |
| `Fraymus_Modelfiles\Fraymus_Modelfiles` | 103 | 2,489,322 | Model recipes, full SYSTEM/template exports, metadata, index and combination notes |
| `fraymus-agency\fraymus-agency` | 22 | 216,076 | Current Python, four backups, environment configuration, SQLite, logs and fleet documents |
| `fraymus-ultimate-agent\fraymus-ultimate-agent` | 14 | 633,369 | Rust, WAT, model recipe, manifests, Makefile, shell script, Markdown, ZIP and PDFs |

All supplied files were inventoried and hashed; textual contents and structured exports were examined. The two PDFs were extracted in full: 2 + 15 pages. Their content overlaps the Markdown. All nine ZIP members match supplied standalone files byte for byte, sometimes under different filenames. Binary files were examined through their format rather than interpreted as text. Environment secrets and private records were kept out of this report and the checked-in inventory. Original files were not patched or executed in place.

The row-level [inventory](evidence/models-agency-input-inventory.tsv) includes source-relative paths, text line counts and non-secret file hashes. Isolated probes ran against audit copies and temporary databases under ignored `build/model-agency-review-2026-10-22/`. No model creation, package installation, deployment, publication, mail sending or cloud LLM invocation was performed.

The GitHub extension inventoried 280 repository metadata records. It then examined these source snapshots:

| Repository | Pinned commit | Scope |
| --- | --- | --- |
| [Fraynix-Studios](https://github.com/eyeoverthink/Fraynix-Studios/tree/cef123a9117a424ba82a2d9b221b4f6320707b2a) | `cef123a9117a424ba82a2d9b221b4f6320707b2a` | Fraymus Transmuter Base44 app; 819 files; selected pure engines, Java runner, backend contracts and architecture |
| [creator-nexus](https://github.com/eyeoverthink/creator-nexus/tree/929aa97aa45eeea262a1d7862963b8689ef9d627) | `929aa97aa45eeea262a1d7862963b8689ef9d627` | Creator Nexus Base44 app; 408 files; selected skills, dispatch, learning and runner contracts |
| [fraymus-comms-protocol](https://github.com/eyeoverthink/fraymus-comms-protocol/tree/7be2705f68a6066c23afde1d9dbd7f3f78ca5ed2) | `7be2705f68a6066c23afde1d9dbd7f3f78ca5ed2` | Repository structure and protocol/reference inventory; not a complete implementation audit |
| [fraymus-bridge](https://github.com/eyeoverthink/fraymus-bridge/tree/299293866752cc8f3ad29e753074180a70e5a49c) | `299293866752cc8f3ad29e753074180a70e5a49c` | Git-backed communication architecture; stored messages are reference data, not authorization to message agents |

The two app trees total 1,227 files. Their inventory is complete; the GitHub source audit is selective, not a claim to have audited every line of all 280 repositories. Authenticated Base44 workflows, private entity records, production secrets, external workers and paid integrations were **not measured**. Repository descriptions and historical status notes do not establish current production health. A directory scan also found 91 skill documents across the agency's configured local roots, 44 with script paths, totaling 134 script paths; that is discovery, not a full audit of those external skill implementations.

### User-confirmed Transmuter endpoints

The user supplied the login and MCP URLs after the initial report. They are two endpoints of the same application, not two separate applications. Public checks on October 22, 2026 (Pacific) returned:

| Endpoint/check | Measured response | What this establishes |
| --- | --- | --- |
| [Login page](https://fraymus-transmuter.base44.app/login), GET | HTTP 200; HTML title `Fraymus Transmuter` | Public application shell is reachable; not proof of authenticated workflow health |
| [MCP endpoint](https://fraymus-transmuter.base44.app/api/mcp), GET | HTTP 401, `invalid_token`, `Authentication required` | Endpoint is present and rejects unauthenticated access |
| MCP `initialize`, POST | HTTP 401, same authentication requirement | No authenticated session or tool catalog was obtained |
| [Protected-resource metadata](https://fraymus-transmuter.base44.app/.well-known/oauth-protected-resource/api/mcp), GET | HTTP 200; resource name `Fraymus Transmuter`; authorization server `https://app.base44.com` | OAuth-protected MCP resource; advertised scopes `app:mcp` and `offline` |

The OAuth scope named `offline` must not be confused with the local engine's no-network runtime requirement. An MCP integration with this hosted URL requires network access. Plugin discovery returned no Base44 plugin in the available catalog. No credentials were requested, read or submitted. Tool discovery and private-record inspection remain unverified until an authenticated connection exists. Creator Nexus remains a separately identified GitHub app; these supplied links do not identify a second app URL.

## What is one system, and what is separate?

The intended creator product is **one Fraynix Studio desktop application**. Java owns the scene, entities, transactions, physics, memory and tool contracts. Ollama is a separate local inference process that owns model loading and weight storage. The terminal is an additional operator interface to selected kernel services.

The screenshot titled `Vaughn-Scotts-Fraymus-Engine` shows the earlier application, with five Fraynix tabs and a 120-second model timeout. The independent Studio build is titled `Fraynix Studio` and includes **Tools**. This distinction needs a visible build/version/project indicator and an explicit migration path; a new default scene must not replace the user's current 15-object scene.

| Surface | Currently available | Important boundary |
| --- | --- | --- |
| Studio editor | Scene/grid, component/rigidbody inspector, physics, reviewed AI scene changes, checkpoints and undo | Native GUI libraries plus Java runtime |
| Fraynix Chat/Create | Local model selection, scene observations, scene/spatial tools and validated proposals | Chat does not automatically execute arbitrary skills |
| Latest Fraynix Tools | `world.inspect`, `memory.search`, `java.analyze`, `docs.generate`, `artifact.verify` | Five typed host-granted tools; no unrestricted terminal tool |
| Kernel CLI | Ingest, profile, benchmark, replay, PDF distillation, propose/evolve and journal administration | Some advanced commands still lack GUI forms |
| Ollama at `127.0.0.1:11434/11435` | Local model discovery and inference | Its browser root is an API/health endpoint, not a second Studio |
| Supplied Python agency | Separate runnable tool-loop application | Not yet wired into Studio's dispatcher/Jobs panel |
| Base44 applications | Separate web frontends, hosted functions and optional external workers | Their existence does not mean their functions are embedded in Studio |

## Model inventory and useful work already present

The export contains **32 model-tag recipes, 23 distinct manifest digests and five underlying weight-blob paths**. These are meaningful custom model configurations. Multiple configurations sharing a weight blob can still behave differently because SYSTEM text, templates and generation parameters differ. No adapters were populated in the 23 detail records and no `ADAPTER` directives appeared in the 32 recipes; these files alone do not show additional weight training or a merged neural checkpoint.

| Exported base weight | Recipes | Family grouping |
| --- | ---: | --- |
| `667b0c1932bc...` | 13 | Llama 8B and Fraynix/Niles derivatives |
| `dde5aa3fc5ff...` | 10 | Llama 3.2 / 3B derivatives |
| `4c27e0f5b5ad...` | 4 | Gemma/Niles derivatives |
| `9c60bdd691c1...` | 3 | Qwen vision derivatives |
| `c8985d236593...` | 2 | Hermes/v6.1 derivatives |

Read-only localhost discovery returned 35 advertised records on port 11434 and 19 on 11435: 47 distinct names, including one cloud-proxy name, hence **46 distinct local names** at review time. Counts are snapshots, not a guarantee that all names are distinct trained networks or fit in memory together.

Important current identities:

- `fraynix-ultimate:8b` is already installed on the primary service, with a compact 3,531-character SYSTEM and a template that renders tool definitions.
- The supplied `fraynix-dreamer-ultimate:8b` recipe was not advertised by either queried service. A recipe file is not proof it has been installed there.
- `eyeoverthink/FRAYNIX-AGENT:8b` has a roughly 79K-character SYSTEM; at least 46,530 characters match obvious embedded web/React boilerplate lines. Full documents belong in retrieval, rather than a permanent prompt on every request.
- Some exported `Name` values are serialized PowerShell objects with a `.value` property and provider metadata. The importer must normalize them before treating them as tag names.
- The same `llama3:latest` name resolves to different manifest/weight digests on the two services, with different context metadata and quantization. Current endpoint-order selection is deterministic but insufficient as a long-term model identity.
- Model capability metadata is an advertisement. Vision, audio and tool quality require actual task tests with the selected template, endpoint and settings.

`COMBINE-NOTES.md` correctly distinguishes combining prompt/configuration overlays from merging unrelated weights. Preserve this distinction in the GUI importer.

## Verified strengths

These are executed observations unless explicitly marked source inspection.

| Anchor | Result |
| --- | --- |
| Agency `fraymus_offline_agency.py:108,154,180,266` | An isolated fixture discovered a skill, read its documentation, executed its actual script and recorded a receipt through the real agent loop with a deterministic model substitute. Entry-path traversal was rejected. All five current/backup Python variants compiled. |
| Agency `compute_hash:94` and supplied SQLite | Database integrity check passed; all 13 stored receipt payload hashes recomputed correctly. This establishes consistency under that hash format, not task binding or authenticity. |
| Transmuter [`src/lib/fraymus.js:233`](https://github.com/eyeoverthink/Fraynix-Studios/blob/cef123a9117a424ba82a2d9b221b4f6320707b2a/src/lib/fraymus.js#L233) | Actual source round-tripped 1-, 257- and 4,097-byte fixtures, produced valid WASM containers and 2,048-byte DNA sections. This browser computation is real and does not require a cloud LLM. |
| [`src/lib/glyphsmith.js:18`](https://github.com/eyeoverthink/Fraynix-Studios/blob/cef123a9117a424ba82a2d9b221b4f6320707b2a/src/lib/glyphsmith.js#L18) | All 256 byte values survived encode/extract through variation selectors. This is an encoding transport, not encryption. |
| [`src/lib/fraymusMail.js:62`](https://github.com/eyeoverthink/Fraynix-Studios/blob/cef123a9117a424ba82a2d9b221b4f6320707b2a/src/lib/fraymusMail.js#L62) | RFC 8032 self-test passed; an additional fixture signature verified with Node's independent native Ed25519 implementation; changing the message was rejected. Only the browser import alias was rewritten in a scratch copy. No relay call or real identity was used. |
| [`src/lib/hdc.js:62`](https://github.com/eyeoverthink/Fraynix-Studios/blob/cef123a9117a424ba82a2d9b221b4f6320707b2a/src/lib/hdc.js#L62) | XOR binding/unbinding restored the original 16,384-bit vector. |
| Transmuter Java `KernelSource.java:5,11`; `OuroborosEngine.java` | Source inspection: finite 20-variant mutation grammar, candidate subprocesses, watchdog, measured benchmark and sequential promotion confirmation. This is a concrete narrow optimizer design, not an unrestricted self-rewriting agent. |
| Creator Nexus `base44/shared/nexusLearning.ts:3` | Source explicitly separates saved skills, symbolic sandbox scores and observational guidance from model-weight training, and treats saved reference text as untrusted. Preserve that boundary. |
| Studio baseline `gradlew --offline :check` | Successful current-source check. Existing JUnit compilation/results were up-to-date; the executable integration harnesses ran. No new application code was introduced by this review. |

## Ranked findings

Severity vocabulary: **BLOCKING** means the named normal workflow cannot meet its contract; **REAL** means a reproduced or identifiable conditional defect; **COSMETIC** means misleading presentation without an execution defect. Findings are scoped to their named implementation, not projected onto every repository.

### 1. Completion and queue ownership are not reliable enough for autonomous integration

**Agency `fraymus_offline_agency.py:296-302` — REAL; BLOCKING when completion is used as authorization to promote a result.** A current task with one successful script was marked `COMPLETED` when its final summary cited an eight-character hash prefix of an unrelated previous failed receipt. `judge` searches the entire database and does not bind the cited receipt to the current task or result. Read-only tasks also cannot complete without running a script, even when execution is unnecessary.

**Agency `:94-103,310-318` — REAL.** Changing a receipt's `node_id` and `task_id` left verification green (`4 checked, 0 mismatched` in a fixture). Those fields are outside the hash. All 13 original hashes passing does not contradict this: they verify the narrower payload format.

**Agency `:320-330` — REAL; BLOCKING with multiple workers.** Two connections read the same pending task, then both claim updates succeeded. Startup also marks all `WORKING` tasks failed, including a task still owned by another worker. Use atomic claims, leases, fencing and explicit crash reconciliation.

### 2. The current 120B GUI request contract is too short

**Studio `OllamaIntegration.java:25`; `FraynixWorkspace.java:94-98` — BLOCKING for requests exceeding 120 seconds.** The screenshot's failures agree with the configured 120,000 ms timeout. Previous documented CPU measurements found a 341-second cold load and 136.7-second warm scene proposal. The service is local and reads the actual approximately 65.37 GB F: weight blob; using a loopback URL is how Studio reaches that local runtime.

Chat also forces `num_ctx=8192`, includes prior successful exchanges without a token budget and stores full scene JSON inside the displayed human prompt. This explains the noisy chat and increasing context cost. A source method that accumulates streaming chunks internally does not provide incremental UI updates.

### 3. Offline configuration and process scope disagree with their labels

**Agency `.env`; `fraymus_offline_agency.py:237-247` — REAL; BLOCKING for offline-only operation when this configuration is used.** Supplied configuration selects an external router first, despite the README's no-cloud statement; local Ollama is a fallback. Environment overrides can change this, so this is a configuration/source finding, not a claim that a cloud request occurred during review. The displayed active-model label also follows configured backend rather than the provider that actually answered after fallback.

**Agency `:154-172` — REAL boundary gap.** Entry-path confinement does not confine the subprocess. A harmless fixture script inside the skills root successfully wrote an audit marker outside that root. Scripts run with the user's permissions. This is not an OS sandbox.

**Agency `:293` — REAL.** Slicing serialized tool output can cut a JSON object mid-value. Preserve structured results and store large outputs separately. A controlled failed CLI task also reported `FAILED` while exiting with code 0, unsuitable for supervising jobs.

### 4. Supplied Dreamer artifacts do not yet implement the advertised factory

**`Fraymus-Dreamer.rs:6,56-82` — BLOCKING for building the package as supplied; REAL for capability presentation.** Direct compilation fails on missing `wasm_bindgen`; no Cargo manifest is supplied. In a native probe with only the binding import/attributes removed, `dream()` returned an announcement about ten ideas, and `create_employees()` returned eight role strings. Neither generated ten candidates nor launched persistent agents. This is a prototype stub, while related GitHub projects contain additional implementations that should be reused.

**`fraymus.data.wat:19-21` — BLOCKING.** The original module fails WASM validation with stack underflow. In a probe-only copy, inserting the missing stack operand permitted execution, revealing further mismatches: `resonance(0)=0` instead of 1; `radius(0.5)=8.090...` instead of 3.25779.... A 145-byte header at offset 0 overlaps a segment beginning at 128, replacing 17 header bytes.

**`Fraymus-Dreamer.rs:45-47,75-76,94-95` — REAL.** Anagrams `ab` and `ba` yield the same angle, while degree/radian units are mixed. Adding NaN poisons all later profit totals. `package_media` produces invalid JSON for quotes/newlines. Invalid importance values also produce surprising clamped results. Use typed serialization, finite validation and consistent formula vectors.

**Makefile and manifest references — BLOCKING for those targets.** Referenced Dreamer/employer/profit/branding/deploy scripts, compiled WASM and the named Java RSI engine are absent from this bundle. The small GGUF metadata JSON is not model weights. Install/deploy/clean targets were not run.

### 5. GitHub Transmuter needs a stronger import/verification contract

**Transmuter `src/lib/fraymus.js:197,267-313` — REAL; BLOCKING if `valid` is interpreted as full container validation.** A zero-byte input generated a WASM module but decoded to `null` and failed verification. Changing the module version to an invalid version still returned `valid=true`, while `WebAssembly.validate` returned false. Changing the ledger's SHA field also returned `valid=true` with `ledgerMatchesActual=false`.

The non-empty byte round trips above remain valid positive results. The current verifier strongly checks recovered payload bytes and size; it does not make every metadata field or the executable container valid. The exported `execute(42)` returns 42: the container preserves original bytes but does not automatically compile arbitrary original software into WASM behavior. Extend validation and define separate payload, structure, metadata and signature results before importing this into Java.

### 6. Existing Java persistence has a Windows portability issue

**Transmuter `src/components/ouroboros/runner/OuroborosState.java:109` — BLOCKING on the reviewed Windows/JDK 21 path.** The original state test and its five required sources compile. Running it passed `empty directory loads no fabricated state`, then failed its first save with `AccessDeniedException` while opening the directory for flushing. The POSIX-style directory force needs a platform-aware durability adapter. Remaining persistence assertions were not executed after that failure. Do not discard the journal design; port and retest it.

### 7. Some Base44 skill outcomes are generated drafts rather than completed external actions

**Creator Nexus `base44/shared/nativeSkillExecutors.ts:462-587` — REAL when output is labeled as completed execution of the advertised skill.** Specific branches implement HTTP acquisition and deterministic computation. The generic branch calls `Core.InvokeLLM` to generate JSON, an artifact and a standalone script, then records a completed native job. Generating that script does not execute it. Port each branch with a declared operation/result type; preserve `DRAFT`, `EXECUTED`, `VERIFIED` and `PUBLISHED` separately.

**`motorDispatcher.ts:3-74` — COSMETIC if displayed only as configuration; REAL if displayed as current telemetry.** Several node telemetry values are literal configuration strings. Service observations need timestamps and live measurements. No claim is made here that every UI shows those values as live.

## Recurring-defect checklist

| Check | Result in this slice |
| --- | --- |
| Discarded/ignored results | Finding: Transmuter computes ledger match flags but excludes them from overall `valid`; generic generated scripts are not executed by that branch. |
| Hardcoded results | Finding: Dreamer role/awareness/dream strings and some node telemetry are literal. Positive counterexample: mail signatures, recovered bytes and agency receipts are computed. |
| Complexity claims | Finding: `hdc.js` XOR/permutation loop over 256 words; O(D/64), constant only when D is fixed. It does not establish O(1) retrieval over an expanding corpus. |
| Naming versus mechanism | Finding: distinguish container encoding, inference, script generation, execution, signature and training. The Java optimizer already states its limited trusted grammar. |
| Manufactured latency | Clean in reviewed executable paths: no delay used as evidence of intelligence. Queue polling and watchdogs are operational scheduling, not fake computation. |
| Calibration | Unverified: Phi/novelty/profit constants are not established quality metrics by these probes. Preserve author branding; benchmark control parameters. |
| Edge-case consistency | Finding: empty files, NaN/Infinity, JSON escaping and WAT/Rust formula differences. |
| Positional identifiers | Clean for the reviewed agency receipt/task primary keys and Studio persistent IDs; dynamic queue ownership is a separate defect. No guarantee for unreviewed app components. |
| Unvalidated operands | Finding: generic script arguments/effects, loose hash-prefix completion and non-finite numeric inputs need explicit boundaries. |

## Terminology corrections for integration

| Current phrase | Accurate implementation boundary |
| --- | --- |
| Custom model / new model | Installed model configuration; distinct trained weights only when artifact evidence establishes it |
| Absorbed skill | Documented, installed, executable and tested are separate availability states |
| FRAY header / Phi seal | Version/identity branding; Ed25519 signature only when actual signing and verification run |
| Reversible WASM | Byte-preserving container in the tested Transmuter, with separate executable semantics |
| Agent factory | Persisted specifications plus scheduled tasks and verified artifacts; role strings alone are templates |
| Self-learning | Persistent lessons, retrieval and routing; weight updates require a training run and measured checkpoint |
| Quantum / consciousness proof | Specific classical numerical tests or research hypotheses; these results do not establish physical quantum computing or consciousness |

## Couldn't verify

- Live health or end-to-end execution of either deployed Base44 app, their database state, private relays, media providers, training jobs or remote runners.
- A complete Rust/WASM build of the supplied Dreamer bundle; dependencies/build manifest are missing. The adapted native probe is explicitly not a WASM build.
- The full Java quantum runner: its persistence subset compiled, but the Windows save failure prevented the state suite; the complete runner was not certified.
- All external skill implementations, all 280 GitHub repositories, or all source lines in the two large app trees.
- General model superiority, reliable 32K GPU fit, model training, self-awareness, revenue or cryptographic guarantees from prompt text or branding.
- Claims about proprietary third-party model internals in supplied narratives; those are not implementation specifications for Studio.

The evidence supports a much larger reusable foundation than the three local exports alone suggest. The integration should preserve that work, test its contracts and bring selected mechanisms into the existing Java engine rather than launch another disconnected product.
