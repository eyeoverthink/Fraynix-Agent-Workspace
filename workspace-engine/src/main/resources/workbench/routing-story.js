const changeMap = document.getElementById('change-map');
const emptyChanges = 'No matched changes in this view.';
const selectChange = 'Select a bar to inspect both source readings.';

function changeChart(allRows) {
    const maximum = new Map();
    for (const row of allRows) {
        if (row[2] !== 'Matched') continue;
        const delta = number(row[5]);
        const magnitude = delta < 0n ? -delta : delta;
        if (magnitude > (maximum.get(row[1]) ?? 0n)) maximum.set(row[1],magnitude);
    }
    return function renderChanges(visible) {
        const groups = new Map([...units].map(unit=>[unit,[]]));
        for (const row of visible) if (row[2] === 'Matched') groups.get(row[1]).push(row);
        const detail = changeMap.querySelector('#change-selection');
        const title = changeMap.querySelector('#change-selection-title');
        const readings = changeMap.querySelector('#change-selection-readings');
        const lanes = [];
        let first = true;
        let selected;
        title.textContent = selectChange;
        readings.textContent = '';
        for (const [unit,matched] of groups) {
            if (!matched.length) continue;
            const largest = maximum.get(unit) ?? 0n;
            const lane = node('section','');
            lane.className = 'change-lane';
            lane.dataset.unit = unit;
            const axis = node('div','');
            axis.className = 'change-axis';
            const range = largest === 0n ? ['0 '+unit] :
                [display(-largest,false,unit,true),'0',display(largest,true,unit,true)];
            axis.append(...range.map(label=>node('span',label)));
            lane.append(node('h3',unit),axis);
            for (const row of matched) {
                const key = visibleKey(JSON.parse(row[0]));
                const delta = number(row[5]);
                const magnitude = delta < 0n ? -delta : delta;
                const width = largest === 0n ? 0 : Number(magnitude * 10000n / largest);
                const direction = delta < 0n ? 'loss' : delta > 0n ? 'gain' : 'steady';
                const amount = display(delta,true,unit,true);
                const button = node('button','');
                button.type = 'button';
                button.className = 'change-bar';
                button.dataset.direction = direction;
                button.dataset.width = String(width);
                if (first) button.dataset.first = '';
                first = false;
                button.setAttribute('aria-pressed','false');
                button.setAttribute('aria-controls',detail.id);
                const track = node('span','');
                track.className = 'change-track';
                track.setAttribute('aria-hidden','true');
                const mark = node('span','');
                mark.className = 'change-mark';
                mark.style.setProperty('--bar',(width/200)+'%');
                track.append(mark);
                const label = node('span',amount);
                label.className = 'change-delta';
                button.append(node('span',key),label,track);
                button.onclick = () => {
                    selected?.setAttribute('aria-pressed','false');
                    selected = button;
                    button.setAttribute('aria-pressed','true');
                    title.textContent = key + ' · ' + amount;
                    readings.textContent = 'Earlier '+readingText(row[3])+' | Later '+readingText(row[4]);
                };
                lane.append(button);
            }
            lanes.push(lane);
        }
        changeMap.querySelector('#change-bars').replaceChildren(...lanes);
        changeMap.querySelector('#change-empty').textContent = lanes.length ? '' : emptyChanges;
        detail.hidden = lanes.length === 0;
    };
}
