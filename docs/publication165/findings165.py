from pathlib import Path
import json, statistics

r = Path.cwd()
comp = json.loads((r/'build/comparison165.json').read_bytes())
assert comp['passed'] and comp['totalCompletedWorkflowsIncludingBaseline'] == 60
baseline = comp['publishedBaseline']
pair_saving = comp['meanSavedPercent']
old_saving = baseline['savedPercent']
def change(value):
    return f'{abs(value):.1f}% '+('less' if value >= 0 else 'more')+' time'
finding = ('Edition 165 reuses completed source/history computation behind fresh evidence checks. '
           f'In two alternating local pairs it used {change(pair_saving)} than the new full-computation control. '
           f'Against two untouched published164 sessions, it used {change(old_saving)} on average. '
           'All 60 final workflows retained exact source, numerical and actual-report outcomes. '
           'New raw evidence is published outside Git with verified retrieval; architecture renewal is documented as explicit subsequent phases.')
lines = [f'Against untouched published164, mean complete task time was **{baseline["meanTaskMs"]/1000:.2f}s** per ten-task session; edition165 reuse averaged **{baseline["candidateMeanTaskMs"]/1000:.2f}s**, or **{change(old_saving)}**. The two same-build pairs averaged **{change(pair_saving)}**. All individual measurements are retained.',
         '| Run | Complete tasks s | Whole session s | Cold verification s |\n| --- | ---: | ---: | ---: |']
for row in baseline['runs']:
    lines[-1] += f'\n| published164 {row["run"]} | {row["totalTaskMs"]/1000:.2f} | {row["totalSessionMs"]/1000:.2f} | {row["coldVerificationMs"]/1000:.2f} |'
for row in comp['sessions']:
    total = sum(x['totalMs'] for x in comp['costRows'] if x['run'] == row['run'])
    lines[-1] += f'\n| edition165 {row["run"]} | {total/1000:.2f} | {row["totalSessionMs"]/1000:.2f} | {row["coldVerificationMs"]/1000:.2f} |'
lines += ['Timing categories below are inclusive and overlap. They explain where the implementation spends time; they do not form an additive partition of task cost.', '| Mode | Source checks s | History checks s | Outcome checks s |\n| --- | ---: | ---: | ---: |']
for mode in ['on', 'off']:
    totals = {}
    for row in comp['diagnosticRows']:
        if row['run'].startswith(mode+'-') and row['domain'] == 'java':
            totals[row['operation']] = totals.get(row['operation'], 0) + row['inclusiveMs']
    lines[-1] += '\n| '+mode+' | '+' | '.join(f'{totals.get(k, 0)/1000:.3f}' for k in ['SOURCE', 'HISTORY', 'NATIVE_OUTCOME'])+' |'
    print(json.dumps({'mode': mode, 'diagnosticTotalsMs': totals}))
lines += ['The full-computation control includes the newly added witnesses. Savings against that control alone would not establish an improvement over the old app. The separate published-build comparison is therefore reported directly. Repeated source episodes, only two pairs and a single machine limit generalization. Cold processes intentionally rebuild computation; the cache is not a durable proof store.']
(r/'build/findings165.md').write_text('\n\n'.join(lines)+'\n', encoding='utf8')
(r/'build/findings165.json').write_text(json.dumps({'finding': finding, 'meanPairedSavingPercent': pair_saving, 'publishedSavingPercent': old_saving}, indent=2)+'\n', encoding='utf8')
print(finding)
