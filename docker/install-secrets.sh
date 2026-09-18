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

# BCrypt hash compatible with Spring Security BCrypt.checkpw ($2y$ / $2a$).
install_hash_bcrypt() {
  local plain=$1
  local hash
  if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: 需要 Docker 以生成 BCrypt 密码哈希" >&2
    return 1
  fi
  hash=$(docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 x "$plain" 2>/dev/null | cut -d: -f2 | tr -d '\r\n')
  if [ -z "$hash" ] || [[ "$hash" != \$2* ]]; then
    echo "ERROR: BCrypt 哈希生成失败" >&2
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
