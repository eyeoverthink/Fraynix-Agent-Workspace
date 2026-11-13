from pathlib import Path
r=Path.cwd();base=r/'workspace-engine/src/main/java/fraymus/workspace'
p=base/'ForegroundGuard139.java';s=(base/'DriftGuard138.java').read_text(encoding='utf8').replace('DriftGuard138','ForegroundGuard139').replace('drift138','drift139').replace('drift-guard-trace-138','foreground-guard-trace-139').replace('immutable-source-recovery-138','immutable-source-recovery-139')
s=s.replace('import org.json.*;','import org.json.*;\nimport java.util.*;')
s=s.replace('private FlowTracker flow;','private FlowTracker flow;\n    private final SpatialEvidence139 spatial;\n    private final Map<String,Integer> streaks=new TreeMap<>();')
s=s.replace('anchor=new AppearanceEvidence138(first,seed);','spatial=new SpatialEvidence139(first,seed);anchor=new AppearanceEvidence138(first,seed);')
s=s.replace('boolean suspect=flowAccepted&&!reasons.isEmpty(),wasQuarantined=quarantined;', '''var structure=spatial.assess(image,flowAccepted?PixelTracker.Box.from(proposal.getJSONArray("box")):null);
        var present=new HashSet<String>();for(Object raw:structure.getJSONArray("candidateReasons")){String reason=(String)raw;present.add(reason);int count=streaks.getOrDefault(reason,0)+1;streaks.put(reason,count);if(count>=2)reasons.put(reason);}
        streaks.keySet().removeIf(k->!present.contains(k));structure.put("consecutiveEvidence",new JSONObject(streaks));
        boolean suspect=flowAccepted&&!reasons.isEmpty(),wasQuarantined=quarantined;''')
s=s.replace('AppearanceEvidence138.reasons(recoveredEvidence).isEmpty();','AppearanceEvidence138.reasons(recoveredEvidence).isEmpty()&&spatial.supports(small,found.box());')
s=s.replace('.put("suspect",suspect)', '.put("structure",structure).put("suspect",suspect)')
p.write_text(s,encoding='utf8')
p=base/'SpatialEvidence139.java';s=p.read_text(encoding='utf8');s=s.replace('    JSONObject assess(','''    boolean supports(BufferedImage small,PixelTracker.Box box){
        int eligible=0,matched=0;var rows=new HashSet<Integer>();var cols=new HashSet<Integer>();
        for(var part:parts)if(part.eligible){eligible++;double q=score(part.patch,patch(small,box.x()+part.u*box.w(),box.y()+part.v*box.h(),box.w()*.22,box.h()*.22));if(q>=.55){matched++;rows.add(part.index/3);cols.add(part.index%3);}}
        return eligible<4||matched>=3&&rows.size()>=2&&cols.size()>=2;
    }
    JSONObject assess(''');p.write_text(s,encoding='utf8')
p=base/'ForegroundAudit139.java';s=(base/'DriftAudit138.java').read_text(encoding='utf8').replace('DriftAudit138','ForegroundAudit139').replace('DriftGuard138','ForegroundGuard139').replace('drift-guard-138','foreground-guard-139').replace('monitor-only-138','monitor-only-139').replace('drift138','drift139').replace('drift-audit-138','foreground-audit-139');p.write_text(s,encoding='utf8')
def edit(name,old,new):
 p=r/name;s=p.read_text(encoding='utf8');assert old in s,name;p.write_text(s.replace(old,new),encoding='utf8')
method='workspace-engine/src/main/java/fraymus/workspace/ObservationMethods129.java'
edit(method,'Set.of(DEFAULT,CANDIDATE,','Set.of(DEFAULT,CANDIDATE,"foreground-guard-139","monitor-only-139",')
edit(method,'return switch(policy){','return switch(policy){case "foreground-guard-139"->new ForegroundGuard139(id,label,first,box,true);case "monitor-only-139"->new ForegroundGuard139(id,label,first,box,false);')
edit(method,'List.of(ObservationMethods129.class,','List.of(ObservationMethods129.class,SpatialEvidence139.class,ForegroundGuard139.class,ForegroundAudit139.class,')
audit='workspace-engine/src/main/java/fraymus/workspace/AssociationAudit134.java'
edit(audit,'var drift=DriftAudit138.class.getResourceAsStream("DriftAudit138.class")','var drift=DriftAudit138.class.getResourceAsStream("DriftAudit138.class");var foreground=ForegroundAudit139.class.getResourceAsStream("ForegroundAudit139.class")')
edit(audit,'PerceptionStore.hash(drift.readAllBytes()))','PerceptionStore.hash(drift.readAllBytes())+PerceptionStore.hash(foreground.readAllBytes()))')
s=(r/audit).read_text(encoding='utf8');begin=s.index('        if(DriftAudit138.uses(');end=s.index('        return result.put("input"',begin);block=s[begin:end].replace('DriftAudit138','ForegroundAudit139').replace('"drift138"','"drift139"');s=s[:end]+block+s[end:];(r/audit).write_text(s,encoding='utf8')
ui='workspace-desktop/renderer/observation-panel.js'
edit(ui,'<option value="drift-guard-138">','<option value="foreground-guard-139">Experimental 139 · foreground motion &amp; structure</option><option value="monitor-only-139">Control 139 · monitor without changing flow</option><option value="drift-guard-138">')
edit(ui,'const suspect=row.tracker.drift138;','const suspect=row.tracker.drift139||row.tracker.drift138;for(const p of row.tracker.drift139?.structure?.parts||[]){c.fillStyle=p.unique?"#dfb6ff":"#9c7d70";c.beginPath();c.arc(p.matched[0]*canvas.width,p.matched[1]*canvas.height,3,0,Math.PI*2);c.fill();}')
edit(ui,"(o.tracker.drift138?.suspect?' · '+o.tracker.drift138.reasons.join('; '):'')","((o.tracker.drift139||o.tracker.drift138)?.suspect?' · '+(o.tracker.drift139||o.tracker.drift138).reasons.join('; '):'')")
ui='workspace-desktop/renderer/association134-panel.js';s=(r/ui).read_text(encoding='utf8');begin=s.index('if(a.drift138)');end=s.index('if(a.appearance135',begin);block=s[begin:end].replace('drift138','drift139').replace('Drift monitor:','Foreground/structure monitor:');s=s[:end]+block+s[end:];(r/ui).write_text(s,encoding='utf8')
