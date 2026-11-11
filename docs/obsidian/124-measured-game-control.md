# Measured game control — edition116

[[README-116.md]] preserves the original failure and original controller, adds bounded collision-ray landing prediction and committed edge crossing through normal input. Twelve hardware layouts: simple12/12, original5/12, revised12/12. Revised collects2 power-ups versus original7; completion is not richer play. Six reserved layouts also repeat on software rendering.

The115 CI failure was an invalid GLSL330 sampler index, not missing image tolerance. Literal sampler branches and explicit shader failures restore actual sprites. Shader bytes join engine identity; unchanged known-sprite matching now passes both renderers. Six fresh references per renderer reconstruct separate candidates and playtest; four unsupported images refuse. Actual chat/restart costs and all failed prototypes retained. Wider image seed15 with a five-tile gap remains an AI failure: simple6/6 versus revised5/6 reconstructed courses; do not conflate that with the narrower12-case cohort.

No new model training or general scene understanding. Next: wider-gap contact recovery, safe power-up pursuit and explicit procedure retirement/reactivation under32 active slots. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]] preserves the larger phased direction.
