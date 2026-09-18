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

package cn.shopex.ecshopx.orders.service.adapay;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdapayOrdersTradePaymentCallbackService {

	private final TradeMapper tradeMapper;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate redisTemplate;

	public AdapayOrdersTradePaymentCallbackService(
			TradeMapper tradeMapper,
			ObjectMapper objectMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redisTemplate) {
		this.tradeMapper = tradeMapper;
		this.objectMapper = objectMapper;
		this.redisTemplate = redisTemplate;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyPaymentSucceeded(Map<String, Object> postData, String normalizedStatus) {
		if (postData == null) {
			throw new BadRequestException("回调数据不能为空");
		}
		String orderNo = text(postData.get("order_no"));
		if (!StringUtils.hasText(orderNo)) {
			throw new BadRequestException("缺少 order_no");
		}
		Object description = postData.get("description");
		String desc = description == null ? "" : description.toString();
		if ("membercard".equals(desc)) {
			updateTradeRow(
					orderNo,
					row -> {
						try {
							row.setInitalResponse(objectMapper.writeValueAsString(postData));
						} catch (JsonProcessingException e) {
							throw new BadRequestException("回调数据无法序列化");
						}
						row.setAdapayDivStatus("DIVED");
					});
		} else {
			updateTradeRow(
					orderNo,
					row -> {
						try {
							row.setInitalResponse(objectMapper.writeValueAsString(postData));
						} catch (JsonProcessingException e) {
							throw new BadRequestException("回调数据无法序列化");
						}
					});
		}
		applyStatusUpdate(orderNo, normalizedStatus, postData);
	}

	private void noopFinishEventsForThisIteration() {}

	private void updateTradeRow(String tradeId, java.util.function.Consumer<Trade> patch) {
		Trade row = tradeMapper.selectById(tradeId);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		patch.accept(row);
		tradeMapper.updateById(row);
	}

	private void applyStatusUpdate(String tradeId, String status, Map<String, Object> postData) {
		Trade trade = tradeMapper.selectById(tradeId);
		if (trade == null) {
			throw new ResourceException("更新订单不存在");
		}
		if (!"NOTPAY".equals(trade.getTradeState())) {
			throw new BadRequestException("更新已处理，不需要更新");
		}
		String payChannel = text(postData.get("pay_channel"));
		String bankType = "";
		Object expend = postData.get("expend");
		if (expend instanceof Map<?, ?> em) {
			Object bt = em.get("bank_type");
			bankType = bt == null ? "" : bt.toString();
		}
		String transactionId = text(postData.get("id"));
		int couponFee = 0;
		String couponInfo = null;
		LambdaUpdateWrapper<Trade> uw = new LambdaUpdateWrapper<>();
		uw.eq(Trade::getTradeId, tradeId)
				.set(Trade::getTradeState, status)
				.set(Trade::getBankType, bankType)
				.set(Trade::getPayType, "adapay")
				.set(Trade::getCouponFee, couponFee)
				.set(Trade::getCouponInfo, couponInfo);
		if (StringUtils.hasText(transactionId)) {
			uw.set(Trade::getTransactionId, transactionId);
		}
		if (StringUtils.hasText(payChannel)) {
			uw.set(Trade::getPayChannel, payChannel);
		}
		if ("SUCCESS".equals(status)) {
			int now = (int) (System.currentTimeMillis() / 1000L);
			uw.set(Trade::getTimeExpire, String.valueOf(now));
			String tradeNo = buildTradeNoSuffix(trade);
			uw.set(Trade::getTradeNo, tradeNo);
		}
		int updated = tradeMapper.update(null, uw);
		if (updated <= 0) {
			throw new ResourceException("交易单状态更新失败");
		}
		if ("SUCCESS".equals(status)) {
			noopFinishEventsForThisIteration();
		}
	}

	private String buildTradeNoSuffix(Trade trade) {
		String companyId = trade.getCompanyId() == null ? "0" : trade.getCompanyId();
		String distributorId = trade.getDistributorId() == null ? "0" : trade.getDistributorId();
		String orderId = trade.getOrderId() == null ? "" : trade.getOrderId();
		long seq = nextTodayTradeSequence(companyId, distributorId, orderId);
		String md = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		return md + "-" + seq;
	}

	private long nextTodayTradeSequence(String companyId, String distributorId, String orderId) {
		String today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		String hKey = "h_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String cKey = "c_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String existing = (String) redisTemplate.opsForHash().get(hKey, orderId);
		if (StringUtils.hasText(existing)) {
			return Long.parseLong(existing);
		}
		Long count = redisTemplate.opsForValue().increment(cKey);
		if (count == null) {
			count = 1L;
		}
		redisTemplate.opsForHash().put(hKey, orderId, String.valueOf(count));
		redisTemplate.expire(hKey, java.time.Duration.ofSeconds(86400));
		redisTemplate.expire(cKey, java.time.Duration.ofSeconds(86400));
		return count;
	}

	private static String text(Object o) {
		return o == null ? "" : o.toString();
	}
}
