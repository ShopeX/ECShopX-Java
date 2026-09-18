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

package cn.shopex.ecshopx.aftersales.dispatch;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AftersalesRefundTradeRefundFinishPayloadMapper {

	private static final DateTimeFormatter REFUND_SUCCESS_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private AftersalesRefundTradeRefundFinishPayloadMapper() {}

	public static Map<String, Object> toPayload(AftersalesRefund refund) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", refund.getCompanyId());
		m.put("order_id", refund.getOrderId());
		m.put("refund_bn", refund.getRefundBn() == null ? "" : refund.getRefundBn());
		m.put("trade_id", blankIfNull(refund.getTradeId()));
		m.put("aftersales_bn", refund.getAftersalesBn() == null ? "" : refund.getAftersalesBn());
		m.put("shop_id", refund.getShopId() == null ? "" : refund.getShopId());
		m.put("distributor_id", refund.getDistributorId() == null ? "" : refund.getDistributorId());
		m.put("refund_fee", refund.getRefundFee() == null ? "" : refund.getRefundFee());
		m.put("refunded_fee", refund.getRefundedFee() == null ? "" : refund.getRefundedFee());
		m.put("refund_point", refund.getRefundPoint() == null ? 0 : refund.getRefundPoint());
		m.put("return_freight", refund.getReturnFreight() == null ? 0 : refund.getReturnFreight());
		m.put("freight", refund.getFreight() == null ? 0 : refund.getFreight());
		m.put("refund_success_time", formatRefundSuccessTime(refund.getRefundSuccessTime()));
		return m;
	}

	private static String blankIfNull(String s) {
		return s == null ? "" : s;
	}

	private static String formatRefundSuccessTime(Long raw) {
		if (raw == null) {
			return "";
		}
		long epochSeconds = raw > 10_000_000_000L ? raw / 1000L : raw;
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
		return REFUND_SUCCESS_TIME.format(ldt);
	}
}
