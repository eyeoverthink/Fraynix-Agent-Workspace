# Learned world model and recorded reality

Edition 018 · October 30, 2026. Java fits five structured transition models with105 coefficients from512 native transitions, calibrates on128 separate transitions and evaluates256 further transitions plus0.4s rollouts. Native one-step position RMSE 0.57mm, rollout 2.74cm. Counterfactual planning passes native limits with100% model actions, while prior feedback tracks better on mean score.

World model prepares a paused scene and previews49 sequences/seven constant-action alternatives without motion. State digest invalidates changed previews. Live planning executes one impulse, measures innovation and replans; actual JSON/video, cancellation, restart, rollback and command replay work. Violet projected geometry is excluded from native observer/video exports.

Recorded-data free forecast remains rejected: RMSE 2.97cm, maximum 6.22cm against unchanged1.5cm tolerance. First2 recording points condition47 scored forecasts; one-step teacher forcing differs. No observation/mapping change or physical action authority. Predeclared local native evaluation is not an external benchmark; empirical coverage is not a confidence/safety guarantee.

- [README 018](../../README-018.md)
- [Contract](../WORLD_MODEL_REALITY.md)
- [Evidence](../readmes/018/verification.json)
- [Map](world-model-reality.canvas)
- [Memory](../PROJECT_MEMORY.md)
