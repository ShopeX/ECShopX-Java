#!/usr/bin/env bash
# 执行 V20260824170000 企业购灌点模式 DDL（本地 MySQL 或 docker compose mysql）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL="$ROOT/ecshopx-bootstrap/src/main/resources/db/migration/V20260824170000__employee_purchase_prepaid_point.sql"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-ecshopx}"
MYSQL_USER="${MYSQL_USER:-ecshopx}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-ecshopx}"

echo "Applying migration to ${MYSQL_USER}@${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}"
mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < "$SQL"
echo "Done. Verifying columns..."
mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" -e \
  "SHOW COLUMNS FROM employee_purchase_activities LIKE 'purchase_mode';
   SHOW COLUMNS FROM employee_purchase_activity_enterprises LIKE 'per_capita_limitfee';
   SHOW COLUMNS FROM employee_purchase_orders_rel_activity LIKE 'purchase_mode';"
