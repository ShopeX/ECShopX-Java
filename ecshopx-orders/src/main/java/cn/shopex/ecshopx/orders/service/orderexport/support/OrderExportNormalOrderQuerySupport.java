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

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.util.StringUtils;

public final class OrderExportNormalOrderQuerySupport {

	private OrderExportNormalOrderQuerySupport() {
	}

	public static LambdaQueryWrapper<NormalOrders> toCountWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		return baseWrapper(companyId, filter);
	}

	public static LambdaQueryWrapper<NormalOrders> toListWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<NormalOrders> w = baseWrapper(companyId, filter);
		w.orderByDesc(NormalOrders::getCreateTime);
		return w;
	}

	private static LambdaQueryWrapper<NormalOrders> baseWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		filter.remove("order_type");
		w.eq(NormalOrders::getCompanyId, companyId);
		w.eq(NormalOrders::getOrderType, "normal");

		Object orderClassNotIn = filter.get("order_class|notin");
		if (orderClassNotIn instanceof Collection<?> nc && !nc.isEmpty()) {
			List<String> vals =
					nc.stream().map(String::valueOf).map(String::trim).filter(StringUtils::hasText).toList();
			if (!vals.isEmpty()) {
				w.notIn(NormalOrders::getOrderClass, vals);
			}
		}

		String orderClass = str(filter.get("order_class"));
		if (StringUtils.hasText(orderClass)) {
			w.eq(NormalOrders::getOrderClass, orderClass);
		}

		Object merchantId = filter.get("merchant_id");
		if (merchantId != null && StringUtils.hasText(String.valueOf(merchantId))) {
			w.eq(NormalOrders::getMerchantId, longVal(merchantId));
		}

		Object supplierId = filter.get("supplier_id");
		if (supplierId != null && StringUtils.hasText(String.valueOf(supplierId).trim())) {
			w.eq(NormalOrders::getSupplierId, intVal(supplierId));
		}

		applyCreateTimeRange(w, filter);
		applyDeliveryTimeRange(w, filter);

		Object orderStatusIn = filter.get("order_status|in");
		if (orderStatusIn instanceof Collection<?> osc && !osc.isEmpty()) {
			List<String> vals = osc.stream().map(String::valueOf).toList();
			w.in(NormalOrders::getOrderStatus, vals);
		} else {
			putEqIfPresent(w, filter, "order_status", NormalOrders::getOrderStatus);
		}
		putEqIfPresent(w, filter, "cancel_status", NormalOrders::getCancelStatus);
		putEqIfPresent(w, filter, "receipt_type", NormalOrders::getReceiptType);
		putEqIfPresent(w, filter, "ziti_status", NormalOrders::getZitiStatus);
		putEqIfPresent(w, filter, "delivery_status", NormalOrders::getDeliveryStatus);
		putEqIfPresent(w, filter, "receiver_name", NormalOrders::getReceiverName);
		putEqIfPresent(w, filter, "mobile", NormalOrders::getMobile);
		putEqIfPresent(w, filter, "user_id", NormalOrders::getUserId);
		putEqIfPresent(w, filter, "source_id", NormalOrders::getSourceId);
		putEqIfPresent(w, filter, "distributor_id", NormalOrders::getDistributorId);
		applyShopIdConstraint(w, filter);

		Object distributorIn = filter.get("distributor_id|in");
		if (distributorIn instanceof Collection<?> dc && !dc.isEmpty()) {
			List<Long> ids =
					dc.stream().map(OrderExportNormalOrderQuerySupport::longVal).filter(id -> id > 0L).distinct().toList();
			if (!ids.isEmpty()) {
				w.in(NormalOrders::getDistributorId, ids);
			}
		}

		Object isInvoiced = filter.get("is_invoiced");
		if (isInvoiced != null && StringUtils.hasText(String.valueOf(isInvoiced).trim())) {
			w.eq(NormalOrders::getIsInvoiced, intVal(isInvoiced) != 0);
		}

		Object cancelIn = filter.get("cancel_status|in");
		if (cancelIn instanceof Collection<?> c && !c.isEmpty()) {
			List<String> vals = c.stream().map(String::valueOf).toList();
			w.in(NormalOrders::getCancelStatus, vals);
		}

		Object deliveryIn = filter.get("delivery_status|in");
		if (deliveryIn instanceof Collection<?> c && !c.isEmpty()) {
			List<String> vals = c.stream().map(String::valueOf).toList();
			w.in(NormalOrders::getDeliveryStatus, vals);
		}

		if (truthy(filter.get("invoice_not_empty"))) {
			w.isNotNull(NormalOrders::getInvoice);
			w.ne(NormalOrders::getInvoice, "");
		}

		Object oidLike = filter.get("order_id|like");
		if (oidLike != null && StringUtils.hasText(String.valueOf(oidLike))) {
			String pat = "%" + String.valueOf(oidLike).trim() + "%";
			w.apply("CAST(orders_normal_orders.order_id AS CHAR) LIKE {0}", pat);
		} else {
			Object oidIn = filter.get("order_id|in");
			if (oidIn instanceof Collection<?> oc && !oc.isEmpty()) {
				List<Long> ids =
						oc.stream()
								.map(OrderExportNormalOrderQuerySupport::longVal)
								.filter(id -> id > 0L)
								.distinct()
								.toList();
				if (!ids.isEmpty()) {
					w.in(NormalOrders::getOrderId, ids);
				}
			} else {
				Object oidEq = filter.get("order_id");
				if (oidEq != null && StringUtils.hasText(String.valueOf(oidEq))) {
					w.eq(NormalOrders::getOrderId, longVal(oidEq));
				}
			}
		}

		Object isRate = filter.get("is_rate");
		if (isRate != null && StringUtils.hasText(String.valueOf(isRate).trim())) {
			w.eq(NormalOrders::getIsRate, intVal(isRate) != 0);
		}

		Object titleLike = filter.get("title|like");
		if (titleLike != null && StringUtils.hasText(String.valueOf(titleLike))) {
			w.like(NormalOrders::getTitle, String.valueOf(titleLike).trim());
		}

		Object itemName = filter.get("item_name");
		if (itemName != null && StringUtils.hasText(String.valueOf(itemName))) {
			String like = "%" + String.valueOf(itemName).trim() + "%";
			w.apply(
					"EXISTS (SELECT 1 FROM orders_normal_orders_items i WHERE i.order_id = orders_normal_orders.order_id AND i.company_id = {0} AND i.item_name LIKE {1})",
					companyId,
					like);
		}

		Object actIds = filter.get("act_id");
		if (actIds instanceof Collection<?> c && !c.isEmpty()) {
			List<Long> ids = c.stream().map(OrderExportNormalOrderQuerySupport::longVal).distinct().toList();
			w.in(NormalOrders::getActId, ids);
		} else if (actIds != null && StringUtils.hasText(String.valueOf(actIds))) {
			w.eq(NormalOrders::getActId, longVal(actIds));
		}

		Object salesmanId = filter.get("salesman_id");
		if (salesmanId != null) {
			w.eq(NormalOrders::getSalesmanId, longVal(salesmanId));
		}

		putEqIfPresent(w, filter, "pay_type", NormalOrders::getPayType);

		if (filter.containsKey("subdistrict_parent_id")) {
			w.eq(NormalOrders::getSubdistrictParentId, longVal(filter.get("subdistrict_parent_id")));
		}
		if (filter.containsKey("subdistrict_id")) {
			w.eq(NormalOrders::getSubdistrictId, longVal(filter.get("subdistrict_id")));
		}

		applyPromoterConstraints(w, companyId, filter);

		Object tradeId = filter.get("trade_id");
		if (tradeId != null && StringUtils.hasText(String.valueOf(tradeId).trim())) {
			String v = String.valueOf(tradeId).trim();
			w.apply(
					"EXISTS (SELECT 1 FROM trade t WHERE t.company_id = {0} AND t.trade_id = {1} AND t.order_id = CAST(orders_normal_orders.order_id AS CHAR))",
					String.valueOf(companyId),
					v);
		}

		return w;
	}

	private static void applyShopIdConstraint(LambdaQueryWrapper<NormalOrders> w, LinkedHashMap<String, Object> filter) {
		Object shopIn = filter.get("shop_id|in");
		if (shopIn instanceof Collection<?> sc && !sc.isEmpty()) {
			List<Long> ids =
					sc.stream().map(OrderExportNormalOrderQuerySupport::longVal).filter(id -> id > 0L).distinct().toList();
			if (!ids.isEmpty()) {
				w.in(NormalOrders::getShopId, ids);
				return;
			}
		}
		Object shopRaw = filter.get("shop_id");
		if (shopRaw instanceof Collection<?> sc2 && !sc2.isEmpty()) {
			List<Long> ids =
					sc2.stream().map(OrderExportNormalOrderQuerySupport::longVal).filter(id -> id > 0L).distinct().toList();
			if (!ids.isEmpty()) {
				w.in(NormalOrders::getShopId, ids);
				return;
			}
		}
		putEqIfPresent(w, filter, "shop_id", NormalOrders::getShopId);
	}

	private static void applyPromoterConstraints(
			LambdaQueryWrapper<NormalOrders> w, long companyId, LinkedHashMap<String, Object> filter) {
		String pm = str(filter.get("promoter_mobile")).trim();
		String pi = str(filter.get("promoter_identity")).trim();
		if (!StringUtils.hasText(pm) && !StringUtils.hasText(pi)) {
			return;
		}
		if (StringUtils.hasText(pm)) {
			w.apply(
					"EXISTS (SELECT 1 FROM popularize_promoter pp INNER JOIN members_info mi ON mi.company_id = pp.company_id AND mi.user_id = pp.user_id WHERE pp.company_id = {0} AND pp.user_id = orders_normal_orders.user_id AND pp.is_promoter = 1 AND mi.mobile = {1})",
					companyId,
					pm);
		}
		if (StringUtils.hasText(pi)) {
			String like = "%" + pi.trim() + "%";
			w.apply(
					"EXISTS (SELECT 1 FROM popularize_promoter pp INNER JOIN popularize_promoter_identity pid ON pid.id = pp.identity_id AND pid.company_id = pp.company_id WHERE pp.company_id = {0} AND pp.user_id = orders_normal_orders.user_id AND pp.is_promoter = 1 AND pid.name LIKE {1})",
					companyId,
					like);
		}
	}

	private static void applyCreateTimeRange(LambdaQueryWrapper<NormalOrders> w, LinkedHashMap<String, Object> filter) {
		Object gte = filter.get("create_time|gte");
		Object lte = filter.get("create_time|lte");
		if (gte != null) {
			if (isEpochLike(gte)) {
				w.ge(NormalOrders::getCreateTime, (int) longVal(gte));
			} else {
				w.apply("orders_normal_orders.create_time >= {0}", String.valueOf(gte));
			}
		}
		if (lte != null) {
			if (isEpochLike(lte)) {
				w.le(NormalOrders::getCreateTime, (int) longVal(lte));
			} else {
				w.apply("orders_normal_orders.create_time <= {0}", String.valueOf(lte));
			}
		}
	}

	private static void applyDeliveryTimeRange(LambdaQueryWrapper<NormalOrders> w, LinkedHashMap<String, Object> filter) {
		Object gte = filter.get("delivery_time|gte");
		Object lte = filter.get("delivery_time|lte");
		if (gte != null) {
			w.ge(NormalOrders::getDeliveryTime, (int) longVal(gte));
		}
		if (lte != null) {
			w.le(NormalOrders::getDeliveryTime, (int) longVal(lte));
		}
	}

	private static boolean isEpochLike(Object v) {
		if (v instanceof Number) {
			return true;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		try {
			Long.parseLong(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static void putEqIfPresent(
			LambdaQueryWrapper<NormalOrders> w,
			LinkedHashMap<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<NormalOrders, ?> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		if ("distributor_id".equals(key)) {
			w.eq(col, longVal(v));
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if ("user_id".equals(key) || "shop_id".equals(key) || "source_id".equals(key)) {
			w.eq(col, longVal(v));
			return;
		}
		w.eq(col, s);
	}

	private static boolean truthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return "true".equalsIgnoreCase(String.valueOf(o).trim()) || "1".equals(String.valueOf(o).trim());
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
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
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
