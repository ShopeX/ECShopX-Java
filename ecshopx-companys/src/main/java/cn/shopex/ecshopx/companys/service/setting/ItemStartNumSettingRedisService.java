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
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ItemStartNumSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(ItemStartNumSettingRedisService.class);

	private static final String KEY_PREFIX = "ItemStartNum:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final FalseStringLooseEqualityHelper.Mode itemStartNumLooseEqualsMode;

	public ItemStartNumSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.companys.item-start-num.weak-string-equals-mode:MODERN}")
					String itemStartNumLooseEqualsModeRaw) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.itemStartNumLooseEqualsMode =
				FalseStringLooseEqualityHelper.fromProperty(itemStartNumLooseEqualsModeRaw);
	}

	private String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static Map<String, Object> singleItemStartNum(boolean value) {
		Map<String, Object> m = new HashMap<>(1);
		m.put("item_start_num", value);
		return m;
	}

	public Map<String, Object> getItemStartNumSetting(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return singleItemStartNum(true);
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (!root.isObject()) {
				return singleItemStartNum(true);
			}
			JsonNode node = root.get("item_start_num");
			if (node == null || node.isNull()) {
				return singleItemStartNum(true);
			}
			if (node.isBoolean()) {
				return singleItemStartNum(node.booleanValue());
			}
			if (node.isNumber()) {
				Map<String, Object> out = new HashMap<>(1);
				out.put("item_start_num", node.numberValue());
				return out;
			}
			if (node.isTextual()) {
				Map<String, Object> out = new HashMap<>(1);
				out.put("item_start_num", node.textValue());
				return out;
			}
			log.warn("Unexpected item_start_num JSON type for companyId={}", companyId);
			return singleItemStartNum(true);
		} catch (Exception e) {
			log.warn("Failed to parse item start num setting for companyId={}", companyId, e);
			return singleItemStartNum(true);
		}
	}

	public Map<String, Object> setItemStartNumSetting(long companyId, Map<String, Object> inputdata) {
		boolean innerWrite =
				inputdata != null
						&& inputdata.containsKey("item_start_num")
						&& inputdata.get("item_start_num") != null;
		if (innerWrite) {
			boolean eq =
					FalseStringLooseEqualityHelper.looselyEqualsFalseString(
							inputdata.get("item_start_num"), itemStartNumLooseEqualsMode);
			boolean flag = !eq;
			Map<String, Object> data = new HashMap<>(1);
			data.put("item_start_num", flag);
			try {
				companysRedisTemplate.opsForValue().set(key(companyId), objectMapper.writeValueAsString(data));
			} catch (JsonProcessingException e) {
				throw new ResourceException("保存自营商品起订量设置失败");
			}
			return data;
		}
		return getItemStartNumSetting(companyId);
	}
}
