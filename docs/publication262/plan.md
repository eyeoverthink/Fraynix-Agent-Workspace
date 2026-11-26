# Edition 262 research and implementation

## Research

buildingSMART IFC represents an opening as a void that can exist independently of the door or window object that fills it. Fraynix checks the open space and surrounding solid at separate heights. It uses its own local GLB workflow. [IFC opening element](https://standards.buildingsmart.org/IFC/DEV/IFC4_3/HTML/lexical/IfcOpeningElement.html)

Plan2Scene combines floor plans and photos to build textured scenes. This phase uses geometry from retained GLB triangles and keeps appearance attached to the original source. A later texture workflow can pair each observation with the surface it supports. [Plan2Scene research project](https://3dlg-hcvc.github.io/plan2scene/)

## Measurement and retention

Choose up to six bands with explicit world-height boundaries. Java cuts original triangles at one-quarter, one-half and three-quarters of each band and calculates one common X/Z grid. The centre is the reference and the other two heights are witnesses. The intersection of all three filled-cell sets becomes eligible for rebuilding. Their union minus that intersection is shown as disagreement. Open contours are counted separately. The preview fits the observed footprint with a common frame across bands.

The generated GLB stores editable box meshes for agreed cells and binds the exact retained source and audit identifiers. It extends the sampled footprint over each band’s declared height. Java cuts the result at all three heights before returning it for desktop activation. A highlighted disagreement requires explicit authorization to leave the varying cells out. There is no semantic door or window inference in the algorithm. Geometry between probes, thin features, open surfaces and source scale outside the model require further evidence.

The durable receipt stores source identity, sampling settings and the resulting audit digest. Reviewing, exporting or rebuilding regenerates the measured report and checks the original GLB bytes. The renderer’s context failure leaves the decoded scene, Java calculations and CPU ray queries available.

## Acceptance

- Independent authored facade with a door, window, sill and headers. The three-band case matched nine generated sections. Fraynix and Blender checked six separate elevations on both models, including the two clear openings and four blocked controls.
- The broad-band case detected 120 changing positions and required explicit approval for a partial rebuild. An invalid height sequence refused.
- Desktop restart reopened the exact audit and source link. Replacing an original byte blocked both reporting and reconstruction.
- The apartment test found 2,987 agreed and 1,490 changing positions over nine cuts on a 10 cm grid. Explicit partial rebuild produced 137 solids. The untouched original reopened from the rebuild.
- The existing floor-plan and environment regressions passed locally. A forced WebGL failure verified usable decoded geometry, measured rays, exports and height-layer rebuilds with the viewport removed. Hosted Intel Mac coverage is recorded in the portable matrix.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/height-layers-integration.cjs build/height-layers
blender -b --python-exit-code 5 --python workspace-desktop/test/height-layers-blender.py -- build/height-layers
$env:FRAYNIX_TEST_NO_WEBGL='1'
node workspace-desktop/test/environment-integration.cjs build/environment-no-webgl
node workspace-desktop/test/height-layers-integration.cjs build/height-layers-no-webgl
```

Use `./gradlew` on macOS or Linux. Use `xvfb-run -a` for desktop tests on Linux without a display.

## Next design test

Use geometric elevation changes to propose additional boundaries. Spend the extra cuts where disagreement is largest, compare the cost of each refinement, and confirm the resulting openings with new probes and a measured path through the scene.

[Edition 261](../../README-261.md) · [Public sample tests and models](../readmes/262/FILE_TREE.md)
