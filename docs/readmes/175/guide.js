'use strict';

const byId = id => document.getElementById(id);
const system = JSON.parse(byId('system-data').textContent);
const inventory = JSON.parse(byId('inventory-data').textContent);
const seconds = ms => (ms / 1000).toFixed(2) + ' s';
byId('intro').textContent = system.summary;

function picker(id, list, onChange) {
  const select = byId(id);
  for (const row of list) select.add(new Option(row.title, row.id));
  const show = () => onChange(list.find(row => row.id === select.value));
  select.addEventListener('change', show);
  show();
}

picker('pair', system.pairs, row => {
  const extent = Math.max(row.directMs, row.modelMs);
  const percentage = ms => (100 * ms / extent) + '%';
  byId('pair-goal').textContent = row.request;
  byId('direct-label').textContent = 'Direct · ' + seconds(row.directMs);
  byId('model-label').textContent = 'Model · ' + seconds(row.modelMs);
  byId('direct-bar').style.width = percentage(row.directMs);
  byId('model-bar').style.width = percentage(row.selectionMs);
  byId('rest-bar').style.width = percentage(row.modelMs - row.selectionMs);
  byId('pair-result').textContent = seconds(row.selectionMs) + ' spent selecting the procedure. ' + row.result;
});
picker('probe', system.probes, row => {
  byId('probe-goal').textContent = row.request;
  byId('probe-result').textContent = row.reason + ' ' + row.modelCalls +
    (row.modelCalls === 1 ? ' model request' : ' model requests') + ' · no child started';
});
picker('shot', system.screenshots, row => {
  byId('original').href = row.id;
  byId('screenshot').src = row.id;
  byId('caption').textContent = row.title + ' · actual desktop';
});
picker('module', system.modules, row => {
  byId('module-detail').textContent = row.description + ' ' + row.source;
});

const findSource = () => {
  const query = byId('search').value.trim().toLowerCase();
  const found = inventory.filter(row => row.path.toLowerCase().includes(query));
  byId('matches').textContent = String(found.length) + (found.length === 1 ? ' matching file' : ' matching files');
  byId('files').replaceChildren(...found.slice(0, 18).map(row => {
    const item = document.createElement('li');
    item.textContent = row.path;
    return item;
  }));
};
byId('search').addEventListener('input', findSource);
findSource();
