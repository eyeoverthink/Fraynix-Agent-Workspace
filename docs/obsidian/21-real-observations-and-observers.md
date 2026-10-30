# Real observations and observer direction

Edition 013, October 30, 2026. Keep the app in Fraynix-Agent-Workspace; preserve the original Studio, present layout and private data.

Two actual Delfys75 recordings yield 52 training and 49 validation observations. Training-only image-plane scale and the 0..1 friction / 1 s / 0.015 m criteria are frozen. Seventeen candidates execute native Bullet. Selected friction 0.15625; training RMSE 0.009632 m, validation 0.059435 m, max validation residual 0.159463 m. The fit is rejected and no scene is changed. Workflow/provenance/arithmetic checks pass; that does not make the physical fit pass.

The local JING ZIP passes CRC and key-file comparison; it contains no weights. Targeted source/metadata review identifies a bidirectional demo, a 66.9 GB transformer and further encoder/codec requirements. Current GPU is a 6 GB GTX 1660 Ti. License terms are material. No JING inference or weight training is implemented. Next: real native first-person observer, state/tick-linked action/reference packets and bounded observation memory; add a media backend only with a working executor.

- [README 013](../../README-013.md)
- [Methods and provenance](../datasets/delfys75-013/README.md)
- [Verification](../readmes/013/verification.json)
- [Actual offline report](../readmes/013/report/index.html)
- [JING assessment](../XGEN_JING_ASSESSMENT.md)
- [Map](real-observations-and-observers.canvas)
- [Memory](../PROJECT_MEMORY.md)
- [Roadmap](../SCENARIO_ENGINE_ROADMAP.md)
