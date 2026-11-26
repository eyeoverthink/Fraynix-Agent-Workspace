'use strict';

const tools=new Set(['environment.read','environment.probe']);

function vector(value,name) {
    if (!Array.isArray(value)||value.length!==3||value.some(n=>!Number.isFinite(n)||Math.abs(n)>100000))
        throw Error('Provide three finite '+name+' components.');
    return value;
}

function createEnvironmentEvidence({api,measure}) {
    return async (tool,args={})=>{
        if (!tools.has(tool)) throw Error('Unknown environment tool.');
        const allowed=tool==='environment.read'?['start','limit','filter','node']:['origin','direction','maxDistanceM'];
        if (Object.keys(args).some(key=>!allowed.includes(key))) throw Error('Send only supported environment fields.');
        const data=await api('/api/environment/read',{});
        if (!data.id) throw Error('Import an environment GLB first.');
        const {nodes,selected,...environment}=data;
        if (tool==='environment.probe') {
            const origin=vector(args.origin,'origin'), direction=vector(args.direction,'direction');
            if (Math.hypot(...direction)<1e-9) throw Error('Choose a nonzero ray direction.');
            const maxDistanceM=args.maxDistanceM??20;
            if (!Number.isFinite(maxDistanceM)||maxDistanceM<.01||maxDistanceM>500) throw Error('Choose a ray distance from 0.01 to 500 m.');
            const hit=await measure({id:data.id,origin,direction,maxDistanceM});
            return {id:data.id,origin,direction,maxDistanceM,hit,kind:'visual-triangle-ray',units:data.units};
        }
        const filter=(args.filter??'').trim();
        if (filter.length>80) throw Error('Use up to 80 characters when filtering.');
        const start=args.start??0, limit=args.limit??24, node=args.node??selected;
        if (!Number.isInteger(start)||start<0||start>4096||!Number.isInteger(limit)||limit<1||limit>50)
            throw Error('Choose a valid environment page.');
        const filtered=nodes.filter(row=>!filter||(row.name+' '+(row.marker||'')).toLowerCase().includes(filter.toLowerCase()));
        let details=null;
        if (node!==undefined) {
            if (!nodes.some(row=>row.index===node)) throw Error('Choose a node in the saved environment.');
            details=await measure({id:data.id,node});
            if (!details) throw Error('The node was missing from the rendered environment.');
        }
        return {environment,total:nodes.length,matched:filtered.length,start,nodes:filtered.slice(start,start+limit),
            next:start+limit<filtered.length?start+limit:null,details};
    };
}
module.exports={createEnvironmentEvidence};
