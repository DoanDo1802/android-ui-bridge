# Android UI Bridge (No root, Android 9+)

Prototype app implementing:
- AccessibilityService for UI actions
- Foreground Service to stay alive
- Local HTTP API on `127.0.0.1:8080`

## API
`POST /action`

Examples:

```bash
curl -s -X POST http://127.0.0.1:8080/action \
  -H 'Content-Type: application/json' \
  -d '{"type":"click_text","value":"Đăng nhập"}'
```

```bash
curl -s -X POST http://127.0.0.1:8080/action \
  -H 'Content-Type: application/json' \
  -d '{"type":"get_ui_tree"}'
```

Supported `type`:
- `click_text`
- `click_id`
- `input_text`
- `scroll`
- `back`
- `get_ui_tree`

## Important setup (OPPO/Android 9)
1. Enable Accessibility for **UI Bridge** manually.
2. Disable battery optimization for app.
3. Enable auto-start / background run permission.
4. Lock app in recents (prevent system kill).

## Notes
- Prototype quality; add auth token + retries before production use.
- Server binds localhost only, not LAN.
