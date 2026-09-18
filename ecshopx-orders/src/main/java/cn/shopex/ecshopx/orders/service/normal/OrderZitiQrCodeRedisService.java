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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderZitiQrCodeRedisService {

	private static final String KEY_PREFIX = "orderziticode:";

	private final StringRedisTemplate redis;

	public OrderZitiQrCodeRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.redis = stringRedisTemplate;
	}

	public void saveZitiCodeOrderMapping(String code, long orderId, Duration ttl) {
		String fullKey = KEY_PREFIX + code;
		redis.opsForValue().set(fullKey, String.valueOf(orderId), ttl);
	}

	public long resolveOrderIdOrThrow(String code) {
		Long mapped = resolveOrderIdNullable(code);
		if (mapped == null) {
			throw new ResourceException("核销码已过期");
		}
		return mapped;
	}

	/** Redis GET orderziticode:{code}；无映射或非法数字返回 null。 */
	public Long resolveOrderIdNullable(String code) {
		if (!StringUtils.hasText(code)) {
			return null;
		}
		String raw = redis.opsForValue().get(KEY_PREFIX + code);
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String s = raw.trim();
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
