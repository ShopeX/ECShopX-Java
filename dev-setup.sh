#!/usr/bin/env bash
# ECShopX-Java Java 全量开发环境设置（Docker Compose）
# 编排：ECShopX-Java + ECShopX-Java_Admin + ECShopX-Java_Mobile + ECShopX-Java_Web

set -euo pipefail

# ---------------------------------------------------------------------------
# Paths & defaults
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
PARENT_DIR="$(cd "$PROJECT_ROOT/.." && pwd)"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker-compose.dev.yml"
COMPOSE_ENV_FILE="$PROJECT_ROOT/.env"
APP_PROPERTIES="$PROJECT_ROOT/ecshopx-bootstrap/src/main/resources/application.properties"
RUNTIME_BASE_IMAGE="${RUNTIME_BASE_IMAGE:-registry.cn-hangzhou.aliyuncs.com/shopex_company/ecshopx-java:17-node20-openresty}"
# Frontend builds reuse the same runtime base image (Node 20 already included).
NODE_BUILD_IMAGE="${NODE_BUILD_IMAGE:-$RUNTIME_BASE_IMAGE}"

# shellcheck source=docker/install-secrets.sh
source "$PROJECT_ROOT/docker/install-secrets.sh"

PUBLIC_REPO_BASE_URL="${PUBLIC_REPO_BASE_URL:-https://gitee.com/ShopeX}"

ADMIN_DIR="$PARENT_DIR/ECShopX-Java_Admin"
VSHOP_DIR="$PARENT_DIR/ECShopX-Java_Mobile"
WEB_DIR="$PARENT_DIR/ECShopX-Java_Web"

ADMIN_REPO="$PUBLIC_REPO_BASE_URL/ECShopX-Java_Admin.git"
VSHOP_REPO="$PUBLIC_REPO_BASE_URL/ECShopX-Java_Mobile.git"
WEB_REPO="$PUBLIC_REPO_BASE_URL/ECShopX-Java_Web.git"

REBUILD=false
SKIP_ADMIN=false
SKIP_VSHOP=false
SKIP_PC=false
MODE=""                    # b2c | bbc (npm script suffix)
SELECTED_PLATFORM=""       # standard (b2c) | platform (bbc)

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
QIANKUN_ENTRY_URL="http://admin.ecshopx.test/newpc/"
MOBILE_API_URL="http://admin.ecshopx.test/api/v1/h5app/wxapp"
PC_API_URL="http://admin.ecshopx.test/api/v1/h5app"
NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS="http://admin.ecshopx.test"

INSTALLED_ADMIN=false
INSTALLED_VSHOP=false
INSTALLED_PC=false
FRONTEND_ENV_CHANGED=false
DOCKER_COMPOSE_CMD=""
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
JWT_SECRET="${JWT_SECRET:-}"
JWT_GENERATED=false
START_TIME=$(date +%s)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()    { printf "%b\n" "${BLUE}[INFO]${NC} $1"; }
log_success() { printf "%b\n" "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { printf "%b\n" "${YELLOW}[WARNING]${NC} $1"; }
log_error()   { printf "%b\n" "${RED}[ERROR]${NC} $1"; }
log_step()    { printf "%b\n" "${CYAN}[STEP]${NC} $1"; }

trim_trailing_slashes() {
    local value=$1
    while [ "${value%/}" != "$value" ]; do value=${value%/}; done
    echo "$value"
}

validate_public_url() {
    local name=$1 value=$2
    [ -z "$value" ] && return 0
    case "$value" in
        http://*|https://*) return 0 ;;
        *) log_error "$name 必须以 http:// 或 https:// 开头: $value"; exit 1 ;;
    esac
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
        ''|*[!0-9]*) log_error "访问地址端口无效: $url"; exit 1 ;;
    esac
    if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
        log_error "访问地址端口超出范围: $url"; exit 1
    fi
    echo "$port"
}

extract_hostname_from_url() {
    local url=$1
    local without_scheme=${url#*://}
    local authority=${without_scheme%%/*}
    local host=${authority%%:*}
    [ -n "$host" ] || { log_error "无法从 URL 解析主机名: $url"; exit 1; }
    echo "$host"
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

validate_port() {
    local name=$1 port=$2
    case "$port" in
        ''|*[!0-9]*) log_error "$name 端口无效: $port"; exit 1 ;;
    esac
    if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
        log_error "$name 端口超出范围: $port"; exit 1
    fi
}

validate_hostname() {
    local name=$1 host=$2
    [ -n "$host" ] || { log_error "$name 主机名不能为空"; exit 1; }
}

origin_from_url() {
    local url=$1
    local scheme=${url%%://*}
    local without_scheme=${url#*://}
    local authority=${without_scheme%%/*}
    echo "${scheme}://${authority}"
}

build_http_url() {
    local host=$1
    if [ "$HTTP_HOST_PORT" = "80" ]; then
        echo "http://${host}"
    else
        echo "http://${host}:${HTTP_HOST_PORT}"
    fi
}

refresh_urls_from_hosts() {
    ADMIN_URL=$(build_http_url "$ADMIN_HOST")
    H5_URL=$(build_http_url "$H5_HOST")
    PC_URL=$(build_http_url "$PC_HOST")
    derive_api_base_from_admin_url
    QIANKUN_ENTRY_URL="${ADMIN_URL}/newpc/"
    NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS=$(origin_from_url "$ADMIN_URL")
}

print_hosts_tip() {
    log_info "请在本机 /etc/hosts（或 Windows hosts）中添加："
    log_info "  127.0.0.1  ${ADMIN_HOST} ${H5_HOST} ${PC_HOST}"
}

sed_inplace() {
    if [[ "${OSTYPE:-}" == darwin* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

set_env_kv() {
    local file=$1 key=$2 value=$3
    if grep -q "^${key}=" "$file" 2>/dev/null; then
        sed_inplace "s|^${key}=.*|${key}=${value}|" "$file"
    else
        echo "${key}=${value}" >> "$file"
    fi
}

derive_api_base_from_admin_url() {
    local normalized_api_url
    normalized_api_url="$(trim_trailing_slashes "$ADMIN_URL")/api/v1"
    API_BASE_URL="${normalized_api_url}/"
    MOBILE_API_URL="${normalized_api_url}/h5app/wxapp"
    PC_API_URL="${normalized_api_url}/h5app"
}

configure_docker_publish_env() {
    export HTTP_HOST_PORT XXL_HOST_PORT ADMIN_HOST H5_HOST PC_HOST
    export NUXT_PUBLIC_API_BASE="$PC_API_URL"
    export NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS
    export JWT_SECRET
    export RUNTIME_BASE_IMAGE
    log_info "Docker 发布环境："
    log_info "  HTTP: ${HTTP_HOST_PORT}->80  XXL: ${XXL_HOST_PORT}->8080"
    log_info "  Hosts: ADMIN=${ADMIN_HOST} H5=${H5_HOST} PC=${PC_HOST}"
}

ensure_runtime_base_image() {
    export RUNTIME_BASE_IMAGE
    if docker image inspect "$RUNTIME_BASE_IMAGE" >/dev/null 2>&1; then
        log_info "运行时基础镜像已存在: $RUNTIME_BASE_IMAGE"
        return 0
    fi

    log_step "拉取运行时基础镜像: $RUNTIME_BASE_IMAGE ..."
    if docker pull "$RUNTIME_BASE_IMAGE"; then
        log_success "已拉取: $RUNTIME_BASE_IMAGE"
        return 0
    fi

    log_step "拉取失败，本地构建 $RUNTIME_BASE_IMAGE ..."
    if ! bash "$PROJECT_ROOT/docker/build-runtime-base.sh" --tag "$RUNTIME_BASE_IMAGE"; then
        log_error "无法获取 $RUNTIME_BASE_IMAGE（pull/build 均失败）"
        return 1
    fi
    log_success "已就绪: $RUNTIME_BASE_IMAGE"
}

configure_install_secrets() {
    log_step "配置安装密钥（后台密码 / JWT）..."
    install_prompt_admin_password || exit 1
    install_ensure_jwt_secret "$APP_PROPERTIES" "$COMPOSE_ENV_FILE" || exit 1
    JWT_GENERATED="${INSTALL_JWT_GENERATED:-false}"
    if [ "$JWT_GENERATED" = true ]; then
        log_success "JWT_SECRET 未配置，已自动生成并写入 .env（Docker 通过环境变量 / APP_ARGS 注入）"
    else
        log_info "使用已有 JWT_SECRET"
    fi
    export JWT_SECRET
    configure_docker_publish_env
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
            log_info "访问域名/端口配置（直接回车使用默认值）："
            read -r -p "请输入 HTTP 端口 (默认: $HTTP_HOST_PORT): " http_port_input < /dev/tty || true
            read -r -p "请输入管理后台域名 (默认: $ADMIN_HOST): " admin_host_input < /dev/tty || true
            read -r -p "请输入 H5 域名 (默认: $H5_HOST): " h5_host_input < /dev/tty || true
            read -r -p "请输入 PC 域名 (默认: $PC_HOST): " pc_host_input < /dev/tty || true
            [ -n "${http_port_input:-}" ] && HTTP_HOST_PORT="$http_port_input"
            [ -n "${admin_host_input:-}" ] && ADMIN_HOST="$admin_host_input"
            [ -n "${h5_host_input:-}" ] && H5_HOST="$h5_host_input"
            [ -n "${pc_host_input:-}" ] && PC_HOST="$pc_host_input"
        else
            log_info "无交互终端，使用默认访问域名/端口"
        fi
    fi

    validate_port "HTTP" "$HTTP_HOST_PORT"
    validate_port "XXL" "$XXL_HOST_PORT"
    validate_hostname "ADMIN_HOST" "$ADMIN_HOST"
    validate_hostname "H5_HOST" "$H5_HOST"
    validate_hostname "PC_HOST" "$PC_HOST"

    # Host/port first, then full URL overrides sync hostname (+ admin port when present).
    refresh_urls_from_hosts

    ADMIN_URL_OVERRIDE=$(trim_trailing_slashes "${ADMIN_URL_OVERRIDE:-}")
    H5_URL_OVERRIDE=$(trim_trailing_slashes "${H5_URL_OVERRIDE:-}")
    PC_URL_OVERRIDE=$(trim_trailing_slashes "${PC_URL_OVERRIDE:-}")

    validate_public_url "--admin-url" "$ADMIN_URL_OVERRIDE"
    validate_public_url "--h5-url" "$H5_URL_OVERRIDE"
    validate_public_url "--pc-url" "$PC_URL_OVERRIDE"

    if [ -n "$ADMIN_URL_OVERRIDE" ]; then
        ADMIN_URL="$ADMIN_URL_OVERRIDE"
        ADMIN_HOST=$(extract_hostname_from_url "$ADMIN_URL")
        if url_has_explicit_port "$ADMIN_URL"; then
            HTTP_HOST_PORT=$(extract_host_port_from_url "$ADMIN_URL" "$HTTP_HOST_PORT")
        fi
        QIANKUN_ENTRY_URL="${ADMIN_URL}/newpc/"
    fi
    if [ -n "$H5_URL_OVERRIDE" ]; then
        H5_URL="$H5_URL_OVERRIDE"
        H5_HOST=$(extract_hostname_from_url "$H5_URL")
    fi
    if [ -n "$PC_URL_OVERRIDE" ]; then
        PC_URL="$PC_URL_OVERRIDE"
        PC_HOST=$(extract_hostname_from_url "$PC_URL")
    fi

    # Rebuild non-overridden URLs if admin override changed HTTP_HOST_PORT.
    [ -z "$ADMIN_URL_OVERRIDE" ] && ADMIN_URL=$(build_http_url "$ADMIN_HOST")
    [ -z "$H5_URL_OVERRIDE" ] && H5_URL=$(build_http_url "$H5_HOST")
    [ -z "$PC_URL_OVERRIDE" ] && PC_URL=$(build_http_url "$PC_HOST")

    derive_api_base_from_admin_url
    QIANKUN_ENTRY_URL="${ADMIN_URL}/newpc/"
    NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS=$(origin_from_url "$ADMIN_URL")
    configure_docker_publish_env
    print_hosts_tip

    log_info "访问地址：管理后台=$ADMIN_URL  API=$API_BASE_URL  H5=$H5_URL  PC=$PC_URL"
}

show_help() {
    cat <<EOF
ECShopX-Java Java 开发环境设置脚本

用法: $0 [选项]

选项:
  --help, -h         显示帮助
  --mode MODE        业务模式: b2c 或 bbc
  --rebuild          强制重新构建 Docker 镜像（--no-cache）
  --skip-admin       跳过管理后台编译
  --skip-vshop       跳过移动商城 H5 编译
  --skip-pc          跳过 PC 前端仓库克隆检查
  --http-port N      宿主机 HTTP 端口（默认 80）
  --xxl-port N       宿主机 XXL-Job 端口（默认 8080）
  --admin-host HOST  管理后台域名（默认 admin.ecshopx.test）
  --h5-host HOST     H5 域名（默认 h5.ecshopx.test）
  --pc-host HOST     PC 域名（默认 www.ecshopx.test）
  --admin-url URL    覆盖管理后台地址
  --h5-url URL       覆盖 H5 地址
  --pc-url URL       覆盖 PC 地址
  --admin-password P 后台登录密码（无交互终端时必填）
EOF
    exit 0
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help|-h) show_help ;;
            --rebuild) REBUILD=true; shift ;;
            --skip-admin) SKIP_ADMIN=true; shift ;;
            --skip-vshop) SKIP_VSHOP=true; shift ;;
            --skip-pc) SKIP_PC=true; shift ;;
            --mode)
                [ -z "${2:-}" ] && { log_error "--mode 需要 b2c 或 bbc"; exit 1; }
                MODE="$2"; shift 2 ;;
            --http-port)
                [ -z "${2:-}" ] && { log_error "--http-port 需要端口号"; exit 1; }
                HTTP_HOST_PORT="$2"; HTTP_PORT_FROM_CLI=true; shift 2 ;;
            --xxl-port)
                [ -z "${2:-}" ] && { log_error "--xxl-port 需要端口号"; exit 1; }
                XXL_HOST_PORT="$2"; shift 2 ;;
            --admin-host)
                [ -z "${2:-}" ] && { log_error "--admin-host 需要主机名"; exit 1; }
                ADMIN_HOST="$2"; ADMIN_HOST_FROM_CLI=true; shift 2 ;;
            --h5-host)
                [ -z "${2:-}" ] && { log_error "--h5-host 需要主机名"; exit 1; }
                H5_HOST="$2"; H5_HOST_FROM_CLI=true; shift 2 ;;
            --pc-host)
                [ -z "${2:-}" ] && { log_error "--pc-host 需要主机名"; exit 1; }
                PC_HOST="$2"; PC_HOST_FROM_CLI=true; shift 2 ;;
            --admin-url)
                [ -z "${2:-}" ] && { log_error "--admin-url 需要 URL"; exit 1; }
                ADMIN_URL_OVERRIDE="$2"; shift 2 ;;
            --h5-url)
                [ -z "${2:-}" ] && { log_error "--h5-url 需要 URL"; exit 1; }
                H5_URL_OVERRIDE="$2"; shift 2 ;;
            --pc-url)
                [ -z "${2:-}" ] && { log_error "--pc-url 需要 URL"; exit 1; }
                PC_URL_OVERRIDE="$2"; shift 2 ;;
            --admin-password)
                [ -z "${2:-}" ] && { log_error "--admin-password 需要密码"; exit 1; }
                ADMIN_PASSWORD="$2"; shift 2 ;;
            *)
                log_error "未知参数: $1"; echo "使用 --help 查看帮助"; exit 1 ;;
        esac
    done
}

detect_docker_compose() {
    if docker compose version &>/dev/null 2>&1; then
        DOCKER_COMPOSE_CMD="docker compose"
    elif command -v docker-compose &>/dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    else
        log_error "未找到 Docker Compose"; exit 1
    fi
}

check_docker() {
    command -v docker &>/dev/null || { log_error "Docker 未安装"; exit 1; }
    detect_docker_compose
    docker info &>/dev/null 2>&1 || { log_error "Docker 未运行，请先启动 Docker"; exit 1; }
    log_success "Docker 已就绪 ($DOCKER_COMPOSE_CMD)"
}

resolve_business_mode() {
    if [ -n "$MODE" ]; then
        case "$MODE" in
            b2c) SELECTED_PLATFORM="standard"; log_info "业务模式: B2C (build:b2c)" ;;
            bbc) SELECTED_PLATFORM="platform"; log_info "业务模式: BBC (build:bbc)" ;;
            *) log_error "--mode 必须为 b2c 或 bbc"; exit 1 ;;
        esac
        return
    fi

    echo ""
    log_info "请选择业务模式："
    log_info "  1) B2C (线上商城、O2O云店等)"
    log_info "  2) BBC (多商户入驻电商平台)"
    echo ""
    while true; do
        read -r -p "请输入选项 (1 或 2，默认: 1): " choice < /dev/tty || true
        choice=${choice:-1}
        case "$choice" in
            1|b2c) MODE="b2c"; SELECTED_PLATFORM="standard"; log_info "已选择 B2C"; break ;;
            2|bbc) MODE="bbc"; SELECTED_PLATFORM="platform"; log_info "已选择 BBC"; break ;;
            *) log_error "无效选项，请输入 1 或 2" ;;
        esac
    done
}

admin_build_cmd() {
    if [ "$MODE" = "b2c" ]; then
        echo "build:b2c"
    else
        echo "build:bbc"
    fi
}

clone_repo_if_needed() {
    local dir=$1 repo=$2 label=$3
    local installed_var=$4
    local required=${5:-false}

    if [ -d "$dir" ] && [ -f "$dir/package.json" ]; then
        log_info "$label 已存在，跳过克隆"
        eval "$installed_var=true"
        return
    fi

    log_warning "$label 目录缺失或不完整"
    echo -n "是否克隆 $label ？ [Y/n]: "
    read -r answer < /dev/tty || true
    if [ -n "$answer" ] && [ "$answer" != "Y" ] && [ "$answer" != "y" ] && [ "$answer" != "yes" ]; then
        if [ "$required" = true ]; then
            log_error "$label 为安装所必需；请克隆仓库或使用对应的 --skip-* 选项"
            exit 1
        fi
        log_warning "跳过 $label 克隆"
        return
    fi

    [ -d "$dir" ] && find "$dir" -mindepth 1 -delete 2>/dev/null || true
    log_info "克隆 $label ..."
    git clone "$repo" "$dir" || { log_error "$label 克隆失败"; exit 1; }
    log_success "$label 克隆完成"
    eval "$installed_var=true"
}

check_and_clone_frontend() {
    log_step "检查前端项目..."
    [ "$SKIP_PC" != true ] && clone_repo_if_needed "$WEB_DIR" "$WEB_REPO" "ECShopX-Java_Web" INSTALLED_PC
    [ "$SKIP_ADMIN" != true ] && clone_repo_if_needed "$ADMIN_DIR" "$ADMIN_REPO" "ECShopX-Java_Admin" INSTALLED_ADMIN true
    [ "$SKIP_VSHOP" != true ] && clone_repo_if_needed "$VSHOP_DIR" "$VSHOP_REPO" "ECShopX-Java_Mobile" INSTALLED_VSHOP true
    echo ""
}

run_docker_compose() {
    log_step "启动 Docker Compose 服务..."
    cd "$PROJECT_ROOT"
    [ -f "$DOCKER_COMPOSE_FILE" ] || { log_error "未找到 docker-compose.dev.yml"; exit 1; }

    ensure_runtime_base_image || exit 1

    if [ "$REBUILD" = true ]; then
        log_info "强制重新构建镜像 (--no-cache)..."
        $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" build --no-cache
    fi

    $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" up -d --build
    log_success "Docker Compose 服务已启动"

    log_step "更新后台管理员登录密码..."
    if install_apply_admin_password "ecshopx-dev-mysql"; then
        log_success "后台管理员密码已更新（账号: admin）"
    else
        log_error "后台管理员密码更新失败"
        exit 1
    fi
}

# Wait until Java HTTP answers (Flyway migrate runs during Spring Boot startup).
wait_for_java_ready() {
    local url="${1:-http://127.0.0.1:18080/}"
    local max_attempts="${2:-90}"
    local attempt=1
    local code=""

    log_step "等待 Java 就绪（含 Flyway 启动迁移）..."
    while [ "$attempt" -le "$max_attempts" ]; do
        # Do not append a fallback on curl failure — curl still prints 000 on connect errors.
        code=$(curl -s -o /dev/null -w '%{http_code}' -m 3 "$url" 2>/dev/null || true)
        case "$code" in
            ''|000) ;;
            *)
                log_success "Java 已就绪 (HTTP $code)；Flyway 已随启动执行 (spring.flyway.enabled=true)"
                return 0
                ;;
        esac
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "等待 Java 就绪超时（${max_attempts} 次）。请检查: $DOCKER_COMPOSE_CMD -f $DOCKER_COMPOSE_FILE logs ecshopx-app"
    return 1
}

configure_frontend_env() {
    local project_dir=$1
    local project_name=$2
    local api_url=$3
    local app_platform=${4:-}
    local pc_url=${5:-$PC_URL}
    local env_file="$project_dir/.env"
    local env_before env_after

    [ -d "$project_dir" ] || return 0
    FRONTEND_ENV_CHANGED=false
    env_before=$(cat "$env_file" 2>/dev/null || true)

    if [ ! -f "$env_file" ]; then
        if [ -f "$project_dir/.env.example" ]; then
            cp "$project_dir/.env.example" "$env_file"
        else
            touch "$env_file"
        fi
    fi

    case "$project_name" in
        ECShopX-Java_Admin)
            set_env_kv "$env_file" "VUE_APP_BASE_API" "$api_url"
            set_env_kv "$env_file" "VUE_APP_QIANKUN_ENTRY" "$QIANKUN_ENTRY_URL"
            if [ "$INSTALLED_PC" = true ]; then
                set_env_kv "$env_file" "VUE_APP_WEBSITE" "$pc_url"
            fi
            ;;
        ECShopX-Java_Mobile)
            set_env_kv "$env_file" "APP_BASE_URL" "$api_url"
            [ -n "$app_platform" ] && set_env_kv "$env_file" "APP_PLATFORM" "$app_platform"
            ;;
        ECShopX-Java_Web)
            set_env_kv "$env_file" "NUXT_PUBLIC_API_BASE" "$api_url"
            set_env_kv "$env_file" "NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS" "$NUXT_PUBLIC_DECORATION_ADMIN_ORIGINS"
            grep -q "^NUXT_PUBLIC_COMPANY_ID=" "$env_file" 2>/dev/null || \
                echo "NUXT_PUBLIC_COMPANY_ID=1" >> "$env_file"
            ;;
    esac

    env_after=$(cat "$env_file" 2>/dev/null || true)
    if [ "$env_before" != "$env_after" ]; then
        FRONTEND_ENV_CHANGED=true
    fi
}

build_admin() {
    if [ "$SKIP_ADMIN" = true ]; then
        log_info "跳过管理后台编译 (--skip-admin)"
        return 0
    fi
    [ -d "$ADMIN_DIR" ] || { log_error "ECShopX-Java_Admin 不存在（未使用 --skip-admin）"; return 1; }
    [ -f "$ADMIN_DIR/package.json" ] || { log_error "缺少 package.json"; return 1; }

    log_step "编译 ECShopX-Java_Admin..."
    configure_frontend_env "$ADMIN_DIR" "ECShopX-Java_Admin" "$API_BASE_URL" "" "$PC_URL"

    local build_cmd
    build_cmd=$(admin_build_cmd)
    if [ -d "$ADMIN_DIR/dist" ] && [ -f "$ADMIN_DIR/dist/index.html" ] && [ "$FRONTEND_ENV_CHANGED" != true ] && [ "$REBUILD" != true ]; then
        log_info "已有 dist/，跳过编译（删除 dist 或变更 .env 可强制重编）"
        return 0
    fi

    docker run --rm \
        -v "$PARENT_DIR:/data/httpd" \
        -w /data/httpd/ECShopX-Java_Admin \
        -e npm_config_registry="${NPM_REGISTRY:-https://registry.npmmirror.com}" \
        "$NODE_BUILD_IMAGE" \
        sh -c "npm install --legacy-peer-deps && npm run $build_cmd"

    [ -f "$ADMIN_DIR/dist/index.html" ] || { log_error "管理后台编译产物缺失"; return 1; }
    log_success "ECShopX-Java_Admin 编译完成 (dist/)"
    INSTALLED_ADMIN=true
}

build_vshop() {
    if [ "$SKIP_VSHOP" = true ]; then
        log_info "跳过移动商城编译 (--skip-vshop)"
        return 0
    fi
    [ -d "$VSHOP_DIR" ] || { log_error "ECShopX-Java_Mobile 不存在（未使用 --skip-vshop）"; return 1; }
    [ -f "$VSHOP_DIR/package.json" ] || { log_error "缺少 package.json"; return 1; }

    log_step "编译 ECShopX-Java_Mobile (H5)..."
    configure_frontend_env "$VSHOP_DIR" "ECShopX-Java_Mobile" "$MOBILE_API_URL" "$SELECTED_PLATFORM"

    if [ -d "$VSHOP_DIR/dist/h5" ] && [ -f "$VSHOP_DIR/dist/h5/index.html" ] && [ "$FRONTEND_ENV_CHANGED" != true ] && [ "$REBUILD" != true ]; then
        log_info "已有 dist/h5，跳过编译"
        return 0
    fi

    docker run --rm \
        -v "$PARENT_DIR:/data/httpd" \
        -w /data/httpd/ECShopX-Java_Mobile \
        -e npm_config_registry="${NPM_REGISTRY:-https://registry.npmmirror.com}" \
        "$NODE_BUILD_IMAGE" \
        sh -c "npm install --legacy-peer-deps && npm run build:h5"

    [ -d "$VSHOP_DIR/dist/h5" ] || { log_error "H5 编译产物 dist/h5 不存在"; return 1; }
    log_success "ECShopX-Java_Mobile H5 编译完成 (dist/h5)"
    INSTALLED_VSHOP=true
}

configure_web_frontend() {
    if [ "$SKIP_PC" = true ]; then
        return 0
    fi
    [ -d "$WEB_DIR" ] || return 0
    log_info "配置 ECShopX-Java_Web .env..."
    configure_frontend_env "$WEB_DIR" "ECShopX-Java_Web" "$PC_API_URL"
    INSTALLED_PC=true
}

# Nuxt .output must exist before ecshopx-dev-app starts (entrypoint requires it).
build_web_if_needed() {
    [ "$SKIP_PC" = true ] && return 0
    [ -f "$WEB_DIR/.output/server/index.mjs" ] && [ "$REBUILD" != true ] && {
        log_info "已有 web .output，跳过 Nuxt build"; return 0
    }
    [ -d "$WEB_DIR" ] || { log_error "ECShopX-Java_Web 不存在（未使用 --skip-pc）"; return 1; }
    log_step "编译 ECShopX-Java_Web (Nuxt)..."
    configure_frontend_env "$WEB_DIR" "ECShopX-Java_Web" "$PC_API_URL"
    docker run --rm \
        -v "$PARENT_DIR:/data/httpd" \
        -w /data/httpd/ECShopX-Java_Web \
        -e npm_config_registry="${NPM_REGISTRY:-https://registry.npmmirror.com}" \
        -e COREPACK_NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com}" \
        -e NUXT_API_BASE=http://127.0.0.1:18080/api/v1/h5app \
        -e NUXT_API_BASE_INTERNAL=http://127.0.0.1:18080/api/v1/h5app \
        -e NUXT_PUBLIC_API_BASE="${PC_API_URL:-http://admin.ecshopx.test/api/v1/h5app}" \
        "$NODE_BUILD_IMAGE" \
        sh -c 'corepack enable && pnpm install --registry "$npm_config_registry" && pnpm build'
    [ -f "$WEB_DIR/.output/server/index.mjs" ] || { log_error "Nuxt .output 缺失"; return 1; }
    log_success "ECShopX-Java_Web Nuxt 编译完成 (.output/)"
    INSTALLED_PC=true
}

verify_dist_layout() {
    log_step "检查网关静态资源路径..."
    if [ "$SKIP_ADMIN" != true ] && [ -d "$ADMIN_DIR" ]; then
        [ -d "$ADMIN_DIR/dist" ] || log_warning "管理后台 dist/ 尚未生成"
    fi
    if [ "$SKIP_VSHOP" != true ] && [ -d "$VSHOP_DIR" ]; then
        [ -d "$VSHOP_DIR/dist/h5" ] || log_warning "移动商城 dist/h5 尚未生成"
    fi
}

show_success_info() {
    local end duration minutes seconds
    end=$(date +%s)
    duration=$((end - START_TIME))
    minutes=$((duration / 60))
    seconds=$((duration % 60))

    echo ""
    log_success "=========================================="
    log_success "安装完成"
    log_success "=========================================="
    echo ""
    log_info "服务地址："
    log_info "  管理后台: $ADMIN_URL （账号 admin，密码为安装时设置）"
    if [ "$JWT_GENERATED" = true ]; then
        log_info "  JWT_SECRET: 已自动生成（见 .env；容器由环境变量 / APP_ARGS 注入）"
    fi
    log_info "  H5前端:   $H5_URL"
    log_info "  PC前端:   $PC_URL"
    log_info "  API 接口: $API_BASE_URL"
    log_info "  XXL-Job:  http://localhost:${XXL_HOST_PORT}/xxl-job-admin"
    log_info "  Java API: http://localhost:18080 (直连调试)"
    log_info "  数据库:   Flyway 启动迁移已开启 (spring.flyway.enabled=true)"
    echo ""
    print_hosts_tip
    echo ""
    log_info "常用命令："
    log_info "  查看日志: $DOCKER_COMPOSE_CMD -f $DOCKER_COMPOSE_FILE logs -f"
    log_info "  停止服务: $DOCKER_COMPOSE_CMD -f $DOCKER_COMPOSE_FILE down"
    log_info "  重新构建: $0 --rebuild"
    log_info "执行时间: ${minutes}分${seconds}秒"
    log_success "=========================================="
}

main() {
    parse_args "$@"

    echo "=========================================="
    echo "  ECShopX-Java Java 开发环境设置 (Docker)"
    echo "  docker-compose.dev.yml 全栈编排"
    echo "=========================================="
    echo ""

    check_docker
    resolve_business_mode
    configure_public_urls
    configure_install_secrets
    ensure_runtime_base_image || exit 1
    check_and_clone_frontend
    configure_web_frontend
    if ! build_admin; then
        log_error "管理后台编译失败"
        exit 1
    fi
    if ! build_vshop; then
        log_error "移动商城编译失败"
        exit 1
    fi
    if ! build_web_if_needed; then
        log_error "PC/Nuxt 前端编译失败"
        exit 1
    fi
    run_docker_compose
    if ! wait_for_java_ready "http://127.0.0.1:18080/"; then
        exit 1
    fi
    verify_dist_layout
    show_success_info
}

main "$@"
