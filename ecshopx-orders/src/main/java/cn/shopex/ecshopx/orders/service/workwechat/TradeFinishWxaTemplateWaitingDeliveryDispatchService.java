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

package cn.shopex.ecshopx.orders.service.workwechat;

import cn.shopex.ecshopx.common.dispatch.SendWaitingDeliveryNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.orders.TradeFinishPaymentSuccShopNamePort;
import cn.shopex.ecshopx.common.port.orders.TradeFinishPaymentSuccWxaSubscribeSendPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
public class TradeFinishWxaTemplateWaitingDeliveryDispatchService {

	private static final Logger log =
			LoggerFactory.getLogger(TradeFinishWxaTemplateWaitingDeliveryDispatchService.class);

	private static final DateTimeFormatter PAY_INSTANT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final NormalOrdersMapper normalOrdersMapper;
	private final SendWaitingDeliveryNoticeJobDispatchPublisher sendWaitingDeliveryNoticeJobDispatchPublisher;
	private final TradeFinishPaymentSuccWxaSubscribeSendPort tradeFinishPaymentSuccWxaSubscribeSendPort;
	private final TradeFinishPaymentSuccShopNamePort tradeFinishPaymentSuccShopNamePort;

	public TradeFinishWxaTemplateWaitingDeliveryDispatchService(
			NormalOrdersMapper normalOrdersMapper,
			SendWaitingDeliveryNoticeJobDispatchPublisher sendWaitingDeliveryNoticeJobDispatchPublisher,
			TradeFinishPaymentSuccWxaSubscribeSendPort tradeFinishPaymentSuccWxaSubscribeSendPort,
			TradeFinishPaymentSuccShopNamePort tradeFinishPaymentSuccShopNamePort) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.sendWaitingDeliveryNoticeJobDispatchPublisher = sendWaitingDeliveryNoticeJobDispatchPublisher;
		this.tradeFinishPaymentSuccWxaSubscribeSendPort = tradeFinishPaymentSuccWxaSubscribeSendPort;
		this.tradeFinishPaymentSuccShopNamePort = tradeFinishPaymentSuccShopNamePort;
	}

	public void dispatchIfApplicable(Map<String, Object> tradeRowPayload) {
		if (tradeRowPayload == null) {
			return;
		}
		String tradeSourceType = stringify(tradeRowPayload.get("trade_source_type"));
		if (StringUtils.hasText(tradeSourceType) && "membercard".equalsIgnoreCase(tradeSourceType.trim())) {
			return;
		}
		String payType = stringify(tradeRowPayload.get("pay_type"));
		if (!StringUtils.hasText(payType)) {
			return;
		}
		String payNorm = payType.trim().toLowerCase();
		if (!"wxpay".equals(payNorm) && !"deposit".equals(payNorm)) {
			return;
		}

		String companyIdStr = stringify(tradeRowPayload.get("company_id"));
		String orderIdStr = stringify(tradeRowPayload.get("order_id"));
		if (!StringUtils.hasText(companyIdStr) || !StringUtils.hasText(orderIdStr)) {
			return;
		}

		Long companyId = parseLongFlexible(companyIdStr);
		Long orderId = parseLongFlexible(orderIdStr);
		if (companyId == null || orderId == null) {
			return;
		}

		NormalOrders normalOrder =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId));
		if (normalOrder == null) {
			return;
		}

		Long distributorId = normalOrder.getDistributorId();
		if (distributorId != null && distributorId != 0L) {
			String receiptType = normalOrder.getReceiptType();
			if (!StringUtils.hasText(receiptType) || !"ziti".equalsIgnoreCase(receiptType.trim())) {
				sendWaitingDeliveryNoticeJobDispatchPublisher.publish(
						companyIdStr.trim(), orderIdStr.trim(), String.valueOf(distributorId));
			}
		}

		if ("deposit".equals(payNorm)) {
			return;
		}
		if (!"wxpay".equals(payNorm)) {
			return;
		}

		try {
			if (!paymentSuccRequiredFieldsPresent(tradeRowPayload)) {
				return;
			}
			String shopName =
					tradeFinishPaymentSuccShopNamePort.resolveForSubscribeTemplate(
							companyId,
							normalOrder.getOrderClass(),
							stringify(tradeRowPayload.get("shop_id")),
							stringify(tradeRowPayload.get("wxa_appid")));

			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("pay_money", formatPayMoneyFen(resolvePayFeeFenRaw(tradeRowPayload)));
			inner.put("pay_date", formatPayDate(tradeRowPayload.get("time_start")));
			inner.put("item_name", stringifyOrEmpty(tradeRowPayload.get("detail")));
			inner.put("shop_name", shopName == null ? "" : shopName);
			inner.put("order_id", stringify(tradeRowPayload.get("order_id")));
			inner.put("trade_id", stringify(tradeRowPayload.get("trade_id")));
			String rc = normalOrder.getReceiptType();
			if (StringUtils.hasText(rc) && "ziti".equalsIgnoreCase(rc.trim())) {
				inner.put("receipt_type", "门店自提");
			} else {
				inner.put("receipt_type", "物流配送");
			}
			inner.put("pay_type", "微信支付");

			Map<String, Object> sendData = new LinkedHashMap<>();
			sendData.put("scenes_name", "paymentSucc");
			sendData.put("company_id", companyId);
			sendData.put("appid", stringify(tradeRowPayload.get("wxa_appid")));
			sendData.put("openid", stringify(tradeRowPayload.get("open_id")));
			sendData.put("data", inner);

			tradeFinishPaymentSuccWxaSubscribeSendPort.send(sendData, false);
		} catch (RuntimeException e) {
			log.debug("trade-finish paymentSucc wxa subscribe dispatch skipped: {}", e.getMessage());
		}
	}

	private static boolean paymentSuccRequiredFieldsPresent(Map<String, Object> tradeRowPayload) {
		if (!StringUtils.hasText(stringify(tradeRowPayload.get("wxa_appid")))
				|| !StringUtils.hasText(stringify(tradeRowPayload.get("open_id")))) {
			return false;
		}
		if (!StringUtils.hasText(stringify(tradeRowPayload.get("trade_id")))
				|| !StringUtils.hasText(stringify(tradeRowPayload.get("order_id")))) {
			return false;
		}
		return resolvePayFeeFenRaw(tradeRowPayload) != null;
	}

	private static Object resolvePayFeeFenRaw(Map<String, Object> tradeRowPayload) {
		Object payRaw = tradeRowPayload.get("pay_fee");
		if (payRaw == null) {
			payRaw = tradeRowPayload.get("payFee");
		}
		return payRaw;
	}

	private static String formatPayMoneyFen(Object payRaw) {
		Integer fen = parsePayFeeFen(payRaw);
		if (fen == null) {
			return "";
		}
		return BigDecimal.valueOf(fen)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static Integer parsePayFeeFen(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String formatPayDate(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		if (s.matches("\\d{10}")) {
			long epoch = Long.parseLong(s);
			return PAY_INSTANT.format(Instant.ofEpochSecond(epoch));
		}
		if (s.matches("\\d{13}")) {
			long epoch = Long.parseLong(s) / 1000L;
			return PAY_INSTANT.format(Instant.ofEpochSecond(epoch));
		}
		return s;
	}

	private static String stringifyOrEmpty(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static Long parseLongFlexible(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
