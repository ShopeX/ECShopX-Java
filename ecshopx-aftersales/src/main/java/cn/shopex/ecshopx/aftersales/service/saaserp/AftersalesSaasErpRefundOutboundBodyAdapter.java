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

package cn.shopex.ecshopx.aftersales.service.saaserp;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.openapi.OpenapiCancelOrderReasonPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpNormalOrderLookupPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpRefundOutboundBodyPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AftersalesSaasErpRefundOutboundBodyAdapter implements SaasErpRefundOutboundBodyPort {

	private static final BigDecimal HUNDRED = new BigDecimal("100");
	private static final DateTimeFormatter PHP_DATE =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final SaasErpNormalOrderLookupPort orderLookupPort;
	private final OpenapiCancelOrderReasonPort cancelOrderReasonPort;

	public AftersalesSaasErpRefundOutboundBodyAdapter(
			AftersalesMapper aftersalesMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			SaasErpNormalOrderLookupPort orderLookupPort,
			OpenapiCancelOrderReasonPort cancelOrderReasonPort) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.orderLookupPort = orderLookupPort;
		this.cancelOrderReasonPort = cancelOrderReasonPort;
	}

	@Override
	public Map<String, Object> refundAddBody(
			String refundId, long companyId, String orderId, String aftersalesBnOrNull) {
		AftersalesRefund refund = loadRefund(refundId, companyId, orderId, aftersalesBnOrNull);
		if (refund == null) {
			throw new IllegalStateException("获取订单信息失败");
		}
		Map<String, Object> order =
				orderLookupPort
						.find(companyId, orderId)
						.orElseThrow(() -> new IllegalStateException("获取订单信息失败"));
		long buyerId = longVal(order.get("user_id"));
		String memo = resolveMemo(companyId, orderId, aftersalesBnOrNull, refund);
		return buildRefundBody(refund, buyerId, memo, "refund", "SUCC");
	}

	@Override
	public Map<String, Object> refundSendSaasErpBody(
			String refundId, long companyId, String orderId, String aftersalesBn) {
		AftersalesRefund refund = loadRefund(refundId, companyId, orderId, aftersalesBn);
		if (refund == null) {
			throw new IllegalStateException("售后获取失败");
		}
		String memo = "#" + refundId + "#";
		return buildRefundBody(refund, longVal(refund.getUserId()), memo, "refund", "SUCC");
	}

	private AftersalesRefund loadRefund(
			String refundId, long companyId, String orderId, String aftersalesBnOrNull) {
		Long parsedOrderId = parseLong(orderId);
		Long parsedRefundBn = parseLong(refundId);
		if (parsedOrderId == null) {
			return null;
		}
		LambdaQueryWrapper<AftersalesRefund> query = new LambdaQueryWrapper<>();
		query.eq(AftersalesRefund::getOrderId, parsedOrderId);
		if (companyId > 0) {
			query.eq(AftersalesRefund::getCompanyId, companyId);
		}
		if (aftersalesBnOrNull != null && !aftersalesBnOrNull.isBlank()) {
			Long parsedAftersalesBn = parseLong(aftersalesBnOrNull);
			if (parsedAftersalesBn != null) {
				query.eq(AftersalesRefund::getAftersalesBn, parsedAftersalesBn);
			}
		}
		if (parsedRefundBn != null) {
			query.eq(AftersalesRefund::getRefundBn, parsedRefundBn);
		}
		query.last("LIMIT 1");
		return aftersalesRefundMapper.selectOne(query);
	}

	private String resolveMemo(
			long companyId, String orderId, String aftersalesBnOrNull, AftersalesRefund refund) {
		if (aftersalesBnOrNull != null && !aftersalesBnOrNull.isBlank()) {
			Long parsedBn = parseLong(aftersalesBnOrNull);
			if (parsedBn != null) {
				Aftersales after =
						aftersalesMapper.selectOne(
								new LambdaQueryWrapper<Aftersales>()
										.eq(Aftersales::getAftersalesBn, parsedBn)
										.last("LIMIT 1"));
				if (after == null) {
					throw new IllegalStateException("售后获取失败");
				}
				return after.getReason() == null ? "" : after.getReason();
			}
		}
		long orderIdNum = refund.getOrderId() == null ? parseLong(orderId) : refund.getOrderId();
		long company = refund.getCompanyId() == null ? companyId : refund.getCompanyId();
		if (orderIdNum <= 0L || company <= 0L) {
			return "";
		}
		return cancelOrderReasonPort.findCancelReason(company, orderIdNum);
	}

	private Map<String, Object> buildRefundBody(
			AftersalesRefund refund, long buyerId, String memo, String refundType, String status) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("refund_id", String.valueOf(refund.getRefundBn()));
		body.put("tid", refund.getOrderId());
		body.put("buyer_bank", "");
		body.put("buyer_id", buyerId);
		body.put("buyer_account", "");
		body.put("buyer_name", "");
		body.put("refund_fee", feeFromCents(refund.getRefundFee()));
		body.put("currency", refund.getCurrency());
		body.put("currency_fee", currencyFee(refund));
		body.put("pay_type", "online");
		body.put("payment_tid", "");
		body.put("payment_type", "");
		body.put("seller_account", "");
		body.put(
				"t_begin",
				formatEpoch(refund.getCreateTime() == null ? 0 : refund.getCreateTime()));
		body.put("t_sent", "");
		body.put("t_received", "");
		body.put("refund_type", refundType);
		body.put("status", status);
		body.put("memo", memo == null ? "" : memo);
		if ("SUCC".equals(status)) {
			body.put("t_received", formatEpoch((int) Instant.now().getEpochSecond()));
		}
		return body;
	}

	private static String currencyFee(AftersalesRefund refund) {
		if ("point".equalsIgnoreCase(safe(refund.getPayType()))) {
			return "0.00";
		}
		return feeFromCents(parseIntSafe(refund.getCurPayFee()));
	}

	private static String feeFromCents(Integer cents) {
		if (cents == null) {
			return "0.00";
		}
		return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static int parseIntSafe(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		try {
			return new BigDecimal(raw.trim()).intValue();
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static String formatEpoch(int epochSeconds) {
		if (epochSeconds <= 0) {
			return PHP_DATE.format(Instant.ofEpochSecond(0));
		}
		return PHP_DATE.format(Instant.ofEpochSecond(epochSeconds));
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static Long parseLong(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			return Long.valueOf(trimmed);
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
