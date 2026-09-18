#!/usr/bin/env bash
set -euo pipefail

SCRIPT_PATH="${BASH_SOURCE[0]}"
LITE_DIR="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"
# shellcheck source=lib/common.sh
source "$LITE_DIR/lib/common.sh"
# shellcheck source=lib/artifacts.sh
source "$LITE_DIR/lib/artifacts.sh"
# shellcheck source=lib/package.sh
source "$LITE_DIR/lib/package.sh"
# shellcheck source=lib/images.sh
source "$LITE_DIR/lib/images.sh"
release_init_paths "$SCRIPT_PATH"

# Admin / mobile / PC all build on Node 20 (no longer switch to 16).
RELEASE_NODE="${RELEASE_NODE:-20.19}"
RELEASE_PNPM_VERSION="${RELEASE_PNPM_VERSION:-10.13.0}"
APP_IMAGE_CDN="${APP_IMAGE_CDN:-https://ecshopx-vshop-images.oss-cn-shanghai.aliyuncs.com}"
WITH_IMAGES=false

set_env_kv() {
  local file=$1 key=$2 value=$3
  mkdir -p "$(dirname "$file")"
  touch "$file"
  if grep -q "^${key}=" "$file" 2>/dev/null; then
    if [[ "${OSTYPE:-}" == darwin* ]]; then
      sed -i '' "s|^${key}=.*|${key}=${value}|" "$file"
    else
      sed -i "s|^${key}=.*|${key}=${value}|" "$file"
    fi
  else
    echo "${key}=${value}" >> "$file"
  fi
}

prepare_release_envs() {
  set_env_kv "$RELEASE_ADMIN_DIR/.env" "VUE_APP_BASE_API" "/api/v1"
  set_env_kv "$RELEASE_ADMIN_DIR/.env" "VUE_APP_DEFAULT_LANG" "zhcn"
  set_env_kv "$RELEASE_ADMIN_DIR/.env" "VUE_APP_PUBLIC_PATH" "/"
  set_env_kv "$RELEASE_MOBILE_DIR/.env" "APP_BASE_URL" "/api/v1/h5app/wxapp"
  set_env_kv "$RELEASE_MOBILE_DIR/.env" "APP_DEFAULT_LANGUAGE" "zhcn"
  set_env_kv "$RELEASE_MOBILE_DIR/.env" "APP_I18N_ORIGIN_LANG" "zhcn"
  set_env_kv "$RELEASE_MOBILE_DIR/.env" "APP_PUBLIC_PATH" "/"
  set_env_kv "$RELEASE_MOBILE_DIR/.env" "APP_IMAGE_CDN" "$APP_IMAGE_CDN"
  set_env_kv "$RELEASE_PC_DIR/.env" "NUXT_PUBLIC_DEFAULT_COUNTRY_CODE" "zh-CN"
  set_env_kv "$RELEASE_PC_DIR/.env" "NUXT_PUBLIC_API_BASE" "/api/v1/h5app"
}

move_dist_to() {
  local project_dir=$1 dest_name=$2
  rm -rf "$project_dir/$dest_name"
  if [ ! -d "$project_dir/dist" ]; then
    release_log_error "expected $project_dir/dist after build"
    return 1
  fi
  mv "$project_dir/dist" "$project_dir/$dest_name"
}

find_bootstrap_jar() {
  local search_dir=$1
  local jar=""
  jar=$(find "$search_dir" -maxdepth 1 -name 'ecshopx-bootstrap-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null | head -1)
  if [ -z "$jar" ] || [ ! -f "$jar" ]; then
    release_log_error "ecshopx-bootstrap jar not found under $search_dir"
    return 1
  fi
  printf '%s' "$jar"
}

release_build_jar_host() {
  release_log_info "build jar (host mvnw: ecshopx-bootstrap)"
  (
    cd "$RELEASE_ECSHOPX_ROOT"
    ./mvnw -B -pl ecshopx-bootstrap -am package -DskipTests
  )
}

release_build_jar_docker() {
  local tmpdir jar_src
  release_log_info "build jar via docker (docker/Dockerfile.app builder stage)"
  release_require_cmds docker || return 1
  docker build -f "$RELEASE_ECSHOPX_ROOT/docker/Dockerfile.app" --target builder -t ecshopx-pack-builder "$RELEASE_ECSHOPX_ROOT"
  tmpdir=$(mktemp -d)
  local cid
  cid=$(docker create ecshopx-pack-builder)
  docker cp "$cid:/build/ecshopx-bootstrap/target/." "$tmpdir/"
  docker rm "$cid" >/dev/null
  jar_src=$(find_bootstrap_jar "$tmpdir") || { rm -rf "$tmpdir"; return 1; }
  release_stage_bootstrap_jar "$jar_src" || { rm -rf "$tmpdir"; return 1; }
  rm -rf "$tmpdir"
}

release_stage_bootstrap_jar() {
  local jar_src=$1
  local app_dir="$RELEASE_LITE_DIR/app"
  mkdir -p "$app_dir"
  if [ ! -f "$jar_src" ] || [ ! -s "$jar_src" ]; then
    release_log_error "bootstrap jar source missing or empty: $jar_src"
    return 1
  fi
  cp -f "$jar_src" "$app_dir/ecshopx-bootstrap.jar"
  cp -f "$jar_src" "$app_dir/app.jar"
  release_log_success "staged docker-lite/app/ecshopx-bootstrap.jar (+ app.jar for compose)"
}

release_build_jar() {
  local jar_src
  mkdir -p "$RELEASE_LITE_DIR/app"

  if [ -x "$RELEASE_ECSHOPX_ROOT/mvnw" ] && command -v java >/dev/null 2>&1; then
    release_build_jar_host || return 1
    jar_src=$(find_bootstrap_jar "$RELEASE_ECSHOPX_ROOT/ecshopx-bootstrap/target") || return 1
    release_stage_bootstrap_jar "$jar_src" || return 1
    return 0
  fi

  if command -v docker >/dev/null 2>&1; then
    release_log_info "host java/mvnw unavailable; falling back to docker build"
    release_build_jar_docker || return 1
    return 0
  fi

  release_log_error "cannot build jar: need ./mvnw + java on PATH, or docker for docker/Dockerfile.app builder stage"
  return 1
}

main() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --with-images)
        WITH_IMAGES=true
        shift
        ;;
      -h|--help)
        cat <<'EOF'
Usage: ./pack.sh [--with-images]

  --with-images   Download image tars from docker-lite/images.env URLs
                  into docker-lite/images/ and include them in the archive
EOF
        exit 0
        ;;
      *)
        release_log_error "unknown argument: $1"
        exit 1
        ;;
    esac
  done

  release_require_cmds node npm tar || exit 1
  release_load_nvm || exit 1

  for d in "$RELEASE_ADMIN_DIR" "$RELEASE_MOBILE_DIR" "$RELEASE_PC_DIR"; do
    if [ ! -d "$d" ]; then
      release_log_error "missing project dir: $d"
      exit 1
    fi
  done

  if [ ! -f "$RELEASE_LITE_DIR/docker-compose.yml" ]; then
    release_log_error "missing docker-lite/docker-compose.yml"
    exit 1
  fi

  local version
  version=$(release_read_product_version "$RELEASE_ECSHOPX_ROOT/pom.xml")
  release_log_info "packing version $version"

  prepare_release_envs

  if [ "$WITH_IMAGES" = true ]; then
    release_log_info "prefetch runtime image tars (--with-images)"
    release_prefetch_image_tars || exit 1
  fi

  release_build_jar || exit 1

  release_log_info "build admin b2c/bbc (Node ${RELEASE_NODE})"
  (
    release_nvm_use "$RELEASE_NODE" || exit 1
    cd "$RELEASE_ADMIN_DIR"
    npm install --legacy-peer-deps
    npm run build:b2c
    move_dist_to "$RELEASE_ADMIN_DIR" dist-b2c
    npm run build:bbc
    move_dist_to "$RELEASE_ADMIN_DIR" dist-bbc
  )

  release_log_info "build mobile b2c/bbc (Node ${RELEASE_NODE}, APP_IMAGE_CDN=${APP_IMAGE_CDN})"
  (
    release_nvm_use "$RELEASE_NODE" || exit 1
    cd "$RELEASE_MOBILE_DIR"
    npm install --legacy-peer-deps
    APP_PLATFORM=standard APP_IMAGE_CDN="$APP_IMAGE_CDN" npm run build:h5
    move_dist_to "$RELEASE_MOBILE_DIR" dist-b2c
    APP_PLATFORM=platform APP_IMAGE_CDN="$APP_IMAGE_CDN" npm run build:h5
    move_dist_to "$RELEASE_MOBILE_DIR" dist-bbc
  )

  release_log_info "build PC (Node ${RELEASE_NODE}, pnpm ${RELEASE_PNPM_VERSION})"
  (
    release_nvm_use "$RELEASE_NODE" || exit 1
    release_ensure_pnpm "$RELEASE_PNPM_VERSION" || exit 1
    cd "$RELEASE_PC_DIR"
    export NUXT_TELEMETRY_DISABLED=1
    export CI=true
    pnpm install --config.dangerouslyAllowAllBuilds=true
    pnpm build
  )

  release_validate_prebuilt_artifacts || exit 1

  local out="$RELEASE_ECSHOPX_ROOT"
  release_build_tarball "$version" "$out"
  release_log_success "done: $out/ecshopx-java-${version}.tar.gz"
}

main "$@"
