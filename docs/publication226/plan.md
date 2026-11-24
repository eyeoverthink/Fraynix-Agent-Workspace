
# Edit shared mesh components and model a browser reference

2026-11-24 · Edition 226

Extend the Java production workbench with direct component editing and a browser reference workflow. The goal is to let the app inspect an observation, build an editable hypothesis, compare it with the observation, and retain the evidence.

## Mesh editing and undo

- Vertex edits act on a mesh index. An edge is one of the three directed edges of an indexed triangle. A face move translates the three vertices of one triangle. Connected faces share those vertices and deform with the edit.
- Java checks that affected triangles keep their orientation and that the result retains a closed, oriented shell. These checks do not certify global self-intersections.
- Each save binds the exact preview to its source and creates a new scene. Undo uses the source scene and mesh hashes from the checked receipt and reopens the parent. Previous and later copies remain available.
- Fitting computes the local mesh bounds, updates the collider, recenters the visual vertices and rotates the pivot shift into world coordinates. Existing translation keys shift by the same amount. A rotated animated fixture checks that the end pose's travel is retained.
- Blender 4.5 LTS independently measured the 530 mm box, 15 mm center shift and the original triangle count, then edited the visual cap. Fraynix imported the changed mesh with its collider. An incompatible change to a shared triangle is refused before saving.

## Browser evidence

The host reads a unique CSS selector inside the in-app browser's isolated world, checks its visible bounds, captures the rendered pixels and checks the same page and bounds again. The selected graphic must be a visible image, SVG or canvas from 16 to 512 device-independent pixels per side. The retained URL has credentials, query and fragment removed.

Java uses a contrasting uniform background and keeps the largest connected foreground component. It evaluates 16, 20 and 22 cell grids using foreground intersection over union against the captured mask, checks the resulting shell and saves the best model above 0.78. The original fixture measured 0.837, 1.000 and 0.883. Scale and thickness come from the declared inputs.

The application retains the exact capture package with its hash. The Windows test checks a missing selector, opens the result, exports GLB, restarts, and refuses export after the retained capture bytes change. This is a single-view outline with a chosen depth. Texture copying, general photo segmentation and geometry occluded in the view are still pending.

## Build toward observation-driven creation

1. Let the user or agent pick a visible region directly and label it. Test isolation against textured backgrounds and multiple objects.
2. Add a second browser or Blender view, or known dimensions. Record competing depth hypotheses and choose the view that can distinguish them.
3. Render the resulting geometry into a held-out view. Measure silhouette and appearance errors independently, propose a bounded repair, and keep the result only if it improves the check.
4. Expose the same typed capture, edit and verification operations to a reviewed, durable chat request.

[Production workbench roadmap](../../README-225.md) · [Browser capture and mesh test](../../workspace-desktop/test/spatial-components-integration.cjs) · [Measurement receipt](../readmes/226/component-trace-evidence.json)
