# Browser selection, reconstruction, placement, and Blender verification

2026-11-24 · Edition 235

## Prior release

Edition 234's tagged portable run passed Windows x64, Linux x64, Apple Silicon and Intel Mac. The same commit on main passed Windows, Linux, and Apple Silicon. The Intel job reached the placement test and was cancelled. The workflow now allows 22 minutes per platform for the added browser test and hosted runner variation. [Tagged run](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45530232959) · [Main run](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45530233729)

## Source handoff

The normal browser pick still consumes the user's trusted click and captures rendered pixels. The registered fitter selects a candidate using front and side plus an optional top. It compares the selected model to the held-out view afterward. A failed held-out comparison creates no scene or browser placement handoff.

The placement entry point saves the checked reconstruction, then retains the exact scene, object, mesh, source package and reconstruction digests before opening the review. A resumed handoff reads the original retained source. Page access is unnecessary after this checkpoint. Browser picks are transient until a fit succeeds. The existing placement review saves destination, pose, aisle and separate approval. Its portable reference contains crop dimensions and digests. Original pixel bytes and page addresses are retained in the project.

## Verification

The Windows acceptance ran with fresh Blender 5.3 registered source renders served to the actual in-app browser. The wrong view failed. The correct rendering was verified, the page source was stopped, and three desktop restarts covered the handoff, saved review, and committed native result. A rechecked GLB export matched the bundle byte for byte. The case checked an edit preview, refusal on a changed handoff or source bytes, unchanged original objects, and one revision after retry.

The Blender oracle imports the exact saved GLB, verifies object count, identity, placement, transforms and collider metadata against the independently authored inputs, and saves and reopens a native Blender file. It separately renders the imported mesh and original solid at a fourth angle. The reported overlap measures their silhouettes. The authored-level fit and Blender check are bounded to registered images and the existing static GLB scene contract. Native contact and clearance remain verified in Fraynix.

## Reproduce on Windows

```powershell
.\gradlew.bat :workspace-engine:installDist
$env:FRAYNIX_TEST_BLENDER = 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe'
node workspace-desktop/test/browser-views-integration.cjs build/browser-placement235 placement
```

Use your Blender executable path. On macOS and Linux, use the shell Gradle wrapper and set the same environment variable for Node. The hosted portable acceptance uses the checked-in registered fixtures and runs browser, Java, and GLB checks. The local command with Blender enabled also generates fresh views, opens the exact GLB, saves and reopens the Blender scene, and verifies both rendered silhouettes.

[Edition 235](../../README-235.md) · [Native placement contract](../publication234/plan.md)
