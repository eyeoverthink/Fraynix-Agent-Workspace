'use strict';
function localURL(value){try{const u=new URL(value);return ['http:','https:','ws:','wss:'].includes(u.protocol)&&!u.username&&!u.password&&['localhost','127.0.0.1','[::1]'].includes(u.hostname);}catch{return false;}}
class NetworkPolicy {
 constructor(enabled){this.enabled=enabled;this.blocked=0;}
 allow(url){return !this.enabled||localURL(url)||url.startsWith('data:')||url==='about:blank';}
 require(url){if(!this.allow(url)){this.blocked++;throw Error('Offline mode blocks external browser requests; use a loopback URL or restart without -Offline');}}
 status(){return {enabled:this.enabled,blockedRequests:this.blocked,scope:'Application browser requests and fresh local-model metadata admission; not an OS-wide network sandbox or a policy on the shared Ollama process'};}
}
module.exports={NetworkPolicy,localURL};
