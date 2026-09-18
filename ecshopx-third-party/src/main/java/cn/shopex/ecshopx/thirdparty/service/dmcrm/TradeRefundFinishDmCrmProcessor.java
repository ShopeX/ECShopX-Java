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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeRefundFinishDmCrmProcessor {

	private static final Logger log = LoggerFactory.getLogger(TradeRefundFinishDmCrmProcessor.class);

	private static final String SQL_ORDER =
			"SELECT order_id, company_id, dm_point_preid, point_fee, point_use, order_class, salesman_id,"
					+ " sale_salesman_distributor_id, user_id, receiver_name, receiver_mobile, receiver_zip,"
					+ " receiver_address, create_time, freight_fee, item_fee, total_fee, remark"
					+ " FROM orders_normal_orders WHERE company_id = ? AND order_id = ? LIMIT 1";

	private static final String SQL_ORDER_ITEMS =
			"SELECT item_id, goods_id, item_bn, goods_bn, item_name, num, item_fee, total_fee, market_price, price,"
					+ " order_item_type"
					+ " FROM orders_normal_orders_items WHERE company_id = ? AND order_id = ? ORDER BY id ASC";

	private static final String SQL_AFTERSALES_TYPE =
			"SELECT aftersales_type FROM aftersales WHERE company_id = ? AND aftersales_bn = ? LIMIT 1";

	private static final String SQL_AFTERSALES_DETAILS =
			"SELECT item_id, num, refund_fee, refund_point, item_bn, item_name"
					+ " FROM aftersales_detail WHERE company_id = ? AND aftersales_bn = ?";

	private static final String SQL_SHOPPING_GUIDE =
			"SELECT number, name FROM shop_salesperson WHERE company_id = ? AND salesperson_id = ?"
					+ " AND salesperson_type = 'shopping_guide' LIMIT 1";

	private static final String SQL_DISTRIBUTOR_STORE =
			"SELECT shop_code, name FROM distribution_distributor WHERE company_id = ? AND distributor_id = ? LIMIT 1";

	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final JdbcTemplate jdbcTemplate;
	private final DmCrmTradeRefundFinishOrderSyncPort dmCrmTradeRefundFinishOrderSyncPort;

	public TradeRefundFinishDmCrmProcessor(
			DmCrmSettingReadPort dmCrmSettingReadPort,
			JdbcTemplate jdbcTemplate,
			DmCrmTradeRefundFinishOrderSyncPort dmCrmTradeRefundFinishOrderSyncPort) {
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.jdbcTemplate = jdbcTemplate;
		this.dmCrmTradeRefundFinishOrderSyncPort = dmCrmTradeRefundFinishOrderSyncPort;
	}

	public void handle(Map<String, Object> refundEntities) {
		try {
			handleInner(refundEntities);
		} catch (Exception e) {
			log.debug("trade refund finish dm crm swallowed: {}", e.toString());
		}
	}

	private void handleInner(Map<String, Object> refundEntities) {
		if (refundEntities == null || refundEntities.isEmpty()) {
			return;
		}
		log.debug("trade refund finish dm crm handle companyId={} orderId={}", refundEntities.get("company_id"), refundEntities.get("order_id"));
		long companyId = longVal(refundEntities.get("company_id"));
		long orderId = longVal(refundEntities.get("order_id"));
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		if (!dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
			return;
		}
		Optional<Map<String, Object>> orderOpt = loadOrder(companyId, orderId);
		if (orderOpt.isEmpty()) {
			return;
		}
		Map<String, Object> orderRow = orderOpt.get();
		if (shouldSkipForPointSignals(orderRow)) {
			return;
		}
		List<Map<String, Object>> orderItems = loadOrderItems(companyId, orderId);
		Map<Long, Map<String, Object>> itemById = indexByItemId(orderItems);

		enrichClerkAndStore(companyId, orderRow);

		int usedMemberPoints = -intOrZero(refundEntities.get("refund_point"));
		Object refundBn = refundEntities.get("refund_bn");
		int returnFreight = intOrZero(refundEntities.get("return_freight"));
		int freightFromRefund = intOrZero(refundEntities.get("freight"));

		String ruid = String.valueOf(orderId);

		if (hasMeaningfulAftersalesBn(refundEntities.get("aftersales_bn"))) {
			long aftersalesBn = longVal(refundEntities.get("aftersales_bn"));
			String aftersalesType = loadAftersalesType(companyId, aftersalesBn).orElse("");
			List<Map<String, Object>> details = loadAftersalesDetails(companyId, aftersalesBn);
			Set<Long> detailItemIds = new LinkedHashSet<>();
			for (Map<String, Object> d : details) {
				detailItemIds.add(longVal(d.get("item_id")));
			}
			List<Map<String, Object>> mergedItems = new ArrayList<>();
			for (long itemId : detailItemIds) {
				Map<String, Object> oi = itemById.get(itemId);
				if (oi == null) {
					continue;
				}
				Optional<Map<String, Object>> detailOpt =
						details.stream().filter(x -> longVal(x.get("item_id")) == itemId).findFirst();
				if (detailOpt.isEmpty()) {
					continue;
				}
				mergedItems.add(mergeAftersalesLine(orderRow, oi, detailOpt.get()));
			}
			if (mergedItems.isEmpty()) {
				return;
			}
			int freightFee = intOrZero(orderRow.get("freight_fee"));
			if (returnFreight == 1) {
				freightFee = -freightFromRefund;
			}
			int totalFee = intOrZero(refundEntities.get("refunded_fee"));
			if (totalFee == 0) {
				totalFee = intOrZero(refundEntities.get("refund_fee"));
			}
			Map<String, Object> body = baseBody(orderRow, refundEntities, refundBn, mergedItems, freightFee, totalFee, usedMemberPoints);
			if ("REFUND_GOODS".equals(aftersalesType)) {
				body.put("orderStatus", 2);
				dmCrmTradeRefundFinishOrderSyncPort.syncAfter(companyId, ruid, body);
			} else {
				body.put("orderStatus", 5);
				dmCrmTradeRefundFinishOrderSyncPort.syncForwardAfter(companyId, ruid, body);
			}
			return;
		}

		// Presales / cancel-refund: no aftersales_bn — forward sync with negated lines
		if (orderItems.isEmpty()) {
			return;
		}
		List<Map<String, Object>> presaleItems = new ArrayList<>();
		String orderClass = str(orderRow.get("order_class"));
		for (Map<String, Object> oi : orderItems) {
			presaleItems.add(transformPresalesLine(oi, orderClass));
		}
		int freightFee = intOrZero(orderRow.get("freight_fee"));
		int totalFee = intOrZero(refundEntities.get("refunded_fee"));
		if (totalFee == 0) {
			totalFee = intOrZero(refundEntities.get("refund_fee"));
		}
		Map<String, Object> body = baseBody(orderRow, refundEntities, refundBn, presaleItems, freightFee, totalFee, usedMemberPoints);
		body.put("orderStatus", 5);
		dmCrmTradeRefundFinishOrderSyncPort.syncForwardAfter(companyId, ruid, body);
	}

	private Map<String, Object> baseBody(
			Map<String, Object> orderRow,
			Map<String, Object> refundEntities,
			Object refundBn,
			List<Map<String, Object>> items,
			int freightFee,
			int totalFee,
			int usedMemberPoints) {
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("order_id", longVal(orderRow.get("order_id")));
		body.put("company_id", longVal(orderRow.get("company_id")));
		body.put("user_id", longVal(orderRow.get("user_id")));
		body.put("refund_bn", refundBn == null ? "" : String.valueOf(refundBn));
		body.put("receiver_name", str(orderRow.get("receiver_name")));
		body.put("receiver_mobile", str(orderRow.get("receiver_mobile")));
		body.put("receiver_zip", str(orderRow.get("receiver_zip")));
		body.put("receiver_address", str(orderRow.get("receiver_address")));
		body.put("create_time", orderRow.get("create_time"));
		body.put("remark", str(orderRow.get("remark")));
		body.put("items", items);
		body.put("freight_fee", freightFee);
		body.put("total_fee", totalFee);
		body.put("usedMemberPoints", usedMemberPoints);
		if (StringUtils.hasText(str(orderRow.get("clerkCode")))) {
			body.put("clerkCode", str(orderRow.get("clerkCode")));
		}
		if (StringUtils.hasText(str(orderRow.get("clerkName")))) {
			body.put("clerkName", str(orderRow.get("clerkName")));
		}
		if (StringUtils.hasText(str(orderRow.get("storeCode")))) {
			body.put("storeCode", str(orderRow.get("storeCode")));
		}
		if (StringUtils.hasText(str(orderRow.get("storeName")))) {
			body.put("storeName", str(orderRow.get("storeName")));
		}
		body.put("refund_success_time", str(refundEntities.get("refund_success_time")));
		return body;
	}

	private static Map<String, Object> mergeAftersalesLine(
			Map<String, Object> orderRow, Map<String, Object> oi, Map<String, Object> detail) {
		LinkedHashMap<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", oi.get("item_id"));
		line.put("goods_bn", str(oi.get("goods_bn")));
		line.put("item_bn", str(oi.get("item_bn")));
		line.put("item_name", StringUtils.hasText(str(detail.get("item_name"))) ? str(detail.get("item_name")) : str(oi.get("item_name")));
		int dNum = intOrZero(detail.get("num"));
		line.put("num", dNum);
		int refundFee = intOrZero(detail.get("refund_fee"));
		line.put("item_fee", refundFee);
		line.put("item_fee_t", refundFee);
		line.put("total_fee", refundFee);
		boolean pointsmall = "pointsmall".equalsIgnoreCase(str(orderRow.get("order_class")));
		boolean gift = pointsmall || "gift".equalsIgnoreCase(str(oi.get("order_item_type")));
		line.put("is_gift", gift ? 1 : 0);
		return line;
	}

	private static Map<String, Object> transformPresalesLine(Map<String, Object> oi, String orderClass) {
		LinkedHashMap<String, Object> line = new LinkedHashMap<>(oi);
		int itemFee = negateFee(intOrZero(oi.get("item_fee")));
		int total = negateFee(intOrZero(oi.get("total_fee")));
		line.put("item_fee", itemFee);
		line.put("item_fee_t", itemFee);
		line.put("total_fee", total);
		boolean pointsmall = "pointsmall".equalsIgnoreCase(orderClass);
		boolean gift = pointsmall || "gift".equalsIgnoreCase(str(oi.get("order_item_type")));
		line.put("is_gift", gift ? 1 : 0);
		return line;
	}

	private static int negateFee(int cents) {
		if (cents == 0) {
			return 0;
		}
		return -Math.abs(cents);
	}

	private void enrichClerkAndStore(long companyId, Map<String, Object> orderRow) {
		long salesmanId = longVal(orderRow.get("salesman_id"));
		if (salesmanId > 0L) {
			try {
				Map<String, Object> row =
						jdbcTemplate.queryForObject(
								SQL_SHOPPING_GUIDE,
								(rs, rn) -> {
									LinkedHashMap<String, Object> m = new LinkedHashMap<>();
									m.put("clerkCode", str(rs.getObject("number")));
									m.put("clerkName", str(rs.getObject("name")));
									return m;
								},
								companyId,
								salesmanId);
				orderRow.put("clerkCode", row.get("clerkCode"));
				orderRow.put("clerkName", row.get("clerkName"));
			} catch (EmptyResultDataAccessException ignored) {
				// optional enrichment
			}
		}
		long distId = longVal(orderRow.get("sale_salesman_distributor_id"));
		if (distId > 0L) {
			try {
				Map<String, Object> row =
						jdbcTemplate.queryForObject(
								SQL_DISTRIBUTOR_STORE,
								(rs, rn) -> {
									LinkedHashMap<String, Object> m = new LinkedHashMap<>();
									m.put("storeCode", str(rs.getObject("shop_code")));
									m.put("storeName", str(rs.getObject("name")));
									return m;
								},
								companyId,
								distId);
				orderRow.put("storeCode", row.get("storeCode"));
				orderRow.put("storeName", row.get("storeName"));
			} catch (EmptyResultDataAccessException ignored) {
				// optional enrichment
			}
		}
	}

	private Optional<Map<String, Object>> loadOrder(long companyId, long orderId) {
		try {
			Map<String, Object> row =
					jdbcTemplate.queryForObject(SQL_ORDER, (rs, rn) -> mapOrderRow(rs), companyId, orderId);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private static Map<String, Object> mapOrderRow(ResultSet rs) throws SQLException {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", rs.getObject("order_id"));
		m.put("company_id", rs.getObject("company_id"));
		m.put("dm_point_preid", rs.getObject("dm_point_preid"));
		m.put("point_fee", rs.getObject("point_fee"));
		m.put("point_use", rs.getObject("point_use"));
		m.put("order_class", rs.getObject("order_class"));
		m.put("salesman_id", rs.getObject("salesman_id"));
		m.put("sale_salesman_distributor_id", rs.getObject("sale_salesman_distributor_id"));
		m.put("user_id", rs.getObject("user_id"));
		m.put("receiver_name", rs.getObject("receiver_name"));
		m.put("receiver_mobile", rs.getObject("receiver_mobile"));
		m.put("receiver_zip", rs.getObject("receiver_zip"));
		m.put("receiver_address", rs.getObject("receiver_address"));
		m.put("create_time", rs.getObject("create_time"));
		m.put("freight_fee", rs.getObject("freight_fee"));
		m.put("item_fee", rs.getObject("item_fee"));
		m.put("total_fee", rs.getObject("total_fee"));
		m.put("remark", rs.getObject("remark"));
		return m;
	}

	private List<Map<String, Object>> loadOrderItems(long companyId, long orderId) {
		return jdbcTemplate.query(
				SQL_ORDER_ITEMS,
				(rs, rn) -> {
					LinkedHashMap<String, Object> m = new LinkedHashMap<>();
					m.put("item_id", rs.getObject("item_id"));
					m.put("goods_id", rs.getObject("goods_id"));
					m.put("item_bn", rs.getObject("item_bn"));
					m.put("goods_bn", rs.getObject("goods_bn"));
					m.put("item_name", rs.getObject("item_name"));
					m.put("num", rs.getObject("num"));
					m.put("item_fee", rs.getObject("item_fee"));
					m.put("total_fee", rs.getObject("total_fee"));
					m.put("market_price", rs.getObject("market_price"));
					m.put("price", rs.getObject("price"));
					m.put("order_item_type", rs.getObject("order_item_type"));
					return m;
				},
				companyId,
				orderId);
	}

	private Optional<String> loadAftersalesType(long companyId, long aftersalesBn) {
		try {
			String t =
					jdbcTemplate.queryForObject(
							SQL_AFTERSALES_TYPE, (rs, rn) -> str(rs.getObject("aftersales_type")), companyId, aftersalesBn);
			return Optional.ofNullable(t);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private List<Map<String, Object>> loadAftersalesDetails(long companyId, long aftersalesBn) {
		return jdbcTemplate.query(
				SQL_AFTERSALES_DETAILS,
				(rs, rn) -> {
					LinkedHashMap<String, Object> m = new LinkedHashMap<>();
					m.put("item_id", rs.getObject("item_id"));
					m.put("num", rs.getObject("num"));
					m.put("refund_fee", rs.getObject("refund_fee"));
					m.put("refund_point", rs.getObject("refund_point"));
					m.put("item_bn", rs.getObject("item_bn"));
					m.put("item_name", rs.getObject("item_name"));
					return m;
				},
				companyId,
				aftersalesBn);
	}

	private static Map<Long, Map<String, Object>> indexByItemId(List<Map<String, Object>> orderItems) {
		Map<Long, Map<String, Object>> map = new LinkedHashMap<>();
		for (Map<String, Object> oi : orderItems) {
			map.put(longVal(oi.get("item_id")), oi);
		}
		return map;
	}

	private static boolean shouldSkipForPointSignals(Map<String, Object> orderRow) {
		Object pre = orderRow.get("dm_point_preid");
		boolean preBlank = pre == null || !StringUtils.hasText(String.valueOf(pre).trim());
		int pointFee = intOrZero(orderRow.get("point_fee"));
		int pointUse = intOrZero(orderRow.get("point_use"));
		return preBlank && (pointFee > 0 || pointUse > 0);
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

	private static int intOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return new BigDecimal(String.valueOf(o).trim()).setScale(0, RoundingMode.DOWN).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
