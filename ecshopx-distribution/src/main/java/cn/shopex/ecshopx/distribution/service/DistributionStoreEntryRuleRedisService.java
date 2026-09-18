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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributionStoreEntryRuleRedisService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DistributionStoreEntryRuleRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate, ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getInRule(long companyId) {
		String key = inRuleRedisKey(companyId);
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key);
		} catch (DataAccessException e) {
			throw new ResourceException("服务暂不可用");
		}

		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return writeDefaultInRuleAndReturn(key);
		}

		String trimmed = raw.trim();
		if ("null".equalsIgnoreCase(trimmed)) {
			return writeDefaultInRuleAndReturn(key);
		}

		Map<String, Object> parsed;
		try {
			parsed = objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return writeDefaultInRuleAndReturn(key);
		}

		if (parsed == null || parsed.isEmpty()) {
			return writeDefaultInRuleAndReturn(key);
		}

		return parsed;
	}

	public Map<String, Object> saveInRule(long companyId, Map<String, Object> rawBody) {
		Map<String, Object> raw = rawBody != null ? rawBody : Collections.emptyMap();

		Map<String, Object> distributorCode = requireNestedMap(raw, "distributor_code");
		Map<String, Object> shopAssistant = requireNestedMap(raw, "shop_assistant");
		Map<String, Object> shopWhite = requireNestedMap(raw, "shop_white");
		Map<String, Object> shopAssistantPro = requireNestedMap(raw, "shop_assistant_pro");

		Map<String, Object> payload = new LinkedHashMap<>();

		Map<String, Object> distributorCodeOut = new LinkedHashMap<>();
		distributorCodeOut.put("status", coerceToBoolean(distributorCode.get("status")));
		Object sortDc = distributorCode.get("sort");
		distributorCodeOut.put(
				"sort",
				sortDc == null ? Integer.valueOf(1) : coerceToNumberOrStringAsNumber(sortDc, 1));
		payload.put("distributor_code", distributorCodeOut);

		Map<String, Object> shopAssistantOut = new LinkedHashMap<>();
		shopAssistantOut.put("status", coerceToBoolean(shopAssistant.get("status")));
		shopAssistantOut.put("express_time", coerceExpressTime(shopAssistant.get("express_time")));
		Object sortSa = shopAssistant.get("sort");
		shopAssistantOut.put(
				"sort",
				sortSa == null ? Integer.valueOf(2) : coerceToNumberOrStringAsNumber(sortSa, 2));
		payload.put("shop_assistant", shopAssistantOut);

		Map<String, Object> shopWhiteOut = new LinkedHashMap<>();
		shopWhiteOut.put("status", coerceToBoolean(shopWhite.get("status")));
		Object sortSw = shopWhite.get("sort");
		shopWhiteOut.put("sort", sortSw == null ? Integer.valueOf(3) : coerceToNumberOrStringAsNumber(sortSw, 3));
		payload.put("shop_white", shopWhiteOut);

		Map<String, Object> shopAssistantProOut = new LinkedHashMap<>();
		shopAssistantProOut.put("status", coerceToBoolean(shopAssistantPro.get("status")));
		Object sortSap = shopAssistantPro.get("sort");
		shopAssistantProOut.put(
				"sort",
				sortSap == null ? Integer.valueOf(4) : coerceToNumberOrStringAsNumber(sortSap, 4));
		payload.put("shop_assistant_pro", shopAssistantProOut);

		payload.put("shop_lbs", coerceToBoolean(raw.get("shop_lbs")));
		payload.put("radio_type", truncatingIntCast(raw.get("radio_type")));
		payload.put("default_shop", raw.get("default_shop") != null ? raw.get("default_shop") : 0);
		payload.put(
				"intro_page",
				raw.get("intro_page") == null ? "" : String.valueOf(raw.get("intro_page")));

		String key = inRuleRedisKey(companyId);
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		try {
			companysRedisTemplate.opsForValue().set(key, json);
		} catch (DataAccessException e) {
			throw new ResourceException("服务暂不可用");
		}
		return payload;
	}

	private String inRuleRedisKey(long companyId) {
		return "distribution:config:inRule:" + sha1Hex(String.valueOf(companyId));
	}

	private Map<String, Object> defaultInRuleMap() {
		Map<String, Object> distributorCode = new LinkedHashMap<>();
		distributorCode.put("status", Boolean.TRUE);
		distributorCode.put("sort", Integer.valueOf(1));

		Map<String, Object> shopAssistant = new LinkedHashMap<>();
		shopAssistant.put("status", Boolean.FALSE);
		shopAssistant.put("express_time", Integer.valueOf(0));
		shopAssistant.put("sort", Integer.valueOf(2));

		Map<String, Object> shopWhite = new LinkedHashMap<>();
		shopWhite.put("status", Boolean.FALSE);
		shopWhite.put("sort", Integer.valueOf(3));

		Map<String, Object> shopAssistantPro = new LinkedHashMap<>();
		shopAssistantPro.put("status", Boolean.TRUE);
		shopAssistantPro.put("sort", Integer.valueOf(4));

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("distributor_code", distributorCode);
		out.put("shop_assistant", shopAssistant);
		out.put("shop_white", shopWhite);
		out.put("shop_assistant_pro", shopAssistantPro);
		out.put("radio_type", Integer.valueOf(1));
		out.put("default_shop", Integer.valueOf(0));
		out.put("intro_page", "");
		return out;
	}

	private Map<String, Object> writeDefaultInRuleAndReturn(String key) {
		Map<String, Object> defaultMap = defaultInRuleMap();
		String json;
		try {
			json = objectMapper.writeValueAsString(defaultMap);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		try {
			companysRedisTemplate.opsForValue().set(key, json);
		} catch (DataAccessException e) {
			throw new ResourceException("服务暂不可用");
		}
		return defaultMap;
	}

	private static Map<String, Object> requireNestedMap(Map<String, Object> raw, String blockKey) {
		Object v = raw.get(blockKey);
		if (v == null) {
			throw new BadRequestException("缺少 " + blockKey);
		}
		if (!(v instanceof Map<?, ?> m)) {
			throw new BadRequestException(blockKey + " 格式错误，应为对象");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static boolean coerceToBoolean(Object v) {
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		return "true".equalsIgnoreCase(String.valueOf(v).trim());
	}

	private static int truncatingIntCast(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Object coerceToNumberOrStringAsNumber(Object sortVal, int defaultInt) {
		if (sortVal == null) {
			return Integer.valueOf(defaultInt);
		}
		if (sortVal instanceof Number n) {
			return Integer.valueOf(n.intValue());
		}
		if (sortVal instanceof String s) {
			try {
				return Integer.valueOf(Integer.parseInt(s.trim()));
			} catch (NumberFormatException e) {
				return Integer.valueOf(defaultInt);
			}
		}
		try {
			return Integer.valueOf(Integer.parseInt(String.valueOf(sortVal).trim()));
		} catch (NumberFormatException e) {
			return Integer.valueOf(defaultInt);
		}
	}

	private static int coerceExpressTime(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
