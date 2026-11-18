'use strict';
const data = JSON.parse(document.getElementById('system-data').textContent);
const inventory = JSON.parse(document.getElementById('inventory-data').textContent);
const element = id => document.getElementById(id);
element('finding').textContent = data.finding;
element('validation').textContent = data.validation;
for (const item of data.modules) element('module').add(new Option(item.name, item.id));
for (const shot of data.screenshots) element('shot').add(new Option(shot.title, shot.path));
function moduleView() {
  const item = data.modules.find(item => item.id === element('module').value);
  element('module-detail').replaceChildren(document.createTextNode(item.description+' '));
  const link = document.createElement('a');
  link.href = '../../../'+item.source; link.textContent = item.source;
  element('module-detail').append(link);
}
function screenshot() {
  const shot = data.screenshots.find(item => item.path === element('shot').value);
  element('screenshot').src = '../../../'+shot.path;
  element('caption').textContent = shot.title;
}
function rows() {
  element('rows').replaceChildren();
  for (const row of data.comparison.filter(row => element('kind').value === 'all' || row.kind === element('kind').value)) {
    const tr = document.createElement('tr');
    for (const value of [row.name, row.kind, row.passed ? 'Passed' : 'Refused', (row.totalMs/1000).toFixed(2)]) {
      const td = document.createElement('td'); td.textContent = value; tr.append(td);
    }
    element('rows').append(tr);
  }
}
function files() {
  const matches = inventory.filter(row => row.path.toLowerCase().includes(element('search').value.toLowerCase()));
  element('count').textContent = matches.length+' matching files; showing at most 60.';
  element('files').replaceChildren();
  for (const row of matches.slice(0, 60)) {
    const li = document.createElement('li'), link = document.createElement('a');
    link.href = '../../../'+row.path; link.textContent = row.path; li.append(link); element('files').append(li);
  }
}
element('module').onchange = moduleView; element('shot').onchange = screenshot;
element('kind').onchange = rows; element('search').oninput = files;
moduleView(); screenshot(); rows(); files();
