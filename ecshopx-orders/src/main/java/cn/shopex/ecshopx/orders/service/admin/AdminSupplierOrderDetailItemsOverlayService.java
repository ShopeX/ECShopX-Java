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

import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AdminSupplierOrderDetailItemsOverlayService {

	private final SupplierOrderMapper supplierOrderMapper;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;
	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;

	public AdminSupplierOrderDetailItemsOverlayService(
			SupplierOrderMapper supplierOrderMapper,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier,
			TradeCancelSettingRedisService tradeCancelSettingRedisService) {
		this.supplierOrderMapper = supplierOrderMapper;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
	}

	@SuppressWarnings("unchecked")
	public void apply(Map<String, Object> operatorJwt, Map<String, Object> result) {
		if (operatorJwt == null) {
			return;
		}
		String opType = Objects.toString(operatorJwt.get("operator_type"), "").trim();
		if (!"supplier".equals(opType)) {
			return;
		}
		int supplierId = resolveSupplierOperatorId(operatorJwt);
		if (supplierId <= 0) {
			return;
		}
		Object oiObj = result.get("orderInfo");
		if (!(oiObj instanceof Map<?, ?>)) {
			return;
		}
		Map<String, Object> orderInfo = (Map<String, Object>) oiObj;
		Object itemsObj = orderInfo.get("items");
		if (itemsObj instanceof List<?> rawList) {
			List<Map<String, Object>> kept = new ArrayList<>();
			for (Object o : rawList) {
				if (!(o instanceof Map<?, ?> row)) {
					continue;
				}
				int sid = intVal(row.get("supplier_id"));
				if (sid == supplierId) {
					kept.add((Map<String, Object>) row);
				}
			}
			orderInfo.put("items", kept);
		}

		Object orderIdObj = orderInfo.get("order_id");
		if (orderIdObj == null) {
			return;
		}
		long orderId;
		try {
			orderId = Long.parseLong(String.valueOf(orderIdObj).trim());
		} catch (NumberFormatException e) {
			return;
		}
		Object companyObj = orderInfo.get("company_id");
		long companyId = companyObj == null ? 0L : longVal(companyObj);
		if (companyId <= 0L) {
			return;
		}
		SupplierOrder row =
				supplierOrderMapper.selectOne(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getSupplierId, supplierId)
								.eq(SupplierOrder::getOrderId, orderId)
								.eq(SupplierOrder::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row != null) {
			if (row.getOrderStatus() != null) {
				orderInfo.put("order_status", row.getOrderStatus());
			}
			if (row.getDeliveryStatus() != null) {
				orderInfo.put("delivery_status", row.getDeliveryStatus());
			}
			if (row.getCancelStatus() != null) {
				orderInfo.put("cancel_status", row.getCancelStatus());
			}
		}

		refreshSupplierDetailPresentation(companyId, result, orderInfo);
	}

	@SuppressWarnings("unchecked")
	private void refreshSupplierDetailPresentation(
			long companyId, Map<String, Object> result, Map<String, Object> orderInfo) {
		Map<String, Object> dadaMap = dadaSubMap(orderInfo);
		String cancelFrom = "";
		Object cancelDataObj = result.get("cancelData");
		Map<String, Object> cancelData = new LinkedHashMap<>();
		if (cancelDataObj instanceof Map<?, ?> rawCancel && !rawCancel.isEmpty()) {
			cancelData = (Map<String, Object>) rawCancel;
			cancelFrom = str(cancelData.get("cancel_from"));
		}

		adminOrderDetailStatusAppApplier.apply(orderInfo, dadaMap, cancelFrom, "api");

		Map<String, Object> setting = tradeCancelSettingRedisService.getCancelSetting(companyId);
		boolean repeatCancel = Boolean.TRUE.equals(setting.get("repeat_cancel"));
		AdminOrderCanApplyCancelResolver.apply(orderInfo, cancelData, repeatCancel);
	}

	private static Map<String, Object> dadaSubMap(Map<String, Object> orderInfo) {
		Object d = orderInfo.get("dada");
		if (d instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dm = (Map<String, Object>) m;
			return dm;
		}
		return new LinkedHashMap<>();
	}

	private static int resolveSupplierOperatorId(Map<String, Object> jwt) {
		Object sid = jwt.get("supplier_id");
		if (sid != null) {
			int v = intVal(sid);
			if (v > 0) {
				return v;
			}
		}
		return intVal(jwt.get("operator_id"));
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
