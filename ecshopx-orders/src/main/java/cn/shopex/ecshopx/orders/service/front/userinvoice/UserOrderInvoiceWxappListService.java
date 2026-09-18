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

package cn.shopex.ecshopx.orders.service.front.userinvoice;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.UserOrderInvoice;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.repository.UserOrderInvoiceRepository;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListExecutorKind;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListTypeRegistry;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderListService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappOrderListTypeDispatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UserOrderInvoiceWxappListService {

	private final UserOrderInvoiceRepository userOrderInvoiceRepository;
	private final NormalOrdersMapper normalOrdersMapper;
	private final ServiceOrdersMapper serviceOrdersMapper;
	private final AdminOrderListTypeRegistry adminOrderListTypeRegistry;
	private final WxappOrderListTypeDispatchService wxappOrderListTypeDispatchService;
	private final WxappOrderListService wxappOrderListService;

	public UserOrderInvoiceWxappListService(
			UserOrderInvoiceRepository userOrderInvoiceRepository,
			NormalOrdersMapper normalOrdersMapper,
			ServiceOrdersMapper serviceOrdersMapper,
			AdminOrderListTypeRegistry adminOrderListTypeRegistry,
			WxappOrderListTypeDispatchService wxappOrderListTypeDispatchService,
			WxappOrderListService wxappOrderListService) {
		this.userOrderInvoiceRepository = userOrderInvoiceRepository;
		this.normalOrdersMapper = normalOrdersMapper;
		this.serviceOrdersMapper = serviceOrdersMapper;
		this.adminOrderListTypeRegistry = adminOrderListTypeRegistry;
		this.wxappOrderListTypeDispatchService = wxappOrderListTypeDispatchService;
		this.wxappOrderListService = wxappOrderListService;
	}

	public Map<String, Object> getInvoiceList(long userId, long companyId, int page, int pageSize) {
		Page<UserOrderInvoice> pg =
				userOrderInvoiceRepository.lists(companyId, userId, null, null, null, page, pageSize);
		long totalCount = pg.getTotal();
		List<UserOrderInvoice> records = pg.getRecords();

		Set<Long> allOrderIds = new LinkedHashSet<>();
		for (UserOrderInvoice v : records) {
			Long oid = parsePositiveLongFromOrderId(v.getOrderId());
			if (oid != null) {
				allOrderIds.add(oid);
			}
		}

		Map<Long, NormalOrders> normalByOrderId = new HashMap<>();
		if (!allOrderIds.isEmpty()) {
			List<NormalOrders> nl =
					normalOrdersMapper.selectList(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.in(NormalOrders::getOrderId, allOrderIds));
			for (NormalOrders no : nl) {
				normalByOrderId.put(no.getOrderId(), no);
			}
		}
		Set<Long> missing = new LinkedHashSet<>(allOrderIds);
		missing.removeAll(normalByOrderId.keySet());
		Map<Long, ServiceOrders> serviceByOrderId = new HashMap<>();
		if (!missing.isEmpty()) {
			List<ServiceOrders> sl =
					serviceOrdersMapper.selectList(
							new LambdaQueryWrapper<ServiceOrders>()
									.eq(ServiceOrders::getCompanyId, companyId)
									.in(ServiceOrders::getOrderId, missing));
			for (ServiceOrders so : sl) {
				serviceByOrderId.put(so.getOrderId(), so);
			}
		}

		Map<String, DispatchGroup> groups = new LinkedHashMap<>();
		Set<Long> dispatchSeen = new HashSet<>();
		for (UserOrderInvoice v : records) {
			Long oid = parsePositiveLongFromOrderId(v.getOrderId());
			if (oid == null || !dispatchSeen.add(oid)) {
				continue;
			}
			NormalOrders no = normalByOrderId.get(oid);
			ServiceOrders so = serviceByOrderId.get(oid);
			String dbOt;
			String dbOc;
			if (no != null) {
				dbOt = no.getOrderType() == null ? "normal" : no.getOrderType();
				dbOc = no.getOrderClass() == null ? "" : no.getOrderClass();
			} else if (so != null) {
				dbOt = so.getOrderType() == null ? "service" : so.getOrderType();
				dbOc = so.getOrderClass() == null ? "" : so.getOrderClass();
			} else {
				continue;
			}
			LinkedHashMap<String, Object> probe = new LinkedHashMap<>();
			Optional<AdminOrderListExecutorKind> kindOpt =
					adminOrderListTypeRegistry.resolveOrderListDispatch(dbOt, dbOc, probe);
			if (kindOpt.isEmpty()) {
				continue;
			}
			String gKey = groupDispatchKey(kindOpt.get(), probe);
			groups
					.computeIfAbsent(gKey, k -> new DispatchGroup(new LinkedHashMap<>(probe), dbOt))
					.orderIds
					.add(oid);
		}

		Map<String, Map<String, Object>> byOrderId = new LinkedHashMap<>();
		for (DispatchGroup g : groups.values()) {
			if (g.orderIds.isEmpty()) {
				continue;
			}
			List<Long> groupIds = new ArrayList<>(g.orderIds);
			LinkedHashMap<String, Object> f = new LinkedHashMap<>();
			f.put("company_id", companyId);
			f.put("user_id", userId);
			f.put("order_id|in", groupIds);
			for (String key : List.of("order_type", "order_class", "type")) {
				if (g.probeSnapshot.containsKey(key)) {
					f.put(key, g.probeSnapshot.get(key));
				}
			}
			String orderTypeArg = String.valueOf(g.probeSnapshot.getOrDefault("order_type", g.dbOt));
			Map<String, Object> core =
					wxappOrderListTypeDispatchService.getOrderList(
							orderTypeArg, f, 1, Math.max(groupIds.size(), 1), "api", true);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> batchList = (List<Map<String, Object>>) core.get("list");
			if (batchList == null) {
				continue;
			}
			for (Map<String, Object> row : batchList) {
				String k = String.valueOf(longVal(row.get("order_id")));
				if ("0".equals(k)) {
					continue;
				}
				byOrderId.putIfAbsent(k, row);
			}
		}

		List<Map<String, Object>> outList = new ArrayList<>();
		for (UserOrderInvoice v : records) {
			Long oidParsed = parsePositiveLongFromOrderId(v.getOrderId());
			if (oidParsed == null) {
				outList.add(shapeA(v));
				continue;
			}
			String oidKey = String.valueOf(oidParsed);
			Map<String, Object> orderRow = byOrderId.get(oidKey);
			if (orderRow == null) {
				outList.add(shapeA(v));
			} else {
				LinkedHashMap<String, Object> merged = deepCopyToMutableMap(orderRow);
				merged.put("invoice_url", v.getInvoice() == null ? "" : v.getInvoice());
				wxappOrderListService.enrichWxappOrderListRows(companyId, List.of(merged));
				outList.add(merged);
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", outList);
		return out;
	}

	private static String groupDispatchKey(AdminOrderListExecutorKind kind, Map<String, Object> probe) {
		return kind.name()
				+ "\0"
				+ Objects.toString(probe.get("order_type"), "")
				+ "\0"
				+ Objects.toString(probe.get("order_class"), "")
				+ "\0"
				+ Objects.toString(probe.get("type"), "");
	}

	private static Long parsePositiveLongFromOrderId(String orderIdStr) {
		if (orderIdStr == null) {
			return null;
		}
		String t = orderIdStr.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static LinkedHashMap<String, Object> shapeA(UserOrderInvoice v) {
		LinkedHashMap<String, Object> a = new LinkedHashMap<>();
		a.put("id", v.getId());
		a.put("order_id", v.getOrderId());
		a.put("user_id", v.getUserId());
		a.put("company_id", v.getCompanyId());
		a.put("status", v.getStatus());
		a.put("invoice", v.getInvoice());
		return a;
	}

	private static LinkedHashMap<String, Object> deepCopyToMutableMap(Map<?, ?> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			out.put(String.valueOf(e.getKey()), deepCopyValue(e.getValue()));
		}
		return out;
	}

	private static Object deepCopyValue(Object v) {
		if (v instanceof Map<?, ?> m) {
			return deepCopyToMutableMap(m);
		}
		if (v instanceof Collection<?> c) {
			List<Object> nl = new ArrayList<>();
			for (Object x : c) {
				nl.add(deepCopyValue(x));
			}
			return nl;
		}
		return v;
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

	private static final class DispatchGroup {
		final LinkedHashMap<String, Object> probeSnapshot;
		final String dbOt;
		final LinkedHashSet<Long> orderIds = new LinkedHashSet<>();

		DispatchGroup(LinkedHashMap<String, Object> probeSnapshot, String dbOt) {
			this.probeSnapshot = probeSnapshot;
			this.dbOt = dbOt;
		}
	}
}
