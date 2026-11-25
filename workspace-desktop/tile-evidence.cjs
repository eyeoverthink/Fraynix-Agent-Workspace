'use strict';

const tools=new Set(['tile.level','tile.ticks','tile.image','tile.pixels']);
const hash=/^[a-f0-9]{64}$/;

function integer(value,min,max,name) {
    if (!Number.isInteger(value)||value<min||value>max) throw Error('Choose a valid '+name+'.');
    return value;
}

function createTileEvidence({api,surface,save,rendererSha256}) {
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

    return {
        async read(tool,args={}) {
            if (!tools.has(tool)) throw Error('Unknown tile evidence tool.');
            const kinds={
                'tile.level':['start','limit'],
                'tile.ticks':['runId','start','limit'],
                'tile.image':['runId','tick','assetId'],
                'tile.pixels':['runId','tick','assetId','x','y','width','height']
            };
            if (Object.keys(args).some(key=>!kinds[tool].includes(key))) throw Error('Send only fields for this tile evidence tool.');
            for (const key of ['runId','assetId'])
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
            const region=tool==='tile.pixels'?{
                x:integer(args.x,0,1023,'pixel x'),y:integer(args.y,0,1023,'pixel y'),
                width:integer(args.width??1,1,16,'sample width'),height:integer(args.height??1,1,16,'sample height')
            }:undefined;
            let level,row,runId,tick;
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
                if (page.rendererSha256!==rendererSha256)
                    throw Error('Use the recorded renderer version to reconstruct this playtest image.');
                level=await call('inspect',{id:page.levelId});
            }
            const result=await surface('render',{level,row,region,assetId:args.assetId});
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
