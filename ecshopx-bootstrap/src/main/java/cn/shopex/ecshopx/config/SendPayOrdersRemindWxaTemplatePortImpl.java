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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.port.orders.SendPayOrdersRemindWxaTemplatePort;
import cn.shopex.ecshopx.promotions.service.WxaTemplateMsgActivityRemindSendService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SendPayOrdersRemindWxaTemplatePortImpl implements SendPayOrdersRemindWxaTemplatePort {

	private final WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService;

	public SendPayOrdersRemindWxaTemplatePortImpl(
			WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService) {
		this.wxaTemplateMsgActivityRemindSendService = wxaTemplateMsgActivityRemindSendService;
	}

	@Override
	public void sendPayOrdersRemind(Map<String, Object> orderData) {
		Map<String, Object> payload = new LinkedHashMap<>(8);
		payload.put("scenes_name", "payOrdersRemind");
		payload.put("company_id", orderData.get("company_id"));
		payload.put("appid", stringVal(orderData.get("wxa_appid")));
		payload.put("openid", stringVal(orderData.get("open_id")));

		String orderIdStr = stringVal(orderData.get("order_id"));
		if (StringUtils.hasText(orderIdStr)) {
			payload.put("page_query_str", "order_id=" + orderIdStr);
		}

		Map<String, Object> data = new LinkedHashMap<>(8);
		data.put("order_id", orderIdStr);
		data.put("pay_money", formatPayMoney(orderData));
		data.put("item_name", stringVal(orderData.get("title")));
		data.put("created", stringVal(orderData.get("create_time")));
		data.put("remarks", buildRemarks(orderData));
		payload.put("data", data);

		wxaTemplateMsgActivityRemindSendService.send(payload, true);
	}

	private static String buildRemarks(Map<String, Object> orderData) {
		Object autoCancel = orderData.get("auto_cancel_time");
		if (autoCancel != null && StringUtils.hasText(stringVal(autoCancel))) {
			return "请在 " + stringVal(autoCancel) + " 前完成支付";
		}
		return "请及时完成支付";
	}

	private static String formatPayMoney(Map<String, Object> orderData) {
		long fen = parseFeeToLong(orderData.get("total_fee"));
		String symbol = stringVal(orderData.get("fee_symbol"));
		if (!StringUtils.hasText(symbol)) {
			symbol = "￥";
		}
		double yuan = fen / 100.0;
		return symbol + String.format(java.util.Locale.ROOT, "%.2f", yuan);
	}

	private static long parseFeeToLong(Object v) {
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

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
