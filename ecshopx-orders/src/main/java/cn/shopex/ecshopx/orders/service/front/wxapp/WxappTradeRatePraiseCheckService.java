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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappTradeRatePraiseCheckService {

	private final StringRedisTemplate sharedStringRedisTemplate;

	public WxappTradeRatePraiseCheckService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public boolean ratePraiseCheck(long companyId, long userId, long rateId) {
		return ratePraiseCheck(companyId, userId, String.valueOf(rateId));
	}

	public boolean ratePraiseCheck(long companyId, long userId, String rateIdForRedisKey) {
		String key = "ratePraiseUser:" + companyId + ":" + rateIdForRedisKey;
		Object raw = sharedStringRedisTemplate.opsForHash().get(key, String.valueOf(userId));
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		String s = String.valueOf(raw);
		if (s.isEmpty() || "0".equals(s)) {
			return false;
		}
		return true;
	}
}
