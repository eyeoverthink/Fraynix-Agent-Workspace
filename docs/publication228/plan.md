
# Recover a through-opening with a top view

2026-11-24 · Edition 228

The previous front and side workflow estimates outer dimensions. An aperture along the vertical axis can disappear in every level silhouette. This edition adds an optional top capture and an elevated witness so the app can distinguish the two models.

## Registration and witness contract

- The front camera is on +Z and the left-side camera is on −X. The optional top camera is on +Y, with +X to the right and +Z down. All crops have a common orthographic scale, center and size. Java compares top bounds against the X extent of the front and the Z extent of the side.
- Fraynix fits the intersection on the 16, 20 and 22 cell grids. Front, side and any supplied top view vote on the fit. The separate witness never enters grid selection.
- The independent witness remains at 45° horizontal yaw between front and side and can now be elevated 60° above the horizon. This projection uses the exported triangle geometry coordinates. Elevated masks use nine subpixel samples per captured pixel, which improved the positive case from 86.9% to 93.6% in a 75 px browser pane.
- The same held-out refusal threshold of 86% applies. The source SHA binds the exact crop package on restart and export. The original two-view, level format remains readable.

## Independent controls

Blender authored a plate with an asymmetric rectangular aperture that goes through its height. Front and side recovered the outer dimensions, and a level witness accepted the filled volume. An elevated render exposed the opening. With the same front and side bytes and the same elevated witness, Fraynix refused the two-view candidate at 76.5%. Including the top capture produced 96.0%. Removing the top repeated the refusal without saving an additional scene.

Blender reopened both GLBs and measured 20.0 L for the filled control and 12.5 L for the corrected shell. Its 240 px renders kept both front and side silhouettes identical to the original. The elevated image improved from 77.1% to 100% measured mask overlap in Blender 5.3. Fraynix’s comparison operates on captured browser pixels and uses a separate rasterizer. The desktop test also covered invalid top registration, reopen, and refusal after altering retained source bytes.

## Reproduce

Run the browser integration with the top fixture. Set FRAYNIX_TEST_BLENDER for the independent Blender verification. The fixture generator can rerender all five sources.

    ./gradlew :workspace-engine:installDist
    node workspace-desktop/test/browser-views-integration.cjs build/visual-browser-top228 top

[Original crop](../readmes/228/captured-top.png) · [Elevated refusal](../readmes/228/elevated-refused.png) · [Receipt](../readmes/228/browser-top-evidence.json) · [Blender fixture](../../workspace-desktop/test/browser-top-blender.py)
