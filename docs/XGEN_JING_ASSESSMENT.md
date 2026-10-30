# XGEN-JING assessment and integration path

Reviewed October 30, 2026, against upstream commit **41c41ae9a1dbe5213e8733bbebeaebe297afa7a7** and the user's downloaded ZIP. This is a targeted source/interface/weight-metadata review. No JING inference, training, model installation or weight download was performed.

## What the local download contains

The ZIP is 9,597,188 bytes, contains 66 archive entries and passes its CRC integrity check. SHA-256: **5217b146c5ab801bbfeebd58ecc9e37e936754b6eb57c43a1b8c3bdab2b2ed78**. Seven critical files match the pinned upstream checkout: README, base configuration, control module, inference pipeline, entry point, license and notice. An inventory identifies 44 text source/configuration/documentation files with 7,294 lines; that count is an inventory, not a claim that every line has been audited.

There are no bundled safetensors, GGUF, PyTorch checkpoint or binary weight files. The archive contains code, examples and assets. The application source and original Studio are unchanged by this review. The reference checkout stays under ignored build data.

## What is available versus described

The public demo offers bidirectional four-step audiovisual inference, typed cases and reference/control inputs. Its current source constructs complete cases, denoises them and saves MP4 plus settings. Four steps means four diffusion evaluations, not four frames or demonstrated four-step real-time interaction on this machine. The released attention implementation uses noncausal media attention.

The report describes a larger architecture involving DAO world state, observer-specific conditions, causal generation and audiovisual memory. Those descriptions are useful design evidence. They do not establish that DAO, causal inference or the complete training system is included in this downloaded demo. The README lists the causal release as forthcoming. See the [upstream release](https://github.com/XGEN-Labs/XGEN-JING/tree/41c41ae9a1dbe5213e8733bbebeaebe297afa7a7) and [technical report](https://arxiv.org/html/2610.19726v1).

## Useful concepts and their destination

| Concept | Meaning for this application | Current status |
| --- | --- | --- |
| Shared state with observer-specific views | Preserve authoritative Java entities/physics; derive each observer's view from a versioned state | Java state exists; a general observer contract is next |
| Action-conditioned experience | Record camera/action intent with time and entity references; validate actual effects through installed handlers | Current browser/native tools exist; unified observer actions are planned |
| Ordered entity references | Bind each picture to a stable entity, content hash and revision instead of relying on an anonymous image slot | Proposed reference packet |
| Bounded observation history | Retain an initial anchor, recent observations and selected older evidence with explicit budgets | Existing episodes exist; audiovisual observer memory is planned |
| Generated visual/audio layer | Consume a frozen observation packet and produce a labeled media artifact | No JING renderer installed or available |
| Evidence-driven improvement | Evaluate models/controllers on actual outcomes and separate validation | Native calibration and bounded controller evaluation already work |

This is a strong architectural fit because a visual model can be replaceable while the host preserves identities, facts and actions. That is our engineering interpretation, not a claim of equivalent capability or unique prior invention.

## Controls and weight compatibility

The demo maps WASD/IJKL into six camera-intent channels. Its current configuration enables a six-input, 512-wide control module and a 50-layer transformer with hidden width 5376. The index references fifteen safetensor shards totaling **66,885,622,272 tensor bytes**, including 408 named control tensors. Only the small configuration/index metadata was fetched. [Weight configuration](https://huggingface.co/XGENlabs/XGEN-JING/blob/main/jing_flash_v1/config.json), [weight index](https://huggingface.co/XGENlabs/XGEN-JING/blob/main/jing_flash_v1/diffusion_pytorch_model.safetensors.index.json).

Camera keys are conditioning signals; they do not establish an exact displacement in metres or an exact physical camera rotation. A bridge must define the native action rate, duration and coordinate transformation independently, then report the actual resulting pose.

The weights are tied to their diffusion backbone, control layers, latent codecs and text-feature interface. They cannot simply be appended to an Ollama chat model, loaded as Gson/JSON state, or converted into smaller architecture-compatible weights by renaming a format. The host LLM could prepare validated requests, while a compatible renderer executes them separately.

MiniMax's documented base system uses a Qwen3-VL-32B feature encoder and separate audio/video VAEs. Swapping in our current chat model would require a demonstrated compatible embedding adapter or retraining, not a configuration substitution. [Base-model architecture](https://huggingface.co/MiniMaxAI/MiniMax-H3/blob/main/README.md).

## Local feasibility and license facts

Detected hardware: one NVIDIA GeForce GTX 1660 Ti, 6,144 MiB VRAM, driver 560.94. The published demo was validated on six H100 GPUs. Its launcher/runtime uses sharded CUDA ranks and separate encoder/VAE devices; a supported one-GPU/CPU execution path is not supplied by this demo. These observations make its published stack an impractical local target on the detected machine. No out-of-memory trial or multi-gigabyte weight download was attempted.

The repository's NOTICE places JING modifications and the resulting H3 derivative under the MiniMax H3 Community License. Selected upstream portions have Apache-2.0 notices; that does not make the whole release Apache-2.0. The community license's territory definition lists the US, EU, UK and South Korea as excluded, and it contains restrictions on improving other models with H3 works/outputs. Those are material source terms to resolve before deciding on weight deployment or distillation. This assessment reports the published text; it does not determine the user's legal eligibility. [Pinned license](https://github.com/XGEN-Labs/XGEN-JING/blob/41c41ae9a1dbe5213e8733bbebeaebe297afa7a7/LICENSE), [notice](https://github.com/XGEN-Labs/XGEN-JING/blob/41c41ae9a1dbe5213e8733bbebeaebe297afa7a7/NOTICE).

No inference environment, paid cloud instance, provider account or redistributed model package was created.

## Concrete implementation sequence

1. **Native observer first.** Add a functioning first-person observer to the existing 3D lab. Persist bounded pose and field of view; capture the actual rendered frame with scene/run/tick identifiers. Manual keys and agent actions should use the same typed interface. Acceptance: moving the observer changes the actual view, entity queries match the native scene, restart/replay works, and stale observations cannot authorize edits. Free-camera movement and a physical avatar need distinct semantics.
2. **Condition/reference packet.** Produce an immutable packet containing observer ID/pose, source revision/tick, visible entities, action history and ordered reference hashes. Enforce bounded context and preserve source classification: measured, native simulation, generated or inferred. Acceptance: the same recorded packet reproduces its native observation and cannot acquire executable authority from prompt text.
3. **Memory tied to outcomes.** Link anchor/recent/retrieved observations to entity/event history. Acceptance: departure/revisit reflects committed changes, supports rollback and rejects unsupported visual claims. A changing generated image alone is not a world-state update.
4. **An executable media backend.** Select a renderer whose hardware and deployment terms are suitable. Only add its UI/tool action after an actual executor can run a complete case, return a real clip and record model/version/seed/input/output hashes and cancellation. JING can be a reference target if its deployment requirements are resolved; compatible alternatives can use the same host packet.
5. **Separate training gates.** Build meaningful aligned datasets and withheld evaluation families before training policies, perception adapters or weights. Define the target, baseline, independent metric, checkpoint/export/inference compatibility and rollback. Dataset/skill preparation is not itself model training.

~~~mermaid
flowchart LR
    Agent[User or local agent] --> Action[Typed native action]
    Action --> World[Java entities physics and receipts]
    World --> Observer[Observer state and real render]
    Observer --> Memory[Bounded source-linked history]
    World -. proposed .-> Packet[Frozen conditioning packet]
    Memory -. proposed .-> Packet
    Packet -. proposed .-> Generator[Executable eligible media backend]
    Generator -. proposed .-> Media[Labeled generated clip and receipt]
~~~

The native observer, general packet and media renderer are planned additions. No empty application page or inactive generator action has been added to represent them. Current work delivers the source review plus a reproducible real-observation acceptance case. See [README 013](../README-013.md), [real-data methods](datasets/delfys75-013/README.md) and [working roadmap](SCENARIO_ENGINE_ROADMAP.md).
