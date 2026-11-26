# Edition 264 implementation and reproduction

The previous tabs exposed the app’s implementation boundaries. The default workspace now starts with the conversation and one active stage. Project, Browser, Tools and Activity are destinations that preserve the conversation. The docked composer and compact Stage/Conversation controls work with the same Electron window. The manual layout remains available from Interface options.

## Routing and authority

The workspace action registry gives explicit navigation requests a consistent meaning across starting views. Chat can also bring verified task results into the appropriate existing pane and keep the request while a required input is being selected. Floor-plan, desk, motion and ramp work use their established execution and verification paths. The embedded browser reads bounded page text for the explicit inspection request. Tool search displays the current Java registry. Searching it runs no tool. Open-ended planning still uses the app’s existing model and Java routing.

The stage updates the actual Electron surfaces after a change in layout. Opening the launcher temporarily hides the native surface so the launcher is visible and clickable. Closing it restores focus. The ordinary app page keeps the same local URL used by webcam permission checks. Draft guards apply before navigation or task-driven changes.

## Acceptance

- The real Electron workspace with zero model stores read a local browser page and found a registered Java tool by keyboard.
- A floor-plan request started from Browser and resumed after choosing the sample model. The resulting diagram appeared beside a single copy of the request.
- The editor kept its unsaved changes when a navigation action was attempted.
- A desktop restart restored the transcript. A request from Project reopened the retained model and displayed three measured height bands.
- At 1100 and 790 pixels, native view visibility and bounds followed the active workspace. The compact composer remained visible. The ramp request at the smaller size continued after its input was saved.
- The motion integration chose between two recorded simulations, verified the generated outputs and reused a recipe after restart.
- The portable workflow runs the workspace acceptance case in the Windows, macOS and Linux matrix.

## Reproduce

From the repository root, install the engine with `./gradlew :workspace-engine:installDist` on macOS/Linux or `gradlew.bat :workspace-engine:installDist` on Windows. Install the desktop dependencies with `npm --prefix workspace-desktop ci`.

Run `npm --prefix workspace-desktop test`, then `node workspace-desktop/test/workspace-shell-integration.cjs build/workspace-shell-check` and `node workspace-desktop/test/motion-media-integration.cjs build/motion-workspace-check`. On Linux without a display, run the desktop checks with `xvfb-run -a`.

[Edition 263](../../README-263.md) · [Screenshots and sample files](../readmes/264/index.html)
