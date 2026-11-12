#!/bin/sh
set -e
JAR="${APP_JAR:-/app/app.jar}"
NGINX_CONF="${NGINX_CONF:-/usr/local/openresty/nginx/conf/nginx.conf}"

[ -f "$JAR" ] || { echo "ERROR: missing $JAR" >&2; exit 1; }
[ -f "$NGINX_CONF" ] || { echo "ERROR: missing $NGINX_CONF" >&2; exit 1; }

mkdir -p /var/log/nginx /var/lib/nginx/tmp/client_body /var/lib/nginx/tmp/proxy \
  /var/lib/nginx/tmp/fastcgi /var/lib/nginx/tmp/uwsgi /var/lib/nginx/tmp/scgi

echo "[java] starting Java..."
# shellcheck disable=SC2086
java $JAVA_OPTS -jar "$JAR" $APP_ARGS &
JAVA_PID=$!

# 等 Java 就绪后再拉起 OpenResty，避免启动窗口大量 502
i=0
while [ "$i" -lt 90 ]; do
  jcode=$(curl -s -o /dev/null -w '%{http_code}' -m 1 http://127.0.0.1:8080/ 2>/dev/null || true)
  case "$jcode" in ''|000) ;; *) break ;; esac
  sleep 2
  i=$((i + 1))
done

echo "[java] starting OpenResty..."
openresty -c "$NGINX_CONF" -g 'daemon off;' &
NGINX_PID=$!

term() {
  kill -TERM "$JAVA_PID" "$NGINX_PID" 2>/dev/null || true
  wait "$JAVA_PID" "$NGINX_PID" 2>/dev/null || true
}
trap term INT TERM

while kill -0 "$JAVA_PID" 2>/dev/null && kill -0 "$NGINX_PID" 2>/dev/null; do
  sleep 2
done
echo "ERROR: a child process exited" >&2
term
exit 1
