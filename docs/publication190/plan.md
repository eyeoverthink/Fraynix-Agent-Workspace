# Edition 190 implementation and qualification

## Round trip into native Game Lab

Export a paused original Relay course as an editable Blender layout. Use the exported mesh identities and Java snapshot to interpret translations. The first import contract accepts one to sixteen existing ground tiles moved in the game plane, on the original quarter-unit grid and clear of the start and finish. Require complete original drawable identities. Compare quad vertices and transforms in Blender. Refuse new, missing or scaled objects, changed protected objects, off-grid destinations and overlapping ground cells. Preserve the original Blender input hash.

Java rechecks the exported snapshot and the original Relay asset manifest. Rebuild the candidate from the original native components and replace only accepted ground positions. Keep a separate checked candidate directory and independently recompile it on read. A native game process verifies the candidate level bytes and original content assets before opening. Each import gets a distinct candidate ID.

## Playtest evidence

The existing Java AI controller receives bounded 24-tick input chunks in a separate game process. Establish the native spawn, keep session and player identity through each chunk, and record actual position, velocity and beacon contact. A second fresh game process must complete with the same native trace. Bind both runs to the checked Blender input, original snapshot, level, installed engine and finish images. Assert that this background work does not advance the interactive game session. Keep failures and require actual beacon contact for verification.

Qualify the desktop path with a Blender-authored change that moves one ground tile into the first Relay Garden gap. Open the resulting candidate, take manual steps, run the AI playtest, recheck the evidence and restart the app. Confirm the original course still opens with the old tile position. Refuse protected-player edits, off-grid edits and changed source or evidence. Run a separate sixteen-tile wide-gap fixture to confirm a structurally valid import can still fail playtesting.

## Reproduce

Configure JDK 21 and set FRAYNIX_BLENDER to the executable. Use a fresh output directory.

~~~powershell
.\gradlew.bat --offline :workspace-engine:installDist :prepareGameRuntime115
node docs/publication190/validate-desktop.cjs build/edition190-check
python docs/publication190/validate.py
~~~

Candidate import presently covers original Relay sources and ground translations. New geometry, additional gameplay roles, independently calibrated scenes and direct chat scheduling can build on the same evidence contract. [Blender background scripting](https://docs.blender.org/manual/en/dev/advanced/command_line/arguments.html)
