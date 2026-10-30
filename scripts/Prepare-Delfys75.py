"""Reproduce a disclosed real-video observation case; no synthetic fallback or fit tuning.

Python 3.12, numpy and opencv-python are required only for this preparation step.
The Java app can import the resulting CSVs without those Python packages.
Data: jaswar/physical-parameter-prediction, version 11, dataset listing license MIT.
Physical travel/angle: Castañeda Garcia et al., CVPR 2025, appendix A.6.
"""
import argparse
import csv
import hashlib
import json
from pathlib import Path
import urllib.parse
import urllib.request

import cv2
import numpy as np

DATASET = "jaswar/physical-parameter-prediction"
VERSION = 11
PAPER = "https://arxiv.org/html/2410.01376v2#A6"
FILES = {
    "output_selected/parameters.json": "45d231de37366f5ffd8d7153c51917692c6e5b88b4019418ee0aa444f0aa7724",
    "output_selected/sliding_block/low/01/video.mp4": "2898b48dce48c220d4ecce6a6a319ee2319e8588dfb781285ad1717ecec6b013",
    "output_selected/sliding_block/low/01/masks.mp4": "0dd678c2c5cf9f587ae7e9acedceebc4ec302d6701edf28609c4458ce138c72e",
    "output_selected/sliding_block/low/02/video.mp4": "0a3785298a38e2a7ed69d5ede04c1596595960741eef0eb313a7a369a1ca8b5d",
    "output_selected/sliding_block/low/02/masks.mp4": "5b87be77598e5ba470b4dafecd02513a77b551fd42cd6ef487165d7487e77419",
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def write_once(path, content):
    data = content.encode("utf-8") if isinstance(content, str) else content
    if path.exists():
        if path.read_bytes() != data:
            raise ValueError(f"Different existing content at {path}; choose a new output directory")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def dump(path, value):
    write_once(path, json.dumps(value, indent=2, ensure_ascii=False) + "\n")


def acquire(root, offline):
    records = []
    for name, expected in FILES.items():
        path = root / name
        url = ("https://www.kaggle.com/api/v1/datasets/download/" + DATASET + "/"
               + urllib.parse.quote(name, safe="") + "?datasetVersionNumber=" + str(VERSION))
        if path.exists():
            raw = path.read_bytes()
        else:
            if offline:
                raise FileNotFoundError(f"Offline source cache missing: {path}")
            request = urllib.request.Request(url, headers={"User-Agent": "Fraynix-Real-Data/1.0"})
            with urllib.request.urlopen(request, timeout=40) as response:
                raw = response.read(6_000_001)
            if len(raw) > 6_000_000:
                raise ValueError("Unexpected source size")
        if digest(raw) != expected:
            raise ValueError(f"Pinned source digest differs: {name}")
        write_once(path, raw)
        records.append({"path": name, "url": url, "bytes": len(raw), "sha256": expected})
    return records


def track(path, expected_frames):
    capture = cv2.VideoCapture(str(path))
    try:
        if (not capture.isOpened() or capture.get(cv2.CAP_PROP_FPS) != 60
                or capture.get(cv2.CAP_PROP_FRAME_WIDTH) != 1920
                or capture.get(cv2.CAP_PROP_FRAME_HEIGHT) != 1080):
            raise ValueError("Source must match the pinned 1920x1080 / 60 fps recording")
        rows = []
        while True:
            ok, frame = capture.read()
            if not ok:
                break
            mask = (cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY) > 127).astype("uint8")
            count, _, stats, centers = cv2.connectedComponentsWithStats(mask, 8)
            if count < 2:
                raise ValueError("Missing segmentation mask; no gap filling")
            index = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
            area = int(stats[index, cv2.CC_STAT_AREA])
            if area < 100 or area / max(1, int(mask.sum())) < .95:
                raise ValueError("Ambiguous segmentation; no silent row deletion")
            x, y = centers[index]
            # Independent full-mask centroid agrees when there is one foreground component.
            if count == 2:
                yy, xx = np.nonzero(mask)
                if abs(float(xx.mean()) - x) > 1e-9 or abs(float(yy.mean()) - y) > 1e-9:
                    raise ValueError("Independent centroid check failed")
            i = len(rows)
            rows.append({"frame": i, "timeS": i / 60, "xPx": float(x), "yPx": float(y),
                         "areaPx": area, "components": count - 1})
        if len(rows) != expected_frames:
            raise ValueError(f"Expected {expected_frames} frames; obtained {len(rows)}")
        return rows
    finally:
        capture.release()


def csv_bytes(rows):
    import io
    out = io.StringIO(newline="")
    writer = csv.writer(out, lineterminator="\n")
    writer.writerow(["time_s", "displacement_m", "source_frame", "centroid_x_px",
                     "centroid_y_px", "mask_area_px"])
    for row in rows:
        writer.writerow([format(row["timeS"], ".17g"), format(row["distanceM"], ".17g"),
                         row["frame"], format(row["xPx"], ".17g"),
                         format(row["yPx"], ".17g"), row["areaPx"]])
    return out.getvalue()


def prepare(root, offline=False):
    sources = acquire(root / "source", offline)
    training = track(root / "source/output_selected/sliding_block/low/01/masks.mp4", 52)
    displacement = np.array([training[-1]["xPx"] - training[0]["xPx"],
                             training[-1]["yPx"] - training[0]["yPx"]])
    pixels = float(np.linalg.norm(displacement))
    if pixels <= 0:
        raise ValueError("Training mask has no displacement")
    unit, scale = displacement / pixels, .726 / pixels
    # Freeze projection, endpoint scale, selection and gates BEFORE reading validation centroids.
    protocol = {
        "dataset": DATASET, "version": VERSION, "paper": PAPER,
        "training": "output_selected/sliding_block/low/01",
        "validation": "output_selected/sliding_block/low/02",
        "method": "Largest mask component >127; centroid projected onto training first-to-last line",
        "metresPerPixel": scale, "downhillUnitPixels": unit.tolist(), "physicalTravelM": .726,
        "trainingOriginPixels": [training[0]["xPx"], training[0]["yPx"]],
        "angleDeg": 20, "reportedAngleErrorDeg": 1, "gravityMps2": 9.81,
        "parameter": "friction", "minimum": 0, "maximum": 1, "seconds": 1,
        "toleranceM": .015, "minImprovementM": .001,
        "timeOrigin": "First source frame; each run uses its own first-frame spatial origin",
        "rows": "All source frames, no smoothing, no fitted time shift or velocity correction",
        "apply": "Research only; no automatic application, even if numerical gates pass",
        "assumptions": [
            "Shared camera/projection and constant image-plane scale across these two recordings",
            "Training endpoint uses a reported 0.726 m travel constraint; not independent endpoint validation",
            "Mask centroids are estimated from real recordings, not raw position-sensor measurements",
            "Nominal 20 degrees; image perspective and release timing have unresolved uncertainty",
            "Native 12 m ramp, 0.5 m cube and 1 kg are surrogate assumptions, not measured apparatus",
        ],
    }
    dump(root / "spatial-calibration-frozen.json", protocol)
    validation = track(root / "source/output_selected/sliding_block/low/02/masks.mp4", 49)
    for role, rows in [("training", training), ("validation", validation)]:
        for row in rows:
            row["distanceM"] = float(np.dot([row["xPx"] - rows[0]["xPx"],
                                            row["yPx"] - rows[0]["yPx"]], unit) * scale)
        write_once(root / (role + ".csv"), csv_bytes(rows))
        dump(root / (role + "-tracking.json"), rows)
    manifest = {
        "dataset": DATASET, "version": VERSION,
        "sourcePage": "https://www.kaggle.com/datasets/" + DATASET,
        "datasetListingLicense": "MIT", "sources": sources,
        "software": {"python": "3.12", "opencv": cv2.__version__, "numpy": np.__version__},
        "processorSha256Utf8Lf": digest(Path(__file__).read_bytes().replace(b"\r\n", b"\n")),
        "outputs": {name: digest((root / name).read_bytes()) for name in
                    ["training.csv", "validation.csv", "spatial-calibration-frozen.json"]},
        "scientificStatus": "Exploratory surrogate-model comparison; not certified friction metrology",
    }
    dump(root / "provenance.json", manifest)
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--offline", action="store_true", help="Require all pinned raw files in output/source")
    args = parser.parse_args()
    print(json.dumps(prepare(args.output.resolve(), args.offline), indent=2))
