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
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderMarkDownPersistService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final ObjectMapper objectMapper;

	public AdminOrderMarkDownPersistService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			ObjectMapper objectMapper) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void persistAfterMarkdown(
			long companyId,
			Map<String, Object> oldOrderInfo,
			Map<String, Object> newOrderInfo,
			Map<String, Object> logParams,
			long operatorId,
			String operatorType) {
		boolean orderHasMarkDownKey = discountInfoHasOrderMarkDown(newOrderInfo.get("discount_info"));
		long oldTf = longVal(oldOrderInfo.get("total_fee"), 0L);
		long newTf = longVal(newOrderInfo.get("total_fee"), 0L);
		boolean shouldWrite = orderHasMarkDownKey || (newTf != oldTf);
		if (!shouldWrite) {
			return;
		}
		long orderIdNum = longVal(newOrderInfo.get("order_id"), 0L);
		if (orderIdNum <= 0L) {
			throw new ResourceException("订单号无效");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		try {
			patchOrderAssociations(orderIdNum, companyId, newTf, now);
			patchNormalOrder(companyId, orderIdNum, newOrderInfo, newTf, now);
			patchNormalOrderItems(companyId, orderIdNum, newOrderInfo, now);
			publishProcessLog(newOrderInfo, logParams, operatorId, operatorType);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage();
			throw new ResourceException(msg == null || msg.isEmpty() ? "订单更新失败" : msg);
		}
	}

	private void patchOrderAssociations(long orderIdNum, long companyId, long newTf, int now) {
		LambdaUpdateWrapper<OrderAssociations> u = new LambdaUpdateWrapper<>();
		u.eq(OrderAssociations::getOrderId, orderIdNum)
				.eq(OrderAssociations::getCompanyId, companyId)
				.set(OrderAssociations::getTotalFee, newTf)
				.set(OrderAssociations::getUpdateTime, now);
		orderAssociationsMapper.update(null, u);
	}

	private void patchNormalOrder(
			long companyId, long orderIdNum, Map<String, Object> newOrderInfo, long newTf, int now)
			throws JsonProcessingException {
		LambdaUpdateWrapper<NormalOrders> u =
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderIdNum);
		Object di = newOrderInfo.get("discount_info");
		if (shouldWriteDiscountInfoColumn(di)) {
			u.set(NormalOrders::getDiscountInfo, objectMapper.writeValueAsString(di));
		}
		u.set(NormalOrders::getTotalFee, String.valueOf(newTf))
				.set(NormalOrders::getDiscountFee, intVal(newOrderInfo.get("discount_fee"), 0))
				.set(NormalOrders::getFreightFee, intVal(newOrderInfo.get("freight_fee"), 0))
				.set(NormalOrders::getGetPoints, intValPoints(newOrderInfo.get("get_points")))
				.set(NormalOrders::getExtraPoints, intVal(newOrderInfo.get("extra_points"), 0))
				.set(NormalOrders::getUpdateTime, now);
		normalOrdersMapper.update(null, u);
	}

	@SuppressWarnings("unchecked")
	private void patchNormalOrderItems(
			long companyId, long orderIdNum, Map<String, Object> newOrderInfo, int now)
			throws JsonProcessingException {
		Object rawItems = newOrderInfo.get("items");
		if (!(rawItems instanceof List<?> list)) {
			return;
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> rawLine)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) rawLine;
			if (!itemHasMarkDownFlag(item.get("discount_info"))) {
				continue;
			}
			long lineId = longVal(item.get("id"), 0L);
			if (lineId <= 0L) {
				continue;
			}
			Object lineDi = item.get("discount_info");
			String diJson =
					(lineDi == null || !shouldWriteDiscountInfoColumn(lineDi))
							? null
							: objectMapper.writeValueAsString(lineDi);
			LambdaUpdateWrapper<NormalOrdersItems> u =
					new LambdaUpdateWrapper<NormalOrdersItems>()
							.eq(NormalOrdersItems::getCompanyId, companyId)
							.eq(NormalOrdersItems::getOrderId, orderIdNum)
							.eq(NormalOrdersItems::getId, lineId);
			if (diJson != null) {
				u.set(NormalOrdersItems::getDiscountInfo, diJson);
			}
			u.set(NormalOrdersItems::getTotalFee, intVal(item.get("total_fee"), 0))
					.set(NormalOrdersItems::getDiscountFee, intVal(item.get("discount_fee"), 0))
					.set(NormalOrdersItems::getGetPoints, intValPoints(item.get("get_points")))
					.set(NormalOrdersItems::getUpdateTime, now);
			normalOrdersItemsMapper.update(null, u);
		}
	}

	private void publishProcessLog(
			Map<String, Object> newOrderInfo,
			Map<String, Object> logParams,
			long operatorId,
			String operatorType) {
		Object oid = logParams.get("order_id");
		String orderLabel = oid == null ? "" : String.valueOf(oid);
		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", longVal(newOrderInfo.get("order_id"), 0L));
		log.put("company_id", longVal(newOrderInfo.get("company_id"), 0L));
		String ot = operatorType == null ? "" : operatorType.trim();
		log.put("operator_type", ot.isEmpty() ? "system" : ot);
		log.put("operator_id", operatorId);
		log.put("is_show", Boolean.TRUE);
		log.put("remarks", "订单改价");
		log.put("detail", "订单号：" + orderLabel + "，手动改价");
		log.put("params", new LinkedHashMap<>(logParams));
		orderProcessLogPublishPort.publish(log);
	}

	private static boolean discountInfoHasOrderMarkDown(Object o) {
		return o instanceof Map<?, ?> m && m.containsKey("mark_down");
	}

	private static boolean itemHasMarkDownFlag(Object o) {
		if (o instanceof Map<?, ?> m) {
			return m.containsKey("mark_down");
		}
		if (o instanceof List<?> lst) {
			for (Object el : lst) {
				if (!(el instanceof Map<?, ?> m)) {
					continue;
				}
				if ("mark_down".equals(m.get("type")) || m.containsKey("mark_down")) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean shouldWriteDiscountInfoColumn(Object di) {
		if (di == null) {
			return false;
		}
		if (di instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (di instanceof List<?> l) {
			return !l.isEmpty();
		}
		return true;
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intValPoints(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof BigDecimal b) {
			return b.setScale(0, RoundingMode.DOWN).intValue();
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return new BigDecimal(v.toString().trim()).setScale(0, RoundingMode.DOWN).intValue();
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
