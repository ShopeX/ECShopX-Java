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

package cn.shopex.ecshopx.openapi.thirdapi.v1.orders;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailStatusAppApplier;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1MemberOrderListOrdersService {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;

	public OpenapiThirdApiV1MemberOrderListOrdersService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
	}

	public Map<String, Object> queryOrderItemLists(Map<String, Object> orderFilter, int page, int pageSize) {
		long count = normalOrdersMapper.selectCount(buildWrapper(orderFilter));

		LinkedHashMap<String, Object> pager = new LinkedHashMap<>();
		pager.put("count", count);
		pager.put("page_no", page);
		pager.put("page_size", pageSize);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("pager", pager);
		result.put("list", new ArrayList<Map<String, Object>>());

		if (count == 0L) {
			return result;
		}

		long offset = (long) (page - 1) * pageSize;
		LambdaQueryWrapper<NormalOrders> wrapper = buildWrapper(orderFilter);
		wrapper.orderByDesc(NormalOrders::getCreateTime);
		wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);

		List<NormalOrders> orders = normalOrdersMapper.selectList(wrapper);
		Map<Long, NormalOrdersRelDada> dadaByOrderId = loadDadaByOrderId(orders);

		List<Map<String, Object>> list = new ArrayList<>();
		for (NormalOrders order : orders) {
			Map<String, Object> row = normalOrdersServiceOrderDataAssembler.toServiceOrderData(order);
			applyOpenapiNotpayCancel(row);
			NormalOrdersRelDada dada = dadaByOrderId.get(order.getOrderId());
			Map<String, Object> dadaMap = dadaToMap(dada);
			adminOrderDetailStatusAppApplier.apply(row, dadaMap, "", "api");
			row.put("create_date", formatEpoch(row.get("create_time")));
			row.put("items", loadFirstItemAsList(order.getOrderId()));
			list.add(row);
		}
		result.put("list", list);
		return result;
	}

	public Map<Object, Long> sumTotalFeeByUserId(Map<String, Object> orderFilter) {
		List<LinkedHashMap<String, Object>> rows = normalOrdersMapper.sumTotalFeeGroupByUserId(orderFilter);
		if (rows == null || rows.isEmpty()) {
			return Map.of();
		}
		Map<Object, Long> out = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Object userId = row.get("user_id");
			long fee = longVal(row.get("fee"));
			if (userId != null) {
				out.put(userId, fee);
			}
		}
		return out;
	}

	private Map<Long, NormalOrdersRelDada> loadDadaByOrderId(List<NormalOrders> orders) {
		List<Long> orderIds =
				orders.stream().map(NormalOrders::getOrderId).filter(id -> id != null && id > 0L).toList();
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<NormalOrdersRelDada> dadaRows =
				normalOrdersRelDadaMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.in(NormalOrdersRelDada::getOrderId, orderIds));
		return dadaRows.stream()
				.collect(Collectors.toMap(NormalOrdersRelDada::getOrderId, d -> d, (a, b) -> a));
	}

	private List<Map<String, Object>> loadFirstItemAsList(long orderId) {
		List<Map<String, Object>> items = normalOrdersItemsMapper.selectFirstItemByOrderId(orderId);
		if (items == null || items.isEmpty()) {
			return List.of();
		}
		return items;
	}

	private static LambdaQueryWrapper<NormalOrders> buildWrapper(Map<String, Object> orderFilter) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getCompanyId, longVal(orderFilter.get("company_id")));

		Object userId = orderFilter.get("user_id");
		if (userId instanceof Number n) {
			w.eq(NormalOrders::getUserId, n.longValue());
		} else if (userId != null) {
			w.eq(NormalOrders::getUserId, longVal(userId));
		}

		Object orderType = orderFilter.get("order_type");
		if (orderType != null && !String.valueOf(orderType).isEmpty()) {
			w.eq(NormalOrders::getOrderType, String.valueOf(orderType));
		}

		Object notIn = orderFilter.get("order_status|notin");
		if (notIn instanceof List<?> list && !list.isEmpty()) {
			w.notIn(NormalOrders::getOrderStatus, list.stream().map(String::valueOf).toList());
		}

		if (orderFilter.containsKey("order_class")) {
			w.eq(NormalOrders::getOrderClass, String.valueOf(orderFilter.get("order_class")));
		}
		return w;
	}

	private static void applyOpenapiNotpayCancel(Map<String, Object> row) {
		if (!"NOTPAY".equals(str(row.get("order_status")))) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		int autoCancel = intVal(row.get("auto_cancel_time"));
		if (autoCancel > 0 && autoCancel - now <= 0) {
			row.put("order_status", "CANCEL");
		}
	}

	private static String formatEpoch(Object raw) {
		long sec = longVal(raw);
		if (sec <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(sec));
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

	private static int intVal(Object raw) {
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

	private static String str(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static Map<String, Object> dadaToMap(NormalOrdersRelDada d) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (d == null) {
			return m;
		}
		m.put("id", d.getId());
		m.put("order_id", d.getOrderId());
		m.put("company_id", d.getCompanyId());
		if (d.getDadaStatus() != null) {
			m.put("dada_status", String.valueOf(d.getDadaStatus()));
		}
		m.put("dada_delivery_no", d.getDadaDeliveryNo());
		m.put("dada_cancel_from", d.getDadaCancelFrom());
		m.put("dm_id", d.getDmId());
		m.put("dm_name", d.getDmName());
		m.put("dm_mobile", d.getDmMobile());
		m.put("pickup_time", d.getPickupTime());
		m.put("accept_time", d.getAcceptTime());
		m.put("delivered_time", d.getDeliveredTime());
		m.put("create_time", d.getCreateTime());
		m.put("update_time", d.getUpdateTime());
		m.put("delivery_length", 0);
		return m;
	}
}
