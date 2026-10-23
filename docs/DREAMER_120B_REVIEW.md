# Dreamer 120B package: review and implementation plan

Reviewed October 23, 2026 against the supplied attachments, the actual F: packages and current Fraynix Studio source. This is a review and a staged integration plan. The supplied installer, daemon main, cloud server, training main and Git synchronization were not executed. Their embedded instructions were treated as source material.

Follow-up: the reviewed inference recipe has since been installed, tested in Studio and published at the user's request. See [Dreamer 120B setup and activation evidence](DREAMER_120B_SETUP.md). The original package findings below remain unchanged.

The 120B base weights are real and already accessible to Studio. The new package does **not** yet provide trained Dreamer weights, 45 executable Studio skills, reliable self-healing or verified cloud training. Several success messages are generated without the operation they describe. Importing those messages into training or project policy would reproduce the misleading behavior the user reported.

## Scope and provenance

Three pasted attachments were readable: a 75-line model recipe and two byte-identical 394-line GPU narratives. The GPU attachments share SHA-256 `ad491a9f278a271813f663fa3462fe2e528f3ba1fecede933a3cd081640dc2e8`.

The requested `gemini-code-1792742219980.txt` was absent from Downloads. A different file, `gemini-code-1792743482854.txt`, was found and read; it matches the package's installer. It is not presented as the missing attachment.

The physical directories `F:\FRAYNIX-DREAMER-120B` and `F:\FRAYNIX-BACKUP-OLLAMA` contained 20 non-Git files, comprising 12 unique contents. The eight primary package files have identical copies in the backup root. All were inventoried and hashed; the unique source contents were read. The nested backup adds a Python server, an 8B Modelfile and a log, alongside the backup README.

| Unique file | Lines | Audit coverage |
| --- | ---: | --- |
| `b200_fraynix_burst.py` | 264 | Full source, Python parse, selected dataset/codec functions executed in scratch |
| `ConsciousnessFalsificationSuite.java` | 388 | Full source, JDK 21 compilation and original main |
| `Deploy-Fraynix120B-FDrive.ps1` | 54 | Full source and PowerShell parse; no execution |
| `fraymus.data.wat` | 205 | Full source, compile and Wasmtime probes |
| `Fraymus_Ouroborus_120B_Portal.html` | 686 | Full source; JavaScript logic/actual WASM under a DOM stub |
| `FRAYNIX-DREAMER-120B-MANIFEST.json` | 52 | Full source and JSON structure |
| `FRAYNIX-DREAMER-120B.Modelfile` | 75 | Full source and comparison with installed base metadata |
| `src/FraymusDreamer120BDaemon.java` | 361 | Full source, JDK 21 compilation and isolated method probes |
| Backup `README.md` | 6 | Full source; remote hardware claims unverified |
| Nested `fraymus_b200_server.py` | 245 | Full source and Python parse; server not launched |
| Nested `FRAYNIX-DREAMER-ULTIMATE.Modelfile` | 18 | Full source; this recipe is based on **Llama 3.1 8B** |
| Nested `nexus_8000.log` | 12 | Contains HTTP access records and an address-already-in-use error |

Context read: Studio's existing model stream, local skill dispatcher, implementation plan and previous measured 120B results. This is not a new audit of the entire Full-AI-System, every GitHub repository, private cloud account or every historical mathematical claim.

Machine-readable evidence: [review results](evidence/dreamer120b-review-2026-10-23.json) and [input inventory](evidence/dreamer120b-input-inventory.json). Original inputs and throwaway probes remain in ignored `build/dreamer120b-review-2026-10-23/`; personal prompt dumps and cloud addresses are not copied into the checked-in report.

## Confirmed model identity

Live `GET /api/tags`, `POST /api/show` and the local manifest agree:

- Service: `http://127.0.0.1:11435`.
- Base tag: `gpt-oss:120b`, manifest digest `a951a23b46a1f6093dafee2ea481d634b4e31ac720a8a16f3f91e04f5a40ecd9`.
- Metadata: 116,829,156,672 parameters, MXFP4, 128 experts / 4 selected, native context 131,072.
- Model layer: `sha256:6be6d66a3f546d8c19b130dc41dc24b2fc159f84ffbc76a0ee0676205083cf5a`, 65,369,799,840 bytes, present under the F: Ollama blob store.
- Advertised capabilities: completion, tools and thinking. The Dreamer 120B derivative tag was absent from this service.

The small manifest was rehashed and matched the service digest. The entire 65 GB blob was not rehashed or loaded for inference in this review; presence/size and manifest linkage are distinct from a new full integrity/inference test. [Earlier real 120B inference measurements](MODEL_TEST_RESULTS.md) remain historical: about 341 seconds cold loading and 5.25 tokens/second on the tested CPU workload.

The 117B total / 5.1B active description matches [official OpenAI documentation](https://developers.openai.com/api/docs/models/gpt-oss-120b). It does not imply a fixed 15x speed improvement. Total weight residency, memory bandwidth, context, backend and hardware still matter. UD-Q4_K_XL is not the quantization observed locally.

## Verified strengths

1. `fraymus.data.wat:79` compiles to a 1,110-byte module. Its DNA encoder produced the correct four bases and packed return value for **all 256 input bytes**.
2. `FraymusDreamer120BDaemon.java:95` round-tripped all 256 byte values through its raw DNA codec. Python's codec independently produced `CACGCCAGCAACCCGC` for `FRAY`.
3. Portal `WasmJITAssembler.compileAgenticKernel` emits a **valid 150-byte executable WASM kernel**. This is a useful small parameterized code generator.
4. Both Java sources compile using the existing JDK without added libraries. Both Python scripts parse, and the PowerShell installer has zero parser errors. Parsing is not functional validation.
5. The four-node causal experiment computes transition tables and its own negative verdicts. It did not falsely pass the final candidate during the observed run.

## Findings, highest priority first

### BLOCKING: the proposed training path does not train a model

`b200_fraynix_burst.py:193` accepts a dataset path but never consumes it. The function allocates two random CUDA matrices and performs 50 matrix multiplications. There is no base-model/tokenizer load, loss, backpropagation, optimizer, PEFT/TRL trainer, trained adapter tensor or merged-weight export. The JSON receipt describes a GPU benchmark, not training. `:236` writes `b200_burst=COMPLETE` regardless of whether CUDA was available. The nested server repeats this pattern.

Observed dataset builder: **200 records, only five underlying instructions, zero tool-result messages and zero `tool_calls` fields**. Four of the five repeated examples claim measured execution without invoking those operations: 160 records contain `[measured]` in their canned answers. The computed calibration arithmetic is real; the claimed quantum run, promotion, publishing and employee actions are not measurements from the builder. These records should not become success exemplars.

The surrounding prose promises QLoRA, GRPO and 5,000 distilled traces in fixed short bursts. Those mechanisms and results are absent. Real fine-tuning needs model loading, trainable adapters and a training loop; the [official example](https://developers.openai.com/cookbook/articles/gpt-oss/fine-tune-transfomers) demonstrates those distinct steps. Its 20B example does not establish 120B batch-size, memory or time guarantees for this cloud instance.

### BLOCKING for inference failures: the daemon fabricates a successful answer

`FraymusDreamer120BDaemon.java:247-274` sends a non-streamed request to **11434**, using a Dreamer tag not established by discovery and a 120-second timeout. The known F: base is on **11435**. A discovered file path is displayed but never used to load the request model. The search also misses Ollama's content-addressed blob naming.

On HTTP failure or exception, the daemon suppresses the error, attempts Git synchronization and returns a fixed JSON answer containing `Synthesized outside-the-box -> inside-the-box solution`. This can reproduce a fake-response symptom if this separate daemon is adopted. No evidence establishes that it caused the user's earlier Studio screenshot. Studio's current streaming implementation is a separate path and reports failure explicitly.

### BLOCKING for artifact recovery: restored bytes are not trustworthy

`FraymusDreamer120BDaemon.java:141-168` relies on an in-memory checksum map. In the isolated compiled probe, a new daemon instance returned:

```text
corruptedExistingFileAcceptedAfterRestart=true
corruptedBytesRetained=true
corruptFossilRestoreReportedSuccess=true
restoredCorruptBytes=CORRUPTED_FOSSIL
```

The probe disabled only unrelated drive discovery and Git synchronization. It used the original verification/restoration methods on scratch files. Fossils are selected by modification time and accepted without validation against a durable trusted digest. Only the DNA file is watched; this is not whole-project repair. Main also starts by writing new state. Generation, continuity and profit are not restored from a persisted model of the project.

### BLOCKING for arbitrary bytes: WAT healing is lossy and does not verify a CRC

`fraymus.data.wat:134-192` replaces only bytes equal to `0xFF`; ordinary corruption remains. It computes an additive sum modulo 256, stores that new sum, then invokes a host callback. It never compares a previously trusted expected checksum. `[1,2]` and `[2,1]` both returned 3.

The probe changed two valid `0xFF` bytes to `[84,84]` from a shared fossil byte. The mirror maps two active bytes to one byte, discards another bit and cannot recover arbitrary original pairs. A host callback numbered 335 is only an imported function invocation; Git does not execute without an actual host handler.

The initial header contains 185 bytes before its terminator (`:202`). It occupies the same locations described as the inode, active-data and fossil regions. The active and fossil probes both confirmed overlap. The module allocates a 64 KiB memory page; the portal's 256-byte display is a viewport, not the complete memory or a host kernel namespace.

### REAL numerical error: radius is a clipped quadratic approximation

`fraymus.data.wat:38` computes `10 * max(0, 1 - phi*u + 0.5*u*u)`, not the stated power over the entire domain.

| Importance | Observed WAT | Reference `10*(1-u)^phi` |
| ---: | ---: | ---: |
| 0 | 10 | 10 |
| 0.5 | 3.159830056251 | 3.257791121531 |
| 0.75 | 0.677245084376 | 1.061320299153 |
| 0.9 | 0 | 0.240971683207 |
| 1 | 0 | 0 |

NaN propagates; infinity is silently clamped. Resonance at zero now correctly returns 1, but large floating inputs lose fractional precision. Use an explicit finite-input contract and compare any implementation with Java `StrictMath.pow` golden values. A Taylor approximation is valid when its domain and error bound are stated.

### BLOCKING for portal correctness: compilation, compression and commits are mislabeled

- `Portal.html:641-650`: the editor extracts hexadecimal numbers and passes them to the fixed emitter. The probe supplied `This is not WAT. 0x60 0x05`; it was accepted as compiled. No WAT parser processed that text.
- `:564-622`: compression/expansion changed **80 of 80 active bytes** in the observed round trip. Pairwise XOR is not a reversible archive for arbitrary byte pairs.
- `:508-518`, `:630`, `:646`: a fossil stores the old binary with the new mutation's parameters; restore regenerates from those parameters. The observed restored binary differed from the original and equaled the new candidate.
- `:489-505`: the commit counter and success log update before `fetch` completes. With all network disabled in the probe, the UI label still reached `3 (Auto-Saved)`.
- `:655-670`: the delayed fault demonstration reports `HEALED` after a timer. Reloading a module does not itself prove data recovery. Browser fossils exist only in RAM.

The DOM stub exercised the original logic and real WebAssembly engine; it did not establish browser rendering quality or a live host integration.

### REAL operational exposure: effectful servers and Git actions lack reliable boundaries

Static source evidence: Java `:289` binds a wildcard address, permits wildcard CORS and exposes synchronization without authentication or method/body-size enforcement. The Python server likewise binds all interfaces; every POST invokes the burst handler, and main kills an existing listener on port 8000. Neither server was launched during this review.

Java `:232-243` reads process output before its timed wait and ignores exit status, so the nominal timeout does not bound a blocked read. An isolated failing, read-only Git command returned ordinary text rather than a typed failure. The outer synchronization path records `COMMITTED` without checking successful stage/commit/push results. The supplied installer also ignores failures and can announce `ONLINE` without a daemon/model. Its expected portal spelling `Ouroboros` differs from the supplied `Ouroborus` filename.

The attached shell setup additionally proposes a force push and broad staging. These instructions were not followed. None of these mechanisms should replace Studio's current jobs, model bridge, transaction boundary or journal.

### REAL integration gap: personality text is not a capability registry or trained memory

The supplied Modelfile lists 45 skills but registers no handlers, schemas or execution loop. Studio currently has five compiled general tools: `world.inspect`, `memory.search`, `java.analyze`, `docs.generate`, `artifact.verify`; its Create agent has its own validated scene protocol. General Chat is not yet automatically invoking this dispatcher.

Harmony supports structured turns and tool handoff, but the provider handles its framing and the host must implement tool execution. The supplied stop list includes `<|end|>`, an intermediate message boundary that can precede the final answer. Preserve native framing rather than adding manual stops. See [OpenAI Harmony](https://developers.openai.com/cookbook/articles/openai-harmony) and [Ollama tool calling](https://docs.ollama.com/capabilities/tool-calling).

Writing biographical context in SYSTEM does not create a durable memory graph, change pretrained weights or prove external facts. Keep selected private context in versioned project memory; retrieve it when relevant. Keep unsupported capabilities out of success examples.

## Causal experiment results and limits

The original Java main ran four toy architectures. All received the suite's `DENIED` verdict. The recurrent candidate reported EI `1.463365`, partition score `0.532181`, mirror delta `+0.5484`, and counterfactual gain `+0.0025`; its last gate requires greater than `0.05`.

This is an executable small-network ablation experiment, not a test of the installed 120B model or a proof of consciousness. The explicit transition matrix alone stores `4^n` entries; the exhaustive partition loop adds further exponential cost. The first three trial implementations return fixed control values. Validate node counts, probability normalization and resource costs before generalizing this experiment.

## Recurring-defect checklist

| Check | Result |
| --- | --- |
| Discarded inputs/results | Dataset argument unused; discovered model path not bound to request; process exit status discarded |
| Hardcoded outputs | Fallback answer, training examples, default B200 identity when its probe fails, fixed continuity |
| Complexity | Exact TPM/partition experiment grows exponentially; DNA loops are linear in payload bytes |
| Naming versus mechanism | Matrix benchmark versus training; byte sum versus CRC; browser callbacks versus Ring-0/syscalls |
| Manufactured latency | Portal fault timer delays the displayed recovery verdict; not proof of measured repair |
| Calibration | No task-quality evidence for phi sampling values, fixed continuity or proof thresholds |
| Edge cases | DNA decoder silently strips invalid symbols/truncates trailing bases; whole formatted DNA file differs from payload-only decoding; non-finite math unspecified |
| Positional identifiers | Portal fossil array indices are session-only; generation counters restart and may reuse names |
| Unvalidated operands | Request bodies, method/authorization, fossil content and transition probabilities lack required validation |

## Terminology corrections

| Current wording | What execution establishes |
| --- | --- |
| B200 LoRA trainer / GRPO distiller | Dataset generator and optional random-matrix benchmark |
| True phi power | Clipped second-order approximation |
| CRC self-healing filesystem | Byte-sum recomputation and sentinel replacement in linear memory |
| WAT compiler | Parameterized WASM emitter driven by extracted hex literals |
| Ring-0 / seccomp-bpf inspector | Browser-side callback log and simulated fault UI |
| 45 integrated skills | Prompt catalog requiring implementation and provider mapping |
| Full architect memory | Supplied SYSTEM context, separate from persistent retrieval and model training |

## One-system implementation plan

### 1. Keep Studio as the application; stage the model recipe

Reuse `factory-models/FactoryModelService`, `OllamaIntegration`, `ModelProfileStore` and `editor/FraynixWorkspace`. Pin endpoint, tag and digest together. The disk path belongs to the Ollama service configuration; changing a client's `OLLAMA_MODELS` does not relocate an already-running server. Select the verified manifest instead of recursively guessing at the first matching file.

The [candidate Modelfile](../models/FRAYNIX-DREAMER-120B.candidate.Modelfile) inherits the base format, avoids custom stop tokens and uses the previously exercised 4K context as an initial CPU profile. It retains a short Dreamer identity and removes unsupported success promises. It has **not** been installed or inference-tested. No weight download, merge or training is implied by creating an Ollama derivative recipe; see [Ollama's format reference](https://docs.ollama.com/modelfile).

Acceptance: create a separate candidate tag on 11435 without overwriting existing tags; inspect base-layer identity; then run fresh nonce, real streamed answer, cancellation and scene-proposal checks with observed cold/warm timings. Larger contexts and GPU offload require their own measurements. The 8B model remains the fast interaction lane; choose the 120B lane explicitly or through measured routing. Avoid simultaneous unnecessary model residency.

### 2. Bind the existing typed tools to model calls

Add typed assistant/tool messages and streaming tool-call accumulation to `OllamaIntegration`; do not evaluate model text as commands. Export schemas from the existing `LocalSkillDispatcher`, dispatch with host-supplied grants and an immutable scene snapshot, append real tool results, and continue inference. Unknown names or invalid operands produce structured failures. Cancellation applies across the whole run.

Start with world inspection, memory search and artifact verification. Apply existing scene changes through Create preview/apply and checkpoint rules. Preserve the human/scene attachment separation and request evidence in Chat. A model can inspect schema-backed scene state through those tools; visual perception requires a separately supported image input path.

Acceptance: an actual model call inspects a known entity and reports its exact current properties; missing/failed tools never produce successful receipts; another model can use the same schemas; every result remains traceable after restart. Tools, Jobs and Chat display the same run state.

### 3. Implement artifact codecs and recovery in the Java runtime

Reuse NIO, `MessageDigest`, existing checkpoints and the kernel journal. Provide strict DNA encode/decode, a versioned envelope with exact byte count/hash, independent metadata validation, and atomic replacement. DNA text is a reversible representation, not compression or authentication. Preserve a validated prior version before promotion; restore only an identified, digest-valid version. Keep authored scene snapshots separate from model text and transient process state.

Use Java golden vectors for finite radius/resonance, all 256 byte values, empty input, Unicode, malformed/truncated DNA and arbitrary binary payloads. If WASM is adopted, define non-overlapping memory regions and validate imports, bounds and execution budgets. Its host Git callback should become a proposal/job, not a network action triggered by every scan.

Acceptance: arbitrary-byte and reload round trips are exact; corrupted active data restores from a verified backup; corrupted backup is rejected; crash/restart retains identity, status and history; no timer manufactures a success state.

### 4. Make Dreamer and self-improvement real workflows

Represent Builder, Critic and other roles as persisted agent specifications and recipes, not eight weight copies. Generate candidate artifacts, run task-specific validators, compare utility and novelty against the available corpus, and promote through the existing shadow mutation path. Compile success alone does not establish intended behavior.

Use host receipts to produce learning examples. Store successful construction/repair patterns and failures with task, artifact, model, tool and validator identities. A failed model request remains a failure. Refresh project memory after validated changes. Expand the 45-item catalog with per-item states: documented, implemented, tested, provider available, or unavailable offline.

Acceptance: one real creation task produces a saved entity/artifact, behavior checks, recoverable prior version and a reusable lesson that affects a later task. Revenue or publishing claims require actual external records; local counters are not those records.

### 5. Keep cloud training optional and separate from the offline engine

First obtain fresh hardware/service identity and the actual provider's billing state; the pasted balance and default B200 labels are not live account evidence. Prepare a receipt-backed dataset, deduplicate by task family, split training/validation/held-out tasks, and validate tool-call serialization with the exact tokenizer/chat template. Measure the base model before any training.

A real pilot then needs pinned base revision, supported training representation, actual LoRA tensors/optimizer steps, measured peak VRAM/time and a saved adapter. Quantized inference fit does not guarantee training fit or batch size 16. Benchmark a short pilot instead of promising 18-minute completion. Keep model weights/adapters outside ordinary Git source history and verify the exact merge/export/import route before attempting Ollama deployment.

Only consider GRPO after the ordinary supervised pilot works: executable task-specific rewards, hidden checks, anti-reward-hacking controls, multiple seeds and held-out comparisons. Distillation requires actual teacher-generated results verified by the host; five repeated canned examples are not 5,000 traces. Promote only measured improvements; retain the baseline and an explicit rollback path.

Training may use a dedicated optional PyTorch/PEFT/TRL environment. Studio's normal execution stays Java-first and does not gain those dependencies. Remote jobs, model/data export, publication and Git push are explicit operations rather than imported prompt instructions.

## Forecast and remaining unknowns

The immediate useful increment is the shared **typed model/tool loop**: it makes the already installed models act on real component and memory data inside the existing window. Artifact recovery follows it; an optional training pipeline comes after reliable task evidence exists. Adding more personality text first will not close these gaps.

No schedule, superiority, AGI or consciousness conclusion follows from these tests. The actual cloud GPU identity, current balance, remote service, any trained adapter not present in the audited files, missing Gemini attachment, full browser integration and new 120B inference remain unverified. The running Studio and original F: packages were preserved; none of the supplied cloud execution endpoints was contacted, and no cloud job, automatic Git push or daemon was started.
