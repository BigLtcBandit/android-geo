import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = process.env.PORT || 5173;
const publicDir = path.join(__dirname, 'public');

app.disable('x-powered-by');

app.use((req, res, next) => {
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');
  next();
});

// Proxy to resolve chatroom id (avoids browser CORS)
app.get('/api/chatroom-id', async (req, res) => {
  try {
    const slug = String(req.query.slug || '').trim();
    if (!slug) {
      res.status(400).json({ error: 'missing slug' });
      return;
    }
    const url = `https://kick.com/api/v2/channels/${encodeURIComponent(slug)}`;
    const upstream = await fetch(url, { headers: { 'Accept': 'application/json' } });
    if (!upstream.ok) {
      res.status(upstream.status).json({ error: `upstream ${upstream.status}` });
      return;
    }
    const data = await upstream.json();
    const chatroomId = data?.chatroom?.id || data?.livestream?.chatroom?.id || null;
    res.json({ chatroomId, channel: data?.slug || slug });
  } catch (err) {
    res.status(500).json({ error: 'proxy_error', detail: String(err?.message || err) });
  }
});

app.use(express.static(publicDir, { extensions: ['html'] }));

app.get('/health', (req, res) => {
  res.json({ ok: true });
});

app.get('*', (req, res) => {
  res.sendFile(path.join(publicDir, 'index.html'));
});

app.listen(port, () => {
  console.log(`Kick chat overlay server running at http://localhost:${port}`);
});