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

package cn.shopex.ecshopx.orders.integration.notify;

import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort.MiniProgramTradeRecipient;
import cn.shopex.ecshopx.common.port.orders.OrderDeliverySuccessWxaSubscribeSendPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aligns with PHP {@code AbstractNormalOrder::sendDeliverySuccNotice}: build {@code orderDeliverySucc}
 * subscribe payload and send via promotions template sender.
 */
@Service("orderDeliverySuccessNotifyWechat")
@ConditionalOnProperty(
		name = "ecshopx.orders.delivery-success-notify.enabled",
		havingValue = "true",
		matchIfMissing = true)
public class OrderDeliverySuccessNotifyWechatService implements OrderDeliverySuccessNotifyPort {

	private static final Logger log = LoggerFactory.getLogger(OrderDeliverySuccessNotifyWechatService.class);

	private static final String SCENES_NAME = "orderDeliverySucc";

	private final OrderTradeMiniProgramRecipientLookupPort orderTradeMiniProgramRecipientLookupPort;
	private final OrderDeliverySuccessWxaSubscribeSendPort orderDeliverySuccessWxaSubscribeSendPort;
	private final NormalOrdersMapper normalOrdersMapper;

	public OrderDeliverySuccessNotifyWechatService(
			OrderTradeMiniProgramRecipientLookupPort orderTradeMiniProgramRecipientLookupPort,
			OrderDeliverySuccessWxaSubscribeSendPort orderDeliverySuccessWxaSubscribeSendPort,
			NormalOrdersMapper normalOrdersMapper) {
		this.orderTradeMiniProgramRecipientLookupPort = orderTradeMiniProgramRecipientLookupPort;
		this.orderDeliverySuccessWxaSubscribeSendPort = orderDeliverySuccessWxaSubscribeSendPort;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Override
	public void sendDeliverySuccessNotice(Map<String, Object> context) {
		try {
			if (context == null || context.isEmpty()) {
				return;
			}
			long companyId = toLong(context.get("company_id"));
			long orderId = toLong(context.get("order_id"));
			if (companyId <= 0L || orderId <= 0L) {
				return;
			}

			Optional<MiniProgramTradeRecipient> recipient =
					orderTradeMiniProgramRecipientLookupPort.findRecipient(companyId, orderId);
			if (recipient.isEmpty()) {
				return;
			}
			MiniProgramTradeRecipient r = recipient.get();

			String itemName = stringVal(context.get("item_name"));
			if (!StringUtils.hasText(itemName)) {
				itemName = resolveOrderTitle(companyId, orderId);
			}

			Map<String, Object> data = new LinkedHashMap<>(8);
			data.put("order_id", String.valueOf(orderId));
			data.put("delivery_corp", resolveDeliveryCorpDisplayName(context));
			data.put("delivery_code", stringVal(context.get("delivery_code")));
			data.put("item_name", itemName);

			Map<String, Object> payload = new LinkedHashMap<>(8);
			payload.put("scenes_name", SCENES_NAME);
			payload.put("company_id", companyId);
			payload.put("appid", r.wxaAppid());
			payload.put("openid", r.openId());
			payload.put("data", data);

			orderDeliverySuccessWxaSubscribeSendPort.send(payload, true);
		} catch (RuntimeException e) {
			log.debug("orderDeliverySucc wxa subscribe notify skipped: {}", e.toString());
		}
	}

	private String resolveOrderTitle(long companyId, long orderId) {
		NormalOrders row =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (row == null || !StringUtils.hasText(row.getTitle())) {
			return "";
		}
		return row.getTitle().trim();
	}

	private static String resolveDeliveryCorpDisplayName(Map<String, Object> context) {
		String logiName = stringVal(context.get("logi_name"));
		if (StringUtils.hasText(logiName)) {
			return logiName;
		}
		String corpName = stringVal(context.get("delivery_corp_name"));
		if (StringUtils.hasText(corpName)) {
			return corpName;
		}
		String corpCode = stringVal(context.get("delivery_corp"));
		return StringUtils.hasText(corpCode) ? corpCode : "其他";
	}

	private static long toLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
