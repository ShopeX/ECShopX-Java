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

package cn.shopex.ecshopx.orders.service.orderlog;

import cn.shopex.ecshopx.orders.domain.OrderProcessLog;
import cn.shopex.ecshopx.orders.mapper.OrderProcessLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessLogBusService {

	private final OrderProcessLogMapper orderProcessLogMapper;
	private final ObjectMapper objectMapper;

	public OrderProcessLogBusService(OrderProcessLogMapper orderProcessLogMapper, ObjectMapper objectMapper) {
		this.orderProcessLogMapper = orderProcessLogMapper;
		this.objectMapper = objectMapper;
	}

	public void persistEntitiesMap(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		OrderProcessLog row = new OrderProcessLog();
		row.setOrderId(toLong(entities.get("order_id")));
		row.setCompanyId(toLong(entities.get("company_id")));
		row.setSupplierId((int) toLong(entities.get("supplier_id")));
		row.setOperatorType(toStr(entities.get("operator_type")));
		row.setOperatorId(toLong(entities.get("operator_id")));
		row.setRemarks(toStr(entities.get("remarks")));
		row.setDetail(toStr(entities.get("detail")));
		row.setParams(serializeParams(entities.get("params")));
		row.setIsShow(toBool(entities.get("is_show"), true));
		row.setDeliveryRemark(toStr(entities.get("delivery_remark")));
		Object picsRaw = entities.get("pics");
		if (picsRaw != null) {
			row.setPics(serializeParams(picsRaw));
		}
		row.setCreateTime(now);
		row.setUpdateTime(now);
		orderProcessLogMapper.insert(row);
	}

	private String serializeParams(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException ex) {
			return String.valueOf(raw);
		}
	}

	private static long toLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String toStr(Object v) {
		return v == null ? null : v.toString();
	}

	private static boolean toBool(Object v, boolean def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
			return true;
		}
		if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
			return false;
		}
		return def;
	}
}
