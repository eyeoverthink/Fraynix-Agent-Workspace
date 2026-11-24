'use strict';

const {createHash} = require('node:crypto');

function digest(bytes) {
    return createHash('sha256').update(bytes).digest('hex');
}

async function chartFollowup(command, args, {api, canvas, origin, openArtifact, showPreview}) {
    if (['start','state','choose','discard'].includes(command)) {
        return api('/api/reference-chart/' + command, args);
    }
    if (command !== 'open') throw Error('Unknown chart request operation');
    let result = await api('/api/reference-chart/prepare', args);
    if (result.preview) {
        const previous = canvas.view.webContents.getURL();
        try {
            const url = origin + result.preview;
            const response = await fetch(url, {redirect:'error'});
            if (!response.ok || digest(Buffer.from(await response.arrayBuffer())) !== result.htmlSha256) {
                throw Error('The chart preview changed before verification.');
            }
            showPreview();
            await canvas.navigate(url);
            const checks = await canvas.test(result.checks);
            const failed = checks.find(check => !check.passed);
            if (failed) throw Error('Chart display check failed: ' + (failed.selector || failed.error));
            const plot = await canvas.isolated(`(() => {
                const chart = document.getElementById('chart-svg');
                const label = id => document.getElementById(id)?.textContent;
                const bounds = chart?.getBoundingClientRect();
                return {
                    lines: [...document.querySelectorAll('#chart-svg > polyline')].map(line => line.getAttribute('points')),
                    timeStart: label('chart-time-start'), timeEnd: label('chart-time-end'),
                    valueMin: label('chart-value-min'), valueMiddle: label('chart-value-middle'), valueMax: label('chart-value-max'),
                    visible: Boolean(bounds?.width && bounds?.height && getComputedStyle(chart).visibility === 'visible')
                };
            })()`);
            result = await api('/api/reference-chart/finish', {...args, htmlSha256:result.htmlSha256, checks, plot});
        } catch (error) {
            await canvas.navigate(previous.startsWith(origin + '/') ? previous : origin + '/welcome').catch(() => {});
            throw error;
        }
    }
    const artifact = await api('/api/artifact/read', {id:result.artifact.id});
    if (artifact.version !== result.artifact.version || digest(artifact.contents['index.html']) !== result.htmlSha256) {
        throw Error('The verified chart changed before it could be opened.');
    }
    await openArtifact(artifact);
    showPreview();
    return result;
}

module.exports = {chartFollowup};
