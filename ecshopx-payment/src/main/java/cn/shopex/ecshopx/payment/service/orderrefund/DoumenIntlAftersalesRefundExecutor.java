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

package cn.shopex.ecshopx.payment.service.orderrefund;

import cn.shopex.ecshopx.common.refund.AftersalesRefundPayChannelExecutor;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import cn.shopex.ecshopx.payment.service.DoumenIntlPaymentService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(55)
public class DoumenIntlAftersalesRefundExecutor implements AftersalesRefundPayChannelExecutor {

	private final DoumenIntlPaymentService doumenIntlPaymentService;

	public DoumenIntlAftersalesRefundExecutor(DoumenIntlPaymentService doumenIntlPaymentService) {
		this.doumenIntlPaymentService = doumenIntlPaymentService;
	}

	@Override
	public boolean supports(String payTypeLower) {
		return "doumen_intl".equals(payTypeLower);
	}

	@Override
	public Map<String, Object> execute(AftersalesRefundPaymentContext ctx) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", ctx.getCompanyId());
		data.put("refund_bn", String.valueOf(ctx.getRefundBn()));
		data.put("trade_id", ctx.getTradeId());
		data.put("order_id", String.valueOf(ctx.getOrderId()));
		data.put("refund_fee", ctx.getRefundFeeFen());
		data.put("fee_type", ctx.getCurrency());
		data.put("transaction_id", ctx.getTransactionId());
		return doumenIntlPaymentService.doRefund(data);
	}
}
