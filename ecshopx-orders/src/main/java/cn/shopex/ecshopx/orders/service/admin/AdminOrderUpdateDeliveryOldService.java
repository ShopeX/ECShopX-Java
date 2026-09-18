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

import cn.shopex.ecshopx.common.dispatch.NormalOrderDeliveryDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Admin API to revise delivery details on already-shipped normal orders. On success, publishes an
 * order-process log entry and schedules {@code EVENT_NORMAL_ORDER_DELIVERY} after commit (same
 * dispatch publisher contract as {@link AdminNormalOrderDeliveryCoreService}).
 */
@Service
public class AdminOrderUpdateDeliveryOldService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher;
	private final ObjectMapper objectMapper;

	public AdminOrderUpdateDeliveryOldService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher,
			ObjectMapper objectMapper) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.orderAssociationAssociationDataAssembler = orderAssociationAssociationDataAssembler;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.normalOrderDeliveryDispatchPublisher = normalOrderDeliveryDispatchPublisher;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateDeliveryOld(
			long companyId, long operatorId, String pathOrderIdRaw, Map<String, Object> merged) {
		String raw = pathOrderIdRaw == null ? "" : pathOrderIdRaw.trim();
		if (raw.isEmpty()) {
			throw new ResourceException("此订单不存在！");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.apply("CAST(order_id AS CHAR) = {0}", raw)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}

		long dbOrderId = assoc.getOrderId();

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if (!isNormalPhysicalFamily(effective)) {
			throw new ResourceException("当前订单类型不支持发货");
		}

		String deliveryType = resolveDeliveryType(merged);
		if (!"batch".equals(deliveryType) && !"sep".equals(deliveryType)) {
			throw new ResourceException("订单发货类型必选");
		}

		if ("batch".equals(deliveryType)) {
			Object dc = merged.get("delivery_corp");
			Object codeObj = merged.get("delivery_code");
			String corp = dc == null ? "" : String.valueOf(dc).trim();
			String code = codeObj == null ? "" : String.valueOf(codeObj).trim();
			if (corp.isEmpty()) {
				throw new ResourceException("快递公司必填");
			}
			if (code.isEmpty()) {
				throw new ResourceException("快递单号必填");
			}
		} else {
			Object sepRaw = merged.get("sepInfo");
			if (sepRaw == null || String.valueOf(sepRaw).trim().isEmpty()) {
				throw new ResourceException("拆单信息必填");
			}
		}

		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, dbOrderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单号为" + raw + "的订单不存在");
		}
		if (!"WAIT_BUYER_CONFIRM".equals(order.getOrderStatus())) {
			throw new ResourceException("订单号为" + raw + "的订单未发货，不能修改发货信息");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);

		if ("sep".equals(deliveryType)) {
			String corpTop = mergedStringOrNull(merged, "delivery_corp");
			String codeTop = mergedStringOrNull(merged, "delivery_code");
			List<NormalOrdersItems> allItems =
					normalOrdersItemsMapper.selectList(
							new LambdaQueryWrapper<NormalOrdersItems>()
									.eq(NormalOrdersItems::getCompanyId, companyId)
									.eq(NormalOrdersItems::getOrderId, dbOrderId));
			if (allItems == null || allItems.isEmpty()) {
				throw new ResourceException("子订单不存在");
			}
			Set<Long> orderItemIds = new HashSet<>();
			for (NormalOrdersItems row : allItems) {
				if (row.getItemId() != null) {
					orderItemIds.add(row.getItemId());
				}
			}

			JsonNode sepRoot = parseSepInfoRoot(merged.get("sepInfo"));
			Set<Long> deliveryItems = new HashSet<>();
			for (JsonNode el : sepRoot) {
				if (!el.isObject()) {
					throw new ResourceException("拆单信息格式错误");
				}
				Map<String, Object> item =
						objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {});
				Long lineItemId = parseLineItemId(item.get("item_id"));
				if (lineItemId == null) {
					throw new ResourceException("拆单信息格式错误");
				}
				String ec = stringOrEmpty(item.get("delivery_corp"));
				String ed = stringOrEmpty(item.get("delivery_code"));
				if (ec.isEmpty() || ed.isEmpty()) {
					continue;
				}
				NormalOrdersItems sub =
						normalOrdersItemsMapper.selectOne(
								new LambdaQueryWrapper<NormalOrdersItems>()
										.eq(NormalOrdersItems::getCompanyId, companyId)
										.eq(NormalOrdersItems::getOrderId, dbOrderId)
										.eq(NormalOrdersItems::getItemId, lineItemId)
										.last("LIMIT 1"));
				if (sub == null) {
					throw new ResourceException(
							"订单号为" + raw + ",商品id为" + lineItemId + "的子订单不存在");
				}
				LambdaUpdateWrapper<NormalOrdersItems> iuw = new LambdaUpdateWrapper<>();
				iuw.eq(NormalOrdersItems::getCompanyId, companyId)
						.eq(NormalOrdersItems::getOrderId, dbOrderId)
						.eq(NormalOrdersItems::getItemId, lineItemId)
						.set(NormalOrdersItems::getDeliveryCorp, ec)
						.set(NormalOrdersItems::getDeliveryCode, ed)
						.set(NormalOrdersItems::getUpdateTime, now);
				normalOrdersItemsMapper.update(null, iuw);
				deliveryItems.add(lineItemId);
			}

			Set<Long> noupdateItem = new HashSet<>(orderItemIds);
			noupdateItem.removeAll(deliveryItems);

			LambdaUpdateWrapper<NormalOrders> nuw = new LambdaUpdateWrapper<>();
			nuw.eq(NormalOrders::getCompanyId, companyId).eq(NormalOrders::getOrderId, dbOrderId);
			nuw.set(NormalOrders::getDeliveryCorp, corpTop);
			nuw.set(NormalOrders::getDeliveryCode, codeTop);
			if (!noupdateItem.isEmpty()) {
				nuw.set(NormalOrders::getDeliveryStatus, "PARTAIL");
			}
			nuw.set(NormalOrders::getUpdateTime, now);
			normalOrdersMapper.update(null, nuw);

			LambdaUpdateWrapper<OrderAssociations> auw = new LambdaUpdateWrapper<>();
			auw.eq(OrderAssociations::getCompanyId, companyId)
					.eq(OrderAssociations::getOrderId, dbOrderId);
			auw.set(OrderAssociations::getDeliveryCorp, corpTop);
			auw.set(OrderAssociations::getDeliveryCode, codeTop);
			if (!noupdateItem.isEmpty()) {
				auw.set(OrderAssociations::getDeliveryStatus, "PARTAIL");
			}
			auw.set(OrderAssociations::getUpdateTime, now);
			int rows = orderAssociationsMapper.update(null, auw);
			if (rows != 1) {
				throw new ResourceException("订单关联信息不存在");
			}
		} else {
			Object dc = merged.get("delivery_corp");
			Object codeObj = merged.get("delivery_code");
			String corp = dc == null ? "" : String.valueOf(dc).trim();
			String code = codeObj == null ? "" : String.valueOf(codeObj).trim();

			LambdaUpdateWrapper<NormalOrdersItems> iuw = new LambdaUpdateWrapper<>();
			iuw.eq(NormalOrdersItems::getCompanyId, companyId)
					.eq(NormalOrdersItems::getOrderId, dbOrderId)
					.set(NormalOrdersItems::getDeliveryCorp, corp)
					.set(NormalOrdersItems::getDeliveryCode, code)
					.set(NormalOrdersItems::getUpdateTime, now);
			normalOrdersItemsMapper.update(null, iuw);

			LambdaUpdateWrapper<NormalOrders> nuw = new LambdaUpdateWrapper<>();
			nuw.eq(NormalOrders::getCompanyId, companyId)
					.eq(NormalOrders::getOrderId, dbOrderId)
					.set(NormalOrders::getDeliveryCorp, corp)
					.set(NormalOrders::getDeliveryCode, code)
					.set(NormalOrders::getUpdateTime, now);
			normalOrdersMapper.update(null, nuw);

			LambdaUpdateWrapper<OrderAssociations> auw = new LambdaUpdateWrapper<>();
			auw.eq(OrderAssociations::getCompanyId, companyId)
					.eq(OrderAssociations::getOrderId, dbOrderId)
					.set(OrderAssociations::getDeliveryCorp, corp)
					.set(OrderAssociations::getDeliveryCode, code)
					.set(OrderAssociations::getUpdateTime, now);
			int rows = orderAssociationsMapper.update(null, auw);
			if (rows != 1) {
				throw new ResourceException("订单关联信息不存在");
			}
		}

		OrderAssociations reloaded =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, dbOrderId)
								.last("LIMIT 1"));
		if (reloaded == null) {
			throw new ResourceException("订单关联信息不存在");
		}

		LinkedHashMap<String, Object> logParams = new LinkedHashMap<>(merged);
		logParams.put("company_id", companyId);
		logParams.put("operator_type", "admin");
		logParams.put("operator_id", operatorId);

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", dbOrderId);
		log.put("company_id", companyId);
		log.put("operator_type", "admin");
		log.put("operator_id", operatorId);
		log.put("remarks", "订单发货");
		log.put("detail", "订单号：" + raw + "，订单发货信息修改");
		log.put("params", logParams);
		orderProcessLogPublishPort.publish(log);

		Map<String, Object> normalDeliveryPayload = new LinkedHashMap<>();
		normalDeliveryPayload.put("order_id", dbOrderId);
		normalDeliveryPayload.put("company_id", companyId);
		scheduleNormalOrderDeliveryDispatchAfterCommit(normalDeliveryPayload);

		return orderAssociationAssociationDataAssembler.toAssociationDataMap(reloaded);
	}

	private void scheduleNormalOrderDeliveryDispatchAfterCommit(Map<String, Object> normalDeliveryPayload) {
		Runnable publish = () -> normalOrderDeliveryDispatchPublisher.publish(normalDeliveryPayload);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							publish.run();
						}
					});
		} else {
			publish.run();
		}
	}

	private JsonNode parseSepInfoRoot(Object sepRaw) {
		try {
			JsonNode root;
			if (sepRaw instanceof String s) {
				if (s.trim().isEmpty()) {
					throw new ResourceException("拆单信息格式错误");
				}
				root = objectMapper.readTree(s);
			} else {
				root = objectMapper.valueToTree(sepRaw);
			}
			if (root == null || !root.isArray()) {
				throw new ResourceException("拆单信息格式错误");
			}
			return root;
		} catch (JsonProcessingException e) {
			throw new ResourceException("拆单信息格式错误");
		}
	}

	private static Long parseLineItemId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stringOrEmpty(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static String mergedStringOrNull(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key)) {
			return null;
		}
		Object v = merged.get(key);
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static String resolveDeliveryType(Map<String, Object> merged) {
		Object dt = merged.get("delivery_type");
		if (dt == null) {
			return "batch";
		}
		String trim = String.valueOf(dt).trim();
		if (trim.isEmpty() || "0".equals(trim)) {
			return "batch";
		}
		return trim;
	}

	private static boolean isNormalPhysicalFamily(String effective) {
		if (effective == null || effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "normal_groups".equals(effective)
				|| "normal_drug".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_") && !"membercard".equals(effective);
	}
}
