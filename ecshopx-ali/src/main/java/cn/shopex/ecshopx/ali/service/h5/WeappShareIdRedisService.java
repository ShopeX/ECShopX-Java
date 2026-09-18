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

package cn.shopex.ecshopx.ali.service.h5;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WeappShareIdRedisService {

	private static final int DEFAULT_TTL_SECONDS = 2_592_000;

	private static final List<String> PARAM_KEYS =
			List.of("cxdid", "dtid", "smid", "uid", "distributor_id");

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public WeappShareIdRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public String getShareId(String companyIdKey, Map<String, String> rawParams) {
		Map<String, String> params = sanitizeParams(rawParams);
		String shareId = java.util.UUID.randomUUID().toString().replace("-", "");
		String prefix = companyIdKey != null ? companyIdKey : "";
		String redisKey = prefix + "_" + shareId;
		try {
			String json = objectMapper.writeValueAsString(params);
			redis.opsForValue().set(redisKey, json);
			redis.expireAt(redisKey, Instant.now().plusSeconds(DEFAULT_TTL_SECONDS));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("分享参数序列化失败");
		}
		return shareId;
	}

	private static Map<String, String> sanitizeParams(Map<String, String> raw) {
		LinkedHashMap<String, String> params = new LinkedHashMap<>();
		if (raw != null) {
			for (String k : PARAM_KEYS) {
				if (!raw.containsKey(k)) {
					continue;
				}
				String v = raw.get(k);
				if (shouldDropParamValue(v)) {
					continue;
				}
				params.put(k, v);
			}
		}
		if (params.containsKey("distributor_id") && !params.containsKey("dtid")) {
			String d = params.remove("distributor_id");
			params.put("dtid", d);
		}
		return params;
	}

	/**
	 * Whether a share parameter value is treated as absent and omitted from the stored payload.
	 * Null, blank after trim, the literal {@code "undefined"}, and {@code "0"} are dropped.
	 */
	private static boolean shouldDropParamValue(String v) {
		if (v == null) {
			return true;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return true;
		}
		if ("undefined".equals(t)) {
			return true;
		}
		if ("0".equals(t)) {
			return true;
		}
		return false;
	}
}
