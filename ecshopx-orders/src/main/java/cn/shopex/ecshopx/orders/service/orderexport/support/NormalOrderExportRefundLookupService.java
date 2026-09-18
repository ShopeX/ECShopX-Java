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

package cn.shopex.ecshopx.orders.service.orderexport.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderExportRefundLookupService {

	private static final String SQL_ITEM_REFUND = """
			SELECT A.order_id AS order_id, A.item_id AS item_id,
				SUM(COALESCE(C.refunded_fee, 0)) AS refunded_fee,
				SUM(COALESCE(C.refunded_point, 0)) AS refunded_point,
				MAX(C.refund_success_time) AS refund_success_time,
				SUM(COALESCE(A.num, 0)) AS num,
				SUM(COALESCE(A.refund_fee, 0)) AS refund_fee,
				SUM(COALESCE(A.refund_point, 0)) AS refund_point
			FROM aftersales_detail A
			LEFT JOIN aftersales_refund C ON A.aftersales_bn = C.aftersales_bn
				AND C.refund_status IN ('AUDIT_SUCCESS', 'SUCCESS')
			WHERE A.company_id = :companyId AND A.order_id IN (:orderIds)
			GROUP BY A.order_id, A.item_id
			""";

	private static final String SQL_FORWARD_REFUND = """
			SELECT order_id, refund_success_time
			FROM aftersales_refund
			WHERE company_id = :companyId AND order_id IN (:orderIds) AND refund_status = 'SUCCESS'
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public NormalOrderExportRefundLookupService(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Map<String, RefundInfo> loadItemRefunds(long companyId, List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Map.of();
		}
		MapSqlParameterSource p =
				new MapSqlParameterSource("companyId", companyId).addValue("orderIds", orderIds);
		List<Map<String, Object>> rows = jdbc.queryForList(SQL_ITEM_REFUND, p);
		Map<String, RefundInfo> out = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Long orderId = longObj(row.get("order_id"));
			Long itemId = longObj(row.get("item_id"));
			if (orderId == null || itemId == null) {
				continue;
			}
			out.put(key(orderId, itemId), toRefundInfo(row));
		}
		return out;
	}

	public Map<Long, Integer> loadForwardRefundSuccessTime(long companyId, List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Map.of();
		}
		MapSqlParameterSource p =
				new MapSqlParameterSource("companyId", companyId).addValue("orderIds", orderIds);
		List<Map<String, Object>> rows = jdbc.queryForList(SQL_FORWARD_REFUND, p);
		Map<Long, Integer> out = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Long orderId = longObj(row.get("order_id"));
			Integer ts = intObj(row.get("refund_success_time"));
			if (orderId != null && ts != null) {
				out.putIfAbsent(orderId, ts);
			}
		}
		return out;
	}

	public static String key(long orderId, long itemId) {
		return orderId + "_" + itemId;
	}

	private static RefundInfo toRefundInfo(Map<String, Object> row) {
		return new RefundInfo(
				intObj(row.get("num")),
				intObj(row.get("refunded_fee")),
				intObj(row.get("refunded_point")),
				intObj(row.get("refund_fee")),
				intObj(row.get("refund_point")),
				intObj(row.get("refund_success_time")));
	}

	private static Long longObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer intObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public record RefundInfo(
			Integer num,
			Integer refundedFee,
			Integer refundedPoint,
			Integer refundFee,
			Integer refundPoint,
			Integer refundSuccessTime) {}
}
