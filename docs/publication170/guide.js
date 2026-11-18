
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
    $('count').textContent = results.length + (results.length === 1 ? ' match.' : ' matches.') + ' Showing up to 60.';
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

function selectedRun() {
    return selectedCourse().runs.find(run => run.pace === $('pace').value);
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
    const run = selectedRun();
    const point = run.trace[Number($('tick').value)];
    const state = point.won ? 'finish contact' : point.onGround ? 'on ground' : 'airborne';
    $('position').textContent = 'Tick ' + point.tick + ' · X ' + point.x.toFixed(3) + ' · Y '
        + point.y.toFixed(3) + ' · ' + state + ' · Verified rails ' + point.pickups + '/' + course.total;
    const points = run.trace.map(sample => plotX(sample) + ',' + plotY(sample)).join(' ');
    const line = shape('polyline', {points, fill: 'none', stroke: '#8be5cd', 'stroke-width': 3});
    const marker = shape('circle', {cx: plotX(point), cy: plotY(point), r: 6, fill: '#e7b76b'});
    const boxes = course.rails.map(rail => shape('rect', {
        x: plotX({x: rail.left}), y: plotY({y: rail.top}),
        width: (rail.right - rail.left) * 50,
        height: (rail.top - rail.bottom) * 50, fill: '#ff9a65'
    }));
    $('plot').replaceChildren(...boxes, line, marker);
    $('clearance').replaceChildren();
    for (const row of run.crossings) {
        const paragraph = document.createElement('span');
        paragraph.style.display = 'block';
        paragraph.textContent = row.name + ' · launch ' + row.launchSpeed.toFixed(3) + ' · entry ' + row.entrySpeed.toFixed(3)
            + ' · measured clearance ' + row.measuredClearance.toFixed(3);
        $('clearance').append(paragraph);
    }
}

function resetMotion() {
    $('tick').max = selectedRun().trace.length - 1;
    $('tick').value = 0;
    renderMotion();
}

$('validation').textContent = data.validation;
for (const course of data.courses) $('course').add(new Option(course.title, course.id));
for (const pace of data.paces) $('pace').add(new Option(pace.label, pace.id));
for (const item of data.modules) $('module').add(new Option(item.name, item.id));
for (const shot of data.screenshots) $('shot').add(new Option(shot.title, shot.path));
$('module').onchange = renderModule;
$('shot').onchange = renderScreenshot;
$('kind').onchange = renderChecks;
$('search').oninput = renderFiles;
$('course').onchange = resetMotion;
$('pace').onchange = resetMotion;
$('tick').oninput = renderMotion;
renderModule();
renderScreenshot();
renderChecks();
renderFiles();
resetMotion();
