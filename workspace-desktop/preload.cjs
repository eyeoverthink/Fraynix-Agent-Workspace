'use strict';
const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('workspace',{
  request:input=>ipcRenderer.invoke('workspace:request',input),
  onEvent:callback=>{const handler=(_event,message)=>callback(message);ipcRenderer.on('workspace:event',handler);return ()=>ipcRenderer.removeListener('workspace:event',handler);}
});
