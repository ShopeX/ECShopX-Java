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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.config.TrustLoginDefaultProperties;
import cn.shopex.ecshopx.members.service.trustlogin.TrustLoginConfigSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminTrustLoginListService {

	private static final Logger log = LoggerFactory.getLogger(AdminTrustLoginListService.class);

	private static final String REDIS_KEY_PREFIX = "trustlogin_config_";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final TrustLoginDefaultProperties trustLoginDefaultProperties;

	public AdminTrustLoginListService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			TrustLoginDefaultProperties trustLoginDefaultProperties) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.trustLoginDefaultProperties = trustLoginDefaultProperties;
	}

	/** B1：merge + 写回 Redis + admin 脱敏。 */
	public Map<String, Object> getTrustLoginList(long companyId) {
		Map<String, Object> merged = loadMergeAndPersist(companyId);
		return TrustLoginConfigSupport.sanitizeRoot(merged, false);
	}

	/** C1 等：merge + 写回 Redis + front 脱敏。 */
	public List<Map<String, Object>> getTrustLoginListForFront(long companyId, String versionTag) {
		Map<String, Object> merged = loadMergeAndPersist(companyId);
		Map<String, Object> sanitized = TrustLoginConfigSupport.sanitizeRoot(merged, true);
		Object slice = sanitized.get(versionTag);
		if (!(slice instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : map.entrySet()) {
					if (e.getKey() != null) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
				}
				out.add(row);
			}
		}
		return out;
	}

	public Map<String, Object> getConfigRow(String type, String version, long companyId) {
		Map<String, Object> merged = loadMergeAndPersist(companyId);
		return TrustLoginConfigSupport.getConfigRow(merged, type, version);
	}

	/**
	 * 读取 Redis；空则写默认；非空则 merge 缺 type / extra_config 后写回。
	 */
	public Map<String, Object> loadMergeAndPersist(long companyId) {
		String key = REDIS_KEY_PREFIX + companyId;
		String raw = null;
		try {
			raw = sharedStringRedisTemplate.opsForValue().get(key);
		} catch (DataAccessException e) {
			log.warn("trustlogin redis read failed: companyId={}", companyId, e);
		}
		if (raw == null || raw.isBlank()) {
			return materializeAndCacheDefaults(companyId, key);
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			if (parsed == null) {
				return materializeAndCacheDefaults(companyId, key);
			}
			Map<String, Object> merged =
					TrustLoginConfigSupport.mergeTrustLoginConfig(
							parsed, trustLoginDefaultProperties.snapshotAsResponseMap());
			persistConfig(companyId, key, merged);
			return merged;
		} catch (Exception e) {
			log.warn("trustlogin config parse failed: companyId={}", companyId, e);
			return materializeAndCacheDefaults(companyId, key);
		}
	}

	public boolean saveStatusSetting(long companyId, Map<String, Object> params) {
		Map<String, Object> root = loadMergeAndPersist(companyId);
		Object loginVersionObj = params.get("loginversion");
		if (loginVersionObj == null) {
			return false;
		}
		String loginVersion = String.valueOf(loginVersionObj).trim();
		if (loginVersion.isEmpty() || !root.containsKey(loginVersion)) {
			return false;
		}
		Object sectionObj = root.get(loginVersion);
		if (!(sectionObj instanceof List<?>)) {
			return false;
		}
		List<Map<String, Object>> editVersion = copySection(sectionObj);
		Object typeObj = params.get("type");
		if (typeObj == null) {
			return false;
		}
		String type = String.valueOf(typeObj).trim();
		if (type.isEmpty()) {
			return false;
		}
		Map<String, Object> rowResult = null;
		int rowIndex = -1;
		for (int i = 0; i < editVersion.size(); i++) {
			Map<String, Object> row = editVersion.get(i);
			Object t = row.get("type");
			if (t != null && type.equals(String.valueOf(t))) {
				rowResult = row;
				rowIndex = i;
				break;
			}
		}
		if (rowResult == null) {
			return false;
		}
		for (Map.Entry<String, Object> e : rowResult.entrySet()) {
			String k = e.getKey();
			if ("extra_config".equals(k)) {
				continue;
			}
			if (params.containsKey(k)) {
				e.setValue(params.get(k));
			}
		}
		if (params.containsKey("extra_config")) {
			String existing = String.valueOf(rowResult.getOrDefault("extra_config", ""));
			String incoming =
					TrustLoginConfigSupport.normalizeExtraConfig(params.get("extra_config"), objectMapper);
			String mergedExtra = TrustLoginConfigSupport.mergeExtraConfigOnSave(existing, incoming, objectMapper);
			rowResult.put("extra_config", mergedExtra);
			if ("apple".equals(type)) {
				TrustLoginConfigSupport.assertAppleExtraConfigValid(mergedExtra, objectMapper);
			}
		} else if (!rowResult.containsKey("extra_config")) {
			rowResult.put("extra_config", "");
		}
		if (rowIndex >= 0) {
			editVersion.set(rowIndex, rowResult);
		}
		root.put(loginVersion, editVersion);
		persistConfig(companyId, REDIS_KEY_PREFIX + companyId, root);
		return true;
	}

	private Map<String, Object> materializeAndCacheDefaults(long companyId, String key) {
		Map<String, Object> fresh = trustLoginDefaultProperties.snapshotAsResponseMap();
		persistConfig(companyId, key, fresh);
		return fresh;
	}

	private void persistConfig(long companyId, String key, Map<String, Object> config) {
		String json;
		try {
			json = objectMapper.writeValueAsString(config);
		} catch (Exception e) {
			log.error("trustlogin config serialize failed: companyId={}", companyId, e);
			throw new ResourceException("信任登录配置初始化失败");
		}
		try {
			sharedStringRedisTemplate.opsForValue().set(key, json);
		} catch (DataAccessException e) {
			log.warn("trustlogin redis set failed: companyId={}", companyId, e);
		}
	}

	private static List<Map<String, Object>> copySection(Object sectionObj) {
		List<Map<String, Object>> editVersion = new ArrayList<>();
		if (!(sectionObj instanceof List<?> sectionList)) {
			return editVersion;
		}
		for (Object item : sectionList) {
			if (!(item instanceof Map<?, ?> src)) {
				continue;
			}
			Map<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : src.entrySet()) {
				if (e.getKey() != null) {
					copy.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			editVersion.add(copy);
		}
		return editVersion;
	}
}
