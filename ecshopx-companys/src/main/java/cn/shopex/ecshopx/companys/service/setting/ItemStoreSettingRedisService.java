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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ItemStoreSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(ItemStoreSettingRedisService.class);

	private static final String KEY_PREFIX = "ItemStoreSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public ItemStoreSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	public Map<String, Object> getItemStoreSetting(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return singleStatus(true);
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (!root.isObject()) {
				return singleStatus(true);
			}
			JsonNode node = root.get("item_store_status");
			boolean value = jsonNodeToBooleanStrictItemStore(node);
			return singleStatus(value);
		} catch (Exception e) {
			return singleStatus(true);
		}
	}

	private static Map<String, Object> singleStatus(boolean value) {
		Map<String, Object> m = new HashMap<>(1);
		m.put("item_store_status", value);
		return m;
	}

	private static boolean jsonNodeToBooleanStrictItemStore(JsonNode node) {
		if (node == null || node.isNull()) {
			return true;
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isInt()) {
			return node.asInt() != 0;
		}
		if (node.isTextual()) {
			String s = node.asText().trim();
			return "1".equals(s) || "true".equalsIgnoreCase(s);
		}
		return true;
	}

	public Map<String, Object> setItemStoreSetting(long companyId, Map<String, Object> inputdata) {
		boolean innerWrite =
				inputdata != null
						&& inputdata.containsKey("item_store_status")
						&& inputdata.get("item_store_status") != null;
		if (innerWrite) {
			boolean flag = Boolean.TRUE.equals(inputdata.get("item_store_status"));
			Map<String, Object> data = new HashMap<>(1);
			data.put("item_store_status", flag);
			try {
				companysRedisTemplate.opsForValue().set(key(companyId), objectMapper.writeValueAsString(data));
			} catch (JsonProcessingException e) {
				log.warn("Failed to serialize item store setting for companyId={}", companyId, e);
				return getItemStoreSetting(companyId);
			}
			return data;
		}
		return getItemStoreSetting(companyId);
	}
}
