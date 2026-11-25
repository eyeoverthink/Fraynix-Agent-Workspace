# Edition 244 implementation and reproduction

The candidate and parent must differ only in the reviewed box. The same five approach definitions from edition 243 run twice per variant and collider when a verified result is available. A failed approach stores its reason and has no timing result. The pending report seals the exact review, parent, candidate, probe launches and measured data. Reopening verifies its digest and recomputes the timing summary. If approval is sent with a displayed comparison digest, Java verifies the current exact evidence first. The approved edit still passes the original native commit checks.

```powershell
.\gradlew.bat :workspace-engine:test --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/recipient-preview-sweep-integration.cjs build/preview244 docs/readmes/240/signal-bay-handoff.fraynix-recipient.json
```

Use `./gradlew` on macOS and Linux, and use `xvfb-run -a` if Linux lacks a display. The portable workflow exercises the same request against a newly generated Signal Bay archive. The local test uses the exact published bundle and checks native repeat, receipt recovery, stale or altered evidence and export. [Install guide](../INSTALL.md) · [Independent review](review.md)
