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
import org.springframework.util.StringUtils;

@Component
public class CompanysItemWarningStoreReader {

	private static final int DEFAULT_WARNING = 5;

	private final StringRedisTemplate companysRedisTemplate;

	public CompanysItemWarningStoreReader(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public int readPlatformWarningStore(long companyId) {
		String key = "item_warning_store:" + companyId;
		String v = companysRedisTemplate.opsForValue().get(key);
		return parseIntOrDefault(v, DEFAULT_WARNING);
	}

	private static int parseIntOrDefault(String v, int d) {
		if (!StringUtils.hasText(v)) {
			return d;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return d;
		}
	}
}
