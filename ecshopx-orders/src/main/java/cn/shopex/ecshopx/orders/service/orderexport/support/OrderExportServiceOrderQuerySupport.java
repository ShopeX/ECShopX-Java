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

import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.util.StringUtils;

public final class OrderExportServiceOrderQuerySupport {

	private OrderExportServiceOrderQuerySupport() {
	}

	public static LambdaQueryWrapper<ServiceOrders> toCountWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		return baseWrapper(companyId, filter);
	}

	public static LambdaQueryWrapper<ServiceOrders> toListWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<ServiceOrders> w = baseWrapper(companyId, filter);
		w.orderByDesc(ServiceOrders::getCreateTime);
		return w;
	}

	private static LambdaQueryWrapper<ServiceOrders> baseWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<ServiceOrders> w = new LambdaQueryWrapper<>();
		w.eq(ServiceOrders::getCompanyId, companyId);
		w.eq(ServiceOrders::getOrderType, "service");

		applyIntTimeRange(w, filter, "create_time", ServiceOrders::getCreateTime);

		Object orderStatusIn = filter.get("order_status|in");
		if (orderStatusIn instanceof Collection<?> osc && !osc.isEmpty()) {
			w.in(ServiceOrders::getOrderStatus, osc.stream().map(String::valueOf).toList());
		} else {
			putEqIfPresent(w, filter, "order_status", ServiceOrders::getOrderStatus);
		}
		putEqIfPresent(w, filter, "user_id", ServiceOrders::getUserId);
		applyShopId(w, filter);
		putEqIfPresent(w, filter, "mobile", ServiceOrders::getMobile);
		Object svcSmIn = filter.get("salesman_id|in");
		if (svcSmIn instanceof Collection<?> smc && !smc.isEmpty()) {
			List<Long> smIds = smc.stream().map(OrderExportServiceOrderQuerySupport::longVal).toList();
			w.in(ServiceOrders::getSalesmanId, smIds);
		} else {
			Object sm = filter.get("salesman_id");
			if (sm != null) {
				w.eq(ServiceOrders::getSalesmanId, longVal(sm));
			}
		}
		Object titleLike = filter.get("title|like");
		if (titleLike != null && StringUtils.hasText(String.valueOf(titleLike))) {
			w.like(ServiceOrders::getTitle, String.valueOf(titleLike).trim());
		}
		Object oidLike = filter.get("order_id|like");
		if (oidLike != null && StringUtils.hasText(String.valueOf(oidLike))) {
			String pat = "%" + String.valueOf(oidLike).trim() + "%";
			w.apply("CAST(service_orders.order_id AS CHAR) LIKE {0}", pat);
		} else {
			Object oidIn = filter.get("order_id|in");
			if (oidIn instanceof Collection<?> oc && !oc.isEmpty()) {
				List<Long> ids =
						oc.stream()
								.map(OrderExportServiceOrderQuerySupport::longVal)
								.filter(id -> id > 0L)
								.distinct()
								.toList();
				if (!ids.isEmpty()) {
					w.in(ServiceOrders::getOrderId, ids);
				}
			} else {
				Object oidEq = filter.get("order_id");
				if (oidEq != null && StringUtils.hasText(String.valueOf(oidEq))) {
					w.eq(ServiceOrders::getOrderId, longVal(oidEq));
				}
			}
		}
		putEqIfPresent(w, filter, "source_id", ServiceOrders::getSourceId);
		return w;
	}

	private static void applyShopId(LambdaQueryWrapper<ServiceOrders> w, LinkedHashMap<String, Object> filter) {
		Object shopRaw = filter.get("shop_id");
		if (shopRaw instanceof Collection<?> sc && !sc.isEmpty()) {
			List<Long> ids =
					sc.stream()
							.map(OrderExportServiceOrderQuerySupport::longVal)
							.filter(id -> id > 0L)
							.distinct()
							.toList();
			if (!ids.isEmpty()) {
				w.in(ServiceOrders::getShopId, ids);
				return;
			}
		}
		putEqIfPresent(w, filter, "shop_id", ServiceOrders::getShopId);
	}

	private static <T> void applyIntTimeRange(
			LambdaQueryWrapper<T> w,
			LinkedHashMap<String, Object> filter,
			String col,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Integer> getter) {
		Object gte = filter.get(col + "|gte");
		Object lte = filter.get(col + "|lte");
		if (gte != null) {
			w.ge(getter, (int) longVal(gte));
		}
		if (lte != null) {
			w.le(getter, (int) longVal(lte));
		}
	}

	private static <T> void putEqIfPresent(
			LambdaQueryWrapper<T> w,
			LinkedHashMap<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> col) {
		Object v = filter.get(key);
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if ("user_id".equals(key) || "shop_id".equals(key) || "salesman_id".equals(key) || "source_id".equals(key)) {
			w.eq(col, longVal(v));
			return;
		}
		w.eq(col, s);
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
