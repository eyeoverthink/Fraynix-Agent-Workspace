/** Presentation only. Java remains the source of task state and verified outcomes. */
export class Workspace099 {
  constructor(request, relayout) {
    this.request=request; this.relayout=relayout; this.unread=0;
    this.layout=document.querySelector('.layout'); this.agent=document.querySelector('.agent-panel');
    this.messages=document.querySelector('#messages'); this.timeline=document.querySelector('.timeline');
    this.preferences=this.read('fraynix.layout099',{});
    this.width=Number.isFinite(this.preferences.width)?this.preferences.width:430;
    this.timeline.classList.toggle('activity-collapsed099',this.preferences.collapsed!==false);
    this.settings=document.createElement('details'); this.settings.id='settings099';
    this.settings.innerHTML='<summary>Skills, memory &amp; settings <small id="memory099">Loading saved state</small></summary><div class="settings099-body"><p>Saved skills are checked procedures. Task episodes and controller versions are separate records.</p><label title="Optional model planning before a verified world procedure. Java still validates every action and outcome."><span>Model planning for world procedures</span><input id="plan-world099" type="checkbox"></label><label>Conversation width <input id="width099" type="range" min="320" max="720" step="10" aria-label="Conversation width"></label><button id="layout-reset099">Reset layout</button></div>';
    document.querySelector('#model').after(this.settings);
    this.settings.querySelector('#plan-world099').checked=this.preferences.planWorld===true;
    this.settings.querySelector('#plan-world099').onchange=()=>this.persist();
    for(const element of [document.querySelector('.learning-strip'),document.querySelector('#agent042-panel')]) if(element)this.settings.lastElementChild.append(element);
    this.settings.querySelector('#width099').oninput=e=>this.setWidth(Number(e.target.value),true);
    this.settings.querySelector('#layout-reset099').onclick=()=>{this.width=430;this.timeline.classList.add('activity-collapsed099');this.setWidth(430,true);this.activityLabel();};
    this.gutter=document.createElement('div'); this.gutter.id='chat-resize099'; this.gutter.tabIndex=0;
    this.gutter.setAttribute('role','separator'); this.gutter.setAttribute('aria-orientation','vertical');
    this.gutter.setAttribute('aria-label','Resize conversation. Left arrow widens; right arrow narrows.');
    this.gutter.setAttribute('aria-controls','messages'); this.gutter.title='Drag to resize chat · arrow keys adjust · double-click resets'; this.agent.before(this.gutter);
    this.gutter.onpointerdown=e=>{if(e.button!==0)return;e.preventDefault();this.drag={x:e.clientX,width:this.applied};this.gutter.setPointerCapture(e.pointerId);this.layout.classList.add('resizing099');};
    this.gutter.onpointermove=e=>{if(this.drag)this.setWidth(this.drag.width+this.drag.x-e.clientX,false);};
    const finish=()=>{if(!this.drag)return;this.drag=null;this.layout.classList.remove('resizing099');this.width=this.applied;this.persist();};
    this.gutter.onpointerup=finish;this.gutter.onpointercancel=finish;this.gutter.onlostpointercapture=finish;
    this.gutter.ondblclick=()=>this.setWidth(430,true);
    this.gutter.onkeydown=e=>{const change={ArrowLeft:30,ArrowRight:-30,Home:-10000,End:10000}[e.key];if(change!==undefined){e.preventDefault();this.setWidth(this.applied+change,true);}};
    this.activityButton=document.createElement('button');this.activityButton.id='activity-toggle099';this.activityButton.className='quiet';
    this.activityButton.onclick=()=>{if(this.timeline.classList.contains('mission-open044'))document.querySelector('#mission044-activity').click();this.timeline.classList.toggle('activity-collapsed099');this.persist();this.activityLabel();this.relayout();};
    this.timeline.querySelector('.timeline-head').prepend(this.activityButton);this.activityLabel();
    const files=document.createElement('button');files.id='files099';files.textContent='Browser & files';files.onclick=()=>document.querySelector('#expand-browser').click();document.querySelector('.header-actions').prepend(files);
    this.card=document.createElement('section');this.card.id='task099';this.card.hidden=true;
    this.card.innerHTML='<div class="task099-heading"><strong id="task099-phase"></strong><button id="task099-evidence" class="quiet">Evidence &amp; recovery</button></div><p id="task099-request"></p><p id="task099-progress" role="status"></p><p id="task099-result"></p><button id="task099-open" hidden>Open verified result</button>';
    this.messages.before(this.card);
    this.card.querySelector('#task099-evidence').onclick=()=>document.querySelector('#mission044-open').click();
    this.card.querySelector('#task099-open').onclick=async()=>{try{await this.request('agency043',{command:'mission-open',args:{id:this.task.id}});}catch(e){document.querySelector('#notice').textContent=e.message;}};
    this.latest=document.createElement('button');this.latest.id='latest099';this.latest.hidden=true;this.latest.className='quiet';this.messages.after(this.latest);
    this.latest.onclick=()=>this.toLatest();this.messages.onscroll=()=>{if(this.nearEnd()){this.unread=0;this.latest.hidden=true;}};
    const saved=this.read('fraynix.chat099',[]);this.transcript=[];
    if(Array.isArray(saved)&&saved.length&&saved.length<=40&&saved.every(v=>['user','assistant'].includes(v.role)&&typeof v.text==='string'&&v.text.length<=100000)){
      this.messages.replaceChildren();for(const row of saved)this.message(row.text,row.role,false);this.toLatest();
    }
    document.querySelector('#goal').placeholder='Describe what you want to make, measure or change. Include the result you want checked.';
    window.addEventListener('resize',()=>this.setWidth(this.width,false));
    this.setWidth(this.width,false);
  }
  read(key,fallback){try{return JSON.parse(localStorage.getItem(key))??fallback;}catch{return fallback;}}
  persist(){try{localStorage.setItem('fraynix.layout099',JSON.stringify({width:this.width,planWorld:this.settings.querySelector("#plan-world099").checked,collapsed:this.timeline.classList.contains('activity-collapsed099')}));}catch{document.querySelector('#notice').textContent='Layout could not be saved in this profile.';}}
  activityLabel(){const open=!this.timeline.classList.contains('activity-collapsed099');this.activityButton.textContent=open?'Collapse':'Expand';this.activityButton.setAttribute('aria-expanded',String(open));this.activityButton.setAttribute('aria-label',open?'Collapse activity':'Expand activity');}
  setWidth(value,save){
    const focused=document.body.classList.contains('browser-focus')||document.body.classList.contains('canvas-focus');
    const library=(!focused&&innerWidth>1000)?document.querySelector('.left-column').getBoundingClientRect().width:0;
    const maximum=Math.max(320,Math.min(720,this.layout.clientWidth-library-360));
    this.applied=Math.round(Math.max(320,Math.min(maximum,Number.isFinite(value)?value:430)));
    this.layout.style.setProperty('--chat-width099',this.applied+'px');
    for(const [key,v]of Object.entries({'aria-valuemin':320,'aria-valuemax':Math.floor(maximum),'aria-valuenow':this.applied}))this.gutter.setAttribute(key,String(v));
    const slider=this.settings.querySelector('#width099');slider.max=Math.floor(maximum);slider.value=this.applied;
    if(save){this.width=this.applied;this.persist();}this.relayout();
  }
  nearEnd(){return this.messages.scrollHeight-this.messages.scrollTop-this.messages.clientHeight<60;}
  toLatest(){this.messages.scrollTop=this.messages.scrollHeight;this.unread=0;this.latest.hidden=true;}
  message(text,role='assistant',persist=true){
    text=String(text);const follow=role==='user'||this.nearEnd();const item=document.createElement('div');item.className='message '+role;
    const label=document.createElement('strong');label.className='message099-label';label.textContent=role==='user'?'You':'Fraynix';const body=document.createElement('div');body.textContent=text;item.append(label,body);this.messages.append(item);
    this.transcript.push({role,text});while(this.transcript.length>40||this.transcript.length>1&&this.transcript.reduce((n,r)=>n+r.text.length,0)>100000)this.transcript.shift();
    if(persist){try{const saved=this.transcript.map(r=>({...r,text:r.text.length>100000?r.text.slice(0,99900)+'\n[Saved excerpt. Full task evidence remains in Mission.]':r.text}));localStorage.setItem('fraynix.chat099',JSON.stringify(saved));}catch{document.querySelector('#notice').textContent='Conversation could not be saved in this profile; Java task records remain available.';}}
    if(follow)this.toLatest();else{this.unread++;this.latest.hidden=false;this.latest.textContent='New response ↓';}
  }
  state(value){
    const versions=(value.skills?.length||0)+(value.procedures?.length||0),episodes=value.learning?.episodeCount||0;
    document.querySelector('#memory099').textContent=`${versions} saved skill versions · ${episodes} task episodes`;
  }
  agency(state){
    const task=state.tasks?.find(t=>t.id===state.activeId)||state.tasks?.at(-1);if(!task)return;this.task=task;this.card.hidden=false;
    document.body.classList.add('has-task099');this.card.dataset.status=task.status;this.card.dataset.taskId=task.id;
    const phase={queued:'Queued',ready:'Choosing the next step',planning:'Thinking with the local model',vision:'Inspecting the view','waiting-host':'Using the workspace','waiting-native':'Running and checking',interrupted:'Needs attention',cancelled:'Stopped',completed:task.outcome==='verified'?'Verified result':'Task ended'};
    this.card.querySelector('#task099-phase').textContent=phase[task.status]||task.status;
    this.card.querySelector('#task099-request').textContent=task.goal;this.card.querySelector('#task099-request').title=task.goal;
    this.card.querySelector('#task099-progress').textContent=task.reason||'';
    this.card.querySelector('#task099-result').textContent=task.summary||'';
    this.card.querySelector('#task099-open').hidden=task.status!=='completed'||task.outcome!=='verified';
  }
}
