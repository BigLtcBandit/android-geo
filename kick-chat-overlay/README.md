Kick Chat Overlay

Run a lightweight overlay that connects directly to Kick chat and renders messages as a browser source in OBS.

Quick start

1) Install deps and run the server

```bash
cd /workspace/kick-chat-overlay
npm install
npm run start
```

2) Open the overlay in a browser (replace YOUR_CHANNEL)

```
http://localhost:5173/?channel=YOUR_CHANNEL
```

3) Add to OBS as a Browser Source

- URL: `http://localhost:5173/?channel=YOUR_CHANNEL`
- Width/Height: match your canvas (e.g., 1920x1080). The overlay is transparent and will sit on top.
- Custom CSS: leave blank (handled by the page)

URL parameters

- `channel` (required): your Kick channel slug/username
- `max` (default 20): max messages kept on screen
- `ttl_ms` (default 20000): how long each message stays before fading
- `avatars` (default 1): set to `0` to hide avatars
- `badges` (default 1): set to `0` to hide badges

Notes

- This connects anonymously to Kick's public chat relay and listens for `App\\Events\\ChatMessageEvent`.
- No authentication is required for reading public chat.
- If your chat is follower/sub-only, messages may still appear but write actions are not supported by this overlay.

Troubleshooting

- If no messages appear:
  - Verify the URL has `?channel=yourname` and that your channel exists
  - Open DevTools (F12) and check the Console for connection logs/errors
  - Some corporate firewalls block websockets; try a different network

License

MIT