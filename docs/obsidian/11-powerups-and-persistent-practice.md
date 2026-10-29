# Powerups and persistent practice

[Edition 004](../../README-004.md) fixes character growth rendering and adds power-up goals, persistent motor rewards, failed-goal memory and finite practice.

- [Full vision and review suggestions](../EMBODIED_AI_VISION.md)
- [Character renderer](../../src/main/java/renderer/RenderBatch.java)
- [Goal planner](../../src/main/java/agent/PowerupPlanner.java)
- [Play memory](../../src/main/java/agent/PlayMemory.java)
- [Practice controller](../../src/main/java/agent/PlayTraining.java)
- [Native evidence](../readmes/004/native-route.log)
- [Interactive explorer](../readmes/004/index.html)

The original-level target prefix improves from 1117 to 839 simulation ticks while retaining one mushroom pickup and zero damage. This result comes from explicit saved experience; visual recognition and reward-linked neural gameplay remain later milestones.
