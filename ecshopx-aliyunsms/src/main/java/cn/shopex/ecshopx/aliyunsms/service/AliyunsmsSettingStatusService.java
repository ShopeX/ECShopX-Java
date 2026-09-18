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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSettingStatusService {

	private static final String REDIS_KEY = "aliyunsms:status:";

	private final StringRedisTemplate stringRedisTemplate;

	public AliyunsmsSettingStatusService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public boolean getStatus(long companyId) {
		String field = DigestUtils.sha1Hex(String.valueOf(companyId));
		Object raw = stringRedisTemplate.opsForHash().get(REDIS_KEY, field);
		if (raw == null) {
			return false;
		}
		String v = raw instanceof String s ? s : String.valueOf(raw);
		return "true".equals(v);
	}

	public void setStatus(long companyId, Object statusRaw, boolean statusKeyPresent) {
		String field = DigestUtils.sha1Hex(String.valueOf(companyId));
		var ops = stringRedisTemplate.opsForHash();

		if (!statusKeyPresent) {
			ops.delete(REDIS_KEY, field);
			return;
		}
		if (statusRaw == null) {
			ops.delete(REDIS_KEY, field);
			return;
		}
		if (statusRaw instanceof Boolean b) {
			if (Boolean.FALSE.equals(b)) {
				ops.delete(REDIS_KEY, field);
			} else {
				ops.put(REDIS_KEY, field, "true");
			}
			return;
		}
		if (statusRaw instanceof Number n) {
			ops.put(REDIS_KEY, field, numberToRedisString(n));
			return;
		}
		if (statusRaw instanceof String s) {
			ops.put(REDIS_KEY, field, s);
			return;
		}
		if (statusRaw instanceof Map<?, ?> || statusRaw instanceof List<?>) {
			throw new BadRequestException("参数类型错误: 不接受数组");
		}
		ops.put(REDIS_KEY, field, String.valueOf(statusRaw));
	}

	private static String numberToRedisString(Number n) {
		double d = n.doubleValue();
		if (Double.isFinite(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE && d == Math.rint(d)) {
			return Long.toString((long) d);
		}
		return n.toString();
	}
}
