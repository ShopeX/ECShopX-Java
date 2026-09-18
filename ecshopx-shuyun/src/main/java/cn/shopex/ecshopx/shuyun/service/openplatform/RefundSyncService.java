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

/** D5：refund.sync（仅 SUCCESS）；行级载荷对齐 PHP assembler。 */
@Service
public class RefundSyncService {

	private static final Logger log = LoggerFactory.getLogger(RefundSyncService.class);
	private static final DateTimeFormatter DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OrderTradeSourceResolver tradeSourceResolver;
	private final ShuyunOpenPlatformProperties properties;
	private final ShuyunOpenGatewayClient gatewayClient;
	private final JdbcTemplate jdbcTemplate;

	public RefundSyncService(
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

	public boolean syncRefund(long companyId, String refundBn) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(cfg)) {
			return false;
		}
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT refund_bn, order_id, refund_status, refund_fee, refunded_fee,
						       company_id, supplier_id, aftersales_bn, refund_type, refunds_memo,
						       create_time, update_time
						FROM aftersales_refund
						WHERE company_id=? AND refund_bn=?
						LIMIT 1
						""",
						companyId,
						refundBn);
		if (rows.isEmpty()) {
			log.info("Shuyun refund.sync skipped: refund not found companyId={} refundBn={}", companyId, refundBn);
			return false;
		}
		Map<String, Object> refund = rows.get(0);
		if (!"SUCCESS".equals(String.valueOf(refund.get("refund_status")))) {
			log.info(
					"Shuyun refund.sync skipped: refund_status not SUCCESS companyId={} refundBn={} status={}",
					companyId,
					refundBn,
					refund.get("refund_status"));
			return false;
		}
		String orderId = stringVal(refund.get("order_id"));
		if (!StringUtils.hasText(orderId)) {
			return false;
		}
		List<Map<String, Object>> orderRows =
				jdbcTemplate.queryForList(
						"""
						SELECT order_id, order_class, distributor_id, user_id, order_status
						FROM orders_normal_orders WHERE company_id=? AND order_id=? LIMIT 1
						""",
						companyId,
						orderId);
		if (orderRows.isEmpty()) {
			return false;
		}
		Map<String, Object> order = orderRows.get(0);
		String tradeSource =
				tradeSourceResolver.resolve(companyId, orderId, stringVal(order.get("order_class")));
		if (!StringUtils.hasText(tradeSource)) {
			return false;
		}
		Long shopDistributorId = resolveDistributorId(companyId, toLong(order.get("distributor_id")));
		if (shopDistributorId == null) {
			log.warn("Shuyun refund.sync payload: distributor not found companyId={} orderId={}", companyId, orderId);
			return false;
		}
		String suffix = properties.getOfflinePlatIdSuffix() == null ? "-off" : properties.getOfflinePlatIdSuffix();
		String shopId = shopDistributorId + suffix;

		List<Map<String, Object>> items =
				jdbcTemplate.queryForList(
						"""
						SELECT id, item_id, total_fee, supplier_id
						FROM orders_normal_orders_items
						WHERE company_id=? AND order_id=?
						""",
						companyId,
						orderId);
		if (items.isEmpty()) {
			return false;
		}
		long supplierId = toLong(refund.get("supplier_id"));
		Map<Long, Map<String, Object>> byLineId = new LinkedHashMap<>();
		for (Map<String, Object> line : items) {
			if (toLong(line.get("supplier_id")) != supplierId) {
				continue;
			}
			long lid = toLong(line.get("id"));
			if (lid > 0) {
				byLineId.put(lid, line);
			}
		}
		if (byLineId.isEmpty()) {
			log.warn(
					"Shuyun refund.sync payload: no items for refund supplier companyId={} orderId={} supplierId={}",
					companyId,
					orderId,
					supplierId);
			return false;
		}

		String aftersalesBn = stringVal(refund.get("aftersales_bn"));
		List<Map<String, Object>> payloads =
				StringUtils.hasText(aftersalesBn) && !"0".equals(aftersalesBn)
						? buildFromAftersalesDetails(companyId, refund, order, shopId, aftersalesBn, byLineId)
						: buildFromCancelStyleRefund(companyId, refund, order, shopId, byLineId);
		if (payloads.isEmpty()) {
			return false;
		}
		// trade_source 非 PHP 行字段；Gateway 侧平台由 header 传递。与旧 Java 兼容：补到首行不强制。
		try {
			gatewayClient.postJson(
					companyId, ShuyunOpenPlatformGatewayActions.REFUND_SYNC, payloads, "offline");
			log.info("Shuyun refund_sync ok companyId={} refundBn={} lines={}", companyId, refundBn, payloads.size());
			return true;
		} catch (Exception e) {
			log.error(
					"Shuyun refund.sync failed companyId={} refundBn={} err={}",
					companyId,
					refundBn,
					e.getMessage());
			return false;
		}
	}

	private List<Map<String, Object>> buildFromAftersalesDetails(
			long companyId,
			Map<String, Object> refund,
			Map<String, Object> order,
			String shopId,
			String aftersalesBn,
			Map<Long, Map<String, Object>> byLineId) {
		List<Map<String, Object>> details =
				jdbcTemplate.queryForList(
						"""
						SELECT detail_id, sub_order_id, refund_fee, aftersales_type
						FROM aftersales_detail
						WHERE company_id=? AND aftersales_bn=?
						ORDER BY detail_id ASC
						""",
						companyId,
						aftersalesBn);
		if (details.isEmpty()) {
			log.warn(
					"Shuyun refund.sync payload: aftersales_detail empty companyId={} aftersalesBn={}",
					companyId,
					aftersalesBn);
			return List.of();
		}
		String refundBn = stringVal(refund.get("refund_bn"));
		String status = RefundStatusMapper.mapRefundStatus(stringVal(refund.get("refund_status")));
		int phase =
				RefundStatusMapper.resolveRefundPhase(
						stringVal(order.get("order_status")), refund.get("refund_type"));
		String created = formatTs(toInt(refund.get("create_time")));
		int updated = toInt(refund.get("update_time"));
		String modified = formatTs(updated > 0 ? updated : toInt(refund.get("create_time")));
		String reason = stringVal(refund.get("refunds_memo"));
		if (!StringUtils.hasText(reason)) {
			reason = "售后退款";
		}

		List<Candidate> candidates = new ArrayList<>();
		Map<Integer, Integer> weights = new LinkedHashMap<>();
		int idx = 0;
		for (Map<String, Object> detail : details) {
			long subId = toLong(detail.get("sub_order_id"));
			Map<String, Object> line = byLineId.get(subId);
			if (line == null) {
				continue;
			}
			long lineItemId = toLong(line.get("item_id"));
			long lineId = toLong(line.get("id"));
			if (lineItemId <= 0 || lineId <= 0) {
				continue;
			}
			candidates.add(
					new Candidate(
							detail,
							line,
							lineId,
							lineItemId,
							resolveProductId(companyId, lineItemId)));
			weights.put(idx, Math.max(0, toInt(detail.get("refund_fee"))));
			idx++;
		}
		if (candidates.isEmpty()) {
			return List.of();
		}
		int weightSum = weights.values().stream().mapToInt(Integer::intValue).sum();
		if (weightSum == 0) {
			weights.clear();
			for (int i = 0; i < candidates.size(); i++) {
				weights.put(i, Math.max(0, toInt(candidates.get(i).line().get("total_fee"))));
			}
		}
		Map<Object, Integer> allocated =
				RefundLineFeeAllocator.allocateProportional(resolveRefundAmountFen(refund), weights);

		List<Map<String, Object>> out = new ArrayList<>();
		for (int i = 0; i < candidates.size(); i++) {
			Candidate c = candidates.get(i);
			long detailId = toLong(c.detail().get("detail_id"));
			String rid = detailId > 0 ? refundBn + "_d" + detailId : refundBn + "_l" + c.lineId();
			int feeFen = allocated.getOrDefault(i, 0);
			String atype = stringVal(c.detail().get("aftersales_type"));
			if (!StringUtils.hasText(atype)) {
				atype = "ONLY_REFUND";
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("refund_id", rid);
			row.put("order_id", stringVal(order.get("order_id")));
			row.put("order_item_id", String.valueOf(c.lineId()));
			row.put("shop_id", shopId);
			row.put("product_id", c.productId());
			row.put("sku_id", String.valueOf(c.lineItemId()));
			row.put("refund_fee", TradeSyncService.fenToYuan(feeFen));
			row.put("refund_status", status);
			row.put("good_return", RefundStatusMapper.mapGoodReturnFromAftersalesDetailType(atype));
			row.put("refund_reason", reason);
			row.put("created", created);
			row.put("modified", modified);
			row.put("refund_phase", phase);
			out.add(row);
		}
		return out;
	}

	private List<Map<String, Object>> buildFromCancelStyleRefund(
			long companyId,
			Map<String, Object> refund,
			Map<String, Object> order,
			String shopId,
			Map<Long, Map<String, Object>> byLineId) {
		String refundBn = stringVal(refund.get("refund_bn"));
		Map<Long, Integer> weights = new LinkedHashMap<>();
		for (Map.Entry<Long, Map<String, Object>> e : byLineId.entrySet()) {
			weights.put(e.getKey(), toInt(e.getValue().get("total_fee")));
		}
		Map<Object, Integer> allocated =
				RefundLineFeeAllocator.allocateProportional(resolveRefundAmountFen(refund), weights);
		String status = RefundStatusMapper.mapRefundStatus(stringVal(refund.get("refund_status")));
		int phase =
				RefundStatusMapper.resolveRefundPhase(
						stringVal(order.get("order_status")), refund.get("refund_type"));
		String created = formatTs(toInt(refund.get("create_time")));
		int updated = toInt(refund.get("update_time"));
		String modified = formatTs(updated > 0 ? updated : toInt(refund.get("create_time")));
		String reason = stringVal(refund.get("refunds_memo"));
		if (!StringUtils.hasText(reason)) {
			reason = "订单取消退款";
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map.Entry<Long, Map<String, Object>> e : byLineId.entrySet()) {
			long lid = e.getKey();
			Map<String, Object> line = e.getValue();
			long lineItemId = toLong(line.get("item_id"));
			if (lineItemId <= 0) {
				continue;
			}
			int feeFen = allocated.getOrDefault(lid, 0);
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("refund_id", refundBn + "_l" + lid);
			row.put("order_id", stringVal(order.get("order_id")));
			row.put("order_item_id", String.valueOf(lid));
			row.put("shop_id", shopId);
			row.put("product_id", resolveProductId(companyId, lineItemId));
			row.put("sku_id", String.valueOf(lineItemId));
			row.put("refund_fee", TradeSyncService.fenToYuan(feeFen));
			row.put("refund_status", status);
			row.put("good_return", "SY_ONLY_FEE");
			row.put("refund_reason", reason);
			row.put("created", created);
			row.put("modified", modified);
			row.put("refund_phase", phase);
			out.add(row);
		}
		return out;
	}

	private static int resolveRefundAmountFen(Map<String, Object> refund) {
		int refunded = toInt(refund.get("refunded_fee"));
		if (refunded > 0) {
			return refunded;
		}
		return Math.max(0, toInt(refund.get("refund_fee")));
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
				toLong(row.get("goods_id")), toLong(row.get("default_item_id")), toLong(row.get("item_id")));
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
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(stringVal(v));
		} catch (Exception e) {
			return 0;
		}
	}

	private record Candidate(
			Map<String, Object> detail,
			Map<String, Object> line,
			long lineId,
			long lineItemId,
			String productId) {}
}
