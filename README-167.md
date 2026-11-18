# Fraynix Agent Workspace — edition 167

**2026-11-18 · Original playable content, preserved engine checks, retrievable history.**

[Previous edition](README-166.md) · [Edition history](docs/readmes/INDEX.md) · [Offline guide](docs/readmes/167/index.html) · [Architecture renewal](docs/ARCHITECTURE_RENEWAL_PLAN.md)

The application remains one integrated local agent workspace: observations, a persistent world, durable tasks, browser/canvas tools and evidence-backed results. This edition adds **Relay Garden**, an original playable content pack for the existing Java Game Lab, and verifies the next historical archive batch. It does not introduce another numbered runtime subsystem.

## Play Relay Garden

Open a saved workspace, choose **Game**, then **Play Relay Garden**. The robot starts on a modular garden deck. Cross two gaps and reach the lit beacon. The movement buttons and Jump send bounded inputs to the same player and collision solver used by the existing engine. AI play uses the unchanged default controller. Pause, Reset and Close runtime remain available. Reset restores the checked starting level; it does not overwrite the user's authored scene.

The pack is explicit. The existing default original-level button, historical construction procedures and saved projects retain their previous behavior. All rendering stays inside the application through the hidden native renderer; no separate editor window is required.

![Actual app — original robot and garden](docs/readmes/167/relay-garden-start.png)

![Actual app — existing controller reaches the beacon](docs/readmes/167/relay-garden-finish.png)

![Actual app — compact layout](docs/readmes/167/relay-garden-compact.png)

These are actual Java/Electron application captures from 2026-11-18. The scene is generated game content, not camera evidence. No model inference, physical-camera observation or improved real-world accuracy is claimed by this release.

## What is original, and what is reused

`RelayGardenGenerator` draws the robot, animated poses, deck tiles, planter, beacon and sky with Java graphics operations. It synthesizes mono PCM sound cues and constructs a new serialized level from explicit component values and stable identities. It reads no existing image, sound recording or level. The repository contains both the generator and its reproducible outputs, with a manifest identifying every content byte.

The existing renderer, player, animation system, collision solver and bounded input interface are reused. A named `FallBoundary` component gives this level a real loss condition below the deck. Existing levels do not acquire that component automatically. Sound names such as `mario_die` remain compatibility keys used by inherited code; the corresponding WAV is a newly synthesized tone, not a copied recording.

The integrated runtime normally disables audio. Tests decode the original non-silent WAV data; screenshots do not establish speaker playback. The new PCM loader supports those bounded mono cues when the existing audio device is enabled.

Original content provenance does not relicense inherited code or clear all historical assets. The older Mario-style content remains available for compatibility and historical verification. Full replacement of that content and a project-wide distribution license remain separate shipping work.

## Verification and actual outcomes

122 root-engine Java tests, 18 selected workspace Java tests, 181 desktop tests and 10 archive tests passed, without failures or skips. This is the full root-engine suite plus scoped integrated-game/server regressions, not a rerun of every historical workspace capability.

The final actual-app run reached the finish in 408 simulation ticks through bounded steps of the unchanged contact122 controller. A separate run using the visible AI play button had confirmed finish contact at tick 400. Manual input moved the robot, reset restored its initial position, and a full app restart retained its original identity. The native no-jump trial failed as intended below the deck.

Both source and installed pack bytes are checked before launch and reset. Same-size asset corruption with a restored timestamp and a changed source level are rejected. A request cannot supply an arbitrary replacement path. Actual runtime observations list the original content resources used; the new course loads no legacy image or sound asset.

The native test records both success and failure: gap-aware inputs reach actual finish contact; a no-jump run falls into the gap and loses. The real app test checks manual movement, the existing controller, visible pixels, reset, a complete app restart and retained starting identity. These checks demonstrate this bounded course, not universal game generation or general planning.

The compiled identities of 60 existing agent/controller, physics, player, camera and state-machine classes match the preserved edition 166 candidate exactly. New content and the explicit fall-boundary component are separate additions.

## CI diagnosis and development attempts

The initial CI check found both edition 166 publication runs failed before Java execution. Their archive test rejected the Windows runner's linked temporary parent. Restore now resolves and pins the physical parent once and performs all writes through that path. It still requires a new destination, rejects destination links/overwrites and traversal, checks original byte and canonical hashes, and selects exactly one provider. An actual Windows junction regression passes.

The first new junction test had an incorrect PowerShell argument invocation; it was fixed using separate environment values. The first native course trial reached the beacon, but its failure check exposed that the inherited player kept falling without entering its death state. The new level explicitly includes `FallBoundary`. The next launch correctly rejected that type until it was registered in the existing component allowlist. The corrected real runtime passes. Logs and earlier results remain in the evidence archive; none are represented as successful trials.

At the prepublication check, the latest hosted run remained the failed edition 166 run [44271487309](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/44271487309). Local archive checks now pass with the junction regression. The new publication run is reported separately; local success is not labelled hosted-CI success.

The first complete app run also passed, but screenshot inspection exposed the end of the decorative background near the finish. The authored backdrop was extended, the full root-engine suite and the two content-specific native checks repeated, and a new actual-app run captured. The explicit course target now sits beyond its beacon so the visible AI-play action traverses the contact sensor. The final app run checks that button as well as bounded controller steps. The earlier successful run and source lock are retained. Lock preparation also caught binary test fixtures and an incorrect historical JAR filename; both tooling errors were corrected before the final trial.

## Next verified archive batch

Edition 163 contributes **76 original files**, totaling **37,716,596 bytes**, packed into **8,909,823 bytes**. Every downloaded original was compared to its retained Git blob.

The batch is bound to the separately pinned edition 166 historical catalog and original Git commit. Every selected file is restored from Git, packed, restored through the archive provider, and compared byte for byte. The published evidence asset is then downloaded and the nested historical batch restored again against the original Git blobs. Corrupt and missing providers refuse without silently falling back to Git. A checked second copy is retained on another local volume; that is not an off-site disaster-recovery guarantee.

No frozen history was removed from Git, and no remote history was rewritten. This remains a reversible migration. Hashes and catalogs identify missing evidence; they cannot recreate its bytes. A complete cutover still requires all retained evidence to have durable, tested providers.

[Download manifest](docs/readmes/167/original-evidence.json) · [Archive results](docs/readmes/167/archive.json) · [Validation](docs/readmes/167/verification.json) · [Exact source inventory](docs/readmes/167/file-inventory.json)

```powershell
python scripts/evidence_archive.py download --manifest docs/readmes/167/original-evidence.json --archive evidence167-local.zip
```

Choose a new download path. The download verifies every original member. The enclosed `legacy163.zip`, catalog and manifest provide the historical batch; `scripts/history_archive.py --help` explains restoring selected paths into a new destination. The downloaded batch receipt records comparison to the original Git objects.

## Architecture

```mermaid
flowchart LR
  Select[Explicit Game Lab choice] --> Check[Source and installed pack verification]
  Check --> Runtime[Existing Java renderer and physics]
  Runtime --> Input[Bounded human or controller inputs]
  Input --> Outcome[Actual position, contact, loss or finish]
  Archive[Original Git evidence] --> Pack[Hashed archive and manifest]
  Pack --> Download[Downloaded and restored originals]
```

The content profile is a named capability. The server retains shared authentication, admission and active-task ownership checks. Legacy behavior remains default. The archive fix changes path resolution, not authority or evidence acceptance.

## Build and inspect

Use JDK 21 and the repository's Gradle wrapper. Build with `gradlew.bat prepareGameRuntime115 :workspace-engine:installDist`, install locked desktop dependencies with `npm ci` in `workspace-desktop`, then run `npm start` there. Use a separate test project for experiments. The long-history checkout still requires Windows long-path support.

Regenerate the original pack into a new directory using `gradlew.bat generateRelayGarden -PrelayOutput=build/my-original-pack`. Generation refuses an existing destination. The reproducibility test compares every generated file with the checked-in pack. A constructed level is not accepted as playable merely because its JSON parses; the retained native and actual-app tests establish this course's outcomes.

## Next complete slices

1. Extend the original pack to pickups, hazards and varied geometry, with measured playtests and explicit compatibility for existing procedures.
2. Add more historical archive batches and independent retrieval checks before any separate clean-repository cutover.
3. Continue named-domain extraction and retire superseded selection paths behind tested replay compatibility.
4. Improve the everyday Chat, Mission timeline and Evidence layout, then test a genuinely fresh installation.

The destination remains an integrated local agent that observes, acts and checks consequences in its persistent world. Original content makes that world more independently maintainable. This edition does not claim full asset replacement, a new trained model, improved tracking, a complete architecture rewrite or a signed installer.
