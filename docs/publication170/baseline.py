"""Compare the prior native clearance records against the same two live courses."""
from pathlib import Path
import json
import subprocess

root=Path.cwd()
prior='20399ecafab8d1355413ec005b5dc7d1bc3b1164'
old=json.loads(subprocess.check_output(['git','show',prior+':docs/readmes/169/verification.json']))
current=json.loads((root/'workspace-engine/build/rail-navigation/result.json').read_bytes())
assert current['passed']
receipts=[]
for previous, trial in zip(old['courses'],current['trials'],strict=True):
    assert previous['id']==trial['id']
    final=trial['final']
    observed=final['railNavigation']['crossings'][0]
    expected=previous['boundedCrossing']
    for field,value in expected.items():
        assert observed[field]==value,(trial['id'],field,observed[field],value)
    assert final['tick']==previous['boundedFinishObservedTick']
    assert final['player']['won'] and not final['courseProgress'].get('hazard')
    receipts.append({'id':trial['id'],'previousWitness':expected,'currentWitness':observed,'finishTick':final['tick']})
result={'passed':True,'existingRailWitnessesMatched':True,'beginningCommit':prior,'courses':receipts}
(root/'build/baseline170.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
