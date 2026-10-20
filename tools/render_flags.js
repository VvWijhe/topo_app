const { chromium } = require('/home/claude/.npm-global/lib/node_modules/@playwright/mcp/node_modules/playwright-core');
const fs = require('fs'); const path = require('path');
const [svgDir, outDir, codesFile] = process.argv.slice(2);
const H = 320;
(async () => {
  const browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell' });
  const page = await browser.newPage({ deviceScaleFactor: 1 });
  const codes = fs.readFileSync(codesFile, 'utf8').trim().split(/\s+/);
  const missing = [];
  for (const c of codes) {
    const f = path.join(svgDir, c + '.svg');
    if (!fs.existsSync(f)) { missing.push(c); continue; }
    const svg = fs.readFileSync(f, 'utf8');
    await page.setContent(`<html><body style="margin:0;background:transparent"><img id="i" style="display:block;height:${H}px" src="data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}"></body></html>`);
    const img = await page.$('#i');
    await page.waitForFunction(() => document.getElementById('i').complete);
    const box = await img.boundingBox();
    await page.setViewportSize({ width: Math.ceil(box.width), height: H });
    await img.screenshot({ path: path.join(outDir, c + '.png'), omitBackground: true });
  }
  console.log('missing', missing.join(' '));
  await browser.close();
})();
