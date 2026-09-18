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

package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mirrors {@link TradeRefundPushMarketingCenterProcessor} for trade-aftersales-cancel (consumer close)
 * payloads: DB resolution + shopping-guide gate + {@link MarketingCenterOpenApiSignedFormClient#basicsAftersalesProccess}.
 */
@Component
public class TradeAftersalesCancelPushMarketingCenterProcessor {

	private static final String SQL_AFTERSALES =
			"SELECT aftersales_bn, order_id, company_id, salesman_id FROM aftersales WHERE company_id = ? AND aftersales_bn = ? LIMIT 1";

	private static final String SQL_ORDER =
			"SELECT order_id, company_id, salesman_id, bind_salesman_id, chat_id FROM orders_normal_orders WHERE company_id = ? AND order_id = ? LIMIT 1";

	private final JdbcTemplate jdbcTemplate;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	public TradeAftersalesCancelPushMarketingCenterProcessor(
			JdbcTemplate jdbcTemplate, MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient) {
		this.jdbcTemplate = jdbcTemplate;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		long companyId = longVal(payload.get("company_id"));
		long orderId = longVal(payload.get("order_id"));
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}

		Object bnRaw = payload.get("aftersales_bn");
		if (!hasMeaningfulAftersalesBn(bnRaw)) {
			Optional<Map<String, Object>> orderOpt = queryOrderRow(companyId, orderId);
			if (orderOpt.isEmpty()) {
				return;
			}
			Map<String, Object> orderInfo = orderOpt.get();
			if (!hasShoppingGuideBinding(orderInfo, null)) {
				return;
			}
			Map<String, Object> params = mergeTopLevel(payload, orderInfo);
			marketingCenterOpenApiSignedFormClient.basicsOrderProccess(companyId, params);
			return;
		}

		long aftersalesBn = longVal(bnRaw);
		Optional<Map<String, Object>> asOpt = queryAftersalesRow(companyId, aftersalesBn);
		Optional<Map<String, Object>> orderOpt = queryOrderRow(companyId, orderId);
		if (asOpt.isEmpty() || orderOpt.isEmpty()) {
			return;
		}
		Map<String, Object> asInfo = asOpt.get();
		Map<String, Object> orderInfo = orderOpt.get();
		if (!hasShoppingGuideBinding(orderInfo, asInfo)) {
			return;
		}
		Map<String, Object> params = mergeForAftersalesPath(payload, asInfo, orderInfo);
		marketingCenterOpenApiSignedFormClient.basicsAftersalesProccess(companyId, params);
	}

	private Optional<Map<String, Object>> queryAftersalesRow(long companyId, long aftersalesBn) {
		try {
			Map<String, Object> row =
					jdbcTemplate.queryForObject(
							SQL_AFTERSALES,
							(rs, rn) -> {
								Map<String, Object> m = new LinkedHashMap<>();
								m.put("aftersales_bn", rs.getObject("aftersales_bn"));
								m.put("order_id", rs.getObject("order_id"));
								m.put("company_id", rs.getObject("company_id"));
								m.put("salesman_id", rs.getObject("salesman_id"));
								return m;
							},
							companyId,
							aftersalesBn);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private Optional<Map<String, Object>> queryOrderRow(long companyId, long orderId) {
		try {
			Map<String, Object> row =
					jdbcTemplate.queryForObject(
							SQL_ORDER,
							(rs, rn) -> {
								Map<String, Object> m = new LinkedHashMap<>();
								m.put("order_id", rs.getObject("order_id"));
								m.put("company_id", rs.getObject("company_id"));
								m.put("salesman_id", rs.getObject("salesman_id"));
								m.put("bind_salesman_id", rs.getObject("bind_salesman_id"));
								m.put("chat_id", rs.getObject("chat_id"));
								return m;
							},
							companyId,
							orderId);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private static Map<String, Object> mergeTopLevel(Map<String, Object> refund, Map<String, Object> order) {
		Map<String, Object> m = new LinkedHashMap<>(refund);
		m.putAll(order);
		return m;
	}

	private static Map<String, Object> mergeForAftersalesPath(
			Map<String, Object> top, Map<String, Object> asRow, Map<String, Object> order) {
		Map<String, Object> m = new LinkedHashMap<>(top);
		m.putAll(order);
		m.putAll(asRow);
		return m;
	}

	private static boolean hasMeaningfulAftersalesBn(Object bnRaw) {
		if (bnRaw == null) {
			return false;
		}
		if (bnRaw instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(bnRaw).trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) != 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static boolean hasShoppingGuideBinding(Map<String, Object> orderInfo, Map<String, Object> aftersalesInfo) {
		long salesmanOnOrder = longVal(orderInfo.get("salesman_id"));
		long bindSalesman = longVal(orderInfo.get("bind_salesman_id"));
		boolean chat = StringUtils.hasText(str(orderInfo.get("chat_id")));
		long salesmanOnAftersales =
				aftersalesInfo == null ? 0L : longVal(aftersalesInfo.get("salesman_id"));
		return salesmanOnAftersales != 0L || salesmanOnOrder != 0L || bindSalesman != 0L || chat;
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
