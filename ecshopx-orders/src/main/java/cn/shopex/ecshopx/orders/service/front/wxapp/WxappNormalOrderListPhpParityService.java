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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.common.port.order.OrderExportEmployeePurchaseInfoLookupPort;
import cn.shopex.ecshopx.orders.mapper.AdminOrderListSourcesLookupMapper;
import cn.shopex.ecshopx.orders.mapper.AdminOrderListSourcesLookupMapper.SourceNameRow;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class WxappNormalOrderListPhpParityService {

	private static final DateTimeFormatter CREATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final AdminOrderListSourcesLookupMapper adminOrderListSourcesLookupMapper;
	private final ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort;

	public WxappNormalOrderListPhpParityService(
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			AdminOrderListSourcesLookupMapper adminOrderListSourcesLookupMapper,
			ObjectProvider<OrderExportEmployeePurchaseInfoLookupPort> employeePurchaseInfoLookupPort) {
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.adminOrderListSourcesLookupMapper = adminOrderListSourcesLookupMapper;
		this.employeePurchaseInfoLookupPort = employeePurchaseInfoLookupPort;
	}

	/**
	 * Aligns normal-order wxapp list rows with PHP {@code AbstractNormalOrder} + {@code WxappOrder#getOrderList} list
	 * payload (consumer list, not admin detail).
	 */
	public void applyIfDecoratedNormalList(long companyId, List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		Map<String, Object> probe = list.get(0);
		if (!probe.containsKey("trade")) {
			return;
		}

		attachSourceNamesAndCreateDate(list);
		attachDistributorFields(companyId, list);
		attachEmployeePurchaseFields(companyId, list);

		for (Map<String, Object> row : list) {
			row.put("app_info", new ArrayList<>());
			row.remove("trade");

			Object dadaRaw = row.get("dada");
			if (!(dadaRaw instanceof Map<?, ?> dm) || dm.isEmpty()) {
				row.put("dada", new ArrayList<>());
			}

			Object cancelRaw = row.get("cancelData");
			if (cancelRaw instanceof Map<?, ?> cm && cm.isEmpty()) {
				row.remove("cancelData");
			}

			Object inv = row.get("invoice_info");
			if (inv instanceof Map<?, ?> im && im.isEmpty()) {
				row.put("invoice_info", new ArrayList<>());
			}

			row.remove("estimate_get_points");
			row.remove("latest_aftersale_time");
			row.remove("refund_freight");
			row.remove("refund_freight_amount");
			row.remove("promotion_discount");
			row.remove("auto_cancel_seconds");
			row.remove("cny_fee");
			row.remove("end_date");
			row.remove("app_pay_type_desc");

			row.putIfAbsent("orders_purchase_info", null);

			normalizeCanApplyAftersales(row);

			int ops = intVal(row.get("offline_payment_status"));
			row.put("offline_pay_check_status", ops == -1 ? null : ops);
		}
	}

	private void normalizeCanApplyAftersales(Map<String, Object> row) {
		Object raw = row.get("can_apply_aftersales");
		if (raw == null) {
			return;
		}
		int v = intVal(raw);
		if (v == 1) {
			return;
		}
		if ("CANCEL".equals(str(row.get("order_status"))) && v == 0) {
			return;
		}
		row.remove("can_apply_aftersales");
	}

	private void attachSourceNamesAndCreateDate(List<Map<String, Object>> list) {
		List<Long> sourceIds =
				list.stream().map(r -> longVal(r.get("source_id"))).filter(id -> id > 0L).distinct().toList();
		Map<Long, String> names = new LinkedHashMap<>();
		if (!sourceIds.isEmpty()) {
			for (SourceNameRow sn : adminOrderListSourcesLookupMapper.selectSourceNamesByIds(sourceIds)) {
				if (sn.sourceId() != null) {
					names.put(sn.sourceId(), sn.sourceName() != null ? sn.sourceName() : "-");
				}
			}
		}
		for (Map<String, Object> row : list) {
			long sid = longVal(row.get("source_id"));
			if (sourceIds.isEmpty()) {
				row.put("source_name", "-");
			} else {
				row.put("source_name", sid > 0 && names.containsKey(sid) ? names.get(sid) : "-");
			}
			row.put("create_date", formatCreateDate(intVal(row.get("create_time"))));
		}
	}

	private void attachEmployeePurchaseFields(long companyId, List<Map<String, Object>> list) {
		OrderExportEmployeePurchaseInfoLookupPort port = employeePurchaseInfoLookupPort.getIfAvailable();
		if (port == null) {
			return;
		}
		List<Long> orderIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			long oid = longVal(row.get("order_id"));
			if (oid > 0L) {
				orderIds.add(oid);
			}
		}
		if (orderIds.isEmpty()) {
			return;
		}
		Map<Long, OrderExportEmployeePurchaseInfoLookupPort.Info> byOrder =
				port.lookupByOrderIds(companyId, orderIds);
		for (Map<String, Object> row : list) {
			long oid = longVal(row.get("order_id"));
			OrderExportEmployeePurchaseInfoLookupPort.Info info = byOrder.get(oid);
			if (info == null) {
				continue;
			}
			String mode = info.purchaseMode() == null ? "" : info.purchaseMode();
			if (!mode.isEmpty()) {
				row.put("purchase_mode", mode);
				row.put(
						"purchase_mode_desc",
						"prepaid_point".equals(mode) ? "预充点数" : ("cash".equals(mode) ? "现金" : ""));
				row.put("is_employee_purchase", true);
				row.put("employee_purchase_tag", "企业购");
				row.put(
						"employee_purchase_activity_id",
						info.activityId() == null ? 0L : info.activityId());
				row.put(
						"employee_purchase_activity_name",
						info.activityName() == null ? "" : info.activityName());
			}
		}
	}

	private void attachDistributorFields(long companyId, List<Map<String, Object>> list) {
		Map<String, Object> selfRow =
				new LinkedHashMap<>(adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId));
		Set<Long> positiveIds = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			long did = longVal(row.get("distributor_id"));
			if (did > 0L) {
				positiveIds.add(did);
			}
		}
		Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
		for (Long id : positiveIds) {
			Map<String, Object> one =
					adminOrderDetailDistributionSupportPort.getDistributorInfoSimple(companyId, String.valueOf(id));
			byId.put(id, one.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(one));
		}

		for (Map<String, Object> row : list) {
			long did = longVal(row.get("distributor_id"));
			Map<String, Object> info;
			if (did > 0L) {
				info = new LinkedHashMap<>(byId.getOrDefault(did, new LinkedHashMap<>()));
			} else {
				info = new LinkedHashMap<>(selfRow);
			}
			if (info.isEmpty()) {
				row.put("distributor_info", new ArrayList<>());
				row.put("distributor_name", "");
			} else {
				row.put("distributor_info", info);
				Object nm = info.get("name");
				row.put("distributor_name", nm == null ? "" : String.valueOf(nm));
			}
		}
	}

	private static String formatCreateDate(int createTime) {
		if (createTime <= 0) {
			return "";
		}
		return Instant.ofEpochSecond(createTime).atZone(ZoneId.systemDefault()).toLocalDateTime().format(CREATE_FMT);
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
