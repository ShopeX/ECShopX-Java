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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PointsmallFrontSettingReadService {

	private static final String REDIS_TEMPLATE_HASH = "pointsmall_template_setting";

	private static final String REDIS_BASE_HASH = "pointsmall_setting";

	private final StringRedisTemplate redis;

	private final ObjectMapper objectMapper;

	public PointsmallFrontSettingReadService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getAdminTemplateSetting(long companyId) {
		LinkedHashMap<String, Object> merged = mergeTemplateLayer(companyId);
		return new LinkedHashMap<>(merged);
	}

	public Map<String, Object> getAdminBaseSetting(long companyId) {
		LinkedHashMap<String, Object> defaultBase = defaultBaseSetting();
		Map<String, Object> decodedBase = decodeHashJsonField(REDIS_BASE_HASH, companyId);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(defaultBase);
		merged.putAll(decodedBase);
		return new LinkedHashMap<>(merged);
	}

	public Map<String, Object> getH5MergedSetting(long companyId) {
		LinkedHashMap<String, Object> mergedTemplate = mergeTemplateLayer(companyId);

		LinkedHashMap<String, Object> defaultBase = defaultBaseSetting();
		Map<String, Object> decodedBase = decodeHashJsonField(REDIS_BASE_HASH, companyId);
		LinkedHashMap<String, Object> baseMerged = new LinkedHashMap<>(defaultBase);
		baseMerged.putAll(decodedBase);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>(mergedTemplate);
		Map<String, Object> normalizedEntrance = normalizeEntranceForResponse(baseMerged.get("entrance"));
		result.put("entrance", normalizedEntrance);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("pc_banner", result.get("pc_banner"));
		out.put("screen", result.get("screen"));
		out.put("entrance", result.get("entrance"));
		return out;
	}

	private LinkedHashMap<String, Object> mergeTemplateLayer(long companyId) {
		LinkedHashMap<String, Object> defaultTemplate = defaultTemplateSetting();
		Map<String, Object> decodedTemplate = decodeHashJsonField(REDIS_TEMPLATE_HASH, companyId);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(defaultTemplate);
		merged.putAll(decodedTemplate);
		return merged;
	}

	private static LinkedHashMap<String, Object> defaultTemplateSetting() {
		LinkedHashMap<String, Object> def = new LinkedHashMap<>();
		List<String> banners = new ArrayList<>(3);
		banners.add(
				"https://b-img-cdn.yuanyuanke.cn/image/21/2021/03/05/f9d2d5928c7c9ec97f4b1e2a473f44078PjR2ONSsqs5xTza01jT53e437T35ILB");
		banners.add(
				"https://b-img-cdn.yuanyuanke.cn/image/21/2021/03/05/60b682d7fe7844539eb4ad2c2587f73bNEOPNYYDAUgN6oZlEnbLPrZ4W4YE0vuY");
		banners.add(
				"https://b-img-cdn.yuanyuanke.cn/image/21/2021/03/05/63807f7b8ce3809e1bfef62fd769d1ce5OVDXyS6cvL9M9ta07d1eIAP11fWAXlk");
		def.put("pc_banner", banners);

		LinkedHashMap<String, Object> screen = new LinkedHashMap<>();
		screen.put("brand_openstatus", Boolean.TRUE);
		screen.put("cat_openstatus", Boolean.TRUE);
		screen.put("point_openstatus", Boolean.FALSE);
		screen.put("point_section", new ArrayList<>());
		def.put("screen", screen);
		return def;
	}

	private static LinkedHashMap<String, Object> defaultBaseSetting() {
		LinkedHashMap<String, Object> def = new LinkedHashMap<>();
		def.put("freight_type", "cash");
		def.put("proportion", "1");
		def.put("rounding_mode", "down");
		LinkedHashMap<String, Object> entrance = new LinkedHashMap<>();
		entrance.put("mobileterminal_openstatus", Boolean.FALSE);
		entrance.put("pc_openstatus", Boolean.FALSE);
		def.put("entrance", entrance);
		return def;
	}

	private Map<String, Object> decodeHashJsonField(String hashKey, long companyId) {
		Object rawObj = redis.opsForHash().get(hashKey, String.valueOf(companyId));
		String raw;
		if (rawObj instanceof String s) {
			raw = s;
		} else if (rawObj == null) {
			raw = null;
		} else {
			raw = rawObj.toString();
		}
		if (raw == null || raw.isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			return Collections.emptyMap();
		} catch (RuntimeException e) {
			return Collections.emptyMap();
		}
	}

	private static Map<String, Object> normalizeEntranceForResponse(Object entranceVal) {
		Map<?, ?> raw;
		if (entranceVal instanceof Map<?, ?> m) {
			raw = m;
		} else {
			raw = Collections.emptyMap();
		}
		boolean mobile = truthyEntranceFlag(raw, "mobile_openstatus")
				|| truthyEntranceFlag(raw, "mobileterminal_openstatus");
		boolean pc = truthyEntranceFlag(raw, "pc_openstatus");
		LinkedHashMap<String, Object> norm = new LinkedHashMap<>();
		norm.put("mobile_openstatus", mobile);
		norm.put("pc_openstatus", pc);
		return norm;
	}

	private static boolean truthyEntranceFlag(Map<?, ?> map, String key) {
		Object v = map.get(key);
		if (v instanceof Boolean b) {
			return b;
		}
		return Objects.toString(v, "").equals("true");
	}
}
