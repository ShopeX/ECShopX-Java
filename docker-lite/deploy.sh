#!/usr/bin/env bash
set -euo pipefail

# Intentionally does NOT run Maven, npm/pnpm, or frontend builds
# Runtime: docker-lite/docker-compose.yml (jar mount + prebuilt frontend dist).

SCRIPT_PATH="${BASH_SOURCE[0]}"
LITE_DIR="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"
# shellcheck source=lib/common.sh
source "$LITE_DIR/lib/common.sh"
# shellcheck source=lib/artifacts.sh
source "$LITE_DIR/lib/artifacts.sh"
# shellcheck source=lib/images.sh
source "$LITE_DIR/lib/images.sh"
# shellcheck source=../docker/install-secrets.sh
source "$LITE_DIR/../docker/install-secrets.sh"

release_init_paths "$SCRIPT_PATH"

COMPOSE_FILE="$RELEASE_LITE_DIR/docker-compose.yml"
COMPOSE_ENV_FILE="$RELEASE_LITE_DIR/.env"
APP_PROPERTIES="$LITE_DIR/../ecshopx-bootstrap/src/main/resources/application.properties"
DOCKER_COMPOSE_CMD=""
START_TIME=0

MODE=""
SKIP_DEMO=false
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
JWT_SECRET="${JWT_SECRET:-}"
JWT_GENERATED=false
ADMIN_URL_OVERRIDE=""
H5_URL_OVERRIDE=""
PC_URL_OVERRIDE=""
HTTP_PORT_FROM_CLI=false
ADMIN_HOST_FROM_CLI=false
H5_HOST_FROM_CLI=false
PC_HOST_FROM_CLI=false

HTTP_HOST_PORT="80"
XXL_HOST_PORT="8080"
ADMIN_HOST="admin.ecshopx.test"
H5_HOST="h5.ecshopx.test"
PC_HOST="www.ecshopx.test"
ADMIN_URL="http://admin.ecshopx.test"
API_BASE_URL="http://admin.ecshopx.test/api/v1/"
H5_URL="http://h5.ecshopx.test"
PC_URL="http://www.ecshopx.test"
NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS="http://admin.ecshopx.test"
PC_API_URL="http://admin.ecshopx.test/api/v1/h5app"

trim_trailing_slashes() {
  local value=$1
  while [ "${value%/}" != "$value" ]; do
    value=${value%/}
  done
  printf '%s' "$value"
}

validate_public_url() {
  local name=$1 value=$2
  if [ -z "$value" ]; then
    return 0
  fi
  case "$value" in
    http://*|https://*) return 0 ;;
    *)
      release_log_error "$name 必须以 http:// 或 https:// 开头: $value"
      exit 1
      ;;
  esac
}

validate_port() {
  local name=$1 port=$2
  case "$port" in
    ''|*[!0-9]*)
      release_log_error "$name 端口无效: $port"
      exit 1
      ;;
  esac
  if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
    release_log_error "$name 端口超出范围: $port"
    exit 1
  fi
}

validate_hostname() {
  local name=$1 host=$2
  [ -n "$host" ] || { release_log_error "$name 主机名不能为空"; exit 1; }
}

extract_hostname_from_url() {
  local url=$1
  local without_scheme=${url#*://}
  local authority=${without_scheme%%/*}
  local host=${authority%%:*}
  [ -n "$host" ] || { release_log_error "无法从 URL 解析主机名: $url"; exit 1; }
  printf '%s' "$host"
}

url_has_explicit_port() {
  local url=$1
  local without_scheme=${url#*://}
  local authority=${without_scheme%%/*}
  case "$authority" in
    *:*) return 0 ;;
    *)   return 1 ;;
  esac
}

origin_from_url() {
  local url=$1
  local scheme=${url%%://*}
  local without_scheme=${url#*://}
  local authority=${without_scheme%%/*}
  printf '%s' "${scheme}://${authority}"
}

build_http_url() {
  local host=$1
  if [ "$HTTP_HOST_PORT" = "80" ]; then
    printf '%s' "http://${host}"
  else
    printf '%s' "http://${host}:${HTTP_HOST_PORT}"
  fi
}

refresh_urls_from_hosts() {
  ADMIN_URL=$(build_http_url "$ADMIN_HOST")
  H5_URL=$(build_http_url "$H5_HOST")
  PC_URL=$(build_http_url "$PC_HOST")
  derive_api_base_from_admin_url
  NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS=$(origin_from_url "$ADMIN_URL")
}

print_hosts_tip() {
  release_log_info "请在本机 /etc/hosts（或 Windows hosts）中添加："
  release_log_info "  127.0.0.1  ${ADMIN_HOST} ${H5_HOST} ${PC_HOST}"
}

upsert_env_file() {
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
    echo "${key}=${value}" >>"$file"
  fi
}

usage() {
  cat <<'EOF'
Usage: ./deploy.sh --mode b2c|bbc [options]

Loads runtime images (pull), uses packaged ecshopx-bootstrap.jar,
activates frontend dist, then starts the lite stack via docker-lite/docker-compose.yml.

Options:
  --mode b2c|bbc       Business mode (required unless prompted)
  --http-port N        Host HTTP port (default 80)
  --xxl-port N         Host XXL-Job port (default 8080)
  --admin-host HOST    Admin hostname (default admin.ecshopx.test)
  --h5-host HOST       H5 hostname (default h5.ecshopx.test)
  --pc-host HOST       PC hostname (default www.ecshopx.test)
  --admin-url URL      Admin URL override
  --h5-url URL         H5 URL override
  --pc-url URL         PC URL override
  --admin-password P   Admin login password (required without tty)
  --skip-demo          Skip demo SQL import (reserved; schema from ecshopx.sql)
  -h, --help           Show help

Environment:
  ADMIN_PASSWORD       Same as --admin-password
  JWT_SECRET           Reuse existing JWT secret if set

Configure DOCKER_IMAGES_BUNDLE_URL / image tags in docker-lite/images.env
EOF
}

# Use packaged jar only (no remote download).
ensure_app_jar() {
  local dest="$RELEASE_LITE_DIR/app/app.jar"
  local bundled="$RELEASE_LITE_DIR/app/ecshopx-bootstrap.jar"
  mkdir -p "$(dirname "$dest")"

  if [ ! -s "$bundled" ]; then
    release_log_error "缺少 docker-lite/app/ecshopx-bootstrap.jar，请使用完整发行包。"
    return 1
  fi
  cp -f "$bundled" "$dest"
  release_log_success "使用发行包内 jar: $bundled"
}

extract_host_port_from_url() {
  local url=$1 default_port=$2
  local without_scheme=${url#*://}
  local authority=${without_scheme%%/*}
  local port
  case "$authority" in
    *:*) port=${authority##*:} ;;
    *)   port=$default_port ;;
  esac
  case "$port" in
    ''|*[!0-9]*)
      release_log_error "访问地址端口无效: $url"
      exit 1
      ;;
  esac
  if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
    release_log_error "访问地址端口超出范围: $url"
    exit 1
  fi
  printf '%s' "$port"
}

configure_docker_publish_env() {
  export HTTP_HOST_PORT XXL_HOST_PORT
  export ADMIN_HOST H5_HOST PC_HOST
  export NUXT_PUBLIC_API_BASE="$PC_API_URL"
  export NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS
  export JWT_SECRET
  export PRODUCT_MODEL
  release_log_info "Docker 发布环境："
  release_log_info "  HTTP: ${HTTP_HOST_PORT}->80  XXL: ${XXL_HOST_PORT}->8080"
  release_log_info "  Hosts: ADMIN=${ADMIN_HOST} H5=${H5_HOST} PC=${PC_HOST}"
  [ -n "${PRODUCT_MODEL:-}" ] && release_log_info "  PRODUCT_MODEL=${PRODUCT_MODEL}"
}

configure_install_secrets() {
  release_log_info "配置安装密钥（后台密码 / JWT）..."
  install_prompt_admin_password || exit 1
  # Lite pack may not ship application.properties; still persist to compose .env.
  local props=""
  [ -f "$APP_PROPERTIES" ] && props="$APP_PROPERTIES"
  install_ensure_jwt_secret "$props" "$COMPOSE_ENV_FILE" || exit 1
  JWT_GENERATED="${INSTALL_JWT_GENERATED:-false}"
  if [ "$JWT_GENERATED" = true ]; then
    release_log_success "JWT_SECRET 未配置，已自动生成并写入 docker-lite/.env"
  else
    release_log_info "使用已有 JWT_SECRET"
  fi
  export JWT_SECRET
  configure_docker_publish_env
}

derive_api_base_from_admin_url() {
  local normalized_api_url
  normalized_api_url="$(trim_trailing_slashes "$ADMIN_URL")/api/v1"
  API_BASE_URL="${normalized_api_url}/"
  PC_API_URL="${normalized_api_url}/h5app"
}

should_prompt_host_config() {
  [ "$HTTP_PORT_FROM_CLI" = true ] && return 1
  [ "$ADMIN_HOST_FROM_CLI" = true ] && return 1
  [ "$H5_HOST_FROM_CLI" = true ] && return 1
  [ "$PC_HOST_FROM_CLI" = true ] && return 1
  [ -n "$ADMIN_URL_OVERRIDE" ] && return 1
  [ -n "$H5_URL_OVERRIDE" ] && return 1
  [ -n "$PC_URL_OVERRIDE" ] && return 1
  return 0
}

configure_public_urls() {
  local http_port_input="" admin_host_input="" h5_host_input="" pc_host_input=""

  if should_prompt_host_config; then
    if [ -e /dev/tty ]; then
      echo ""
      release_log_info "访问域名/端口配置（直接回车使用默认值）："
      read -r -p "请输入 HTTP 端口 (默认: $HTTP_HOST_PORT): " http_port_input < /dev/tty || true
      read -r -p "请输入管理后台域名 (默认: $ADMIN_HOST): " admin_host_input < /dev/tty || true
      read -r -p "请输入 H5 域名 (默认: $H5_HOST): " h5_host_input < /dev/tty || true
      read -r -p "请输入 PC 域名 (默认: $PC_HOST): " pc_host_input < /dev/tty || true
      [ -n "${http_port_input:-}" ] && HTTP_HOST_PORT="$http_port_input"
      [ -n "${admin_host_input:-}" ] && ADMIN_HOST="$admin_host_input"
      [ -n "${h5_host_input:-}" ] && H5_HOST="$h5_host_input"
      [ -n "${pc_host_input:-}" ] && PC_HOST="$pc_host_input"
    else
      release_log_info "无交互终端，使用默认访问域名/端口"
    fi
  fi

  validate_port "HTTP" "$HTTP_HOST_PORT"
  validate_port "XXL" "$XXL_HOST_PORT"
  validate_hostname "ADMIN_HOST" "$ADMIN_HOST"
  validate_hostname "H5_HOST" "$H5_HOST"
  validate_hostname "PC_HOST" "$PC_HOST"

  refresh_urls_from_hosts

  ADMIN_URL_OVERRIDE=$(trim_trailing_slashes "$ADMIN_URL_OVERRIDE")
  H5_URL_OVERRIDE=$(trim_trailing_slashes "$H5_URL_OVERRIDE")
  PC_URL_OVERRIDE=$(trim_trailing_slashes "$PC_URL_OVERRIDE")

  validate_public_url "--admin-url" "$ADMIN_URL_OVERRIDE"
  validate_public_url "--h5-url" "$H5_URL_OVERRIDE"
  validate_public_url "--pc-url" "$PC_URL_OVERRIDE"

  if [ -n "$ADMIN_URL_OVERRIDE" ]; then
    ADMIN_URL="$ADMIN_URL_OVERRIDE"
    ADMIN_HOST=$(extract_hostname_from_url "$ADMIN_URL")
    if url_has_explicit_port "$ADMIN_URL"; then
      HTTP_HOST_PORT=$(extract_host_port_from_url "$ADMIN_URL" "$HTTP_HOST_PORT")
    fi
  fi
  if [ -n "$H5_URL_OVERRIDE" ]; then
    H5_URL="$H5_URL_OVERRIDE"
    H5_HOST=$(extract_hostname_from_url "$H5_URL")
  fi
  if [ -n "$PC_URL_OVERRIDE" ]; then
    PC_URL="$PC_URL_OVERRIDE"
    PC_HOST=$(extract_hostname_from_url "$PC_URL")
  fi

  [ -z "$ADMIN_URL_OVERRIDE" ] && ADMIN_URL=$(build_http_url "$ADMIN_HOST")
  [ -z "$H5_URL_OVERRIDE" ] && H5_URL=$(build_http_url "$H5_HOST")
  [ -z "$PC_URL_OVERRIDE" ] && PC_URL=$(build_http_url "$PC_HOST")

  derive_api_base_from_admin_url
  NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS=$(origin_from_url "$ADMIN_URL")
  configure_docker_publish_env
  print_hosts_tip
}

sync_nuxt_public_env() {
  local env_file
  env_file=$(release_images_env_file)
  if [ ! -f "$env_file" ]; then
    return 0
  fi
  upsert_env_file "$env_file" "NUXT_PUBLIC_API_BASE" "/api/v1/h5app"
  if [ -n "$ADMIN_URL" ]; then
    upsert_env_file "$env_file" "NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS" "$ADMIN_URL"
  fi
}

detect_docker_compose() {
  if docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD="docker compose"
  elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD="docker-compose"
  else
    release_log_error "未找到 docker compose 或 docker-compose"
    exit 1
  fi
}

compose() {
  local env_args=(--env-file "$(release_images_env_file)")
  if [ -f "$COMPOSE_ENV_FILE" ]; then
    env_args+=(--env-file "$COMPOSE_ENV_FILE")
  fi
  $DOCKER_COMPOSE_CMD \
    "${env_args[@]}" \
    -f "$COMPOSE_FILE" \
    --project-directory "$RELEASE_LITE_DIR" \
    "$@"
}

deploy_ensure_bind_mount_traverse() {
  local parent="$RELEASE_PARENT_DIR"
  local d

  if [ ! -d "$parent" ]; then
    release_log_error "挂载根目录不存在: $parent"
    return 1
  fi

  release_log_info "确保挂载根目录可被容器内非 root 用户遍历: $parent"
  if ! chmod a+x "$parent" 2>/dev/null; then
    release_log_warning "无法 chmod a+x $parent；若管理后台/H5 出现 404，请手动执行: chmod a+x $parent"
  else
    release_log_success "已设置挂载根目录遍历权限 (a+x)"
  fi

  for d in "$RELEASE_ECSHOPX_ROOT" "$RELEASE_ADMIN_DIR" "$RELEASE_MOBILE_DIR" "$RELEASE_PC_DIR"; do
    if [ -d "$d" ]; then
      chmod a+rx "$d" 2>/dev/null || true
    fi
  done

  if [ -d "$RELEASE_ADMIN_DIR/dist" ]; then
    chmod -R a+rX "$RELEASE_ADMIN_DIR/dist" 2>/dev/null || true
  fi
  if [ -d "$RELEASE_MOBILE_DIR/dist" ]; then
    chmod -R a+rX "$RELEASE_MOBILE_DIR/dist" 2>/dev/null || true
  fi
  if [ -d "$RELEASE_PC_DIR/.output" ]; then
    chmod -R a+rX "$RELEASE_PC_DIR/.output" 2>/dev/null || true
  fi
  return 0
}

compose_up() {
  release_log_info "docker compose up -d ..."
  if ! compose up -d; then
    release_log_error "compose up 失败"
    compose logs --tail=80 || true
    return 1
  fi
  release_log_success "compose 服务已启动"

  release_log_info "更新后台管理员登录密码..."
  if install_apply_admin_password "ecshopx-mysql"; then
    release_log_success "后台管理员密码已更新（账号: admin）"
  else
    release_log_error "后台管理员密码更新失败"
    return 1
  fi

  release_log_info "同步业务模式到 companys.menu_type..."
  if install_apply_company_menu_type "ecshopx-mysql" "$MODE"; then
    release_log_success "companys.menu_type 已按模式 $MODE 更新"
  else
    release_log_error "更新 companys.menu_type 失败"
    return 1
  fi
}

# Wait until Java HTTP answers (Flyway migrate runs during Spring Boot startup).
wait_for_java_ready() {
  local url="${1:-http://127.0.0.1:18080/}"
  local max_attempts="${2:-120}"
  local attempt=1
  local code=""

  release_log_info "等待 Java 就绪（含 Flyway 启动迁移）..."
  while [ "$attempt" -le "$max_attempts" ]; do
    # Do not append a fallback on curl failure — curl still prints 000 on connect errors.
    code=$(curl -s -o /dev/null -w '%{http_code}' -m 3 "$url" 2>/dev/null || true)
    case "$code" in
      ''|000) ;;
      *)
        release_log_success "Java 已就绪 (HTTP $code)；Flyway 已随启动执行 (spring.flyway.enabled=true)"
        return 0
        ;;
    esac
    sleep 2
    attempt=$((attempt + 1))
  done

  release_log_error "等待 Java 就绪超时（${max_attempts} 次）。请检查: docker compose -f docker-lite/docker-compose.yml logs ecshopx-app"
  return 1
}

prompt_mode_if_missing() {
  if [ -n "$MODE" ]; then
    return 0
  fi
  echo ""
  release_log_info "请选择业务模式："
  release_log_info "  1) b2c (standard)"
  release_log_info "  2) bbc (platform)"
  echo ""
  local choice=""
  if [ -r /dev/tty ]; then
    read -r -p "请输入选项 (1-2，默认: 1): " choice </dev/tty
  elif [ -t 0 ]; then
    read -r -p "请输入选项 (1-2，默认: 1): " choice
  else
    release_log_error "无交互终端且未指定 --mode；请使用: ./deploy.sh --mode b2c|bbc"
    exit 1
  fi
  choice=${choice:-1}
  case "$choice" in
    1|b2c|B2C) MODE="b2c" ;;
    2|bbc|BBC) MODE="bbc" ;;
    *)
      release_log_error "无效选项: $choice"
      exit 1
      ;;
  esac
}

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --mode) MODE="${2:-}"; shift 2 ;;
      --http-port)
        [ -z "${2:-}" ] && { release_log_error "--http-port 需要端口号"; exit 1; }
        HTTP_HOST_PORT="$2"; HTTP_PORT_FROM_CLI=true; shift 2 ;;
      --xxl-port)
        [ -z "${2:-}" ] && { release_log_error "--xxl-port 需要端口号"; exit 1; }
        XXL_HOST_PORT="$2"; shift 2 ;;
      --admin-host)
        [ -z "${2:-}" ] && { release_log_error "--admin-host 需要主机名"; exit 1; }
        ADMIN_HOST="$2"; ADMIN_HOST_FROM_CLI=true; shift 2 ;;
      --h5-host)
        [ -z "${2:-}" ] && { release_log_error "--h5-host 需要主机名"; exit 1; }
        H5_HOST="$2"; H5_HOST_FROM_CLI=true; shift 2 ;;
      --pc-host)
        [ -z "${2:-}" ] && { release_log_error "--pc-host 需要主机名"; exit 1; }
        PC_HOST="$2"; PC_HOST_FROM_CLI=true; shift 2 ;;
      --admin-url) ADMIN_URL_OVERRIDE="${2:-}"; shift 2 ;;
      --h5-url) H5_URL_OVERRIDE="${2:-}"; shift 2 ;;
      --pc-url) PC_URL_OVERRIDE="${2:-}"; shift 2 ;;
      --admin-password)
        [ -z "${2:-}" ] && { release_log_error "--admin-password 需要密码"; exit 1; }
        ADMIN_PASSWORD="$2"; shift 2 ;;
      --skip-demo) SKIP_DEMO=true; shift ;;
      -h|--help) usage; exit 0 ;;
      *)
        release_log_error "未知参数: $1"
        usage >&2
        exit 1
        ;;
    esac
  done
}

main() {
  START_TIME=$(date +%s)
  parse_args "$@"
  prompt_mode_if_missing

  case "$MODE" in
    b2c|bbc) ;;
    *)
      release_log_error "无效 --mode: $MODE (expected b2c|bbc)"
      exit 1
      ;;
  esac

  install_persist_product_model "$COMPOSE_ENV_FILE" "$MODE" || exit 1
  release_log_info "业务模式 $MODE → PRODUCT_MODEL=${PRODUCT_MODEL}"

  release_require_cmds docker || exit 1
  detect_docker_compose

  if [ ! -f "$COMPOSE_FILE" ]; then
    release_log_error "未找到 $COMPOSE_FILE"
    exit 1
  fi

  release_validate_prebuilt_artifacts || {
    release_log_error "预置产物不完整，请使用完整离线发行包或先执行 pack.sh"
    exit 1
  }

  configure_public_urls
  configure_install_secrets
  sync_nuxt_public_env
  release_activate_frontend_dist "$MODE" || exit 1

  release_log_info "从 CDN 下载并导入 Docker 镜像..."
  release_ensure_runtime_images || exit 1

  ensure_app_jar || exit 1

  deploy_ensure_bind_mount_traverse || exit 1
  compose_up || exit 1
  wait_for_java_ready "http://127.0.0.1:18080/" || exit 1

  if [ "$SKIP_DEMO" = true ]; then
    release_log_info "已跳过 Demo 数据导入 (--skip-demo)"
  fi

  show_success_info
}

show_success_info() {
  local end duration minutes seconds
  end=$(date +%s)
  duration=$((end - START_TIME))
  minutes=$((duration / 60))
  seconds=$((duration % 60))

  echo ""
  release_log_success "=========================================="
  release_log_success "安装完成"
  release_log_success "=========================================="
  echo ""
  release_log_info "服务地址："
  release_log_info "  管理后台: $ADMIN_URL （账号 admin，密码为安装时设置）"
  if [ "$JWT_GENERATED" = true ]; then
    release_log_info "  JWT_SECRET: 已自动生成（见 docker-lite/.env；容器由环境变量 / APP_ARGS 注入）"
  fi
  release_log_info "  H5前端:   $H5_URL"
  release_log_info "  PC前端:   $PC_URL"
  release_log_info "  API 接口: $API_BASE_URL"
  release_log_info "  XXL-Job:  http://localhost:${XXL_HOST_PORT}/xxl-job-admin"
  release_log_info "  Java API: http://localhost:18080 (直连调试)"
  release_log_info "  数据库:   Flyway 启动迁移已开启 (spring.flyway.enabled=true)"
  echo ""
  print_hosts_tip
  echo ""
  release_log_info "常用命令："
  release_log_info "  查看日志: $DOCKER_COMPOSE_CMD -f $COMPOSE_FILE --project-directory $RELEASE_LITE_DIR logs -f"
  release_log_info "  停止服务: $DOCKER_COMPOSE_CMD -f $COMPOSE_FILE --project-directory $RELEASE_LITE_DIR down"
  release_log_info "执行时间: ${minutes}分${seconds}秒"
  release_log_success "=========================================="
}

main "$@"
