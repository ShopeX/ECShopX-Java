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

package cn.shopex.ecshopx.orders.service.notify;

import cn.shopex.ecshopx.common.port.orders.PaymentMsgNotifyWebSocketSendPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeFinishNotifyPushBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeFinishNotifyPushBusService.class);
	private static final DateTimeFormatter PAY_DATE =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final PaymentMsgNotifyWebSocketSendPort paymentMsgNotifyWebSocketSendPort;

	public TradeFinishNotifyPushBusService(PaymentMsgNotifyWebSocketSendPort paymentMsgNotifyWebSocketSendPort) {
		this.paymentMsgNotifyWebSocketSendPort = paymentMsgNotifyWebSocketSendPort;
	}

	public void handlePaymentNotifyTradeFinishRow(Map<String, Object> snakeCaseTradeRow) {
		if (snakeCaseTradeRow == null || snakeCaseTradeRow.isEmpty()) {
			return;
		}
		Object rawPayType = snakeCaseTradeRow.get("pay_type");
		String payType = rawPayType == null ? "" : String.valueOf(rawPayType).trim();
		if ("point".equalsIgnoreCase(payType) || "deposit".equalsIgnoreCase(payType)) {
			return;
		}
		int payFeeCents = parsePayFeeCents(snakeCaseTradeRow.get("pay_fee"));
		if (payFeeCents <= 0) {
			return;
		}
		Object shopIdRaw = snakeCaseTradeRow.get("shop_id");
		String shopId = shopIdRaw == null ? "" : String.valueOf(shopIdRaw).trim();
		Object timeStart = snakeCaseTradeRow.get("time_start");
		String payDate = formatPayDate(timeStart);
		BigDecimal payFeeYuan =
				BigDecimal.valueOf(payFeeCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

		Map<String, Object> envelope = new LinkedHashMap<>(4);
		envelope.put("payFee", payFeeYuan);
		envelope.put("payType", StringUtils.hasText(payType) ? payType : "");
		envelope.put("shopId", shopId);
		envelope.put("payDate", payDate);

		try {
			paymentMsgNotifyWebSocketSendPort.sendPaymentNotify(envelope);
		} catch (Throwable t) {
			log.debug("websocket paymentnotify service Error: {}", t.toString());
		}
	}

	private static int parsePayFeeCents(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String formatPayDate(Object timeStart) {
		if (timeStart == null) {
			return "";
		}
		String s = String.valueOf(timeStart).trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		if (s.chars().allMatch(Character::isDigit)) {
			try {
				long sec = Long.parseLong(s);
				return PAY_DATE.format(Instant.ofEpochSecond(sec));
			} catch (NumberFormatException e) {
				return s;
			}
		}
		return s;
	}
}
