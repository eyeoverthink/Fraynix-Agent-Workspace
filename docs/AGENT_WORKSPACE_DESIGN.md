# Fraynix Agent Workspace design and delivery plan

This successor starts from Fraynix Studio commit `6341404`. The working Studio repository and its personal project state remain separate. The goal is a visible local environment in which agents can inspect real state, operate supported tools, create runnable artifacts, test their behavior, and reuse verified work.

## Product surfaces

The left column contains persistent project objects and a real browser. The center is a runnable creation canvas with a source view. The right column holds the local model selector, request, results and takeover control. The bottom timeline shows decisions, tool calls, failures and measured outcomes. Browser and canvas can each expand while keeping the agent visible.

The original Java game editor remains a separate Game Lab window, launched from this repository with a distinct project directory. Embedding its native viewport in the new shell is a later integration milestone. The existing Studio application is not replaced.

## Implementation boundaries

Java remains the model and data control plane. The new `workspace-engine` module reuses FactoryWorld, schemas, staged transactions, checkpoint storage, FactoryModelService, local Ollama routing, the offline kernel and its memory/journal interfaces. The desktop shell uses Electron to host two actual Chromium WebContentsView surfaces. This separates modern browser hosting from the legacy OpenGL game window.

The shell is trusted application UI. Remote browser pages and generated canvas applications have no Node integration or privileged preload. IPC accepts only the main shell frame. The Java control API binds loopback on an ephemeral port and requires a private session bearer held by the main process. Generated HTML previews cannot make network requests or call the control API. Their renderer sandbox is a boundary for web code; this version does not execute arbitrary generated Python, Java or native programs.

Source code and browser text are data. A model cannot register an executable handler, grant privileges or establish that a test passed by printing text. Structured model responses choose existing tools. Those tools own execution and return observations and receipts.

## Real browser observations and actions

The browser uses the same visible page for human interaction and agent actions. Navigation loads HTTP or HTTPS content. Observations include URL, title, bounded visible text, and supported interactive controls. Controls receive short-lived references tied to an observation. Actions reject stale observation IDs, scroll the target into view, and send actual input before observing again. Password fields require manual interaction.

This first adapter covers the main document's links, buttons and text inputs. General iframe traversal, complex shadow-DOM controls, downloads, multi-tab workflows and synchronized vision-model screenshots require additional adapters and tests. The surface is a real browser, but these automation limits remain explicit.

## Runnable artifacts and component state

An artifact is a FactoryWorld entity with a versioned `workspace.artifact` component. Its metadata contains title, revision, file inventory, source references and creation time. Files are stored under an immutable version directory. Updating an existing artifact requires its current version. Older sources remain available. An interrupted uncommitted version directory is preserved under recovery before another write proceeds.

Supported files are HTML, CSS, JavaScript, JSON, SVG, text, Markdown and CSV. A runnable artifact needs `index.html`. The preview serves those files from the Java host and executes their browser code in an isolated Chromium renderer. User source edits create a new version and invalidate the previous passing test badge.

The general object abstraction preserves domain meaning. A browser tab has a URL and controls; a document has a storage reference and structure; a game actor has a transform and physics. Visual placement on a canvas does not give every data object physical gravity. The inherited game remains a 2D physics environment; its zIndex describes draw order.

## Agent execution and progress

The agent runs a bounded local-model loop outside the render thread. The first milestone allows 16 steps, with model requests using the existing cancellable streaming transport and a JSON schema for permitted tool arguments. Stage guards advance repeated read-only observations toward creation, require tests for a new artifact, require a source change after a failed test and read, and require verified evidence before reporting completion.

The model still chooses page actions, generates artifact contents, specifies behavioral assertions, repairs code and describes its result. Stage guards constrain progress rather than establish that the generated work is correct. The separate integration harness evaluates real behavior independently of model prose.

Take over cancels the active model socket locally and prevents further agent actions. It does not kill a shared Ollama service. Manual browser/canvas actions and agent actions are serialized to prevent one from invalidating the other's observation mid-command.

Renderer operations and navigation also have deadlines. A hung artifact renderer is stopped while the host stays available; editing its source and reopening a corrected version restores a usable canvas. The integration harness includes an actual infinite-loop fixture and recovery.

## Tests and reusable skills

`canvas.test` reloads the current saved artifact, performs supported clicks/fills, and checks observable text, values or element counts. It returns individual assertion results and JavaScript errors. The Java engine records a receipt against the exact artifact version. Test coverage is limited to the assertions supplied; broader task correctness still needs appropriate independent evaluation.

A passing current artifact can become a reusable web-artifact recipe. The saved recipe contains versioned files, source references and actual test steps. Reusing it creates a new artifact and reruns the tests. The kernel indexes the recipe for memory retrieval. Editing a recipe's source requires fresh evidence before another verified version is saved.

These recipes are the first skill type. New compiled executors, operating-system adapters and general code workers require real implementations, declared capabilities and additional evaluation. A prompt naming a skill does not create those capabilities.

## Delivery milestones

| Milestone | Acceptance gate |
| --- | --- |
| Independent successor | Separate repository, launcher, runtime state and browser profile; Studio baseline preserved |
| Browser and canvas | Real external navigation, page interaction, runnable local artifact and visible source |
| Verify and repair | Catch incorrect behavior, write a corrected revision and pass actual assertions |
| Persistent skill reuse | Save only a current passing artifact, restart, recreate the recipe and pass again |
| Local model workflow | A real local model observes, creates, tests and saves through the same tool boundary |
| Broader artifact kinds | Dedicated renderers and validators for documents, diagrams, datasets and additional programs |
| Embodied perception | Tick-aligned engine observations and vision frames linked to object identity |
| Evaluated improvement | Replayable episodes, held-out task suites, candidate promotion and rollback |

The first delivery targets the first five gates. The remaining gates extend the same environment without claiming that every installed model or arbitrary website is already reliable. Initial operation needs Windows, JDK 21, Node.js 22.12 or newer, installed local Ollama models and the pinned Electron package. Docker and WSL are not prerequisites.
