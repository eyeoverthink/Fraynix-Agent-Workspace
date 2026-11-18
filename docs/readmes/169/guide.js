
'use strict';

const $ = id => document.getElementById(id);
const data = JSON.parse($('system-data').textContent);
const inventory = JSON.parse($('inventory-data').textContent);
const repositoryRoot = '../../../';
const svg = 'http://www.w3.org/2000/svg';

function sourceLink(name) {
    const link = document.createElement('a');
    link.href = repositoryRoot + name;
    link.textContent = name;
    return link;
}

function renderModule() {
    const item = data.modules.find(module => module.id === $('module').value);
    $('module-detail').replaceChildren(item.description + ' ', sourceLink(item.source));
}

function renderScreenshot() {
    const image = data.screenshots.find(shot => shot.path === $('shot').value);
    $('screenshot').src = repositoryRoot + image.path;
    $('caption').textContent = image.title;
}

function renderChecks() {
    $('rows').replaceChildren();
    const filter = $('kind').value;
    for (const check of data.checks) {
        if (filter !== 'all' && check.kind !== filter) continue;
        const row = document.createElement('tr');
        for (const value of [check.name, check.result, check.evidence]) {
            const cell = document.createElement('td');
            cell.textContent = value;
            row.append(cell);
        }
        $('rows').append(row);
    }
}

function renderFiles() {
    const query = $('search').value.toLowerCase();
    const results = inventory.filter(file => file.path.toLowerCase().includes(query));
    $('count').textContent = results.length + ' matches. Showing up to 60.';
    $('files').replaceChildren();
    for (const file of results.slice(0, 60)) {
        const row = document.createElement('li');
        row.append(sourceLink(file.path));
        $('files').append(row);
    }
}

function selectedCourse() {
    return data.courses.find(course => course.id === $('course').value);
}

function shape(type, attributes) {
    const element = document.createElementNS(svg, type);
    for (const [key, value] of Object.entries(attributes)) element.setAttribute(key, value);
    return element;
}

const plotX = point => 25 + point.x * 50;
const plotY = point => 150 - point.y * 50;

function renderMotion() {
    const course = selectedCourse();
    const point = course.trace[Number($('tick').value)];
    const state = point.won ? 'finish contact' : point.onGround ? 'on ground' : 'airborne';
    $('position').textContent = 'Tick ' + point.tick + ' · X ' + point.x.toFixed(3) + ' · Y '
        + point.y.toFixed(3) + ' · ' + state + ' · Cells ' + point.pickups + '/' + course.total;
    const points = course.trace.map(sample => plotX(sample) + ',' + plotY(sample)).join(' ');
    const line = shape('polyline', {points, fill: 'none', stroke: '#8be5cd', 'stroke-width': 3});
    const marker = shape('circle', {cx: plotX(point), cy: plotY(point), r: 6, fill: '#e7b76b'});
    const box = shape('rect', {
        x: plotX({x: course.rail.left}), y: plotY({y: course.rail.top}),
        width: (course.rail.right - course.rail.left) * 50,
        height: (course.rail.top - course.rail.bottom) * 50, fill: '#ff9a65'
    });
    $('plot').replaceChildren(box, line, marker);
    const crossed = course.crossing;
    $('clearance').textContent = 'Predicted clearance ' + crossed.predictedClearance.toFixed(3)
        + ' · Measured ' + crossed.measuredClearance.toFixed(3)
        + ' · ' + crossed.overlapSamples + ' overlapping samples';
}

function resetMotion() {
    $('tick').max = selectedCourse().trace.length - 1;
    $('tick').value = 0;
    renderMotion();
}

$('validation').textContent = data.validation;
for (const course of data.courses) $('course').add(new Option(course.title, course.id));
for (const item of data.modules) $('module').add(new Option(item.name, item.id));
for (const shot of data.screenshots) $('shot').add(new Option(shot.title, shot.path));
$('module').onchange = renderModule;
$('shot').onchange = renderScreenshot;
$('kind').onchange = renderChecks;
$('search').oninput = renderFiles;
$('course').onchange = resetMotion;
$('tick').oninput = renderMotion;
renderModule();
renderScreenshot();
renderChecks();
renderFiles();
resetMotion();
