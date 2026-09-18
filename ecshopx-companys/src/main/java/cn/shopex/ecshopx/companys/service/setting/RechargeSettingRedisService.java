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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis key {@code PresaleRechargeSetting:{companyId}}: JSON with only {@code recharge_status}.
 * Whole key replaced on write; no TTL.
 */
@Service
public class RechargeSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(RechargeSettingRedisService.class);

	private static final String KEY_PREFIX = "PresaleRechargeSetting:";
	private static final String FIELD_RECHARGE_STATUS = "recharge_status";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public RechargeSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static boolean isWriteBranchForRechargeStatus(Map<String, Object> merged) {
		return merged != null
				&& merged.containsKey(FIELD_RECHARGE_STATUS)
				&& merged.get(FIELD_RECHARGE_STATUS) != null;
	}

	/**
	 * Whether {@code raw} is loosely equal to the string literal {@code "false"} for the write-path
	 * contract (boolean true, numeric zero, exact {@code "false"}, and numeric strings that evaluate
	 * to zero such as {@code "0"} / {@code "0.0"}).
	 */
	private static boolean rechargeStatusLooselyEqualsFalseString(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return false;
			}
			return d == 0.0d;
		}
		if (raw instanceof String s) {
			if ("false".equals(s)) {
				return true;
			}
			return numericStringEvaluatesToZero(s);
		}
		String asString = String.valueOf(raw);
		if ("false".equals(asString)) {
			return true;
		}
		return numericStringEvaluatesToZero(asString);
	}

	/**
	 * Decimal numeric string (optional sign, fraction, exponent) that parses to zero; leading and
	 * trailing whitespace ignored.
	 */
	private static boolean numericStringEvaluatesToZero(String s) {
		if (s == null) {
			return false;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		if (!t.matches("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?")) {
			return false;
		}
		try {
			double d = Double.parseDouble(t);
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return false;
			}
			return d == 0.0d;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean normalizeRechargeStatusFromStored(Object raw) {
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		if (raw instanceof String s) {
			return !"false".equals(s);
		}
		if (raw instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		return true;
	}

	private static Map<String, Object> defaultStatusPayload() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
		out.put(FIELD_RECHARGE_STATUS, Boolean.TRUE);
		return out;
	}

	public Map<String, Object> handle(long companyId, Map<String, Object> merged) {
		if (isWriteBranchForRechargeStatus(merged)) {
			boolean stored = !rechargeStatusLooselyEqualsFalseString(merged.get(FIELD_RECHARGE_STATUS));
			LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
			data.put(FIELD_RECHARGE_STATUS, stored);
			try {
				String json = objectMapper.writeValueAsString(data);
				companysRedisTemplate.opsForValue().set(key(companyId), json);
			} catch (JsonProcessingException e) {
				log.error("Failed to serialize recharge setting for companyId={}", companyId, e);
			} catch (DataAccessException e) {
				log.error("Failed to write recharge setting to Redis for companyId={}", companyId, e);
			}
			return data;
		}

		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw == null || raw.isBlank()) {
			return defaultStatusPayload();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			boolean status = normalizeRechargeStatusFromStored(parsed.get(FIELD_RECHARGE_STATUS));
			LinkedHashMap<String, Object> out = new LinkedHashMap<>(1);
			out.put(FIELD_RECHARGE_STATUS, status);
			return out;
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse recharge setting JSON for companyId={}", companyId, e);
			return defaultStatusPayload();
		}
	}
}
