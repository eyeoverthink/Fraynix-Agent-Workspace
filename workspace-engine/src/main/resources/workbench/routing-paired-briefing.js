async function readChapter(chapter) {
    const decoder = new TextDecoder('utf-8',{fatal:true});
    const csv = bytes(chapter.csv);
    const receiptBytes = bytes(chapter.receipt);
    const summaryBytes = bytes(chapter.proof);
    const receipt = JSON.parse(decoder.decode(receiptBytes));
    const summary = JSON.parse(decoder.decode(summaryBytes));
    const [csvHash,summaryHash] = await Promise.all([sha(csv),sha(summaryBytes)]);
    const binding = chapter.binding;
    if (csvHash !== binding.sourceSha256 || receipt.sha256 !== csvHash || csv.length !== receipt.bytes ||
        receipt.id !== binding.exportId || summaryHash !== chapter.proofSha256 ||
        JSON.stringify(summary.import) !== JSON.stringify(receipt) || summary.passed !== true)
        throw Error('The comparison and proof disagree');
    const rows = csvRows(decoder.decode(csv));
    const header = ['key_json','unit','status','earlier_readings','later_readings','delta'];
    const actual = rows.shift();
    if (!actual || actual.length !== header.length || !actual.every((value,i)=>value===header[i]) ||
        rows.length !== receipt.source.groups)
        throw Error('The comparison columns or group count changed');
    countsByUnit(rows);
    return {rows,csv,receiptBytes,summaryBytes,binding,summaryHash};
}

function briefingDownload(id, name, data, type) {
    const link = document.getElementById(id);
    if (link.href.startsWith('blob:')) URL.revokeObjectURL(link.href);
    link.href = URL.createObjectURL(new Blob([data],{type}));
    link.download = name;
}

async function render() {
    const payload = /*@BRIEFING_SOURCES@*/ [];
    if (payload.length !== 2) throw Error('Two comparison sources are required');
    const tabs = [document.querySelector('#brief-source-1'),document.querySelector('#brief-source-2')];
    const chapters = await Promise.all(payload.map(async (source,index)=>{
        try { return await readChapter(source); }
        catch (error) { throw Error('Source '+(index+1)+': '+error.message); }
    }));
    function select(index) {
        const source = chapters[index];
        const prefix = 'source-'+(index+1)+'-';
        document.getElementById('source-id').textContent = 'Source '+(index+1)+' · Export '+source.binding.exportId;
        document.getElementById('source-hash').textContent = 'CSV SHA-256 '+source.binding.sourceSha256+
            ' · Summary SHA-256 '+source.summaryHash;
        briefingDownload('briefing-download-csv',prefix+'comparison.csv',source.csv,'text/csv');
        briefingDownload('briefing-download-receipt',prefix+'receipt.json',source.receiptBytes,'application/json');
        briefingDownload('briefing-download-summary',prefix+'summary.json',source.summaryBytes,'application/json');
        installFilters(source.rows);
        tabs.forEach((tab,i)=>tab.setAttribute('aria-pressed',String(index===i)));
    }
    tabs.forEach((tab,index)=>{tab.onclick=()=>select(index);});
    select(0);
}
