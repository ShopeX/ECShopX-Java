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

package cn.shopex.ecshopx.orders.service.statistics;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradeRefundOrderPayStatisticsService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private static final Set<String> SERVICE_TRADE_SOURCE_TYPES =
			Set.of("service", "groups", "service_groups", "service_seckill");

	private static final Set<String> NORMAL_TRADE_SOURCE_TYPES = Set.of(
			"normal",
			"normal_normal",
			"normal_groups",
			"normal_seckill",
			"normal_community",
			"bargain",
			"normal_shopguide",
			"normal_pointsmall");

	private final StringRedisTemplate companysRedis;
	private final TradeMapper tradeMapper;
	private final NormalOrdersMapper normalOrdersMapper;

	public TradeRefundOrderPayStatisticsService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedis,
			TradeMapper tradeMapper,
			NormalOrdersMapper normalOrdersMapper) {
		this.companysRedis = companysRedis;
		this.tradeMapper = tradeMapper;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public void recordRefundStatistics(Map<String, Object> data) {
		if (data == null) {
			log.debug("trade refund statistics skipped: empty payload");
			return;
		}
		Long companyId = longFrom(data.get("company_id"));
		Long orderId = longFrom(data.get("order_id"));
		if (companyId == null || companyId <= 0L || orderId == null || orderId <= 0L) {
			log.debug("trade refund statistics skipped: missing company_id or order_id");
			return;
		}

		Trade tradeRow = findTrade(String.valueOf(companyId), orderId, stringVal(data.get("trade_id")));
		if (tradeRow == null) {
			log.debug(
					"trade refund statistics skipped: no trade companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}

		String tradeSourceRaw = tradeRow.getTradeSourceType();
		String tradeSource = tradeSourceRaw == null ? "" : tradeSourceRaw.trim().toLowerCase(Locale.ROOT);
		String statisticsType = resolveStatisticsType(tradeSource);
		if (statisticsType.isEmpty()) {
			log.debug(
					"trade refund statistics skipped: tradeSourceType not in whitelist companyId={} type={}",
					companyId,
					tradeSourceRaw);
			return;
		}

		int refundFeePayload = intFrom(data.get("refund_fee"));
		int payFeePayload = intFrom(data.get("pay_fee"));
		int payFeeTrade = tradeRow.getPayFee() == null ? 0 : tradeRow.getPayFee();
		int incrementFen = refundFeePayload != 0 ? refundFeePayload : (payFeePayload != 0 ? payFeePayload : payFeeTrade);

		String ymd = ZonedDateTime.now(SHANGHAI).toLocalDate().format(YMD);
		String companyIdsKey = "companyIds:" + ymd;
		companysRedis.opsForSet().add(companyIdsKey, String.valueOf(companyId));

		String hashKey = "OrderPayStatistics:" + statisticsType + ":" + companyId + ":" + ymd;
		Long totalRefund = companysRedis.opsForHash().increment(hashKey, "orderRefundFee", incrementFen);
		if (totalRefund == null) {
			log.debug("trade refund statistics: orderRefundFee increment returned null key={}", hashKey);
			return;
		}

		NormalOrders orderRow = findOrder(companyId, orderId);
		if (orderRow != null && orderRow.getDistributorId() != null) {
			companysRedis
					.opsForHash()
					.increment(hashKey, orderRow.getDistributorId() + "_orderRefundFee", incrementFen);
		}
		if (orderRow != null && orderRow.getMerchantId() != null && orderRow.getMerchantId() > 0L) {
			companysRedis
					.opsForHash()
					.increment(hashKey, orderRow.getMerchantId() + "_merchant_orderRefundFee", incrementFen);
		}
	}

	private static String resolveStatisticsType(String tradeSource) {
		if (tradeSource == null || tradeSource.isEmpty()) {
			return "";
		}
		if (SERVICE_TRADE_SOURCE_TYPES.contains(tradeSource)) {
			return "service";
		}
		if (NORMAL_TRADE_SOURCE_TYPES.contains(tradeSource)) {
			return "normal";
		}
		return "";
	}

	private NormalOrders findOrder(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.last("LIMIT 1"));
	}

	private Trade findTrade(String companyIdStr, long orderId, String tradeIdFromPayload) {
		if (tradeIdFromPayload != null && !tradeIdFromPayload.isBlank()) {
			Trade t =
					tradeMapper.selectOne(
							new LambdaQueryWrapper<Trade>()
									.eq(Trade::getCompanyId, companyIdStr)
									.eq(Trade::getTradeId, tradeIdFromPayload)
									.last("LIMIT 1"));
			if (t != null) {
				return t;
			}
		}
		return tradeMapper.selectOne(
				new LambdaQueryWrapper<Trade>()
						.eq(Trade::getCompanyId, companyIdStr)
						.eq(Trade::getOrderId, String.valueOf(orderId))
						.eq(Trade::getTradeState, "SUCCESS")
						.last("LIMIT 1"));
	}

	private static String stringVal(Object o) {
		return o == null ? null : String.valueOf(o).trim();
	}

	private static Long longFrom(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int intFrom(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
