#!/bin/sh
set -e
JAR="${APP_JAR:-/app/app.jar}"
NUXT_ENTRY="${NUXT_ENTRY:-/data/httpd/ECShopX-Java_Web/.output/server/index.mjs}"

ADMIN_HOST="${ADMIN_HOST:-admin.ecshopx.test}"
H5_HOST="${H5_HOST:-h5.ecshopx.test}"
PC_HOST="${PC_HOST:-www.ecshopx.test}"
export ADMIN_HOST H5_HOST PC_HOST
NGINX_TEMPLATE="${NGINX_TEMPLATE:-/app/nginx.conf.template}"
NGINX_CONF="${NGINX_CONF:-/usr/local/openresty/nginx/conf/nginx.conf}"
[ -f "$NGINX_TEMPLATE" ] || { echo "ERROR: missing $NGINX_TEMPLATE" >&2; exit 1; }
envsubst '${ADMIN_HOST} ${H5_HOST} ${PC_HOST}' < "$NGINX_TEMPLATE" > "$NGINX_CONF"

[ -f "$JAR" ] || { echo "ERROR: missing $JAR" >&2; exit 1; }
[ -f "$NUXT_ENTRY" ] || { echo "ERROR: missing Nuxt output $NUXT_ENTRY" >&2; exit 1; }

mkdir -p /var/log/nginx /var/lib/nginx/tmp/client_body /var/lib/nginx/tmp/proxy \
  /var/lib/nginx/tmp/fastcgi /var/lib/nginx/tmp/uwsgi /var/lib/nginx/tmp/scgi

echo "[app] starting Java..."
java $JAVA_OPTS -jar "$JAR" $APP_ARGS &
JAVA_PID=$!

echo "[app] starting Nuxt..."
# Nitro defaults to [::]:3000; nginx proxies 127.0.0.1:3000 — force IPv4.
export HOST=0.0.0.0
export PORT=3000
export NITRO_HOST=0.0.0.0
export NITRO_PORT=3000
# Override build-time apiBaseInternal (SSR must hit local Java, not old hostname).
export NUXT_API_BASE_INTERNAL="${NUXT_API_BASE_INTERNAL:-http://127.0.0.1:18080/api/v1/h5app}"
export NUXT_API_BASE="${NUXT_API_BASE:-$NUXT_API_BASE_INTERNAL}"
node "$NUXT_ENTRY" &
NODE_PID=$!

# OpenResty serves admin/h5 immediately once Java is up. Do NOT wait for Nuxt SSR HTTP
# (SSR can hang if apiBaseInternal is wrong; that used to delay :8080 by ~3min → RST).
i=0
while [ "$i" -lt 90 ]; do
  jcode=$(curl -s -o /dev/null -w '%{http_code}' -m 1 http://127.0.0.1:18080/ 2>/dev/null || true)
  case "$jcode" in ''|000) ;; *) break ;; esac
  sleep 2
  i=$((i + 1))
done

echo "[app] starting OpenResty..."
openresty -g 'daemon off;' &
NGINX_PID=$!

term() {
  kill -TERM "$JAVA_PID" "$NODE_PID" "$NGINX_PID" 2>/dev/null || true
  wait "$JAVA_PID" "$NODE_PID" "$NGINX_PID" 2>/dev/null || true
}
trap term INT TERM

# Exit only if Java or OpenResty dies. Nuxt is restarted so PC outages
# do not take down admin/H5/API (same process model as full install expects).
while kill -0 "$JAVA_PID" 2>/dev/null && kill -0 "$NGINX_PID" 2>/dev/null; do
  if ! kill -0 "$NODE_PID" 2>/dev/null; then
    echo "[app] WARNING: Nuxt exited; restarting..." >&2
    node "$NUXT_ENTRY" &
    NODE_PID=$!
  fi
  sleep 2
done
echo "ERROR: a child process exited" >&2
term
exit 1
