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

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.service.orderexport.support.NormalOrderExportDistributorLookupService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiThirdApiV2OrderListFormatSupport {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public Map<String, Object> formatOrderListStruct(
			long companyId,
			long totalCount,
			List<NormalOrders> orders,
			Map<Long, NormalOrderExportDistributorLookupService.StoreInfo> storeMap,
			Map<Long, String> tradeIndexByOrderId,
			int page,
			int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));

		if (orders == null || orders.isEmpty()) {
			result.put("list", List.of());
			return result;
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (NormalOrders order : orders) {
			list.add(mapOrder(order, storeMap, tradeIndexByOrderId));
		}
		result.put("list", list);
		return result;
	}

	private static Map<String, Object> mapOrder(
			NormalOrders order,
			Map<Long, NormalOrderExportDistributorLookupService.StoreInfo> storeMap,
			Map<Long, String> tradeIndexByOrderId) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		long orderId = order.getOrderId() != null ? order.getOrderId() : 0L;
		row.put("order_id", String.valueOf(orderId));
		row.put("trade_no", tradeIndexByOrderId.getOrDefault(orderId, "-"));
		row.put("title", nz(order.getTitle()));
		row.put("total_fee", order.getTotalFee());
		Long distributorId = order.getDistributorId() != null ? order.getDistributorId() : 0L;
		NormalOrderExportDistributorLookupService.StoreInfo store =
				storeMap.getOrDefault(
						distributorId, NormalOrderExportDistributorLookupService.StoreInfo.EMPTY);
		row.put("shop_code", store.shopCode());
		row.put("shop_name", store.name());
		row.put("mobile", nz(order.getMobile()));
		row.put("order_status", nz(order.getOrderStatus()));
		row.put("pay_status", nz(order.getPayStatus()));
		row.put("create_time", formatEpochSeconds(order.getCreateTime()));
		row.put("update_time", formatEpochSeconds(order.getUpdateTime()));
		row.put("delivery_corp", nz(order.getDeliveryCorp()));
		row.put("delivery_code", nz(order.getDeliveryCode()));
		row.put("delivery_time", formatOptionalEpochSeconds(order.getDeliveryTime()));
		row.put("delivery_status", nz(order.getDeliveryStatus()));
		row.put("member_discount", order.getMemberDiscount() != null ? order.getMemberDiscount() : 0);
		row.put("coupon_discount", order.getCouponDiscount() != null ? order.getCouponDiscount() : 0);
		row.put("discount_fee", order.getDiscountFee() != null ? order.getDiscountFee() : 0);
		row.put("discount_info", parseDiscountInfo(order.getDiscountInfo()));
		row.put("cancel_status", nz(order.getCancelStatus()));
		row.put("end_time", formatOptionalEpochLong(order.getEndTime()));
		row.put("is_self", distributorId == 0L);
		return row;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch.longValue()));
	}

	private static String formatOptionalEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch.longValue()));
	}

	private static String formatOptionalEpochLong(Long epoch) {
		if (epoch == null || epoch <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch));
	}

	public static List<Map<String, Object>> parseDiscountInfo(Object raw) {
		if (raw instanceof List<?> list) {
			return parseDiscountInfoList(list);
		}
		if (raw instanceof String s) {
			return parseDiscountInfoJson(s);
		}
		return List.of();
	}

	private static List<Map<String, Object>> parseDiscountInfoList(List<?> list) {
		if (list.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object entry : list) {
			if (!(entry instanceof Map<?, ?> info)) {
				continue;
			}
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("id", strOrDefault(info.get("id"), ""));
			item.put("type", strOrDefault(info.get("type"), ""));
			item.put("info", strOrDefault(info.get("info"), ""));
			item.put("rule", strOrDefault(info.get("rule"), ""));
			item.put("discount_fee", numberOrDefault(info.get("discount_fee"), 0));
			out.add(item);
		}
		return out;
	}

	private static List<Map<String, Object>> parseDiscountInfoJson(String discountInfoJson) {
		if (!StringUtils.hasText(discountInfoJson)) {
			return List.of();
		}
		try {
			List<Map<String, Object>> parsed =
					OBJECT_MAPPER.readValue(discountInfoJson, new TypeReference<>() {});
			if (parsed == null || parsed.isEmpty()) {
				return List.of();
			}
			return parseDiscountInfoList(parsed);
		} catch (Exception e) {
			return List.of();
		}
	}

	private static String strOrDefault(Object raw, String defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		return String.valueOf(raw);
	}

	private static Number numberOrDefault(Object raw, Number defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Number n) {
			return n;
		}
		try {
			return Double.parseDouble(String.valueOf(raw));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static String nz(String value) {
		return value != null ? value : "";
	}
}
