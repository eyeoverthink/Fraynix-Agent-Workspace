import * as THREE from '../node_modules/three/build/three.module.js';
import {GLTFLoader} from '../node_modules/three/examples/jsm/loaders/GLTFLoader.js';
import {OrbitControls} from '../node_modules/three/examples/jsm/controls/OrbitControls.js';
import {readBase64} from './media-workshop.js';
import {FloorPlanPanel} from './floor-plan.js';
import {HeightLayersPanel} from './height-layers.js';
import {DeskRecordingPanel} from './desk-recording.js';

const MAX_BYTES=16*1024*1024;
const measured=vector=>vector.toArray().map(value=>Object.is(value,-0)?0:value);

export class EnvironmentPanel {
    constructor({request,activate}) {
        this.request=request;
        this.activate=activate;
        this.root=document.createElement('section');
        this.root.id='environment-view';
        this.root.hidden=true;
        this.root.innerHTML=`
          <div class="environment-toolbar">
            <label class="spatial-file">Import environment GLB<input id="environment-file" type="file" accept=".glb"></label>
            <button id="environment-fit" disabled>Fit scene</button>
            <button id="environment-focus" disabled>Focus selection</button>
            <button id="environment-export" disabled>Export inventory JSON</button>
            <label><input id="environment-cut" type="checkbox" disabled>Section view</label>
            <label>Height · m <input id="environment-cut-height" type="range" disabled step="0.01"></label>
          </div>
          <p id="environment-status" role="status">Open a self-contained Blender or glTF environment.</p>
          <div class="environment-layout">
            <div id="environment-stage" aria-label="3D environment viewport"></div>
            <aside>
              <label>Find an object or marker<input id="environment-filter" type="search" autocomplete="off"></label>
              <label><input id="environment-markers" type="checkbox">Interaction markers</label>
              <div id="environment-nodes" aria-label="Scene objects"></div>
              <dl id="environment-properties"></dl>
            </aside>
          </div>
          <details id="environment-plans"></details>
          <details id="environment-height-layers"></details>
          <details id="environment-desk"></details>`;
        document.querySelector('#canvas-surface').before(this.root);
        const tab=document.createElement('button');
        tab.id='environment-tab';
        tab.textContent='Environment';
        document.querySelector('#spatial-tab').after(tab);
        tab.onclick=()=>this.activate('environment');
        this.floorPlan=new FloorPlanPanel(this);
        this.heightLayers=new HeightLayersPanel(this);
        this.desk=new DeskRecordingPanel(this);
        this.filter=this.find('environment-filter');
        this.markers=this.find('environment-markers');
        this.filter.oninput=this.markers.onchange=()=>this.list();
        this.find('environment-fit').onclick=()=>this.fit(this.box);
        this.find('environment-focus').onclick=()=>this.focus();
        this.find('environment-export').onclick=()=>this.run(async()=>{
            const result=await this.call('export-inventory',{id:this.catalog.id});
            this.tell('Saved inventory to '+result.path);
        });
        for (const field of ['environment-cut','environment-cut-height'])
            this.find(field).oninput=()=>this.section();
        this.find('environment-file').onchange=event=>{
            const input=event.target, file=input.files[0];
            input.value='';
            if (file) Promise.resolve(this.pending).then(()=>this.run(async()=>{
                this.tell('Checking the GLB and embedded images…');
                const info=await this.call('import',{filename:file.name,base64:await readBase64(file,MAX_BYTES)});
                await this.load(info.id,true);
            }));
        };
    }

    find(id) {return this.root.querySelector('#'+id);}
    tell(text) {this.find('environment-status').textContent=text;}
    call(command,args={}) {return this.request('environment',{command,args});}

    show(visible) {
        this.root.hidden=!visible;
        if (visible && !this.catalog) this.run(()=>this.load());
        if (visible) this.resize();
    }

    initialize() {
        if (this.scene) return;
        const stage=this.find('environment-stage');
        this.scene=new THREE.Scene();
        try {
            this.renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
        } catch {
            stage.dataset.graphics='geometry';
            const note=document.createElement('p');
            note.className='environment-render-note';
            note.textContent='3D viewport unavailable on this display. Object measurements, floor plans and GLB export are available.';
            stage.append(note);
            return;
        }
        stage.dataset.graphics='webgl';
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio||1,1.5));
        this.renderer.outputColorSpace=THREE.SRGBColorSpace;
        this.renderer.setClearColor(0x17272e);
        stage.prepend(this.renderer.domElement);
        this.scene.add(new THREE.HemisphereLight(0xe8f4ff,0x726156,2));
        const key=new THREE.DirectionalLight(0xffeedc,1.7);
        key.position.set(8,13,6);
        this.scene.add(key);
        this.camera=new THREE.PerspectiveCamera(47,1,.02,1000);
        this.camera.position.set(4,3,5);
        this.controls=new OrbitControls(this.camera,this.renderer.domElement);
        this.controls.addEventListener('change',()=>this.draw());
        const canvas=this.renderer.domElement;
        canvas.addEventListener('pointerdown',event=>{this.pointer=[event.clientX,event.clientY];});
        canvas.addEventListener('pointerup',event=>{
            if (!this.catalog || !this.pointer || Math.hypot(event.clientX-this.pointer[0],event.clientY-this.pointer[1])>4) return;
            const rect=canvas.getBoundingClientRect();
            const ray=new THREE.Raycaster();
            ray.setFromCamera(new THREE.Vector2((event.clientX-rect.left)/rect.width*2-1,(rect.bottom-event.clientY)/rect.height*2-1),this.camera);
            const picked=this.hits(ray,true)[0];
            if (picked) this.run(()=>this.select(picked.node));
        });
        this.observer=new ResizeObserver(()=>this.resize());
        this.observer.observe(stage);
    }

    async load(id,publish=false) {
        const {base64,...catalog}=await this.call('open',id?{id}:{});
        if (!catalog.id) return;
        if (catalog.id===this.catalog?.id) {
            if (publish) await this.call('activate',{id:catalog.id});
            return;
        }
        this.initialize();
        const bytes=Uint8Array.from(atob(base64),character=>character.charCodeAt(0));
        const loader=new GLTFLoader();
        // The shell CSP allows local blob images and keeps network fetch disabled.
        loader.register(parser=>({
            name:'FraynixEmbeddedImages',
            beforeRoot(){parser.textureLoader=new THREE.TextureLoader(parser.options.manager);}
        }));
        const gltf=await loader.parseAsync(bytes.buffer,'');
        const textures=await gltf.parser.getDependencies('texture');
        if (textures.length!==catalog.textureCount || textures.some(texture=>!texture?.isTexture))
            throw Error('One or more embedded environment textures did not decode.');
        const next=gltf.scene;
        const nodes=new Map();
        const identities=new WeakMap();
        const catalogRows=new Map(catalog.nodes.map(row=>[row.index,row]));
        next.traverse(object=>{
            const index=gltf.parser.associations.get(object)?.nodes;
            if (catalogRows.has(index)) {
                nodes.set(index,object);
                identities.set(object,index);
            }
        });
        if (nodes.size!==catalog.nodes.length) throw Error('The displayed hierarchy did not match the GLB node catalog.');
        next.updateMatrixWorld(true);
        const box=new THREE.Box3().setFromObject(next,true);
        if (box.isEmpty()||![...box.min.toArray(),...box.max.toArray()].every(Number.isFinite)) throw Error('The environment has invalid bounds.');
        if (publish) await this.call('activate',{id:catalog.id});
        this.releaseModel();
        this.model=next;
        this.nodes=nodes;
        this.identities=identities;
        this.catalog=catalog;
        this.rows=catalogRows;
        this.box=box;
        this.scene.add(next);
        this.find('environment-fit').disabled=!this.renderer;
        this.find('environment-export').disabled=false;
        const slider=this.find('environment-cut-height');
        slider.min=box.min.y;
        slider.max=box.max.y;
        slider.value=box.min.y+(box.max.y-box.min.y)*.62;
        this.find('environment-cut').disabled=!this.renderer;
        this.find('environment-cut').checked=false;
        this.section();
        this.selected=catalog.selected;
        this.list();
        this.properties();
        this.floorPlan.bind();
        this.heightLayers.bind(catalog);
        this.onCatalog?.(catalog);
        this.fit(box);
        this.resize();
        this.tell(catalog.name+' · '+catalog.nodes.length+' objects · '+catalog.imageCount+' images · '+catalog.markerCount+' interaction markers');
    }

    releaseModel() {
        if (!this.model) return;
        const textures=new Set(), materials=new Set(), buffers=new Set();
        this.model.traverse(object=>{
            if (object.geometry) buffers.add(object.geometry);
            for (const material of [object.material].flat().filter(Boolean)) {
                materials.add(material);
                for (const value of Object.values(material)) if (value?.isTexture) textures.add(value);
            }
        });
        for (const resource of [...textures,...materials,...buffers]) resource.dispose();
        this.scene.remove(this.model);
    }

    resize() {
        if (!this.renderer || this.root.hidden) return;
        const stage=this.find('environment-stage');
        const width=stage.clientWidth, height=stage.clientHeight;
        if (!width||!height) return;
        this.renderer.setSize(width,height);
        this.camera.aspect=width/height;
        this.camera.updateProjectionMatrix();
        this.draw();
    }

    draw() {
        if (this.renderer && !this.root.hidden) this.renderer.render(this.scene,this.camera);
    }

    fit(box) {
        if (!this.camera || !box || box.isEmpty()) return;
        const center=box.getCenter(new THREE.Vector3());
        const sphere=box.getBoundingSphere(new THREE.Sphere());
        const distance=Math.max(1,sphere.radius*2.9);
        this.camera.position.copy(center).addScaledVector(new THREE.Vector3(1,.8,1).normalize(),distance);
        this.camera.near=Math.min(.02,distance/1500);
        this.camera.far=Math.max(100,distance*25);
        this.camera.updateProjectionMatrix();
        this.controls.target.copy(center);
        this.controls.update();
        this.draw();
    }

    section() {
        const on=this.find('environment-cut').checked;
        const slider=this.find('environment-cut-height');
        slider.disabled=!on;
        this.cut=on?slider.valueAsNumber:null;
        if (!this.renderer) return;
        this.renderer.clippingPlanes=on?[new THREE.Plane(new THREE.Vector3(0,-1,0),this.cut)]:[];
        this.draw();
    }

    focus() {
        const object=this.nodes?.get(this.selected);
        if (!object) return;
        const box=new THREE.Box3().setFromObject(object,true);
        if (box.isEmpty()) {
            const position=object.getWorldPosition(new THREE.Vector3());
            box.setFromCenterAndSize(position,new THREE.Vector3(.7,.7,.7));
        }
        this.fit(box);
    }

    list() {
        if (!this.catalog) return;
        const key=this.filter.value.trim().toLocaleLowerCase();
        const nodes=this.catalog.nodes.filter(row=>(!this.markers.checked||row.marker) &&
            (!key||(row.name+' '+(row.marker||'')).toLocaleLowerCase().includes(key)));
        this.find('environment-nodes').replaceChildren(...nodes.map(row=>{
            const button=document.createElement('button');
            button.type='button';
            button.dataset.environmentNode=row.index;
            button.textContent=row.marker?'◆ '+row.marker:row.name;
            button.title=row.name;
            button.setAttribute('aria-pressed',String(this.selected===row.index));
            button.onclick=()=>this.run(()=>this.select(row.index));
            return button;
        }));
    }

    async select(index) {
        const changed=await this.call('select',{id:this.catalog.id,node:index});
        this.selected=changed.node;
        this.list();
        this.properties();
    }

    node(index) {
        const row=this.rows?.get(index);
        const object=this.nodes?.get(index);
        if (!row||!object) return null;
        const vector=measured;
        const box=new THREE.Box3().setFromObject(object,true);
        const position=vector(object.getWorldPosition(new THREE.Vector3()));
        const dimensions=box.isEmpty()?null:vector(box.getSize(new THREE.Vector3()));
        return {...row,position,dimensions,bounds:box.isEmpty()?null:{min:vector(box.min),max:vector(box.max)}};
    }

    properties() {
        const selected=this.node(this.selected);
        this.find('environment-focus').disabled=!this.renderer||!selected;
        const fmt=value=>value?.map(number=>number.toFixed(3)).join(', ');
        const properties=this.find('environment-properties');
        properties.replaceChildren();
        if (!selected) return;
        for (const [key,value] of [
            ['Object',selected.name],['Position · m',fmt(selected.position)],['Bounds size · m',fmt(selected.dimensions)],
            ['Marker',selected.marker],['Triangles',selected.triangles]
        ]) if (value!==undefined) {
            const label=document.createElement('dt'), data=document.createElement('dd');
            label.textContent=key;data.textContent=value;
            properties.append(label,data);
        }
    }

    hits(ray,visible) {
        return ray.intersectObject(this.model,true).filter(hit=>!visible||this.cut===null||hit.point.y<=this.cut)
            .map(hit=>{
                let node=hit.object;
                while (node&&!this.identities.has(node)) node=node.parent;
                return node?{node:this.identities.get(node),distanceM:hit.distance,point:hit.point.toArray()}:null;
            }).filter(Boolean);
    }

    async measure({id,node,origin,direction,maxDistanceM=20,inventory=false}) {
        if (id!==this.catalog?.id) await this.pending;
        if (id!==this.catalog?.id) await this.load(id);
        if (inventory) return this.catalog.nodes.map(row=>this.node(row.index));
        if (origin) {
            const ray=new THREE.Raycaster(new THREE.Vector3(...origin),new THREE.Vector3(...direction).normalize(),.00001,maxDistanceM);
            const hit=this.hits(ray,false)[0];
            return hit?{...hit,name:this.rows.get(hit.node).name}:null;
        }
        return this.node(node);
    }

    run(task) {
        if (this.pending) return this.pending;
        this.pending=Promise.resolve().then(task).catch(error=>this.tell(error.message)).finally(()=>{this.pending=null;});
        return this.pending;
    }
}
