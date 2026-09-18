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

import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminDeliveryListsService {

	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final SupplierMapper supplierMapper;

	public AdminDeliveryListsService(
			OrdersDeliveryMapper ordersDeliveryMapper, SupplierMapper supplierMapper) {
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.supplierMapper = supplierMapper;
	}

	public List<LinkedHashMap<String, Object>> lists(
			long companyId, String operatorType, long operatorId, String orderIdRaw) {
		LambdaQueryWrapper<OrdersDelivery> w = new LambdaQueryWrapper<>();
		w.eq(OrdersDelivery::getCompanyId, companyId);
		if ("supplier".equals(operatorType)) {
			w.eq(OrdersDelivery::getSupplierId, toIntSupplierFilter(operatorId));
		}
		String raw = orderIdRaw == null ? "" : orderIdRaw.trim();
		if (raw.isEmpty()) {
			w.isNull(OrdersDelivery::getOrderId);
		} else {
			try {
				long parsed = Long.parseLong(raw);
				w.eq(OrdersDelivery::getOrderId, parsed);
			} catch (NumberFormatException e) {
				w.eq(OrdersDelivery::getOrderId, 0L);
			}
		}
		List<OrdersDelivery> rows = ordersDeliveryMapper.selectList(w);
		List<LinkedHashMap<String, Object>> result = new ArrayList<>(rows.size());
		for (OrdersDelivery e : rows) {
			int sec = e.getDeliveryTime() == null ? 0 : e.getDeliveryTime();
			String deliveryTimeFormatted =
					Instant.ofEpochSecond(sec)
							.atZone(ZoneId.of("Asia/Shanghai"))
							.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			result.add(toRowMap(e, deliveryTimeFormatted));
		}
		if (result.isEmpty()) {
			return result;
		}
		LinkedHashSet<Integer> idSet = new LinkedHashSet<>();
		for (OrdersDelivery row : rows) {
			Integer sid = row.getSupplierId();
			if (sid != null && sid > 0) {
				idSet.add(sid);
			}
		}
		if (idSet.isEmpty()) {
			return result;
		}
		List<Supplier> suppliers =
				supplierMapper.selectList(
						new LambdaQueryWrapper<Supplier>()
								.select(Supplier::getOperatorId, Supplier::getSupplierName)
								.in(Supplier::getOperatorId, idSet));
		Map<Long, String> nameByOperatorId = new LinkedHashMap<>();
		for (Supplier supplier : suppliers) {
			Long opId = supplier.getOperatorId();
			if (opId == null) {
				continue;
			}
			String name = supplier.getSupplierName() == null ? "" : supplier.getSupplierName();
			nameByOperatorId.put(opId, name);
		}
		for (int i = 0; i < rows.size(); i++) {
			OrdersDelivery row = rows.get(i);
			LinkedHashMap<String, Object> m = result.get(i);
			Integer sid = row.getSupplierId();
			if (sid == null || sid <= 0) {
				continue;
			}
			m.put("supplier_name", nameByOperatorId.getOrDefault(Long.valueOf(sid), ""));
		}
		return result;
	}

	private static int toIntSupplierFilter(long operatorId) {
		if (operatorId < Integer.MIN_VALUE || operatorId > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) operatorId;
	}

	private static LinkedHashMap<String, Object> toRowMap(OrdersDelivery e, String deliveryTimeFormatted) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("orders_delivery_id", e.getOrdersDeliveryId());
		m.put("company_id", e.getCompanyId());
		m.put("supplier_id", e.getSupplierId());
		m.put("order_id", e.getOrderId());
		m.put("user_id", e.getUserId());
		m.put("delivery_corp", e.getDeliveryCorp());
		m.put("delivery_corp_name", e.getDeliveryCorpName());
		m.put("delivery_code", e.getDeliveryCode());
		m.put("delivery_time", deliveryTimeFormatted);
		m.put("delivery_corp_source", e.getDeliveryCorpSource());
		m.put("receiver_mobile", e.getReceiverMobile());
		m.put("package_type", e.getPackageType());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("self_delivery_operator_id", e.getSelfDeliveryOperatorId());
		m.put("delivery_remark", e.getDeliveryRemark());
		m.put("delivery_pics", e.getDeliveryPics());
		return m;
	}
}
