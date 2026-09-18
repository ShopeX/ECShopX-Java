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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CategoryPageSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(CategoryPageSettingRedisService.class);

	private static final String KEY_PREFIX = "CategoryPageSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public CategoryPageSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	public Object getCategoryPageSetting(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read category page setting from Redis for companyId={}", companyId, e);
			throw new ResourceException("获取分类页设置失败");
		}

		if (raw == null || raw.isBlank()) {
			Map<String, Object> def = new HashMap<>(1);
			def.put("style", "category");
			return def;
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse category page setting JSON for companyId={}", companyId, e);
			return null;
		}

		if (root.isNull() || !root.isContainerNode()) {
			return null;
		}

		if (root.isObject()) {
			return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
		}
		if (root.isArray()) {
			return objectMapper.convertValue(root, new TypeReference<List<Object>>() {});
		}
		return null;
	}

	public Map<String, Object> save(long companyId, String style) {
		Map<String, Object> data = new HashMap<>(1);
		data.put("style", style);
		try {
			String json = objectMapper.writeValueAsString(data);
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize category page setting for companyId={}", companyId, e);
			throw new ResourceException("保存分类页设置失败");
		} catch (DataAccessException e) {
			log.warn("Failed to write category page setting to Redis for companyId={}", companyId, e);
			throw new ResourceException("保存分类页设置失败");
		}

		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (DataAccessException e) {
			log.warn("Failed to read back category page setting for companyId={}", companyId, e);
			throw new ResourceException("保存分类页设置失败");
		}

		if (raw == null || raw.isBlank()) {
			log.warn("Category page setting read back empty for companyId={}", companyId);
			throw new ResourceException("保存分类页设置失败");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse category page setting JSON for companyId={}", companyId, e);
			throw new ResourceException("保存分类页设置失败");
		}

		if (!root.isObject()) {
			log.warn("Category page setting root is not JSON object for companyId={}", companyId);
			throw new ResourceException("保存分类页设置失败");
		}

		JsonNode styleNode = root.get("style");
		if (styleNode == null || styleNode.isNull()) {
			log.warn("Category page setting missing style field for companyId={}", companyId);
			throw new ResourceException("保存分类页设置失败");
		}

		if (!styleNode.isTextual()) {
			log.warn("Category page setting style is not textual for companyId={}", companyId);
			throw new ResourceException("保存分类页设置失败");
		}

		String readBack = styleNode.asText();
		if (!style.equals(readBack)) {
			log.warn(
					"Category page setting read back mismatch for companyId={}, expected={}, actual={}",
					companyId,
					style,
					readBack);
			throw new ResourceException("保存分类页设置失败");
		}

		Map<String, Object> result = new HashMap<>(1);
		result.put("style", style);
		return result;
	}
}
