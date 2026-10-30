#!/usr/bin/env bash
# Pack the CDN Docker image bundle the same way install-docker-images.sh expects:
#
#   docker save <tags...> | gzip > ecx-java-docker-images.zip
#   gunzip -c ecx-java-docker-images.zip | docker load
#
# Note: the file extension is .zip historically, but the payload is gzip'd
# docker save (not a ZIP archive). Docker Engine 29+ with containerd image
# store emits OCI layout; older engines emitted legacy docker-archive.
# Both load via the same gunzip | docker load pipeline.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT_DIR="${OUT_DIR:-$PROJECT_ROOT/docker/images}"
OUT_FILE="${OUT_FILE:-$OUT_DIR/ecx-java-docker-images.zip}"

# Keep in sync with docker-lite/images.env
IMAGES=(
  "${RUNTIME_BASE_IMAGE:-ecshopx-java:17-node22-openresty}"
  "${MAVEN_BUILD_IMAGE:-maven:3.9-eclipse-temurin-17}"
  "${MYSQL_IMAGE:-mysql:8.0}"
  "${REDIS_IMAGE:-redis:7-alpine}"
  "${XXL_JOB_IMAGE:-xuxueli/xxl-job-admin:2.5.0}"
)

usage() {
  cat <<'EOF'
Usage: ./docker/pack-docker-images-bundle.sh

Env:
  OUT_DIR / OUT_FILE   Output path (default docker/images/ecx-java-docker-images.zip)
  RUNTIME_BASE_IMAGE / MAVEN_BUILD_IMAGE / MYSQL_IMAGE / REDIS_IMAGE / XXL_JOB_IMAGE
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

for img in "${IMAGES[@]}"; do
  docker image inspect "$img" >/dev/null 2>&1 || {
    echo "ERROR: missing image: $img" >&2
    exit 1
  }
done

mkdir -p "$(dirname "$OUT_FILE")"
tmp="${OUT_FILE}.partial"
rm -f "$tmp"
echo "[pack] docker save ${IMAGES[*]} | gzip > $OUT_FILE"
docker save "${IMAGES[@]}" | gzip -c > "$tmp"
mv "$tmp" "$OUT_FILE"

echo "[verify] format + tags"
file "$OUT_FILE"
# Prefer gzip -dc: macOS gunzip mis-handles a .zip suffix.
if gzip -dc "$OUT_FILE" | tar -t | grep -qx 'index.json'; then
  echo "[verify] OCI layout (index.json)"
  gzip -dc "$OUT_FILE" | tar -xO index.json | python3 -m json.tool | head -80
elif gzip -dc "$OUT_FILE" | tar -t | grep -qx 'repositories'; then
  echo "[verify] legacy docker-archive (repositories)"
  gzip -dc "$OUT_FILE" | tar -xO repositories | python3 -m json.tool
else
  echo "ERROR: neither index.json nor repositories found in bundle" >&2
  exit 1
fi

ls -lh "$OUT_FILE"
echo "[ok] $OUT_FILE"
echo "[hint] upload to CDN path used by DOCKER_IMAGES_BUNDLE_URL, then clients run:"
echo "       gunzip -c ecx-java-docker-images.zip | docker load"
