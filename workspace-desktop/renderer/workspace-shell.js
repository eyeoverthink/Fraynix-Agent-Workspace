import {CAPABILITIES,searchCapabilities} from './workspace-capabilities.mjs';

const $=id=>document.getElementById(id);
const SHOWCASE=['floor-plan','desk-recording','browser-read','tile-workshop'];

function button(id,label,action,className='') {
    const control=document.createElement('button');
    control.id=id;
    control.type='button';
    control.textContent=label;
    control.className=className;
    control.onclick=action;
    return control;
}

export class WorkspaceShell {
    constructor({open,relayout,request,requireSaved,context,interfaceMode}) {
        Object.assign(this,{open,relayout,request,requireSaved,context});
        this.stage='home';
        this.side='';
        this.installHeader();
        this.installHome();
        this.installTools();
        this.installConversation();
        this.installManualControls();
        const saved=localStorage.getItem('fraynix.workspace.interface');
        const requested=interfaceMode;
        this.setExpert(requested==='expert'||(requested!=='unified'&&saved==='expert'),false);
        this.onResize=()=>{this.syncFocus();this.relayout();};
        window.addEventListener('resize',this.onResize);
        window.addEventListener('keydown',event=>{
            if(!(event.ctrlKey||event.metaKey)||event.key.toLowerCase()!=='k')return;
            event.preventDefault();
            if(this.tools.open)this.tools.close();
            else if(!document.querySelector('dialog[open]'))this.showTools();
        });
    }

    installHeader() {
        const header=document.querySelector('body > header');
        const brand=header.querySelector('.brand');
        brand.classList.add('workspace-brand');
        const name=brand.querySelector('small');
        name.textContent='Local workspace';
        this.header=document.createElement('div');
        this.header.id='workspace-header-actions';
        this.header.className='workspace-nav';
        const home=button('workspace-home-open','Workspace',()=>this.side?this.showSide(''):this.choose('home'));
        const project=button('workspace-project-open','Project',()=>this.choose('project'));
        const tools=button('workspace-tools-open','Tools',()=>this.showTools());
        tools.title='Find capabilities · Ctrl/⌘+K';
        const activity=button('workspace-activity-open','Activity',()=>this.open('activity'));
        this.header.append(home,project,button('workspace-browser-open','Browser',()=>this.choose('browser')),tools,activity);
        const engine=$('engine-state');
        this.header.append(engine);
        this.legacyHeader=header.querySelector('.header-actions');
        const old=this.legacyHeader;
        old.before(this.header);
        this.expertBack=button('workspace-simple','Unified workspace',()=>this.setExpert(false,true));
        old.append(this.expertBack);
        this.stageLabel=document.createElement('span');
        this.stageLabel.className='workspace-stage-state';
        this.stageLabel.id='workspace-stage-state';
        this.stageLabel.textContent='Ready';
        const stageHead=document.querySelector('.canvas-panel > .panel-head');
        this.stageHeading=stageHead.querySelector('h2');
        stageHead.append(this.stageLabel);
    }

    installHome() {
        this.home=document.createElement('section');
        this.home.id='workspace-home';
        this.home.hidden=true;
        this.home.innerHTML=`
          <div class="workspace-hero">
            <h1>What are we working on?</h1>
            <p>Start in conversation. Follow the source, the work, and its evidence here.</p>
            <button id="workspace-home-ask" class="primary" type="button">Start a request</button>
          </div>
          <section class="workspace-quick">
            <h2>Try a request</h2>
            <div class="workspace-suggestions"></div>
          </section>`;
        const results=this.home.querySelector('.workspace-suggestions');
        for(const id of SHOWCASE) {
            const row=CAPABILITIES.find(item=>item.id===id);
            if(!row?.prompt)continue;
            const item=button('workspace-try-'+id,row.title,()=>this.compose(row.prompt),'workspace-suggestion');
            const text=document.createElement('small');
            text.textContent=row.prompt;
            item.append(text);
            results.append(item);
        }
        this.home.querySelector('#workspace-home-ask').onclick=()=>this.compose();
        $('canvas-surface').before(this.home);
    }

    installTools() {
        this.tools=document.createElement('dialog');
        this.tools.id='workspace-tools';
        this.tools.innerHTML=`
          <div class="workspace-tools-heading">
            <h2>Find a capability</h2>
            <button id="workspace-tools-close" type="button" aria-label="Close tools">Close</button>
          </div>
          <label for="workspace-tool-search">Search workspace tools</label>
          <input id="workspace-tool-search" type="search" placeholder="Floor plan, timeline, browser…" autocomplete="off">
          <p id="workspace-tool-summary" role="status"></p>
          <div id="workspace-tool-results"></div>
          <details id="workspace-expert-options">
            <summary>Interface options</summary>
            <label><input id="workspace-expert" type="checkbox"> Show all manual controls</label>
          </details>`;
        document.body.append(this.tools);
        this.search=$('workspace-tool-search');
        this.results=$('workspace-tool-results');
        $('workspace-tools-close').onclick=()=>this.tools.close();
        this.tools.addEventListener('keydown',event=>{
            if(event.key==='Escape'){
                event.preventDefault();
                this.tools.close();
            }
        });
        this.tools.addEventListener('close',()=>{
            ++this.querySequence;
            clearTimeout(this.searchDelay);
            this.relayout();
            if(this.returnFocus?.isConnected)this.returnFocus.focus({preventScroll:true});
        });
        $('workspace-expert').onchange=event=>{
            this.setExpert(event.target.checked,true);
            this.tools.close();
        };
        this.search.oninput=()=>this.findTools();
        this.search.onkeydown=event=>{
            if(event.key==='ArrowDown'&&!event.altKey) {
                event.preventDefault();
                this.results.querySelector('button')?.focus();
            }
        };
        this.results.onkeydown=event=>{
            if(!['ArrowUp','ArrowDown'].includes(event.key))return;
            const items=[...this.results.querySelectorAll('button')];
            const current=items.indexOf(document.activeElement);
            if(current<0)return;
            event.preventDefault();
            const next=current+(event.key==='ArrowDown'?1:-1);
            if(next<0)this.search.focus();
            else items[Math.min(items.length-1,next)]?.focus();
        };
        this.querySequence=0;
    }

    installConversation() {
        const thread=document.querySelector('.agent-panel');
        const title=thread.querySelector('.panel-head h2');
        title.textContent='Conversation';
        $('goal').placeholder='Ask Fraynix to make, measure, or find something';
        $('run').textContent='Send';
        $('run').title='Send request · Ctrl/⌘+Enter';
        $('run-state').textContent='Ready';
        const welcome=$('messages').querySelector('.message:not(:has(.message099-label))');
        if(welcome)welcome.textContent='Ask me to work with a scene, a recording, a webpage, or a project file.';
        const details=document.createElement('details');
        details.id='workspace-chat-controls';
        const summary=document.createElement('summary');
        summary.textContent='Model & inputs';
        details.append(summary);
        const modelLabel=thread.querySelector('.model-label');
        modelLabel.before(details);
        details.append(modelLabel,$('model'),$('settings099'),$('inputs100'));
        if(localStorage.getItem('fraynix.workspace.interface')==='expert')details.open=true;
        this.contextNode=document.createElement('p');
        this.contextNode.id='workspace-context';
        this.contextNode.className='workspace-context';
        this.contextNode.textContent='Workspace ready';
        $('goal').before(this.contextNode);
        this.followup=document.createElement('section');
        this.followup.id='workspace-next';
        this.followup.hidden=true;
        this.followup.setAttribute('aria-label','Current request next step');
        $('goal').before(this.followup);
        this.compact=document.createElement('nav');
        this.compact.className='workspace-modes';
        this.compact.setAttribute('aria-label','Workspace focus');
        this.compact.append(
            button('workspace-focus-stage','Workspace',()=>this.focusPane('stage')),
            button('workspace-focus-chat','Conversation',()=>this.focusPane('chat'))
        );
        document.querySelector('.layout').before(this.compact);
        this.focusPane('chat');
    }

    installManualControls() {
        const toolbar=document.querySelector('.canvas-toolbar');
        this.manual=document.createElement('details');
        this.manual.id='workspace-manual';
        const summary=document.createElement('summary');
        summary.textContent='Manual controls';
        toolbar.before(this.manual);
        this.manual.append(summary,toolbar);
        this.manual.addEventListener('toggle',this.relayout);
    }

    setExpert(enabled,save=true) {
        this.expert=enabled;
        document.body.classList.toggle('unified-workspace',!enabled);
        document.body.classList.remove('workspace-browser','workspace-project','browser-focus','canvas-focus');
        this.side='';
        if(save)localStorage.setItem('fraynix.workspace.interface',enabled?'expert':'unified');
        if(enabled)this.legacyHeader.prepend($('engine-state'));
        else this.header.append($('engine-state'));
        $('workspace-expert').checked=enabled;
        this.stageHeading.textContent=enabled?'Creation canvas':'Workspace';
        $('workspace-chat-controls').open=enabled;
        this.manual.open=enabled;
        if(this.startup) this.choose(enabled?'preview':'home');
        this.relayout();
    }

    start() {
        this.startup=true;
        this.open(this.expert?'preview':'home');
    }

    focusPane(pane) {
        this.pane=pane;
        this.syncFocus();
        this.relayout();
    }

    syncFocus() {
        for(const pane of ['stage','chat']){
            document.body.classList.toggle('workspace-compact-'+pane,this.pane===pane);
            $('workspace-focus-'+pane)?.setAttribute('aria-pressed',String(this.pane===pane));
        }
    }

    compose(prompt) {
        if(prompt){
            $('goal').value=prompt;
            $('goal').dispatchEvent(new Event('input'));
        }
        this.focusPane('chat');
        $('goal').focus({preventScroll:true});
    }

    choose(id) {
        try{
            this.requireSaved();
            return Promise.resolve(this.open(id)).catch(error=>this.showError(error));
        }catch(error) {
            this.showError(error);
        }
    }

    showError(error) {
        const notice=$('notice');
        notice.textContent=error.message;
        this.stageLabel.textContent=error.message;
        this.stageLabel.dataset.status='needs-input';
    }

    stageChanged(mode) {
        this.stage=mode;
        this.home.hidden=mode!=='home';
        this.showSide('');
        this.stageLabel.dataset.status='';
        this.stageLabel.textContent=mode==='home'?'Ready':'In view';
        if(mode!=='home')this.focusPane('stage');
        this.updateContext();
    }

    showSide(name) {
        this.side=name;
        for(const kind of ['browser','project'])
            document.body.classList.toggle('workspace-'+kind,!this.expert&&kind===name);
        if(this.expert){
            document.body.classList.toggle('browser-focus',name==='browser');
            document.body.classList.remove('canvas-focus');
        }
        for(const kind of ['project','browser'])
            $('workspace-'+kind+'-open').setAttribute('aria-pressed',String(name===kind));
        if(name)this.focusPane('stage');
        this.updateContext();
        this.relayout();
    }

    updateContext() {
        const detail=this.context(this.side||this.stage);
        this.contextNode.textContent=detail||'Workspace ready';
        this.contextNode.title=this.contextNode.textContent;
        this.syncFollowup();
    }

    get pending() {return this.nextAction;}

    setStageStatus(label,state='') {
        this.stageLabel.textContent=label;
        this.stageLabel.dataset.status=state;
    }

    needInput({title,description,choose,canContinue,resume}) {
        this.nextAction={title,description,choose,canContinue,resume};
        this.followup.replaceChildren();
        const heading=document.createElement('strong');
        heading.textContent=title;
        const body=document.createElement('p');
        body.textContent=description;
        const actions=document.createElement('div');
        actions.className='workspace-next-actions';
        const open=button('workspace-next-choose',choose.label,()=>choose.action());
        this.continueButton=button('workspace-next-continue','Continue request',()=>this.resume(),'primary');
        const dismiss=button('workspace-next-dismiss','Dismiss',()=>this.clearInput(),'quiet');
        actions.append(open,this.continueButton,dismiss);
        this.followup.append(heading,body,actions);
        this.followup.hidden=false;
        this.setStageStatus('Needs input','needs-input');
        this.syncFollowup();
    }

    clearInput() {
        this.nextAction=null;
        this.followup.hidden=true;
        this.followup.replaceChildren();
    }

    syncFollowup() {
        if(this.nextAction)this.continueButton.disabled=!this.nextAction.canContinue();
    }

    async resume() {
        const action=this.nextAction;
        if(!action||!action.canContinue())return;
        try{
            this.requireSaved();
            this.clearInput();
            await action.resume();
        }catch(error){
            this.showError(error);
        }
    }

    showTools() {
        if(this.tools.open)return;
        this.returnFocus=document.activeElement;
        this.search.value='';
        this.tools.showModal();
        this.relayout();
        this.search.focus();
        this.findTools();
    }

    findTools() {
        const sequence=++this.querySequence;
        const query=this.search.value.trim();
        this.renderTools(searchCapabilities(query),[]);
        clearTimeout(this.searchDelay);
        this.searchDelay=setTimeout(async()=>{
            try{
                const result=await this.request('knowledge',{command:'self',args:{query:query.slice(0,80)}});
                if(sequence!==this.querySequence||!this.tools.open)return;
                const matches=query?(result.capabilities||[]).slice(0,12):[];
                this.renderTools(searchCapabilities(query),matches,result.registeredToolCount);
            }catch{
                if(sequence===this.querySequence&&this.tools.open)this.renderTools(searchCapabilities(query),[]);
            }
        },220);
    }

    renderTools(rows,tools,count) {
        const active=this.results.contains(document.activeElement)?document.activeElement.dataset.capability:null;
        this.results.replaceChildren();
        $('workspace-tool-summary').textContent=count===undefined
            ? rows.length+' workspace actions'
            : rows.length+' workspace actions · '+count+' registered capabilities';
        for(const row of rows)this.appendTool(row.title,row.description,row.group,()=>{
            this.tools.close();
            this.choose(row.id);
        },row.id);
        for(const tool of tools)this.appendTool(tool.name,tool.description,'Registered tool',async()=>{
            this.tools.close();
            await this.choose('knowledge');
            $('knowledge-query').value=tool.name;
            $('knowledge-self').click();
        });
        if(active)[...this.results.querySelectorAll('button')].find(button=>button.dataset.capability===active)?.focus();
        if(!rows.length&&!tools.length){
            const empty=document.createElement('p');
            empty.textContent='No actions match this search.';
            this.results.append(empty);
        }
    }

    appendTool(title,description,group,action,id='registered') {
        const entry=button('',title,action,'workspace-tool');
        entry.dataset.capability=id;
        const type=document.createElement('span');
        type.className='workspace-tool-group';
        type.textContent=group;
        const detail=document.createElement('small');
        detail.textContent=description;
        entry.append(type,detail);
        this.results.append(entry);
    }
}
