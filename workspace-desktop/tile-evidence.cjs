'use strict';

const tools=new Set(['tile.level','tile.ticks','tile.image','tile.pixels','tile.replay','tile.comparison']);
const hash=/^[a-f0-9]{64}$/;

function integer(value,min,max,name) {
    if (!Number.isInteger(value)||value<min||value>max) throw Error('Choose a valid '+name+'.');
    return value;
}

function createTileEvidence({api,surface,save,rendererSha256,legacyRendererSha256,legacyRaster=false}) {
    const call=(operation,args={})=>api('/api/media-tiles/'+operation,args);
    async function selectedLevel() {
        const id=await surface('levelId');
        if (id) return call('inspect',{id});
        const latest=await call('open');
        if (!latest.id) throw Error('Save a tile level first.');
        return latest;
    }

    async function ticks(args) {
        const start=integer(args.start??0,0,7200,'starting tick');
        const limit=integer(args.limit??12,1,60,'tick page size');
        if (args.runId===undefined) {
            const live=await surface('live',{start,limit});
            if (live&&!live.sealed) return {...live,live:true,rendererSha256};
            await surface('settle');
        }
        const record=await call('read-run',{start,limit,...(args.runId?{runId:args.runId}:{})});
        if (!record.id) throw Error('Play a tile level to record its ticks.');
        return record;
    }

    async function replay(args) {
        const source=await call('export-run',args.runId?{runId:args.runId}:{});
        const legacy=source.rendererSha256===legacyRendererSha256;
        if (source.format!=='fraynix.tile-playtest-v2' || (!legacy&&source.rendererSha256!==rendererSha256))
            throw Error('Record a playtest with a supported input-recording renderer to compare controls.');
        const jump=integer(args.jumpTick,1,source.lastTick,'recorded jump tick');
        const replacement=integer(args.replacementTick??0,0,source.lastTick,'replacement tick');
        if (!source.rows[jump][16] || replacement && (Math.abs(replacement-jump)>30||source.rows[replacement][16]))
            throw Error('Choose a recorded jump and a free replacement tick within 30 steps.');
        const inputs=source.rows.slice(1).map(row=>[row[18],row[16]]);
        const level=await call('inspect',{id:source.levelId});
        if (legacy&&level.layout.player) throw Error('The saved renderer predates animated players.');
        const verified=await surface('replay',{level,inputs,legacy});
        if (verified.reason!==source.reason || JSON.stringify(verified.rows)!==JSON.stringify(source.rows))
            throw Error('The unchanged inputs did not reconstruct the exact saved playtest.');
        inputs[jump-1][1]=false;
        if (replacement) inputs[replacement-1][1]=true;
        const result=await surface('replay',{level,inputs,legacy});
        const variant=await call('save-variant',{...result,rendererSha256:source.rendererSha256});
        return call('compare-run',{sourceId:source.id,variantId:variant.id,jumpTick:jump,replacementTick:replacement});
    }

    return {
        async read(tool,args={}) {
            if (!tools.has(tool)) throw Error('Unknown tile evidence tool.');
            const kinds={
                'tile.level':['start','limit'],
                'tile.replay':['runId','jumpTick','replacementTick'],
                'tile.comparison':['id'],
                'tile.ticks':['runId','start','limit'],
                'tile.image':['runId','tick','assetId'],
                'tile.pixels':['runId','tick','assetId','x','y','width','height']
            };
            if (Object.keys(args).some(key=>!kinds[tool].includes(key))) throw Error('Send only fields for this tile evidence tool.');
            for (const key of ['runId','assetId','id'])
                if (args[key]!==undefined && (typeof args[key]!=='string'||!hash.test(args[key]))) throw Error('Choose a verified '+key+'.');
            if (tool==='tile.level') {
                const {id,layout,sprites}=await selectedLevel();
                const start=integer(args.start??0,0,390,'object offset');
                const limit=integer(args.limit??12,1,24,'object page size');
                const {objects,...settings}=layout;
                return {id,settings,total:objects.length,start,objects:objects.slice(start,start+limit),
                    next:start+limit<objects.length?start+limit:null,width:720,height:336,tileSize:24,tickHz:120,
                    units:{position:'px',velocity:'px/s',animationTime:'s'},
                    assets:Object.fromEntries(Object.entries(sprites).map(([key,asset])=>[key,
                        {width:asset.width,height:asset.height,sha256:asset.sha256,label:asset.label}]))};
            }
            if (tool==='tile.ticks') return ticks(args);
            if (tool==='tile.replay') return replay(args);
            if (tool==='tile.comparison') return call('read-comparison',args);
            const region=tool==='tile.pixels'?{
                x:integer(args.x,0,1023,'pixel x'),y:integer(args.y,0,1023,'pixel y'),
                width:integer(args.width??1,1,16,'sample width'),height:integer(args.height??1,1,16,'sample height')
            }:undefined;
            let level,row,runId,tick,legacy=false;
            if (args.assetId) {
                if (args.runId!==undefined || args.tick!==undefined) throw Error('Choose either a level asset or a playtest frame.');
                level=await selectedLevel();
                if (!Object.hasOwn(level.sprites,args.assetId)) throw Error('The asset is not in this saved level.');
            } else {
                tick=integer(args.tick,0,7200,'tick');
                const page=await ticks({start:tick,limit:1,runId:args.runId});
                [row]=page.rows;
                if (!row || row[0]!==tick) throw Error('This playtest has no such tick.');
                runId=page.id;
                legacy=page.rendererSha256===legacyRendererSha256 || (legacyRaster&&
                    page.rendererSha256==='326ef4a6e5f9737a7d7b3fff8454279f33b52deb23c72d4b97583e60e40e2522');
                if (!legacy&&page.rendererSha256!==rendererSha256)
                    throw Error('Use the recorded renderer version to reconstruct this playtest image.');
                level=await call('inspect',{id:page.levelId});
                if (legacy&&level.layout.player) throw Error('The saved renderer predates animated players.');
            }
            const result=await surface('render',{level,row,region,assetId:args.assetId,legacy});
            const source={levelId:level.id,...(args.assetId?{assetId:args.assetId}:{tick,...(runId?{runId}:{live:true})})};
            if (region) return {...source,...result,order:'row-major RGBA8 from top-left'};
            const {png,width,height}=result;
            const file=await save('tile-evidence-image','png',Buffer.from(png,'base64'),source,
                args.assetId?level.sprites[args.assetId].sha256:undefined);
            return {...source,width,height,file};
        }
    };
}
module.exports={createTileEvidence};
