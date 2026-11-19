const $ = id => document.getElementById(id);

// This is a UI hint. The Java Workbench decides whether the chat request matches.
export function mayRequestFrayTool(value) {
    if (value.length > 1200) return false;
    const text = value.normalize('NFKC').toLowerCase().replace(/[^a-z0-9]+/g, ' ');
    return (/\b(?:inch|inches)\b/.test(text) && /\b(?:mm|millimet(?:er|re)s?)\b/.test(text))
        || /\b(?:celsius|centigrade|c) (?:to|into) (?:fahrenheit|f)\b/.test(text);
}

const isFray = action => ['build-inch-converter', 'build-temperature-converter'].includes(action);

export class RequestWorkbenchPanel {
    constructor({request, getModel, requireSaved, layout, openCreation, announce}) {
        Object.assign(this, {request, getModel, requireSaved, layout, openCreation, announce});
        this.dialog = $('requests-dialog');
        this.task = null;
        this.busy = false;
        this.dialog.addEventListener('close', layout);
        $('requests-open').onclick = () => this.run(async () => {
            requireSaved();
            $('requests-text').value = $('goal').value;
            this.dialog.showModal();
            layout();
        });
        $('requests-close').onclick = () => this.dialog.close();
        $('requests-suggest').onclick = () => this.run(async () => {
            this.task = null;
            this.render();
            $('requests-status').textContent = 'Reviewing your request…';
            const request = $('requests-text').value.trim();
            const result = await this.call('suggest', {request, model:getModel(),
                useModel:$('requests-model').checked});
            this.show(result);
        });
        $('requests-reopen').onclick = () => this.run(async () => {
            const id = localStorage.getItem('fraynix-request-review');
            if (id) this.show(await this.call('read', {id}));
        });
        $('requests-file').onchange = () => this.run(async () => {
            const file = $('requests-file').files[0];
            if (!file || !this.task) return;
            if (file.size > 65536) throw Error('Choose a file up to 64 KiB.');
            const bytes = new Uint8Array(await file.arrayBuffer());
            let characters = '';
            for (const byte of bytes) characters += String.fromCharCode(byte);
            this.show(await this.call('attach', {id:this.task.id, name:file.name, base64:btoa(characters)}));
        });
        $('requests-action').onchange = () => this.run(async () => {
            const action = $('requests-action').value;
            if (this.task && action) this.show(await this.call('choose', {id:this.task.id, action}));
        });
        $('requests-confirm').onclick = () => this.run(async () => {
            this.requireSaved();
            const {id, reviewSha256} = this.task;
            if (this.task.choice.needsFile) {
                const result = await this.call('execute', {id, reviewSha256});
                this.show(result.task);
                this.dialog.close();
            } else if (isFray(this.task.choice.action)) {
                this.dialog.close();
                this.layout();
                try {
                    const result = await this.call('build-fray', {id, reviewSha256});
                    this.show(result.task);
                    this.announce(result.summary);
                } catch (error) {
                    this.dialog.showModal();
                    this.layout();
                    throw error;
                }
            } else {
                const brief = await this.call('brief', {id, reviewSha256});
                this.dialog.close();
                await this.openCreation(brief);
            }
        });
        $('requests-result').onclick = () => this.run(async () => {
            this.requireSaved();
            await this.call('open', {id:this.task.id});
            this.dialog.close();
        });
        $('requests-export').onclick = () => this.run(async () => {
            const file = await this.call('export', {id:this.task.id});
            $('requests-status').textContent = 'Exported to ' + file.path;
        });
        $('requests-learn').onclick = () => this.run(async () => {
            await this.call('learn', {id:this.task.id});
            $('requests-status').textContent = 'Phrase remembered in this project. Future runs still need review.';
        });
        this.render();
    }

    async offerFromChat(request) {
        const result = await this.call('chat', {request});
        if (!result.matched) return false;
        this.show(result.task);
        if (!this.dialog.open) this.dialog.showModal();
        this.layout();
        return true;
    }

    call(command, args) { return this.request('requestWorkbench', {command,args}); }

    async run(action) {
        if (this.busy) return;
        this.busy = true;
        this.render();
        try { await action(); }
        catch (error) { $('requests-status').textContent = error.message; }
        finally { this.busy = false; this.render(); }
    }

    show(task) {
        if (this.task?.id !== task.id || !task.source || task.status !== 'review') $('requests-file').value = '';
        this.task = task;
        localStorage.setItem('fraynix-request-review', task.id);
        $('requests-text').value = task.request;
        $('requests-action').value = '';
        $('requests-status').textContent = task.summary || task.choice.description;
        this.render();
    }

    render() {
        const task = this.task;
        const file = task?.choice.needsFile;
        const pending = task?.status === 'review';
        const finished = task?.status === 'verified';
        $('requests-suggest').disabled = this.busy;
        $('requests-reopen').disabled = this.busy || !localStorage.getItem('fraynix-request-review');
        $('requests-file').disabled = this.busy || !pending;
        $('requests-action').disabled = this.busy || !pending;
        $('requests-action-label').hidden = !pending;
        $('requests-attachment').hidden = !file || !pending;
        $('requests-choice').textContent = task
            ? task.choice.label + ' · ' + ({java:'local rule', model:'local model', remembered:'remembered wording', user:'your choice'})[task.choice.by] : '';
        $('requests-source').textContent = task?.source
            ? task.source.name + ' · ' + task.source.size + ' bytes · SHA-256 ' + task.source.sha256 : '';
        $('requests-confirm').hidden = !pending || task.choice.action === 'clarify';
        const fray = isFray(task?.choice.action);
        $('requests-confirm').textContent = fray ? 'Build and verify in browser'
            : file ? 'Confirm and verify' : 'Review in Creation Canvas';
        $('requests-confirm').disabled = this.busy || (file && !task?.source);
        for (const id of ['requests-result','requests-export','requests-learn']) {
            $(id).hidden = !finished;
            $(id).disabled = this.busy;
        }
    }
}
