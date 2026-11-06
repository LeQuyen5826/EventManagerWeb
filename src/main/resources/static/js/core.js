/* Lõi: gọi API, phiên đăng nhập, tiện ích giao diện. */
const App = { user: null, views: {}, timers: [] };
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const STATUS_LABEL = {
  DRAFT: 'Nháp', PENDING_APPROVAL: 'Chờ duyệt', PUBLISHED: 'Đã công bố', REJECTED: 'Bị từ chối', ONGOING: 'Đang diễn ra',
  ENDED: 'Đã kết thúc', CANCELLED: 'Đã hủy', PENDING: 'Chờ xử lý', CONFIRMED: 'Đã xác nhận', CHECKED_IN: 'Đã check-in',
  WAITLISTED: 'Danh sách chờ', NO_SHOW: 'Vắng mặt'
};
const ROLE_LABEL = { ADMIN: 'Quản trị viên', ORGANIZER: 'Người tổ chức', ATTENDEE: 'Người tham gia', STAFF: 'Nhân viên' };
const badge = s => `<span class="badge b-${esc(s)}">${esc(STATUS_LABEL[s] || s)}</span>`;

const fmtDT = s => s ? new Date(s).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' }) : '';
const fmtD = s => s ? new Date(s).toLocaleDateString('vi-VN') : '';
const toInput = s => (s || '').slice(0, 16);
const fromInput = v => v ? (v.length === 16 ? v + ':00' : v) : null;

/* ---------- Phiên đăng nhập ---------- */
const Auth = {
  get token() { return localStorage.getItem('token'); },
  set token(t) { t ? localStorage.setItem('token', t) : localStorage.removeItem('token'); },
  has(...roles) { return !!App.user && roles.some(r => App.user.roles.includes(r)); },
  async load() {
    App.user = null;
    if (!this.token) return;
    try { App.user = await api('/api/me/profile'); } catch { this.token = null; }
  },
  logout() { this.token = null; App.user = null; location.hash = '#/'; renderNav(); }
};

/* ---------- Gọi API ---------- */
class ApiError extends Error {
  constructor(status, body) { super(body?.message || 'Có lỗi xảy ra'); this.status = status; this.errors = body?.errors || null; }
}
async function request(path, { method = 'GET', body, raw = false } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (Auth.token) headers.Authorization = 'Bearer ' + Auth.token;
  const res = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  if (!res.ok) {
    let data = null; try { data = await res.json(); } catch { }
    if (res.status === 401 && Auth.token && !path.startsWith('/api/auth')) {
      Auth.token = null; App.user = null; renderNav();
      toast('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại', 'bad'); location.hash = '#/login';
    }
    throw new ApiError(res.status, data);
  }
  if (raw) return res;
  if (res.status === 204) return null;
  const ct = res.headers.get('content-type') || '';
  return ct.includes('json') ? res.json() : null;
}
const api = (p, o) => request(p, o);
const qs = obj => { const p = new URLSearchParams(); Object.entries(obj).forEach(([k, v]) => { if (v !== '' && v != null) p.set(k, v); }); const s = p.toString(); return s ? '?' + s : ''; };

async function download(path, filename) {
  const res = await request(path, { raw: true });
  const url = URL.createObjectURL(await res.blob());
  const a = Object.assign(document.createElement('a'), { href: url, download: filename });
  document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
}

/* ---------- Giao diện chung ---------- */
function toast(msg, type = '') {
  const t = Object.assign(document.createElement('div'), { className: 'toast ' + type, textContent: msg });
  $('#toasts').appendChild(t); setTimeout(() => t.remove(), 3800);
}
function showModal(html) {
  $('#modalBox').innerHTML = html; $('#modal').classList.remove('hidden');
}
function closeModal() { $('#modal').classList.add('hidden'); $('#modalBox').innerHTML = ''; }
$('#modal').addEventListener('click', e => { if (e.target.id === 'modal') closeModal(); });

function errText(e) {
  if (e.errors) return Object.values(e.errors).join('; ');
  return e.message;
}
const fail = e => toast(errText(e), 'bad');
/** Chạy hành động, báo lỗi bằng toast. */
async function act(fn, okMsg) {
  try { const r = await fn(); if (okMsg) toast(okMsg, 'ok'); return r ?? true; } catch (e) { fail(e); return false; }
}
function confirmBox(message) { return window.confirm(message); }
function askReason(title) {
  return new Promise(resolve => {
    showModal(`<h2>${esc(title)}</h2><label>Lý do</label><textarea id="reasonTxt" rows="3" maxlength="255"></textarea>
      <div class="row" style="margin-top:14px;justify-content:flex-end"><button id="rCancel">Đóng</button><button class="btn-primary" id="rOk">Xác nhận</button></div>`);
    $('#rCancel').onclick = () => { closeModal(); resolve(null); };
    $('#rOk').onclick = () => { const v = $('#reasonTxt').value.trim(); if (!v) return toast('Vui lòng nhập lý do', 'bad'); closeModal(); resolve(v); };
  });
}
function pager(page, onChange, host) {
  host.innerHTML = page.totalPages > 1 ? `<button ${page.first ? 'disabled' : ''} data-p="${page.number - 1}">← Trước</button>
    <span class="muted small">Trang ${page.number + 1}/${page.totalPages}</span>
    <button ${page.last ? 'disabled' : ''} data-p="${page.number + 1}">Sau →</button>` : '';
  $$('button', host).forEach(b => b.onclick = () => onChange(+b.dataset.p));
}
function setTimer(fn, ms) { const id = setInterval(fn, ms); App.timers.push(id); }
function clearTimers() { App.timers.forEach(clearInterval); App.timers = []; if (App.stopCam) { App.stopCam(); App.stopCam = null; } }
const loading = () => '<div class="loading">Đang tải…</div>';

const CAT_STYLE = { 'Hội thảo': ['#2740e8', '#7a8dff', '🎤'], 'Workshop': ['#0b7a75', '#35c7b4', '🛠️'], 'Ngày hội việc làm': ['#b4310f', '#ff8f5e', '💼'], 'Thể thao': ['#1f7a3d', '#7fdc93', '⚽'] };
const CAT_FALLBACK = [['#6d28d9', '#a78bfa'], ['#be185d', '#f472b6'], ['#0e7490', '#22d3ee'], ['#b45309', '#fbbf24']];
/** Ảnh bìa: dùng ảnh người tổ chức nhập, nếu không có thì sinh nền màu + biểu tượng theo loại sự kiện. */
function coverInfo(cat, url) {
  if (url) return { css: `background:linear-gradient(rgba(18,23,43,.05),rgba(18,23,43,.4)),url('${esc(url)}') center/cover`, icon: '' };
  const [c1, c2, icon] = CAT_STYLE[cat] || [...CAT_FALLBACK[(cat || '').length % 4], '🎫'];
  return { css: `background:linear-gradient(135deg,${c1},${c2})`, icon };
}
function eventCard(e) {
  const taken = e.capacity - e.availableSeats, pct = e.capacity ? Math.min(100, Math.round(taken * 100 / e.capacity)) : 0;
  const d = new Date(e.startAt), cv = coverInfo(e.categoryName, e.coverImageUrl);
  const low = e.availableSeats <= 0 || e.availableSeats <= Math.max(5, e.capacity * 0.1);
  return `<a class="card event-card" href="#/events/${e.id}">
    <div class="thumb" style="${cv.css}"><span class="thumb-icon">${cv.icon}</span>
      <div class="datebox"><b>${String(d.getDate()).padStart(2, '0')}</b><small>Th${d.getMonth() + 1}</small></div><span class="thumb-badge">${badge(e.status)}</span></div>
    <div class="body"><div class="cat">${esc(e.categoryName)}</div><h3>${esc(e.title)}</h3>
      <div class="meta">🕒 ${fmtDT(e.startAt)}</div><div class="meta">📍 ${esc(e.venueName)}</div>
      <div class="progress"><i style="width:${pct}%"></i></div>
      <div class="seats ${low ? 'low' : ''}">${e.availableSeats > 0 ? `Còn ${e.availableSeats}/${e.capacity} chỗ` : 'Đã hết chỗ'}</div></div></a>`;
}

/* ---------- Thanh điều hướng ---------- */
async function renderNav() {
  const u = App.user, links = [['#/', 'Sự kiện']];
  if (u) {
    if (Auth.has('ATTENDEE')) links.push(['#/tickets', 'Vé của tôi']);
    if (Auth.has('ORGANIZER')) links.push(['#/organizer', 'Quản lý sự kiện']);
    if (Auth.has('STAFF', 'ORGANIZER')) links.push(['#/checkin', 'Check-in']);
    if (Auth.has('ADMIN')) links.push(['#/admin', 'Quản trị']);
    links.push(['#/notifications', 'Thông báo <span id="unread"></span>'], ['#/profile', '👤 ' + esc(u.fullName.split(' ').slice(-1)[0])]);
  } else links.push(['#/login', 'Đăng nhập'], ['#/register', 'Đăng ký']);
  const cur = location.hash || '#/';
  $('#nav').innerHTML = links.map(([h, t]) => `<a href="${h}" class="${cur === h || (h !== '#/' && cur.startsWith(h)) ? 'active' : ''}">${t}</a>`).join('')
    + (u ? '<button class="link" id="logout">Đăng xuất</button>' : '');
  $('#nav').classList.remove('open');
  if (u) { $('#logout').onclick = () => Auth.logout(); updateUnread(); }
}
async function updateUnread() {
  try {
    const p = await api('/api/me/notifications?size=50'), n = p.content.filter(x => !x.read).length, el = $('#unread');
    if (el) el.innerHTML = n ? `<span class="badge-count">${n}</span>` : '';
  } catch { }
}
$('#burger').onclick = () => $('#nav').classList.toggle('open');
