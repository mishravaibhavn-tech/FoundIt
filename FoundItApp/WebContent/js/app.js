async function post(url, params) {
  const res = await fetch(url, { method:'POST', body:new URLSearchParams(params) });
  return res.json();
}

async function get(url, params) {
  const qs = new URLSearchParams(params).toString();
  const res = await fetch(url + (qs ? '?' + qs : ''));
  return res.json();
}

function say(text, ok) {
  const el = document.getElementById('msg');
  if (!el) return;
  el.textContent = text || '';
  el.className = 'msg ' + (text ? (ok ? 'ok' : 'err') : '');
}

function escapeHtml(s) {
  return (s || '').replace(/[&<>"']/g, c =>
    ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

async function requireLogin() {
  const d = await get('auth', {action:'me'});
  if (!d.success) location.href = 'index.html';
}

document.addEventListener('click', async e => {
  if (e.target.id === 'logout') {
    e.preventDefault();
    await get('auth', {action:'logout'});
    location.href = 'index.html';
  }
});