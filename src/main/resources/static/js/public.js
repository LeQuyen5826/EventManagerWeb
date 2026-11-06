/* Trang công khai: danh sách sự kiện, chi tiết, đăng nhập, đăng ký. */
let catalogCache = null;
async function catalog() {
  if (!catalogCache) { const [categories, venues] = await Promise.all([api('/api/categories'), api('/api/venues')]); catalogCache = { categories, venues }; }
  return catalogCache;
}
const opts = (list, sel, ph) => (ph ? `<option value="">${ph}</option>` : '') + list.map(x => `<option value="${x.id}" ${String(x.id) === String(sel) ? 'selected' : ''}>${esc(x.name)}</option>`).join('');

App.views.home = async (el) => {
  const { categories, venues } = await catalog();
  el.innerHTML = `<section class="hero"><h1>Tìm sự kiện, giữ chỗ trong vài giây</h1><p>Hội thảo, workshop, ngày hội việc làm và nhiều hoạt động khác của trường. Đăng ký xong nhận mã QR để vào cửa.</p></section>
    <form class="card filters" id="filters">
      <div><label>Từ khóa</label><input name="keyword" placeholder="Tên sự kiện…"></div>
      <div><label>Loại</label><select name="categoryId">${opts(categories, '', 'Tất cả')}</select></div>
      <div><label>Địa điểm</label><select name="venueId">${opts(venues, '', 'Tất cả')}</select></div>
      <div><label>Từ ngày</label><input type="date" name="from"></div>
      <div><label>Đến ngày</label><input type="date" name="to"></div>
      <div class="row"><button class="btn-primary">Tìm</button></div>
    </form>
    <div class="home-bar"><div class="chips" id="chips"><button type="button" data-c="" class="active">Tất cả</button>${categories.map(c => `<button type="button" data-c="${c.id}">${esc(c.name)}</button>`).join('')}</div>
      <div class="seg" id="seg">${[['PUBLISHED', 'Sắp diễn ra'], ['ONGOING', 'Đang diễn ra'], ['ENDED', 'Đã kết thúc'], ['CANCELLED', 'Đã hủy']].map(([v, t], i) => `<button type="button" data-s="${v}" class="${i ? '' : 'active'}">${t}</button>`).join('')}</div>
      <select id="statusSel" hidden><option value="PUBLISHED"></option><option value="ONGOING"></option><option value="ENDED"></option><option value="CANCELLED"></option></select></div>
    <div id="list" style="margin-top:14px"></div><div class="pager" id="pager"></div>`;
  let page = 0;
  async function load() {
    const f = Object.fromEntries(new FormData($('#filters')));
    const params = { ...f, from: f.from ? f.from + 'T00:00:00' : '', to: f.to ? f.to + 'T23:59:59' : '', status: $('#statusSel').value, page, size: 9 };
    $('#list').innerHTML = loading();
    try {
      const p = await api('/api/events' + qs(params));
      $('#list').innerHTML = p.content.length ? `<div class="grid">${p.content.map(eventCard).join('')}</div>` : '<div class="empty">Không có sự kiện phù hợp.</div>';
      pager(p, n => { page = n; load(); }, $('#pager'));
    } catch (e) { $('#list').innerHTML = `<div class="alert bad">${esc(errText(e))}</div>`; }
  }
  $('#filters').onsubmit = e => { e.preventDefault(); page = 0; load(); };
  $$('#chips button').forEach(b => b.onclick = () => { $$('#chips button').forEach(x => x.classList.toggle('active', x === b)); $('#filters').categoryId.value = b.dataset.c; page = 0; load(); });
  $$('#seg button').forEach(b => b.onclick = () => { $$('#seg button').forEach(x => x.classList.toggle('active', x === b)); $('#statusSel').value = b.dataset.s; page = 0; load(); });
  load();
};

App.views.event = async (el, id) => {
  el.innerHTML = loading();
  let ev, sessions;
  try { ev = await api('/api/events/' + id); sessions = ev.sessions || await api(`/api/events/${id}/sessions`); }
  catch (e) { el.innerHTML = `<div class="alert bad">${esc(errText(e))}</div><a href="#/">← Về danh sách</a>`; return; }
  const now = new Date(), open = now >= new Date(ev.registrationOpenAt) && now <= new Date(ev.registrationCloseAt);
  const canManage = App.user && (Auth.has('ADMIN') || (Auth.has('ORGANIZER') && ev.organizerId === App.user.id));
  const full = ev.availableSeats <= 0;
  const cv = coverInfo(ev.categoryName, ev.coverImageUrl);

  let side = '';
  if (ev.status === 'CANCELLED') side = `<div class="alert bad">Sự kiện đã bị hủy.${ev.cancelReason ? '<br>Lý do: ' + esc(ev.cancelReason) : ''}</div>`;
  else if (ev.status !== 'PUBLISHED') side = `<div class="alert">Sự kiện hiện không mở đăng ký (${esc(STATUS_LABEL[ev.status])}).</div>`;
  else if (!App.user) side = `<p>Đăng nhập để đăng ký tham gia.</p><a class="btn btn-primary" href="#/login">Đăng nhập</a>`;
  else if (!Auth.has('ATTENDEE')) side = `<div class="alert">Chỉ tài khoản Người tham gia mới đăng ký được.</div>`;
  else if (!open) side = `<div class="alert">Đăng ký mở từ ${fmtDT(ev.registrationOpenAt)} đến ${fmtDT(ev.registrationCloseAt)}.</div>`;
  else side = `<form id="regForm" class="form-grid" style="grid-template-columns:1fr">
      <div><label>Mã sinh viên (tùy chọn)</label><input name="studentId" maxlength="30"></div>
      <div><label>Đơn vị / Lớp (tùy chọn)</label><input name="organization" maxlength="100"></div>
      ${full ? '<div class="alert">Sự kiện đã hết chỗ. Bạn có thể vào danh sách chờ.</div>' : ''}
      <button class="btn-primary" style="justify-content:center">${full ? 'Vào danh sách chờ' : 'Đăng ký tham gia'}</button></form>`;

  el.innerHTML = `<a href="#/" class="muted small">← Tất cả sự kiện</a>
    <div class="detail-hero" style="margin-top:8px;${cv.css}"><span class="thumb-icon big">${cv.icon}</span></div>
    <div class="page-head"><div><h1>${esc(ev.title)}</h1><div class="row">${badge(ev.status)} <span class="badge">${esc(ev.categoryName)}</span></div></div>
      ${canManage ? `<a class="btn btn-primary" href="#/manage/${ev.id}">Quản lý sự kiện</a>` : ''}</div>
    <div class="detail-layout"><div>
      <div class="card"><h2>Giới thiệu</h2><p style="white-space:pre-wrap;margin:0">${esc(ev.description || 'Chưa có mô tả.')}</p></div>
      <div class="card"><h2>Lịch trình</h2>${sessions.length ? `<div class="timeline">${sessions.map(s => `<div class="item"><b>${esc(s.title)}</b>
        <div class="muted small">${fmtDT(s.startAt)} – ${fmtDT(s.endAt)}${s.room ? ' · ' + esc(s.room) : ''}${s.speakerName ? ' · ' + esc(s.speakerName) : ''}</div>
        ${s.description ? `<div class="small">${esc(s.description)}</div>` : ''}</div>`).join('')}</div>` : '<div class="muted">Chưa có lịch trình chi tiết.</div>'}</div>
    </div><div>
      <div class="card"><dl class="kv">
        <dt>Bắt đầu</dt><dd>${fmtDT(ev.startAt)}</dd><dt>Kết thúc</dt><dd>${fmtDT(ev.endAt)}</dd>
        <dt>Địa điểm</dt><dd>${esc(ev.venueName)}<div class="muted small">${esc(ev.venueAddress || '')}</div></dd>
        <dt>Tổ chức</dt><dd>${esc(ev.organizerName)}</dd>
        <dt>Chỗ trống</dt><dd>${ev.availableSeats}/${ev.capacity}</dd>
        <dt>Hạn đăng ký</dt><dd>${fmtDT(ev.registrationCloseAt)}</dd></dl></div>
      <div class="card"><h2>Đăng ký</h2>${side}</div></div></div>`;

  const f = $('#regForm');
  if (f) f.onsubmit = async e => {
    e.preventDefault(); const d = Object.fromEntries(new FormData(f)); d.joinWaitlist = full;
    const r = await act(() => api(`/api/events/${id}/registrations`, { method: 'POST', body: d }));
    if (r) { toast(r.status === 'WAITLISTED' ? 'Bạn đã vào danh sách chờ' : 'Đăng ký thành công! Xem mã QR ở mục Vé của tôi', 'ok'); location.hash = '#/tickets'; }
  };
};

function showFieldErrors(form, e) {
  $$('.err', form).forEach(x => x.remove());
  if (!e.errors) return fail(e);
  Object.entries(e.errors).forEach(([k, m]) => {
    const inp = form.elements[k]; if (inp) inp.parentElement.insertAdjacentHTML('beforeend', `<div class="err">${esc(m)}</div>`);
  });
}

App.views.login = el => {
  el.innerHTML = `<div class="card auth-box"><h1>Đăng nhập</h1><p class="muted">Chào mừng quay lại EventHub.</p>
    <form id="f" class="form-grid" style="grid-template-columns:1fr">
      <div><label>Email</label><input name="email" type="email" required autocomplete="username"></div>
      <div><label>Mật khẩu</label><input name="password" type="password" required autocomplete="current-password"></div>
      <button class="btn-primary" style="justify-content:center">Đăng nhập</button></form>
    <p class="small muted" style="margin-bottom:0">Chưa có tài khoản? <a href="#/register">Đăng ký</a></p></div>`;
  $('#f').onsubmit = async e => {
    e.preventDefault(); const f = e.target;
    try {
      const t = await api('/api/auth/login', { method: 'POST', body: Object.fromEntries(new FormData(f)) });
      Auth.token = t.accessToken; await Auth.load(); renderNav(); toast('Đăng nhập thành công', 'ok');
      location.hash = Auth.has('ADMIN') ? '#/admin' : Auth.has('ORGANIZER') ? '#/organizer' : Auth.has('STAFF') ? '#/checkin' : '#/';
    } catch (err) { showFieldErrors(f, err); }
  };
};

App.views.register = el => {
  el.innerHTML = `<div class="card auth-box"><h1>Tạo tài khoản</h1><p class="muted">Tài khoản mới có vai trò Người tham gia.</p>
    <form id="f" class="form-grid" style="grid-template-columns:1fr">
      <div><label>Họ và tên</label><input name="fullName" required maxlength="100"></div>
      <div><label>Email</label><input name="email" type="email" required></div>
      <div><label>Số điện thoại</label><input name="phone" placeholder="0912345678"></div>
      <div><label>Mật khẩu (8–64 ký tự)</label><input name="password" type="password" required minlength="8" maxlength="64" autocomplete="new-password"></div>
      <button class="btn-primary" style="justify-content:center">Đăng ký</button></form>
    <p class="small muted" style="margin-bottom:0">Đã có tài khoản? <a href="#/login">Đăng nhập</a></p></div>`;
  $('#f').onsubmit = async e => {
    e.preventDefault(); const f = e.target, d = Object.fromEntries(new FormData(f)); if (!d.phone) delete d.phone;
    try { await api('/api/auth/register', { method: 'POST', body: d }); toast('Đăng ký thành công, hãy đăng nhập', 'ok'); location.hash = '#/login'; }
    catch (err) { showFieldErrors(f, err); }
  };
};
