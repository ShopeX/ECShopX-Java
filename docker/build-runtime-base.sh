#!/usr/bin/env bash
# Build (and optionally save / push) the app runtime base image.
# Default tag: ecshopx-java:17-node22-openresty
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

IMAGE_NAME="${RUNTIME_BASE_IMAGE:-ecshopx-java:17-node22-openresty}"
PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"
SAVE_PATH=""
PUSH=false

usage() {
  cat <<'EOF'
Usage: ./docker/build-runtime-base.sh [options]

Options:
  --tag NAME           Image tag (default ecshopx-java:17-node22-openresty, or env RUNTIME_BASE_IMAGE)
  --platform PLAT      Target platform (default linux/amd64, or env DOCKER_PLATFORM)
  --save PATH          docker save -o PATH after build
  --push               docker push the image tag
  -h, --help           Show help
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --tag) IMAGE_NAME="${2:-}"; shift 2 ;;
    --platform) PLATFORM="${2:-}"; shift 2 ;;
    --save) SAVE_PATH="${2:-}"; shift 2 ;;
    --push) PUSH=true; shift ;;
    --registry)
      echo "NOTE: --registry is obsolete; pass full image with --tag / RUNTIME_BASE_IMAGE" >&2
      shift 2
      ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "unknown arg: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

[ -n "$IMAGE_NAME" ] || { echo "empty image tag" >&2; exit 1; }

cd "$PROJECT_ROOT"

# CVE-2026-114938 (Java4Shell): pin Temurin 17.0.21.1+2 JRE (vendor tarball, bind-mounted at build).
VENDOR_DIR="$PROJECT_ROOT/docker/vendor"
TEMURIN_TGZ="$VENDOR_DIR/OpenJDK17U-jre_x64_linux_hotspot_17.0.21.1_2.tar.gz"
TEMURIN_SHA256="${TEMURIN_JRE_SHA256:-5a092a0bc21de4c3d855480edd7945be9cfe157b38cdb84dac280511893bb009}"
TEMURIN_URLS=(
  "${TEMURIN_JRE_URL:-}"
  "https://ghfast.top/https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21.1%2B2/OpenJDK17U-jre_x64_linux_hotspot_17.0.21.1_2.tar.gz"
  "https://api.adoptium.net/v3/binary/version/jdk-17.0.21.1%2B2/linux/x64/jre/hotspot/normal/eclipse?project=jdk"
  "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21.1%2B2/OpenJDK17U-jre_x64_linux_hotspot_17.0.21.1_2.tar.gz"
)
mkdir -p "$VENDOR_DIR"
if [ ! -f "$TEMURIN_TGZ" ]; then
  fetched=false
  for url in "${TEMURIN_URLS[@]}"; do
    [ -n "$url" ] || continue
    echo "[fetch] $url"
    if curl -fL --connect-timeout 20 --retry 2 --retry-delay 1 -o "${TEMURIN_TGZ}.partial" "$url" \
      && echo "$TEMURIN_SHA256  ${TEMURIN_TGZ}.partial" | shasum -a 256 -c -; then
      mv "${TEMURIN_TGZ}.partial" "$TEMURIN_TGZ"
      fetched=true
      break
    fi
    rm -f "${TEMURIN_TGZ}.partial"
  done
  [ "$fetched" = true ] || { echo "ERROR: failed to download Temurin 17.0.21.1+2 JRE" >&2; exit 1; }
fi
echo "$TEMURIN_SHA256  $TEMURIN_TGZ" | shasum -a 256 -c -

# Node 22.23.4 official linux-x64 tarball (NodeSource apt no longer ships this pin).
NODE_TGZ="$VENDOR_DIR/node-v22.23.4-linux-x64.tar.gz"
NODE_SHA256="${NODE_SHA256:-b8492284c76a54baf58d61668dd0e41ee0fc98adabf9503d30f05d08f55b4406}"
NODE_URLS=(
  "${NODE_URL:-}"
  "https://npmmirror.com/mirrors/node/v22.23.4/node-v22.23.4-linux-x64.tar.gz"
  "https://nodejs.org/dist/v22.23.4/node-v22.23.4-linux-x64.tar.gz"
)
if [ ! -f "$NODE_TGZ" ]; then
  fetched=false
  for url in "${NODE_URLS[@]}"; do
    [ -n "$url" ] || continue
    echo "[fetch] $url"
    if curl -fL --connect-timeout 20 --retry 2 --retry-delay 1 -o "${NODE_TGZ}.partial" "$url" \
      && echo "$NODE_SHA256  ${NODE_TGZ}.partial" | shasum -a 256 -c -; then
      mv "${NODE_TGZ}.partial" "$NODE_TGZ"
      fetched=true
      break
    fi
    rm -f "${NODE_TGZ}.partial"
  done
  [ "$fetched" = true ] || { echo "ERROR: failed to download Node 22.23.4" >&2; exit 1; }
fi
echo "$NODE_SHA256  $NODE_TGZ" | shasum -a 256 -c -

echo "[build] docker build --platform $PLATFORM -f docker/Dockerfile.runtime -t $IMAGE_NAME ."
DOCKER_BUILDKIT=1 docker build --platform "$PLATFORM" -f docker/Dockerfile.runtime -t "$IMAGE_NAME" .

# Keep short local alias for convenience.
if [ "$IMAGE_NAME" != "ecshopx-java:17-node22-openresty" ]; then
  docker tag "$IMAGE_NAME" "ecshopx-java:17-node22-openresty" 2>/dev/null || true
fi

if [ "$PUSH" = true ]; then
  echo "[push] $IMAGE_NAME"
  docker push "$IMAGE_NAME"
fi

if [ -n "$SAVE_PATH" ]; then
  mkdir -p "$(dirname "$SAVE_PATH")"
  echo "[save] $SAVE_PATH ($IMAGE_NAME)"
  docker save -o "$SAVE_PATH" "$IMAGE_NAME"
  echo "[save] done: $SAVE_PATH"
fi

echo "[ok] $IMAGE_NAME"
