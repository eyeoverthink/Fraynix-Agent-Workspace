from pathlib import Path
r=Path.cwd();s=(r/'workspace-desktop/test/drift138-integration.cjs').read_text(encoding='utf8')
s=s.replace('138','139').replace('drift-guard-139','foreground-guard-139').replace('skating2-1-1','ballet-100')
s=s.replace('Twelve actual-app fresh comparisons: four fixed target windows, three methods','Twenty actual-app fresh comparisons: five fixed target windows, four methods').replace('All12 runs/audits','All20 runs/audits')
s=s.replace('drift detection and independent evidence','foreground structure and independent evidence')
s=s.replace('const m=t.drift139;','const m=t.drift139||t.drift138;')
s=s.replace('if(audit.drift139){const s=audit.drift139.summary;','if(audit.drift139||audit.drift138){const d=audit.drift139||audit.drift138;const s=d.summary;').replace('assert.equal(audit.drift139.replayedFrames,run.trace.length);','assert.equal(d.replayedFrames,run.trace.length);')
s=s.replace("if(policy==='monitor-only-139'){", "if(policy==='monitor-only-139'){")
# All actual screenshots retain the production UI. The foreground overlay and audit are separate views.
(r/'workspace-desktop/test/foreground139-integration.cjs').write_text(s,encoding='utf8')
