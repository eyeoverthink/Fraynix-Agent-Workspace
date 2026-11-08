# Edition093 — recovered-state forecasting

[[README-093.md|Full edition]] · [[docs/readmes/093/index.html|Offline evidence]] · [[docs/obsidian/100-growing-bank-restarts.md|Prior edition]]

Java now creates and verifies a distinct forecast from recovered state before later actual motion. Both declared early-restart cases completed; a second interruption during forecasting also recovered. Stopping during forecasting prevented the next actual stage and any world commit.

61 Java checks passed across 11 focused classes; 1 optional host-dependent case skipped. 135 desktop checks passed. Five new native goals completed and one was deliberately cancelled.

One replacement per later stage; original budgets and all proof gates retained. Two-stage synthetic recovery and controlled shutdown tested; abrupt crash, three-stage native recovery and general accuracy remain unvalidated. Resumed cost clocks remain ineligible.

[[workspace-engine/src/main/java/fraymus/workspace/RecoveryForecast093.java]] → [[workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java]] → [[scripts/Inspect-Recovery093.cjs]]
