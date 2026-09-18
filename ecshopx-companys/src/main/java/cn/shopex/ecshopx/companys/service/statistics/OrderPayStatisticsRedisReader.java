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

package cn.shopex.ecshopx.companys.service.statistics;

import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderPayStatisticsRedisReader {

	private static final String[] ORDER_STAT_TYPES = {"normal", "service"};

	private final StringRedisTemplate companysRedis;

	public OrderPayStatisticsRedisReader(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedis) {
		this.companysRedis = companysRedis;
	}

	private static String orderPayKey(String type, long companyId, String ymd) {
		return "OrderPayStatistics:" + type + ":" + companyId + ":" + ymd;
	}

	public LinkedHashMap<String, Long> getStatistics(long companyId, String ymd, String shopIdOrNull) {
		long payedFee = 0L;
		long payedNum = 0L;
		long refundFee = 0L;
		long aftersales = 0L;
		long payedMembers = 0L;
		boolean shopScoped = shopIdOrNull != null && !shopIdOrNull.isBlank();
		for (String type : ORDER_STAT_TYPES) {
			String hashKey = orderPayKey(type, companyId, ymd);
			if (shopScoped) {
				String sid = shopIdOrNull;
				payedFee += hGetLong(hashKey, sid + "_orderPayFee");
				payedNum += hGetLong(hashKey, sid + "_orderPayNum");
				refundFee += hGetLong(hashKey, sid + "_orderRefundFee");
				aftersales += hGetLong(hashKey, sid + "_orderAftersales");
				payedMembers += sCard(hashKey + "_" + sid + "_orderPayUser");
			} else {
				payedFee += hGetLong(hashKey, "orderPayFee");
				payedNum += hGetLong(hashKey, "orderPayNum");
				refundFee += hGetLong(hashKey, "orderRefundFee");
				aftersales += hGetLong(hashKey, "orderAftersales");
				payedMembers += sCard(hashKey + "_orderPayUser");
			}
		}
		long realAtv = payedMembers > 0L ? payedFee / payedMembers : 0L;
		LinkedHashMap<String, Long> out = new LinkedHashMap<>();
		out.put("real_payed_fee", payedFee);
		out.put("real_payed_orders", payedNum);
		out.put("real_refunded_fee", refundFee);
		out.put("real_aftersale_count", aftersales);
		out.put("real_payed_members", payedMembers);
		out.put("real_atv", realAtv);
		return out;
	}

	public LinkedHashMap<String, Long> getMerchantStatistics(long companyId, long merchantId, String ymd) {
		long payedFee = 0L;
		long payedNum = 0L;
		long refundFee = 0L;
		long aftersales = 0L;
		long payedMembers = 0L;
		String mPrefix = merchantId + "_merchant_";
		for (String type : ORDER_STAT_TYPES) {
			String hashKey = orderPayKey(type, companyId, ymd);
			payedFee += hGetLong(hashKey, mPrefix + "orderPayFee");
			payedNum += hGetLong(hashKey, mPrefix + "orderPayNum");
			refundFee += hGetLong(hashKey, mPrefix + "orderRefundFee");
			aftersales += hGetLong(hashKey, mPrefix + "orderAftersales");
			payedMembers += sCard(hashKey + "_" + merchantId + "_merchant_orderPayUser");
		}
		long realAtv = payedMembers > 0L ? payedFee / payedMembers : 0L;
		LinkedHashMap<String, Long> out = new LinkedHashMap<>();
		out.put("real_payed_fee", payedFee);
		out.put("real_payed_orders", payedNum);
		out.put("real_refunded_fee", refundFee);
		out.put("real_aftersale_count", aftersales);
		out.put("real_payed_members", payedMembers);
		out.put("real_atv", realAtv);
		return out;
	}

	private long hGetLong(String hashKey, String field) {
		Object raw = companysRedis.opsForHash().get(hashKey, field);
		if (raw == null) {
			return 0L;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private long sCard(String key) {
		Long n = companysRedis.opsForSet().size(key);
		return n == null ? 0L : n;
	}
}
