'use strict';

const byId = id => document.getElementById(id);
const system = JSON.parse(byId('system-data').textContent);
const inventory = JSON.parse(byId('inventory-data').textContent);
byId('intro').textContent = system.summary;

function picker(id, list, onChange) {
  const select = byId(id);
  for (const row of list) select.add(new Option(row.title, row.id));
  const show = () => onChange(list.find(row => row.id === select.value));
  select.addEventListener('change', show);
  show();
}

const canvas = byId('plot'), context = canvas.getContext('2d'), sample = byId('sample');
let selected;
function plot() {
  const rows = selected.rows;
  const extent = Math.max(...rows.map(row => Math.abs(row[1]))) || 1;
  const x = time => 24 + time / rows.at(-1)[0] * (canvas.width - 48);
  const y = value => canvas.height - 20 - value / extent * (canvas.height - 44);
  const [time, distance] = rows[Number(sample.value)];
  context.clearRect(0, 0, canvas.width, canvas.height);
  context.strokeStyle = '#52747a';
  context.beginPath();
  context.moveTo(24, 20);
  context.lineTo(24, canvas.height - 20);
  context.lineTo(canvas.width - 24, canvas.height - 20);
  context.stroke();
  context.lineWidth = 3;
  context.strokeStyle = '#98e6c2';
  context.beginPath();
  rows.forEach(([t, value], index) => index ? context.lineTo(x(t), y(value)) : context.moveTo(x(t), y(value)));
  context.stroke();
  context.fillStyle = '#f7cd90';
  context.beginPath();
  context.arc(x(time), y(distance), 5, 0, 2 * Math.PI);
  context.fill();
  byId('sample-result').textContent = 'Sample ' + (Number(sample.value) + 1) + ' · ' +
    time.toFixed(3) + ' s · ' + distance.toFixed(3) + ' m simulated displacement';
}
picker('trial', system.trials, row => {
  selected = row;
  sample.max = row.rows.length - 1;
  sample.value = sample.max;
  byId('trace-detail').textContent = row.detail;
  plot();
});
sample.addEventListener('input', plot);
picker('probe', system.probes, row => byId('probe-result').textContent = row.reason + ' No summary started.');
picker('shot', system.screenshots, row => {
  byId('original').href = row.id;
  byId('screenshot').src = row.id;
  byId('caption').textContent = row.title + ' · actual desktop';
});
picker('module', system.modules, row => {
  byId('module-detail').textContent = row.description + ' ' + row.source;
});
const find = () => {
  const query = byId('search').value.trim().toLowerCase();
  const files = inventory.filter(row => row.path.toLowerCase().includes(query));
  byId('matches').textContent = files.length + (files.length === 1 ? ' matching file' : ' matching files');
  byId('files').replaceChildren(...files.slice(0, 18).map(row => {
    const item = document.createElement('li');
    item.textContent = row.path;
    return item;
  }));
};
byId('search').addEventListener('input', find);
find();
