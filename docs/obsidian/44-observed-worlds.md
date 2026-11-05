# Real footage and persistent world estimates

[[README-036|Edition036]] · [[docs/OBSERVED_WORLDS|Runtime contract]] · [[docs/obsidian/observed-worlds.canvas|System map]]

Java tracks actual human-seeded pixels, predicts prior motion and retains accepted image-plane estimates on the SAME035world/body IDs. Native verified physics remains separate. Missing/outlier observations do not fabricate measurements. Exact prefix replay, Stop generation, stale bindings and complete multi-run export are tested. Nominal covariance is uncalibrated; first reserved results include slightly worse car prediction and poor2sigma coverage.

Next: calibrated plane/time, camera-motion separation, empirically evaluated uncertainty and explicit re-observation. No general semantic recognition, physical calibration or weight training is claimed.
