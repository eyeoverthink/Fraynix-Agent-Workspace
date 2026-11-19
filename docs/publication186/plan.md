# Edition 186 implementation plan

Save both validated Fray browser receipts before writing the artifact. Bind them to the expected artifact and recheck the saved evidence on recovery. If the desktop stops during the save, use the original review and stable artifact ID to complete the write. Preserve a stranded version folder as recovery evidence. Reuse an already recorded matching browser receipt.

The desktop acceptance test holds the same OS lock used by the world checkpoint writer. It waits until the browser receipts and version files exist while the registered checkpoint still has its original hash. It then forcibly stops the isolated desktop and Java process tree. The next session completes the same save through the real Workbench. The test inspects browser navigations, the original receipts, the artifact count and a sibling review.

Measure the visible chooser at 3, 24, and 60 genuinely paused chains across both conversion recipes. Capture seven local click-to-ready samples at each size. The measurements include listing, verification, transport, rendering and the automation wait. They describe one Windows machine and one run.

Local regression tests also cover the boundary after the registered artifact and test receipt were written. The current release addresses interrupted processes. A later qualification can examine storage failures and recovery with a near-capacity pending bank.
