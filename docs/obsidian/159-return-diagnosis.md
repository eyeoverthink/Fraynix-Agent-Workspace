# Return diagnosis and verified replay — edition151

[[README-151.md]]

The shifted/rotated camera return remains unresolved: **11/42 correct observations, 31 missing, 0/30 hidden outputs and 0 matches to the labelled red lighter**, exactly as in edition150. Read-only probes explain why; they do not add recovered observations. Reopening the unchanged verified comparison took **0.127 seconds median**, versus **129.227 seconds** for its cold full replay. Defaults and tracking thresholds remain unchanged.

Across all **30 fully visible returned samples**, the original search overlapped the target in 0 and retained a competing-object veto in 0. At the labelled axis-aligned crop, correlation was **0.564–0.579** and gradient agreement **0.206–0.232**. The best label-guided probes used [10] degrees, raised correlation to **0.919–0.933**, and raised gradient agreement to **0.779–0.812**. Color and quadrant errors improved, but the correlation and gradient thresholds still failed. **0/30** best probes passed all unchanged appearance checks.

This separates two problems: an axis-aligned search failed to propose the rotated target, and simply supplying a rotated crop still did not meet the original full-object evidence requirements. Lowering thresholds on this known sequence would not be evidence of general recovery. The first original hypothesis was near the red object but was rejected; an unaccepted hypothesis is not a measured identity switch.

**29 distinct selected Java checks and 164 desktop checks passed**, with no failures or skipped cases. The final nine focused Java checks include the missing-history refusal and unchanged diagnostic scoring. The broader Java regression passed before that narrow read-only guard was added. Two actual app studies cover one exposed camera case and two exposed generated controls. The installed build receipt matches current compiled inputs. This is a scoped regression, not a rerun of every historical system capability.

Next: label-free orientation-aware proposals, unchanged identity safeguards and fresh evaluation; see [[docs/INTEGRATED_AGENT_ROADMAP.md]].
