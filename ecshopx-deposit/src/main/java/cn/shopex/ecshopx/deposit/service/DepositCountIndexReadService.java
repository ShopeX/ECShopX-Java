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

package cn.shopex.ecshopx.deposit.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DepositCountIndexReadService {

	private static final ZoneId DEPOSIT_STATS_ZONE = ZoneId.of("Asia/Shanghai");

	private final StringRedisTemplate redisTemplate;

	public DepositCountIndexReadService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public Map<String, Object> getDepositCountIndex(long companyId) {
		String date = LocalDate.now(DEPOSIT_STATS_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE);
		String field = String.valueOf(companyId);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("shopDepositTotal", hgetAsPayloadValue("shopDepositTotal", field));
		out.put("rechargeDayTotal", hgetAsPayloadValue("dayRechargeTotal" + date, field));
		out.put("consumeDayTotal", hgetAsPayloadValue("dayConsumeTotal" + date, field));
		return out;
	}

	/** Absent field → {@code null}; otherwise {@link String#valueOf} of the stored value. */
	private Object hgetAsPayloadValue(String hashKey, String field) {
		Object v = redisTemplate.opsForHash().get(hashKey, field);
		if (v == null) {
			return null;
		}
		return String.valueOf(v);
	}
}
