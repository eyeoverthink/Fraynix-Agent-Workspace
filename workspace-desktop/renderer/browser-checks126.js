/** User-selected browser completion criteria; Java freezes these when the request starts. */
export class BrowserChecks126 {
  constructor(){
    const panel=document.createElement('details');panel.id='browser-checks126';
    panel.innerHTML='<summary>Check a browser task</summary><p>Choose the result that must be visible before this browser request can finish. These checks stay fixed during the task.</p><label><input id="browser126-enabled" type="checkbox"> Use these checks for my next request</label><label>Exact result page <input id="browser126-url" type="url" maxlength="2048" placeholder="https://…"></label><button id="browser126-current" type="button">Use current browser address</button><label>Result element <input id="browser126-selector" maxlength="200" value="#result"></label><small>A page selector such as #result must identify one visible element.</small><label>Expected visible text <input id="browser126-text" maxlength="500"></label><small>This checks the page at completion. It does not prove an external service processed a payment, message, or other request.</small>';
    document.querySelector('#settings099 .settings099-body').append(panel);
    panel.querySelector('#browser126-current').onclick=()=>{panel.querySelector('#browser126-url').value=document.querySelector('#address').value;};
    this.panel=panel;
  }
  contract(){
    if(!this.panel.querySelector('#browser126-enabled').checked)return null;
    const url=this.panel.querySelector('#browser126-url').value.trim(),selector=this.panel.querySelector('#browser126-selector').value.trim(),text=this.panel.querySelector('#browser126-text').value;
    const parsed=new URL(url);if(!['http:','https:'].includes(parsed.protocol)||parsed.username||parsed.password||!selector)throw Error('Choose an HTTP(S) page and a result element before starting.');
    return {url:parsed.href,checks:[{selector,text}]};
  }
}
