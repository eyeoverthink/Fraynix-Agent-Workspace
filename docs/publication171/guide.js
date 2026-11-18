'use strict';

const byId = id => document.getElementById(id);
const data = id => JSON.parse(byId(id).textContent);
const system = data('system-data');
const inventory = data('inventory-data');
const select = (id, choices) => byId(id).replaceChildren(...choices.map(row => new Option(row.title, row.id)));

byId('summary').textContent = system.summary;
select('course', system.courses.map(row => ({id: row.title, title: row.title})));
select('shot', system.screenshots.map(row => ({id: row.file, title: row.title})));
select('module', system.modules);
byId('course').onchange = () => {
  const course = system.courses.find(row => row.title === byId('course').value);
  select('pace', [{id: '', title: 'All measured paces'}, ...course.runs.map(run => ({id: run.pace, title: run.pace}))]);
  showRows();
};
function showRows() {
  const course = system.courses.find(row => row.title === byId('course').value);
  const pace = byId('pace').value;
  byId('verdict').textContent = course.independentReplays + ' exact fresh replays. Minimum measured clearance '
    + course.minimumClearance.toFixed(3) + ' game units across the selected course.';
  const rows = course.runs.filter(run => !pace || run.pace === pace).flatMap(run =>
    run.crossings.map(rail => [run.pace, rail.name, rail.entrySpeed.toFixed(3),
      rail.measuredClearance.toFixed(3), run.finishTick]));
  byId('rows').replaceChildren(...rows.map(values => {
    const row = document.createElement('tr');
    for (const value of values) row.insertCell().textContent = value;
    return row;
  }));
}
byId('pace').onchange = showRows;
function showShot() {
  const shot = system.screenshots.find(row => row.file === byId('shot').value);
  byId('screenshot').src = shot.file;
  byId('screenshot').alt = shot.title;
  byId('original').href = shot.file;
  byId('caption').textContent = shot.title + ' · actual desktop · 18 November 2026';
}
function showModule() {
  const module = system.modules.find(item => item.id === byId('module').value);
  byId('module-detail').textContent = module.description + ' ' + module.source;
}
function showInventory() {
  const word = byId('search').value.trim().toLowerCase();
  const matches = inventory.filter(row => row.path.toLowerCase().includes(word));
  byId('matches').textContent = matches.length + ' matched files' + (matches.length > 10 ? ' · first 10 shown' : '');
  byId('files').replaceChildren(...matches.slice(0, 10).map(({path}) => {
    const row = document.createElement('li');
    row.textContent = path;
    return row;
  }));
}
byId('shot').onchange = showShot;
byId('module').onchange = showModule;
byId('search').oninput = showInventory;
byId('course').onchange();
showShot();
showModule();
showInventory();
