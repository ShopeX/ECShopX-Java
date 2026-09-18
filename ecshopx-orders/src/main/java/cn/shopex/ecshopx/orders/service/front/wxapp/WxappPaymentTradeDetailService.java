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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappPaymentTradeDetailService {

	private final TradeMapper tradeMapper;
	private final ObjectMapper objectMapper;

	public WxappPaymentTradeDetailService(TradeMapper tradeMapper, ObjectMapper objectMapper) {
		this.tradeMapper = tradeMapper;
		this.objectMapper = objectMapper;
	}

	public Object getTradeDetail(String tradeId, String companyId, String userId) {
		LambdaQueryWrapper<Trade> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(Trade::getTradeId, tradeId)
				.eq(Trade::getCompanyId, companyId)
				.eq(Trade::getUserId, userId)
				.last("LIMIT 1");
		Trade row = tradeMapper.selectOne(wrapper);
		if (row == null) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("trade_id", row.getTradeId());
		m.put("order_id", row.getOrderId());
		m.put("company_id", row.getCompanyId());
		m.put("shop_id", row.getShopId());
		m.put("distributor_id", row.getDistributorId());
		m.put("dealer_id", row.getDealerId());
		m.put("merchant_id", row.getMerchantId());
		m.put("user_id", row.getUserId());
		m.put("supplier_id", row.getSupplierId());
		m.put("trade_source_type", row.getTradeSourceType());
		m.put("trade_state", row.getTradeState());
		m.put("pay_type", row.getPayType());
		m.put("pay_channel", row.getPayChannel());
		m.put("trade_no", row.getTradeNo());
		m.put("total_fee", row.getTotalFee());
		m.put("discount_fee", row.getDiscountFee());
		m.put("fee_type", row.getFeeType());
		m.put("pay_fee", row.getPayFee());
		m.put("cur_pay_fee", row.getCurPayFee());
		m.put("cur_fee_symbol", row.getCurFeeSymbol());
		m.put("cur_fee_rate", row.getCurFeeRate());
		m.put("cur_fee_type", row.getCurFeeType());
		m.put("coupon_fee", row.getCouponFee());
		m.put("refunded_fee", row.getRefundedFee());
		m.put("mch_id", row.getMchId());
		m.put("transaction_id", row.getTransactionId());
		m.put("authorizer_appid", row.getAuthorizerAppid());
		m.put("wxa_appid", row.getWxaAppid());
		m.put("bank_type", row.getBankType());
		m.put("open_id", row.getOpenId());
		m.put("mobile", row.getMobile());
		m.put("body", row.getBody());
		m.put("detail", row.getDetail());
		m.put("time_start", parseEpochSecondOrNull(row.getTimeStart()));
		m.put("time_expire", parseEpochSecondOrNull(row.getTimeExpire()));
		m.put("discount_info", row.getDiscountInfo());
		m.put("coupon_info", row.getCouponInfo());
		m.put("inital_request", decodeJsonNullable(row.getInitalRequest()));
		m.put("inital_response", decodeJsonNullable(row.getInitalResponse()));
		m.put("div_members", row.getDivMembers());
		m.put("adapay_div_status", row.getAdapayDivStatus());
		m.put("adapay_fee", row.getAdapayFee());
		m.put("adapay_fee_mode", row.getAdapayFeeMode());
		m.put("payment_params", row.getPaymentParams());
		m.put("is_settled", row.getIsSettled());
		m.put("bspay_div_members", row.getBspayDivMembers());
		m.put("bspay_div_status", row.getBspayDivStatus());
		m.put("bspay_fee", row.getBspayFee());
		m.put("bspay_fee_mode", row.getBspayFeeMode());
		m.put("bspay_req_date", row.getBspayReqDate());
		return m;
	}

	private Long parseEpochSecondOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		if (!t.matches("^-?\\d+$")) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Object decodeJsonNullable(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return objectMapper.readValue(t, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
