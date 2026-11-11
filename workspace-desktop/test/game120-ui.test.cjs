'use strict';
// UI transition fixtures execute the real component methods. Native outcomes have separate integration tests.
const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
function fixture(call){
 const nodes={'game-status':{textContent:''},'game-frame':{hidden:false},'game-view':{hidden:false},'brain-dialog':{open:false}};
 const source=fs.readFileSync(path.join(__dirname,'../renderer/brain-game.js'),'utf8').replace(/^import .*;\r?\n/gm,'').replace('export class BrainGame','return class BrainGame');
 const BrainGame=new Function('document',source)({getElementById:id=>nodes[id]});const game=Object.create(BrainGame.prototype);Object.assign(game,{call,notice(text){this.noticeText=text;},requireSaved(){},activate(){},show(frame){this.frame=frame;this.running=true;},running:false,watching:false,polling:false});return{game,nodes};
}
test('resume/open ownership race waits for the owner frame without retrying mutation',async()=>{
 let opens=0,ready=false;const f=fixture(async(op,command)=>{if(command==='state')return{running:ready};if(command==='list')return{tasks:[{status:'interrupted'}]};if(command==='open'){opens++;throw Error('The active Java task owns execution; this request has no current action authority');}if(command==='frame')return{tick:12};throw Error(command);});
 await f.game.openIntegrated();assert.match(f.nodes['game-status'].textContent,/waiting for its next frame/);assert.equal(opens,1);ready=true;await f.game.poll();assert.equal(f.game.frame.tick,12);assert.equal(opens,1);
});
test('closing a native trial between frames returns to observation then displays the next trial',async()=>{
 let ready=false;const f=fixture(async(op,command)=>{if(command==='state')return{running:ready};if(command==='frame'){if(!ready)throw Error('Open embedded Game first');return{tick:0};}throw Error('No mutation allowed');});f.game.running=true;f.game.watching=true;await f.game.poll();assert.equal(f.game.running,false);assert.match(f.nodes['game-status'].textContent,/Waiting/);ready=true;await f.game.poll();assert.equal(f.game.frame.tick,0);
});
test('unexpected open and frame failures remain visible instead of becoming waiting',async()=>{
 const f=fixture(async(op,command)=>{if(command==='state')return{running:false};if(command==='list')return{tasks:[]};throw Error('Source verification failed');});await assert.rejects(f.game.openIntegrated(),/Source verification failed/);f.game.running=true;await assert.rejects(f.game.poll(),/Source verification failed/);assert.equal(f.game.polling,false);
});
