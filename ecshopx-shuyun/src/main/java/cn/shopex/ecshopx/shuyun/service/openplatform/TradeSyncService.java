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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D4：组装并出站 {@code shuyun.base.trade.sync}。 */
@Service
public class TradeSyncService {

	private static final Logger log = LoggerFactory.getLogger(TradeSyncService.class);
	private static final DateTimeFormatter DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OrderTradeSourceResolver tradeSourceResolver;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public TradeSyncService(
			OpenPlatformConfigService openPlatformConfigService,
			OrderTradeSourceResolver tradeSourceResolver,
			ShuyunOpenPlatformProperties properties,
			ShuyunOpenGatewayClient gatewayClient,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.tradeSourceResolver = tradeSourceResolver;
		this.properties = properties;
		this.gatewayClient = gatewayClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean syncOrder(long companyId, String orderId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			log.info("Shuyun trade_sync job skipped: tenant not eligible. companyId={} orderId={}", companyId, orderId);
			return false;
		}
		Map<String, Object> order = loadOrder(companyId, orderId);
		if (order == null) {
			log.warn("Shuyun trade_sync skipped: order not found companyId={} orderId={}", companyId, orderId);
			return false;
		}
		long userId = toLong(order.get("user_id"));
		int totalFee = toInt(order.get("total_fee"));
		if (userId <= 0 || totalFee <= 0) {
			log.info(
					"Shuyun trade_sync skipped: user_id/total_fee invalid companyId={} orderId={} userId={} totalFee={}",
					companyId,
					orderId,
					userId,
					totalFee);
			return false;
		}
		String tradeSource =
				tradeSourceResolver.resolve(companyId, orderId, stringVal(order.get("order_class")));
		if (!StringUtils.hasText(tradeSource)) {
			return false;
		}
		Map<String, Object> payload = assemble(companyId, orderId, order, tradeSource);
		if (payload == null) {
			return false;
		}
		try {
			gatewayClient.postJson(
					companyId, ShuyunOpenPlatformGatewayActions.TRADE_SYNC, List.of(payload), "offline");
			log.info("Shuyun trade_sync ok companyId={} orderId={}", companyId, orderId);
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun trade.sync failed companyId={} orderId={} err={}",
					companyId,
					orderId,
					e.getMessage());
			return false;
		}
	}

	private Map<String, Object> assemble(
			long companyId, String orderId, Map<String, Object> order, String tradeSource) {
		List<Map<String, Object>> items = loadItems(companyId, orderId);
		if (items.isEmpty()) {
			log.warn("Shuyun trade.sync payload: no order items. companyId={} orderId={}", companyId, orderId);
			return null;
		}
		long distributorId = toLong(order.get("distributor_id"));
		Long shopDistributorId = resolveDistributorId(companyId, distributorId);
		if (shopDistributorId == null) {
			log.warn(
					"Shuyun trade.sync payload: distributor not found. companyId={} orderId={}",
					companyId,
					orderId);
			return null;
		}
		String shopId = shopDistributorId + (properties.getOfflinePlatIdSuffix() == null
				? "-off"
				: properties.getOfflinePlatIdSuffix());
		String shuyunStatus = mapOrderStatus(stringVal(order.get("order_status")), stringVal(order.get("delivery_status")));
		int totalFee = toInt(order.get("total_fee"));
		if (isFullyRefunded(companyId, orderId, totalFee)) {
			shuyunStatus = "TRADE_CLOSED_ALL_REFUND";
		}
		String deliveryType = mapDeliveryType(stringVal(order.get("receipt_type")));
		int freight = toInt(order.get("freight_fee"));
		int discount = toInt(order.get("discount_fee"));
		int created = toInt(order.get("create_time"));
		int updated = toInt(order.get("update_time"));
		int endTime = toInt(order.get("end_time"));
		String payTime = resolvePayTime(companyId, orderId, created);

		List<Map<String, Object>> lines = new ArrayList<>();
		int productNum = 0;
		for (Map<String, Object> line : items) {
			long lineId = toLong(line.get("id"));
			long itemId = toLong(line.get("item_id"));
			int num = toInt(line.get("num"));
			if (lineId <= 0 || itemId <= 0 || num <= 0) {
				continue;
			}
			productNum += num;
			Map<String, Object> ol = new LinkedHashMap<>();
			ol.put("order_item_id", String.valueOf(lineId));
			ol.put("product_id", resolveProductId(companyId, itemId));
			ol.put("sku_id", String.valueOf(itemId));
			ol.put("product_name", stringVal(line.get("item_name")));
			ol.put("price", fenToYuan(toInt(line.get("price"))));
			ol.put("product_num", num);
			ol.put("discount_fee", fenToYuan(toInt(line.get("discount_fee"))));
			ol.put("adjust_fee", 0.0);
			ol.put("pay_time", payTime);
			int consignTs = toInt(line.get("delivery_time"));
			if (consignTs > 0) {
				ol.put("consign_time", formatTs(consignTs));
			}
			String logisticsCompany = stringVal(line.get("delivery_corp"));
			if (StringUtils.hasText(logisticsCompany)) {
				ol.put("logistics_company", logisticsCompany);
			}
			String logisticsNo = stringVal(line.get("delivery_code"));
			if (StringUtils.hasText(logisticsNo)) {
				ol.put("logistics_no", logisticsNo);
			}
			lines.add(ol);
		}
		if (lines.isEmpty()) {
			return null;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("shop_id", shopId);
		payload.put("plat_account", String.valueOf(toLong(order.get("user_id"))));
		payload.put("order_id", orderId);
		payload.put("order_status", shuyunStatus);
		payload.put("trade_type", "FIXED");
		payload.put("is_presale", "0");
		payload.put("trade_source", tradeSource);
		payload.put("payment", fenToYuan(totalFee));
		payload.put("post_fee", fenToYuan(freight));
		payload.put("adjust_fee", 0.0);
		payload.put("product_num", productNum);
		payload.put("created", formatTs(created));
		payload.put("modified", formatTs(updated > 0 ? updated : created));
		payload.put("delivery_type", deliveryType);
		payload.put("trade_discount_fee", fenToYuan(discount));
		payload.put("orders", lines);
		if (shouldSendEndTime(shuyunStatus) && endTime > 0) {
			payload.put("endtime", formatTs(endTime));
		}
		return payload;
	}

	private Map<String, Object> loadOrder(long companyId, String orderId) {
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT order_id, company_id, user_id, total_fee, freight_fee, discount_fee,
						       order_status, delivery_status, receipt_type, order_class,
						       distributor_id, create_time, update_time, end_time
						FROM orders_normal_orders
						WHERE company_id=? AND order_id=?
						LIMIT 1
						""",
						companyId,
						orderId);
		return rows.isEmpty() ? null : rows.get(0);
	}

	private List<Map<String, Object>> loadItems(long companyId, String orderId) {
		return jdbcTemplate.queryForList(
				"""
				SELECT id, item_id, item_name, num, price, discount_fee,
				       delivery_time, delivery_corp, delivery_code, total_fee, supplier_id
				FROM orders_normal_orders_items
				WHERE company_id=? AND order_id=?
				""",
				companyId,
				orderId);
	}

	private String resolvePayTime(long companyId, String orderId, int fallbackCreateTime) {
		Integer te =
				jdbcTemplate.query(
						"""
						SELECT time_expire FROM trade
						WHERE company_id=? AND order_id=?
						ORDER BY time_expire DESC
						LIMIT 1
						""",
						rs -> rs.next() ? rs.getInt(1) : null,
						companyId,
						orderId);
		int ts = te != null && te > 0 ? te : fallbackCreateTime;
		return formatTs(ts);
	}

	private boolean isFullyRefunded(long companyId, String orderId, int orderTotalFeeFen) {
		if (orderTotalFeeFen <= 0) {
			return false;
		}
		Integer sum =
				jdbcTemplate.query(
						"""
						SELECT COALESCE(SUM(CASE WHEN refund_status='SUCCESS' THEN refunded_fee ELSE 0 END), 0)
						FROM aftersales_refund
						WHERE company_id=? AND order_id=?
						""",
						rs -> rs.next() ? rs.getInt(1) : 0,
						companyId,
						orderId);
		int successRefunded = sum == null ? 0 : sum;
		return successRefunded > 0 && successRefunded >= orderTotalFeeFen;
	}

	private Long resolveDistributorId(long companyId, long distributorId) {
		if (distributorId > 0) {
			Integer exists =
					jdbcTemplate.query(
							"SELECT 1 FROM distribution_distributor WHERE company_id=? AND distributor_id=? LIMIT 1",
							rs -> rs.next() ? 1 : null,
							companyId,
							distributorId);
			if (exists != null) {
				return distributorId;
			}
		}
		return jdbcTemplate.query(
				"SELECT distributor_id FROM distribution_distributor WHERE company_id=? AND distributor_self=1 LIMIT 1",
				rs -> rs.next() ? rs.getLong(1) : null,
				companyId);
	}

	private String resolveProductId(long companyId, long itemId) {
		Map<String, Object> row =
				jdbcTemplate.query(
						"SELECT goods_id, default_item_id, item_id FROM items WHERE company_id=? AND item_id=? LIMIT 1",
						rs -> {
							if (!rs.next()) {
								return null;
							}
							Map<String, Object> m = new LinkedHashMap<>();
							m.put("goods_id", rs.getObject("goods_id"));
							m.put("default_item_id", rs.getObject("default_item_id"));
							m.put("item_id", rs.getObject("item_id"));
							return m;
						},
						companyId,
						itemId);
		if (row == null) {
			return ItemProductIdResolver.resolve(0, 0, itemId);
		}
		return ItemProductIdResolver.resolveFromItemRow(
				toLongObj(row.get("goods_id")),
				toLongObj(row.get("default_item_id")),
				toLongObj(row.get("item_id")));
	}

	private static Long toLongObj(Object v) {
		long n = toLong(v);
		return n;
	}

	static String mapOrderStatus(String orderStatus, String deliveryStatus) {
		return switch (orderStatus == null ? "" : orderStatus) {
			case "DONE" -> "TRADE_FINISHED";
			case "CANCEL" -> "TRADE_CLOSED";
			case "WAIT_BUYER_CONFIRM" -> "WAIT_BUYER_CONFIRM_GOODS";
			case "WAIT_PAID_CONFIRM" -> "WAIT_BUYER_CONFIRM_PAY";
			case "WAIT_GROUPS_SUCCESS", "NOTPAY", "PART_PAYMENT" -> "WAIT_BUYER_PAY";
			case "REVIEW_PASS", "PAYED" -> mapShipped(deliveryStatus);
			case "REFUND_SUCCESS" -> "TRADE_CLOSED_ALL_REFUND";
			case "REFUND_PROCESS" -> "WAIT_SELLER_SEND_GOODS";
			default -> "WAIT_BUYER_PAY";
		};
	}

	private static String mapShipped(String deliveryStatus) {
		return switch (deliveryStatus == null ? "" : deliveryStatus) {
			case "DONE" -> "WAIT_BUYER_CONFIRM_GOODS";
			case "PARTAIL" -> "SELLER_CONSIGNED_PART";
			default -> "WAIT_SELLER_SEND_GOODS";
		};
	}

	static String mapDeliveryType(String receiptType) {
		return switch (receiptType == null ? "" : receiptType) {
			case "ziti" -> "SY_SELFLIFT";
			case "dada" -> "SY_INTRA_CITY_SERVICE";
			case "merchant" -> "SY_NONE";
			default -> "SY_EXPRESS";
		};
	}

	private static boolean shouldSendEndTime(String status) {
		return "TRADE_FINISHED".equals(status)
				|| "TRADE_CLOSED".equals(status)
				|| "TRADE_CLOSED_ALL_REFUND".equals(status);
	}

	static double fenToYuan(int fen) {
		return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
	}

	private static String formatTs(int unixTs) {
		long ts = unixTs > 0 ? unixTs : Instant.now().getEpochSecond();
		return DT.format(Instant.ofEpochSecond(ts));
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(stringVal(v));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(stringVal(v));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
