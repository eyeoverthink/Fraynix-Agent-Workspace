# Real recorded observations · Delfys75

Source: [Physical Parameter Prediction](https://www.kaggle.com/datasets/jaswar/physical-parameter-prediction), version 11, associated with Castañeda Garcia, Warchocki, van Gemert, Brinks and Tömen, *Learning Physics From Video*, CVPR 2025. The dataset listing reports MIT. The paper has its own publication license; no paper figures or paper text are redistributed here.

Use [appendix A.6](https://arxiv.org/html/2410.01376v2#A6) for the reported 0.726 m travel distance and nominal 20° low setting with an approximately one-degree angle error. The published aggregate friction summary was not used as the optimizer target or search bounds.

The observations come from real recordings and the authors' supplied segmentation masks. They are estimated mask-centroid positions, not raw instrument position measurements. Only derived numerical CSV/tracking data, protocols and provenance are committed. Raw recordings/masks can be retrieved with the pinned downloader; they remain under ignored build storage.

## Fixed selection and transformation

Training is low/01, validation is low/02, selected before trajectory inspection. Both recordings are 1920×1080 at 60 fps. Threshold each mask at gray >127; use the largest connected component only when it occupies at least 95% of foreground and at least 100 pixels. Missing/ambiguous masks fail preparation; there is no interpolation, smoothing or silent row deletion.

The downhill direction is the training first-to-last centroid direction. A constant image-plane scale maps its pixel travel to the reported 0.726 m. Freeze that direction, scale, selection and numerical criteria before processing validation centroids. Each recording uses its own first-frame spatial origin. Time is source frame/60; no fitted release offset or initial velocity correction. The training endpoint constrains scale, so its distance is not an independent check of that same scale.

The shared-camera/constant-scale assumption is approximate. The second recording's last displacement under this unchanged mapping is 0.775559 m, not forced to equal 0.726 m. Neither that difference nor the fit residuals are corrected by rescaling validation.

The raw initial protocol is retained as [initial-protocol.json](initial-protocol.json); [the frozen spatial protocol](spatial-calibration-frozen.json) and [provenance](provenance.json) state exact mappings, assumptions and SHA-256 digests. The later reproducible processor preserves the same CSV bytes as the first actual run.

## Native model and criteria

The current engine uses a nominal 20° ramp, gravity 9.81 m/s² and friction zero as baseline. Its existing 12 m ramp, 0.5 m cube and 1 kg mass are surrogate assumptions. They are not measured dimensions/mass of the recorded apparatus. The test evaluates the pre-edge downhill trajectory of that surrogate; it is not a complete digital twin or certified material-friction measurement.

Search one parameter, friction, over 0..1. Run each candidate for one simulated second / 240 native ticks. Minimum training RMSE gain is 0.001 m; validation tolerance is 0.015 m. The original three gates remain unchanged: sufficient training gain, every validation residual within tolerance and no validation RMSE regression. No automatic application occurs during research even if a numerical gate passes.

## Actual results

| Metric | Baseline | Fitted |
| --- | ---: | ---: |
| Training RMSE · 52 observations | 0.232497 m | 0.009632 m |
| Validation RMSE · 49 observations | 0.156920 m | 0.059435 m |
| Validation maximum absolute residual | 0.340718 m | 0.159463 m |

Training selects friction **0.15625** using 17 executed candidates and 20 total native operations. Training improvement and validation nonregression pass. The all-point validation gate fails: maximum residual 0.159463 m exceeds 0.015 m. The fit is **rejected**, Apply is unavailable and both scenes remain v1.

The workflow passes eight independent import/provenance/arithmetic/UI checks. This is distinct from a passing fit. The held-apart recording is now an observed regression case; it must not be relabeled an untouched scientific holdout in future development.

Likely questions for subsequent experiments are camera-plane calibration, perspective, source clipping/release timing and surrogate geometry. The current data do not isolate which explanation causes the mismatch. Do not raise tolerance, fit a validation-specific delay or change its scale to produce acceptance. Use newly collected calibration references and fresh evaluation recordings for later scientific claims.

## Reproduce

The app-side replay needs the normal Java/Electron prerequisites and only these committed numerical files:

~~~powershell
cd workspace-desktop
npm.cmd run test:real-data
~~~

To re-extract observations from pinned raw videos/masks, use Python 3.12 with numpy and opencv-python. Those are preprocessing dependencies, not new app runtime dependencies:

~~~powershell
python scripts/Prepare-Delfys75.py --output build/delfys75-case
# After the first source download:
python scripts/Prepare-Delfys75.py --output build/delfys75-case --offline
~~~

The processor verifies five pinned source digests and refuses conflicting existing output. It never falls back to simulated data. It reproduces [training.csv](training.csv) and [validation.csv](validation.csv) exactly in the recorded environment. Recorded Python package versions and the processor's canonical source hash appear in provenance. [Published execution evidence](../../readmes/013/real-data-delfys.json).
