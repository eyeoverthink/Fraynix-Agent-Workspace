# Edition 275 implementation and verification

## Intent and approval

Java resolves a unique crate label or an explicit reference to the selected crate. A bare “the crate” with multiple candidates, an unmatched name, or a duplicated name opens a question with the eligible objects. The question retains the original language, source hash, scene ID and the exact candidate IDs. Chat and the selection buttons use the same options. A superseded question or an answer from a different bay is refused. The question cannot be used as approval.

```mermaid
flowchart LR
    A["Chat and saved bay"] --> BOne resolved crate?
    B -->|Ask and save| C["Answer in chat or select a listed crate"]
    C --> D["Measure the direct move"]
    B -->|Yes| D
    D -->|Path blocked| E["Measure bounded left and right offsets"]
    D -->|Clear| F["Review exact move"]
    E -->|Clear| F
    E -->|No safe path| G["Keep the original bay"]
    D -->|Aisle already blocked| G
    F --> H["Approve and repeat native checks"]
    H --> I["Save, replay and export"]
```

## How an alternate is chosen

Try the 40 cm door-relative move first. If an obstacle blocks it, try lateral offsets of 20, 35, 50 and 65 cm to both sides, with the outward side first at equal distance. Each alternate still advances 40 cm into the bay. The accepted 50 cm side offsets are straight diagonal sweeps of about 64 cm total. The exact offset and reason for the direct refusal appear in the review. The parent aisle must already be clear.

Every accepted proposal passes the crate’s whole swept path against scene objects and the reserved aisle prism. Java also checks the destination for overlap, the full aisle at three scene states, and the final 60 supported ticks. It compares all source fields except the approved crate position and revision title. Approval repeats the measurement twice in fresh native worlds and verifies that the original obstruction still explains the alternate. Replay repeats the same checks against the stored preview and receipt. Previously saved edition 274 proposals continue through their original measurement format.

## Measured Windows results

Four new synthetic layouts vary door yaw, origin, crate count, aisle length and obstructions. The two accepted alternates went in opposite directions. Both protected two shelves and verified the destination and one-metre aisle. The 68° bay also passed a direct 40 cm move with a different crate. In the narrow bay, the aisle itself was clear but both rails and the trolley blocked the available paths. The closed-aisle control stopped at the initial clearance check.

The desktop exercised three offline restarts. It checked duplicate labels, a stale question, a tampered choice list, an incorrect approval digest, a changed alternate and exact export bytes. Local edition 274 regression and 191 desktop tests passed. The measurements apply to the declared-scale synthetic bays. [Test report](../readmes/275/result.json) · [Exported scene and receipt](../readmes/275/alternate-verified.json)

The prior edition’s loading-aisle CI step passed on all four runners. The broader portable run later failed in mesh component or collider tests. The new scenario runs as a separate step on the same matrix. [Edition 274 portable CI](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/46102180021)

## Reproduce

Run `./gradlew :workspace-engine:installDist` or `gradlew.bat :workspace-engine:installDist` and `npm --prefix workspace-desktop ci`. Then run `node workspace-desktop/test/loading-aisle-alternates-integration.cjs build/loading-aisle-alternates`. On Linux without a display, prefix the test command with `xvfb-run -a`.

The next design test is to measure multi-segment paths around staggered obstacles, with the same independent approval and native sweeps of every segment.
