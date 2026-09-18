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

package cn.shopex.ecshopx.goods.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 分类可售过滤开关（对齐 PHP ItemsCategoryService Redis key）。
 */
@Service
public class ItemsCategorySaleableFilterService {

	private final StringRedisTemplate companysRedisTemplate;

	public ItemsCategorySaleableFilterService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public boolean isEnabled(long companyId) {
		if (companyId <= 0L) {
			return false;
		}
		String value = companysRedisTemplate.opsForValue().get(redisKey(companyId));
		return "1".equals(value);
	}

	public boolean setEnabled(long companyId, boolean enabled) {
		if (companyId <= 0L) {
			return false;
		}
		companysRedisTemplate.opsForValue().set(redisKey(companyId), enabled ? "1" : "0");
		return true;
	}

	static String redisKey(long companyId) {
		return "goods:category:saleable_filter:" + companyId;
	}
}
