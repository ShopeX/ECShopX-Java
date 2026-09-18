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

package cn.shopex.ecshopx.pointsmall.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class PointsmallBaseSettingWriteService {

	private static final String REDIS_HASH_KEY = "pointsmall_setting";

	private static final String HSET_LUA = "return redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])";

	private static final DefaultRedisScript<Long> HSET_STATUS_SCRIPT = new DefaultRedisScript<>();

	static {
		HSET_STATUS_SCRIPT.setScriptText(HSET_LUA);
		HSET_STATUS_SCRIPT.setResultType(Long.class);
	}

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public PointsmallBaseSettingWriteService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public long save(long companyId, Map<String, Object> rawParams) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("freight_type", rawParams.get("freight_type"));
		payload.put("proportion", rawParams.get("proportion"));
		payload.put("rounding_mode", rawParams.get("rounding_mode"));

		Object ent = rawParams.get("entrance");
		Map<?, ?> entranceRaw;
		if (ent instanceof Map<?, ?> m) {
			entranceRaw = m;
		} else {
			entranceRaw = new LinkedHashMap<>();
		}

		LinkedHashMap<String, Object> entranceNorm = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : entranceRaw.entrySet()) {
			if (!(e.getKey() instanceof String k)) {
				continue;
			}
			if ("mobile_openstatus".equals(k) || "pc_openstatus".equals(k)) {
				continue;
			}
			entranceNorm.put(k, e.getValue());
		}

		boolean mobileOpen = entranceRaw.containsKey("mobile_openstatus")
				&& Objects.toString(entranceRaw.get("mobile_openstatus"), "").equals("true");
		entranceNorm.put("mobile_openstatus", mobileOpen);

		boolean pcOpen = entranceRaw.containsKey("pc_openstatus")
				&& Objects.toString(entranceRaw.get("pc_openstatus"), "").equals("true");
		entranceNorm.put("pc_openstatus", pcOpen);

		payload.put("entrance", entranceNorm);

		final String jsonString;
		try {
			jsonString = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize pointsmall base setting", e);
		}

		Long raw = redis.execute(
				HSET_STATUS_SCRIPT,
				Collections.singletonList(REDIS_HASH_KEY),
				String.valueOf(companyId),
				jsonString);
		if (raw == null) {
			throw new IllegalStateException("Redis HSET returned null for pointsmall_setting");
		}
		return raw.longValue();
	}
}
