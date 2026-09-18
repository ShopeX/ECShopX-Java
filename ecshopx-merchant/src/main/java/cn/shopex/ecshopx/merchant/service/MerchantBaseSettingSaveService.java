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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MerchantBaseSettingSaveService {

	private final ObjectMapper redisSettlementAgreementJsonMapper;
	private final StringRedisTemplate companysRedisTemplate;

	public MerchantBaseSettingSaveService(
			ObjectMapper objectMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.redisSettlementAgreementJsonMapper =
				objectMapper.copy().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public Map<String, Object> loadCompanyBaseSetting(long companyId) {
		LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();
		defaults.put("status", "false");
		defaults.put("display_on_pc", "false");
		defaults.put("settled_type", new ArrayList<String>());
		defaults.put("content", "");

		String key = "settlementAgreement:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>(defaults);
		if (raw != null && !raw.isEmpty()) {
			try {
				Map<String, Object> decoded =
						redisSettlementAgreementJsonMapper.readValue(raw, new TypeReference<>() {});
				if (decoded != null) {
					result.putAll(decoded);
				}
			} catch (Exception e) {
				result = new LinkedHashMap<>(defaults);
			}
		}

		Object statusRaw = result.get("status");
		result.put(
				"status",
				"false".equals(String.valueOf(statusRaw).trim()) ? Boolean.FALSE : Boolean.TRUE);

		Object dop = result.get("display_on_pc");
		String displayOnPc;
		if (dop instanceof Boolean b) {
			displayOnPc = b ? "true" : "false";
		} else if (dop instanceof String s) {
			displayOnPc = "true".equalsIgnoreCase(s.trim()) ? "true" : "false";
		} else {
			displayOnPc = "false";
		}
		result.put("display_on_pc", displayOnPc);

		Object settled = result.get("settled_type");
		List<String> settledList = new ArrayList<>();
		if (settled instanceof Collection<?> c) {
			for (Object o : c) {
				settledList.add(String.valueOf(o));
			}
		} else if (settled instanceof String[] arr) {
			for (String o : arr) {
				settledList.add(String.valueOf(o));
			}
		}
		result.put("settled_type", settledList);

		Object contentVal = result.get("content");
		String contentStr =
				contentVal == null
						? ""
						: (contentVal instanceof String cs ? cs : String.valueOf(contentVal));
		result.put("content", contentStr);

		return result;
	}

	public void saveBase(long companyId, Map<String, Object> params) {
		validate(params);

		Map<String, Object> dataMap = new LinkedHashMap<>();
		dataMap.put("status", isStatusLooseTrue(params.get("status")) ? "true" : "false");
		String displayOnPc = ((String) params.get("display_on_pc")).trim();
		dataMap.put(
				"display_on_pc",
				"true".equalsIgnoreCase(displayOnPc) ? "true" : "false");
		dataMap.put("settled_type", params.get("settled_type"));
		dataMap.put("content", params.get("content"));

		String key = "settlementAgreement:" + companyId;
		String json;
		try {
			json = redisSettlementAgreementJsonMapper.writeValueAsString(dataMap);
		} catch (JsonProcessingException e) {
			throw new ResourceException("缓存写入失败");
		}
		try {
			companysRedisTemplate.opsForValue().set(key, json);
		} catch (RuntimeException e) {
			throw new ResourceException("缓存写入失败");
		}
	}

	private static void validate(Map<String, Object> params) {
		Object statusRaw = params.get("status");
		if (statusRaw == null || (statusRaw instanceof String s && s.trim().isEmpty())) {
			throw new ResourceException("是否允许加盟商入驻必填");
		}

		validateDisplayOnPc(params.get("display_on_pc"));

		Object settled = params.get("settled_type");
		if (settled == null
				|| (settled instanceof String s && s.trim().isEmpty())
				|| (settled instanceof Collection<?> c && c.isEmpty())) {
			throw new ResourceException("允许加盟商入驻类型必填");
		}

		Object content = params.get("content");
		if (content == null || (content instanceof String s && s.trim().isEmpty())) {
			throw new ResourceException("入驻协议内容必填");
		}
	}

	/**
	 * {@code display_on_pc} must be the string {@code "true"} or {@code "false"} (case-insensitive).
	 * Booleans, blank strings, and other values are invalid.
	 */
	private static void validateDisplayOnPc(Object dop) {
		if (dop == null) {
			throw new ResourceException("是否在pc端展示入口");
		}
		if (dop instanceof Boolean) {
			throw new ResourceException("是否在pc端展示入口");
		}
		if (dop instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException("是否在pc端展示入口");
			}
			if ("true".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t)) {
				return;
			}
		}
		throw new ResourceException("是否在pc端展示入口");
	}

	private static boolean isStatusLooseTrue(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		String t = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
		if (t.isEmpty() || "0".equals(t) || "false".equals(t)) {
			return false;
		}
		if ("1".equals(t) || "true".equals(t)) {
			return true;
		}
		try {
			return Long.parseLong(t) == 1L;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
