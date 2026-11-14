from pathlib import Path
import json,hashlib
r=Path.cwd();manifest=json.loads((r/'docs/publication146/source146.json').read_text(encoding='utf8'));rows=[]
lock=json.loads((r/'docs/OBSERVATION_146_LOCK.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,n
for group in sorted({x['group'] for x in manifest['records']}):
 study=json.loads((r/'build'/('return146-'+group+'-app')/'result.json').read_text(encoding='utf8'));assert study['passed']
 for row in study['runs']:
  if row['policy']!='return-guard-146':continue
  anchor=0;blocked=[];accepted=[];checked=0
  parent=next(x for x in study['runs'] if x['caseName']==row['caseName'] and x['policy']=='trajectory-guard-145')
  for ordinal,frame in enumerate(row['run']['trace'],1):
   t=frame['objects'][0]['tracker'];g=t['continuity146'];path=t['trajectory145']['target'];base=t['references143']['baselineAccepted'];before=parent['run']['trace'][ordinal-1]['objects'][0]['tracker']
   if base:anchor=ordinal
   if 'anchorOrdinal' in path:assert path['anchorOrdinal']==anchor,(group,row['caseName'],ordinal)
   assert g['automaticAdmissions']==g['extraSearches']==0
   if g['checkedWithoutWarning']:checked+=1;assert not base and not t['ambiguity144']['latched'] and before['trajectory145']['accepted']
   if g['blocked']:
    assert g['checkedWithoutWarning'] and g['wouldBlock'] and not t.get('box');blocked.append(ordinal)
   else:
    reduced={k:v for k,v in t.items() if k!='continuity146'};assert reduced==before
   if g['accepted'] and g['checkedWithoutWarning']:assert path['valid'] and t['trajectory145']['targetCompatible'] and t['trajectory145']['scaleCompatible'];accepted.append(ordinal)
  evidence=row['audit']['continuity146'];assert len(blocked)==evidence['screened']['returned']
  rows.append({'group':group,'case':row['caseName'],'domain':row['domain'],'checkedWithoutWarning':checked,'newlyBlockedOrdinals':blocked,'supportedReturnOrdinals':accepted,'screenedOutcomes':evidence['screened'],'filteredOutcomes':evidence['filtered'],'scope':'Every unchanged parent proposal checked; restored/predicted boxes never extend the original139 target anchor.'})
(r/'build/diagnostics146.json').write_text(json.dumps({'passed':True,'frozenInputs':len(lock['sourceHashes']),'rows':rows},indent=2)+'\n',encoding='utf8');print(json.dumps({'passed':True,'cases':len(rows)}))
