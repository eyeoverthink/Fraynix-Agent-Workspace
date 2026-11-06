# Patient-chart continuity: useful engineering, qualified mathematics

[Original supplied chart](research075/patient-chart-source.txt) · [Edition 075 contract](EDITION_075_CONTRACT.md) · [Existing Java HDC memory](../fraynix-modules/brain-core/src/main/java/fraymus/neural/HdcSkillMemory.java) · [Existing continuing agent](../workspace-engine/src/main/java/fraymus/workspace/AgentRuntime042.java)

The useful architectural idea is a durable, compact chart that a restarted agent reads before acting: identity, current source, demonstrated capabilities, evidence, failures, invalidated assumptions and the next bounded task. The current application already persists Java identity, goals, skills and episodes. Edition075 applies that idea to exact reconstruction lineage: what original experiment was checked, which native/restore contract it used, the owned proof, its creation cost and whether that proof still applies. A certificate may save repeat computation; its descriptive label cannot replace verification.

The supplied ages are explicitly synthetic. They should remain a curriculum or narrative, never a claim that this application has passed arithmetic-to-quantum competency tests. The app is not a patient with clinically established amnesia, and loading a chart does not establish consciousness or AGI. No patent search, validity or ownership opinion is implied by this engineering review.

## What transfers now

- **Continuity with provenance.** Read current verified records at startup/resume. Keep the original evidence and explicit unknowns; do not turn a model-written summary into an authority store.
- **Role/value binding.** XOR can bind binary representations, and binding twice with the same role recovers the original bits. An HDC vector can index related cases. It does not prove that the retrieved procedure is compatible with the current source or succeeds physically.
- **Measured curriculum.** Store a claimed capability separately from a task it passed, including input domain, units, original checks, cost and failure cases. New claims need held-out evaluation. A synthetic age is not a score.
- **Real observations.** Timestamp, units, calibration, source identity and uncertainty belong beside a measured value. A camera or sensor observation is different from a declared native simulation value. Edition075 adds no new physical data feed.

The existing `HdcSkillMemory` uses 8,192 bits, learned phrase votes, exact skill references and bounded eligibility filtering. `FraynixCortex061` also uses XOR/popcount similarity in its existing processing. Those implementations are not evidence that the supplied 16,384-bit/register/hardware configuration is active. The small episode bank in075 uses transparent component distance; changing that to HDC would require a separate accuracy/cost comparison rather than assuming more dimensions help.

## Mathematical and evidential qualifications

| Chart item | Review |
| --- | --- |
| Arithmetic, distributivity, quadratic formula and Vieta | Standard identities with their domains. The quadratic formulas require `a != 0`. A formula in a chart is not a demonstrated solver. |
| Pythagoras and circle area | Apply to the stated Euclidean geometry: a right triangle, and a circle of radius r. Units and geometric assumptions matter. |
| Derivatives and integrals | The power antiderivative requires `n != -1`; `1/x` instead integrates to `ln|x| + C` on an interval excluding zero. Differentiability/integrability assumptions accompany the chain/product rules and FTC. [OpenStax calculus](https://openstax.org/books/calculus-volume-1/pages/6-7-integrals-exponential-functions-and-logarithms). |
| `D = 16384` | If D is **bits**, then 256 64-bit words =2,048 bytes =2 KiB =32 512-bit blocks. This is storage arithmetic, not evidence that Java emitted ZMM instructions, that SIMD is supported, or that a neural accelerator exists. |
| `H = popcount(a XOR b)`, `sim = 1-H/D` | A valid normalized Hamming similarity for two equal-length D-bit vectors. It is not a probability of task success or a proof of semantic equivalence. |
| `entropy 90`, voltage threshold, bus/clock/offset/checksum | The chart supplies no entropy definition, units, circuit parameters, binary payload or measured trace. These remain unverified. A transistor-style square-law expression needs its operating assumptions and parameters; a phi threshold alone does not establish a neuron or learning mechanism. A 64 KiB/128-sector relationship additionally assumes 512-byte sectors. |
| Quantum expressions | The displayed Schrödinger equation is the familiar nonrelativistic particle form. `ψ*ψ` denotes probability **density** for a spatial wavefunction; probabilities require integration and normalization. A qubit amplitude pair must be normalized. [MIT quantum-physics notes](https://ocw.mit.edu/courses/8-04-quantum-physics-i-spring-2016/d87c768c4e1181fde8d4366112169719_MIT8_04S16_LecNotes6.pdf). No quantum hardware or entanglement experiment was added. |
| Mass/energy and orbital expressions | The invariant-mass shorthand assumes natural units `c=1` and squared norm of total momentum. Vis-viva assumes the specified two-body Keplerian model and consistent gravitational parameter, distances and units. Naming CERN/NASA does not provide an observed dataset or validation. |
| Phi resonance as written | The two arguments differ: `v*phi` versus `v-phi`. At `v=1` it evaluates to approximately `-4.23606798`, so it is not a normalized 0–1 resonance score. If a bounded periodic score is intended, `1-2*abs(v*phi-floor(v*phi+0.5))` is one different candidate formula; that correction is a proposal, not an inferred intention or a proven useful controller. |

## Recommended chart structure

`identity → current source/revision → verified procedures and exact proofs → invalidated/missing records → measured costs → current bounded goal → next checks`.

Keep hypotheses, educational formulas, imported text, simulator results and physical measurements as distinct record types. Retrieval suggests what to investigate. The existing Java validators decide whether a proof remains applicable. A changed original source, native engine, restore contract or proof invalidates a reconstruction certificate; a valid old certificate never skips a new scene's forecast or actual execution.

A useful next extension is an evidence-backed capability chart, so the agent can report correctly: “I have checked this original experiment before; its source and engine still match; this new task still needs its own test; here is the cost and evidence.” Edition075 measures that narrower, useful capability and retains negative results.
