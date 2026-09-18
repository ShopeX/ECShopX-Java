#!/usr/bin/env bash
# 后管未就绪时，用 Admin JWT 创建现金 + 预充点测试活动。
# 用法：
#   export ADMIN_TOKEN='Bearer <operator_jwt>'
#   export COMPANY_ID=1
#   ./scripts/seed-prepaid-test-activities.sh
set -euo pipefail

API_BASE="${API_BASE:-http://127.0.0.1:18080/api/v1}"
ADMIN_TOKEN="${ADMIN_TOKEN:?请设置 ADMIN_TOKEN（后管登录 JWT，含 Bearer 前缀）}"
COMPANY_ID="${COMPANY_ID:-1}"
# 按库内实际数据修改：参与企业 ID、首页模板 ID
ENTERPRISE_ID="${ENTERPRISE_ID:-1}"
PAGES_TEMPLATE_ID="${PAGES_TEMPLATE_ID:-1}"
NOW=$(date +%s)
END=$((NOW + 86400 * 30))

common_body() {
  cat <<EOF
{
  "name": "$1",
  "title": "$1",
  "pages_template_id": ${PAGES_TEMPLATE_ID},
  "pic": "https://example.com/pic.jpg",
  "share_pic": "https://example.com/share.jpg",
  "list_pic": "https://example.com/list.jpg",
  "enterprise_id": [${ENTERPRISE_ID}],
  "enterprise_configs": [
    {
      "enterprise_id": ${ENTERPRISE_ID},
      "per_capita_limitfee": 200000,
      "participate_quota": 0,
      "passphrase_code": ""
    }
  ],
  "display_time": ${NOW},
  "employee_begin_time": ${NOW},
  "employee_end_time": ${END},
  "if_relative_join": false,
  "if_share_limitfee": 0,
  "relative_limitfee": 0,
  "minimum_amount": 0,
  "close_modify_hours_after_activity": 24,
  "is_passphrase_enabled": false,
  "price_display_config": {"show_market_price": true, "show_discount": true}
}
EOF
}

create_activity() {
  local mode="$1"
  local name="$2"
  local body
  body=$(common_body "$name")
  body=$(echo "$body" | sed "s/\"if_relative_join\": false/\"purchase_mode\": \"${mode}\",\n  \"if_relative_join\": false/")
  echo ">>> Creating ${mode} activity: ${name}"
  curl -sS -X POST "${API_BASE}/employeepurchase/activity" \
    -H "Authorization: ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$body" | python3 -m json.tool
}

create_activity cash "联调-现金企业购-${NOW}"
create_activity prepaid_point "联调-预充点企业购-${NOW}"
