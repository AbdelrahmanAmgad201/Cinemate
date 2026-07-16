#!/bin/sh
# Generates a runtime config file from environment variables at container start
# (FE-02), instead of baking URLs into the Vite build. Lets the same built image
# work across environments — just change env vars and restart the container,
# no rebuild needed.
set -e

# Note the `-` (not `:-`): an explicitly-set empty value is honored as "same origin"
# (the SPA is served through the gateway, so /api and /ws are same-origin). The
# default only applies when the var is entirely unset (standalone image runs).
cat > /usr/share/nginx/html/config.js <<EOF
window.__CINEMATE_CONFIG__ = {
  API_BASE_URL: "${API_BASE_URL-http://localhost:8080}",
  WATCH_PARTY_BASE_URL: "${WATCH_PARTY_BASE_URL-}"
};
EOF

exec "$@"
