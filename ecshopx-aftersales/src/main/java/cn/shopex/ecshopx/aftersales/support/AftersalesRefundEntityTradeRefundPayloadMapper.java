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

package cn.shopex.ecshopx.aftersales.support;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Maps {@link AftersalesRefund} rows to trade-refund event payload maps. */
@Component
public class AftersalesRefundEntityTradeRefundPayloadMapper {

	public Map<String, Object> toDispatchPayload(AftersalesRefund refund) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("refund_bn", refund.getRefundBn());
		m.put("aftersales_bn", refund.getAftersalesBn());
		m.put("order_id", refund.getOrderId());
		m.put("trade_id", refund.getTradeId() == null ? "" : refund.getTradeId());
		m.put("company_id", refund.getCompanyId());
		m.put("supplier_id", refund.getSupplierId() == null ? 0L : refund.getSupplierId());
		m.put("user_id", refund.getUserId());
		m.put("shop_id", refund.getShopId() == null ? 0L : refund.getShopId());
		m.put("distributor_id", refund.getDistributorId() == null ? 0L : refund.getDistributorId());
		m.put("refund_type", refundTypeAsInteger(refund.getRefundType()));
		m.put(
				"refund_channel",
				refund.getRefundChannel() == null ? "" : refund.getRefundChannel());
		m.put(
				"refund_status",
				refund.getRefundStatus() == null ? "" : refund.getRefundStatus());
		m.put("refund_fee", refund.getRefundFee() == null ? 0 : refund.getRefundFee());
		m.put(
				"refund_point", refund.getRefundPoint() == null ? 0 : refund.getRefundPoint());
		m.put(
				"return_freight",
				refund.getReturnFreight() == null ? 0 : refund.getReturnFreight());
		m.put("freight", refund.getFreight() == null ? 0 : refund.getFreight());
		m.put(
				"freight_type",
				refund.getFreightType() == null ? "cash" : refund.getFreightType());
		m.put("pay_type", refund.getPayType() == null ? "" : refund.getPayType());
		m.put("currency", refund.getCurrency() == null ? "" : refund.getCurrency());
		m.put(
				"cur_fee_type",
				refund.getCurFeeType() == null ? "" : refund.getCurFeeType());
		m.put(
				"cur_fee_rate",
				refund.getCurFeeRate() == null ? 1.0 : refund.getCurFeeRate());
		m.put(
				"cur_fee_symbol",
				refund.getCurFeeSymbol() == null ? "" : refund.getCurFeeSymbol());
		m.put(
				"cur_pay_fee",
				refund.getCurPayFee() == null ? "" : refund.getCurPayFee());
		m.put(
				"merchant_id",
				refund.getMerchantId() == null ? 0L : refund.getMerchantId());
		Integer rp = refund.getReturnPoint();
		if (rp != null && rp != 0) {
			m.put("return_point", rp);
		}
		return m;
	}

	private static int refundTypeAsInteger(String refundType) {
		if (!StringUtils.hasText(refundType)) {
			return 0;
		}
		try {
			return Integer.parseInt(refundType.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
