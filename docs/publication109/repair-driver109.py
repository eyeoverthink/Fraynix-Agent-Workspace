from pathlib import Path
r=Path.cwd();p=r/'workspace-engine/src/test/java/fraymus/workspace/Qualification105Test.java';s=p.read_text(encoding='utf-8')
a='            var audit=f.workflows.claim();if(audit.has("id")){'
b='''            JSONObject audit;
            synchronized(f.workflows){
                // Stop and claim share the runner lock: never acknowledge the boundary being inspected.
                var current=f.workflows.read(id);
                if(stop.test(current)||!current.getString("status").equals("running"))return current;
                audit=f.workflows.claim();
            }
            if(audit.has("id")){'''
assert a in s;p.write_text(s.replace(a,b),encoding='utf-8',newline='\n')
p=r/'workspace-engine/src/test/java/fraymus/workspace/Qualification108Test.java';s=p.read_text(encoding='utf-8')
a='assertEquals("running",w.getString("status"));reviseSource(f);'
b='assertEquals("running",w.getString("status"));assertEquals(5,w.getJSONObject("qualification105").getInt("checked"));assertEquals("audit",w.getJSONObject("activeWorkflow105").getString("step"));reviseSource(f);'
assert a in s;p.write_text(s.replace(a,b),encoding='utf-8',newline='\n')
