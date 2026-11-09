from pathlib import Path
import xml.etree.ElementTree as ET,json,subprocess
r=Path.cwd()
expected={('fraymus.workspace.Payload057Test','explicitChatPayloadRequestCannotBeDropped'),('fraymus.workspace.Payload058PolicyTest','explicitRedesignAndUnfamiliarPayloadRequestPreservesRequirements'),('fraymus.workspace.Support059Test','nativeOptionsAreExplicitAndDisabledRedesignRefusesBothOffsets'),('fraymus.workspace.Support060Test','exactIndependentPolicyAndChatCannotBecomeAsymmetric')}
failures=set();tests=0
for p in (r/'build/full096-results').rglob('TEST-*.xml'):
 s=ET.parse(p).getroot()
 for c in s.findall('testcase'):
  tests+=1
  if c.find('failure') is not None or c.find('error') is not None:failures.add((s.attrib['name'],c.attrib['name']))
assert tests>735 and failures==expected,(tests,failures)
assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()=='06d6d33ad4b46349603a053d4078494cd93ccb4a'
assert not subprocess.check_output(['git','diff','--name-only','--','workspace-engine','build.gradle'],text=True).strip()
print(json.dumps({'originalMethods':tests,'originalFailures':sorted(failures),'safeToApplyDeclaredTestOnlyCorrection':True}))
