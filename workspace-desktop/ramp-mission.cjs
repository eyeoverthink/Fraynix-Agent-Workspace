'use strict';

function rampPath(value, origin) {
  if (!value) return '';
  const url = new URL(value);
  if (url.origin !== origin || url.search || url.hash) return '';
  return /^\/artifact\/[a-f0-9-]{36}\/[1-9][0-9]{0,8}\/index\.html$/.test(url.pathname) ? url.pathname : '';
}

function rampRequest(goal) {
  return /^run the local ramp experiment(?: and pause after reading)?[.!]?$/i.test(goal.trim());
}

async function observeRamp(surface, origin, path) {
  const url = origin + path;
  if (surface.kind !== 'browser' || rampPath(url, origin) !== path) throw Error('A pinned local ramp input page is required');
  if (surface.view.webContents.getURL() !== url) await surface.navigate(url);
  const observation = await surface.isolated(`(() => {
    const tables = document.querySelectorAll('#ramp-input');
    const rows = [...document.querySelectorAll('#ramp-input tbody > tr')];
    const visible = tables.length === 1 && rows.length === 5 && rows.every(row =>
      row.children.length === 2 && [...row.children].every(cell => {
        const bounds = cell.getBoundingClientRect(), style = getComputedStyle(cell);
        return bounds.width > 0 && bounds.height > 0 && style.visibility === 'visible' && style.opacity !== '0';
      }));
    return {
      url: location.href,
      rows: rows.slice(0, 5).map(row => ({
        field: String(row.dataset.field ?? '').slice(0, 40),
        label: String(row.cells[0]?.innerText ?? '').trim().slice(0, 60),
        value: String(row.cells[1]?.innerText ?? '').trim().slice(0, 40)
      })),
      visible
    };
  })()`);
  if (surface.view.webContents.getURL() !== url || observation.url !== url) {
    throw Error('The browser navigated away from the pinned ramp input');
  }
  return {path, rows: observation.rows, visible: observation.visible};
}

module.exports = {observeRamp, rampPath, rampRequest};
