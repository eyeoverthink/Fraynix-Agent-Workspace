'use strict';

const byId = id => document.getElementById(id);
const read = id => JSON.parse(byId(id).textContent);
const system = read('system-data'), inventory = read('inventory-data');
const pick = (id, choices) => byId(id).replaceChildren(...choices.map(row => new Option(row.title, row.id)));
byId('intro').textContent = system.summary;
pick('revision', system.runs);
pick('shot', system.screenshots.map(row => ({id:row.file,title:row.title})));
pick('module', system.modules);

function draw(run, selected) {
  const canvas = byId('plot'), context = canvas.getContext('2d');
  const height = canvas.height, width = canvas.width;
  context.clearRect(0,0,width,height);
  const distances = run.points.flatMap(row => [row.original,row.replay]);
  const low = Math.min(0,...distances), high = Math.max(.01,...distances);
  const x = row => 52+(width-82)*row.seconds/run.duration;
  const y = value => height-32-(height-60)*(value-low)/(high-low);
  context.fillStyle='#bed1d8';
  context.font='15px system-ui';
  context.fillText(high.toFixed(2)+' m',8,21);
  context.fillText(run.duration.toFixed(2)+' s',width-78,height-9);
  context.fillText('0 s',48,height-9);
  context.strokeStyle='#43646b';
  context.beginPath();
  context.moveTo(51,24);context.lineTo(51,height-32);context.lineTo(width-25,height-32);
  context.stroke();
  context.strokeStyle='#96eacc';context.lineWidth=3;context.beginPath();
  run.points.forEach((row,i)=>i ? context.lineTo(x(row),y(row.original)):context.moveTo(x(row),y(row.original)));
  context.stroke();
  context.fillStyle='#f4c885';
  for(const row of run.points){
    context.beginPath();
    context.arc(x(row),y(row.replay),row===selected?6:2.7,0,2*Math.PI);context.fill();
  }
}

function showSample() {
  const run=system.runs.find(row=>row.id===byId('revision').value);
  const selected=run.points[Number(byId('sample').value)];
  byId('input-facts').textContent=run.description;
  byId('trial-time').textContent=run.clock;
  byId('sample-result').textContent=selected.seconds.toFixed(3)+' s · original '+selected.original.toFixed(4)
    +' m · replay '+selected.replay.toFixed(4)+' m · '+selected.contacts+' contacts';
  draw(run,selected);
}
byId('revision').onchange=()=>{
 const run=system.runs.find(row=>row.id===byId('revision').value);
 byId('sample').max=run.points.length-1;
 byId('sample').value=run.points.length-1;
 showSample();
};
byId('sample').oninput=showSample;
byId('shot').onchange=()=>{
 const shot=system.screenshots.find(row=>row.file===byId('shot').value);
 byId('screenshot').src=shot.file;
 byId('screenshot').alt=shot.title;
 byId('original').href=shot.file;
 byId('caption').textContent=shot.title+' · actual desktop · 18 November 2026';
};
byId('module').onchange=()=>{
 const module=system.modules.find(row=>row.id===byId('module').value);
 byId('module-detail').textContent=module.description+' '+module.source;
};
byId('search').oninput=()=>{
 const word=byId('search').value.trim().toLowerCase();
 const matches=inventory.filter(row=>row.path.toLowerCase().includes(word));
 byId('matches').textContent=matches.length===1?'1 matching file':matches.length+' matching files';
 byId('files').replaceChildren(...matches.slice(0,10).map(({path})=>{
  const li=document.createElement('li');
  li.textContent=path;return li;
 }));
};
for(const id of ['revision','shot','module'])byId(id).onchange();
byId('search').oninput();
