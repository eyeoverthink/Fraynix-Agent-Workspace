from pathlib import Path
p=Path('workspace-desktop/renderer/workspace099.js');s=p.read_text(encoding='utf8')
def change(old,new):
 global s
 assert s.count(old)==1,(old[:60],s.count(old));s=s.replace(old,new)
change('this.request=request; this.relayout=relayout; this.unread=0;','this.request=request; this.relayout=relayout; this.unread=0; this.followLatest=true; this.userScrollUntil=0;')
change("this.messages.before(this.card);", "this.messages.before(this.card);\n    const native=document.querySelector('#java-goal');this.nativeControls=document.createElement('details');this.nativeControls.id='goal-controls099';const nativeLabel=document.createElement('summary');nativeLabel.textContent='Goal controls & recovery';native.before(this.nativeControls);this.nativeControls.append(nativeLabel,native);const syncNative=()=>{this.nativeControls.hidden=native.hidden;};new MutationObserver(syncNative).observe(native,{attributes:true,attributeFilter:['hidden']});syncNative();")
change('<p id="task099-result"></p><button id="task099-open"', '<details id="task099-details"><summary>Outcome details</summary><p id="task099-result"></p></details><button id="task099-open"')
change("this.latest.onclick=()=>this.toLatest();this.messages.onscroll=()=>{if(this.nearEnd()){this.unread=0;this.latest.hidden=true;}};", "this.latest.onclick=()=>this.toLatest();\n    const intent=()=>{this.userScrollUntil=performance.now()+2000;this.followLatest=false;};\n    this.messages.addEventListener('wheel',intent,{passive:true});this.messages.addEventListener('touchstart',intent,{passive:true});this.messages.addEventListener('pointerdown',intent);this.messages.addEventListener('pointermove',e=>{if(e.buttons)intent();});this.messages.addEventListener('keydown',e=>{if(['ArrowUp','ArrowDown','PageUp','PageDown','Home','End'].includes(e.key))intent();});\n    this.messages.onscroll=()=>{if(this.nearEnd()){this.followLatest=true;this.unread=0;this.latest.hidden=true;}else if(performance.now()<this.userScrollUntil)this.followLatest=false;};\n    new ResizeObserver(()=>{if(this.followLatest)this.toLatest();}).observe(this.messages);")
change('toLatest(){this.messages.scrollTop=', 'toLatest(){this.followLatest=true;this.messages.scrollTop=')
change("text=String(text);const follow=role==='user'||this.nearEnd();", "text=String(text);const follow=role==='user'||this.nearEnd();this.followLatest=follow;")
change("this.card.querySelector('#task099-result').textContent=task.summary||'';", "this.card.querySelector('#task099-result').textContent=task.summary||'';this.card.querySelector('#task099-details').hidden=!task.summary;")
p.write_text(s,encoding='utf8')
p=Path('workspace-desktop/renderer/workspace099.css');p.write_text(p.read_text(encoding='utf8')+'''\n/* Compact actual-task chrome leaves the transcript room to breathe. */
#goal-controls099{flex-shrink:0;font-size:12px;margin:4px 0 7px}#goal-controls099>summary{cursor:pointer;color:#b7d0c6}#goal-controls099[hidden]{display:none}#goal-controls099 #java-goal{max-height:190px;margin:8px 0}
#task099{max-height:190px;padding:9px 11px}#task099-request{max-height:3.2em;margin:5px 0}#task099-progress{max-height:2.9em;overflow:auto;font-size:11px;margin:4px 0}#task099-details{font-size:11px;margin:5px 0}#task099-details>summary{cursor:pointer;color:#c6dcd4}#task099-details[hidden]{display:none}#task099-open{margin-top:5px}.task099-heading{position:sticky;top:-9px;background:#172e2b;padding:3px 0}
''',encoding='utf8')
print('Refined compact native controls, expandable outcome and pinned-chat resizing')
