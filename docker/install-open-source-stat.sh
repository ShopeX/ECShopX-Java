#!/usr/bin/env bash
# Shared open-source install stat report (aligned with PHP ecshopx-api).
# Sourced by dev-setup.sh and docker-lite/deploy.sh (not meant to be executed alone).
#
# Usage after successful install:
#   report_open_source_install_stat /path/to/pom.xml

open_source_stat_md5_upper() {
  local data="$1"
  if command -v md5sum &>/dev/null; then
    printf '%s' "$data" | md5sum | awk '{print toupper($1)}'
  elif command -v md5 &>/dev/null; then
    printf '%s' "$data" | md5 -q | tr '[:lower:]' '[:upper:]'
  else
    echo ""
  fi
}

# Sign: keys excluding sign, sorted name concat k+v, MD5(secret + concat + secret) upper.
open_source_stat_sign() {
  local secret=$1 product=$2 instance_id=$3 version=$4 timestamp=$5
  local s="instance_id${instance_id}product${product}timestamp${timestamp}version${version}"
  open_source_stat_md5_upper "${secret}${s}${secret}"
}

open_source_stat_escape_json() {
  local s=$1
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  printf '%s' "$s"
}

open_source_stat_log() {
  if declare -F release_log_info >/dev/null 2>&1; then
    release_log_info "$@"
  elif declare -F log_info >/dev/null 2>&1; then
    log_info "$@"
  else
    printf '[INFO] %s\n' "$*"
  fi
}

# Prefer release_read_product_version when already sourced (lite); else sed-only parse.
open_source_stat_read_version() {
  local pom_xml=$1
  local ver="" prop=""
  if [ ! -f "$pom_xml" ]; then
    echo "0.0.0"
    return 0
  fi
  if declare -F release_read_product_version >/dev/null 2>&1; then
    ver=$(release_read_product_version "$pom_xml" 2>/dev/null || true)
    if [ -n "$ver" ]; then
      printf '%s' "$ver"
      return 0
    fi
  fi
  # sed only — must not invoke host mvn/java on customer machines.
  ver=$(sed -n '/<artifactId>ecshopx<\/artifactId>/,/<packaging>/ {
    /<version>/ {
      s/.*<version>\([^<]*\)<\/version>.*/\1/p
      q
    }
  }' "$pom_xml")
  case "$ver" in
    '${'*)
      prop=${ver#'${'}
      prop=${prop%'}'}
      if [[ "$prop" =~ ^[A-Za-z0-9._-]+$ ]]; then
        ver=$(sed -n "s|.*<${prop}>\\([^<]*\\)</${prop}>.*|\\1|p" "$pom_xml" | head -1)
      fi
      ;;
  esac
  [ -n "$ver" ] || ver="0.0.0"
  printf '%s' "$ver"
}

get_open_source_instance_id() {
  local mac="" f
  if [[ "${OSTYPE:-}" == darwin* ]]; then
    mac=$(ifconfig 2>/dev/null | awk '/ether/ {print tolower($2); exit}' || true)
  else
    for f in /sys/class/net/*/address; do
      [ ! -f "$f" ] && continue
      case "$f" in
        */lo/address) continue ;;
      esac
      mac=$(tr '[:upper:]' '[:lower:]' <"$f" || true)
      [ -n "$mac" ] && [ "$mac" != "00:00:00:00:00:00" ] && break
    done
  fi
  if [ -z "$mac" ]; then
    mac=$(hostname 2>/dev/null || echo "unknown")
  fi
  echo "${mac:0:64}"
}

# $1 = path to root pom.xml (version source). Failures never abort install.
report_open_source_install_stat() {
  local pom_xml=${1:-}
  local gateway="https://gwnextapi.shopex.cn"
  local secret="aF3dG6hJ1kL9zXcV4bN2mQ"
  ([ -z "$gateway" ] || [ -z "$secret" ]) && return 0
  command -v curl &>/dev/null || return 0

  local product="ecshopx-java"
  local instance_id version timestamp sign base url inst_esc ver_esc body
  instance_id=$(get_open_source_instance_id)
  version=$(open_source_stat_read_version "$pom_xml")
  [ -n "$version" ] || version="0.0.0"
  timestamp=$(date +%s)
  sign=$(open_source_stat_sign "$secret" "$product" "$instance_id" "$version" "$timestamp")
  [ -z "$sign" ] && return 0

  base="${gateway%/}"
  url="${base}/usercenter/open_source/stat/report"
  inst_esc=$(open_source_stat_escape_json "$instance_id")
  ver_esc=$(open_source_stat_escape_json "$version")
  body=$(printf '{"product":"%s","instance_id":"%s","version":"%s","timestamp":%s,"sign":"%s"}' \
    "$product" "$inst_esc" "$ver_esc" "$timestamp" "$sign")

  open_source_stat_log "上报开源安装统计..."
  curl -sS --max-time 15 -o /dev/null -X POST "$url" \
    -H 'Content-Type: application/json' \
    -d "$body" 2>/dev/null || true
  return 0
}
