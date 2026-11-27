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
        ['Open Avatar Studio', 'avatar-studio'],
        ['Open image to playable level', 'avatar-level'],
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
        'Make an avatar from this photo.',
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

test('plain language distinguishes the verified avatar level workflow from other media work', async () => {
    const {avatarLevelIntent}=await import('../renderer/avatar-level-panel.mjs');
    const jump='Could you take my photo and build a playable platformer with hurdles named “First Jump”, then test it and export offline?';
    assert.deepEqual(avatarLevelIntent(jump),{request:jump,title:'First Jump',course:'jump',pause:false});
    const relay='Make a playable level from the image with no jumps and pause before export.';
    assert.deepEqual(avatarLevelIntent(relay),{request:relay,title:'',course:'relay',pause:true});
    assert.deepEqual(avatarLevelIntent('Resume my avatar level.'),{resume:true});
    assert.equal(avatarLevelIntent('Make this portrait into a talking avatar.'),null);
    assert.equal(avatarLevelIntent('How do I export a playable level with an avatar?'),null);
});

test('avatar level briefs retain counts, spacing, repair review and geometry constraints', async () => {
    const {avatarLevelIntent}=await import('../renderer/avatar-level-panel.mjs');
    const request='Build a playable level from this image with three hurdles and six coins, staggered, an oversized first hurdle and an elevated pickup. Pause before repair.';
    assert.deepEqual(avatarLevelIntent(request),{request,title:'',course:'jump',pause:false,
        brief:{hurdles:3,pickups:6,spacing:'staggered',oversized:true,elevated:true,locked:false,pauseRepair:true}});
    const relay=avatarLevelIntent('Make a playable relay from this portrait with no obstacles, five pickups and keep the layout fixed.');
    assert.equal(relay.course,'relay');
    assert.equal(relay.brief.hurdles,0);
    assert.equal(relay.brief.pickups,5);
    assert.equal(relay.brief.locked,true);
    assert.equal(avatarLevelIntent('Make a playable platformer from this image with 9 hurdles.').brief.hurdles,9);
});
