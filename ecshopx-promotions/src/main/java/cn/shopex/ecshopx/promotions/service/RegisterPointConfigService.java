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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegisterPointConfigService {

	private static final Logger log = LoggerFactory.getLogger(RegisterPointConfigService.class);

	private static final Pattern INTEGER_DECIMAL_STRING =
			Pattern.compile("^(-?)(0|[1-9]\\d*)$");

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public RegisterPointConfigService(
			StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getRegisterPointConfig(long companyId, String type) {
		String typePart = type != null ? type : "";
		String key = "registerPoint:" + companyId + ":" + typePart;

		String raw;
		try {
			raw = stringRedisTemplate.opsForValue().get(key);
		} catch (DataAccessException e) {
			log.warn("register point config redis get failed, key={}", key, e);
			throw new ResourceException("注册积分配置读取失败");
		}

		if (raw == null || raw.isEmpty()) {
			LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();
			defaults.put("is_open", Boolean.FALSE);
			defaults.put("point", Integer.valueOf(0));
			defaults.put("type", "point");
			return defaults;
		}

		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			log.warn("register point config json parse failed, key={}", key, e);
			throw new ResourceException("注册积分配置格式错误");
		}
	}

	public void saveRegisterPointConfig(long companyId, Map<String, Object> merged) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		putIfKeyPresent(payload, merged, "is_open");
		putIfKeyPresent(payload, merged, "type");
		putIfKeyPresent(payload, merged, "point");
		putIfKeyPresent(payload, merged, "rebate");

		Object pointNorm = normalizeIntegerLike(merged.get("point"), "注册赠送积分必须为整数");
		Object rebateNorm = normalizeIntegerLike(merged.get("rebate"), "注册返上级积分必须为整数");
		payload.put("point", pointNorm);
		payload.put("rebate", rebateNorm);

		String typePart =
				merged.containsKey("type") && merged.get("type") != null
						? merged.get("type").toString()
						: "";
		String key = "registerPoint:" + companyId + ":" + typePart;

		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			log.warn("register point config json serialize failed", e);
			throw new ResourceException("注册积分配置保存失败");
		}

		try {
			stringRedisTemplate.opsForValue().set(key, json);
		} catch (DataAccessException e) {
			log.warn("register point config redis set failed, key={}", key, e);
			throw new ResourceException("注册积分配置保存失败");
		}
	}

	private static void putIfKeyPresent(LinkedHashMap<String, Object> payload, Map<String, Object> merged, String k) {
		if (merged.containsKey(k)) {
			payload.put(k, merged.get(k));
		}
	}

	private static Object normalizeIntegerLike(Object raw, String errorMessage) {
		if (raw == null) {
			throw new BadRequestException(errorMessage);
		}
		if (raw instanceof Boolean) {
			throw new BadRequestException(errorMessage);
		}
		if (raw instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty()) {
				throw new BadRequestException(errorMessage);
			}
			if (!INTEGER_DECIMAL_STRING.matcher(s).matches()) {
				throw new BadRequestException(errorMessage);
			}
			return s;
		}
		if (raw instanceof Number n) {
			try {
				BigDecimal bd = new BigDecimal(n.toString());
				bd = bd.stripTrailingZeros();
				if (bd.scale() > 0) {
					throw new BadRequestException(errorMessage);
				}
				return bd.longValueExact();
			} catch (ArithmeticException | NumberFormatException ex) {
				throw new BadRequestException(errorMessage);
			}
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException(errorMessage);
		}
		if (!INTEGER_DECIMAL_STRING.matcher(s).matches()) {
			throw new BadRequestException(errorMessage);
		}
		return s;
	}
}
