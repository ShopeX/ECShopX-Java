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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 异常退款渠道-only 打款，对齐 {@link AftersalesRefundDoRefundService#doRefund} 的渠道段且无副作用。 */
@Service
public class AftersalesRefundAbnormalChannelService {

	private final OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final TradeMapper tradeMapper;
	private final HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator;

	public AftersalesRefundAbnormalChannelService(
			OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService,
			AftersalesRefundMapper aftersalesRefundMapper,
			TradeMapper tradeMapper,
			HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator) {
		this.ordersRefundPaymentDispatchService = ordersRefundPaymentDispatchService;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.tradeMapper = tradeMapper;
		this.hfPayOrderApplyIdGenerator = hfPayOrderApplyIdGenerator;
	}

	public Map<String, Object> dispatchChannelOnly(long companyId, long refundBn) {
		AftersalesRefund refund =
				aftersalesRefundMapper.selectOne(
						new LambdaQueryWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getCompanyId, companyId)
								.eq(AftersalesRefund::getRefundBn, refundBn)
								.last("LIMIT 1"));
		if (refund == null) {
			throw new ResourceException("退款单不存在");
		}
		int effectiveRefundFee = refund.getRefundFee() == null ? 0 : refund.getRefundFee();
		int effectiveRefundPoint = refund.getRefundPoint() == null ? 0 : refund.getRefundPoint();

		if (refund.getTradeId() == null || refund.getTradeId().isBlank()) {
			throw new ResourceException("支付信息未找到！");
		}
		Trade trade =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getTradeId, refund.getTradeId())
								.eq(Trade::getTradeState, "SUCCESS")
								.last("LIMIT 1"));
		if (trade == null) {
			throw new ResourceException("支付信息未找到！");
		}

		String payType = refund.getPayType() == null ? "" : refund.getPayType().toLowerCase(Locale.ROOT);
		String wxa = trade.getWxaAppid() == null ? "" : trade.getWxaAppid();

		if ("prepaid_point".equals(payType)
				|| (!"point".equals(payType) && effectiveRefundFee <= 0)) {
			Map<String, Object> instant = new LinkedHashMap<>();
			instant.put("status", "SUCCESS");
			instant.put("refund_id", "");
			return instant;
		}
		if ("localpay".equals(payType)) {
			throw new ResourceException("0元订单不支持退款");
		}
		if ("hfpay".equals(payType) && !StringUtils.hasText(refund.getHfOrderId())) {
			String hfOid = hfPayOrderApplyIdGenerator.nextOrderId();
			aftersalesRefundMapper.update(
					null,
					new LambdaUpdateWrapper<AftersalesRefund>()
							.eq(AftersalesRefund::getCompanyId, companyId)
							.eq(AftersalesRefund::getRefundBn, refundBn)
							.set(AftersalesRefund::getHfOrderId, hfOid));
			refund.setHfOrderId(hfOid);
		}
		try {
			return ordersRefundPaymentDispatchService.dispatch(
					companyId, wxa, refund, trade, effectiveRefundFee, effectiveRefundPoint, false);
		} catch (Exception e) {
			Map<String, Object> fail = new LinkedHashMap<>();
			fail.put("status", "FAIL");
			fail.put("error_desc", e.getMessage());
			return fail;
		}
	}
}