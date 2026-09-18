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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.port.orders.SendPayOrdersRemindWxaTemplatePort;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SendPayOrdersRemindJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(SendPayOrdersRemindJobHandler.class);

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SendPayOrdersRemindWxaTemplatePort sendPayOrdersRemindWxaTemplatePort;

	public SendPayOrdersRemindJobHandler(
			OrderAssociationsMapper orderAssociationsMapper,
			SendPayOrdersRemindWxaTemplatePort sendPayOrdersRemindWxaTemplatePort) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.sendPayOrdersRemindWxaTemplatePort = sendPayOrdersRemindWxaTemplatePort;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			Object raw = payload.get("orderData");
			if (!(raw instanceof Map<?, ?>)) {
				return;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> orderData = (Map<String, Object>) raw;
			handleOrderData(orderData);
		} catch (RuntimeException e) {
			log.debug("SendPayOrdersRemindJob failed: {}", e.toString());
		}
	}

	private void handleOrderData(Map<String, Object> orderData) {
		Long companyId = parseLong(orderData.get("company_id"));
		Long orderId = parseLong(orderData.get("order_id"));
		if (companyId == null || orderId == null) {
			return;
		}

		OrderAssociations row =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (row == null) {
			return;
		}
		String status = row.getOrderStatus();
		if (status == null || !"NOTPAY".equals(status.trim())) {
			return;
		}

		sendPayOrdersRemindWxaTemplatePort.sendPayOrdersRemind(orderData);
	}

	private static Long parseLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
