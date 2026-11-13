// Shared by every page: a "full screen" button.
// Android / computer / iPad: real browser full screen (and it comes back on the next page at the first tap).
// iPhone: the browser does not allow full screen for pages, so we show how to add the site to the home screen,
// where it opens like an app with no address bar and no tabs.
(function () {
  const mm = q => window.matchMedia && matchMedia(q).matches;
  const standalone = mm('(display-mode: standalone)') || mm('(display-mode: fullscreen)') || navigator.standalone === true;
  if (standalone) return;                       // already running like an app

  const root = document.documentElement;
  const canFs = !!(root.requestFullscreen || root.webkitRequestFullscreen);
  const isIOS = /iPhone|iPod/.test(navigator.userAgent) || (/iPad|Macintosh/.test(navigator.userAgent) && 'ontouchend' in document && !canFs);
  const isFs = () => !!(document.fullscreenElement || document.webkitFullscreenElement);
  const KEY = 'kids-fullscreen';
  const want = v => { try { if (v === undefined) return sessionStorage.getItem(KEY) === '1'; v ? sessionStorage.setItem(KEY, '1') : sessionStorage.removeItem(KEY); } catch { return false; } };

  function enter() {
    const p = root.requestFullscreen ? root.requestFullscreen({ navigationUI: 'hide' }) : root.webkitRequestFullscreen();
    if (p && p.catch) p.catch(() => {});
  }
  function exit() { (document.exitFullscreen || document.webkitExitFullscreen).call(document); }

  // ---------- styles ----------
  const css = document.createElement('style');
  css.textContent = `
    .fs-btn { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; font-size: 0.85rem; font-weight: 800; border: none; cursor: pointer;
      padding: 4px 12px; border-radius: 999px; background: rgba(255,255,255,0.92); color: #5a3fd1; box-shadow: 0 3px 0 rgba(0,0,0,0.18);
      margin-inline-start: auto; white-space: nowrap; touch-action: manipulation; }
    .fs-btn:active { transform: translateY(2px); box-shadow: 0 1px 0 rgba(0,0,0,0.18); }
    .fs-float { position: fixed; top: 10px; left: 10px; z-index: 40; }
    .fs-help { position: fixed; inset: 0; z-index: 1000; background: rgba(20,20,40,0.55); display: flex; align-items: flex-end; justify-content: center; }
    .fs-card { direction: rtl; background: #fff; color: #222; width: min(100%, 420px); border-radius: 22px 22px 0 0; padding: 18px 18px 26px;
      font-family: 'Comic Sans MS', 'Segoe UI', Tahoma, Arial, sans-serif; box-shadow: 0 -10px 30px rgba(0,0,0,0.3); animation: fsUp .25s ease; }
    @keyframes fsUp { from { transform: translateY(40px); opacity: 0; } }
    .fs-card h3 { font-size: 1.2rem; margin: 0 0 4px; text-align: center; }
    .fs-card p { color: #666; font-size: 0.9rem; text-align: center; margin: 0 0 12px; }
    .fs-step { display: flex; align-items: center; gap: 10px; background: #f3f0ff; border-radius: 14px; padding: 9px 12px; margin: 7px 0; font-size: 0.98rem; }
    .fs-step b.n { background: #7c5cff; color: #fff; border-radius: 50%; width: 26px; height: 26px; display: grid; place-items: center; flex: none; }
    .fs-step .ic { font-size: 1.3rem; margin-inline-start: auto; }
    .fs-close { display: block; margin: 14px auto 0; border: none; background: #7c5cff; color: #fff; font: inherit; font-weight: bold; font-size: 1rem;
      padding: 9px 30px; border-radius: 999px; box-shadow: 0 4px 0 #5a3fd1; cursor: pointer; }
  `;
  document.head.appendChild(css);

  // ---------- how to add to the home screen ----------
  function help() {
    const box = document.createElement('div');
    box.className = 'fs-help';
    const steps = isIOS
      ? [['לחצו על <b>•••</b> או על כפתור השיתוף בתחתית המסך', '⬆️'], ['בחרו <b>"שיתוף"</b> ואז <b>"הוספה למסך הבית"</b>', '➕'], ['לחצו <b>"הוסף"</b>, ופתחו את המשחקים מהאייקון החדש 🌈', '📱']]
      : [['פתחו את תפריט הדפדפן <b>⋮</b>', '⋮'], ['בחרו <b>"הוספה למסך הבית"</b> או <b>"התקנת אפליקציה"</b>', '➕'], ['פתחו את המשחקים מהאייקון החדש 🌈', '📱']];
    box.innerHTML = `<div class="fs-card">
        <h3>📱 משחקים במסך מלא</h3>
        <p>${isIOS ? 'באייפון מוסיפים את האתר למסך הבית, ואז הוא נפתח כמו אפליקציה, בלי שורת כתובת ובלי טאבים:' : 'כדי שהמסך המלא יישאר תמיד, הוסיפו את האתר למסך הבית:'}</p>
        ${steps.map(([t, ic], i) => `<div class="fs-step"><b class="n">${i + 1}</b><span>${t}</span><span class="ic">${ic}</span></div>`).join('')}
        <button class="fs-close">הבנתי 👍</button></div>`;
    const close = () => box.remove();
    box.addEventListener('click', e => { if (e.target === box) close(); });
    box.querySelector('.fs-close').addEventListener('click', close);
    document.body.appendChild(box);
  }

  // ---------- the button ----------
  const btn = document.createElement('button');
  btn.className = 'fs-btn';
  const label = () => { btn.textContent = isFs() ? '✕ צא ממסך מלא' : '⛶ מסך מלא'; };
  label();
  btn.addEventListener('click', e => {
    e.preventDefault(); e.stopPropagation();
    if (!canFs || isIOS) { help(); return; }
    if (isFs()) { want(false); exit(); } else { want(true); enter(); }
  });
  ['fullscreenchange', 'webkitfullscreenchange'].forEach(t => document.addEventListener(t, label));

  function place() {
    const nav = document.querySelector('nav');
    if (nav) { nav.appendChild(btn); if (getComputedStyle(nav).justifyContent === 'center') btn.style.marginInlineStart = '8px'; }
    else if (document.querySelector('header p')) {
      // the home page: sit next to the subtitle, then let the page re-fit its tiles
      const sub = document.querySelector('header p');
      btn.style.marginInlineStart = '8px'; btn.style.fontSize = '0.8rem'; btn.style.padding = '2px 10px';
      sub.appendChild(btn); dispatchEvent(new Event('resize'));
    }
    else { btn.classList.add('fs-float'); document.body.appendChild(btn); }
  }
  if (document.body) place(); else document.addEventListener('DOMContentLoaded', place);

  // full screen ends when a new page opens; bring it back at the first tap on the new page
  if (canFs && !isIOS && want()) {
    const again = () => { if (!isFs() && want()) enter(); };
    document.addEventListener('pointerdown', again, { once: true, capture: true });
  }
  // leaving full screen with the browser's own gesture or Esc means the child wants it off
  let leaving = false;
  addEventListener('pagehide', () => { leaving = true; });
  ['fullscreenchange', 'webkitfullscreenchange'].forEach(t => document.addEventListener(t, () => { if (!isFs() && !leaving) setTimeout(() => { if (!leaving && !isFs()) want(false); }, 300); }));
})();
