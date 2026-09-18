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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CompanysDepositDayRechargeRedisReader {

	private final StringRedisTemplate depositRedis;

	public CompanysDepositDayRechargeRedisReader(
			@Qualifier("depositStringRedisTemplate") StringRedisTemplate depositRedis) {
		this.depositRedis = depositRedis;
	}

	public long getRechargeTotal(long companyId, String ymdBasicIsoDate) {
		String hashKey = "dayRechargeTotal" + ymdBasicIsoDate;
		Object raw = depositRedis.opsForHash().get(hashKey, String.valueOf(companyId));
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
}
