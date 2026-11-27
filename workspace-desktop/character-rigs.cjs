'use strict';

const {execFile} = require('node:child_process');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {promisify} = require('node:util');
const {findBlender} = require('./blender-exchange.cjs');

const execute = promisify(execFile);
const adapter = path.join(__dirname, 'blender-character.py');
const palettes = ['teal', 'amber'];
const names = ['idle', 'blink', 'walk-1', 'walk-2', 'walk-3', 'walk-4', 'takeoff', 'airborne', 'landing'];
const format = 'fraynix.character-rig-v1';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

async function readBound(file, limit) {
    const info = await fs.lstat(file);
    if (!info.isFile() || !info.size || info.size > limit) throw Error('Invalid character asset size.');
    const bytes = await fs.readFile(file);
    if (bytes.length !== info.size) throw Error('The character asset changed during reading.');
    return bytes;
}

async function blender(executable, folder, command, args = []) {
    const source = command === 'inspect' ? [path.join(folder, 'courier.blend')] : [];
    try {
        await execute(executable, ['--background', '--factory-startup', '--disable-autoexec',
            ...source, '--python-exit-code', '20', '--python', adapter, '--', command, folder, ...args],
        {timeout: 120_000, maxBuffer: 256_000, windowsHide: true});
    } catch (error) {
        if (error.code === 'ENOENT') throw Error('Install Blender locally or set FRAYNIX_BLENDER to its executable.');
        const detail = (String(error.stderr || '') + '\n' + String(error.stdout || ''))
            .match(/(?:AssertionError|ValueError|RuntimeError): ([^\r\n]+)/);
        throw Error(detail?.[1]?.slice(0, 200) || (error.killed
            ? 'Character baking timed out.' : 'Blender could not build or reopen the character.'));
    }
}

function characterRigs(project, api, exports) {
    const root = path.join(project, 'media', 'character-rigs');
    let baking = false;

    async function read(id) {
        if (typeof id !== 'string' || !/^[0-9a-f]{64}$/.test(id)) throw Error('Choose a saved character rig.');
        const directory = path.join(root, id);
        if (!(await fs.lstat(directory)).isDirectory()) throw Error('Invalid saved character folder.');
        const encoded = await readBound(path.join(directory, 'rig.json'), 12_000);
        if (hash(encoded) !== id) throw Error('The saved character manifest changed.');
        const manifest = JSON.parse(encoded);
        if (manifest.format !== format || !palettes.includes(manifest.palette))
            throw Error('Unknown character rig format.');
        const blend = await readBound(path.join(directory, 'courier.blend'), 20_000_000);
        if (hash(blend) !== manifest.blendSha256) throw Error('The saved Blender project changed.');
        const frames = await Promise.all(names.map(async name => {
            const png = await readBound(path.join(directory, name + '.png'), 100_000);
            if (hash(png) !== manifest.frames[name]) throw Error('The saved ' + name + ' frame changed.');
            return png;
        }));
        return {id, manifest, blend, frames};
    }

    async function build(palette) {
        if (!palettes.includes(palette)) throw Error('Choose a Courier palette.');
        if (baking) throw Error('A character bake is already running.');
        baking = true;
        let folder;
        try {
            const executable = await findBlender();
            await fs.mkdir(root, {recursive: true});
            folder = await fs.mkdtemp(path.join(root, '.bake-'));
            await blender(executable, folder, 'build', [palette]);
            await blender(executable, folder, 'inspect');
            const inspectionFile = path.join(folder, 'inspection.json');
            const inspection = JSON.parse(await readBound(inspectionFile, 10_000));
            const blendSha256 = hash(await readBound(path.join(folder, 'courier.blend'), 20_000_000));
            const bytes = await Promise.all(names.map(name => readBound(path.join(folder, name + '.png'), 100_000)));
            await check(bytes);
            const frames = Object.fromEntries(names.map((name, i) => [name, hash(bytes[i])]));
            const manifest = {format, name: palette === 'teal' ? 'Teal Courier' : 'Amber Courier',
                palette, blendSha256, inspection, frames};
            const record = JSON.stringify(manifest);
            const id = hash(record);
            await fs.unlink(inspectionFile);
            await fs.writeFile(path.join(folder, 'rig.json'), record, {flag: 'wx'});
            try {
                await fs.rename(folder, path.join(root, id));
                folder = null;
            } catch (error) {
                // The only safe reuse is a complete existing bundle of the same bytes.
                try { await read(id); } catch { throw error; }
            }
            return show(await read(id));
        } finally {
            baking = false;
            if (folder) await fs.rm(folder, {recursive: true, force: true});
        }
    }

    function check(frames, player) {
        return api('/api/avatar/rig-frames', {frames: frames.map(bytes => bytes.toString('base64')), player});
    }

    async function show({id, manifest, frames}) {
        await check(frames);
        return {id, ...manifest, frames: names.map((name, i) => ({name, png: frames[i].toString('base64')}))};
    }

    async function list() {
        const entries = await fs.readdir(root, {withFileTypes: true}).catch(error => {
            if (error.code === 'ENOENT') return [];
            throw error;
        });
        const result = [];
        const directories = entries.filter(entry => entry.isDirectory() && /^[0-9a-f]{64}$/.test(entry.name));
        for (const {name: id} of directories) {
            try {
                const record = await readBound(path.join(root, id, 'rig.json'), 12_000);
                if (hash(record) !== id) continue;
                const {name} = JSON.parse(record);
                if (typeof name === 'string') result.push({id, name});
            } catch (error) {
                if (!['ENOENT', 'ENOTDIR'].includes(error.code) && !(error instanceof SyntaxError)) throw error;
            }
        }
        return result.sort((a, b) => a.name.localeCompare(b.name) || a.id.localeCompare(b.id));
    }

    return async (command, args) => {
        if (command === 'list') return list();
        if (command === 'bake') return build(args.palette);
        if (!['open', 'player', 'blend', 'atlas'].includes(command)) throw Error('Unknown character rig command.');
        const verified = await read(args.id);
        const {id, manifest, blend, frames} = verified;
        if (command === 'open') return show(verified);
        if (command === 'player') return check(frames, {id, name: manifest.name});
        const source = {id, blendSha256: manifest.blendSha256, frames: manifest.frames};
        if (command === 'blend') return exports.save('character-rig', 'blend', blend, source, manifest.blendSha256);
        const atlas = await check(frames);
        return exports.save('character-atlas', 'png', Buffer.from(atlas.png, 'base64'), source, atlas.sha256);
    };
}

module.exports = {characterRigs};
