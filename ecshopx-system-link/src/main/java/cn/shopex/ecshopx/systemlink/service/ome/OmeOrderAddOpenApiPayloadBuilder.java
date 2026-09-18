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

package cn.shopex.ecshopx.systemlink.service.ome;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builds {@code ome.order.add} request payload from the same admin order bundle used for detail APIs,
 * aligned with the legacy OME order struct shape (amounts in yuan strings, JSON sub-documents as
 * serialized strings).
 */
@Component
public class OmeOrderAddOpenApiPayloadBuilder {

	private static final DateTimeFormatter REFRESH_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final AdminNormalOrderDetailService adminNormalOrderDetailService;

	public OmeOrderAddOpenApiPayloadBuilder(AdminNormalOrderDetailService adminNormalOrderDetailService) {
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
	}

	public Optional<Map<String, Object>> tryBuild(long companyId, String orderIdRaw, String sourceTypeIgnored) {
		if (!StringUtils.hasText(orderIdRaw)) {
			return Optional.empty();
		}
		String orderIdStr = orderIdRaw.trim();
		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdStr, false, "api");
		} catch (BadRequestException ex) {
			return Optional.empty();
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		@SuppressWarnings("unchecked")
		Map<String, Object> tradeInfo = (Map<String, Object>) bundle.get("tradeInfo");
		if (orderInfo == null || orderInfo.isEmpty()) {
			return Optional.empty();
		}
		if ("ziti".equals(str(orderInfo.get("receipt_type")))) {
			return Optional.empty();
		}
		if (tradeInfo == null || tradeInfo.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Object> tax = resolveTax(orderInfo);
		Map<String, Object> company = defaultCompanyRow();
		int itemFeeFen = intVal(orderInfo.get("item_fee"));
		int freightFen = intVal(orderInfo.get("freight_fee"));
		int discountFen = intVal(orderInfo.get("discount_fee"));
		int payFeeFen = intVal(tradeInfo.get("pay_fee"));
		int totalFeeFen = intVal(tradeInfo.get("total_fee"));
		String payType = str(tradeInfo.get("pay_type"));
		String orderStatus = str(orderInfo.get("order_status"));
		String status;
		String payStatus;
		switch (orderStatus) {
			case "DONE" -> {
				status = "finish";
				payStatus = "1";
			}
			case "NOTPAY" -> {
				status = "active";
				payStatus = "0";
			}
			case "PAYED" -> {
				status = "active";
				payStatus = "1";
			}
			case "CANCEL" -> {
				status = "dead";
				payStatus = "5";
			}
			default -> {
				status = "active";
				payStatus = "0";
			}
		}
		long updateTime = longVal(orderInfo.get("update_time"));
		String lastmodify =
				updateTime > 0
						? Instant.ofEpochSecond(updateTime).atZone(ZoneId.systemDefault()).format(REFRESH_TIME)
						: Instant.now().atZone(ZoneId.systemDefault()).format(REFRESH_TIME);

		String payedYuan = fenToYuan(payFeeFen);
		String totalAmountYuan = fenToYuan(totalFeeFen);
		if ("dhpoint".equals(payType)) {
			int sum = itemFeeFen + freightFen;
			payedYuan = fenToYuan(sum);
			totalAmountYuan = payedYuan;
		}
		if (new BigDecimal(payedYuan).compareTo(new BigDecimal(totalAmountYuan)) > 0) {
			payedYuan = totalAmountYuan;
		}

		Map<String, Object> memberInfo = new LinkedHashMap<>();
		memberInfo.put("user_id", orderInfo.get("user_id"));
		memberInfo.put("name", str(orderInfo.get("receiver_name")));
		memberInfo.put("mobile", str(orderInfo.get("receiver_mobile")));

		Map<String, Object> payinfo = new LinkedHashMap<>();
		payinfo.put("pay_name", payType);
		payinfo.put("cost_payment", "0.000");

		Map<String, Object> shipping = new LinkedHashMap<>();
		shipping.put("cost_shipping", fenToYuan(freightFen));

		Map<String, Object> orderStruct = new LinkedHashMap<>();
		orderStruct.put("to_api_v", "3.0");
		orderStruct.put("refresh_time", Instant.now().atZone(ZoneId.systemDefault()).format(REFRESH_TIME));
		orderStruct.put("cost_item", fenToYuan(itemFeeFen));
		orderStruct.put("lastmodify", lastmodify);
		orderStruct.put("title", stripEmojiRough(str(orderInfo.get("title"))));
		orderStruct.put("from_type", "ecos.b2c");
		orderStruct.put("order_bn", str(orderInfo.get("order_id")));
		orderStruct.put("pmt_detail", "[]");
		orderStruct.put("pmt_goods", "0.000");
		orderStruct.put("score_u", "0.000");
		orderStruct.put("timestamp", System.currentTimeMillis() / 1000L);
		orderStruct.put("from_api_v", "2.0");
		orderStruct.put("score_g", "0.000");
		orderStruct.put("is_tax", tax.get("is_tax"));
		orderStruct.put("tax_title", tax.get("title"));
		orderStruct.put("cost_tax", tax.get("cost_tax"));
		orderStruct.put("orders_number", "1");
		orderStruct.put("mark_text", stripEmojiRough(str(orderInfo.get("remark"))));
		orderStruct.put("from_release_version", "default");
		orderStruct.put("modified", System.currentTimeMillis() / 1000L);
		orderStruct.put("payed", payedYuan);
		orderStruct.put("order_objects", "[]");
		orderStruct.put("payments", "[]");
		orderStruct.put("pay_bn", payType);
		orderStruct.put("weight", "0.000");
		orderStruct.put("cur_rate", "1.0000");
		orderStruct.put("consignee", "{}");
		orderStruct.put("currency", "CNY");
		orderStruct.put("node_type", "ecos.ome");
		orderStruct.put("consigner", "{}");
		orderStruct.put("payinfo", toJsonString(payinfo));
		orderStruct.put("custom_mark", "");
		orderStruct.put("node_version", "2.0");
		orderStruct.put("shipping_tid", "3");
		orderStruct.put("selling_agent", "");
		orderStruct.put("pay_status", payStatus);
		orderStruct.put("status", status);
		orderStruct.put("pmt_order", fenToYuan(discountFen));
		orderStruct.put("member_info", toJsonString(memberInfo));
		orderStruct.put("discount", "0.000");
		orderStruct.put("payment_lists", "[]");
		orderStruct.put("total_amount", totalAmountYuan);
		orderStruct.put("to_type", "ecos.ome");
		orderStruct.put("ship_status", "0");
		orderStruct.put("cur_amount", totalAmountYuan);
		orderStruct.put("shipping", toJsonString(shipping));
		orderStruct.put("sales_org", company.get("vkorg"));
		orderStruct.put("customer_code", company.get("kunnr"));
		orderStruct.put("customer_name", company.get("mall_id"));
		orderStruct.put("brand_code", company.get("werks"));
		orderStruct.put("buyer_id", orderInfo.get("user_id"));
		orderStruct.put("createtime", System.currentTimeMillis() / 1000L);
		orderStruct.put("to_node_type", "ecos.ome");

		return Optional.of(orderStruct);
	}

	private static Map<String, Object> defaultCompanyRow() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("vkorg", "");
		m.put("kunnr", "");
		m.put("mall_id", "");
		m.put("werks", "");
		return m;
	}

	private static Map<String, Object> resolveTax(Map<String, Object> orderInfo) {
		Map<String, Object> tax = new LinkedHashMap<>();
		Object invObj = orderInfo.get("invoice");
		if (invObj instanceof Map<?, ?> inv && !inv.isEmpty()) {
			Object title = inv.get("title");
			String t = title != null ? title.toString().trim() : "";
			tax.put("title", StringUtils.hasText(t) ? t : "0");
			tax.put("is_tax", 1);
		} else {
			tax.put("title", "0");
			tax.put("is_tax", 0);
		}
		tax.put("cost_tax", "0.00");
		return tax;
	}

	private static String toJsonString(Map<String, Object> m) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		for (Map.Entry<String, Object> e : m.entrySet()) {
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('"').append(escapeJson(e.getKey())).append("\":");
			Object v = e.getValue();
			if (v == null) {
				sb.append("null");
			} else if (v instanceof Number n) {
				sb.append(n);
			} else {
				sb.append('"').append(escapeJson(String.valueOf(v))).append('"');
			}
		}
		sb.append('}');
		return sb.toString();
	}

	private static String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String stripEmojiRough(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return s.replaceAll("\\p{So}+", "").trim();
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String fenToYuan(int fen) {
		return BigDecimal.valueOf(fen)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}
}
