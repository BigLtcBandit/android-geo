const qs = new URLSearchParams(window.location.search);
const channelSlug = qs.get('channel') || qs.get('user') || qs.get('u');
const maxMessages = Number(qs.get('max') || 20);
const messageTTLms = Number(qs.get('ttl_ms') || 20000);
const showAvatars = (qs.get('avatars') || '1') !== '0';
const showBadges = (qs.get('badges') || '1') !== '0';

const messagesEl = document.getElementById('messages');

if (!channelSlug) {
  messagesEl.innerHTML = `<div style="color:#fff;font:600 16px/1.4 system-ui">Add ?channel=YOUR_KICK_USERNAME to the URL</div>`;
} else {
  startOverlay(channelSlug).catch(err => {
    console.error('[overlay] failed to start', err);
    messagesEl.innerHTML = `<div style="color:#ff6b6b;font:600 14px/1.4 system-ui">Error: ${escapeHtml(String(err.message || err))}</div>`;
  });
}

async function startOverlay(slug) {
  const chatroomId = await getChatroomId(slug);
  if (!chatroomId) {
    throw new Error('Could not resolve chatroom id');
  }
  console.log('[overlay] chatroom id', chatroomId);

  const socket = window.io('https://ws-relay.kick.com', {
    transports: ['websocket'],
    path: '/socket.io/',
    reconnection: true,
    reconnectionAttempts: Infinity,
    reconnectionDelay: 1000,
    reconnectionDelayMax: 5000,
    timeout: 20000,
  });

  socket.on('connect', () => {
    console.log('[overlay] connected to ws-relay');
    socket.emit('join', { room: `chatrooms:${chatroomId}` });
  });

  socket.on('disconnect', (reason) => {
    console.log('[overlay] disconnected', reason);
  });

  socket.on('connect_error', (err) => {
    console.error('[overlay] connect_error', err);
  });

  socket.on('App\\\Events\\\ChatMessageEvent', (payload) => {
    try {
      const rendered = normalizeChatMessage(payload);
      if (rendered) renderMessage(rendered);
    } catch (e) { console.error('failed to render message', e, payload); }
  });
  socket.on('App\\Events\\ChatMessageEvent', (payload) => {
    try {
      const rendered = normalizeChatMessage(payload);
      if (rendered) renderMessage(rendered);
    } catch (e) { console.error('failed to render message', e, payload); }
  });
}

async function getChatroomId(slug) {
  const res = await fetch(`/api/chatroom-id?slug=${encodeURIComponent(slug)}`);
  if (!res.ok) throw new Error(`Failed to resolve chatroom id: ${res.status}`);
  const data = await res.json();
  return data.chatroomId || null;
}

function normalizeChatMessage(payload) {
  const sender = payload?.sender || payload?.data?.sender || payload?.message?.sender || {};
  const messageObj = payload?.message || payload?.data?.message || payload?.content || {};
  const username = sender?.username || sender?.slug || 'unknown';
  const userId = sender?.id || null;
  const avatarUrl = sender?.profile_picture || sender?.avatar || null;
  const content = messageObj?.message || messageObj?.content || payload?.message || '';
  const badges = (sender?.badges || payload?.badges || []).map(b => (typeof b === 'string' ? b : b?.text || b?.type)).filter(Boolean);
  if (!content || !username) return null;
  return { username, userId, avatarUrl, content: String(content), badges };
}

function renderMessage(msg) {
  while (messagesEl.children.length >= maxMessages) {
    messagesEl.removeChild(messagesEl.firstChild);
  }
  const el = document.createElement('div');
  el.className = 'message';
  if (showAvatars) {
    const avatar = document.createElement('div');
    avatar.className = 'avatar';
    if (msg.avatarUrl) {
      const img = document.createElement('img');
      img.src = msg.avatarUrl; img.alt = '';
      avatar.appendChild(img);
    }
    el.appendChild(avatar);
  }
  const header = document.createElement('div');
  header.className = 'header';
  const nameSpan = document.createElement('span');
  nameSpan.className = 'username';
  nameSpan.textContent = msg.username;
  header.appendChild(nameSpan);
  if (showBadges && Array.isArray(msg.badges) && msg.badges.length) {
    const badgesEl = document.createElement('span');
    badgesEl.className = 'badges';
    for (const b of msg.badges) {
      const badgeEl = document.createElement('span');
      badgeEl.className = 'badge';
      badgeEl.textContent = b;
      badgesEl.appendChild(badgeEl);
    }
    header.appendChild(badgesEl);
  }
  el.appendChild(header);
  const content = document.createElement('div');
  content.className = 'content';
  content.innerHTML = renderRichText(msg.content);
  el.appendChild(content);
  messagesEl.appendChild(el);
  if (messageTTLms > 0) {
    setTimeout(() => { el.classList.add('fade-out'); setTimeout(() => el.remove(), 650); }, messageTTLms);
  }
}

function renderRichText(text) {
  const safe = escapeHtml(text);
  return linkify(safe);
}

function escapeHtml(str) {
  return str
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function linkify(str) {
  const urlRegex = /(https?:\/\/[^\s]+)/g;
  return str.replace(urlRegex, (url) => `<a href="${url}" target="_blank" rel="noopener noreferrer" style="color: var(--accent); text-decoration: none;">${url}</a>`);
}