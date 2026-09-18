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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis key {@code kujiale:config:{companyId}}: JSON with {@code appKey}, {@code appSecret},
 * {@code updated} (epoch seconds). No TTL; default Redis connection bean.
 */
@Service
public class KujialeConfigRedisService {

	private static final Logger log = LoggerFactory.getLogger(KujialeConfigRedisService.class);

	private static final String KEY_PREFIX = "kujiale:config:";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public KujialeConfigRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static Map<String, Object> emptyAppPair() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(2);
		out.put("appKey", "");
		out.put("appSecret", "");
		return out;
	}

	private static String textOrEmpty(JsonNode node) {
		if (node == null || !node.isTextual()) {
			return "";
		}
		return node.asText("");
	}

	public Map<String, Object> readConfig(long companyId) {
		try {
			String raw = sharedStringRedisTemplate.opsForValue().get(key(companyId));
			if (raw == null || raw.isBlank()) {
				return emptyAppPair();
			}
			JsonNode root = objectMapper.readTree(raw);
			LinkedHashMap<String, Object> out = new LinkedHashMap<>(2);
			out.put("appKey", textOrEmpty(root.get("appKey")));
			out.put("appSecret", textOrEmpty(root.get("appSecret")));
			return out;
		} catch (Exception e) {
			log.warn("Failed to read kujiale config for companyId={}", companyId, e);
			return emptyAppPair();
		}
	}

	public void writeConfig(long companyId, String appKey, String appSecret) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>(3);
		map.put("appKey", appKey);
		map.put("appSecret", appSecret);
		map.put("updated", Instant.now().getEpochSecond());
		try {
			String json = objectMapper.writeValueAsString(map);
			sharedStringRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			throw new ResourceException("酷家乐配置序列化失败");
		}
	}
}
