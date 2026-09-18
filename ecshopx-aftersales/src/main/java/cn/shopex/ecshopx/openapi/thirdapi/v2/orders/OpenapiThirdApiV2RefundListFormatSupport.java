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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiThirdApiV2RefundListFormatSupport {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenapiThirdApiV2AftersalesListFormatSupport aftersalesListFormatSupport;

	public OpenapiThirdApiV2RefundListFormatSupport(
			OpenapiThirdApiV2AftersalesListFormatSupport aftersalesListFormatSupport) {
		this.aftersalesListFormatSupport = aftersalesListFormatSupport;
	}

	public Map<String, Object> formatOpenApiRefundRow(AftersalesRefund r) {
		Map<String, Object> formatted = new LinkedHashMap<>();
		formatted.put("refund_bn", String.valueOf(r.getRefundBn()));
		formatted.put(
				"aftersales_bn",
				r.getAftersalesBn() != null ? String.valueOf(r.getAftersalesBn()) : null);
		formatted.put("order_id", String.valueOf(r.getOrderId()));
		formatted.put("trade_id", r.getTradeId());
		formatted.put("refund_channel", r.getRefundChannel());
		formatted.put("refund_status", r.getRefundStatus());
		formatted.put("refund_fee", r.getRefundFee());
		formatted.put("refunded_fee", r.getRefundedFee());
		formatted.put("pay_type", r.getPayType());
		formatted.put("refund_id", r.getRefundId());
		formatted.put("refund_point", r.getRefundPoint());
		formatted.put("refunded_point", r.getRefundedPoint());
		formatted.put("refund_success_time", formatEpochSecondsLong(r.getRefundSuccessTime()));
		formatted.put("create_time", formatEpochSeconds(r.getCreateTime()));
		formatted.put("update_time", formatEpochSeconds(r.getUpdateTime()));
		return formatted;
	}

	public Map<String, Object> formatOpenApiRefundDetail(
			AftersalesRefund r, String cancelReasonForPresale) {
		Map<String, Object> formatted = new LinkedHashMap<>();
		formatted.put("refund_bn", String.valueOf(r.getRefundBn()));
		formatted.put(
				"aftersales_bn",
				r.getAftersalesBn() != null ? String.valueOf(r.getAftersalesBn()) : null);
		formatted.put("order_id", String.valueOf(r.getOrderId()));
		formatted.put("trade_id", r.getTradeId());
		formatted.put("refund_channel", r.getRefundChannel());
		formatted.put("refund_status", r.getRefundStatus());
		formatted.put("return_freight", r.getReturnFreight() != null ? r.getReturnFreight() : 0);
		formatted.put("refund_fee", r.getRefundFee());
		formatted.put("refunded_fee", r.getRefundedFee());
		formatted.put("pay_type", r.getPayType());
		formatted.put("refund_id", r.getRefundId());
		formatted.put("refund_point", r.getRefundPoint());
		formatted.put("refunded_point", r.getRefundedPoint());
		formatted.put("refund_success_time", formatRefundSuccessTimeForDetail(r.getRefundSuccessTime()));
		formatted.put("create_time", formatEpochSeconds(r.getCreateTime()));
		formatted.put("update_time", formatEpochSeconds(r.getUpdateTime()));
		if (isEmptyAftersalesBn(r.getAftersalesBn())) {
			formatted.put(
					"cancel_reason", cancelReasonForPresale != null ? cancelReasonForPresale : "");
		}
		return formatted;
	}

	public Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		return aftersalesListFormatSupport.formatListStruct(totalCount, list, page, pageSize);
	}

	private static boolean isEmptyAftersalesBn(Long aftersalesBn) {
		return aftersalesBn == null || aftersalesBn == 0L;
	}

	private static String formatRefundSuccessTimeForDetail(Long epoch) {
		if (epoch == null || epoch <= 0L) {
			return LocalDateTime.now(ZoneId.systemDefault())
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch));
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return DATE_TIME_FMT.format(Instant.ofEpochSecond(0L));
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch.longValue()));
	}

	private static String formatEpochSecondsLong(Long epoch) {
		if (epoch == null || epoch <= 0L) {
			return DATE_TIME_FMT.format(Instant.ofEpochSecond(0L));
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch));
	}
}
