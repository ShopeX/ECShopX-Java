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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserDailyRecordPhysicalSettingService {

	private final FormTemplateMapper formTemplateMapper;
	private final FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public UserDailyRecordPhysicalSettingService(
			FormTemplateMapper formTemplateMapper,
			FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.formTemplateMapper = formTemplateMapper;
		this.formTemplateOutsideMultiLangReadService = formTemplateOutsideMultiLangReadService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> settingPhysical(
			long companyId,
			String tempIdRaw,
			String statusRaw,
			String requestLangTag,
			Locale locale) {
		if (isTruthyStatusString(statusRaw) && isEmptyQueryParam(tempIdRaw)) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.user_daily_record.please_select_form_template", null, locale));
		}
		long tempId = parseTempIdAsLong(tempIdRaw);
		LinkedHashMap<String, Object> response = new LinkedHashMap<>();
		response.put("temp_id", tempId);
		long statusLong;
		if (isEmptyQueryParam(statusRaw)) {
			statusLong = 0L;
		} else {
			try {
				statusLong = Long.parseLong(statusRaw.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("status 格式不正确");
			}
		}
		response.put("status", statusLong);

		String redisKey = "settingPhysical:" + companyId;
		Map<String, Object> redisPayload = new LinkedHashMap<>();
		redisPayload.put("temp_id", response.get("temp_id"));
		redisPayload.put("status", response.get("status"));
		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(redisPayload));
		} catch (JsonProcessingException ex) {
			throw new BadRequestException("体测配置数据无法序列化，请检查参数后重试");
		}

		if (isTruthyStatusString(statusRaw) && tempId != 0L) {
			FormTemplate row = formTemplateMapper.selectOne(
					new LambdaQueryWrapper<FormTemplate>()
							.eq(FormTemplate::getCompanyId, companyId)
							.eq(FormTemplate::getId, tempId));
			if (row == null) {
				throw new ResourceException(
						messageSource.getMessage("selfservice.user_daily_record.form_template_not_found", null, locale));
			}
			String baseName = row.getTemName() == null ? "" : row.getTemName();
			String temName = formTemplateOutsideMultiLangReadService
					.loadTemNameOverride(companyId, tempId, requestLangTag)
					.orElse(baseName);
			response.put("temp_name", temName);
		}
		return response;
	}

	public Map<String, Object> getSettingPhysical(long companyId, String requestLangTag) {
		String redisKey = "settingPhysical:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(redisKey);
		if (raw == null || raw.isBlank()) {
			return defaultPhysicalSettingMap();
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException ex) {
			return defaultPhysicalSettingMap();
		}
		if (root == null || !root.isObject()) {
			return defaultPhysicalSettingMap();
		}
		LinkedHashMap<String, Object> result =
				objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
		if (result.get("status") == null) {
			result.put("status", 0L);
		}
		if (result.get("temp_id") == null) {
			result.put("temp_id", 0L);
		}
		Object v = result.get("temp_id");
		if (!hasMeaningfulTempId(v)) {
			return result;
		}
		String redisTempIdString;
		if (v instanceof String s) {
			redisTempIdString = s.trim();
		} else if (v instanceof Number n) {
			redisTempIdString = Long.toString(n.longValue());
		} else {
			result.put("temp_name", "");
			return result;
		}
		OptionalLong tempIdOpt = parsePositiveLongFromRedisString(redisTempIdString);
		if (tempIdOpt.isEmpty()) {
			result.put("temp_name", "");
			return result;
		}
		long tempId = tempIdOpt.getAsLong();
		FormTemplate row = formTemplateMapper.selectOne(
				new LambdaQueryWrapper<FormTemplate>()
						.eq(FormTemplate::getCompanyId, companyId)
						.eq(FormTemplate::getId, tempId));
		if (row == null) {
			result.put("temp_name", "");
			return result;
		}
		String baseName = row.getTemName() == null ? "" : row.getTemName();
		String temName = formTemplateOutsideMultiLangReadService
				.loadTemNameOverride(companyId, tempId, requestLangTag)
				.orElse(baseName);
		result.put("temp_name", temName);
		return result;
	}

	private static LinkedHashMap<String, Object> defaultPhysicalSettingMap() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("status", 0L);
		m.put("temp_id", 0L);
		return m;
	}

	private static boolean hasMeaningfulTempId(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return true;
	}

	/**
	 * Redis / JSON 中 temp_id 的字符串形态解析为严格正数模板 id。
	 * 入参为 null、trim 后空白、非 Long 可解析数字、或解析值 {@code <= 0} 时返回 empty。
	 */
	private static OptionalLong parsePositiveLongFromRedisString(String raw) {
		if (raw == null) {
			return OptionalLong.empty();
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return OptionalLong.empty();
		}
		try {
			long parsed = Long.parseLong(t);
			if (parsed <= 0L) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(parsed);
		} catch (NumberFormatException ex) {
			return OptionalLong.empty();
		}
	}

	/**
	 * Reads the configured form template id from Redis for the physical form type only.
	 * Missing key, invalid JSON, or missing non-numeric {@code temp_id} yields {@code 0}; {@code status} is ignored.
	 */
	public long getTempIdForRead(long companyId, String formType) {
		if (formType == null || !"physical".equals(formType)) {
			return 0L;
		}
		String key = "settingPhysical:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			JsonNode tempNode = root.get("temp_id");
			if (tempNode == null || tempNode.isNull() || !tempNode.isNumber()) {
				return 0L;
			}
			return tempNode.asLong();
		} catch (JsonProcessingException ex) {
			return 0L;
		}
	}

	private static boolean isEmptyQueryParam(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static boolean isTruthyStatusString(String raw) {
		return !isEmptyQueryParam(raw);
	}

	private static long parseTempIdAsLong(String tempIdRaw) {
		if (isEmptyQueryParam(tempIdRaw)) {
			return 0L;
		}
		try {
			return Long.parseLong(tempIdRaw.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("temp_id 格式不正确");
		}
	}
}
