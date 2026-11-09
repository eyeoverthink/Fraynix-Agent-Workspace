from pathlib import Path
import json,hashlib
r=Path.cwd();changes=[]
p=r/'workspace-engine/src/test/java/fraymus/workspace/Payload057Test.java';s=p.read_text(encoding='utf8');original=s
old='var args=Domain044Test.domain(store).prepare(task,"construction.start",new JSONObject().put("payloadTests057",false));'
new='var proposal=new JSONObject().put("payloadTests057",false);ToolSchema043.validate(Tools044.schema("construction.start",task),proposal);var args=Domain044Test.domain(store).prepare(task,"construction.start",proposal);'
assert old in s;s=s.replace(old,new)
old='ToolSchema043.validate(Tools044.schema("construction.start",task),args);'
new='ToolSchema043.validate(Tools044.definition("construction.start").getJSONObject("function").getJSONObject("parameters"),args);assertThrows(IllegalArgumentException.class,()->ToolSchema043.validate(Tools044.schema("construction.start",task),args));'
assert old in s;s=s.replace(old,new)
# The whole native experiment method, its checks and all remaining tests stay byte-for-byte equal after LF normalization.
cut=' @Test public void explicitChatPayloadRequestCannotBeDropped()'
assert original.split(cut)[0]==s.split(cut)[0]
cut2=' @Test public void payloadOnlyChatCannotInventUnrequestedRedesignSettings()'
assert original.split(cut2)[1]==s.split(cut2)[1]
changes.append({'path':p.relative_to(r).as_posix(),'unchangedNativePrefixSha256':hashlib.sha256(s.split(cut)[0].encode()).hexdigest(),'oldMethod':original.split(cut)[1].split(cut2)[0],'newMethod':s.split(cut)[1].split(cut2)[0]});p.write_text(s,encoding='utf8')
p=r/'workspace-engine/src/test/java/fraymus/workspace/Payload058PolicyTest.java';s=p.read_text(encoding='utf8');original=s
old='var args=Domain044Test.domain(store).prepare(task,"construction.start",new JSONObject().put("payloadDesign058",false));'
new='var proposal=new JSONObject().put("payloadDesign058",false);\n            ToolSchema043.validate(Tools044.schema("construction.start",task),proposal);\n            var args=Domain044Test.domain(store).prepare(task,"construction.start",proposal);'
assert old in s;s=s.replace(old,new)
old='ToolSchema043.validate(Tools044.schema("construction.start",task),args);'
new='ToolSchema043.validate(Tools044.definition("construction.start").getJSONObject("function").getJSONObject("parameters"),args);\n            assertThrows(IllegalArgumentException.class,()->ToolSchema043.validate(Tools044.schema("construction.start",task),args));'
assert old in s;s=s.replace(old,new)
cut='    @Test public void conflictingProtocolsAreRejectedBeforeAnyNativeWork()';assert original.split(cut)[1]==s.split(cut)[1]
changes.append({'path':p.relative_to(r).as_posix(),'unchangedOtherChecksSha256':hashlib.sha256(s.split(cut)[1].encode()).hexdigest(),'change':'Validate the original model proposal first, then Java-enriched arguments against the unchanged full typed catalog; explicitly refuse enriched flags at model-input boundary. All original policy, conflicting-protocol, invalid-type and projection assertions retained.'});p.write_text(s,encoding='utf8')
for name,oldProposal,newProposal in [
 ('Support059Test.java','var args=Domain044Test.domain(artifacts).prepare(task,"construction.start",new JSONObject().put("asymmetricSupports059",false));','var proposal=new JSONObject().put("asymmetricSupports059",false);ToolSchema043.validate(Tools044.schema("construction.start",task),proposal);var args=Domain044Test.domain(artifacts).prepare(task,"construction.start",proposal);'),
 ('Support060Test.java','var args=Domain044Test.domain(artifacts).prepare(task,"construction.start",new JSONObject());','var proposal=new JSONObject();ToolSchema043.validate(Tools044.schema("construction.start",task),proposal);var args=Domain044Test.domain(artifacts).prepare(task,"construction.start",proposal);')]:
 p=r/'workspace-engine/src/test/java/fraymus/workspace'/name;original=p.read_text(encoding='utf8');assert oldProposal in original
 oldValidation='ToolSchema043.validate(Tools044.schema("construction.start",task),args);'
 newValidation='ToolSchema043.validate(Tools044.definition("construction.start").getJSONObject("function").getJSONObject("parameters"),args);assertThrows(IllegalArgumentException.class,()->ToolSchema043.validate(Tools044.schema("construction.start",task),args));'
 assert oldValidation in original
 changed=original.replace(oldProposal,newProposal).replace(oldValidation,newValidation)
 assert changed.replace(newProposal,oldProposal).replace(newValidation,oldValidation)==original
 changes.append({'path':p.relative_to(r).as_posix(),'originalSourceSha256':hashlib.sha256(original.encode()).hexdigest(),'unchangedOutsideDeclaredReplacements':True,'replacements':[{'old':oldProposal,'new':newProposal},{'old':oldValidation,'new':newValidation}],'scope':'Only chat command validation stages change. All geometry, force, rejected candidates, policy and reserved-protocol assertions are unchanged.'});p.write_text(changed,encoding='utf8')
(r/'build/payload-boundary096-correction.json').write_text(json.dumps(changes,indent=2)+'\n',encoding='utf8');print('Corrected only four test methods; all payload native/outcome assertions preserved.')
