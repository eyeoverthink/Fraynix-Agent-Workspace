function showSelectedSections(sections) {
    const targets = {totals:'#visible-totals',chart:'#change-map',rows:'.table-wrap'};
    for (const [section, selector] of Object.entries(targets))
        document.querySelector(selector).hidden = !sections.includes(section);
    document.getElementById('briefing-sections').textContent = 'Included sections · ' + sections.join(' · ');
}
