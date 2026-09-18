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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptProcessor {

	private static final String SQL_ORDER_HEADER =
			"SELECT order_id, company_id, salesman_id, bind_salesman_id, chat_id,"
					+ " sale_salesman_distributor_id, bind_salesman_distributor_id,"
					+ " title, mobile, user_id, total_fee, order_status, pay_type, order_class, order_type,"
					+ " shop_id, distributor_id, receipt_type,"
					+ " receiver_name, receiver_mobile, receiver_state, receiver_city, receiver_district, receiver_address,"
					+ " create_time, auto_cancel_time, freight_fee, item_fee, discount_fee"
					+ " FROM orders_normal_orders WHERE company_id = ? AND order_id = ? LIMIT 1";

	private static final String SQL_ORDER_ITEMS =
			"SELECT id, item_id, goods_id, item_bn, goods_bn, item_name, item_unit, pic, num, price, market_price,"
					+ " total_fee, item_fee, cost_fee, commission_fee, shop_id, distributor_id, order_item_type,"
					+ " fee_type, fee_rate, fee_symbol, item_spec_desc"
					+ " FROM orders_normal_orders_items WHERE company_id = ? AND order_id = ? ORDER BY id ASC";

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final JdbcTemplate jdbcTemplate;
	private final OrderAddPushMarketingCenterSalesDataFormatter salesDataFormatter;

	public OrderConfirmReceiptPushMarketingCenterOnNormalOrderConfirmReceiptProcessor(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			JdbcTemplate jdbcTemplate,
			OrderAddPushMarketingCenterSalesDataFormatter salesDataFormatter) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.jdbcTemplate = jdbcTemplate;
		this.salesDataFormatter = salesDataFormatter;
	}

	public void handle(Map<String, Object> busPayload) {
		if (busPayload == null || busPayload.isEmpty()) {
			return;
		}
		long companyId = longVal(busPayload.get("company_id"));
		Object orderIdRaw = busPayload.get("order_id");
		if (companyId <= 0L || orderIdRaw == null) {
			return;
		}
		Optional<Map<String, Object>> orderOpt = queryOrderHeader(companyId, orderIdRaw);
		if (orderOpt.isEmpty()) {
			return;
		}
		Map<String, Object> order = orderOpt.get();
		if (longVal(order.get("salesman_id")) == 0L) {
			return;
		}
		List<Map<String, Object>> items = queryOrderItems(companyId, orderIdRaw);
		LinkedHashMap<String, Object> draft = new LinkedHashMap<>(order);
		draft.put("items", items);
		Object payType = busPayload.get("pay_type");
		if (payType != null) {
			draft.put("pay_type", payType);
		}
		draft.put("order_source", "1");
		Optional<Map<String, Object>> formatted = salesDataFormatter.format(companyId, draft);
		if (formatted.isEmpty()) {
			return;
		}
		Map<String, Object> out = stringifyValuesForOutbound(formatted.get());
		marketingCenterOpenApiSignedFormClient.basicsOrderProccess(companyId, out);
	}

	private Optional<Map<String, Object>> queryOrderHeader(long companyId, Object orderId) {
		try {
			Map<String, Object> row =
					jdbcTemplate.queryForObject(SQL_ORDER_HEADER, (rs, rn) -> mapRow(rs), companyId, orderId);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private List<Map<String, Object>> queryOrderItems(long companyId, Object orderId) {
		return jdbcTemplate.query(SQL_ORDER_ITEMS, (rs, rn) -> mapRow(rs), companyId, orderId);
	}

	private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
		var md = rs.getMetaData();
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			String name = md.getColumnLabel(i);
			if (name == null || name.isEmpty()) {
				name = md.getColumnName(i);
			}
			m.put(name.toLowerCase(Locale.ROOT), rs.getObject(i));
		}
		return m;
	}

	private static Map<String, Object> stringifyValuesForOutbound(Map<String, Object> src) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : src.entrySet()) {
			Object v = e.getValue();
			if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
				out.put(e.getKey(), str(((Number) v).longValue()));
			} else if (v == null) {
				out.put(e.getKey(), str(v));
			} else if (v instanceof List<?> list && list.isEmpty()) {
				out.put(e.getKey(), "");
			} else if (v instanceof List<?> list) {
				List<Object> mapped = new ArrayList<>();
				for (Object o : list) {
					if (o instanceof Map<?, ?> mm) {
						@SuppressWarnings("unchecked")
						Map<String, Object> sm = (Map<String, Object>) mm;
						mapped.add(stringifyValuesForOutbound(sm));
					} else {
						mapped.add(o);
					}
				}
				out.put(e.getKey(), mapped);
			} else {
				out.put(e.getKey(), v);
			}
		}
		return out;
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(str(o));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
