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

package cn.shopex.ecshopx.digios.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 对齐 PHP {@code DigiOsStoreAdjustmentLineResolver}。 */
@Service
public class DigiOsStoreAdjustmentLineResolver {

	public record AdjustmentContext(String nm, String shopCode, List<Map<String, Object>> lines) {}

	private final JdbcTemplate jdbcTemplate;

	public DigiOsStoreAdjustmentLineResolver(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public AdjustmentContext resolveForDelivery(long companyId, long orderId) {
		if (companyId < 1L || orderId < 1L) {
			return null;
		}
		List<Map<String, Object>> orders = jdbcTemplate.queryForList(
				"""
				SELECT distributor_id FROM orders_normal_orders
				WHERE company_id = ? AND order_id = ?
				LIMIT 1
				""",
				companyId,
				orderId);
		if (orders.isEmpty()) {
			return null;
		}
		long distributorId = longVal(orders.get(0).get("distributor_id"));
		String shopCode = resolveShopCode(companyId, distributorId);
		if (!StringUtils.hasText(shopCode)) {
			return null;
		}
		List<Map<String, Object>> deliveries = jdbcTemplate.queryForList(
				"""
				SELECT orders_delivery_id FROM orders_delivery
				WHERE company_id = ? AND order_id = ?
				ORDER BY orders_delivery_id DESC
				LIMIT 1
				""",
				companyId,
				orderId);
		if (deliveries.isEmpty()) {
			return null;
		}
		long deliveryId = longVal(deliveries.get(0).get("orders_delivery_id"));
		List<Map<String, Object>> deliveryItems = jdbcTemplate.queryForList(
				"""
				SELECT di.num, di.order_items_id, oi.item_bn
				FROM orders_delivery_items di
				LEFT JOIN orders_normal_orders_items oi ON oi.id = di.order_items_id
				WHERE di.orders_delivery_id = ?
				""",
				deliveryId);
		List<Map<String, Object>> lines = mapDeliveryLines(deliveryItems);
		if (lines.isEmpty()) {
			return null;
		}
		return new AdjustmentContext(String.valueOf(orderId), shopCode, lines);
	}

	public AdjustmentContext resolveForAftersalesReturn(long companyId, long aftersalesBn) {
		if (companyId < 1L || aftersalesBn < 1L) {
			return null;
		}
		List<Map<String, Object>> mains = jdbcTemplate.queryForList(
				"""
				SELECT aftersales_type FROM aftersales
				WHERE company_id = ? AND aftersales_bn = ?
				LIMIT 1
				""",
				companyId,
				aftersalesBn);
		if (mains.isEmpty()) {
			return null;
		}
		String type = String.valueOf(mains.get(0).get("aftersales_type"));
		if (!"REFUND_GOODS".equalsIgnoreCase(type)) {
			return null;
		}
		List<Map<String, Object>> details = jdbcTemplate.queryForList(
				"""
				SELECT item_bn, refunded_num, num, distributor_id
				FROM aftersales_detail
				WHERE company_id = ? AND aftersales_bn = ?
				""",
				companyId,
				aftersalesBn);
		if (details.isEmpty()) {
			return null;
		}
		String shopCode = null;
		List<Map<String, Object>> lines = new ArrayList<>();
		for (Map<String, Object> detail : details) {
			String skuId = stringVal(detail.get("item_bn"));
			int qty = intVal(detail.get("refunded_num"));
			if (qty < 1) {
				qty = intVal(detail.get("num"));
			}
			if (!StringUtils.hasText(skuId) || qty < 1) {
				continue;
			}
			if (shopCode == null) {
				long distributorId = longVal(detail.get("distributor_id"));
				shopCode = resolveShopCode(companyId, distributorId);
				if (!StringUtils.hasText(shopCode)) {
					return null;
				}
			}
			lines.add(Map.of("sku_id", skuId, "qty", qty));
		}
		if (lines.isEmpty()) {
			return null;
		}
		return new AdjustmentContext(String.valueOf(aftersalesBn), shopCode, lines);
	}

	private List<Map<String, Object>> mapDeliveryLines(List<Map<String, Object>> deliveryItems) {
		List<Map<String, Object>> lines = new ArrayList<>();
		for (Map<String, Object> item : deliveryItems) {
			int qty = intVal(item.get("num"));
			if (qty < 1) {
				continue;
			}
			String skuId = stringVal(item.get("item_bn"));
			if (!StringUtils.hasText(skuId)) {
				continue;
			}
			lines.add(Map.of("sku_id", skuId, "qty", qty));
		}
		return lines;
	}

	private String resolveShopCode(long companyId, long distributorId) {
		if (companyId < 1L || distributorId < 1L) {
			return null;
		}
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"""
				SELECT shop_code FROM distribution_distributor
				WHERE company_id = ? AND distributor_id = ?
				LIMIT 1
				""",
				companyId,
				distributorId);
		if (rows.isEmpty()) {
			return null;
		}
		String shopCode = stringVal(rows.get(0).get("shop_code"));
		return StringUtils.hasText(shopCode) ? shopCode.trim() : null;
	}

	private static long longVal(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
