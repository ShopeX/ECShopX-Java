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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis key {@code TradeRateSetting:{companyId}}: JSON object typically {@code {"rate_status":true}}.
 * Whole key replaced on write; no TTL.
 */
@Service
public class TradeRateSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(TradeRateSettingRedisService.class);

	private static final String KEY_PREFIX = "TradeRateSetting:";
	private static final String FIELD_RATE_STATUS = "rate_status";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public TradeRateSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	/**
	 * Persist path when the merged request map {@linkplain Map#containsKey(Object) contains}
	 * {@link #FIELD_RATE_STATUS} and the mapped value is not {@code null}.
	 */
	private static boolean isWriteBranch(Map<String, Object> merged) {
		return merged != null
				&& merged.containsKey(FIELD_RATE_STATUS)
				&& merged.get(FIELD_RATE_STATUS) != null;
	}

	/**
	 * Coerces client-supplied {@code rate_status} values to a boolean flag: {@code true} for
	 * {@link Boolean#TRUE}, numeric zero, the literal string {@code "true"}, or a numeric string
	 * that parses to zero; {@code false} for {@code null}, empty string, other strings, non-zero
	 * numbers, and structured types (map, collection, array).
	 */
	private static boolean isLooselyEqualToLowercaseTrueString(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		if (raw instanceof Number n) {
			return numberLooselyEqualsTrueLiteral(n);
		}
		if (raw instanceof CharSequence cs) {
			return charSequenceLooselyEqualsTrueLiteral(cs.toString());
		}
		if (raw instanceof Map<?, ?> || raw instanceof Collection<?> || raw.getClass().isArray()) {
			return false;
		}
		return false;
	}

	private static boolean numberLooselyEqualsTrueLiteral(Number n) {
		if (n instanceof Double d) {
			return !Double.isNaN(d) && d == 0.0d;
		}
		if (n instanceof Float f) {
			float fv = f.floatValue();
			return !Float.isNaN(fv) && fv == 0.0f;
		}
		if (n instanceof BigDecimal bd) {
			return bd.compareTo(BigDecimal.ZERO) == 0;
		}
		if (n instanceof BigInteger bi) {
			return bi.signum() == 0;
		}
		return BigDecimal.valueOf(n.longValue()).compareTo(BigDecimal.ZERO) == 0;
	}

	private static boolean charSequenceLooselyEqualsTrueLiteral(String s) {
		if ("true".equals(s)) {
			return true;
		}
		if (s.isEmpty()) {
			return false;
		}
		try {
			BigDecimal val = new BigDecimal(s.trim());
			return val.compareTo(BigDecimal.ZERO) == 0;
		} catch (NumberFormatException | ArithmeticException e) {
			return false;
		}
	}

	private static Map<String, Object> readMissPayload() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
		out.put(FIELD_RATE_STATUS, Boolean.FALSE);
		return out;
	}

	private static Map<String, Object> defaultRateStatusOnlyMap() {
		return Map.of(FIELD_RATE_STATUS, Boolean.FALSE);
	}

	private static String rawPreview(String raw, int maxLen) {
		if (raw == null) {
			return "";
		}
		if (raw.length() <= maxLen) {
			return raw;
		}
		return raw.substring(0, maxLen) + "...";
	}

	/**
	 * H5 只读：读取 TradeRateSetting Redis JSON，按「默认单键 / 成功全量 Map」策略返回，不写入 Redis。
	 *
	 * @param companyId 店铺 ID
	 * @return 永不为 null。miss/空串/解析失败/根为数组 → 仅含 {@code rate_status=false}；解析成功且根为对象 → 完整 {@link Map}（含扩展字段）
	 */
	public Map<String, Object> getRateSettingStatus(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return defaultRateStatusOnlyMap();
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root.isArray()) {
				log.warn(
						"Trade rate setting JSON root is array for companyId={}, raw={}",
						companyId,
						rawPreview(raw, 256));
				return defaultRateStatusOnlyMap();
			}
			if (root.isObject()) {
				return objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
			}
			log.warn(
					"Trade rate setting JSON root is not object for companyId={}, raw={}",
					companyId,
					rawPreview(raw, 256));
			return defaultRateStatusOnlyMap();
		} catch (JsonProcessingException e) {
			log.warn(
					"Failed to parse trade rate setting JSON for companyId={}, raw={}",
					companyId,
					rawPreview(raw, 256),
					e);
			return defaultRateStatusOnlyMap();
		}
	}

	public Map<String, Object> rateSetting(long companyId, Map<String, Object> merged) {
		if (isWriteBranch(merged)) {
			Object raw = merged.get(FIELD_RATE_STATUS);
			boolean flag = isLooselyEqualToLowercaseTrueString(raw);
			LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
			data.put(FIELD_RATE_STATUS, Boolean.valueOf(flag));
			try {
				String json = objectMapper.writeValueAsString(data);
				companysRedisTemplate.opsForValue().set(key(companyId), json);
			} catch (JsonProcessingException e) {
				log.error("Failed to serialize trade rate setting for companyId={}", companyId, e);
			} catch (DataAccessException e) {
				log.error("Failed to write trade rate setting to Redis for companyId={}", companyId, e);
			}
			return data;
		}

		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return readMissPayload();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return new LinkedHashMap<>(parsed);
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse trade rate setting JSON for companyId={}", companyId, e);
			return null;
		}
	}
}
