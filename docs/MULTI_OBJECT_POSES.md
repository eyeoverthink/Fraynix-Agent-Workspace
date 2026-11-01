# Native multi-object poses — edition034

[README034](../README-034.md) · [Previous contact controller](CONTACT_CONTROLLERS.md) · [Original evidence](readmes/034/verification.json)

## Implemented boundary

`spatial-arrange-v1` is a separate **bounded virtual force/torque gripper**. It
arranges2–4 imported dynamic boxes at absolute world positions and yaw headings.
Sources are declared-parameter synthetic scenes, with upright box starts and an
axis-aligned floor. This is not a robot hand, contact-only pushing, calibrated
perception or model-weight training. Existing033controller/profile paths remain
unchanged. Existing Bullet/Minie and desktop dependencies remain explicit.

`PosePlan` resolves actual source roles, checks conservative swept clearance and
lift height, searches target order and refuses blocked/unsupported geometry before
actuation. `PoseRequests` additionally narrows app admission to supported floor
and minimum body sizes. `PosePhysics` applies selected-body central force and
torque. All dynamic bodies, including unselected bystanders, remain physical.
No post-initialization pose/velocity writes occur except compatible cold restore.

`PoseVerify` reconstructs authorized wrench use, displacement/orientation, work,
grip/release continuity, final position/orientation/speed, all inactive-body drift
and actual native solver contact observations. Same-run restore includes every
body, controller and exact committed trace. `PoseCheckpoint` uses canonical hashes,
version/code/source fences and atomic forced writes. These hashes detect changes;
they are not signatures against an adversary rewriting state and its hash.

Normal limits:60N/4Nm;240Hz;60simulated seconds total,12seconds/stage and measured
stall refusal. Final position≤.1m, orientation≤5degrees, linear speed≤.2m/s,
angular speed≤.2rad/s. Protected/inactive drift≤.05m and angle≤5degrees. Lower
explicit limits remain real physical constraints; failure retains no skill.

## Durable execution and observations

`PoseStore` owns one native task and commits all-body/controller observations.
The shared `GoalRunner` owns source/target binding, task progression, stop/resume,
native audit, registered report storage and actual host canvas receipt.
`spatial-arrange-v1` skills retain exact native controller identity and defaults;
fresh source roles are resolved again and every reuse must independently verify.

`/api/pose/observe` uses committed native positions/quaternions for the matching
scene, with explicitly qualified authored-source fallback. Its oriented-box rays
are declared-state visibility, not image detection. `pose.read` separates immutable
sourceObjects from nativeObservation. `goal.read` reports actual owned-run bodies,
force/torque, contacts, controller and checkpoint. Private comparison viewport
targets bind that world's bodies rather than the authored selection's IDs.

New goals currently start from immutable imported sources. Explicit Resume
continues a compatible interrupted goal/run/attempt with all moving bodies.
Separate completed goals do not implicitly share one continuously evolving world.
Versioned cross-goal world ownership is the next proposed feature.

## Frozen capability measurement

Cases/gates committed a28a75e before development. Five native sources locked
e646bc6 after development and before the first reserved measurement. They remain
unchanged. Ten development and ten different reserved definitions each run two
arms: translation-only virtual actuator and full pose control. The same outcome
verifier still checks yaw in both arms. This is a feature/torque ablation and
does not compare with, replace or activate033contact pushing.

Full control passes8/8feasible development and reserved cases; translation-only
passes4/8reserved. Both arms fail force/torque negative controls. Four matched
successful reserved pairs have integrated-force-effort ratio1.0; all-case ratio
1.0436544129234546; mean penalized-time gain12.608333333333338s. Failure60s is a
score penalty, not actual duration. Force effort N·s is not energy. These cases
are now seen; no later tuning may be described as fresh reserved generalization.

`PoseStudyStore` preserves original40runs, exact hashes, chronology and code pin
after20development trials. `PoseStudyReport` exposes all180cells/four paired modes.
`PoseReport` exposes individual body error rows and six actual native chart modes.
Native outcome verification and original Chromium point/data checks precede any
retention. No model-provided gates, reports or success flags authorize completion.

## Lossless bounded exports

`NativeExports.poseComparison` supports a registered64MBtransport, splitting
large original packages into10MBparts. Ordinary native exports remain20MB.
Each part passes the ordinary save bound; a manifest binds names, lengths and
SHA-256, plus original compressed/raw byte hashes. Keep manifest and parts
together. `readNativeExport` rejects missing/altered parts, traversal, oversized
data, reconstructed hash mismatch and invalid gzip/raw hash or size.

```powershell
node scripts/Inspect-PoseExport.cjs docs/readmes/034/examples/native-experience.json
```

Package-private native report storage requires a passing actual verifier;
external flags cannot expand public creation limits. Original reports, all-run
transport and first-measurement archival transport are labelled separately.

## Acceptance and next work

Java tests cover actual native development, negative controls, angular-motion
cold resume, corrupted checkpoint preservation, wrench/source/sensor/row/chart
tampering, frozen locks and first40trial measurement. Actual desktop checks cover
process loss, original reports, visible versioned Skills, fresh roles, blocked
geometry, physical negative, complete exports, Stop/history and moving-state
observations. Actual local-model chat exercises comparison/retention/reuse;
earlier failed episodes and bounded repairs remain in the development record.
Independent offline Java, desktop units and previous UI regressions are recorded
in the edition evidence rather than asserted from a model summary.

Next: version verified final world state, bind later goals to that revision,
invalidate stale observations/targets, make commit/revert explicit, and freeze
different sequential arrangements before implementation. Include process loss
between goals, changed obstacles, blocked later destinations and safe recovery.
Only then broaden non-upright starts, contact transitions and independent sensors.
