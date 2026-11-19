# Edition 188 implementation plan

Test the final review write, after the artifact and the browser receipt already exist. Use a new isolated Windows project with 125 browser-verified pending chains and three unapproved drafts. Block the selected artifact directory briefly to leave both verified stages in a durable review, then restore the directory. Hold the selected review file open with Windows sharing that allows reads and denies replacement. Run the ordinary **Finish verified save** action and verify that the current review bytes survive the failed replacement.

Release the file, restart the desktop and confirm that the original review can finish. Compare the artifact ID, version and creation time, both five-check observations, the complete receipt bytes and the other 127 review files. The retry should open the saved artifact with no browser preview replay.

Read the current receipt from disk whenever a completed chain is verified. Compare the artifact ID, version, ten recorded observations and their steps against the original review. Refuse an altered receipt both during recovery and after completion. Refuse a missing receipt on a completed chain and compare the restored proof with its earlier response. Check two published Edition 187 artifacts when its validation project is available.

## Local replay

Install the Java distribution and desktop dependencies using the project setup instructions. Use an unused output directory.

~~~powershell
.\gradlew.bat --offline :workspace-engine:installDist
node docs/publication188/validate-desktop.cjs build/edition188-check
python docs/publication188/validate.py
~~~

An optional second argument to the desktop script points to the Edition 187 qualification project. The script copies its pending reviews and tests the two earlier completed proofs in a separate copy. A run without that argument creates the 125 first-stage reviews from scratch.
