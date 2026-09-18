#!/usr/bin/env bash
# Shared helpers for pack.sh / deploy.sh (sourced, not executed)

release_log_info() { printf '[INFO] %s\n' "$*"; }
release_log_success() { printf '[SUCCESS] %s\n' "$*"; }
release_log_warning() { printf '[WARNING] %s\n' "$*"; }
release_log_error() { printf '[ERROR] %s\n' "$*" >&2; }

release_init_paths() {
  local caller=$1
  local caller_dir
  caller_dir="$(cd "$(dirname "$caller")" && pwd)"
  RELEASE_CALLER_DIR="$caller_dir"
  if [ "$(basename "$caller_dir")" = "lib" ]; then
    RELEASE_LITE_DIR="$(cd "$caller_dir/.." && pwd)"
  else
    RELEASE_LITE_DIR="$caller_dir"
  fi
  RELEASE_ECSHOPX_ROOT="$(cd "$RELEASE_LITE_DIR/.." && pwd)"
  RELEASE_PARENT_DIR="$(cd "$RELEASE_ECSHOPX_ROOT/.." && pwd)"
  RELEASE_ADMIN_DIR="$RELEASE_PARENT_DIR/ECShopX-Java_Admin"
  RELEASE_MOBILE_DIR="$RELEASE_PARENT_DIR/ECShopX-Java_Mobile"
  RELEASE_PC_DIR="$RELEASE_PARENT_DIR/ECShopX-Java_Web"
}

release_read_product_version() {
  local pom_xml=$1
  local ver=""
  if [ ! -f "$pom_xml" ]; then
    release_log_error "pom.xml not found: $pom_xml"
    return 1
  fi
  # sed only — lite deploy must not invoke host mvn/java (may be broken/unrelated on PATH).
  ver=$(sed -n '/<artifactId>ecshopx<\/artifactId>/,/<packaging>/{/<version>/{s/.*<version>\([^<]*\)<\/version>.*/\1/p;q}}' "$pom_xml")
  if [ -z "$ver" ]; then
    release_log_error "cannot parse version from $pom_xml"
    return 1
  fi
  printf '%s' "$ver"
}

release_mode_to_product_model() {
  case "$1" in
    b2c) printf 'standard' ;;
    bbc) printf 'platform' ;;
    *) release_log_error "invalid mode: $1 (expected b2c|bbc)"; return 1 ;;
  esac
}

release_mode_to_dist_suffix() {
  case "$1" in
    b2c|bbc) printf '%s' "$1" ;;
    *) release_log_error "invalid mode: $1 (expected b2c|bbc)"; return 1 ;;
  esac
}

release_require_cmds() {
  local missing=0 c
  for c in "$@"; do
    if ! command -v "$c" >/dev/null 2>&1; then
      release_log_error "missing required command: $c"
      missing=1
    fi
  done
  [ "$missing" -eq 0 ]
}

# Load nvm into the current shell (required before release_nvm_use).
release_load_nvm() {
  export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
  if [ ! -s "$NVM_DIR/nvm.sh" ]; then
    release_log_error "nvm not found at ${NVM_DIR}/nvm.sh (needed to switch Node versions for pack.sh)"
    return 1
  fi
  # shellcheck disable=SC1090
  . "$NVM_DIR/nvm.sh"
}

# Install (if missing) and activate a Node version via nvm. Example: release_nvm_use 20.19
release_nvm_use() {
  local version=${1:-}
  if [ -z "$version" ]; then
    release_log_error "release_nvm_use requires a Node version (e.g. 20.19)"
    return 1
  fi
  release_load_nvm || return 1
  release_log_info "nvm: switching to Node ${version}"
  nvm install "$version"
  nvm use "$version"
  hash -r 2>/dev/null || true
  release_log_info "active node $(node -v); npm $(npm -v)"
}

# Ensure a pnpm version that works with the currently active Node (avoid global pnpm that requires Node 22+).
release_ensure_pnpm() {
  local want=${1:-10.13.0}
  if command -v corepack >/dev/null 2>&1; then
    corepack enable >/dev/null 2>&1 || true
    corepack prepare "pnpm@${want}" --activate
  else
    npm install -g "pnpm@${want}"
  fi
  hash -r 2>/dev/null || true
  if ! command -v pnpm >/dev/null 2>&1; then
    release_log_error "pnpm@${want} is not available after install under $(node -v)"
    return 1
  fi
  release_log_info "active pnpm $(pnpm -v) (requested ${want})"
}
