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
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpNormalOrderLookupPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpOrderStructPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({SaasErpOrderStructPort.class, SaasErpNormalOrderLookupPort.class})
public class GetOrdersInfoSaasErpAct implements SaasErpActHandler {

	private static final Set<String> ALLOWED_ORDER_CLASSES =
			Set.of("normal", "groups", "seckill", "shopadmin", "shopguide");

	private static final String PROMOTION_DETAIL_NULL_JSON = "{\"promotiondetail\":null}";

	private final SaasErpNormalOrderLookupPort normalOrderLookupPort;
	private final SaasErpOrderStructPort orderStructPort;
	private final ObjectMapper objectMapper;

	public GetOrdersInfoSaasErpAct(
			SaasErpNormalOrderLookupPort normalOrderLookupPort,
			SaasErpOrderStructPort orderStructPort,
			ObjectMapper objectMapper) {
		this.normalOrderLookupPort = normalOrderLookupPort;
		this.orderStructPort = orderStructPort;
		this.objectMapper = objectMapper;
	}

	@Override
	public String act() {
		return "get_orders_info";
	}

	@Override
	public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
		String orderId = params.get("order_id");
		if (orderId == null || orderId.isBlank()) {
			return SaasErpInboundResult.fail("订单号缺少！");
		}
		orderId = orderId.trim();

		Optional<Map<String, Object>> orderRow = normalOrderLookupPort.find(companyId, orderId);
		if (orderRow.isEmpty()) {
			return SaasErpInboundResult.fail("未找到订单");
		}

		Object orderClassRaw = orderRow.get().get("order_class");
		String orderClass = orderClassRaw == null ? null : String.valueOf(orderClassRaw);
		if (orderClass == null || !ALLOWED_ORDER_CLASSES.contains(orderClass)) {
			String shown = orderClass == null ? "null" : orderClass;
			return SaasErpInboundResult.fail(shown + "类型订单禁止同步");
		}

		String sourceType = resolveSourceType(orderClass);
		Optional<Map<String, Object>> structOpt =
				orderStructPort.getOrderStruct(companyId, orderId, sourceType);
		if (structOpt.isEmpty()) {
			return SaasErpInboundResult.fail("获取订单信息失败");
		}

		Map<String, Object> trade = new LinkedHashMap<>(structOpt.get());
		trade.put("promotion_details", rewrapPromotionDetails(trade.get("promotion_details")));

		Map<String, Object> info = Map.of("trade", trade);
		return SaasErpInboundResult.ok("操作成功", info);
	}

	static String resolveSourceType(String orderClass) {
		if (orderClass.startsWith("normal")) {
			return orderClass;
		}
		return "normal_" + orderClass;
	}

	private String rewrapPromotionDetails(Object raw) {
		if (!(raw instanceof String json) || json.isBlank()) {
			return PROMOTION_DETAIL_NULL_JSON;
		}
		try {
			Object decoded = objectMapper.readValue(json, Object.class);
			return objectMapper.writeValueAsString(Map.of("promotiondetail", decoded));
		} catch (JsonProcessingException e) {
			return PROMOTION_DETAIL_NULL_JSON;
		}
	}
}
