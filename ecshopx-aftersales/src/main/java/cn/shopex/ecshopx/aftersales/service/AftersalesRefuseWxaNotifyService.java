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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefuseWxaSubscribeSendPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort.MiniProgramTradeRecipient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aligns with PHP {@code AftersalesService::sendWxaTemplateMsg} for scene {@code aftersalesRefuse}.
 */
@Service
public class AftersalesRefuseWxaNotifyService {

	private static final Logger log = LoggerFactory.getLogger(AftersalesRefuseWxaNotifyService.class);

	private final OrderTradeMiniProgramRecipientLookupPort orderTradeMiniProgramRecipientLookupPort;
	private final AftersalesRefuseWxaSubscribeSendPort aftersalesRefuseWxaSubscribeSendPort;

	public AftersalesRefuseWxaNotifyService(
			OrderTradeMiniProgramRecipientLookupPort orderTradeMiniProgramRecipientLookupPort,
			AftersalesRefuseWxaSubscribeSendPort aftersalesRefuseWxaSubscribeSendPort) {
		this.orderTradeMiniProgramRecipientLookupPort = orderTradeMiniProgramRecipientLookupPort;
		this.aftersalesRefuseWxaSubscribeSendPort = aftersalesRefuseWxaSubscribeSendPort;
	}

	public void sendByStatus(int aftersalesStatus, Map<String, Object> aftersalesLike) {
		try {
			if (aftersalesLike == null || aftersalesLike.isEmpty()) {
				return;
			}
			long companyId = toLong(aftersalesLike.get("company_id"));
			long orderId = toLong(aftersalesLike.get("order_id"));
			if (companyId <= 0L || orderId <= 0L) {
				return;
			}

			String type = stringVal(aftersalesLike.get("aftersales_type"));
			String statusKey = resolveStatusKey(aftersalesStatus, type);
			if (statusKey == null) {
				return;
			}

			String refuseReason = firstNonEmpty(
					aftersalesLike.get("refuse_reason"), aftersalesLike.get("refunds_memo"));
			String remarks;
			switch (statusKey) {
				case "SELLER_REFUSE_BUYER" -> remarks = refuseReason;
				case "WAIT_BUYER_RETURN_GOODS" -> remarks = "商家已同意售后申请，请进行退货回寄处理";
				case "REFUND_CLOSED" -> remarks = refuseReason;
				case "REFUND_SUCCESS" -> remarks = "商家已同意退款";
				default -> {
					return;
				}
			}

			Optional<MiniProgramTradeRecipient> recipient =
					orderTradeMiniProgramRecipientLookupPort.findRecipient(companyId, orderId);
			if (recipient.isEmpty()) {
				return;
			}
			MiniProgramTradeRecipient r = recipient.get();

			int refundFen = resolveRefundFen(aftersalesLike);
			String refundYuan =
					BigDecimal.valueOf(refundFen)
							.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
							.toPlainString();

			Map<String, Object> data = new LinkedHashMap<>(8);
			data.put("order_id", String.valueOf(orderId));
			data.put("refund_fee", refundYuan);
			data.put("remarks", remarks == null ? "" : remarks);

			Map<String, Object> payload = new LinkedHashMap<>(8);
			payload.put("scenes_name", "aftersalesRefuse");
			payload.put("company_id", companyId);
			payload.put("appid", r.wxaAppid());
			payload.put("openid", r.openId());
			payload.put("data", data);

			aftersalesRefuseWxaSubscribeSendPort.send(payload, true);
		} catch (RuntimeException e) {
			log.debug("aftersalesRefuse wxa subscribe notify skipped: {}", e.toString());
		}
	}

	/** Auto-refuse path: always seller refuse buyer for REFUND_GOODS. */
	public void sendSellerRefuseBuyer(Map<String, Object> templateData) {
		if (templateData == null) {
			return;
		}
		Map<String, Object> copy = new LinkedHashMap<>(templateData);
		if (!copy.containsKey("refuse_reason") || !StringUtils.hasText(stringVal(copy.get("refuse_reason")))) {
			copy.put("refuse_reason", stringVal(copy.get("refuse_reason")));
		}
		sendByStatus(3, copy);
	}

	private static String resolveStatusKey(int status, String aftersalesType) {
		String statusStr = String.valueOf(status);
		String key = statusStr;
		if ("1".equals(statusStr) && "REFUND_GOODS".equals(aftersalesType)) {
			key = "WAIT_BUYER_RETURN_GOODS";
		} else if ("1".equals(statusStr) && "ONLY_REFUND".equals(aftersalesType)) {
			key = "REFUND_SUCCESS";
		} else if ("2".equals(statusStr)) {
			key = "REFUND_SUCCESS";
		} else if ("3".equals(statusStr) && "REFUND_GOODS".equals(aftersalesType)) {
			key = "SELLER_REFUSE_BUYER";
		} else if ("3".equals(statusStr) && "ONLY_REFUND".equals(aftersalesType)) {
			key = "REFUND_CLOSED";
		} else if ("3".equals(statusStr)) {
			key = "SELLER_REFUSE_BUYER";
		} else {
			return null;
		}
		return key;
	}

	private static int resolveRefundFen(Map<String, Object> m) {
		Object raw = m.get("refund_amount");
		if (raw == null) {
			raw = m.get("refund_fee");
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(stringVal(raw));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String firstNonEmpty(Object a, Object b) {
		String sa = stringVal(a);
		if (StringUtils.hasText(sa)) {
			return sa;
		}
		return stringVal(b);
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
