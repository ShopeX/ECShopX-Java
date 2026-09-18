/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.popularize.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Ensures admin promoter list rows carry the same stable scalar keys as the legacy list payload for
 * {@code popularize_promoter} columns (ORM serialization shape), including defaults when the JDBC map
 * omitted null columns.
 */
final class PromoterAdminListPromoterRowCompleter {

	private PromoterAdminListPromoterRowCompleter() {
	}

	/**
	 * Resolves {@code popularize_promoter.updated} from the JDBC row only (not merged member fields).
	 */
	static Object resolvePromoterJdbcUpdatedRaw(Map<String, Object> promoterJdbcRow) {
		if (promoterJdbcRow == null) {
			return null;
		}
		if (promoterJdbcRow.containsKey("updated")) {
			return promoterJdbcRow.get("updated");
		}
		if (promoterJdbcRow.containsKey("updatedAt")) {
			return promoterJdbcRow.get("updatedAt");
		}
		return null;
	}

	/**
	 * Epoch seconds as a plain decimal string (list contract), or {@code null} when absent / unparsable.
	 */
	static String formatPromoterUpdatedEpochSecondsAsDecimalString(Object raw) {
		if (raw == null) {
			return null;
		}
		long seconds;
		if (raw instanceof Number n) {
			long lv = n.longValue();
			if (lv > 100_000_000_000L) {
				lv = lv / 1000L;
			}
			seconds = lv;
		} else if (raw instanceof Date d) {
			seconds = d.getTime() / 1000L;
		} else if (raw instanceof java.sql.Timestamp t) {
			seconds = t.getTime() / 1000L;
		} else {
			String s = String.valueOf(raw).trim();
			if (s.isEmpty()) {
				return null;
			}
			try {
				long lv = Long.parseLong(s);
				if (lv > 100_000_000_000L) {
					lv = lv / 1000L;
				}
				seconds = lv;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return String.valueOf(seconds);
	}

	/**
	 * Writes list-row {@code updated} from the promoter JDBC snapshot and returns the same serialized value
	 * for a sidecar map so later merges cannot revive {@code members.updated}.
	 */
	static String applyListRowUpdatedFromPromoterJdbcRow(
			Map<String, Object> mergedRow, Map<String, Object> promoterJdbcRow) {
		String s = formatPromoterUpdatedEpochSecondsAsDecimalString(resolvePromoterJdbcUpdatedRaw(promoterJdbcRow));
		if (mergedRow != null) {
			mergedRow.put("updated", s);
		}
		return s;
	}

	static void reapplyListRowUpdatedFromSidecar(
			List<Map<String, Object>> listRows, Map<Long, String> serializedUpdatedByUserId) {
		if (listRows == null || listRows.isEmpty() || serializedUpdatedByUserId == null
				|| serializedUpdatedByUserId.isEmpty()) {
			return;
		}
		for (Map<String, Object> row : listRows) {
			if (row == null) {
				continue;
			}
			long uid = longFrom(row.get("user_id"));
			if (uid <= 0L) {
				continue;
			}
			if (serializedUpdatedByUserId.containsKey(uid)) {
				row.put("updated", serializedUpdatedByUserId.get(uid));
			}
		}
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	static void fillAbsentStablePromoterColumns(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return;
		}
		putNullIfAbsent(row, "offline_card_code");
		putNullIfAbsent(row, "authorizer_appid");
		putNullIfAbsent(row, "remarks");
		putNullIfAbsent(row, "third_data");
		putNullIfAbsent(row, "fp_salesperson");
		putDefaultIfAbsent(row, "promoter_name", "");
		putDefaultIfAbsent(row, "regions_id", "");
		putDefaultIfAbsent(row, "address", "");
		putDefaultIfAbsent(row, "shop_name", "");
		putDefaultIfAbsent(row, "alipay_name", "");
		putDefaultIfAbsent(row, "shop_pic", "");
		putDefaultIfAbsent(row, "brief", "");
		putDefaultIfAbsent(row, "alipay_account", "");
		putDefaultIfAbsent(row, "reason", "");
		putDefaultIfAbsent(row, "pmobile", "");
		putDefaultIfAbsent(row, "pname", "");
		putDefaultIfAbsent(row, "pid", 0L);
		putDefaultIfAbsent(row, "shop_status", 0);
		putDefaultIfAbsent(row, "grade_level", 0);
		putDefaultIfAbsent(row, "is_subordinates", 0);
		putDefaultIfAbsent(row, "is_promoter", 0);
		putDefaultIfAbsent(row, "disabled", 0);
		putDefaultIfAbsent(row, "is_buy", 0);
		putDefaultIfAbsent(row, "identity_id", 0L);
	}

	private static void putDefaultIfAbsent(Map<String, Object> row, String key, Object def) {
		if (!row.containsKey(key)) {
			row.put(key, def);
		}
	}

	private static void putNullIfAbsent(Map<String, Object> row, String key) {
		if (!row.containsKey(key)) {
			row.put(key, null);
		}
	}
}
