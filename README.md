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
- `health`
- `click_text`
- `click_id`
- `input_text`
- `scroll`
- `back`
- `home`
- `recent`
- `open_app` (value: package name, e.g. `com.google.android.youtube`)
- `tap` (fields: `x`, `y`)
- `swipe` (fields: `x1`, `y1`, `x2`, `y2`, optional `durationMs`)
- `swipe_left`
- `swipe_right`
- `swipe_up`
- `swipe_down`
- `get_ui_tree`

## Important setup (OPPO/Android 9)
1. Enable Accessibility for **UI Bridge** manually.
2. Disable battery optimization for app.
3. Enable auto-start / background run permission.
4. Lock app in recents (prevent system kill).

## Quick YouTube smoke test
```bash
# Open YouTube app
curl -s -X POST http://127.0.0.1:8080/action -H 'Content-Type: application/json' \
  -d '{"type":"open_app","value":"com.google.android.youtube"}'

# Swipe up to browse
curl -s -X POST http://127.0.0.1:8080/action -H 'Content-Type: application/json' \
  -d '{"type":"swipe_up"}'

# Tap roughly center (adjust by your screen)
curl -s -X POST http://127.0.0.1:8080/action -H 'Content-Type: application/json' \
  -d '{"type":"tap","x":540,"y":1200}'
```

## Notes
- Prototype quality; add auth token + retries before production use.
- Server binds localhost only, not LAN.
