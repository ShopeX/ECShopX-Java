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

package cn.shopex.ecshopx.orders.service.invoice.export;

import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public final class InvoiceExportSupplierOrderQuerySupport {

	private InvoiceExportSupplierOrderQuerySupport() {
	}

	public static LambdaQueryWrapper<SupplierOrder> toCountWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		return baseWrapper(companyId, filter);
	}

	public static LambdaQueryWrapper<SupplierOrder> toListWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<SupplierOrder> w = baseWrapper(companyId, filter);
		w.orderByDesc(SupplierOrder::getCreateTime);
		return w;
	}

	private static LambdaQueryWrapper<SupplierOrder> baseWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<SupplierOrder> w = new LambdaQueryWrapper<>();
		w.eq(SupplierOrder::getCompanyId, companyId);
		Object supId = filter.get("supplier_id");
		if (supId != null) {
			w.eq(SupplierOrder::getSupplierId, (int) longVal(supId));
		}

		applyCreateTimeRange(w, filter);

		Object orderStatusIn = filter.get("order_status|in");
		if (orderStatusIn instanceof Collection<?> osc && !osc.isEmpty()) {
			List<String> vals = osc.stream().map(String::valueOf).toList();
			w.in(SupplierOrder::getOrderStatus, vals);
		} else {
			putEqIfPresent(w, filter, "order_status", SupplierOrder::getOrderStatus);
		}
		putEqIfPresent(w, filter, "cancel_status", SupplierOrder::getCancelStatus);
		putEqIfPresent(w, filter, "receipt_type", SupplierOrder::getReceiptType);
		putEqIfPresent(w, filter, "ziti_status", SupplierOrder::getZitiStatus);
		putEqIfPresent(w, filter, "delivery_status", SupplierOrder::getDeliveryStatus);

		Object cancelIn = filter.get("cancel_status|in");
		if (cancelIn instanceof Collection<?> c && !c.isEmpty()) {
			w.in(SupplierOrder::getCancelStatus, c.stream().map(String::valueOf).toList());
		}

		Object deliveryIn = filter.get("delivery_status|in");
		if (deliveryIn instanceof Collection<?> c && !c.isEmpty()) {
			w.in(SupplierOrder::getDeliveryStatus, c.stream().map(String::valueOf).toList());
		}

		Object isInvoiced = filter.get("is_invoiced");
		if (isInvoiced != null && StringUtils.hasText(String.valueOf(isInvoiced).trim())) {
			w.eq(SupplierOrder::getIsInvoiced, intVal(isInvoiced) != 0);
		}

		if (truthy(filter.get("invoice_not_empty"))) {
			w.isNotNull(SupplierOrder::getInvoice);
			w.ne(SupplierOrder::getInvoice, "");
		}

		putEqIfPresent(w, filter, "pay_type", SupplierOrder::getPayType);
		Object oidEq = filter.get("order_id");
		if (oidEq != null && StringUtils.hasText(String.valueOf(oidEq))) {
			w.eq(SupplierOrder::getOrderId, longVal(oidEq));
		}
		Object mobile = filter.get("mobile");
		if (mobile != null && StringUtils.hasText(String.valueOf(mobile).trim())) {
			w.eq(SupplierOrder::getReceiverMobile, String.valueOf(mobile).trim());
		}
		Object uid = filter.get("user_id");
		if (uid != null && StringUtils.hasText(String.valueOf(uid).trim())) {
			w.eq(SupplierOrder::getUserId, longVal(uid));
		}
		Object sid = filter.get("source_id");
		if (sid != null && StringUtils.hasText(String.valueOf(sid).trim())) {
			w.eq(SupplierOrder::getSourceId, longVal(sid));
		}
		String orderClass = str(filter.get("order_class"));
		if (StringUtils.hasText(orderClass)) {
			w.eq(SupplierOrder::getOrderClass, orderClass);
		}

		applyInvoiceExists(w, companyId, filter);
		return w;
	}

	private static void applyCreateTimeRange(LambdaQueryWrapper<SupplierOrder> w, LinkedHashMap<String, Object> filter) {
		Object gte = filter.get("create_time|gte");
		Object lte = filter.get("create_time|lte");
		if (gte != null) {
			if (isEpochLike(gte)) {
				w.ge(SupplierOrder::getCreateTime, (int) longVal(gte));
			} else {
				w.apply("supplier_order.create_time >= {0}", String.valueOf(gte));
			}
		}
		if (lte != null) {
			if (isEpochLike(lte)) {
				w.le(SupplierOrder::getCreateTime, (int) longVal(lte));
			} else {
				w.apply("supplier_order.create_time <= {0}", String.valueOf(lte));
			}
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

	private static void applyInvoiceExists(
			LambdaQueryWrapper<SupplierOrder> w, long companyId, LinkedHashMap<String, Object> filter) {
		Object raw = filter.get("invoice_status|in");
		if (!(raw instanceof Collection<?> c) || c.isEmpty()) {
			return;
		}
		List<String> statuses =
				c.stream().map(String::valueOf).map(String::trim).filter(StringUtils::hasText).toList();
		if (statuses.isEmpty()) {
			return;
		}
		String inList =
				statuses.stream()
						.map(s -> "'" + s.replace("'", "''") + "'")
						.collect(Collectors.joining(","));
		w.apply(
				"EXISTS (SELECT 1 FROM orders_invoice inv WHERE inv.company_id = {0} AND inv.invoice_status IN ("
						+ inList
						+ ") AND FIND_IN_SET(CAST(supplier_order.order_id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci, inv.order_id COLLATE utf8mb4_unicode_ci) > 0)",
				companyId);
	}

	private static void putEqIfPresent(
			LambdaQueryWrapper<SupplierOrder> w,
			LinkedHashMap<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<SupplierOrder, ?> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if ("user_id".equals(key) || "shop_id".equals(key) || "source_id".equals(key) || "distributor_id".equals(key)) {
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
