# Fraynix Studio: complete local creator and agent factory plan

Prepared October 22, 2026 for Vaughn Scott. This is an implementation plan, not a claim that future capabilities already ship. See the [source review](MODELS_AGENCY_REVIEW.md) and [measured evidence](evidence/models-agency-review-2026-10-22.json).

## October 23 implementation progress

Streaming Chat, collapsed scene attachments, runtime profiles, load/unload, connection checks, cancellation and a visible live-chat build title are implemented and tested. See [live model controls](LIVE_MODEL_CONTROLS.md). Full migration, server supervision, native agency/factory, digest-based model selection and Base44 integration remain planned; the original phase gates below still apply.

## Product goal

One desktop creator workspace should create, inspect, process, simulate, save and restore entities, components, data, software artifacts and agent specifications. Local models should reason over the current space and relevant memory, invoke typed operations, produce reviewable results and learn reusable construction patterns from verified outcomes.

The engine should run without internet for ordinary local creation. Java remains the control plane and default implementation language. Existing GUI/physics libraries and Ollama retain their specific roles. Optional web/media/provider connectors stay explicit capabilities; no automatic cloud fallback belongs in an offline job.

The terminal is optional administration. Each supported operation should eventually be available through a relevant GUI form or command palette backed by the same service and schema. The two existing Base44 applications can remain companion frontends, references and optional connectors. They are not prerequisites for local creation, and they should not be overwritten by this integration.

## Target system

```text
Studio GUI / optional CLI / optional authenticated companion interface
                              |
             OperationRegistry + ProjectSessionCoordinator
                              |
       FactoryWorld / JobSupervisor / AgentOrchestrator
             |                 |                 |
       SceneProcessor     ModelSupervisor    Typed tool dispatcher
       JBox2D preview     Local Ollama       Native Java handlers
             |                 |                 |
       Transaction -> validators -> reviewed commit -> receipts
                              |
        Checkpoints + artifacts + memory + repair/recipe lessons
```

The GUI owns mutable editor state. Background workers consume immutable observations and post staged commands back to the owning session. Models cannot mutate the scene, grant permissions or declare verification successful merely by returning text.

### Reuse map

| Existing source | Reuse in Studio | Integration action |
| --- | --- | --- |
| Studio `factory-runtime`, `FactoryWorld`, checkpoint store | Entities, stable IDs, schema validation, transactions and real-time snapshots | Extend schemas; preserve tested save/restore and physics paths |
| Studio `FactoryKernelService`, `LocalSkillDispatcher`, offline kernel | Tools, retrieval, verification, evolution and journals | Extend existing contracts instead of adding a parallel agency |
| `FactoryModelService`, `OllamaIntegration`, `FraynixWorkspace`, `SceneAgent` | Multi-store models, chat, observations and scene creation | Add profiles, streaming, job supervision and model identity |
| Supplied Python agency | Tool-loop behavior and legacy receipt history | Port behavior to typed Java; migrate records as legacy evidence |
| Model exports / compact installed Ultimate model | Role prompts, templates and profiles | Normalize recipes; preserve source provenance; benchmark before routing |
| Transmuter `fraymus.js`, `glyphsmith.js`, protocol libraries | Reversible containers, codecs and transport | Implement JDK counterparts with cross-language fixture vectors |
| Transmuter signed mail | Actual v13 identities/signature/canonical message format | Use JDK Ed25519; verify interoperability without copying a custom crypto implementation unnecessarily |
| Transmuter `OuroborosState`, `KernelSource`, evaluator/runner | Durable optimizer patterns, finite candidate grammar and benchmark confirmation | Port durability to Windows; retain narrow tested scope; reconcile with existing mutation engine |
| Creator Nexus entities, learning, runner and dispatch contracts | Agent/job definitions, examples, artifacts and provider metadata | Translate useful contracts into native schemas; replace cloud SDK storage in the offline path |
| Base44 media/knowledge/director workflows | Persistent multi-stage creator recipes | Separate deterministic local steps, model-generated drafts and optional remote providers |
| Dreamer manifest, role list and bundle | Product templates and design inputs | Implement real agents/jobs/artifacts; do not treat labels or prompt declarations as handlers |

Only versioned selected sources should be imported. Record origin repo/commit/file/license and local adaptation in provenance. Do not mirror private databases, raw credentials, large model blobs or whole frontend dependency trees into Studio.

## Phase 0: make the running application unambiguous

Deliver a single canonical launcher and display build commit, version, project directory and model endpoint in the System view. Distinguish the old `Vaughn-Scotts-Fraymus-Engine` from independent `Fraynix Studio`.

Add a project import preview for `%USERPROFILE%\.fraymus\projects\fraynix-studio` into a chosen Studio project. Require a saved checkpoint, copy it with a manifest, verify object/component IDs and authored values, and retain the original backup. Preserve conversation/receipt provenance with format versions. Unsaved editor changes cannot be inferred from an older file; show that clearly before import.

Acceptance: the user's saved 15-object scene, rigidbody/collider settings, grid positions and identities survive import, save, restart and restore. Existing source repositories and projects remain usable. No automatic closing of the old application or substitution of a sample scene.

## Phase 1: dependable local models, starting with 120B

Introduce `ModelSupervisor` and per-model `ModelProfile` around the existing adapter. Include context budget, prediction budget, reasoning mode, threads, GPU settings, keep-alive, load/request/idle allowances and measured memory use. These are operational settings, not arbitrary capability caps.

The 120B profile should begin with the successfully measured CPU/4K/low-reasoning baseline and an allowance compatible with the observed 341-second cold load; the earlier successful isolated server used 15 minutes. A request timeout alone does not extend Ollama's server-side load timeout. Inspect the actual service configuration and residency before starting a heavy request. Do not restart or kill a shared Ollama service silently.

Implement actual incremental stream events from the HTTP reader through model service, job supervisor and GUI. Expose elapsed time, loading/queued/generating state, first token, first visible answer, token counts and final load/prompt/evaluation durations. Show unknown progress honestly; no invented loading percentage. Add cancellation with a clear distinction between client disconnection and confirmed runner termination.

Separate the human message from its scene attachment. Store scene snapshot hash/version, collapsed observations and retrieval references separately. Budget history and attachments by model profile; retain full source outside the prompt and summarize/retrieve when needed. Remove the unconditional 8K override. Serialize heavy jobs according to observed host capacity; a small model need not be unloaded merely because a larger model exists.

Acceptance: a request exceeding 120 seconds remains responsive, streams real events, can be cancelled, and records its actual endpoint/model/settings. Cold and warm 120B scene proposals are measured separately and pass schema/physics checks. A responsive editor is required throughout loading.

## Phase 2: import and manage the user's model family

Create explicit identities rather than keying solely by tag:

```text
ModelIdentity = storeId + endpoint + exactTag + manifestDigest
                + weightDigest + adapterDigests
ModelProfile  = identityRef + templateVersion + resourceSettings
                + measuredCapabilities + benchmarkReferences
```

Parse model recipes and normalize PowerShell `.Name.value` exports. Preserve `FROM`, SYSTEM, template, renderer/parser, stop parameters and context settings without executing the file. Group shared artifacts for disk accounting, while retaining distinct configurations. Keep same-tag/different-store entries separate and visible. Unknown/ambiguous identities should not silently switch models.

Keep a compact identity/tool contract in SYSTEM. Put long protocol documents and useful history in versioned retrieval packs. Exclude embedded web boilerplate and copied private deployment details from the default core prompt. Preserve the original export for provenance. Do not mutate existing installed tags just to clean an import; create an explicitly named derived configuration if needed.

Test template rendering, tool schemas, stop tokens, JSON output, multi-turn behavior and context pressure for each family. Installed Ultimate is a candidate, not an automatic winner. The Dreamer recipe needs installation and tests before it becomes selectable as a verified profile. Vision receives actual captured pixels through a tested adapter; structured component observations remain available independently.

Acceptance: importer accounts for all 32 recipes, identifies five shared weight groups, rejects malformed metadata, excludes cloud proxies in offline mode and explains which endpoint owns each selection. Neither recipe combination nor adding skills is labeled weight training.

## Phase 3: native agency and durable job processing

Implement the agency loop inside the existing Java kernel/tool registry. Support native function calls and a strict schema-checked JSON fallback for models that require it. Tool grants come from the host/project configuration, never a model response, imported SKILL.md or SYSTEM paragraph.

Suggested contracts:

```text
AgentSpec(id, templateVersion, modelRef, capabilityRefs, workspaceScope,
          memoryNamespace, outputSchema, validationPolicy)
TaskRun(taskId, goalHash, agentId, attemptId, modelIdentity,
        leaseOwner, leaseEpoch, leaseUntil, state, receiptHead)
OperationDescriptor(id, version, inputSchema, outputSchema, requiredGrants,
                    effects, executionMode, validator, concurrencyPolicy)
ToolReceipt(taskId, runId, attemptId, callId, actorId, modelIdentity,
            implementationHash, inputHash, outputHash, status, start, end)
```

Use a JDK-only durable queue/journal/checkpoint mechanism unless measurements justify another dependency. The existing J2 journal is a useful integrity boundary, but does not automatically make every kernel memory/router write transactional. A `ProjectSessionCoordinator` must serialize workspace writes; CLI commands should attach to the owning session or acquire compatible locks.

Claims must be atomic with owner/lease/fencing information. Reconcile only expired or demonstrably interrupted work. A second worker must not reset another worker's live job. Make retry/idempotency policy explicit for operations with effects. Promise one accepted state commit under the defined protocol, not universal exactly-once execution across arbitrary external services.

A valid result is task-specific: inspecting memory does not require executing a script. Bind completion to the current task's verified receipts and output validator. Preserve full structured results; large stdout/artifacts become referenced blobs with hashes rather than cut JSON strings. CLI failures return nonzero statuses.

Import legacy SQLite records through an offline migration/export utility. Verify their old payload hashes, retain IDs and provenance, and label omitted task/actor binding accurately. Do not rebrand old hashes as authenticated new receipts. Raw database/private conversation import is opt-in project data, not a system policy update.

Acceptance: two competing JVM workers produce one accepted lease/commit; crash/restart preserves history; foreign receipt prefixes and retargeted task metadata cannot complete jobs; cancelled/failed/read-only jobs have correct statuses; loopback-only mode never falls back to a cloud router.

## Phase 4: real component, data, software and agent factories

Extend the schema registry with versioned components such as `agent.spec`, `model.profile`, `processor.spec`, `job.state`, `artifact.ref` and `recipe.ref`. Keep scheduling state distinct from authored definitions. An agent is a persisted specification with a model, allowed operations, memory namespace, goal and validators; creating eight agents does not require loading eight weight copies.

Turn Dreamer into a tested pipeline: generate a configurable set of structured candidates; check constraints; score novelty against available corpus/history and utility against the user's goal; retain evidence; select or request review; create real child jobs. Role names such as Builder, Critic, Designer and Coder become templates pointing to concrete handlers/providers. Persist all specifications and results across restart.

Use the existing shadow mutation/evolution path for software edits: baseline, detached candidate, compile/tests/behavior checks, compare evidence, reviewed promotion and rollback. Incorporate successful repairs and creation recipes into project memory. A compilation-only stub is not a functional implementation; validators must exercise intended behavior. Adapt the finite Java optimizer as one narrow benchmark lane after its portability and complete build are verified.

Phi can remain part of identity, layout or an experimental strategy. Quality/routing decisions require comparison with straightforward baselines. Novelty, coherence, consciousness, profit and security are different measurements; one numeric constant cannot substitute for all of them.

Acceptance: factory creation produces saved entity/agent definitions, executable queued work and validated artifacts; restart restores them; a rejected candidate does not alter the live scene or erase prior versions.

## Phase 5: capability parity through selected native implementations

Build an operation catalog that drives **GUI, CLI and agent schemas from one registry**. Each entry records native implementation, optional provider, grants, dependencies, availability, last verified time and result type. Import the fleet's named skills as candidates, not automatically executable scripts.

Prioritize local mechanisms already present across the apps:

1. Reversible file/WASM containers, Unicode byte codecs, byte/metadata manifests, SHA-256 and v13 signature interoperability.
2. Search, code AST/diagnostics, document generation, ZIP packaging, data transforms and persisted memory/artifact queries.
3. Agent/job queues, workflow recipes, benchmark/repair memory and continuity import/export.
4. Local media exports where the JDK suffices: Java2D PNG/SVG generation, AudioSystem WAV handling and frame/scene export. Neural speech, video encoding and model training need explicit local providers and measured dependencies.
5. Optional remote acquisition, connected services and publishing behind separate capability grants.

Use JDK `MessageDigest`, `Signature`/Ed25519, `ZipOutputStream`, NIO and HTTP libraries where appropriate. Preserve byte order, schema versions and canonical signing bytes. Do not fork a second cryptographic implementation merely to avoid a JDK API. Keep namespace/version distinctions between file DNA fingerprints and semantic memory vectors.

Acceptance: a per-skill coverage table distinguishes **documented**, **installed**, **implemented**, **tested** and **provider available**. Generic LLM script generation returns a draft; only an actual execution receipt upgrades it to executed. No claimed savings, deployment or media output without the corresponding artifact/evidence.

## Phase 6: complete, relevant desktop controls

Keep current Scene, Properties, Assets, Chat, Create, Models, Kernel, Tools and Jobs functional. Add views when their services exist, rather than adding placeholder tabs:

| View | Actual data and actions |
| --- | --- |
| Models | Store/endpoint identity, digest, resource profile, residency, preload/unload, benchmark and failure state |
| Agents | Persisted specs, roles, model/tools, memory namespaces, create/clone/edit/start/stop |
| Jobs | Durable queue, leases, current stage, elapsed time, cancellation, retry policy, receipts and artifacts |
| Memory | Ingest/retrieve, provenance, namespaces, examples, repair lessons, retention and benchmark results |
| Artifacts | Inspect payload/metadata/signature checks, export/import, previous versions and restore preview |
| System | Version/project, service health, permissions, journal continuity, configuration and explicit recovery |
| Advanced operations | GUI forms/command palette for profile, benchmark, distillation, replay, propose/evolve and administration |

All views read service snapshots/events. Do not print a full scene JSON into the main chat conversation by default. Use inspectable attachments and source links. A generic terminal may be optional for advanced users, but launching a shell must not be the hidden implementation of every creator function.

Acceptance: the same operation invoked from GUI and CLI produces equivalent schemas, results and receipts. Every displayed live number has an observation source/time. Disabled functionality explains the missing local provider or configuration.

## Phase 7: continuity, portability and optional substrates

An artifact package should retain exact bytes, metadata, source identities, payload hashes, metadata hashes, schema versions and optional signatures. Test empty/binary/Unicode inputs, escaping, truncation, duplicates, large-file limits and restored references. Verify payload, container structure and signatures independently; a green result should name its exact scope.

Keep trusted journal heads/identity keys separately when the threat model requires authenticity or rollback detection. An internal hash chain is not sufficient against a party that can replace the entire chain. Measure journal growth and implement segmentation/checkpoint anchors if warranted; do not silently change existing journal semantics.

Port the existing Java optimizer's POSIX directory flushing to a documented Windows-aware durability strategy. Keep current Studio checkpoint guarantees and clarify crash windows. Fix Dreamer WAT stack/arithmetic/overlap issues only if that backend is adopted; establish Java golden vectors first, then run Rust/WASM conformance. Do not require Rust/WASM for ordinary Studio operation.

Acceptance: corrupted payloads, metadata and signatures yield distinct failures; exact bytes and archived content restore after restart; original imported artifacts remain recoverable; backend differences cannot silently change mathematical results.

## Phase 8: optional Base44/fleet integration

The user-confirmed Transmuter MCP URL is `https://fraymus-transmuter.base44.app/api/mcp`; its login page is another endpoint of the same app. Public checks found a reachable login shell and an OAuth-protected MCP endpoint returning 401 without authentication. Its protected-resource metadata names `https://app.base44.com` as authorization server. Authenticated `tools/list` must precede any claim about available MCP functions or private data. Do not assume every backend function in GitHub is exposed through MCP.

Add an optional authenticated MCP adapter if its discovered tools provide useful companion access. Record tool schemas, permissions, provider identity and results under the same operation registry. Start with read-only discovery and reference retrieval. Keep remote model invocation, modifications, messages, paid calls and publication separate from read access. A hosted MCP endpoint does not replace the local kernel or make the offline creator depend on Base44. The OAuth scope named `offline` is not evidence that a hosted MCP service runs without internet.

Keep both Base44 apps intact. Implement explicit import/export adapters for agent definitions, artifacts, continuity and selected task schemas. Pin protocol/source versions and map cloud entity IDs into namespaced local IDs; never equate a localhost endpoint with an internet-hosted worker. Store receipts from external systems with their own trust/provenance labels.

Connected workflows can offer remote media, scraping, calendar, collaboration and publication. They should be disabled in offline projects and show the actual provider, network requirement and any costs. Use existing user-authorized persistent grants; avoid repeated prompts for routine authorized local work. Messages, posts, paid actions and publication require the user's explicit authorization for those actions.

A business workflow may track actual sourced transactions and costs; adding a number to `profit_today` does not establish revenue. Research/legal role templates are tools for organizing evidence, not automatic legal clearance or guaranteed commercial success.

Acceptance: offline workflows work with connectors absent; a failed remote provider does not trigger an undisclosed fallback; drafts and published external artifacts have separate states and receipts.

## Verification matrix and forecasts

| Gate | Required evidence before promotion |
| --- | --- |
| Existing creator regression | Grid snapping, negative coordinates, inspector edits, rigidbody/collider behavior, play/stop, preview/apply/undo and restart |
| Model lifecycle | Real cold/warm small-model and 120B measurements, bounded prompt budget, actual streaming, cancel/retry and endpoint/digest identity |
| Job integrity | Multi-worker claim race, restart during each stage, stale lease, duplicate delivery, foreign receipt, nonzero failure exit |
| Capability boundary | Prompt/skill text cannot grant filesystem/network/process rights; script entry confinement is not presented as sandboxing |
| Artifact conformance | Empty and binary byte round trips, malformed WASM/version/length, tampered metadata, signature mismatch, large files and Unicode |
| Memory/self-building | Successful lessons persist and affect later candidate generation; failures/rejections remain recorded; no automatic live-core overwrite |
| GUI parity | Every exposed operation uses the shared registry and shows real state rather than static demo telemetry |
| Companion interoperability | Offline import/export fixtures first; live read-only checks next; authorized effectful integration only after concrete review |

Near-term forecast: phases 0â€“2 resolve the visible build/model confusion and make large-model use dependable. Phases 3â€“4 establish the actual local agent factory. Phases 5â€“7 broaden verified capability and continuity; phase 8 adds optional network reach without making it a core dependency. These are ordered engineering gates, not guaranteed dates or claims of AGI/market superiority.

Streaming Chat, per-model runtime profiles, explicit failures, lifecycle controls, conversation continuity and clean scene attachments are implemented; server-side cold-load supervision still needs fresh 120B validation. See [live model controls](LIVE_MODEL_CONTROLS.md) for the current tested surface.

The newer [Dreamer 120B review](DREAMER_120B_REVIEW.md) confirms the F: base-model manifest and documents why the supplied daemon, portal and B200 scripts cannot be imported as completed capabilities. At the user's follow-up request, the reviewed inference recipe was [installed, tested through Studio and published to their Ollama account](DREAMER_120B_SETUP.md); those separate scripts remain inactive. The next implementation slice is the **shared typed model/tool loop**, using the existing dispatcher, host grants, world snapshots, conversation evidence and Jobs surface. Artifact recovery and optional receipt-backed training follow its verification gates.


## Full game integration on October 28 2026

Edition 002 restores the original Mario game in Studio and adds fixed simulation ticks, actor input leases, a local geometry controller, and a headless Hrm cortex with actual sensory channels and JSON checkpoints. Full-level round trips and a native walk, jump, enemy-motion and authoring-restoration diagnostic pass. Scene proposals can apply automatically after the existing preview and transaction checks; Chat JSON and reasoning no longer execute edits.

The next gates are synchronized viewport images for visual models, complete episode recording and replay, human demonstrations, measured motor-policy improvement on held-out tasks, and outcome-based evolution with rollback. The optional neural jump output and synaptic plasticity remain experimental. The original browser BinaryNervousSystem and BaremetalLLM need their own parity and training evaluation. General self-modifying code execution remains a later isolated-worker milestone.


## Review corrections in edition 003

Implemented and tested: position-based motor recovery with safe backoff and bounded retries; enemy landing alignment; explicit asset selection versus placement; Save, Import, Export and Reload controls; model completion-capability validation and mounted-drive store discovery; bounded scene summaries, component queries and context budgeting. The packaged original-level route reaches x=12.158443 with no forced jumps. Complete-level solving and learned policy improvement remain separate gates. Desktop file-dialog clicks were not verified because Windows capture timed out; native file event paths and persistence tests pass.
