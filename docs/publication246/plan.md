# Edition 246 implementation and reproduction

Use the existing local Ollama catalog and model admission to obtain one native intent choice. Java checks the allowed tool and arguments. It persists the checked question and exact preview identity across restart. A changed target requires new confirmation. Java starts the five-approach mission against the current saved review, retains the model receipt with the request, and uses the existing approval, simulation and export checks.

The desktop acceptance test uses the real **gpt-oss:20b** weight digest, verifies the loopback transport and records the native model calls in the routing receipt. It runs on an isolated project. It first tests plain wording, then ambiguity, restart and an explicit clarification. It checks the local world before and after human approval, and closes and opens the desktop again to verify the complete mission. The Java tests cover malformed choices, a missing proposal, and replies for a replaced proposal. They are also in the portable GitHub job and do not require model weights.

```powershell
.\gradlew.bat :workspace-engine:test --tests '*ColliderRoutingTest' --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/recipient-model-routing-integration.cjs build/recipient-model-routing246
node workspace-desktop/test/recipient-mission-integration.cjs build/recipient-mission-regression246 docs/readmes/245/raised-switchyard.fraynix-level.json
node workspace-desktop/test/placement-exchange-integration.cjs build/recipient-model-replay246 build/recipient-model-routing246/raised-switchyard-model-reviewed.fraynix-recipient.json
```

Run with Ollama already available. The real-model test defaults to `gpt-oss:20b` on CPU. `FRAYNIX_TEST_MODEL` can select another installed model for qualification. On macOS and Linux use `./gradlew`. With no graphical display on Linux, prefix the desktop commands with `xvfb-run -a`. The real-model run and screenshots here are from Windows.

[Previous portable CI](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45664813369) · [Source scene](../readmes/245/raised-switchyard.fraynix-level.json) · [Local route receipt](../readmes/246/model-routing-proof.json)
