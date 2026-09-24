#!/usr/bin/env bash
# Shared install helpers: admin password prompt + JWT_SECRET auto-generation.
# Sourced by dev-setup.sh and docker-lite/deploy.sh (not meant to be executed alone).

# Upsert KEY=VALUE in an env-style file (no shell escaping of value).
install_upsert_env_file() {
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
    printf '%s=%s\n' "$key" "$value" >>"$file"
  fi
}

# Read JWT_SECRET= value from a properties/env file; empty/missing -> return 1.
install_read_jwt_from_file() {
  local file=$1
  [ -f "$file" ] || return 1
  local line val
  line=$(grep -E '^[[:space:]]*JWT_SECRET=' "$file" | head -1) || return 1
  val=${line#*JWT_SECRET=}
  val=${val%%$'\r'}
  val=$(printf '%s' "$val" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  [ -n "$val" ] || return 1
  printf '%s' "$val"
}

# Ensure JWT_SECRET is non-empty: prefer existing config/env, else generate.
# Sets/export JWT_SECRET and INSTALL_JWT_GENERATED=true|false. Optional args:
#   $1 = application.properties path (read-only check; never written — tracked template)
#   $2 = compose .env path to persist (Docker runtime injection via env / APP_ARGS)
install_ensure_jwt_secret() {
  local props_file=${1:-}
  local compose_env=${2:-}
  local existing=""

  INSTALL_JWT_GENERATED=false

  if [ -n "${JWT_SECRET:-}" ]; then
    existing="$JWT_SECRET"
  elif [ -n "$compose_env" ] && existing=$(install_read_jwt_from_file "$compose_env"); then
    :
  elif [ -n "$props_file" ] && existing=$(install_read_jwt_from_file "$props_file"); then
    :
  fi

  if [ -n "$existing" ]; then
    JWT_SECRET="$existing"
    export JWT_SECRET
    if [ -n "$compose_env" ]; then
      install_upsert_env_file "$compose_env" "JWT_SECRET" "$JWT_SECRET"
    fi
    export INSTALL_JWT_GENERATED
    return 0
  fi

  if ! command -v openssl >/dev/null 2>&1; then
    echo "ERROR: openssl 未安装，无法生成 JWT_SECRET" >&2
    return 1
  fi

  JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n\r')
  export JWT_SECRET
  INSTALL_JWT_GENERATED=true
  export INSTALL_JWT_GENERATED

  # Persist only to compose .env. Do not mutate tracked application.properties.
  # Docker does not use --spring.profiles.active=local, so application-local.properties
  # is never written here.
  if [ -n "$compose_env" ]; then
    install_upsert_env_file "$compose_env" "JWT_SECRET" "$JWT_SECRET"
  fi

  return 0
}

# Prompt (or use ADMIN_PASSWORD / --admin-password) for admin login password.
# Sets ADMIN_PASSWORD. Requires interactive tty unless ADMIN_PASSWORD is preset.
install_prompt_admin_password() {
  local pass1="" pass2=""

  if [ -n "${ADMIN_PASSWORD:-}" ]; then
    if [ "${#ADMIN_PASSWORD}" -lt 6 ]; then
      echo "ERROR: 后台登录密码至少 6 位" >&2
      return 1
    fi
    export ADMIN_PASSWORD
    return 0
  fi

  if [ ! -e /dev/tty ]; then
    echo "ERROR: 无交互终端，请通过 --admin-password 指定后台登录密码" >&2
    return 1
  fi

  echo ""
  echo "请设置管理后台登录密码（账号: admin，至少 6 位）："
  while true; do
    read -r -s -p "后台登录密码: " pass1 < /dev/tty || true
    echo "" >&2
    read -r -s -p "再次确认密码: " pass2 < /dev/tty || true
    echo "" >&2
    if [ -z "${pass1:-}" ]; then
      echo "ERROR: 密码不能为空" >&2
      continue
    fi
    if [ "${#pass1}" -lt 6 ]; then
      echo "ERROR: 密码至少 6 位" >&2
      continue
    fi
    if [ "$pass1" != "$pass2" ]; then
      echo "ERROR: 两次输入不一致，请重试" >&2
      continue
    fi
    ADMIN_PASSWORD="$pass1"
    export ADMIN_PASSWORD
    return 0
  done
}

# Directory of this sourced file (docker/).
install_secrets_dir() {
  local src="${BASH_SOURCE[0]:-}"
  if [ -n "$src" ] && [ -f "$src" ]; then
    cd "$(dirname "$src")" && pwd
    return 0
  fi
  return 1
}

# Prefer the already-running app container (lite: ecshopx-app, full: ecshopx-dev-app).
install_resolve_app_container() {
  local c
  for c in "${INSTALL_APP_CONTAINER:-}" ecshopx-app ecshopx-dev-app; do
    [ -n "$c" ] || continue
    if [ "$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null || true)" = "true" ]; then
      printf '%s' "$c"
      return 0
    fi
  done
  return 1
}

# Local runtime/app image already imported from CDN (no registry pull).
install_resolve_runtime_image() {
  local img
  for img in \
    "${RUNTIME_BASE_IMAGE:-}" \
    "${APP_IMAGE:-}" \
    ecshopx-app:latest \
    ecshopx-java:17-node20-openresty
  do
    [ -n "$img" ] || continue
    if docker image inspect "$img" >/dev/null 2>&1; then
      printf '%s' "$img"
      return 0
    fi
  done
  return 1
}

# BCrypt hash compatible with Spring Security BCrypt.checkpw ($2y$ / $2a$).
# Uses Node inside the running app container (or local runtime image) — never pulls httpd.
install_hash_bcrypt() {
  local plain=$1
  local hash out tools_dir container image remote_dir
  local hash_js min_js

  tools_dir="$(install_secrets_dir)/tools"
  hash_js="$tools_dir/bcrypt-hash.js"
  min_js="$tools_dir/bcrypt.min.js"
  if [ ! -f "$hash_js" ] || [ ! -f "$min_js" ]; then
    echo "ERROR: 缺少 BCrypt 脚本: $tools_dir/bcrypt-hash.js （及 bcrypt.min.js）" >&2
    return 1
  fi

  if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: 需要 Docker 以生成 BCrypt 密码哈希" >&2
    return 1
  fi

  remote_dir="/tmp/ecshopx-bcrypt-$$"
  if container=$(install_resolve_app_container); then
    out=$(
      {
        docker exec "$container" mkdir -p "$remote_dir" &&
          docker cp "$hash_js" "$container:$remote_dir/bcrypt-hash.js" &&
          docker cp "$min_js" "$container:$remote_dir/bcrypt.min.js" &&
          docker exec -e INSTALL_PLAIN_PASSWORD="$plain" "$container" \
            node "$remote_dir/bcrypt-hash.js"
      } 2>&1
    ) || {
      echo "ERROR: BCrypt 哈希生成失败（容器: $container）" >&2
      echo "$out" >&2
      docker exec "$container" rm -rf "$remote_dir" >/dev/null 2>&1 || true
      return 1
    }
    docker exec "$container" rm -rf "$remote_dir" >/dev/null 2>&1 || true
  elif image=$(install_resolve_runtime_image); then
    out=$(
      docker run --rm \
        -e INSTALL_PLAIN_PASSWORD="$plain" \
        -v "$tools_dir:/bcrypt-tools:ro" \
        --entrypoint node \
        "$image" \
        /bcrypt-tools/bcrypt-hash.js 2>&1
    ) || {
      echo "ERROR: BCrypt 哈希生成失败（镜像: $image）" >&2
      echo "$out" >&2
      return 1
    }
  else
    echo "ERROR: 未找到运行中的 app 容器（ecshopx-app / ecshopx-dev-app），也没有本地 runtime 镜像" >&2
    echo "ERROR: 请确认 compose 已启动，或已导入 ecshopx-java:17-node20-openresty" >&2
    return 1
  fi

  hash=$(printf '%s\n' "$out" | tr -d '\r' | grep -E '^\$2[aby]?\$' | tail -n 1)
  if [ -z "$hash" ]; then
    echo "ERROR: BCrypt 哈希生成失败（输出不是 \$2* 格式）" >&2
    echo "$out" >&2
    return 1
  fi
  printf '%s' "$hash"
}

# Wait until mysql container accepts queries.
install_wait_for_mysql() {
  local container=$1
  local max_attempts=${2:-60}
  local attempt=1
  while [ "$attempt" -le "$max_attempts" ]; do
    if docker exec "$container" mysqladmin ping -h 127.0.0.1 -uroot -prootpassword --silent >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
  echo "ERROR: 等待 MySQL 就绪超时: $container" >&2
  return 1
}

# UPDATE operators.password for admin after MySQL is ready.
install_apply_admin_password() {
  local container=$1
  local plain=${2:-${ADMIN_PASSWORD:-}}
  local hash sql

  if [ -z "$plain" ]; then
    echo "ERROR: ADMIN_PASSWORD 未设置" >&2
    return 1
  fi

  install_wait_for_mysql "$container" || return 1
  hash=$(install_hash_bcrypt "$plain") || return 1
  sql=$(printf "UPDATE operators SET password='%s' WHERE login_name='admin' AND operator_type='admin';" "$hash")
  if ! docker exec -i "$container" mysql -uecshopx -pecshopx ecshopx -e "$sql"; then
    echo "ERROR: 更新后台管理员密码失败" >&2
    return 1
  fi
  return 0
}

# Install mode → product model key (common.product-model / APP_PLATFORM).
# b2c → standard (menu_type=4); bbc → platform (menu_type=3)
install_mode_to_product_model() {
  case "$1" in
    b2c) printf 'standard' ;;
    bbc) printf 'platform' ;;
    *) return 1 ;;
  esac
}

install_mode_to_menu_type() {
  case "$1" in
    b2c) printf '4' ;;
    bbc) printf '3' ;;
    *) return 1 ;;
  esac
}

# Persist PRODUCT_MODEL for compose APP_ARGS (--common.product-model=...).
# Call after MODE is resolved and before docker compose up.
install_persist_product_model() {
  local compose_env=${1:-}
  local mode=${2:-}
  local product_model
  product_model=$(install_mode_to_product_model "$mode") || {
    echo "ERROR: 无效业务模式: $mode（需要 b2c|bbc）" >&2
    return 1
  }
  PRODUCT_MODEL="$product_model"
  export PRODUCT_MODEL
  if [ -n "$compose_env" ]; then
    install_upsert_env_file "$compose_env" "PRODUCT_MODEL" "$PRODUCT_MODEL"
  fi
}

# Sync companys.menu_type with selected business mode (seed defaults to 3/platform).
install_apply_company_menu_type() {
  local container=$1
  local mode=${2:-}
  local menu_type product_model

  if [ -z "$mode" ]; then
    echo "ERROR: 业务模式未设置，无法更新 companys.menu_type" >&2
    return 1
  fi
  menu_type=$(install_mode_to_menu_type "$mode") || {
    echo "ERROR: 无效业务模式: $mode（需要 b2c|bbc）" >&2
    return 1
  }
  product_model=$(install_mode_to_product_model "$mode") || return 1

  install_wait_for_mysql "$container" || return 1
  if ! docker exec "$container" mysql -uecshopx -pecshopx ecshopx -e \
    "UPDATE companys SET menu_type=${menu_type};"; then
    echo "ERROR: 更新 companys.menu_type 失败" >&2
    return 1
  fi
  echo "INFO: companys.menu_type=${menu_type} (${product_model}, mode=${mode})"
}
