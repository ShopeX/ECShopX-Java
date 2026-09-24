#!/usr/bin/env bash
# Shared Docker image bootstrap for full/lite install.
# Download one CDN gzip bundle, then: gunzip -c FILE | docker load
# Sourced by docker-lite/lib/images.sh and dev-setup.sh.

DOCKER_IMAGES_BUNDLE_URL_DEFAULT="https://shopex-onex-yundian-image.yuanyuanke.cn/ecx-java-docker-image-base/ecx-java-docker-images.zip"

install_docker_images_bundle_url() {
  printf '%s' "${DOCKER_IMAGES_BUNDLE_URL:-$DOCKER_IMAGES_BUNDLE_URL_DEFAULT}"
}

install_download_file() {
  local url=$1
  local dest=$2
  if [ -f "$dest" ] && [ -s "$dest" ]; then
    echo "[images] 已存在，跳过下载: $dest"
    return 0
  fi
  if [ -z "$url" ]; then
    echo "[images] ERROR: empty download URL" >&2
    return 1
  fi
  mkdir -p "$(dirname "$dest")"
  local tmp="${dest}.partial"
  echo "[images] 下载镜像包: $url"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 --retry-delay 2 --progress-bar -o "$tmp" "$url" || {
      rm -f "$tmp"
      echo "[images] ERROR: 下载失败: $url" >&2
      return 1
    }
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$tmp" "$url" || {
      rm -f "$tmp"
      echo "[images] ERROR: 下载失败: $url" >&2
      return 1
    }
  else
    echo "[images] ERROR: 需要 curl 或 wget" >&2
    return 1
  fi
  mv "$tmp" "$dest"
  echo "[images] 已下载: $dest"
}

# Download CDN gzip docker-save bundle and import into local Docker.
# Usage: install_ensure_docker_images_from_cdn [dest_dir]
install_ensure_docker_images_from_cdn() {
  local dest_dir=${1:-}
  local url bundle_name dest

  if ! command -v docker >/dev/null 2>&1; then
    echo "[images] ERROR: docker 未安装" >&2
    return 1
  fi
  if ! command -v gunzip >/dev/null 2>&1 && ! command -v gzip >/dev/null 2>&1; then
    echo "[images] ERROR: 需要 gunzip 或 gzip" >&2
    return 1
  fi

  url=$(install_docker_images_bundle_url)
  bundle_name=$(basename "${url%%\?*}")
  [ -n "$bundle_name" ] || bundle_name="ecx-java-docker-images.zip"

  if [ -z "$dest_dir" ]; then
    dest_dir="${TMPDIR:-/tmp}/ecshopx-docker-images"
  fi
  dest="${dest_dir}/${bundle_name}"

  install_download_file "$url" "$dest" || return 1

  echo "[images] 导入镜像: gunzip -c $dest | docker load"
  if command -v gunzip >/dev/null 2>&1; then
    gunzip -c "$dest" | docker load || {
      echo "[images] ERROR: docker load 失败（请确认包为 gzip 压缩的 docker save）" >&2
      return 1
    }
  else
    gzip -dc "$dest" | docker load || {
      echo "[images] ERROR: docker load 失败（请确认包为 gzip 压缩的 docker save）" >&2
      return 1
    }
  fi
  echo "[images] 镜像已导入本地 Docker"
}
