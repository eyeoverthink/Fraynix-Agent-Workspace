'use strict';

const {execFile} = require('node:child_process');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const {COPYFILE_EXCL} = require('node:fs').constants;
const os = require('node:os');
const path = require('node:path');
const {promisify} = require('node:util');

const execute = promisify(execFile);
const adapter = path.join(__dirname, 'blender-exchange.py');
const maxBlendBytes = 32 * 1024 * 1024;

async function findBlender() {
    if (process.env.FRAYNIX_BLENDER) {
        const executable = path.resolve(process.env.FRAYNIX_BLENDER);
        await fs.access(executable);
        return executable;
    }
    if (process.platform === 'win32') {
        const root = path.join(process.env.ProgramFiles || 'C:/Program Files', 'Blender Foundation');
        const installs = await fs.readdir(root, {withFileTypes: true}).catch(() => []);
        const versions = installs.filter(entry => entry.isDirectory() && /^Blender [0-9.]+$/.test(entry.name))
            .sort((a, b) => b.name.localeCompare(a.name, undefined, {numeric: true}));
        if (versions.length) return path.join(root, versions[0].name, 'blender.exe');
    }
    return process.platform === 'darwin' ? '/Applications/Blender.app/Contents/MacOS/Blender' : 'blender';
}

async function runBlender(inputs, mode, source, output) {
    const executable = await findBlender();
    const script = mode==='export-shot'?path.join(__dirname,'blender-route-shot.py'):adapter;
    try {
        await execute(executable, ['--background', '--factory-startup', '--disable-autoexec', ...inputs,
            '--python-exit-code', '20', '--python', script, '--', mode, source, output],
        {windowsHide: true, timeout: 60000, maxBuffer: 256 * 1024});
    } catch (error) {
        if (error.code === 'ENOENT')
            throw Error('Install Blender locally or set FRAYNIX_BLENDER to its executable.');
        const message = (error.stderr + '\n' + error.stdout).match(/(?:ValueError|RuntimeError): ([^\r\n]+)/);
        if (message) throw Error(message[1].slice(0, 240));
        throw Error(error.killed ? 'Blender did not finish within 60 seconds.' : 'Blender could not exchange this scene.');
    }
}

async function job(action) {
    const folder = await fs.mkdtemp(path.join(os.tmpdir(), 'fraynix-blender-'));
    try {
        return await action(folder);
    } finally {
        await fs.rm(folder, {recursive: true, force: true});
    }
}

async function readBlend(input, mode, maxJsonBytes, read) {
    if (path.extname(input).toLowerCase() !== '.blend') throw Error('Choose a .blend file.');
    const file = await fs.stat(input);
    if (!file.isFile() || file.size < 1 || file.size > maxBlendBytes) throw Error('Choose a Blender file up to 32 MiB.');
    const original = await fs.readFile(input);
    if (original.length > maxBlendBytes) throw Error('Choose a Blender file up to 32 MiB.');
    const sha256 = createHash('sha256').update(original).digest('hex');
    return job(async folder => {
        const staged = path.join(folder, 'original.blend');
        const output = path.join(folder, 'scene.json');
        await fs.writeFile(staged, original);
        await runBlender([staged], mode, staged, output);
        if ((await fs.stat(output)).size > maxJsonBytes) throw Error('Blender data exceeds the exchange size limit.');
        return read(JSON.parse(await fs.readFile(output, 'utf8')), sha256);
    });
}

function importBlend(input) {
    return readBlend(input, 'import', 131072, ({scene, blenderVersion}, sha256) => {
        scene.source.note = ('Blender ' + blenderVersion + ' source SHA-256 ' + sha256 + '. ' + scene.source.note).slice(0, 1000);
        return {text: JSON.stringify(scene), sha256, blenderVersion};
    });
}

function importGameLayout(input) {
    return readBlend(input, 'import-game', 1900000, (data, sha256) => ({...data, blendSha256: sha256}));
}

async function exportBlend(text, destination, mode = 'export') {
    if (path.extname(destination).toLowerCase() !== '.blend') throw Error('Use a .blend export filename.');
    return job(async folder => {
        const input = path.join(folder, 'scene.json');
        const rendered = path.join(folder, 'scene.blend');
        await fs.writeFile(input, text, {encoding: 'utf8'});
        await runBlender([], mode, input, rendered);
        const output = await fs.readFile(rendered);
        await fs.copyFile(rendered, destination, COPYFILE_EXCL).catch(error => {
            if (error.code === 'EEXIST') throw Error('Choose a new .blend filename to keep the existing file.');
            throw error;
        });
        return {path: destination, sha256: createHash('sha256').update(output).digest('hex'), bytes: output.length};
    });
}

module.exports = {findBlender, importBlend, importGameLayout, exportBlend};
