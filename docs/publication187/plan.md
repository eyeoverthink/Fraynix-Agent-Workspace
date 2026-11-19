# Edition 187 implementation plan

Exercise the existing Fray save recovery through real local filesystem errors at two write destinations. At the first boundary, block creation of the artifact directory. At the second, block creation of the browser receipt directory after Java registers the artifact. Keep the five original browser checks for each conversion and the expected artifact hash. The verification endpoint must refuse completion until the save and receipt both succeed.

After an I/O failure, read the exact durable review and offer **Finish verified save** in the Workbench when the two verified stages are present. Recover the artifact write after a desktop restart. Recover the receipt write in the same session. Verify the original artifact ID, one version, unchanged browser evidence and no preview replay at either boundary. Preserve the existing chain identities, artifact checks and review cap.

Measure seven local click-to-ready samples at 125 verified pending chains in an isolated project. Fill the remaining three slots with unapproved drafts and check the 128-review limit. Compare the exact file hashes of all unaffected reviews before and after recovery. Reopen the archived Edition 181–186 results and run the Java, desktop, and evidence archive regressions locally. Capture the chooser and both failure states in the desktop.

## Local replay

Install the workspace Java distribution and desktop dependencies using the project setup instructions. From the repository root, use a new output directory for each run.

~~~powershell
.\gradlew.bat --offline :workspace-engine:installDist
node docs/publication187/validate-desktop.cjs build/edition187-check
python docs/publication187/validate.py
~~~

The desktop script can create all 125 chains from scratch. To reproduce the archived-proof checks from the published run, pass the Edition 186 desktop qualification project directory as the second argument. It supplies the 59 earlier verified pending reviews and the saved Edition 181–186 proofs. The script works on copies in its own output directory.
