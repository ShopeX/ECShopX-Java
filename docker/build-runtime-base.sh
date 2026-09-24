#!/usr/bin/env bash
# Build (and optionally save / push) the app runtime base image.
# Default tag: ecshopx-java:17-node20-openresty
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

IMAGE_NAME="${RUNTIME_BASE_IMAGE:-ecshopx-java:17-node20-openresty}"
SAVE_PATH=""
PUSH=false

usage() {
  cat <<'EOF'
Usage: ./docker/build-runtime-base.sh [options]

Options:
  --tag NAME           Image tag (default ecshopx-java:17-node20-openresty, or env RUNTIME_BASE_IMAGE)
  --save PATH          docker save -o PATH after build
  --push               docker push the image tag
  -h, --help           Show help
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --tag) IMAGE_NAME="${2:-}"; shift 2 ;;
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
echo "[build] docker build -f docker/Dockerfile.runtime -t $IMAGE_NAME ."
docker build -f docker/Dockerfile.runtime -t "$IMAGE_NAME" .

# Keep short local alias for convenience.
if [ "$IMAGE_NAME" != "ecshopx-java:17-node20-openresty" ]; then
  docker tag "$IMAGE_NAME" "ecshopx-java:17-node20-openresty" 2>/dev/null || true
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
