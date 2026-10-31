# Recorded tracking research data

Edition 023 uses selected DAVIS 2017 TrainVal 480p frames for noncommercial research
evaluation. Original JPEGs, indexed PNG masks and base64 import packages remain
outside Git in the chosen research folder. They are not new unrestricted app
assets or model weights.

- [Official downloads](https://davischallenge.org/davis2017/code.html).
- Archive: `https://data.vision.ee.ethz.ch/csergi/share/davis/DAVIS-2017-trainval-480p.zip`.
- Archive size: 832,766,765 bytes; pinned ETag: `"31a3032d-54c6d0af8b4dc"`.
- The archive README's Terms of Use specify [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/).
- `blackswan`, `car-shadow` and `hike` are not external-source exceptions listed
  in the same archive's `SOURCES.md`.

Credit the DAVIS authors: Jordi Pont-Tuset, Federico Perazzi, Sergi Caelles,
Pablo Arbeláez, Alexander Sorkine-Hornung and Luc Van Gool for the 2017 release;
also Federico Perazzi, Jordi Pont-Tuset, Brian McWilliams, Luc Van Gool,
Markus Gross and Alexander Sorkine-Hornung for the 2016 dataset work.

Cite *The 2017 DAVIS Challenge on Video Object Segmentation*, arXiv:1704.00675,
and *A Benchmark Dataset and Evaluation Methodology for Video Object Segmentation*,
CVPR 2016. Both are linked from the official download page.

Research documentation contains credited reduced thumbnails, app screenshots and
derived bounding-box/overlap data. Changes include resizing, displayed reference
and prediction rectangles, and charts. These research materials retain the source
data's noncommercial terms; the dataset does not relicense the independent Java
implementations.

`scripts/Prepare-TrackingResearch.py` fetches only declared members through verified
ZIP byte ranges, validates CRCs and records individual SHA-256 hashes. It does not
download the full archive. Reports retain original source-frame ordinals and
image/mask hashes.

Separate synthetic software controls use the existing public-domain NASA photo
credited in [PERCEPTION_FIXTURES.md](PERCEPTION_FIXTURES.md), with generated motion,
aspect changes and masks. They are not real recorded motion or independent
evaluation footage.
