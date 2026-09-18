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

package cn.shopex.ecshopx.orders.service.payment;

import cn.shopex.ecshopx.common.dispatch.JushuitanTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.port.VipGradeMembercardTradePaidPort;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrdersTradePaymentCallbackService {

	private static final ObjectMapper SNAKE_ROW =
			new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	private final TradeMapper tradeMapper;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate redisTemplate;
	private final JushuitanTradeFinishDispatchPublisher jushuitanTradeFinishDispatchPublisher;
	private final OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;
	private final WdtErpTradeFinishDispatchPublisher wdtErpTradeFinishDispatchPublisher;
	private final ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort;

	public OrdersTradePaymentCallbackService(
			TradeMapper tradeMapper,
			ObjectMapper objectMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redisTemplate,
			JushuitanTradeFinishDispatchPublisher jushuitanTradeFinishDispatchPublisher,
			OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher,
			WdtErpTradeFinishDispatchPublisher wdtErpTradeFinishDispatchPublisher,
			ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort) {
		this.tradeMapper = tradeMapper;
		this.objectMapper = objectMapper;
		this.redisTemplate = redisTemplate;
		this.jushuitanTradeFinishDispatchPublisher = jushuitanTradeFinishDispatchPublisher;
		this.ordersTradeFinishDispatchPublisher = ordersTradeFinishDispatchPublisher;
		this.wdtErpTradeFinishDispatchPublisher = wdtErpTradeFinishDispatchPublisher;
		this.vipGradeMembercardTradePaidPort = vipGradeMembercardTradePaidPort;
	}

	public void applyTradePaymentNotify(String outTradeNo, String status, Map<String, Object> options) {
		if (!StringUtils.hasText(outTradeNo)) {
			throw new BadRequestException("回调数据无效");
		}
		Map<String, Object> postDataForStore = new LinkedHashMap<>();
		if (options != null) {
			postDataForStore.putAll(options);
		}

		Trade row = tradeMapper.selectById(outTradeNo);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		try {
			row.setInitalResponse(objectMapper.writeValueAsString(postDataForStore));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("回调数据无法序列化");
		}
		tradeMapper.updateById(row);

		Trade trade = tradeMapper.selectById(outTradeNo);
		if (trade == null) {
			throw new ResourceException("更新订单不存在");
		}
		if (!"NOTPAY".equals(trade.getTradeState())) {
			throw new ResourceException("更新已处理，不需要更新");
		}

		String payType = text(options != null ? options.get("pay_type") : null);
		String transactionId = text(options != null ? options.get("transaction_id") : null);
		String payChannel = text(options != null ? options.get("pay_channel") : null);
		String bankType = text(options != null ? options.get("bank_type") : null);
		int couponFee = intFrom(options != null ? options.get("coupon_fee") : null, 0);
		String couponInfo = couponInfoFrom(options != null ? options.get("coupon_info") : null);

		LambdaUpdateWrapper<Trade> uw = new LambdaUpdateWrapper<>();
		uw.eq(Trade::getTradeId, outTradeNo)
				.set(Trade::getTradeState, status)
				.set(Trade::getPayType, payType)
				.set(Trade::getPayChannel, payChannel)
				.set(Trade::getBankType, bankType)
				.set(Trade::getCouponFee, couponFee)
				.set(Trade::getCouponInfo, couponInfo);
		if (StringUtils.hasText(transactionId)) {
			uw.set(Trade::getTransactionId, transactionId);
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
			publishSystemLinkTradeFinishAfterSuccess(outTradeNo);
		}
	}

	private void publishSystemLinkTradeFinishAfterSuccess(String tradeId) {
		Trade latest = tradeMapper.selectById(tradeId);
		if (latest == null) {
			throw new ResourceException("更新订单不存在");
		}
		maybeFulfillVipGradeMembercardSale(latest);
		Map<String, Object> tradeRow;
		try {
			tradeRow = SNAKE_ROW.convertValue(latest, new TypeReference<Map<String, Object>>() {});
		} catch (IllegalArgumentException e) {
			throw new ResourceException("交易数据序列化失败");
		}
		jushuitanTradeFinishDispatchPublisher.publish(tradeRow);
		ordersTradeFinishDispatchPublisher.publish(tradeRow);
		wdtErpTradeFinishDispatchPublisher.publish(tradeRow);
	}

	/**
	 * PHP parity: payment notify for {@code trade_source_type=membercard} must open VIP after trade SUCCESS
	 * ({@code UpdateOrderStatusListener} → {@code VipGradeOrderService::tradeSuccUpdateOrderStatus}).
	 */
	private void maybeFulfillVipGradeMembercardSale(Trade trade) {
		if (trade == null || !"membercard".equals(trade.getTradeSourceType())) {
			return;
		}
		VipGradeMembercardTradePaidPort port = vipGradeMembercardTradePaidPort.getIfAvailable();
		if (port == null) {
			return;
		}
		String companyIdStr = trade.getCompanyId();
		String userIdStr = trade.getUserId();
		String orderId = trade.getOrderId();
		if (companyIdStr == null
				|| companyIdStr.isBlank()
				|| userIdStr == null
				|| userIdStr.isBlank()
				|| orderId == null
				|| orderId.isBlank()) {
			return;
		}
		long companyId = Long.parseLong(companyIdStr.trim());
		long userId = Long.parseLong(userIdStr.trim());
		port.onTradeSuccess(companyId, orderId.trim(), userId);
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

	private static int intFrom(Object o, int defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private String couponInfoFrom(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			return String.valueOf(o);
		}
	}
}
