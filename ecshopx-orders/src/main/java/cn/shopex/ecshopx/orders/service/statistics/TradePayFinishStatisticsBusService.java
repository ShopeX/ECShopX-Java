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
public class TradePayFinishStatisticsBusService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * {@code trade_source_type} values that roll up into the {@code service} pay-statistics bucket.
	 */
	private static final Set<String> SERVICE_TRADE_SOURCE_TYPES =
			Set.of("service", "groups", "service_groups", "service_seckill");

	/**
	 * {@code trade_source_type} values that roll up into the {@code normal} pay-statistics bucket (mall, campaigns,
	 * storefront variants).
	 */
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

	public TradePayFinishStatisticsBusService(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedis) {
		this.companysRedis = companysRedis;
	}

	/**
	 * Whether the trade row should participate in pay-finish Redis aggregates.
	 */
	public boolean isEligibleTradeSourceType(Object raw) {
		String tradeSource = normalizeSourceType(raw);
		if (tradeSource.isEmpty()) {
			return false;
		}
		return SERVICE_TRADE_SOURCE_TYPES.contains(tradeSource) || NORMAL_TRADE_SOURCE_TYPES.contains(tradeSource);
	}

	/**
	 * Resolves {@code service} vs {@code normal} hash namespace used in {@code OrderPayStatistics:{type}:...} keys.
	 */
	public String resolveStatisticsType(Object raw) {
		String tradeSource = normalizeSourceType(raw);
		if (SERVICE_TRADE_SOURCE_TYPES.contains(tradeSource)) {
			return "service";
		}
		if (NORMAL_TRADE_SOURCE_TYPES.contains(tradeSource)) {
			return "normal";
		}
		return "";
	}

	public void recordPayFinishStatistics(Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return;
		}
		Long companyId = longFrom(data.get("company_id"));
		Long orderId = longFrom(data.get("order_id"));
		if (companyId == null || companyId <= 0L || orderId == null || orderId <= 0L) {
			log.debug("trade pay finish statistics skipped: missing company_id or order_id");
			return;
		}

		String tradeSourceRaw = stringVal(data.get("trade_source_type"));
		if (!isEligibleTradeSourceType(tradeSourceRaw)) {
			log.debug(
					"trade pay finish statistics skipped: unsupported trade_source_type orderId={} type={}",
					orderId,
					tradeSourceRaw);
			return;
		}

		if (!"SUCCESS".equals(stringVal(data.get("trade_state")))) {
			log.debug("trade pay finish statistics skipped: trade_state is not SUCCESS orderId={}", orderId);
			return;
		}

		String statisticsType = resolveStatisticsType(tradeSourceRaw);
		if (statisticsType.isEmpty()) {
			return;
		}

		long totalFeeFen = payAmountFen(data);
		if (totalFeeFen < 0L) {
			log.debug("trade pay finish statistics skipped: negative pay amount orderId={}", orderId);
			return;
		}

		String userId = String.valueOf(data.get("user_id") == null ? "" : data.get("user_id")).trim();
		String ymd = ZonedDateTime.now(SHANGHAI).toLocalDate().format(YMD);
		String companyIdsKey = "companyIds:" + ymd;
		companysRedis.opsForSet().add(companyIdsKey, String.valueOf(companyId));

		String redisKey = orderPayStatisticsKey(companyId, statisticsType, ymd);

		log.debug("order pay finish statistics orderId={} feeFen={} userId={}", orderId, totalFeeFen, userId);

		companysRedis.opsForHash().increment(redisKey, "orderPayFee", totalFeeFen);
		companysRedis.opsForHash().increment(redisKey, "orderPayNum", 1L);
		if (!userId.isEmpty()) {
			companysRedis.opsForSet().add(redisKey + "_orderPayUser", userId);
		}

		long shopId = parseLongLoose(data.get("distributor_id"));
		if (shopId > 0L) {
			companysRedis.opsForHash().increment(redisKey, shopId + "_orderPayFee", totalFeeFen);
			companysRedis.opsForHash().increment(redisKey, shopId + "_orderPayNum", 1L);
			if (!userId.isEmpty()) {
				companysRedis.opsForSet().add(redisKey + "_" + shopId + "_orderPayUser", userId);
			}
		}

		long merchantId = parseLongLoose(data.get("merchant_id"));
		if (merchantId > 0L) {
			companysRedis.opsForHash().increment(redisKey, merchantId + "_merchant_orderPayFee", totalFeeFen);
			companysRedis.opsForHash().increment(redisKey, merchantId + "_merchant_orderPayNum", 1L);
			if (!userId.isEmpty()) {
				companysRedis.opsForSet().add(redisKey + "_" + merchantId + "_merchant_orderPayUser", userId);
			}
		}
	}

	private static String orderPayStatisticsKey(long companyId, String statisticsType, String ymd) {
		return "OrderPayStatistics:" + statisticsType + ":" + companyId + ":" + ymd;
	}

	private static String normalizeSourceType(Object raw) {
		String s = stringVal(raw);
		return s.isEmpty() ? "" : s.toLowerCase(Locale.ROOT);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
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

	private static long payAmountFen(Map<String, Object> data) {
		Object total = data.get("total_fee");
		if (total != null) {
			long v = longFromAmount(total);
			if (v > 0L) {
				return v;
			}
		}
		Object payFee = data.get("pay_fee");
		return payFee == null ? 0L : longFromAmount(payFee);
	}

	private static long longFromAmount(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseLongLoose(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
