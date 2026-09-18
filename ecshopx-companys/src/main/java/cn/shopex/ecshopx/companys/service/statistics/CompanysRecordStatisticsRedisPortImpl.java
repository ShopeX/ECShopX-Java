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

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 委托 {@code companysRedisTemplate}，与 PHP 默认 Redis 连接行为对齐。
 */
@Component
@Profile("!test-cron")
public class CompanysRecordStatisticsRedisPortImpl implements CompanysRecordStatisticsRedisPort {

	private final StringRedisTemplate companysRedisTemplate;

	public CompanysRecordStatisticsRedisPortImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	@Override
	public long scard(String key) {
		Long s = companysRedisTemplate.opsForSet().size(key);
		return s == null ? 0L : s;
	}

	@Override
	public void expireAt(String key, long epochSeconds) {
		companysRedisTemplate.expireAt(key, new Date(epochSeconds * 1000L));
	}

	@Override
	public Map<String, String> hgetall(String key) {
		Map<Object, Object> raw = companysRedisTemplate.opsForHash().entries(key);
		if (raw.isEmpty()) {
			return Map.of();
		}
		return raw.entrySet().stream()
				.collect(Collectors.toMap(
						e -> e.getKey().toString(), e -> e.getValue() == null ? "" : e.getValue().toString(), (a, b) -> a, HashMap::new));
	}

	@Override
	public String get(String key) {
		return companysRedisTemplate.opsForValue().get(key);
	}
}
