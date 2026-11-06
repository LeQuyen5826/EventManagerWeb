/* Người tổ chức: danh sách sự kiện của tôi, form tạo/sửa, trang quản lý, check-in. */
App.views.organizer = async el => {
  let page = 0;
  async function load() {
    el.innerHTML = loading();
    const p = await api('/api/me/events' + qs({ page, size: 10 }));
    el.innerHTML = `<div class="page-head"><div><h1>Sự kiện của tôi</h1><div class="muted">Tạo, công bố và quản lý sự kiện.</div></div>
      <a class="btn btn-primary" href="#/event-form">+ Tạo sự kiện</a></div>
      <div class="card"><div class="table-wrap"><table><thead><tr><th>Sự kiện</th><th>Thời gian</th><th>Chỗ</th><th>Trạng thái</th><th></th></tr></thead><tbody>
      ${p.content.map(e => `<tr><td><a href="#/events/${e.id}"><b>${esc(e.title)}</b></a><div class="muted small">${esc(e.venueName)}</div></td>
        <td>${fmtDT(e.startAt)}</td><td>${e.capacity - e.availableSeats}/${e.capacity}</td><td>${badge(e.status)}</td>
        <td><div class="row"><a class="btn btn-sm" href="#/manage/${e.id}">Quản lý</a>
        ${['DRAFT', 'REJECTED', 'PUBLISHED'].includes(e.status) ? `<a class="btn btn-sm" href="#/event-form/${e.id}">Sửa</a>` : ''}
        ${['DRAFT', 'REJECTED'].includes(e.status) ? `<button class="btn-sm btn-primary" data-pub="${e.id}">Công bố</button><button class="btn-sm btn-danger" data-del="${e.id}">Xóa</button>` : ''}
        ${['PUBLISHED', 'ONGOING'].includes(e.status) ? `<button class="btn-sm btn-danger" data-cancel="${e.id}">Hủy</button>` : ''}</div></td></tr>`).join('')
        || '<tr><td colspan="5" class="empty">Bạn chưa có sự kiện nào.</td></tr>'}</tbody></table></div></div><div class="pager" id="pg"></div>`;
    pager(p, n => { page = n; load(); }, $('#pg'));
    $$('[data-pub]').forEach(b => b.onclick = async () => { if (await act(() => api(`/api/events/${b.dataset.pub}/publish`, { method: 'POST' }), 'Đã gửi công bố')) load(); });
    $$('[data-del]').forEach(b => b.onclick = async () => { if (confirmBox('Xóa sự kiện này?') && await act(() => api('/api/events/' + b.dataset.del, { method: 'DELETE' }), 'Đã xóa')) load(); });
    $$('[data-cancel]').forEach(b => b.onclick = async () => {
      const reason = await askReason('Hủy sự kiện'); if (reason && await act(() => api(`/api/events/${b.dataset.cancel}/cancel`, { method: 'POST', body: { reason } }), 'Đã hủy sự kiện')) load();
    });
  }
  load();
};

App.views.eventForm = async (el, id) => {
  const [{ categories, venues }, ev] = await Promise.all([catalog(), id ? api('/api/events/' + id) : null]);
  const v = ev || {};
  el.innerHTML = `<a href="#/organizer" class="muted small">← Sự kiện của tôi</a><h1 style="margin-top:6px">${id ? 'Chỉnh sửa' : 'Tạo'} sự kiện</h1>
    <form id="f" class="card form-grid">
      <div class="field full"><label>Tên sự kiện</label><input name="title" maxlength="200" required value="${esc(v.title)}"></div>
      <div class="field full"><label>Mô tả</label><textarea name="description" rows="4" maxlength="2000">${esc(v.description)}</textarea></div>
      <div class="field full"><label>Ảnh bìa (URL, tùy chọn)</label><input name="coverImageUrl" value="${esc(v.coverImageUrl)}"></div>
      <div class="field"><label>Loại sự kiện</label><select name="categoryId" required>${opts(categories, v.categoryId, 'Chọn…')}</select></div>
      <div class="field"><label>Địa điểm</label><select name="venueId" required>${opts(venues.map(x => ({ id: x.id, name: `${x.name} (tối đa ${x.maxCapacity})` })), v.venueId, 'Chọn…')}</select></div>
      <div class="field"><label>Sức chứa</label><input name="capacity" type="number" min="1" max="10000" required value="${v.capacity ?? ''}"></div>
      <div class="field"></div>
      <div class="field"><label>Bắt đầu</label><input name="startAt" type="datetime-local" required value="${toInput(v.startAt)}"></div>
      <div class="field"><label>Kết thúc</label><input name="endAt" type="datetime-local" required value="${toInput(v.endAt)}"></div>
      <div class="field"><label>Mở đăng ký</label><input name="registrationOpenAt" type="datetime-local" required value="${toInput(v.registrationOpenAt)}"></div>
      <div class="field"><label>Đóng đăng ký</label><input name="registrationCloseAt" type="datetime-local" required value="${toInput(v.registrationCloseAt)}"></div>
      <div class="field full row"><button class="btn-primary">${id ? 'Lưu thay đổi' : 'Lưu nháp'}</button><a class="btn" href="#/organizer">Hủy</a></div>
    </form>`;
  $('#f').onsubmit = async e => {
    e.preventDefault(); const f = e.target, d = Object.fromEntries(new FormData(f));
    ['startAt', 'endAt', 'registrationOpenAt', 'registrationCloseAt'].forEach(k => d[k] = fromInput(d[k]));
    d.categoryId = +d.categoryId; d.venueId = +d.venueId; d.capacity = +d.capacity;
    if (!d.coverImageUrl) delete d.coverImageUrl;
    try {
      const r = await api(id ? '/api/events/' + id : '/api/events', { method: id ? 'PUT' : 'POST', body: d });
      toast(id ? 'Đã cập nhật sự kiện' : 'Đã tạo sự kiện (Nháp). Hãy thêm lịch trình rồi công bố.', 'ok'); location.hash = '#/manage/' + r.id;
    } catch (err) { showFieldErrors(f, err); }
  };
};

/* ---------- Trang quản lý một sự kiện ---------- */
App.views.manage = async (el, id) => {
  let ev;
  try { ev = await api('/api/events/' + id); } catch (e) { el.innerHTML = `<div class="alert bad">${esc(errText(e))}</div>`; return; }
  const tabs = [['report', 'Báo cáo'], ['sessions', 'Lịch trình'], ['attendees', 'Người đăng ký'], ['staff', 'Nhân viên']];
  el.innerHTML = `<a href="#/organizer" class="muted small">← Sự kiện của tôi</a>
    <div class="page-head" style="margin-top:6px"><div><h1>${esc(ev.title)}</h1><div class="row">${badge(ev.status)}<span class="muted small">${fmtDT(ev.startAt)} · ${esc(ev.venueName)}</span></div></div>
    <div class="row"><a class="btn" href="#/events/${id}">Xem trang công khai</a><button id="xl">⬇ Xuất Excel</button></div></div>
    <div class="tabs" id="tabs">${tabs.map(([k, t], i) => `<button data-t="${k}" class="${i ? '' : 'active'}">${t}</button>`).join('')}</div><div id="tab"></div>`;
  $('#xl').onclick = () => act(() => download(`/api/events/${id}/report/export`, `registrations-${id}.xlsx`));
  const show = k => { $$('#tabs button').forEach(b => b.classList.toggle('active', b.dataset.t === k)); tabViews[k]($('#tab'), id, ev); };
  $$('#tabs button').forEach(b => b.onclick = () => show(b.dataset.t));
  show('report');
};

const bars = (obj, labelFn = k => k) => {
  const e = Object.entries(obj || {}), max = Math.max(1, ...e.map(x => x[1]));
  return e.length ? `<div class="bars">${e.map(([k, v]) => `<div class="bar" title="${esc(labelFn(k))}: ${v}"><em>${v}</em><i style="height:${Math.round(v / max * 100)}%"></i><small>${esc(labelFn(k))}</small></div>`).join('')}</div>` : '<div class="muted">Chưa có dữ liệu.</div>';
};

const tabViews = {
  async report(el, id) {
    el.innerHTML = loading();
    const r = await api(`/api/events/${id}/report`);
    el.innerHTML = `<div class="stats">
      ${[['Sức chứa', r.capacity], ['Đã đăng ký', r.registered], ['Đã check-in', r.checkedIn], ['Vắng mặt', r.noShow], ['Danh sách chờ', r.waitlisted], ['Đã hủy', r.cancelled], ['Tỷ lệ tham dự', (r.attendanceRate * (r.attendanceRate <= 1 ? 100 : 1)).toFixed(1) + '%']]
        .map(([l, v]) => `<div class="stat"><b>${v}</b><span>${l}</span></div>`).join('')}</div>
      <div class="card"><h2>Đăng ký theo ngày</h2>${bars(r.registrationsByDay, k => k.slice(5))}</div>
      <div class="card"><h2>Check-in theo giờ</h2>${bars(r.checkInsByHour, k => k + 'h')}</div>`;
  },
  async sessions(el, id, ev) {
    async function load() {
      const list = await api(`/api/events/${id}/sessions`);
      el.innerHTML = `<div class="row" style="margin-bottom:12px"><h2 style="margin:0">Lịch trình</h2><span class="spacer"></span><button class="btn-primary" id="add">+ Thêm phiên</button></div>
        ${list.length ? list.map(s => `<div class="card"><div class="row"><div><b>${esc(s.title)}</b><div class="muted small">${fmtDT(s.startAt)} – ${fmtDT(s.endAt)}${s.room ? ' · ' + esc(s.room) : ''}${s.speakerName ? ' · ' + esc(s.speakerName) : ''}</div>
          ${s.description ? `<div class="small">${esc(s.description)}</div>` : ''}</div><span class="spacer"></span>
          <button class="btn-sm" data-edit="${s.id}">Sửa</button><button class="btn-sm btn-danger" data-del="${s.id}">Xóa</button></div></div>`).join('') : '<div class="card empty">Chưa có phiên nào.</div>'}`;
      $('#add').onclick = () => form(null);
      $$('[data-edit]').forEach(b => b.onclick = () => form(list.find(s => s.id == b.dataset.edit)));
      $$('[data-del]').forEach(b => b.onclick = async () => { if (confirmBox('Xóa phiên này?') && await act(() => api(`/api/events/${id}/sessions/${b.dataset.del}`, { method: 'DELETE' }), 'Đã xóa')) load(); });
    }
    function form(s) {
      const v = s || {};
      showModal(`<h2>${s ? 'Sửa' : 'Thêm'} phiên</h2><form id="sf" class="form-grid" style="grid-template-columns:1fr">
        <div><label>Tiêu đề</label><input name="title" required maxlength="200" value="${esc(v.title)}"></div>
        <div><label>Bắt đầu</label><input type="datetime-local" name="startAt" required value="${toInput(v.startAt || ev.startAt)}"></div>
        <div><label>Kết thúc</label><input type="datetime-local" name="endAt" required value="${toInput(v.endAt || ev.endAt)}"></div>
        <div><label>Phòng</label><input name="room" maxlength="100" value="${esc(v.room)}"></div>
        <div><label>Diễn giả</label><input name="speakerName" maxlength="100" value="${esc(v.speakerName)}"></div>
        <div><label>Mô tả</label><textarea name="description" rows="2" maxlength="1000">${esc(v.description)}</textarea></div>
        <div class="row" style="justify-content:flex-end"><button type="button" id="cx">Đóng</button><button class="btn-primary">Lưu</button></div></form>`);
      $('#cx').onclick = closeModal;
      $('#sf').onsubmit = async e => {
        e.preventDefault(); const d = Object.fromEntries(new FormData(e.target)); d.startAt = fromInput(d.startAt); d.endAt = fromInput(d.endAt);
        try { await api(s ? `/api/events/${id}/sessions/${s.id}` : `/api/events/${id}/sessions`, { method: s ? 'PUT' : 'POST', body: d }); closeModal(); toast('Đã lưu phiên', 'ok'); load(); }
        catch (err) { showFieldErrors(e.target, err); }
      };
    }
    load();
  },
  async attendees(el, id) {
    el.innerHTML = `<form class="row" id="sf" style="margin-bottom:12px"><input name="keyword" placeholder="Tìm theo tên / email…" style="max-width:260px">
      <select name="status" style="width:auto"><option value="">Mọi trạng thái</option>${['CONFIRMED', 'CHECKED_IN', 'WAITLISTED', 'CANCELLED', 'NO_SHOW'].map(s => `<option value="${s}">${STATUS_LABEL[s]}</option>`).join('')}</select>
      <button class="btn-primary">Lọc</button></form><div id="tb"></div><div class="pager" id="pg"></div>`;
    let page = 0;
    async function load() {
      const f = Object.fromEntries(new FormData($('#sf')));
      const p = await api(`/api/events/${id}/registrations` + qs({ ...f, page, size: 15 }));
      $('#tb').innerHTML = `<div class="card"><div class="table-wrap"><table><thead><tr><th>Họ tên</th><th>Email</th><th>MSSV / Đơn vị</th><th>Đăng ký lúc</th><th>Trạng thái</th><th></th></tr></thead><tbody>
        ${p.content.map(a => `<tr><td>${esc(a.fullName)}</td><td>${esc(a.email)}</td><td>${esc(a.studentId || '')} ${esc(a.organization || '')}</td><td>${fmtDT(a.registeredAt)}</td><td>${badge(a.status)}</td>
        <td><div class="row">${a.status === 'CONFIRMED' ? `<button class="btn-sm" data-ci="${a.registrationId}">Check-in</button>` : ''}
        ${a.status === 'CHECKED_IN' ? `<button class="btn-sm" data-undo="${a.registrationId}">Hoàn tác</button>` : ''}
        ${!['CANCELLED', 'CHECKED_IN'].includes(a.status) ? `<button class="btn-sm btn-danger" data-rm="${a.registrationId}">Hủy</button>` : ''}</div></td></tr>`).join('')
        || '<tr><td colspan="6" class="empty">Không có dữ liệu.</td></tr>'}</tbody></table></div></div>`;
      pager(p, n => { page = n; load(); }, $('#pg'));
      $$('[data-ci]').forEach(b => b.onclick = async () => { if (await act(() => api('/api/checkin/manual/' + b.dataset.ci, { method: 'POST' }), 'Đã check-in')) load(); });
      $$('[data-undo]').forEach(b => b.onclick = async () => { if (await act(() => api('/api/checkin/undo/' + b.dataset.undo, { method: 'POST' }), 'Đã hoàn tác')) load(); });
      $$('[data-rm]').forEach(b => b.onclick = async () => { if (confirmBox('Hủy đăng ký này?') && await act(() => api(`/api/events/${id}/registrations/${b.dataset.rm}`, { method: 'DELETE' }), 'Đã hủy')) load(); });
    }
    $('#sf').onsubmit = e => { e.preventDefault(); page = 0; load(); };
    load();
  },
  staff(el, id) {
    el.innerHTML = `<div class="card"><h2>Phân công nhân viên</h2>
      <p class="muted small">Nhập ID người dùng có vai trò Nhân viên (Admin xem ID ở mục Quản trị → Người dùng). Tài khoản demo staff@example.com thường có ID = 3.</p>
      <form id="sf" class="row"><input name="userId" type="number" min="1" placeholder="ID người dùng" required style="max-width:200px">
      <button class="btn-primary">Phân công</button><button type="button" class="btn-danger" id="rm">Gỡ phân công</button></form></div>`;
    $('#sf').onsubmit = async e => { e.preventDefault(); if (await act(() => api(`/api/events/${id}/staff`, { method: 'POST', body: { userId: +e.target.userId.value } }), 'Đã phân công')) e.target.reset(); };
    $('#rm').onclick = () => { const v = $('#sf').userId.value; if (!v) return toast('Nhập ID người dùng cần gỡ', 'bad'); act(() => api(`/api/events/${id}/staff/${v}`, { method: 'DELETE' }), 'Đã gỡ phân công'); };
  }
};

/* ---------- Check-in (Staff / Organizer) ---------- */
App.views.checkin = async el => {
  const [a, b] = await Promise.all([api('/api/events?status=PUBLISHED&size=50'), api('/api/events?status=ONGOING&size=50')]);
  const events = [...b.content, ...a.content];
  el.innerHTML = `<div class="page-head"><div><h1>Check-in tại cửa</h1><div class="muted">Quét mã QR hoặc check-in thủ công.</div></div></div>
    <div class="card"><label>Sự kiện</label><select id="evSel">${opts(events.map(e => ({ id: e.id, name: `${e.title} · ${fmtDT(e.startAt)}` })), '', events.length ? '' : 'Không có sự kiện')}</select>
      <div class="stats" id="stats" style="margin:14px 0 0"></div></div>
    <div class="grid" style="grid-template-columns:repeat(auto-fit,minmax(320px,1fr));margin-top:16px">
      <div class="card"><h2>Quét mã QR</h2><video id="cam" class="hidden" playsinline muted style="display:none"></video>
        <div class="row" style="margin-bottom:10px"><button id="camBtn">📷 Bật camera</button></div>
        <form id="tk" class="row"><input name="qrToken" placeholder="Hoặc dán mã QR vào đây" required style="flex:1"><button class="btn-primary">Check-in</button></form>
        <div class="scan-result" id="res"></div></div>
      <div class="card"><h2>Check-in thủ công</h2><form id="ms" class="row"><input name="keyword" placeholder="Tên hoặc email" style="flex:1"><button class="btn-primary">Tìm</button></form>
        <div id="found" style="margin-top:10px"></div></div></div>`;
  const evId = () => $('#evSel').value;
  async function stats() {
    if (!evId()) return;
    try { const s = await api(`/api/events/${evId()}/checkin-stats`); $('#stats').innerHTML = Object.entries(s).map(([k, v]) => `<div class="stat"><b>${v}</b><span>${esc(k)}</span></div>`).join(''); } catch (e) { $('#stats').innerHTML = `<div class="err">${esc(errText(e))}</div>`; }
  }
  const result = r => $('#res').innerHTML = `<div class="alert ok"><b>✔ ${esc(r.fullName)}</b><br>${esc(r.email)} · ${esc(r.eventTitle)}<br><span class="small">Lúc ${fmtDT(r.checkedInAt)}</span></div>`;
  async function scan(token) {
    try { result(await api('/api/checkin/scan', { method: 'POST', body: { qrToken: token } })); toast('Check-in thành công', 'ok'); stats(); }
    catch (e) { $('#res').innerHTML = `<div class="alert bad">${esc(errText(e))}</div>`; }
  }
  $('#evSel').onchange = stats; stats(); setTimer(stats, 10000);
  $('#tk').onsubmit = e => { e.preventDefault(); scan(e.target.qrToken.value.trim()); e.target.reset(); };
  $('#ms').onsubmit = async e => {
    e.preventDefault(); if (!evId()) return;
    try {
      const p = await api(`/api/events/${evId()}/registrations` + qs({ keyword: e.target.keyword.value, size: 8 }));
      $('#found').innerHTML = p.content.map(a => `<div class="row" style="padding:6px 0;border-bottom:1px solid var(--line)"><div><b>${esc(a.fullName)}</b><div class="muted small">${esc(a.email)}</div></div><span class="spacer"></span>${badge(a.status)}
        ${a.status === 'CONFIRMED' ? `<button class="btn-sm btn-primary" data-ci="${a.registrationId}">Check-in</button>` : ''}</div>`).join('') || '<div class="muted">Không tìm thấy.</div>';
      $$('[data-ci]').forEach(b => b.onclick = async () => { const r = await act(() => api('/api/checkin/manual/' + b.dataset.ci, { method: 'POST' }), 'Check-in thành công'); if (r) { result(r); stats(); b.closest('.row').remove(); } });
    } catch (err) { fail(err); }
  };
  $('#camBtn').onclick = async () => {
    if (!('BarcodeDetector' in window)) return toast('Trình duyệt chưa hỗ trợ quét QR bằng camera (dùng Chrome/Edge) – hãy dán mã vào ô bên cạnh', 'bad');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } }), v = $('#cam'), det = new BarcodeDetector({ formats: ['qr_code'] });
      v.style.display = 'block'; v.srcObject = stream; await v.play(); let last = '', lastAt = 0, on = true;
      App.stopCam = () => { on = false; stream.getTracks().forEach(t => t.stop()); };
      (async function loop() {
        while (on) {
          try { const codes = await det.detect(v); if (codes[0] && (codes[0].rawValue !== last || Date.now() - lastAt > 4000)) { last = codes[0].rawValue; lastAt = Date.now(); await scan(last); } } catch { }
          await new Promise(r => setTimeout(r, 400));
        }
      })();
      $('#camBtn').textContent = '📷 Đang quét…'; $('#camBtn').disabled = true;
    } catch { toast('Không mở được camera (cần HTTPS hoặc localhost và cấp quyền)', 'bad'); }
  };
};
