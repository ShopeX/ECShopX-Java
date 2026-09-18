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

package cn.shopex.ecshopx.im.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MeiqiaConfigService {

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public MeiqiaConfigService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getInfo(long companyId) {
		String raw = redis.opsForValue().get(redisKey(companyId));
		if (!StringUtils.hasText(raw)) {
			return defaultInfo();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			if (parsed == null) {
				return defaultInfo();
			}
			return normalizeInfo(new LinkedHashMap<>(parsed));
		} catch (Exception e) {
			return defaultInfo();
		}
	}

	public Map<String, Object> saveImInfo(long companyId, Map<String, Object> postdata) {
		String channel = String.valueOf(postdata.get("channel"));
		LinkedHashMap<String, Object> meiqiaUrl = new LinkedHashMap<>();
		meiqiaUrl.put("common", trimField(postdata.get("common")));
		meiqiaUrl.put("wxapp", trimField(postdata.get("wxapp")));
		meiqiaUrl.put("h5", trimField(postdata.get("h5")));
		meiqiaUrl.put("app", trimField(postdata.get("app")));
		meiqiaUrl.put("aliapp", trimField(postdata.get("aliapp")));
		meiqiaUrl.put("pc", trimField(postdata.get("pc")));

		Object isOpenObj = postdata.get("is_open");
		Object distObj = postdata.get("is_distributor_open");
		boolean isOpen = isOpenObj instanceof Boolean b && Boolean.TRUE.equals(b);
		boolean isDistOpen = distObj instanceof Boolean b && Boolean.TRUE.equals(b);

		LinkedHashMap<String, Object> toStore = new LinkedHashMap<>();
		toStore.put("channel", channel);
		toStore.put("meiqia_url", meiqiaUrl);
		toStore.put("is_open", isOpen);
		toStore.put("is_distributor_open", isDistOpen);

		String json;
		try {
			json = objectMapper.writeValueAsString(toStore);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("美洽配置序列化失败", e);
		}
		redis.opsForValue().set(redisKey(companyId), json);
		return getInfo(companyId);
	}

	public Map<String, Object> saveDistributorMeiQia(long companyId, String distributorId, Map<String, Object> postdata) {
		LinkedHashMap<String, Object> toStore = new LinkedHashMap<>();
		toStore.put("channel", String.valueOf(postdata.get("channel")));
		LinkedHashMap<String, Object> meiqiaUrl = new LinkedHashMap<>();
		meiqiaUrl.put("common", trimField(postdata.get("common")));
		meiqiaUrl.put("wxapp", trimField(postdata.get("wxapp")));
		meiqiaUrl.put("h5", trimField(postdata.get("h5")));
		meiqiaUrl.put("app", trimField(postdata.get("app")));
		meiqiaUrl.put("aliapp", trimField(postdata.get("aliapp")));
		meiqiaUrl.put("pc", trimField(postdata.get("pc")));
		toStore.put("meiqia_url", meiqiaUrl);

		String json;
		try {
			json = objectMapper.writeValueAsString(toStore);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("美洽配置序列化失败", e);
		}
		redis.opsForValue().set(redisKeyDistributor(companyId, distributorId), json);
		return getDistributorMeiQia(companyId, distributorId);
	}

	public Map<String, Object> getDistributorMeiQia(long companyId, String distributorId) {
		Map<String, Object> result = new LinkedHashMap<>(getInfo(companyId));
		if (!Boolean.TRUE.equals(result.get("is_distributor_open"))) {
			return result;
		}
		String raw = redis.opsForValue().get(redisKeyDistributor(companyId, distributorId));
		if (!StringUtils.hasText(raw)) {
			return result;
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			if (parsed == null) {
				return result;
			}
			LinkedHashMap<String, Object> replaced = new LinkedHashMap<>(parsed);
			patchDistributorMeiqiaUrlDefaults(replaced);
			return replaced;
		} catch (Exception e) {
			return result;
		}
	}

	private static void patchDistributorMeiqiaUrlDefaults(LinkedHashMap<String, Object> out) {
		Object urlObj = out.get("meiqia_url");
		if (!(urlObj instanceof Map<?, ?> urlMap)) {
			out.put("meiqia_url", defaultMeiqiaUrl());
			return;
		}
		LinkedHashMap<String, Object> fixed = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : urlMap.entrySet()) {
			Object v = e.getValue();
			fixed.put(String.valueOf(e.getKey()), v == null ? "" : String.valueOf(v).trim());
		}
		for (String key : List.of("common", "wxapp", "h5", "app", "aliapp", "pc")) {
			fixed.putIfAbsent(key, "");
		}
		out.put("meiqia_url", fixed);
	}

	private Map<String, Object> normalizeInfo(LinkedHashMap<String, Object> out) {
		Object urlObj = out.get("meiqia_url");
		if (!(urlObj instanceof Map<?, ?> urlMap)) {
			out.put("meiqia_url", defaultMeiqiaUrl());
		} else {
			LinkedHashMap<String, Object> fixed = new LinkedHashMap<>();
			putUrlString(fixed, urlMap, "common");
			putUrlString(fixed, urlMap, "wxapp");
			putUrlString(fixed, urlMap, "h5");
			putUrlString(fixed, urlMap, "app");
			putUrlString(fixed, urlMap, "aliapp");
			putUrlString(fixed, urlMap, "pc");
			out.put("meiqia_url", fixed);
		}
		if (!out.containsKey("is_open") || out.get("is_open") == null) {
			out.put("is_open", Boolean.FALSE);
		} else {
			out.put("is_open", toOutputBoolean(out.get("is_open")));
		}
		if (!out.containsKey("is_distributor_open") || out.get("is_distributor_open") == null) {
			out.put("is_distributor_open", Boolean.FALSE);
		} else {
			out.put("is_distributor_open", toOutputBoolean(out.get("is_distributor_open")));
		}
		return out;
	}

	private static void putUrlString(LinkedHashMap<String, Object> target, Map<?, ?> src, String key) {
		Object v = src.get(key);
		target.put(key, v == null ? "" : String.valueOf(v).trim());
	}

	private static boolean toOutputBoolean(Object v) {
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		return "true".equals(String.valueOf(v).trim());
	}

	private static String trimField(Object v) {
		return Objects.toString(v, "").trim();
	}

	private static Map<String, Object> defaultMeiqiaUrl() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("common", "");
		m.put("wxapp", "");
		m.put("h5", "");
		m.put("app", "");
		m.put("aliapp", "");
		m.put("pc", "");
		return m;
	}

	private static Map<String, Object> defaultInfo() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("channel", "single");
		m.put("meiqia_url", defaultMeiqiaUrl());
		m.put("is_open", Boolean.FALSE);
		m.put("is_distributor_open", Boolean.FALSE);
		return m;
	}

	private static String redisKey(long companyId) {
		return "im:meiqia:" + companyId;
	}

	private static String redisKeyDistributor(long companyId, String distributorId) {
		return "im:meiqia:distributor:" + companyId + ":" + distributorId;
	}
}
