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

package cn.shopex.ecshopx.orders.service.kdniao;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class KdniaoQinglongSettingAdminService {

	private static final String REDIS_KEY_PREFIX = "kdniaoQingLongSetting:";

	private final StringRedisTemplate stringRedisTemplate;

	public KdniaoQinglongSettingAdminService(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void setQinglongcode(long companyId, Map<String, Object> merged) {
		if (!merged.containsKey("qinglong_code")) {
			throw new BadRequestException("青龙物流编码必填");
		}
		Object raw = merged.get("qinglong_code");
		if (raw == null) {
			throw new BadRequestException("青龙物流编码必填");
		}
		if (raw instanceof Collection<?> c && c.isEmpty()) {
			throw new BadRequestException("青龙物流编码必填");
		}
		if (raw instanceof Map<?, ?> m && m.isEmpty()) {
			throw new BadRequestException("青龙物流编码必填");
		}
		if (raw instanceof CharSequence cs && cs.toString().trim().isEmpty()) {
			throw new BadRequestException("青龙物流编码必填");
		}
		String code = String.valueOf(raw).trim();
		if (code.isEmpty()) {
			throw new BadRequestException("青龙物流编码必填");
		}
		String key = REDIS_KEY_PREFIX + sha1Hex(String.valueOf(companyId));
		stringRedisTemplate.opsForValue().set(key, code);
	}

	public String getQinglongcode(long companyId) {
		String key = REDIS_KEY_PREFIX + sha1Hex(String.valueOf(companyId));
		String raw = stringRedisTemplate.opsForValue().get(key);
		return raw == null ? "" : raw;
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
