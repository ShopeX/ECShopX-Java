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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpDeliveryOrderPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpSepDeliveryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({SaasErpDeliveryOrderPort.class, SaasErpSepDeliveryPort.class})
public class CreateDeliverySaasErpAct implements SaasErpActHandler {

	private final SaasErpDeliveryOrderPort deliveryOrderPort;
	private final SaasErpSepDeliveryPort sepDeliveryPort;
	private final ObjectMapper objectMapper;

	public CreateDeliverySaasErpAct(
			SaasErpDeliveryOrderPort deliveryOrderPort,
			SaasErpSepDeliveryPort sepDeliveryPort,
			ObjectMapper objectMapper) {
		this.deliveryOrderPort = deliveryOrderPort;
		this.sepDeliveryPort = sepDeliveryPort;
		this.objectMapper = objectMapper;
	}

	@Override
	public String act() {
		return "ome_create_delivery";
	}

	@Override
	public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
		String orderId = params.get("order_id");
		if (orderId == null || orderId.isBlank()) {
			return SaasErpInboundResult.fail("订单号缺少！");
		}
		orderId = orderId.trim();

		String logiNo = params.get("logi_no");
		if (logiNo == null || logiNo.isBlank()) {
			return SaasErpInboundResult.fail("缺少物流单号");
		}

		String structRaw = params.get("struct");
		if (structRaw == null || structRaw.isBlank()) {
			return SaasErpInboundResult.fail("缺少发货商品");
		}

		String deliveryType = params.get("delivery_type");
		if ("delivery".equals(deliveryType)) {
			String logiCode = params.get("logi_code");
			if (logiCode == null || logiCode.isBlank()) {
				return SaasErpInboundResult.fail("缺少物流编码");
			}
		}

		if ("return".equals(deliveryType)) {
			return SaasErpInboundResult.ok("操作成功", Boolean.TRUE);
		}
		if (!"delivery".equals(deliveryType)) {
			String shown = deliveryType == null ? "" : deliveryType;
			return SaasErpInboundResult.fail("不支持发货类型：" + shown);
		}

		List<Map<String, Object>> structRows;
		try {
			structRows = objectMapper.readValue(structRaw, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			return SaasErpInboundResult.fail("操作失败");
		}
		if (structRows == null) {
			return SaasErpInboundResult.fail("操作失败");
		}

		Optional<Map<String, Object>> orderOpt = deliveryOrderPort.load(companyId, orderId);
		if (orderOpt.isEmpty()) {
			return SaasErpInboundResult.fail("此订单不存在");
		}
		Map<String, Object> order = orderOpt.get();
		if ("DONE".equals(String.valueOf(order.get("delivery_status")))) {
			return SaasErpInboundResult.fail("订单已发货，请勿重复发货");
		}

		try {
			return doOrderDelivery(order, structRows, params, companyId, orderId, logiNo);
		} catch (RuntimeException e) {
			StackTraceElement[] stack = e.getStackTrace();
			int line = stack.length > 0 ? stack[0].getLineNumber() : 0;
			return SaasErpInboundResult.fail(line + ",msg=>" + e.getMessage());
		}
	}

	private SaasErpInboundResult doOrderDelivery(
			Map<String, Object> order,
			List<Map<String, Object>> structRows,
			Map<String, String> params,
			long callbackCompanyId,
			String orderId,
			String logiNo) {
		Set<String> deliveryBn = new HashSet<>();
		Map<String, Double> deliveryNum = new HashMap<>();
		for (Map<String, Object> row : structRows) {
			Object bnRaw = row.get("product_bn");
			if (bnRaw == null) {
				continue;
			}
			String bn = String.valueOf(bnRaw);
			deliveryBn.add(bn);
			double number = toDouble(row.get("number"));
			deliveryNum.merge(bn, number, Double::sum);
		}

		String logiName = params.containsKey("logi_name") ? params.get("logi_name") : null;
		String shipMobile = params.containsKey("ship_mobile") ? params.get("ship_mobile") : null;
		String deliveryCode = logiNo;
		String deliveryCorp = params.get("logi_code");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items =
				order.get("items") instanceof List<?> list
						? (List<Map<String, Object>>) list
						: List.of();

		List<Map<String, Object>> noDelivery = new ArrayList<>();
		List<Map<String, Object>> emptyDelivery = new ArrayList<>();

		for (Map<String, Object> item : items) {
			if (!"PENDING".equals(String.valueOf(item.get("delivery_status")))) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>(item);
			double deliveryItemNum = toDouble(line.get("delivery_item_num"));
			double num = toDouble(line.get("num"));
			String itemBn = String.valueOf(line.get("item_bn"));

			Double requested = deliveryNum.get(itemBn);
			if (requested != null && requested > 0) {
				double shipNum = requested;
				double remaining = num - deliveryItemNum;
				if (remaining < shipNum) {
					shipNum = remaining;
				}
				deliveryNum.put(itemBn, requested - shipNum);
				deliveryItemNum += shipNum;
				// Match PHP: delivery_num must stringify as a whole positive int
				// ("1", not "1.0") for OrderDeliveryService's /^[1-9][0-9]*$/ check.
				int shipNumInt = (int) Math.round(shipNum);
				line.put("delivery_item_num", (int) Math.round(deliveryItemNum));
				line.put("delivery_num", shipNumInt);
			}

			if (deliveryItemNum >= num) {
				line.put("delivery_status", "DONE");
			}

			if (deliveryBn.contains(itemBn)) {
				line.put("delivery_code", deliveryCode);
				line.put("delivery_corp", deliveryCorp);
				noDelivery.add(line);
			} else {
				emptyDelivery.add(line);
			}
		}

		if (noDelivery.isEmpty() && !emptyDelivery.isEmpty()) {
			return SaasErpInboundResult.fail("发货商品有误");
		}

		List<Map<String, Object>> sepInfo = noDelivery;
		if (sepInfo.isEmpty()) {
			return SaasErpInboundResult.fail("订单已经发货完成");
		}

		long deliverCompanyId = resolveCompanyId(order.get("company_id"), callbackCompanyId);
		Map<String, Object> deliveryParams = new LinkedHashMap<>();
		deliveryParams.put("company_id", deliverCompanyId);
		deliveryParams.put("logi_name", logiName);
		deliveryParams.put("ship_mobile", shipMobile);
		deliveryParams.put("delivery_corp", deliveryCorp);
		deliveryParams.put("delivery_code", deliveryCode);
		deliveryParams.put("delivery_type", "sep");
		deliveryParams.put("order_id", order.get("order_id"));
		try {
			deliveryParams.put("sepInfo", objectMapper.writeValueAsString(sepInfo));
		} catch (JsonProcessingException e) {
			return SaasErpInboundResult.fail("操作失败");
		}
		deliveryParams.put("type", "new");

		Object result = sepDeliveryPort.deliver(deliveryParams);
		if (isTruthy(result)) {
			return SaasErpInboundResult.ok("操作成功", result);
		}
		return SaasErpInboundResult.fail("操作失败");
	}

	static long resolveCompanyId(Object orderCompanyId, long callbackCompanyId) {
		if (orderCompanyId instanceof Number number) {
			return number.longValue();
		}
		if (orderCompanyId != null) {
			try {
				return Long.parseLong(String.valueOf(orderCompanyId).trim());
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		return callbackCompanyId;
	}

	static boolean isTruthy(Object value) {
		if (value == null) {
			return false;
		}
		if (Boolean.FALSE.equals(value)) {
			return false;
		}
		if (value instanceof Number number) {
			return number.longValue() != 0L;
		}
		if (value instanceof String s) {
			return !s.isEmpty();
		}
		return true;
	}

	private static double toDouble(Object raw) {
		if (raw == null) {
			return 0d;
		}
		if (raw instanceof Number number) {
			return number.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(raw));
		} catch (NumberFormatException e) {
			return 0d;
		}
	}
}
