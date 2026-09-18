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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdapayTradedataEspierImportRowSink implements EspierImportRowSink {

	private final TradeMapper tradeMapper;

	public AdapayTradedataEspierImportRowSink(TradeMapper tradeMapper) {
		this.tradeMapper = tradeMapper;
	}

	@Override
	public String supportedFileType() {
		return "adapay_tradedata";
	}

	@Override
	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			@SuppressWarnings("unused") String operatorType) {
		String tradeId = stripQuotes(str(row.get("trade_id")));
		String orderId = stripQuotes(str(row.get("order_id")));
		String divLabel = str(row.get("adapay_div_status"));
		if (!StringUtils.hasText(tradeId) || !StringUtils.hasText(orderId) || !StringUtils.hasText(divLabel)) {
			throw new BadRequestException("订单号、交易单号、是否分账均必填");
		}
		String companyKey = String.valueOf(companyId);
		Trade trade =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getTradeId, tradeId)
								.eq(Trade::getOrderId, orderId)
								.eq(Trade::getCompanyId, companyKey)
								.eq(Trade::getTradeState, "SUCCESS")
								.ne(Trade::getTradeSourceType, "membercard")
								.last("LIMIT 1"));
		if (trade == null) {
			throw new BadRequestException("分账订单不存在");
		}
		if ("adapay".equalsIgnoreCase(trade.getPayType())) {
			throw new BadRequestException("线上分账订单不可手动分账");
		}
		boolean canDiv = canDiv(trade);
		if (!canDiv) {
			throw new BadRequestException("订单当前不可分账");
		}
		String status;
		if ("已分账".equals(divLabel)) {
			status = "DIVED";
		} else if ("未分账".equals(divLabel)) {
			status = "NOTDIV";
		} else {
			throw new BadRequestException("是否分账填写错误");
		}
		LambdaUpdateWrapper<Trade> uw =
				new LambdaUpdateWrapper<Trade>()
						.eq(Trade::getTradeId, tradeId)
						.eq(Trade::getOrderId, orderId)
						.eq(Trade::getCompanyId, companyKey);
		uw.set(Trade::getAdapayDivStatus, status);
		int n = tradeMapper.update(null, uw);
		if (n != 1) {
			throw new BadRequestException("分账状态更新失败");
		}
	}

	private static boolean canDiv(Trade t) {
		int refunded = t.getRefundedFee() == null ? 0 : t.getRefundedFee();
		int pay = t.getPayFee() == null ? 0 : t.getPayFee();
		return refunded < pay;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static String stripQuotes(String s) {
		String v = s.trim();
		if (v.length() >= 2 && v.startsWith("'") && v.endsWith("'")) {
			return v.substring(1, v.length() - 1).trim();
		}
		return v;
	}
}
