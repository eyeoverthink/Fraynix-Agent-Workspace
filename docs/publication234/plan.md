# Existing-level placement and offline play

2026-11-24 · Edition 234

## Review

Edition 233 independently replayed a dedicated contact deck. The successor uses the same native contact protocol in an existing authored Spatial scene. The predecessor’s portable CI passed Windows x64, Linux x64, macOS arm64 and macOS x64. [CI results](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45517862538)

## Exact edit and recovery

The registered route opens a form for the selected held-out-verified prop. The user chooses a separate metric destination, a static horizontal floor, a position and yaw, and an axis-aligned start-to-exit aisle. The preview displays a conservative rotated box footprint and the full player envelope. The saved review hashes the original prop and destination revisions. Approval binds that review.

Java checks floor support, existing collider overlap and the entire route before approving. On execution, Java repeats the geometry checks and asks Bullet to check the player envelope against the authored collision bodies. A moving sphere must hit the copied prop and give it measurable planar motion. A laterally offset pass must miss. The hit approach and the miss must stay free of other collider contacts during their checked interval. Two isolated native runs must agree.

A successful run creates a deterministic new scene ID and writes the revision and evidence together. An interrupted run can restart from the approved descriptor. Completed retries return the same revision, and a competing review of an already superseded destination must be reviewed again. Reads and export recheck the original retained scene, reference provenance, approval and saved evidence.

## Portability and limits

The playable export is a single self-contained HTML file. Movement uses the top-down projection of conservative collision boxes on the authored floor, with keyboard controls and a reachable exit. The native contact trial and playable traversal check different behaviors. The app’s physics evidence applies to the authored geometry and the declared launch conditions. Concurrent movement of other bodies during play, walkable slopes, freeform aisles and full Game Lab course generation are outside this edition’s supported geometry.

The companion evidence export retains scene and GLB bytes, the approved edit, both measured traces and digests. The imported editable scene does not create an approval in a recipient project. The original visual inputs live in the authoring project.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
node workspace-desktop/test/spatial-level-integration.cjs build/spatial-level234
```

On macOS use the shell wrapper. On Linux use `xvfb-run -a` for the desktop test if there is no display.

[Read edition 234](../../README-234.md)
