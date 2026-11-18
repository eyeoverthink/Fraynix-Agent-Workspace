from pathlib import Path
import json, statistics, collections
r=Path.cwd();c=json.loads((r/'build/comparison164.json').read_bytes());assert c['passed']
mean=statistics.mean
depths={n:[p for p in c['probeRows']if p['revision']==n]for n in [2,4,6,8]}
hist={n:mean([p[k]for p in rows for k in ['beforeOriginalCheckMs','afterOriginalCheckMs']])for n,rows in depths.items()}
native=mean(p['nativeReplayMs']for p in depths[8]);report=mean(p['reportSourceAuditMs']for p in depths[8])
s=f"Across all four sessions, mean original full-history verification grew from **{hist[2]/1000:.3f}s at two entries to {hist[8]/1000:.3f}s at eight** ({hist[8]/hist[2]:.2f}×). At eight entries, the separate repeated native runs averaged **{native/1000:.3f}s**, and report rebuilding/source/saved-observation audit averaged **{report/1000:.3f}s**. These separately repeated measurements identify work; they do not partition the original verifier's time.\n\n"
s+='| Repeated source task | Early mean complete task s | Later mean complete task s |\n| --- | ---: | ---: |\n'
for first,last,label in [('motion-1-camera-end','motion-5-camera-end','Camera end: history1 →5'),('motion-2-generated-short-start','motion-6-generated-short-start','Generated short start: history2 →6')]:
 a=mean(x['totalMs']for x in c['costRows']if x['case']==first);b=mean(x['totalMs']for x in c['costRows']if x['case']==last);s+=f'| {label} | {a/1000:.2f} | {b/1000:.2f} |\n'
s+='\nThese repeated endpoints have growing history and different retained procedure ancestry. They show complete-task growth, not an isolated causal estimate of history length.\n\n'
totals=collections.defaultdict(lambda:dict(count=0,inclusiveMs=0,exclusiveMs=0))
for row in c['diagnosticRows']:
 if row['run'].startswith('on-'):
  t=totals[(row['domain'],row['operation'])]
  for k in t:t[k]+=row[k]
s+='Diagnostic totals below cover the **twenty timed tasks in the two timing-enabled sessions**, joined across process restarts. Read-only probes and their separately collected counters are outside these task intervals. Counts include repeated required checks, not distinct new observations.\n\n| Domain / operation | Calls | Including children s | Excluding children s |\n| --- | ---: | ---: | ---: |\n'
for (domain,op),v in sorted(totals.items(),key=lambda x:(x[0][0],-x[1]['exclusiveMs'])):
 s+=f"| {domain} / {op} | {v['count']} | {v['inclusiveMs']/1000:.3f} | {v['exclusiveMs']/1000:.3f} |\n"
s+='\nThe next optimization should be selected from these measured repeated operations and evaluated using the whole request. Diagnostic wrappers are not permission to skip an original check.\n'
(r/'build/findings164.md').write_text(s,encoding='utf8')
print(json.dumps({'historyMeanMs':hist,'depth8RepeatedNativeMs':native,'depth8RepeatedReportMs':report,'operations':dict((f'{k[0]}/{k[1]}',v)for k,v in totals.items())}))
