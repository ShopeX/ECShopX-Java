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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailPayloadMaps;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationAssociationDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1OrderDeliverService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;
	private final OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV1OrderDeliverService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService,
			OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler,
			ObjectMapper objectMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.adminNormalOrderDeliveryCoreService = adminNormalOrderDeliveryCoreService;
		this.orderAssociationAssociationDataAssembler = orderAssociationAssociationDataAssembler;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiDeliver(
			long companyId, String orderIdRaw, String itemInfoRaw, String lcCode, String lCode) {
		long orderId;
		try {
			orderId = Long.parseLong(orderIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("此订单不存在");
		}

		NormalOrders tradeInfo =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (tradeInfo == null) {
			throw new ResourceException("此订单不存在");
		}
		if ("DONE".equals(tradeInfo.getDeliveryStatus())) {
			throw new ResourceException("订单已发货，请勿重复发货");
		}

		List<NormalOrdersItems> itemRows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.orderByAsc(NormalOrdersItems::getId));
		if (itemRows == null) {
			throw new ResourceException("获取订单信息失败");
		}
		Map<String, Object> order = buildOrderMap(tradeInfo, itemRows);
		if (order.isEmpty()) {
			throw new ResourceException("获取订单信息失败");
		}

		Map<String, Object> result = doOrderDelivery(order, itemInfoRaw, lcCode, lCode, companyId, orderId);
		if (result == null) {
			throw new ResourceException("操作失败");
		}
		return result;
	}

	private Map<String, Object> buildOrderMap(NormalOrders tradeInfo, List<NormalOrdersItems> itemRows) {
		Map<String, Object> order = new LinkedHashMap<>();
		order.put("order_id", tradeInfo.getOrderId());
		order.put("company_id", tradeInfo.getCompanyId());
		List<Map<String, Object>> items = new ArrayList<>();
		for (NormalOrdersItems row : itemRows) {
			items.add(AdminOrderDetailPayloadMaps.itemToMap(row));
		}
		order.put("items", items);
		return order;
	}

	private Map<String, Object> doOrderDelivery(
			Map<String, Object> order,
			String itemInfoRaw,
			String lcCode,
			String lCode,
			long companyId,
			long orderId) {
		try {
			List<Map<String, Object>> itemInfoList = parseItemInfoJson(itemInfoRaw);
			Map<String, Map<String, Object>> itemInfoBySkuId = indexItemInfoBySkuId(itemInfoList);

			String deliveryCode = lCode.trim();
			String deliveryCorp = lcCode.trim();

			@SuppressWarnings("unchecked")
			List<Map<String, Object>> orderItems = (List<Map<String, Object>>) order.get("items");

			List<Map<String, Object>> noDelivery = new ArrayList<>();
			List<Map<String, Object>> emptyDelivery = new ArrayList<>();

			for (Map<String, Object> items : orderItems) {
				if (!"PENDING".equals(String.valueOf(items.get("delivery_status")))) {
					continue;
				}
				String itemBn = String.valueOf(items.get("item_bn"));
				Map<String, Object> matched = itemInfoBySkuId.get(itemBn);
				if (matched != null) {
					Map<String, Object> row = new LinkedHashMap<>(items);
					row.put("delivery_code", deliveryCode);
					row.put("delivery_corp", deliveryCorp);
					row.put("delivery_num", matched.get("qty"));
					noDelivery.add(row);
				} else {
					emptyDelivery.add(items);
				}
			}

			if (noDelivery.isEmpty() && !emptyDelivery.isEmpty()) {
				return null;
			}

			List<Map<String, Object>> sepInfo = noDelivery;
			if (sepInfo.isEmpty()) {
				return null;
			}

			OrderAssociations assoc =
					orderAssociationsMapper.selectOne(
							new LambdaQueryWrapper<OrderAssociations>()
									.eq(OrderAssociations::getCompanyId, companyId)
									.eq(OrderAssociations::getOrderId, orderId)
									.last("LIMIT 1"));
			if (assoc == null) {
				return null;
			}

			Map<String, Object> deliveryParams = new LinkedHashMap<>();
			deliveryParams.put("company_id", companyId);
			deliveryParams.put("order_id", orderId);
			deliveryParams.put("delivery_corp", deliveryCorp);
			deliveryParams.put("delivery_code", deliveryCode);
			deliveryParams.put("delivery_type", "sep");
			deliveryParams.put("sepInfo", objectMapper.writeValueAsString(sepInfo));
			deliveryParams.put("operator_type", "system");
			deliveryParams.put("operator_id", 0L);
			deliveryParams.put("supplier_id", 0);

			adminNormalOrderDeliveryCoreService.deliveryNormalPhysical(deliveryParams, assoc, "normal");

			OrderAssociations reloaded =
					orderAssociationsMapper.selectOne(
							new LambdaQueryWrapper<OrderAssociations>()
									.eq(OrderAssociations::getCompanyId, companyId)
									.eq(OrderAssociations::getOrderId, orderId)
									.last("LIMIT 1"));
			if (reloaded == null) {
				return null;
			}
			return orderAssociationAssociationDataAssembler.toAssociationDataMap(reloaded);
		} catch (Exception e) {
			return null;
		}
	}

	private List<Map<String, Object>> parseItemInfoJson(String raw) throws Exception {
		List<Map<String, Object>> list =
				objectMapper.readValue(raw.trim(), new TypeReference<List<Map<String, Object>>>() {});
		if (list == null) {
			throw new IllegalStateException("empty item_info");
		}
		return list;
	}

	private Map<String, Map<String, Object>> indexItemInfoBySkuId(List<Map<String, Object>> list) {
		Map<String, Map<String, Object>> bySku = new LinkedHashMap<>();
		for (Map<String, Object> entry : list) {
			Object sku = entry.get("sku_id");
			if (sku != null) {
				bySku.put(String.valueOf(sku), entry);
			}
		}
		return bySku;
	}
}
