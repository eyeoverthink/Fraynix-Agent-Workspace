# Closed-loop native control

Edition 017 · October 30, 2026. Java observes native state and corrects the dynamic block every 12 solver ticks, with a bounded ramp-tangent impulse. A one-second trajectory plus half-second hold has measured position, velocity and settling gates. Real disturbances, pause/step/stop, saved-command replay, restart, export and policy rollback work.

Two feedback gains are selected from 54 actual training trials; 32 separate evaluation trials compare zero, feedforward, hand-designed feedback and the candidate. Protocol/cases/gates were committed before experiments at 58ef53a and remain unchanged. The selected score improves 40.9% against hand-designed feedback and 90.5% against feedforward; worst endpoint 1.36 cm. This is a local development suite, not an external scientific holdout or a general robot/game intelligence result. No new model/spiking weights are trained.

Activation and completed job commit together. Existing one-shot policy remains separate; cancellation, interrupted run handling, actual history and monotonic feedback rollback are tested. Browser/canvas/chat/Game layout and private user state stay intact.

- [README 017](../../README-017.md)
- [Contract](../CLOSED_LOOP_CONTROL.md)
- [Evidence](../readmes/017/verification.json)
- [Map](closed-loop-control.canvas)
- [Memory](../PROJECT_MEMORY.md)
