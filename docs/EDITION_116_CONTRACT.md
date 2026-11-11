# Edition 116 — measured controller recovery and new reference images

2026-11-11, America/Los_Angeles. Continue Fraynix-Agent-Workspace from published main `2eae6260`. Preserve private projects and frozen editions 001–115. No delegation; expensive validation runs serially.

CI first: edition 115 main failed one of 863 workspace-engine tests (two skipped): the software-rendered native overview did not match the original terrain grid. Edition 114 main passed. Retain the CI log and original test report. Reproduce this rendering difference and correct matching without dropping source, contrast, unknown-content or playability checks.

Reproduce the seed0 controller failure with the original policy in a fresh session. Diagnose measured position, velocity, goal, input and grounded state. Improve ordinary input selection using observed terrain and landing conditions; do not change actor positions, terrain, physics, finish rules or test outcomes to obtain a win. Keep a selectable original-policy baseline for comparisons. Compare simple control, original AI and revised AI on the failure and separately declared new layouts. Report misses, costs and power-up outcomes as well as success. No claim of model training.

Test new native reference images across new seeds, dimensions, gap widths, crops and rendering conditions. Retain original PNGs and declared transforms; verify reconstructed geometry against the independent source candidate. Include unsupported/occluded images and preserve refusals. Image appearance does not reveal hidden component behavior. Reconstruction must still produce a separately checked scene and actual input-driven playtest.

Existing durable chat ownership, source/engine identities, repair budgets, restart rules, trial evidence and procedure limits remain. Historical proofs stay bound to their original implementation; never relabel old trials as current evidence. Record actual UI requests and screenshots, update the linked README, offline guide, memories and logs, then push main/tag and verify the fetched files. Report remote CI separately from local checks.
