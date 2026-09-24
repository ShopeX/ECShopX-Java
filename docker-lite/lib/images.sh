#!/usr/bin/env bash
# docker-lite image helpers: CDN bundle download + docker load (no registry pull).

# shellcheck source=../../docker/install-docker-images.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/docker/install-docker-images.sh"

release_images_dir() {
  printf '%s' "${RELEASE_LITE_DIR}/images"
}

release_images_env_file() {
  printf '%s' "${RELEASE_LITE_DIR}/images.env"
}

release_load_images_env() {
  local env_file
  env_file=$(release_images_env_file)
  if [ ! -f "$env_file" ]; then
    if [ -f "${env_file}.example" ]; then
      release_log_info "创建 docker-lite/images.env from example"
      cp "${env_file}.example" "$env_file"
    else
      release_log_error "missing $env_file (and .example)"
      return 1
    fi
  fi
  # shellcheck disable=SC1090
  set -a
  # shellcheck disable=SC1091
  . "$env_file"
  set +a
}

# Download CDN image bundle and docker load (used by deploy.sh).
release_ensure_runtime_images() {
  release_load_images_env || return 1
  if [ -n "${DOCKER_IMAGES_BUNDLE_URL:-}" ]; then
    export DOCKER_IMAGES_BUNDLE_URL
  fi

  release_log_info "从 CDN 下载并导入 Docker 镜像（不从仓库 pull）..."
  if ! install_ensure_docker_images_from_cdn "$(release_images_dir)"; then
    release_log_error "Docker 镜像导入失败"
    return 1
  fi
  release_log_success "运行时镜像已就绪"
}

# Prefetch CDN bundle into docker-lite/images/ (pack.sh --with-images).
release_prefetch_image_tars() {
  release_load_images_env || return 1
  if [ -n "${DOCKER_IMAGES_BUNDLE_URL:-}" ]; then
    export DOCKER_IMAGES_BUNDLE_URL
  fi
  local images_dir url name
  images_dir=$(release_images_dir)
  mkdir -p "$images_dir"
  url=$(install_docker_images_bundle_url)
  name=$(basename "${url%%\?*}")
  [ -n "$name" ] || name="ecx-java-docker-images.zip"
  install_download_file "$url" "${images_dir}/${name}" || return 1
}
