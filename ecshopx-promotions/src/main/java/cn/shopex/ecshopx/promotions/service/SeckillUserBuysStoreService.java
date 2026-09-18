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

package cn.shopex.ecshopx.promotions.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 限时特惠/秒杀用户已购件数与金额（Redis seckill_buy_data:{companyId}）。
 */
@Service
public class SeckillUserBuysStoreService {

	private final StringRedisTemplate companysRedisTemplate;

	public SeckillUserBuysStoreService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void setUserBuysStore(long seckillId, long companyId, long userId, long itemId, int store, long price) {
		if (store == 0 && price == 0L) {
			return;
		}
		String hashKey = "seckill_buy_data:" + companyId;
		String buyStoreKey = "user_buy_store:" + seckillId + ":" + userId + ":" + itemId;
		String buyPriceKey = "user_buy_price:" + seckillId + ":" + userId + ":" + itemId;
		String buyTotalStore = "user_buy_total_store:" + seckillId + ":" + userId;
		String buyTotalPrice = "user_buy_total_price:" + seckillId + ":" + userId;
		companysRedisTemplate.opsForHash().increment(hashKey, buyStoreKey, store);
		companysRedisTemplate.opsForHash().increment(hashKey, buyPriceKey, price);
		companysRedisTemplate.opsForHash().increment(hashKey, buyTotalStore, store);
		companysRedisTemplate.opsForHash().increment(hashKey, buyTotalPrice, price);
	}

	public Map<String, Object> getUserBuysData(long seckillId, long companyId, long userId, long itemId) {
		String buyStoreKey = "user_buy_store:" + seckillId + ":" + userId + ":" + itemId;
		String buyPriceKey = "user_buy_price:" + seckillId + ":" + userId + ":" + itemId;
		String buyTotalStore = "user_buy_total_store:" + seckillId + ":" + userId;
		String buyTotalPrice = "user_buy_total_price:" + seckillId + ":" + userId;
		String hashKey = "seckill_buy_data:" + companyId;
		List<Object> result =
				companysRedisTemplate
						.opsForHash()
						.multiGet(hashKey, List.of(buyStoreKey, buyPriceKey, buyTotalStore, buyTotalPrice));
		long userBuyStore = redisLong(result, 0);
		long userBuyPrice = redisLong(result, 1);
		long userBuyTotalStore = redisLong(result, 2);
		long userBuyTotalPrice = redisLong(result, 3);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("userBuyStore", userBuyStore);
		data.put("userBuyPrice", userBuyPrice);
		data.put("userBuyTotalStore", userBuyTotalStore);
		data.put("userBuyTotalPrcie", userBuyTotalPrice);
		return data;
	}

	private static long redisLong(List<Object> result, int index) {
		if (result == null || index >= result.size()) {
			return 0L;
		}
		Object v = result.get(index);
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
