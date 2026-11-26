'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const capabilities = import('../renderer/workspace-capabilities.mjs');

test('explicit requests find a workspace surface without changing existing task routing', async () => {
    const {matchWorkspaceIntent} = await capabilities;
    const commands = [
        ['Please open the browser.', 'browser'],
        ['Open Game Lab', 'game'],
        ['Can you pull up the browser?', 'browser'],
        ['Let me see my saved skills.', 'project-skills'],
        ['Open media timeline', 'media-timeline'],
        ['Open my files', 'project'],
        ['Show capabilities!', 'help'],
        ['What can Fraynix do?', 'help'],
        ['Preview source', 'source'],
        ['Show activity', 'activity'],
        ['Inspect the current web page.', 'browser-read'],
        ['Summarize this page', 'browser-read'],
        ['Open the floor plan', 'floor-plan'],
        ['Open height layers', 'height-layers'],
        ['Open the desk recording', 'desk-recording'],
        ['Open the tile workshop', 'tile-workshop'],
    ];
    for (const [prompt,id] of commands) assert.equal(matchWorkspaceIntent(prompt),id,prompt);
    const tasks = [
        'Make a floor plan at 1.2 m.',
        'Show the floor plan',
        'Show the imported environment',
        'Show the 3D environment',
        'Measure the height layers.',
        'Resume the desk recording mission',
        'Make a motion media package from the selected run.',
        'Compare the tile playtest without the jump at tick 50.',
        'Compare a collider from my project files',
        'Open a new game and save it as my own',
        'Open https://example.com',
        'Edit current source',
        'Tell me if the browser is open',
    ];
    for (const prompt of tasks) assert.equal(matchWorkspaceIntent(prompt),null,prompt);
});

test('catalog search uses all query terms across supported actions and aliases', async () => {
    const {CAPABILITIES,searchCapabilities} = await capabilities;
    const ids = CAPABILITIES.map(({id})=>id);
    assert.equal(new Set(ids).size,ids.length);
    assert.deepEqual(searchCapabilities('  '), CAPABILITIES);
    assert.deepEqual(searchCapabilities('door   svg').map(({id})=>id),['floor-plan']);
    assert.deepEqual(searchCapabilities('camera/calendar').map(({id})=>id),['desk-recording']);
    assert.deepEqual(searchCapabilities('does-not-exist'),[]);
    const source = CAPABILITIES.filter(({id})=>id==='source');
    assert.deepEqual(searchCapabilities('javascript',source),source);
});
