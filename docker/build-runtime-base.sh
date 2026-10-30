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

# CVE-2026-91403: pin Temurin 17.0.21+9 JRE (vendor tarball, bind-mounted at build).
VENDOR_DIR="$PROJECT_ROOT/docker/vendor"
TEMURIN_TGZ="$VENDOR_DIR/OpenJDK17U-jre_x64_linux_hotspot_17.0.21_9.tar.gz"
TEMURIN_SHA256="${TEMURIN_JRE_SHA256:-9f41bf88c7900c7097444a1bdeb84146ae692bc08842490ab098063c74bf9a8a}"
TEMURIN_URLS=(
  "${TEMURIN_JRE_URL:-}"
  "https://ghfast.top/https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jre_x64_linux_hotspot_17.0.21_9.tar.gz"
  "https://api.adoptium.net/v3/binary/version/jdk-17.0.21%2B9/linux/x64/jre/hotspot/normal/eclipse?project=jdk"
  "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jre_x64_linux_hotspot_17.0.21_9.tar.gz"
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
  [ "$fetched" = true ] || { echo "ERROR: failed to download Temurin 17.0.21+9 JRE" >&2; exit 1; }
fi
echo "$TEMURIN_SHA256  $TEMURIN_TGZ" | shasum -a 256 -c -

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
