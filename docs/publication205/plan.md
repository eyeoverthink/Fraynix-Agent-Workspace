
# Verify a portable workspace install

1. Download a sparse copy from GitHub following the [installation guide](../INSTALL.md). Confirm that the application code, runtime assets and recorded motion dataset are available.
2. Prepare Java and Electron on the target host. Builds resolve the LWJGL native variant for that host and architecture. The separate Game Lab process uses Cocoa-compatible OpenGL settings on macOS.
3. Run the desktop proof in an isolated project. The smoke test exercises Java startup, the browser and creation canvas, a native Bullet ramp check, and the hidden Java renderer with actual game movement.
4. Reopen the same project as a continuing Java session and verify that it can load the saved artifact and stop cleanly.

Run `node workspace-desktop/test/portable-smoke.cjs build/portable-smoke` after preparing on each platform. On a Linux CI runner, the workflow supplies a virtual display and Mesa software OpenGL. The [GitHub Actions workflow](../../.github/workflows/portable-workspace.yml) retains each job's screenshots, check receipt and logs. On the hosted Mac jobs only, the test can continue after an explicitly detected OpenGL display refusal. Its receipt records that the renderer was unavailable, and the harness checks that the game process stopped before verifying restart. Running the same command on a physical Mac requires the game to render and move.

## Next candidate

Package the workspace for download as platform-specific installers and keep testing a clean install on each platform. Add signing and notarization when a distribution identity is available.
