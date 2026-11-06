/* Bộ định tuyến theo hash (#/...) và khởi động ứng dụng. */
const routes = [
  [/^\/?$/, 'home'], [/^\/events\/(\d+)$/, 'event'], [/^\/login$/, 'login'], [/^\/register$/, 'register'],
  [/^\/tickets$/, 'tickets', ['ATTENDEE']], [/^\/notifications$/, 'notifications', true], [/^\/profile$/, 'profile', true],
  [/^\/organizer$/, 'organizer', ['ORGANIZER']], [/^\/event-form(?:\/(\d+))?$/, 'eventForm', ['ORGANIZER', 'ADMIN']],
  [/^\/manage\/(\d+)$/, 'manage', ['ORGANIZER', 'ADMIN']], [/^\/checkin$/, 'checkin', ['STAFF', 'ORGANIZER']],
  [/^\/admin$/, 'admin', ['ADMIN']]
];
async function route() {
  clearTimers(); closeModal();
  const path = (location.hash.slice(1) || '/'), el = $('#app');
  renderNav(); window.scrollTo(0, 0);
  for (const [re, view, guard] of routes) {
    const m = path.match(re); if (!m) continue;
    if (guard) {
      if (!App.user) { toast('Vui lòng đăng nhập', 'bad'); return void (location.hash = '#/login'); }
      if (Array.isArray(guard) && !Auth.has(...guard)) { el.innerHTML = '<div class="card empty">Bạn không có quyền truy cập trang này.</div>'; return; }
    }
    el.innerHTML = loading();
    try { await App.views[view](el, ...m.slice(1)); } catch (e) { el.innerHTML = `<div class="alert bad">${esc(errText(e))}</div>`; }
    return;
  }
  el.innerHTML = '<div class="card empty">Không tìm thấy trang. <a href="#/">Về trang chủ</a></div>';
}
window.addEventListener('hashchange', route);
(async () => { await Auth.load(); route(); })();
