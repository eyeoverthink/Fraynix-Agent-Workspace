import * as THREE from '../node_modules/three/build/three.module.js';
const $=id=>document.getElementById(id);
export class Workshop050 {
 constructor({call,goal,requireSaved}){
  Object.assign(this,{call,goal,requireSaved});this.current=null;this.trial=null;this.meshes=new Map();this.playing=false;this.last='';
  const panel=document.createElement('details');panel.id='workshop050';
  panel.innerHTML=`<summary>Construction workshop · build, test & remember</summary>
   <p>Java assembles a ramp and collection bin from finite stock, releases three native blocks, and repairs failed designs. Each trial is a disposable simulation world.</p>
   <div class="workshop050-fields"><label>Surface friction multiplier<input id="w050-friction" type="number" value="1" min="0.5" max="1.8" step="0.1"></label><label>Starting offset · m<input id="w050-offset" type="number" value="0" min="-.35" max=".35" step=".05"></label><label>Save successful procedure<input id="w050-name" value="ramp-delivery" maxlength="56"></label></div>
   <div class="runtime-controls"><button id="w050-start">Build & test two surfaces</button><button id="w050-reuse" disabled>Retest saved procedure</button><button id="w050-stop" disabled>Stop</button><button id="w050-resume" disabled>Resume interrupted task</button></div>
   <label>Retained experiment<select id="w050-history"><option value="">Choose an experiment</option></select></label>
   <p id="w050-status" role="status">Stock: 1 ramp · 3 blocks · 3 bin walls. Open to restore experiment history.</p>
   <div id="w050-inventory"></div><label>Trial<select id="w050-trials"><option value="">No completed trial</option></select></label>
   <div class="runtime-controls"><button id="w050-play" disabled>Play native replay</button><button id="w050-camera">Top view</button><button id="w050-export" disabled>Export original trial JSON</button></div>
   <div id="w050-viewport" aria-label="Three dimensional native construction trial"></div>
   <label>Recorded simulation time<input id="w050-seek" type="range" min="0" max="120" step="1" value="0" disabled></label><p id="w050-time"></p>
   <svg id="w050-chart" viewBox="0 0 700 180" role="img" aria-label="Predicted and observed ramp displacement in metres over time"></svg>
   <p class="w050-legend">Dashed amber: analytic ramp prediction · solid teal: native block 1 motion while on the ramp. Landing and bin collisions are outside the prediction model.</p>
   <div id="w050-results"></div><details><summary>Decisions and verified outcomes</summary><ol id="w050-log"></ol></details>`;
  $('spatial-view').prepend(panel);
  const safe=fn=>async(...args)=>{try{await fn(...args);}catch(e){$('w050-status').textContent=e.message;}};
  this.safe=safe;
  panel.ontoggle=safe(async()=>{if(panel.open){this.initView();await this.refresh();this.resize();}else this.pause();});
  $('w050-start').onclick=safe(()=>this.begin('start',{config:this.config(),saveSkillName:$('w050-name').value.trim()}));
  $('w050-reuse').onclick=safe(()=>{const s=this.current.savedSkill;return this.begin('reuse',{name:s.name,version:s.version,config:this.config(),saveSkillName:$('w050-name').value.trim()});});
  $('w050-stop').onclick=safe(async()=>this.show(await call('stop',{id:this.current.id})));
  $('w050-resume').onclick=safe(async()=>{this.show(await call('resume',{id:this.current.id}));this.goal(this.current);});
  $('w050-history').onchange=safe(async()=>{this.pause();this.last='';this.trial=null;if($('w050-history').value){this.show(await call('read',{id:$('w050-history').value}));await this.chooseLast();}});
  $('w050-trials').onchange=safe(()=>this.loadTrial($('w050-trials').value));
  $('w050-export').onclick=safe(async()=>{$('w050-status').textContent='Exported '+(await call('export',{id:this.trial.id})).path;});
  $('w050-seek').oninput=()=>{this.pause();this.frame(Number($('w050-seek').value));};
  $('w050-play').onclick=()=>{if(this.playing){this.pause();return;}if(!this.trial)return;this.playing=true;$('w050-play').textContent='Pause replay';const from=Number($('w050-seek').value)>=120?0:Number($('w050-seek').value),start=performance.now();const step=()=>{if(!this.playing)return;const i=Math.min(120,Math.floor(from+(performance.now()-start)/100));this.frame(i);if(i===120)this.pause();else this.raf=requestAnimationFrame(step);};step();};
  $('w050-camera').onclick=()=>{this.top=!this.top;$('w050-camera').textContent=this.top?'Perspective view':'Top view';this.positionCamera();this.draw();};
  document.addEventListener('workshop050-procedure',safe(async event=>{const skill=event.detail;$('spatial-tab').click();panel.open=true;this.initView();this.show(await call('read',{id:skill.sourceGoalId}));this.current.savedSkill={name:skill.name,version:skill.version};this.show(this.current);await this.refreshList();await this.chooseLast();panel.scrollIntoView({block:'start'});}));
  document.addEventListener('visibilitychange',()=>{if(document.hidden)this.pause();});
  new ResizeObserver(()=>this.resize()).observe($('w050-viewport'));
  this.poll=setInterval(async()=>{if(this.polling||!panel.open||document.hidden||$('spatial-view').hidden)return;this.polling=true;try{if(this.current&&['running','waiting-host'].includes(this.current.status)){this.show(await call('read',{id:this.current.id}));await this.chooseLast();}}catch(e){$('w050-status').textContent=e.message;}finally{this.polling=false;}},450);
 }
 config(){return {frictionScale:Number($('w050-friction').value),startShiftM:Number($('w050-offset').value)};}
 async begin(command,args){this.requireSaved();if(this.starting)return;this.starting=true;$('w050-status').dataset.status='starting';$('w050-start').disabled=$('w050-reuse').disabled=true;try{const g=await this.call(command,args);this.pause();this.last='';this.show(g);this.goal(g);await this.refreshList();}finally{this.starting=false;if(this.current)this.show(this.current);else $('w050-start').disabled=false;}}
 async refreshList(){const data=await this.call('list');const list=data.goals.filter(g=>g.procedure==='construction-workshop-050-v1');$('w050-history').replaceChildren(new Option('Choose an experiment',''),...list.map(g=>new Option(`${new Date(g.createdAt).toLocaleString()} · ${g.status}`,g.id)));if(this.current)$('w050-history').value=this.current.id;return list;}
 async refresh(){const list=await this.refreshList();if(!this.current&&list.length)this.show(await this.call('read',{id:list.at(-1).id}));await this.chooseLast();}
 show(g){this.current=g;const option=[...$('w050-history').options].find(o=>o.value===g.id);if(option)option.textContent=`${new Date(g.createdAt).toLocaleString()} � ${g.status}`;const w=g.workshop050;if(!w)return;const active=['running','waiting-host'].includes(g.status);$('w050-status').textContent=`${g.status.toUpperCase()} · ${g.reason}`;$('w050-status').dataset.status=g.status;$('w050-start').disabled=active;$('w050-stop').disabled=!active;$('w050-resume').disabled=g.status!=='interrupted';$('w050-reuse').disabled=active||!g.savedSkill;
  $('w050-inventory').textContent=`Available stock: ${w.stock.ramp} ramp, ${w.stock.blocks} blocks, ${w.stock.walls} walls. ${w.pending&&active?'Reserved for the next native trial: 1 ramp, 3 blocks, 3 walls.':'All candidate parts returned.'} ${w.trials.length} completed trial(s). ${g.savedSkill?`Retained ${g.savedSkill.name} v${g.savedSkill.version}.`:''}`;
  const previous=$('w050-trials').value;$('w050-trials').replaceChildren(...w.trials.map(t=>new Option(`${t.design.material} · ${t.design.angleDeg.toFixed(1)}° · ${t.verification.delivered}/3 delivered${w.selected?.id===t.id?' · selected':''}`,t.id)));if(w.trials.some(t=>t.id===previous))$('w050-trials').value=previous;
  $('w050-results').replaceChildren(...w.trials.map(t=>{const p=document.createElement('p');p.textContent=`${t.design.material}, ${t.design.angleDeg.toFixed(1)}°: ${t.verification.delivered}/3 settled · ramp prediction RMSE ${t.verification.rampRmseM?.toFixed(4)??'unavailable'} m · parts returned`;return p;}));
  $('w050-log').replaceChildren(...g.history.map(e=>{const li=document.createElement('li');li.textContent=e.reason;return li;}));
 }
 async chooseLast(){const trials=this.current?.workshop050?.trials;if(!trials?.length)return;const id=trials.at(-1).id;if(id!==this.last){this.last=id;await this.loadTrial(id);}}
 async loadTrial(id){if(!id)return;this.pause();this.trial=await this.call('trial',{id});$('w050-trials').value=id;this.initView();for(const mesh of this.meshes.values()){this.scene.remove(mesh);mesh.geometry.dispose();mesh.material.dispose();}this.meshes.clear();
  for(const o of this.trial.objects){const material=new THREE.MeshStandardMaterial({color:o.id.startsWith('block')?0x5ed9c2:o.id==='ramp'?(this.trial.specification.design.material==='smooth'?0x83a8df:0xc28f64):o.id==='ground'?0x20353b:0x566a79,roughness:.8});const mesh=new THREE.Mesh(new THREE.BoxGeometry(...o.size),material);mesh.position.fromArray(o.position);mesh.quaternion.fromArray(o.rotation);this.scene.add(mesh);this.meshes.set(o.id,mesh);}
  $('w050-play').disabled=$('w050-seek').disabled=$('w050-export').disabled=false;this.frame(0);this.chart();
 }
 initView(){if(this.renderer)return;this.scene=new THREE.Scene();this.scene.background=new THREE.Color('#102229');this.scene.add(new THREE.HemisphereLight(0xffffff,0x304050,2));const light=new THREE.DirectionalLight(0xffffff,2);light.position.set(-3,9,5);this.scene.add(light);this.camera=new THREE.PerspectiveCamera(45,1,.1,100);this.renderer=new THREE.WebGLRenderer({antialias:true});this.renderer.setPixelRatio(Math.min(devicePixelRatio,1.5));$('w050-viewport').append(this.renderer.domElement);this.positionCamera();this.resize();}
 positionCamera(){if(!this.camera)return;this.camera.position.set(...(this.top?[1,13,.01]:[9,7,10]));this.camera.up.set(0,1,0);this.camera.lookAt(1,1,0);}
 resize(){if(!this.renderer)return;const b=$('w050-viewport').getBoundingClientRect();if(b.width<1)return;this.renderer.setSize(b.width,330);this.camera.aspect=b.width/330;this.camera.updateProjectionMatrix();this.draw();}
 draw(){if(this.renderer)this.renderer.render(this.scene,this.camera);}
 frame(i){if(!this.trial)return;const s=this.trial.samples[i];for(const b of s.bodies){const mesh=this.meshes.get(b.id);mesh.position.fromArray(b.position);mesh.quaternion.fromArray(b.rotation);}this.draw();$('w050-seek').value=i;$('w050-time').textContent=`Native replay ${s.timeS.toFixed(2)} / 12.00 s · ${s.bodies.reduce((n,b)=>n+b.contacts,0)} contact points · no motion computed by the display`;}
 pause(){this.playing=false;cancelAnimationFrame(this.raf);$('w050-play').textContent='Play native replay';}
 chart(){const r=this.trial,p=r.specification.prediction,a=r.specification.design.angleDeg*Math.PI/180,start=r.samples[0].bodies[0].position;const svg=$('w050-chart');svg.replaceChildren();const add=(name,attrs,text)=>{const e=document.createElementNS('http://www.w3.org/2000/svg',name);for(const[k,v]of Object.entries(attrs))e.setAttribute(k,v);if(text)e.textContent=text;svg.append(e);};const x=t=>45+t/12*630,y=d=>145-d/5.2*125;add('path',{d:'M45 12 V145 H680',stroke:'#78929a',fill:'none'});add('text',{x:50,y:18,fill:'#d6e7eb','font-size':12},'Ramp displacement · metres');add('text',{x:570,y:173,fill:'#d6e7eb','font-size':12},'Time · 0–12 s');
  const points=[];for(const s of r.samples){const v=s.bodies[0].position,d=(v[0]-start[0])*Math.cos(a)-(v[1]-start[1])*Math.sin(a);if(d>p.rampTravelM)break;points.push(`${x(s.timeS)},${y(d)}`);}add('polyline',{points:points.join(' '),fill:'none',stroke:'#5ed9c2','stroke-width':2});const expected=[];for(const s of r.samples){const d=.5*p.accelerationMps2*s.timeS*s.timeS;if(d>p.rampTravelM)break;expected.push(`${x(s.timeS)},${y(d)}`);}add('polyline',{points:expected.join(' '),fill:'none',stroke:'#f4bb62','stroke-width':2,'stroke-dasharray':'6 4'});
 }
}
