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

package cn.shopex.ecshopx.distribution.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 门店列表背景图 Redis 读写，键规则与 PHP {@code DistributorService::getDistributorListBackgroundKey} 一致。
 */
@Service
public class DistributorListBackgroundRedisService {

	private final StringRedisTemplate sharedStringRedisTemplate;

	public DistributorListBackgroundRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public String getBackgroundUrl(long companyId) {
		String raw = sharedStringRedisTemplate.opsForValue().get(redisKey(companyId));
		if (!StringUtils.hasText(raw) || "null".equalsIgnoreCase(raw.trim())) {
			return "";
		}
		return raw.trim();
	}

	public boolean setBackgroundUrl(long companyId, String backgroundUrl) {
		sharedStringRedisTemplate.opsForValue().set(redisKey(companyId), backgroundUrl);
		return true;
	}

	private static String redisKey(long companyId) {
		return "distributor_list_background:" + companyId;
	}
}
