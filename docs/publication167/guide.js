'use strict';
const data=JSON.parse(document.getElementById('system-data').textContent);
const inventory=JSON.parse(document.getElementById('inventory-data').textContent);
const $=id=>document.getElementById(id);
$('validation').textContent=data.validation;
for(const item of data.modules)$('module').add(new Option(item.name,item.id));
for(const shot of data.screenshots)$('shot').add(new Option(shot.title,shot.path));
function modules(){const item=data.modules.find(m=>m.id===$('module').value);$('module-detail').textContent=item.description+' ';const a=document.createElement('a');a.href='../../../'+item.source;a.textContent=item.source;$('module-detail').append(a);}
function screenshots(){const shot=data.screenshots.find(s=>s.path===$('shot').value);$('screenshot').src='../../../'+shot.path;$('caption').textContent=shot.title;}
function rows(){ $('rows').replaceChildren();for(const row of data.checks.filter(r=>$('kind').value==='all'||r.kind===$('kind').value)){const tr=document.createElement('tr');for(const text of [row.name,row.result,row.evidence]){const td=document.createElement('td');td.textContent=text;tr.append(td);}$('rows').append(tr);}}
function files(){const matches=inventory.filter(f=>f.path.toLowerCase().includes($('search').value.toLowerCase()));$('count').textContent=matches.length+' matching files; showing at most 60.';$('files').replaceChildren();for(const file of matches.slice(0,60)){const li=document.createElement('li'),a=document.createElement('a');a.href='../../../'+file.path;a.textContent=file.path;li.append(a);$('files').append(li);}}
function motion(){const index=Number($('tick').value),p=data.trace[index];$('position').textContent='Tick '+p.tick+' · X '+p.x.toFixed(3)+' · Y '+p.y.toFixed(3)+' · '+(p.won?'finish contact':p.onGround?'on ground':'airborne')+' · legacy game units';const ns='http://www.w3.org/2000/svg';$('plot').replaceChildren();const line=document.createElementNS(ns,'polyline');line.setAttribute('points',data.trace.map(v=>(25+v.x*50)+','+(150-v.y*65)).join(' '));line.setAttribute('fill','none');line.setAttribute('stroke','#8be5cd');line.setAttribute('stroke-width','3');$('plot').append(line);const dot=document.createElementNS(ns,'circle');dot.setAttribute('cx',25+p.x*50);dot.setAttribute('cy',150-p.y*65);dot.setAttribute('r','6');dot.setAttribute('fill','#e7b76b');$('plot').append(dot);}
$('tick').max=data.trace.length-1;$('tick').oninput=motion;
$('module').onchange=modules;$('shot').onchange=screenshots;$('kind').onchange=rows;$('search').oninput=files;
modules();screenshots();rows();files();motion();
