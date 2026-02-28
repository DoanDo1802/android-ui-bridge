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
- `status` (active package + screen width/height)
- `click_text`
- `click_id`
- `input_text`
- `scroll`
- `back`
- `home`
- `recent`
- `open_app` (value: package name, e.g. `com.google.android.youtube`)
- `tap` (fields: `x`, `y`)
- `tap_ratio` (fields: `x`,`y` in range 0..1)
- `swipe` (fields: `x1`, `y1`, `x2`, `y2`, optional `durationMs`)
- `swipe_pages` (fields: `count`, optional `direction` = `up|down|left|right`)
- `swipe_left`
- `swipe_right`
- `swipe_up`
- `swipe_down`
- `macro_youtube` (fields: optional `query`, `pages`, `tapY`)
- `capture_temp` (temporary screenshot for analysis)
- `tap_ratio_ephemeral` (capture -> tap -> auto-delete screenshot)
- `cleanup_captures` (delete all temp captures)
- `get_ui_tree`

## Important setup (OPPO/Android 9)
1. Enable Accessibility for **UI Bridge** manually.
2. Disable battery optimization for app.
3. Enable auto-start / background run permission.
4. Lock app in recents (prevent system kill).

## Quick YouTube smoke test
```bash
# One-shot macro: open YouTube, search, scroll pages, open one result
curl -s -X POST http://127.0.0.1:8080/action -H 'Content-Type: application/json' \
  -d '{"type":"macro_youtube","query":"a do mixi na na na a phung thanh do","pages":3,"tapY":0.56}'

# Or manual with ratio tap (0..1)
curl -s -X POST http://127.0.0.1:8080/action -H 'Content-Type: application/json' \
  -d '{"type":"open_app","value":"com.google.android.youtube"}'
curl -s -X POST http://127.0.0.1:8080/action -H 'Content-Type: application/json' \
  -d '{"type":"swipe_pages","count":2,"direction":"up"}'
curl -s -X POST http://127.0.0.1:8080/action -H 'Content-Type: application/json' \
  -d '{"type":"tap_ratio","x":0.5,"y":0.56}'
```

## Temp screenshot policy
- Launch UI Bridge app once and **allow screen capture** prompt (MediaProjection).
- Temp captures are written to app cache (`.../cache/captures`).
- Use `tap_ratio_ephemeral` for one-shot flow: capture -> tap -> delete immediately.
- Run `cleanup_captures` as a safety cleanup step.
- Check readiness with `health` (`captureReady: true`).

## Notes
- Prototype quality; add auth token + retries before production use.
- Server binds localhost only, not LAN.
