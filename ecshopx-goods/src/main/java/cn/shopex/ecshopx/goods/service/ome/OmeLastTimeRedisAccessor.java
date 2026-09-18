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

package cn.shopex.ecshopx.goods.service.ome;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OmeLastTimeRedisAccessor {

	private static final String FIELD_BRAND = "brand";

	private static final String FIELD_SPEC = "spec";

	private static final String FIELD_ITEMS = "items";

	private final StringRedisTemplate redis;

	public OmeLastTimeRedisAccessor(@Qualifier("companysRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public long getBrandCursorUnix(long companyId) {
		String key = hashKey(companyId);
		Object v = redis.opsForHash().get(key, FIELD_BRAND);
		if (v == null) {
			return 0L;
		}
		String s = v.toString();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public void setBrandCursorUnix(long companyId, long unixSeconds) {
		String key = hashKey(companyId);
		redis.opsForHash().put(key, FIELD_BRAND, String.valueOf(unixSeconds));
	}

	public long getSpecCursorUnix(long companyId) {
		String key = hashKey(companyId);
		Object v = redis.opsForHash().get(key, FIELD_SPEC);
		if (v == null) {
			return 0L;
		}
		String s = v.toString();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public void setSpecCursorUnix(long companyId, long unixSeconds) {
		String key = hashKey(companyId);
		redis.opsForHash().put(key, FIELD_SPEC, String.valueOf(unixSeconds));
	}

	public long getItemsCursorUnix(long companyId) {
		String key = hashKey(companyId);
		Object v = redis.opsForHash().get(key, FIELD_ITEMS);
		if (v == null) {
			return 0L;
		}
		String s = v.toString();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public void setItemsCursorUnix(long companyId, long unixSeconds) {
		String key = hashKey(companyId);
		redis.opsForHash().put(key, FIELD_ITEMS, String.valueOf(unixSeconds));
	}

	private static String hashKey(long companyId) {
		return "LastTimeGetFromOme:" + companyId;
	}
}
