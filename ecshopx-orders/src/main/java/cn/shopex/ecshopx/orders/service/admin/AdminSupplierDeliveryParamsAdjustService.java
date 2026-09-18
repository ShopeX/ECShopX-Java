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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminSupplierDeliveryParamsAdjustService {

	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final ObjectMapper objectMapper;

	public AdminSupplierDeliveryParamsAdjustService(
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			ObjectMapper objectMapper) {
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.objectMapper = objectMapper;
	}

	public void adjustForSupplierIfNeeded(Map<String, Object> params, OrderAssociations assoc, int supplierId) {
		String deliveryType = stringParam(params, "delivery_type");
		if ("sep".equalsIgnoreCase(deliveryType)) {
			return;
		}
		long orderId = parseOrderId(params.get("order_id"));
		long companyId = parseLong(params.get("company_id"));
		SupplierOrder supplierOrder =
				supplierOrderMapper.selectOne(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getOrderId, orderId)
								.eq(SupplierOrder::getSupplierId, supplierId)
								.eq(SupplierOrder::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (supplierOrder == null) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		List<NormalOrdersItems> lines =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getSupplierId, supplierId));
		List<Map<String, Object>> sepInfo = new ArrayList<>();
		String deliveryCorp = stringParam(params, "delivery_corp");
		String deliveryCode = stringParam(params, "delivery_code");
		for (NormalOrdersItems v : lines) {
			Map<String, Object> entry = new LinkedHashMap<>();
			int num = v.getNum() == null ? 0 : v.getNum();
			int sent = v.getDeliveryItemNum() == null ? 0 : v.getDeliveryItemNum();
			entry.put("delivery_num", num);
			entry.put("ship_num", num);
			entry.put("delivery_item_num", sent);
			entry.put("num", num);
			entry.put("id", v.getId());
			entry.put("item_id", v.getItemId());
			entry.put("item_name", v.getItemName());
			entry.put("pic", v.getPic());
			entry.put("delivery_corp", deliveryCorp);
			entry.put("delivery_code", deliveryCode);
			entry.put("delivery_status", "DONE");
			sepInfo.add(entry);
		}
		try {
			params.put("sepInfo", objectMapper.writeValueAsString(sepInfo));
		} catch (JsonProcessingException e) {
			throw new ResourceException("拆单信息不正确");
		}
		params.put("delivery_type", "sep");
		String mobile = supplierOrder.getReceiverMobile();
		if (StringUtils.hasText(mobile)) {
			params.put("ship_mobile", mobile.trim());
		}
	}

	private static String stringParam(Map<String, Object> params, String key) {
		Object v = params.get(key);
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long parseOrderId(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseLong(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
