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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverV2FailException;
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
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderDeliverService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;
	private final OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2OrderDeliverService(
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

	public Map<String, Object> executeOpenapiDeliverV2(
			long companyId, String orderIdRaw, String itemInfoRaw, String lcCode, String lCode) {
		List<Map<String, Object>> itemInfoList = parseItemInfoFormat(itemInfoRaw);
		validateParamsV2(orderIdRaw, itemInfoList, lcCode, lCode);

		long orderId;
		try {
			orderId = Long.parseLong(orderIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new OpenapiOrderDeliverV2FailException(
					OpenapiErrorCode.ORDER_NOT_FOUND, "订单相应的明细不存在");
		}

		NormalOrders tradeInfo =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (tradeInfo == null) {
			throw new OpenapiOrderDeliverV2FailException(
					OpenapiErrorCode.ORDER_NOT_FOUND, "订单相应的明细不存在");
		}
		if ("DONE".equals(tradeInfo.getDeliveryStatus())) {
			throw new OpenapiOrderDeliverV2FailException(
					OpenapiErrorCode.ORDER_HANDLE_EXIST, "订单已发货，请勿重复发货");
		}

		List<NormalOrdersItems> itemRows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.orderByAsc(NormalOrdersItems::getId));
		Map<String, Object> order = buildOrderMap(tradeInfo, itemRows);
		if (order.isEmpty()) {
			throw new OpenapiOrderDeliverV2FailException(
					OpenapiErrorCode.ORDER_NOT_FOUND, "订单相应的明细不存在");
		}

		return doOrderDeliveryV2(order, itemInfoList, lcCode, lCode, companyId, orderId);
	}

	private List<Map<String, Object>> parseItemInfoFormat(String itemInfoRaw) {
		if (!StringUtils.hasText(itemInfoRaw)) {
			throw formatError();
		}
		try {
			List<Map<String, Object>> list =
					objectMapper.readValue(
							itemInfoRaw.trim(), new TypeReference<List<Map<String, Object>>>() {});
			if (list == null || list.isEmpty()) {
				throw formatError();
			}
			for (Object element : list) {
				if (!(element instanceof Map<?, ?>)) {
					throw formatError();
				}
			}
			return list;
		} catch (OpenapiOrderDeliverV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw formatError();
		}
	}

	private OpenapiOrderDeliverV2FailException formatError() {
		return new OpenapiOrderDeliverV2FailException(
				OpenapiErrorCode.SERVICE_PARAMS_FORMAT_ERROR, "发货商品信息格式错误");
	}

	private void validateParamsV2(
			String orderIdRaw, List<Map<String, Object>> itemInfoList, String lcCode, String lCode) {
		if (!StringUtils.hasText(orderIdRaw)) {
			throw missingParams("订单号必填");
		}
		for (Map<String, Object> item : itemInfoList) {
			if (isMissingRequired(item.get("name"))) {
				throw missingParams("发货商品信息的商品名称必填");
			}
			if (isMissingRequired(item.get("sku_id"))) {
				throw missingParams("发货商品信息的商品货号必填");
			}
			if (isMissingRequired(item.get("qty"))) {
				throw missingParams("发货商品信息的发货数量必填");
			}
		}
		if (!StringUtils.hasText(lcCode)) {
			throw missingParams("快递公司编码必填");
		}
		if (!StringUtils.hasText(lCode)) {
			throw missingParams("快递单编码必填");
		}
	}

	private OpenapiOrderDeliverV2FailException missingParams(String message) {
		return new OpenapiOrderDeliverV2FailException(
				OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private boolean isMissingRequired(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String s) {
			return s.trim().isEmpty();
		}
		return false;
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

	private Map<String, Object> doOrderDeliveryV2(
			Map<String, Object> order,
			List<Map<String, Object>> itemInfoList,
			String lcCode,
			String lCode,
			long companyId,
			long orderId) {
		try {
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
				throw handleError("发货商品有误");
			}

			List<Map<String, Object>> sepInfo = noDelivery;
			if (sepInfo.isEmpty()) {
				throw handleError("订单处理错误");
			}

			OrderAssociations assoc =
					orderAssociationsMapper.selectOne(
							new LambdaQueryWrapper<OrderAssociations>()
									.eq(OrderAssociations::getCompanyId, companyId)
									.eq(OrderAssociations::getOrderId, orderId)
									.last("LIMIT 1"));
			if (assoc == null) {
				throw handleError("订单处理错误");
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
				throw handleError("订单处理错误");
			}
			return orderAssociationAssociationDataAssembler.toAssociationDataMap(reloaded);
		} catch (OpenapiOrderDeliverV2FailException e) {
			throw e;
		} catch (ResourceException e) {
			throw handleError(e.getMessage());
		} catch (Exception e) {
			throw handleError(e.getMessage());
		}
	}

	private OpenapiOrderDeliverV2FailException handleError(String message) {
		return new OpenapiOrderDeliverV2FailException(OpenapiErrorCode.ORDER_HANDLE_ERROR, message);
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
