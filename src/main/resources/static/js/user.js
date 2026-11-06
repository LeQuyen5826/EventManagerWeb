/* Khu vực người dùng: vé, thông báo, hồ sơ. */
App.views.tickets = async el => {
  el.innerHTML = loading();
  const list = await api('/api/me/registrations');
  el.innerHTML = `<div class="page-head"><div><h1>Vé của tôi</h1><div class="muted">Các sự kiện bạn đã đăng ký.</div></div></div>
    ${list.length ? list.slice().reverse().map(r => `<div class="card ticket">
      <div><a href="#/events/${r.eventId}"><b>${esc(r.eventTitle)}</b></a><div style="margin-top:4px">${badge(r.status)}</div></div>
      <div class="row">${r.status === 'CONFIRMED' || r.status === 'CHECKED_IN' ? `<button class="btn-primary" data-qr="${r.id}">Xem mã QR</button>` : ''}
      ${['CONFIRMED', 'WAITLISTED', 'PENDING'].includes(r.status) ? `<button class="btn-danger" data-cancel="${r.id}">Hủy đăng ký</button>` : ''}</div></div>`).join('')
      : '<div class="card empty">Bạn chưa đăng ký sự kiện nào. <a href="#/">Khám phá sự kiện</a></div>'}`;
  $$('[data-qr]').forEach(b => b.onclick = async () => {
    try {
      const res = await request(`/api/registrations/${b.dataset.qr}/qr`, { raw: true }), url = URL.createObjectURL(await res.blob());
      showModal(`<div style="text-align:center"><h2>Mã QR check-in</h2><img class="qr-img" src="${url}" alt="QR"><p class="muted small">Đưa mã này cho nhân viên tại cửa sự kiện.</p><button id="cl">Đóng</button></div>`);
      $('#cl').onclick = () => { closeModal(); URL.revokeObjectURL(url); };
    } catch (e) { fail(e); }
  });
  $$('[data-cancel]').forEach(b => b.onclick = async () => {
    if (!confirmBox('Bạn chắc chắn muốn hủy đăng ký?')) return;
    if (await act(() => api('/api/registrations/' + b.dataset.cancel, { method: 'DELETE' }), 'Đã hủy đăng ký')) App.views.tickets(el);
  });
};

App.views.notifications = async el => {
  el.innerHTML = loading();
  let page = 0;
  async function load() {
    const p = await api('/api/me/notifications' + qs({ page, size: 15 }));
    el.innerHTML = `<div class="page-head"><div><h1>Thông báo</h1></div></div>
      ${p.content.length ? p.content.map(n => `<div class="card notif ${n.read ? '' : 'unread'}"><div class="row"><b>${esc(n.title)}</b><span class="spacer"></span>
        <span class="muted small">${fmtDT(n.createdAt)}</span></div><div>${esc(n.content)}</div>
        ${n.read ? '' : `<button class="btn-sm" style="margin-top:8px" data-read="${n.id}">Đánh dấu đã đọc</button>`}</div>`).join('') : '<div class="card empty">Chưa có thông báo.</div>'}
      <div class="pager" id="pg"></div>`;
    pager(p, n => { page = n; load(); }, $('#pg'));
    $$('[data-read]').forEach(b => b.onclick = async () => { await act(() => api(`/api/me/notifications/${b.dataset.read}/read`, { method: 'POST' })); load(); updateUnread(); });
  }
  load();
};

App.views.profile = async el => {
  const u = App.user;
  el.innerHTML = `<div class="page-head"><div><h1>Hồ sơ cá nhân</h1><div class="muted">${esc(u.email)} · ${u.roles.map(r => ROLE_LABEL[r] || r).join(', ')}</div></div></div>
   <div class="grid" style="grid-template-columns:repeat(auto-fit,minmax(300px,1fr))">
    <div class="card"><h2>Thông tin</h2><form id="pf" class="form-grid" style="grid-template-columns:1fr">
      <div><label>Họ và tên</label><input name="fullName" value="${esc(u.fullName)}" required></div>
      <div><label>Số điện thoại</label><input name="phone" value="${esc(u.phone || '')}"></div>
      <div><label>Ảnh đại diện (URL)</label><input name="avatarUrl" value="${esc(u.avatarUrl || '')}"></div>
      <button class="btn-primary" style="justify-content:center">Lưu thay đổi</button></form></div>
    <div class="card"><h2>Đổi mật khẩu</h2><form id="pw" class="form-grid" style="grid-template-columns:1fr">
      <div><label>Mật khẩu hiện tại</label><input name="oldPassword" type="password" required autocomplete="current-password"></div>
      <div><label>Mật khẩu mới (8–64 ký tự)</label><input name="newPassword" type="password" minlength="8" maxlength="64" required autocomplete="new-password"></div>
      <button class="btn-primary" style="justify-content:center">Đổi mật khẩu</button></form></div></div>`;
  $('#pf').onsubmit = async e => {
    e.preventDefault(); const d = Object.fromEntries(new FormData(e.target)); if (!d.phone) delete d.phone; if (!d.avatarUrl) delete d.avatarUrl;
    try { App.user = await api('/api/me/profile', { method: 'PUT', body: d }); toast('Đã cập nhật hồ sơ', 'ok'); renderNav(); } catch (err) { showFieldErrors(e.target, err); }
  };
  $('#pw').onsubmit = async e => {
    e.preventDefault();
    if (await act(() => api('/api/me/change-password', { method: 'POST', body: Object.fromEntries(new FormData(e.target)) }), 'Đã đổi mật khẩu')) e.target.reset();
  };
};
