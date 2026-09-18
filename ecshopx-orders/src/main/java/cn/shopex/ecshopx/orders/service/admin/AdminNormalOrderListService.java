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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.members.domain.MembersDeleteRecord;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminNormalOrderListService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final AdminNormalOrderListRowDecorator adminNormalOrderListRowDecorator;
	private final MembersDeleteRecordMapper membersDeleteRecordMapper;

	public AdminNormalOrderListService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			AdminNormalOrderListRowDecorator adminNormalOrderListRowDecorator,
			MembersDeleteRecordMapper membersDeleteRecordMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.adminNormalOrderListRowDecorator = adminNormalOrderListRowDecorator;
		this.membersDeleteRecordMapper = membersDeleteRecordMapper;
	}

	/**
	 * All rows matching the filter (no pagination), for aggregations such as community chief statistics.
	 */
	public List<NormalOrders> listAllMatching(long companyId, Map<String, Object> filter, boolean createTimeAsc) {
		LambdaQueryWrapper<NormalOrders> wrapper = buildWrapper(companyId, filter);
		if (createTimeAsc) {
			wrapper.orderByAsc(NormalOrders::getCreateTime);
		} else {
			wrapper.orderByDesc(NormalOrders::getCreateTime);
		}
		return normalOrdersMapper.selectList(wrapper);
	}

	public AdminNormalOrderListPageResult pageOrders(
			Map<String, Object> filter, int pageNo, int pageSize, boolean createTimeAsc) {
		long companyId = longVal(filter.get("company_id"));
		LambdaQueryWrapper<NormalOrders> wrapper = buildWrapper(companyId, filter);
		LambdaQueryWrapper<NormalOrders> countWrapper = buildWrapper(companyId, filter);

		Page<NormalOrders> page = new Page<>(pageNo, pageSize, false);
		if (createTimeAsc) {
			wrapper.orderByAsc(NormalOrders::getCreateTime);
		} else {
			wrapper.orderByDesc(NormalOrders::getCreateTime);
		}
		Page<NormalOrders> result = normalOrdersMapper.selectPage(page, wrapper);
		Long totalCt = normalOrdersMapper.selectCount(countWrapper);
		long total = totalCt == null ? 0L : totalCt;

		List<Map<String, Object>> rows = new ArrayList<>();
		for (NormalOrders order : result.getRecords()) {
			Map<String, Object> row = new LinkedHashMap<>(normalOrdersServiceOrderDataAssembler.toServiceOrderData(order));
			row.put("order_id", String.valueOf(order.getOrderId()));
			row.put("company_id", companyId);
			rows.add(row);
		}
		adminNormalOrderListRowDecorator.decorateBatch(companyId, rows);
		applyUserDeleteFlags(companyId, rows);
		return new AdminNormalOrderListPageResult(total, rows);
	}

	private LambdaQueryWrapper<NormalOrders> buildWrapper(long companyId, Map<String, Object> filter) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getCompanyId, companyId);
		w.eq(NormalOrders::getOrderType, "normal");
		String orderClass = str(filter.get("order_class"));
		if (StringUtils.hasText(orderClass)) {
			w.eq(NormalOrders::getOrderClass, orderClass);
		}

		Object merchantId = filter.get("merchant_id");
		if (merchantId != null && StringUtils.hasText(String.valueOf(merchantId))) {
			w.eq(NormalOrders::getMerchantId, longVal(merchantId));
		}

		applyIntTimeRange(w, filter, "create_time", NormalOrders::getCreateTime);
		applyIntTimeRange(w, filter, "delivery_time", NormalOrders::getDeliveryTime);
		Object acGt = filter.get("auto_cancel_time|gt");
		if (acGt != null) {
			w.apply("(CAST(auto_cancel_time AS SIGNED) > {0})", longVal(acGt));
		}

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
		putEqIfPresent(w, filter, "shop_id", NormalOrders::getShopId);
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
								.map(AdminNormalOrderListService::longVal)
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
			List<Long> ids =
					c.stream()
							.map(AdminNormalOrderListService::longVal)
							.distinct()
							.toList();
			w.in(NormalOrders::getActId, ids);
		}

		return w;
	}

	private static void applyIntTimeRange(
			LambdaQueryWrapper<NormalOrders> w,
			Map<String, Object> filter,
			String col,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<NormalOrders, Integer> getter) {
		Object gte = filter.get(col + "|gte");
		Object lte = filter.get(col + "|lte");
		if (gte != null) {
			w.ge(getter, (int) longVal(gte));
		}
		if (lte != null) {
			w.le(getter, (int) longVal(lte));
		}
	}

	private static void putEqIfPresent(
			LambdaQueryWrapper<NormalOrders> w,
			Map<String, Object> filter,
			String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<NormalOrders, ?> col) {
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

	private void applyUserDeleteFlags(long companyId, List<Map<String, Object>> rows) {
		List<Long> userIds =
				rows.stream()
						.map(r -> longVal(r.get("user_id")))
						.filter(id -> id > 0)
						.distinct()
						.toList();
		if (userIds.isEmpty()) {
			for (Map<String, Object> r : rows) {
				r.put("user_delete", false);
			}
			return;
		}
		List<MembersDeleteRecord> recs =
				membersDeleteRecordMapper.selectList(
						new LambdaQueryWrapper<MembersDeleteRecord>()
								.eq(MembersDeleteRecord::getCompanyId, companyId)
								.in(MembersDeleteRecord::getUserId, userIds));
		Set<Long> deleted =
				recs.stream().map(MembersDeleteRecord::getUserId).collect(Collectors.toSet());
		for (Map<String, Object> r : rows) {
			long uid = longVal(r.get("user_id"));
			r.put("user_delete", uid > 0 && deleted.contains(uid));
		}
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
