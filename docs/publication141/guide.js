const data=JSON.parse(document.querySelector('#system-data').textContent),files=JSON.parse(document.querySelector('#inventory-data').textContent),$=id=>document.getElementById(id);
$('validation').textContent=data.validation;
for(const s of data.screenshots)$('shot').add(new Option(s.title,s.path));
function shot(){const s=data.screenshots.find(x=>x.path===$('shot').value);$('screenshot').src='../../../'+s.path;$('shot-caption').textContent=s.title;}
$('shot').onchange=shot;shot();
function table(){$('rows').replaceChildren();for(const row of data.comparison.rows){if($('method').value!=='all'&&row.policy!==$('method').value)continue;const s=row.summary,tr=document.createElement('tr');for(const value of [row.policy+' / '+row.case,s.correct+' / '+s.visible,s.missing,s.overlapFailures,s.confirmedDistractorMatches,s.unsupported]){const td=document.createElement('td');td.textContent=value;tr.append(td);}$('rows').append(tr);}}
$('method').onchange=table;table();
for(const m of data.modules)$('module').add(new Option(m.name,m.id));
function module(){const m=data.modules.find(x=>x.id===$('module').value);$('module-detail').replaceChildren();const a=document.createElement('a');a.href='../../../'+m.source;a.textContent=m.source;$('module-detail').append(a);}
$('module').onchange=module;module();
function inventory(){const q=$('search').value.toLowerCase(),matches=files.filter(f=>f.path.toLowerCase().includes(q));$('file-count').textContent=matches.length+' verified source files';$('files').replaceChildren();for(const f of matches.slice(0,80)){const li=document.createElement('li'),a=document.createElement('a');a.href='../../../'+f.path;a.textContent=f.path;li.append(a);$('files').append(li);}}
$('search').oninput=inventory;inventory();
