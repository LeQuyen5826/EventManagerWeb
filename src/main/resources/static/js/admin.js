/* Khu vực Admin. */
App.views.admin = async el => {
  const tabs = [['dash', 'Tổng quan'], ['users', 'Người dùng'], ['approve', 'Duyệt sự kiện'], ['catalog', 'Danh mục & địa điểm'], ['audit', 'Nhật ký']];
  el.innerHTML = `<div class="page-head"><div><h1>Quản trị hệ thống</h1></div></div>
    <div class="tabs" id="tabs">${tabs.map(([k, t], i) => `<button data-t="${k}" class="${i ? '' : 'active'}">${t}</button>`).join('')}</div><div id="tab"></div>`;
  const show = k => { $$('#tabs button').forEach(b => b.classList.toggle('active', b.dataset.t === k)); adminTabs[k]($('#tab')); };
  $$('#tabs button').forEach(b => b.onclick = () => show(b.dataset.t));
  show('dash');
};

const adminTabs = {
  async dash(el) {
    el.innerHTML = loading(); const d = await api('/api/admin/dashboard');
    el.innerHTML = `<div class="stats">${[['Sự kiện', d.totalEvents], ['Người dùng', d.totalUsers], ['Lượt đăng ký', d.totalRegistrations]].map(([l, v]) => `<div class="stat"><b>${v}</b><span>${l}</span></div>`).join('')}</div>
      <div class="card"><h2>Sự kiện theo trạng thái</h2>${bars(d.eventsByStatus, k => STATUS_LABEL[k] || k)}</div>`;
  },
  async users(el) {
    el.innerHTML = `<form class="row" id="sf" style="margin-bottom:12px"><input name="keyword" placeholder="Tìm tên / email…" style="max-width:280px"><button class="btn-primary">Tìm</button></form><div id="tb"></div><div class="pager" id="pg"></div>`;
    let page = 0;
    async function load() {
      const p = await api('/api/admin/users' + qs({ keyword: $('#sf').keyword.value, page, size: 15 }));
      $('#tb').innerHTML = `<div class="card"><div class="table-wrap"><table><thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th></th></tr></thead><tbody>
        ${p.content.map(u => `<tr><td>${u.id}</td><td>${esc(u.fullName)}</td><td>${esc(u.email)}</td><td>${u.roles.map(r => `<span class="badge">${ROLE_LABEL[r] || r}</span>`).join(' ')}</td>
        <td>${u.enabled ? '<span class="badge b-CONFIRMED">Hoạt động</span>' : '<span class="badge b-CANCELLED">Đã khóa</span>'}</td>
        <td><div class="row"><button class="btn-sm" data-roles="${u.id}">Vai trò</button>
        ${u.enabled ? `<button class="btn-sm btn-danger" data-lock="${u.id}">Khóa</button>` : `<button class="btn-sm" data-unlock="${u.id}">Mở khóa</button>`}</div></td></tr>`).join('')}</tbody></table></div></div>`;
      pager(p, n => { page = n; load(); }, $('#pg'));
      $$('[data-lock]').forEach(b => b.onclick = async () => { if (confirmBox('Khóa tài khoản này?') && await act(() => api(`/api/admin/users/${b.dataset.lock}/lock`, { method: 'POST' }), 'Đã khóa')) load(); });
      $$('[data-unlock]').forEach(b => b.onclick = async () => { if (await act(() => api(`/api/admin/users/${b.dataset.unlock}/unlock`, { method: 'POST' }), 'Đã mở khóa')) load(); });
      $$('[data-roles]').forEach(b => b.onclick = () => {
        const u = p.content.find(x => x.id == b.dataset.roles);
        showModal(`<h2>Vai trò của ${esc(u.fullName)}</h2><form id="rf">${Object.entries(ROLE_LABEL).map(([k, t]) => `<label style="display:flex;gap:8px;align-items:center;font-weight:400"><input type="checkbox" style="width:auto" name="r" value="${k}" ${u.roles.includes(k) ? 'checked' : ''}>${t}</label>`).join('')}
          <div class="row" style="justify-content:flex-end;margin-top:14px"><button type="button" id="cx">Đóng</button><button class="btn-primary">Lưu</button></div></form>`);
        $('#cx').onclick = closeModal;
        $('#rf').onsubmit = async e => { e.preventDefault(); const roles = $$('input:checked', e.target).map(i => i.value);
          if (await act(() => api(`/api/admin/users/${u.id}/roles`, { method: 'PUT', body: { roles } }), 'Đã cập nhật vai trò')) { closeModal(); load(); } };
      });
    }
    $('#sf').onsubmit = e => { e.preventDefault(); page = 0; load(); };
    load();
  },
  async approve(el) {
    el.innerHTML = `<div class="row" style="margin-bottom:12px"><label style="margin:0">Trạng thái</label><select id="st" style="width:auto">${['PENDING_APPROVAL', 'PUBLISHED', 'REJECTED', 'DRAFT', 'CANCELLED', 'ENDED'].map(s => `<option value="${s}">${STATUS_LABEL[s]}</option>`).join('')}</select></div><div id="tb"></div>
      <p class="muted small">Gợi ý: chỉ khi bật <code>EVENT_REQUIRE_APPROVAL=true</code> thì sự kiện mới vào trạng thái "Chờ duyệt".</p>`;
    async function load() {
      const st = $('#st').value, p = await api('/api/admin/events' + qs({ status: st, size: 30 }));
      $('#tb').innerHTML = `<div class="card"><div class="table-wrap"><table><thead><tr><th>Sự kiện</th><th>Thời gian</th><th>Trạng thái</th><th></th></tr></thead><tbody>
        ${p.content.map(e => `<tr><td><a href="#/events/${e.id}"><b>${esc(e.title)}</b></a></td><td>${fmtDT(e.startAt)}</td><td>${badge(e.status)}</td>
        <td>${st === 'PENDING_APPROVAL' ? `<div class="row"><button class="btn-sm btn-primary" data-ap="${e.id}">Duyệt</button><button class="btn-sm btn-danger" data-rj="${e.id}">Từ chối</button></div>` : `<a class="btn btn-sm" href="#/manage/${e.id}">Chi tiết</a>`}</td></tr>`).join('')
        || '<tr><td colspan="4" class="empty">Không có sự kiện.</td></tr>'}</tbody></table></div></div>`;
      $$('[data-ap]').forEach(b => b.onclick = async () => { if (await act(() => api(`/api/admin/events/${b.dataset.ap}/approve`, { method: 'POST' }), 'Đã duyệt')) load(); });
      $$('[data-rj]').forEach(b => b.onclick = async () => { const reason = await askReason('Từ chối sự kiện'); if (reason && await act(() => api(`/api/admin/events/${b.dataset.rj}/reject`, { method: 'POST', body: { reason } }), 'Đã từ chối')) load(); });
    }
    $('#st').onchange = load; load();
  },
  async catalog(el) {
    async function load() {
      catalogCache = null; const { categories, venues } = await catalog();
      el.innerHTML = `<div class="grid" style="grid-template-columns:repeat(auto-fit,minmax(320px,1fr))">
        <div class="card"><div class="row"><h2 style="margin:0">Loại sự kiện</h2><span class="spacer"></span><button class="btn-sm btn-primary" id="addC">+ Thêm</button></div>
          <table style="margin-top:8px"><tbody>${categories.map(c => `<tr><td>${esc(c.name)}</td><td style="text-align:right"><button class="btn-sm" data-ec="${c.id}">Sửa</button> <button class="btn-sm btn-danger" data-dc="${c.id}">Xóa</button></td></tr>`).join('')}</tbody></table></div>
        <div class="card"><div class="row"><h2 style="margin:0">Địa điểm</h2><span class="spacer"></span><button class="btn-sm btn-primary" id="addV">+ Thêm</button></div>
          <table style="margin-top:8px"><tbody>${venues.map(v => `<tr><td><b>${esc(v.name)}</b><div class="muted small">${esc(v.address || '')} · tối đa ${v.maxCapacity}</div></td><td style="text-align:right"><button class="btn-sm" data-ev="${v.id}">Sửa</button> <button class="btn-sm btn-danger" data-dv="${v.id}">Xóa</button></td></tr>`).join('')}</tbody></table></div></div>`;
      const catForm = c => { showModal(`<h2>${c ? 'Sửa' : 'Thêm'} loại sự kiện</h2><form id="cf"><label>Tên</label><input name="name" required maxlength="100" value="${esc(c?.name)}"><div class="row" style="justify-content:flex-end;margin-top:14px"><button type="button" id="cx">Đóng</button><button class="btn-primary">Lưu</button></div></form>`);
        $('#cx').onclick = closeModal; $('#cf').onsubmit = async e => { e.preventDefault(); if (await act(() => api(c ? '/api/admin/categories/' + c.id : '/api/admin/categories', { method: c ? 'PUT' : 'POST', body: { name: e.target.name.value } }), 'Đã lưu')) { closeModal(); load(); } }; };
      const venForm = v => { showModal(`<h2>${v ? 'Sửa' : 'Thêm'} địa điểm</h2><form id="vf" class="form-grid" style="grid-template-columns:1fr"><div><label>Tên</label><input name="name" required maxlength="150" value="${esc(v?.name)}"></div>
        <div><label>Địa chỉ</label><input name="address" maxlength="255" value="${esc(v?.address)}"></div><div><label>Sức chứa tối đa</label><input name="maxCapacity" type="number" min="1" required value="${v?.maxCapacity ?? ''}"></div>
        <div class="row" style="justify-content:flex-end"><button type="button" id="cx">Đóng</button><button class="btn-primary">Lưu</button></div></form>`);
        $('#cx').onclick = closeModal; $('#vf').onsubmit = async e => { e.preventDefault(); const d = Object.fromEntries(new FormData(e.target)); d.maxCapacity = +d.maxCapacity;
          if (await act(() => api(v ? '/api/admin/venues/' + v.id : '/api/admin/venues', { method: v ? 'PUT' : 'POST', body: d }), 'Đã lưu')) { closeModal(); load(); } }; };
      $('#addC').onclick = () => catForm(null); $('#addV').onclick = () => venForm(null);
      $$('[data-ec]').forEach(b => b.onclick = () => catForm(categories.find(c => c.id == b.dataset.ec)));
      $$('[data-ev]').forEach(b => b.onclick = () => venForm(venues.find(v => v.id == b.dataset.ev)));
      $$('[data-dc]').forEach(b => b.onclick = async () => { if (confirmBox('Xóa loại sự kiện này?') && await act(() => api('/api/admin/categories/' + b.dataset.dc, { method: 'DELETE' }), 'Đã xóa')) load(); });
      $$('[data-dv]').forEach(b => b.onclick = async () => { if (confirmBox('Xóa địa điểm này?') && await act(() => api('/api/admin/venues/' + b.dataset.dv, { method: 'DELETE' }), 'Đã xóa')) load(); });
    }
    load();
  },
  async audit(el) {
    let page = 0;
    async function load() {
      const p = await api('/api/admin/audit-logs' + qs({ page, size: 25 }));
      el.innerHTML = `<div class="card"><div class="table-wrap"><table><thead><tr><th>Thời gian</th><th>Người thực hiện (ID)</th><th>Hành động</th><th>Chi tiết</th></tr></thead><tbody>
        ${p.content.map(l => `<tr><td>${fmtDT(l.createdAt)}</td><td>${l.actorId ?? ''}</td><td><code>${esc(l.action)}</code></td><td class="small">${esc(l.detail)}</td></tr>`).join('') || '<tr><td colspan="4" class="empty">Chưa có nhật ký.</td></tr>'}</tbody></table></div></div><div class="pager" id="pg"></div>`;
      pager(p, n => { page = n; load(); }, $('#pg'));
    }
    load();
  }
};
