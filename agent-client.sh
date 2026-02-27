#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ACTION=${1:-get_ui_tree}
VALUE=${2:-}

if [ -n "$VALUE" ]; then
  DATA=$(printf '{"type":"%s","value":"%s"}' "$ACTION" "$VALUE")
else
  DATA=$(printf '{"type":"%s"}' "$ACTION")
fi

curl -s -X POST http://127.0.0.1:8080/action \
  -H 'Content-Type: application/json' \
  -d "$DATA"

echo
