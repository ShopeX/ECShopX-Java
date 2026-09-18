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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiThirdApiV2OrderDetailFormatSupport {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	public Map<String, Object> formatOrderInfoStruct(
			long companyId,
			Map<String, Object> orderInfo,
			String shopCode,
			String tradeNo,
			String payTime,
			String username,
			String unionid,
			String openid,
			Map<Long, Long> goodsIdByItemId,
			OpenapiThirdApiV2OrderListFormatSupport listFormatSupport) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("order_id", String.valueOf(orderInfo.get("order_id")));
		result.put("trade_no", tradeNo);
		result.put("title", nz(orderInfo.get("title")));
		result.put("shop_code", shopCode);
		result.put("total_fee", intOrZero(orderInfo.get("total_fee")));
		result.put("mobile", nz(orderInfo.get("mobile")));
		result.put("freight_fee", intOrZero(orderInfo.get("freight_fee")));
		result.put("item_fee", intOrZero(orderInfo.get("item_fee")));
		result.put("receipt_type", nz(orderInfo.get("receipt_type")));
		result.put("order_status", nz(orderInfo.get("order_status")));
		result.put("pay_status", nz(orderInfo.get("pay_status")));
		result.put("pay_time", payTime != null ? payTime : "");
		result.put("delivery_corp", nz(orderInfo.get("delivery_corp")));
		result.put("delivery_code", nz(orderInfo.get("delivery_code")));
		result.put("delivery_time", formatOptionalEpoch(orderInfo.get("delivery_time")));
		result.put("end_time", formatOptionalEpoch(orderInfo.get("end_time")));
		result.put("delivery_status", nz(orderInfo.get("delivery_status")));
		result.put("cancel_status", nz(orderInfo.get("cancel_status")));
		result.put("receiver_name", nz(orderInfo.get("receiver_name")));
		result.put("receiver_mobile", nz(orderInfo.get("receiver_mobile")));
		result.put("receiver_zip", nz(orderInfo.get("receiver_zip")));
		result.put("receiver_state", nz(orderInfo.get("receiver_state")));
		result.put("receiver_city", nz(orderInfo.get("receiver_city")));
		result.put("receiver_district", nz(orderInfo.get("receiver_district")));
		result.put("receiver_address", nz(orderInfo.get("receiver_address")));
		result.put("member_discount", intOrZero(orderInfo.get("member_discount")));
		result.put("coupon_discount", intOrZero(orderInfo.get("coupon_discount")));
		result.put("discount_fee", intOrZero(orderInfo.get("discount_fee")));
		result.put("discount_info", listFormatSupport.parseDiscountInfo(orderInfo.get("discount_info")));
		result.put("create_time", formatRequiredEpoch(orderInfo.get("create_time")));
		result.put("update_time", formatRequiredEpoch(orderInfo.get("update_time")));
		result.put("pay_type", nz(orderInfo.get("pay_type")));
		result.put("remark", nz(orderInfo.get("remark")));
		result.put("distributor_remark", strOrDefault(orderInfo.get("distributor_remark"), ""));
		result.put("point_use", orderInfo.get("point_use"));
		result.put("point_fee", intOrZero(orderInfo.get("point_fee")));
		result.put("items", formatItems(orderInfo.get("items"), goodsIdByItemId, listFormatSupport));
		result.put("user_id", orderInfo.get("user_id"));
		result.put("username", username != null ? username : "");
		result.put("unionid", unionid != null ? unionid : "");
		result.put("openid", openid != null ? openid : "");
		result.put("order_class", nz(orderInfo.get("order_class")));
		return result;
	}

	private static List<Map<String, Object>> formatItems(
			Object itemsObj,
			Map<Long, Long> goodsIdByItemId,
			OpenapiThirdApiV2OrderListFormatSupport listFormatSupport) {
		if (!(itemsObj instanceof List<?> items)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object itemObj : items) {
			if (!(itemObj instanceof Map<?, ?> rawItem)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> item = (Map<String, Object>) rawItem;
			long itemId = longVal(item.get("item_id"));
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("goods_id", goodsIdByItemId.getOrDefault(itemId, 0L).intValue());
			row.put("item_bn", nz(item.get("item_bn")));
			row.put("item_name", nz(item.get("item_name")));
			row.put("num", intOrZero(item.get("num")));
			row.put("price", intOrZero(item.get("price")));
			row.put("total_fee", intOrZero(item.get("total_fee")));
			row.put("item_fee", intOrZero(item.get("item_fee")));
			row.put("discount_info", listFormatSupport.parseDiscountInfo(item.get("discount_info")));
			row.put("point_use", item.get("share_points"));
			row.put("point_fee", intOrZero(item.get("point_fee")));
			row.put("item_spec_desc", nz(item.get("item_spec_desc")));
			row.put("volume", item.get("volume"));
			row.put("weight", item.get("weight"));
			row.put("item_type", nz(item.get("order_item_type")));
			out.add(row);
		}
		return out;
	}

	private static String formatRequiredEpoch(Object raw) {
		long sec = longVal(raw);
		if (sec <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(sec));
	}

	private static String formatOptionalEpoch(Object raw) {
		long sec = longVal(raw);
		if (sec <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(sec));
	}

	private static int intOrZero(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String nz(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static String strOrDefault(Object raw, String defaultValue) {
		return raw == null ? defaultValue : String.valueOf(raw);
	}
}
