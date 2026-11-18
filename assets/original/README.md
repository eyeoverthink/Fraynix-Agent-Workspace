# Original game content

Relay Garden is authored in this repository. Its robot, modular deck, planters,
beacon, sky, tone waveforms and level layout are constructed by
`src/main/java/content/RelayGardenGenerator.java` without reading any existing
image, sound or level. The generator uses the JDK graphics and audio APIs and
the project's existing JSON library. No stock image, model, music or external
content generator is used.

The existing renderer, physics, player inputs, animation and contact components
remain engine dependencies with their own notices. Original content does not
replace or relicense that code. Legacy cue names are compatibility keys, not
copied sound recordings. Existing packs and saved levels remain unchanged.

Regenerate into a **new directory** with `generateRelayGarden -PrelayOutput=...`.
The checked-in manifest identifies every content byte. Launch and reset check
the source and installed pack before execution. Select **Play Relay Garden** in
integrated Game Lab. Movement, jumping, AI play and Reset use the actual runtime.

Project-wide distribution licensing remains unselected. This provenance note is
not a new license grant or a claim that all inherited assets are cleared.
