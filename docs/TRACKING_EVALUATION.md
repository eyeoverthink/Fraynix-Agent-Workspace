# Edition 023: tracking evaluation protocol

This phase continues edition 022's browser perception with a real, bounded
comparison and promotion loop. The original tracker is the baseline. A candidate
must improve measured tracking on separate sequences before it becomes the
default for new runs. Rejected candidates remain recorded; rollback restores a
previous active version. Existing runs retain their pinned tracker version.

The machine-readable [acceptance declaration](../workspace-engine/src/main/resources/perception/acceptance-023.json)
is committed before the first tracking evaluation. Development uses only the
DAVIS `blackswan` sequence. `car-shadow` and `hike` are reserved for a single
evaluation after the candidate implementation is frozen. Only the first-frame
reference rectangle initializes tracking. Later reference masks are available to
the scorer, never to the tracker. Every selected frame counts, including failures.

The official [DAVIS download page](https://davischallenge.org/davis2017/code.html)
provides original frames and annotations. Its archive's README Terms of Use specify
CC BY-NC 4.0. Data remains separate from normal projects and model weights; source
URLs, archive metadata, original file hashes and attribution are retained. These
are human-guided bounding-box measurements derived from masks, rather than the
official segmentation benchmark metrics. Original capture FPS is not assumed.

Implementation, measured results, interface screenshots and the final edition 023
README are pending. No candidate improvement or promotion is claimed yet.
