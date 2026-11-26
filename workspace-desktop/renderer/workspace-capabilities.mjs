// The workspace uses these entries for discovery. Tools and approvals stay in their existing handlers.
export const CAPABILITIES = [
    {
        id:'environment', title:'Environment', group:'World and simulation',
        description:'Open an imported room or building and inspect its geometry.',
        keywords:['room','building','space','apartment','glb','3d','geometry'],
        target:'#environment-tab',
    },
    {
        id:'floor-plan', title:'Floor plans', group:'World and simulation',
        description:'Measure a horizontal section or build geometry from a scaled floor plan.',
        keywords:['2d','floorplan','section','walls','doors','height','glb','svg'],
        prompt:'Make a floor plan at 1.2 m.',
        target:'#environment-plans > summary',
    },
    {
        id:'height-layers', title:'Height layers', group:'World and simulation',
        description:'Compare geometry across heights and review where its shape changes.',
        keywords:['height','layers','bands','measurement','geometry','openings'],
        prompt:'Measure the height layers.',
        target:'#environment-height-layers > summary',
    },
    {
        id:'desk-recording', title:'Desk recording mission', group:'Media and evidence',
        description:'Resume a recording, verified preview, and draft calendar event.',
        keywords:['recording','camera','footage','preview','calendar','mission','video'],
        prompt:'Resume the desk recording mission.',
        target:'#environment-desk > summary',
    },
    {
        id:'browser', title:'Browser', group:'Workspace',
        description:'Open the embedded browser for web pages and local files.',
        keywords:['web','website','page','url','browser','file'],
        target:'#expand-browser',
    },
    {
        id:'browser-read', title:'Inspect the current page', group:'Workspace',
        description:'Read what is visible on the current browser page.',
        keywords:['web','page','inspect','read','observe','browser','evidence'],
        prompt:'Inspect the current web page.',
        target:'#observe-browser',
    },
    {
        id:'motion-media', title:'Motion media', group:'Media and evidence',
        description:'Build video, audio, a poster, and a timeline from a completed motion recording.',
        keywords:['recording','media','video','poster','audio','motion','experiment','export'],
        prompt:'Make a motion media package from the selected run.',
        target:'#media-motion-mode',
    },
    {
        id:'tile-workshop', title:'Tile Workshop', group:'Media and evidence',
        description:'Edit tile levels and inspect or replay recorded playtests.',
        keywords:['tile','sprite','game','level','playtest','replay','nexus','media'],
        prompt:'Inspect the saved tile playtest.',
        target:'#media-tiles-mode',
    },
    {
        id:'game', title:'Game Lab', group:'World and simulation',
        description:'Open the Java game workspace for playing and verifying levels.',
        keywords:['game','level','play','relay','playtest','platformer'],
        target:'#game',
    },
    {
        id:'scene', title:'3D Lab', group:'World and simulation',
        description:'Open mechanics scenes to test motion, collisions, and forces.',
        keywords:['3d','scene','mechanics','physics','ramp','motion','force','simulation'],
        target:'#scene-tab',
    },
    {
        id:'spatial', title:'Spatial', group:'World and simulation',
        description:'Inspect models, place objects, and review collision and geometry evidence.',
        keywords:['spatial','prop','placement','model','geometry','collision','collider','blender'],
        target:'#spatial-tab',
    },
    {
        id:'maps', title:'Maps', group:'World and simulation',
        description:'Open or create a playable map from a reference image.',
        keywords:['map','image','world','level','navigation','dungeon'],
        target:'#maps-tab',
    },
    {
        id:'perception', title:'Perception', group:'Media and evidence',
        description:'Open browser perception tools to observe and track a target.',
        keywords:['observe','track','tracking','vision','perception','image','browser'],
        target:'#perception-tab',
    },
    {
        id:'preview', title:'Live preview', group:'Workspace',
        description:'View the active project artifact and its checked result.',
        keywords:['preview','artifact','canvas','output','result','project'],
        target:'#preview-tab',
    },
    {
        id:'source', title:'Source editor', group:'Workspace',
        description:'Review or edit the source of the active project artifact.',
        keywords:['source','editor','code','html','css','javascript','artifact'],
        target:'#source-tab',
    },
    {
        id:'project', title:'Project files', group:'Workspace',
        description:'Find saved artifacts, scenes, and project skills.',
        keywords:['project','files','library','artifact','skill','scene'],
        target:'#artifacts-tab',
    },
    {
        id:'data', title:'Data tasks', group:'Media and evidence',
        description:'Import and analyze data, then review checked results.',
        keywords:['data','csv','dataset','analysis','measurements','report'],
        target:'#data-open',
    },
    {
        id:'world', title:'World model', group:'World and simulation',
        description:'Review the learned model and its predictions against observed motion.',
        keywords:['world','model','prediction','learning','simulation'],
        target:'#world-open',
    },
    {
        id:'brain', title:'Java Brain', group:'World and simulation',
        description:'Inspect the local controller, training, and checked feedback.',
        keywords:['java','brain','controller','learning','training','feedback'],
        target:'#brain-open',
    },
    {
        id:'activity', title:'Activity', group:'Workspace',
        description:'See recent work, task status, and execution evidence.',
        keywords:['activity','mission','history','evidence','progress','timeline','events'],
        target:'#activity-toggle099',
    },
    {
        id:'knowledge', title:'Knowledge and tools', group:'Workspace',
        description:'Inspect project knowledge, retained sources, and available tools.',
        keywords:['knowledge','tool','sources','research','reference','notes'],
        target:'#knowledge-open',
    },
    {
        id:'media-timeline', title:'Media timeline', group:'Media and evidence',
        description:'Arrange clips, images, and audio and verify the edited timeline.',
        keywords:['video','audio','editing','tracks','timeline','media'],
        target:'#media-timeline-mode',
    },
    {
        id:'sprite-editor', title:'Sprite editor', group:'Media and evidence',
        description:'Isolate a sprite and build a playable 2D scene.',
        keywords:['sprite','image','platformer','game','pixel','2d'],
        target:'#media-legacy-mode',
    },
    {
        id:'task-history', title:'Task history', group:'Workspace',
        description:'Reopen finished work and recheck its original evidence.',
        keywords:['history','restart','saved','mission','outcome','archive'],
        target:'#history110-open',
    },
    {
        id:'ramp-input', title:'Ramp setup', group:'World and simulation',
        description:'Choose or save the measured input for a ramp experiment.',
        keywords:['ramp','setup','csv','friction','input','comparison','physics'],
        target:'#ramp-input-open',
    },
    {
        id:'relay-review', title:'Relay mission review', group:'World and simulation',
        description:'Review the checked Relay playtest mission.',
        keywords:['relay','review','mission','game','level','proof'],
        target:'#relay-review-open',
    },
    {
        id:'request-workbench', title:'Request workbench', group:'Workspace',
        description:'Open and review durable work from natural-language requests.',
        keywords:['task','request','workbench','workflow','creation'],
        target:'#requests-open',
    },
    {
        id:'project-skills', title:'Saved skills', group:'Workspace',
        description:'Browse the project’s verified skills and procedures.',
        keywords:['learning','memory','verified','skill','procedure','project'],
        target:'#skills-tab',
    },
    {
        id:'help', title:'What can I ask?', group:'Workspace',
        description:'Find a supported request or open a workspace tool.',
        keywords:['help','capabilities','commands','actions','examples','what can you do'],
        prompt:'What can you do?',
    },
];

const normalize = value => value.normalize('NFKC').toLowerCase().trim();
const terms = value => normalize(value).split(/[\s\p{P}\p{S}]+/u).filter(Boolean);

export function searchCapabilities(query, rows = CAPABILITIES) {
    const wanted = terms(query);
    return rows.filter(({id,title,description,group,keywords}) => {
        const text = terms([id,title,description,group,...keywords].join(' ')).join(' ');
        return wanted.every(word => text.includes(word));
    });
}

const SURFACE_NAMES = {
    environment:/^(?:environment|3d environment|room viewer|building viewer)$/,
    'floor-plan':/^(?:the )?floor[ -]?plan (?:tools?|panel|view)$/,
    'height-layers':/^height (?:layers|bands)(?: panel| view)?$/,
    'desk-recording':/^desk recording (?:panel|view|tools)$/,
    browser:/^(?:live |embedded )?(?:web )?browser$/,
    'motion-media':/^motion media(?: tools| panel| view)?$/,
    'tile-workshop':/^tile (?:workshop|editor)$/,
    game:/^(?:game lab|game(?: workspace)?)$/,
    scene:/^(?:3d lab|mechanics lab|scene lab|physics lab)$/,
    spatial:/^(?:spatial|spatial (?:workspace|tools|panel))$/,
    maps:/^(?:maps?|playable maps)$/,
    perception:/^(?:perception|browser perception)$/,
    preview:/^(?:live preview|artifact preview|canvas preview|current artifact)$/,
    source:/^(?:(?:artifact |project )?source(?: editor| code)?|code editor)$/,
    project:/^(?:my |project )?(?:files|artifacts|project library|library)$/,
    data:/^(?:data tasks|data tools|data workspace|datasets)$/,
    world:/^world model$/,
    brain:/^(?:java brain|brain|local controller)$/,
    'media-timeline':/^(?:media|video|editing) timeline$/,
    'sprite-editor':/^(?:single )?sprite editor$/,
    'task-history':/^(?:task |mission |project )?history$/,
    'ramp-input':/^(?:ramp setup|ramp inputs?)$/,
    'relay-review':/^relay (?:mission|review)$/,
    'request-workbench':/^request workbench$/,
    'project-skills':/^(?:my )?(?:(?:saved|verified) )?skills$/,
    activity:/^(?:recent |task |execution )?(?:activity|events|timeline)$/,
    knowledge:/^(?:knowledge|knowledge and tools|knowledge & tools)$/,
};

export function workspaceRequestText(goal) {
    return goal.normalize('NFKC').trim()
        .replace(/^(?:please[,\s]+|(?:can|could|would|will) you\s+(?:please\s+)?)/i, '')
        .replace(/^let me see\s+/i,'show ')
        .replace(/^pull up\s+/i,'open ')
        .replace(/\s+/g,' ');
}

export function matchWorkspaceIntent(goal) {
    const text = normalize(workspaceRequestText(goal)).replace(/[.!?]+$/, '').trim();

    if (/^(?:help|what can (?:you|this app|fraynix) do|what can i (?:ask|do here)|(?:show|list|open) (?:me )?(?:(?:your|the|available) )?(?:capabilities|commands|request examples))$/.test(text)) {
        return 'help';
    }

    if (/^(?:read|inspect|observe|describe|summarize) (?:the |this |my )?(?:(?:current|active|visible|open) )?(?:(?:web|browser) )?page$/.test(text)) {
        return 'browser-read';
    }

    const navigation = /^(open|show|go to|switch to|preview) (?:me )?(?:the )?(.+)$/.exec(text);
    if (!navigation) return null;
    const [,verb,surface] = navigation;
    if (verb === 'open' && /^floor[ -]?plan$/.test(surface)) return 'floor-plan';
    if (verb === 'open' && surface === 'desk recording') return 'desk-recording';
    // Preserve the established measurement and inspection requests.
    if (verb === 'show' && /\b(?:environment|floor[ -]?plan)\b/.test(surface)) return null;
    for (const [id, pattern] of Object.entries(SURFACE_NAMES)) {
        if (pattern.test(surface)) return id;
    }
    return null;
}
