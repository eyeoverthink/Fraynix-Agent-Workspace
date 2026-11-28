# Edition 274 implementation and verification

## What runs

Java reads the selected dynamic crate and declared-scale Spatial scene. The upright loading door provides the horizontal normal, and the aisle marker establishes which way is into the bay. A 37° bay produces **+0.240726 m X, 0 m Y and +0.319454 m Z**. The square control produces **+0.40 m Z**. The source stores the move in the scene’s units.

Use **40 cm away from the loading door** for the relative move. **40 cm from the loading door** could also describe a final distance and needs clarification. Java refuses additional scene edits in the same request.

```mermaid
flowchart LR
    A["Chat and selected crate"] --> B["Bind the parent and proposal"]
    B --> C["Preview and exact approval"]
    C --> D["Native sweep, aisle and shelf checks"]
    D --> E["Repeat and retain the verified revision"]
    E --> F["Reopen, replay or export"]
```

## Verification

- Compare the full source after reversing the approved crate position and revision title. The parent and every other object retain their source properties.
- Sweep the oriented crate collider. The sweep rises 5 mm to avoid counting floor support.
- Native Bullet checks the **1.00 × 1.80 × 3.60 m** oriented prism before the move, at the new position and after settling. The prism starts 2 cm above the floor and uses a 1 mm overlap tolerance. Each repeat checks 27 body pairs across the three states.
- Check a 60-tick supported, low-speed hold at the end of a 180-step run. Two fresh runs must agree before Java saves a child scene.
- Bind the receipt to the approval, scene identity and candidate source hash. The editable scene and receipt share one JSON export.

The authored move uses a collision sweep and a separate settling run. The measurements apply to the declared geometry of the test bay.

## Reproduce

Run `./gradlew :workspace-engine:installDist` or `gradlew.bat :workspace-engine:installDist`, then `npm --prefix workspace-desktop ci`. Run `node workspace-desktop/test/loading-aisle-integration.cjs build/loading-aisle` from the root. Use `xvfb-run -a` on Linux without a display. Set `FRAYNIX_AISLE_NO_WEBGL=1` to exercise the top-down preview.

Windows verified both restarts, exact export bytes and repeat approval. Separate trolley controls rejected an obstructed aisle and an obstructed move path without creating a scene. A changed proof refused replay. [Windows test report](../readmes/274/result.json) · [2D mode report](../readmes/274/result-2d.json)

Edition 273’s dedicated Courier step succeeded on four hosted targets. Later spatial steps failed on Windows, Linux and Apple Silicon, and the Intel Mac job was cancelled. Edition 274 adds a portable regression for the aisle review. [Prior portable CI](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/46097223997)

## Next design test

Use held-out bays with unfamiliar door orientations, multiple crate candidates and varied aisle shapes. Measure when clarification or refusal is correct and evaluate reviewed alternatives with the same shelf and aisle checks.
