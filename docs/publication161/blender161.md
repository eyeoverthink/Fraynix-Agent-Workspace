# What Blender could contribute to the Java workspace

Reviewed 2026-11-17. The requested projects.blender.org endpoint was unavailable to the reader. The official [Blender GitHub mirror](https://github.com/blender/blender) identifies the upstream project; the following source files were read there. This is a targeted architecture assessment, not a claim to have audited the entire codebase.

## Useful additions, in priority order

| Idea | Value here | Implementation boundary |
| --- | --- | --- |
| Original and evaluated scene states | Preserve the intended scene while testing changes in a separate runtime state | Edition 161 uses the app's own Java source, provisional trial and accepted revision. Blender's dependency graph is a useful architectural reference. |
| Explicit scale, margins, solver settings | Small shapes require different tolerances from room-sized blocks | Edition 161 declares metres, mass, timestep and size-dependent collision margins in its separate controller. No existing defaults are relaxed. |
| Optional authored scenes and assets | Build detailed environments, material variants, cameras and labelled test arrangements | Future external Blender authoring/export; Java remains the source of task state, authority and outcome verification. |
| Rendered training/evaluation sequences | Produce controlled variations in lighting, occlusion and viewpoints, with independently generated labels | Future synthetic-data lane. Such scenes must be labelled synthetic and kept distinct from camera validation. |
| Scene exchange and replay export | Let users inspect or render the same experiment in another tool | Future glTF plus an explicit physics/identity sidecar, round-trip tested. Geometry transfer alone does not preserve our task/evidence model. |

Blender's dependency graph distinguishes original objects from evaluated objects that include modifiers and constraints. That separation fits the app's distinction between authored scene, experiment and verified outcome. [Official dependency-graph API](https://docs.blender.org/api/current/bpy.types.Depsgraph.html)

Blender's rigid-body implementation calls Bullet, as does this app's existing Java native-physics integration. It explicitly handles collision margins and solver settings. Therefore a Blender simulation could cross-check import/export and setup, but agreement would not constitute an independent proof of real-world physics. Camera measurements, independently marked distances and eventually instrument measurements remain necessary. [Rigid-body source](https://github.com/blender/blender/blob/main/source/blender/blenkernel/intern/rigidbody.cc)

## Source receipts

These are Git blob identities returned by the connected GitHub reader, not claims about a permanently fixed main branch:

| Source path | Blob SHA | Inspected concepts |
| --- | --- | --- |
| `source/blender/blenkernel/intern/rigidbody.cc` | `f784fa630c4f6de4c89628961a4a8f40b052aa54` | Bullet interface, shape construction, margins, solver iterations, runtime objects |
| `source/blender/depsgraph/intern/eval/deg_eval_copy_on_write.cc` | `ca60328065c3358b2613898a1c4d827f198cb0b6` | Original/evaluated datablock separation and original identity |
| `COPYING` | `3be7c91320a650eb99fc4ebda1f5fab48c7dbea2` | Distribution notice and license references |

[Dependency-graph source](https://github.com/blender/blender/blob/main/source/blender/depsgraph/intern/eval/deg_eval_copy_on_write.cc) · [License notice](https://github.com/blender/blender/blob/main/COPYING)

Blender is free/open source, but copying implementation code still carries license obligations; translating it into Java does not remove those obligations. Its software license is distinct from ownership of artwork made with it. Review the particular files and proposed distribution before incorporating derived code. This edition copies no Blender code and adds no Blender runtime dependency. [Blender licensing explanation](https://www.blender.org/about/license/) · [Manual: software and artwork](https://docs.blender.org/manual/en/dev/getting_started/about/license.html)

## Proposed later phases

1. **Exchange contract:** define stable object IDs, SI units, axis conversion, transform hierarchy, collider provenance, mass/friction and unsupported-shape refusal. Round-trip simple boxes with exact tolerances before complex assets.
2. **Optional exporter:** write our own adapter using public APIs; record Blender version and export options. Launch one bounded external job and collect declared output files, without embedding or reparenting its full UI.
3. **Independent labels:** render predeclared trajectories and masks. Keep generator labels separate from tracker inputs and scoring. Compare generated and physical footage without pooling them as equivalent evidence.
4. **Replay films:** render saved experiment states as video while keeping JSON, native traces and source hashes authoritative. A convincing movie is a presentation of evidence, not a replacement for verification.

This preserves the product direction: one local agent workspace, Java-owned reasoning and durable execution, useful optional creation tools, and measured capability claims.
