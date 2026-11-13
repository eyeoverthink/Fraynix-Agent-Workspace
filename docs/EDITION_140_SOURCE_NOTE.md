# Edition 140 source-only eligibility clarification

2026-11-13, after production/scorer freeze and annotation access, before any new source pixels or predictions.

The originally selected group2 has five qualifying annotation absence intervals. Every prescribed starting rectangle fails the existing measurable side bound, and its first-frame control seed also fails. Those exclusions are retained in `build/annotation140-first.json` and the original group2 annotations. No tracker output caused this decision. The minimum size and all runtime/scoring checks remain unchanged.

Use the next entry after group2 in the official metadata order, group3, for this one slot, under the same fixed interval/control selection rules. This is a bounded source-only replacement. If it is also ineligible, retain only the other three recordings and report the smaller cohort; do not search further for favorable results. Preserve the originally frozen contract and this explicit clarification.

Group1 has no qualifying NaN interval and therefore uses the predeclared first96 control. NaN does not establish complete occlusion: visibility and disappearance cause still require source-only image review, with uncertainty kept explicit.

Group3 also has five qualifying intervals, and all prescribed seeds plus the first control seed fail the same bound. Per the bounded fallback above, stop source search. The actual cohort is cat1, cat2 and group1: three windows and twelve comparisons across four methods. Both excluded annotation archives and exact geometry findings remain retained. No new pixels or tracking predictions were read before this decision. Production and scoring rules are unchanged; the harness description/count check is updated to reflect the actual cohort.
