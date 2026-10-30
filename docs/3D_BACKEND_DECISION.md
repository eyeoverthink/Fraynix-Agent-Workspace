# First 3D adapter decision

October 29, 2026 · implemented Windows x64 slice

Use **Minie 9.0.3 / native Libbulletjme 22.0.3** in the Java workspace engine and **Three.js 0.186.1** in the trusted existing shell renderer. Java owns authored entities, physics, clock, experiment records and evaluation. The Three.js viewport consumes those poses; it does not calculate motion or own a second physical world. No CDN or new service is required. Gradle/npm dependency declarations and the npm lockfile pin the selected releases.

## Why this combination

- Minie exposes actual Bullet 3D rigid bodies and contacts through Java, including depth, rotations, collision shapes and materials. Headless physics can run without embedding another game-rendering application in Electron.
- Three.js supplies a perspective WebGL2 view inside the existing center canvas area, with selectable geometry, orbit/zoom, live pose updates and measurement charts.
- The existing JBox2D Game Lab remains appropriate for its 2D game and units. It is preserved as a separate app. A new 3D transform schema avoids reinterpreting zIndex as depth or changing old saved levels.
- A bespoke analytical-only solver would not establish actual 3D contact behavior. A complete engine migration would replace substantially more existing rendering/UI infrastructure. Neither was needed for the first bounded lab. No comparative performance benchmark against other backends is claimed.

## Evidence

Eight Java tests exercise varied ramp angles/masses against an ideal reference, friction, resting contact, native free fall, separation along Z, ground contact, scene persistence and revision rejection, pause/step/reset, interruption recovery and replay. Twelve actual Electron integration checks exercise the visible workflow and preserved browser/artifact behavior. The release evidence and actual local-model trial are in [README 010](../README-010.md).

The integration uses fixed external 1/240-second steps and records backend/version, actual native solver type/iteration count, precision, step mode, friction combination, restitution and damping. It retains scene parameters, stable entity identities, authored revision, initial definition, samples, final pose and evaluation. A live run advances four fixed ticks per scheduled 60 Hz callback; simulation time comes from ticks, and may lag wall time under load. Reproducibility is tested on this supported Windows x64 setup, within a declared tolerance.

## Dependencies, redistribution and execution boundaries

The [Minie project overview](https://stephengold.github.io/Minie/minie/overview.html) describes its Bullet foundation and scope. It is a real-time mechanics library; the first lab is an educational/game-scale experiment, not a calibrated scientific or engineering model. Its license text is retained in [MINIE-LICENSE.txt](../licenses/MINIE-LICENSE.txt). Three.js uses the [retained MIT license](../licenses/THREE-LICENSE.txt); see its [official installation guide](https://threejs.org/manual/pages/installation.html).

JME core/desktop/terrain, Heart, sim-math and platform Bullet libraries are resolved from Maven Central by their published dependency metadata. Packages retain their upstream notices. Platform artifacts may be packaged for other systems, but this milestone verifies Windows x64 only. Native extraction goes inside the selected project's experiments/native directory, outside source control. The rendering package is installed from the lockfile and is not vendored into source.

The trusted shell has the existing controlled preload bridge. Browser and generated artifact renderers still have no privileged preload or Node. Scene values are bounded numbers and fixed labels; no imported shaders, meshes or executable scene scripts are accepted in this slice. This does not create an arbitrary native-code execution sandbox or require Docker/WSL.

## Remaining scope

The lab contains a ramp, block and ground with bounded parameters. Arbitrary scene authoring, mesh import, joints/robotics controls, cross-domain atomic transactions, real measurement ingestion, physical-policy training, C/Go generation and full native viewport embedding remain later work. The adapter interface can expand after additional shape/action/evaluation contracts are tested. See [the working roadmap](SCENARIO_ENGINE_ROADMAP.md).
