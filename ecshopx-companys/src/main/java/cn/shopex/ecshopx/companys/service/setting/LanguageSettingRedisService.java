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

import cn.shopex.ecshopx.common.companys.language.CompanyLanguageResolver;
import cn.shopex.ecshopx.common.companys.language.LanguageCanonicalCodes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.language.LanguageSettingErrorCodes;
import cn.shopex.ecshopx.companys.domain.language.LanguageSettingErrorMessages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LanguageSettingRedisService implements CompanyLanguageResolver {

	private static final Logger log = LoggerFactory.getLogger(LanguageSettingRedisService.class);

	private static final String KEY_PREFIX = "languageSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;
	private final MessageSource messageSource;

	public LanguageSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			LangueProperties langueProperties,
			MessageSource messageSource) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
		this.messageSource = messageSource;
	}

	private String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	public Map<String, Object> getAdminSetting(long companyId) {
		ResolvedSetting setting = loadSetting(companyId);
		return buildAdminResponse(setting);
	}

	public Map<String, Object> getSwitchSetting(long companyId) {
		ResolvedSetting setting = loadSetting(companyId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("defaultLanguage", effectiveDefaultLanguage(setting));
		data.put("enabledLanguages", buildEnabledLanguageRows(setting));
		if (setting.updatedAt() != null) {
			data.put("updatedAt", setting.updatedAt());
		}
		return data;
	}

	public Map<String, Object> saveSetting(long companyId, Map<String, Object> body) {
		if (body == null || body.isEmpty()) {
			throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
		}
		ResolvedSetting current = loadSetting(companyId);
		int expectedVersion = parseVersion(body.get("version"));
		if (current.persisted()) {
			if (!body.containsKey("version") || expectedVersion != current.version()) {
				throw validationError(LanguageSettingErrorCodes.VERSION_CONFLICT);
			}
		} else if (body.containsKey("version") && expectedVersion > 0 && expectedVersion != current.version()) {
			throw validationError(LanguageSettingErrorCodes.VERSION_CONFLICT);
		}

		String defaultLanguage = stringValue(body.get("defaultLanguage"));
		if (!StringUtils.hasText(defaultLanguage)) {
			throw validationError(LanguageSettingErrorCodes.DEFAULT_REQUIRED);
		}
		defaultLanguage = LanguageCanonicalCodes.normalize(defaultLanguage.trim());
		if (!LanguageCanonicalCodes.isFixedCanonical(defaultLanguage)) {
			throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
		}

		Map<String, Boolean> enabledByCode = parseAndValidateLanguages(body.get("languages"));
		long enabledCount = enabledByCode.values().stream().filter(Boolean::booleanValue).count();
		if (enabledCount == 0L) {
			throw validationError(LanguageSettingErrorCodes.ALL_DISABLED);
		}
		if (!Boolean.TRUE.equals(enabledByCode.get(defaultLanguage))) {
			throw validationError(LanguageSettingErrorCodes.DEFAULT_NOT_ENABLED);
		}

		for (String code : LanguageCanonicalCodes.FIXED_ORDER) {
			if (Boolean.TRUE.equals(enabledByCode.get(code)) && !isDeploymentSupported(code)) {
				throw validationError(LanguageSettingErrorCodes.NOT_SUPPORTED_BY_DEPLOYMENT);
			}
		}

		int newVersion = current.persisted() ? current.version() + 1 : 1;
		long updatedAt = System.currentTimeMillis();
		List<Map<String, Object>> languageRows = buildStoredLanguageRows(enabledByCode);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("version", newVersion);
		payload.put("defaultLanguage", defaultLanguage);
		payload.put("languages", languageRows);
		payload.put("updatedAt", updatedAt);
		writePayload(companyId, payload);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("result", Boolean.TRUE);
		data.put("version", newVersion);
		data.put("defaultLanguage", defaultLanguage);
		data.put("enabledCount", enabledCount);
		data.put("languages", buildAdminLanguageRows(enabledByCode, defaultLanguage));
		data.put("message", LanguageSettingErrorMessages.savedMessage(messageSource));
		return data;
	}

	@Override
	public List<String> listEnabledCodes(long companyId) {
		ResolvedSetting setting = loadSetting(companyId);
		List<String> out = new ArrayList<>();
		for (String code : LanguageCanonicalCodes.FIXED_ORDER) {
			if (isEnabledForCompany(setting, code) && isDeploymentSupported(code)) {
				out.add(code);
			}
		}
		return out;
	}

	@Override
	public String getDefaultLanguage(long companyId) {
		return effectiveDefaultLanguage(loadSetting(companyId));
	}

	@Override
	public String resolveEffectiveTag(long companyId, String rawTag) {
		ResolvedSetting setting = loadSetting(companyId);
		String normalized = LanguageCanonicalCodes.normalize(rawTag);
		if (!StringUtils.hasText(normalized)) {
			normalized = langueProperties.resolveToSupportedTag(rawTag);
		}
		String deploymentTag = langueProperties.resolveToSupportedTag(normalized);
		if (isEnabledForCompany(setting, deploymentTag) && isDeploymentSupported(deploymentTag)) {
			return deploymentTag;
		}
		return effectiveDefaultLanguage(setting);
	}

	private Map<String, Object> buildAdminResponse(ResolvedSetting setting) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("version", setting.version());
		data.put("defaultLanguage", setting.defaultLanguage());
		data.put("enabledCount", countEnabled(setting));
		data.put("languages", buildAdminLanguageRows(setting.enabledByCode(), setting.defaultLanguage()));
		boolean configValid = isEnabledForCompany(setting, setting.defaultLanguage());
		data.put("configValid", configValid);
		if (!configValid) {
			data.put(
					"configWarning",
					LanguageSettingErrorMessages.configWarningMessage(messageSource));
		} else {
			data.put("configWarning", null);
		}
		if (setting.updatedAt() != null) {
			data.put("updatedAt", setting.updatedAt());
		}
		return data;
	}

	private List<Map<String, Object>> buildAdminLanguageRows(
			Map<String, Boolean> enabledByCode, String defaultLanguage) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (String code : LanguageCanonicalCodes.FIXED_ORDER) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("code", code);
			row.put("displayName", LanguageCanonicalCodes.displayName(code));
			row.put("enabled", Boolean.TRUE.equals(enabledByCode.get(code)));
			row.put("sort", LanguageCanonicalCodes.sortOf(code));
			rows.add(row);
		}
		return rows;
	}

	private List<Map<String, Object>> buildEnabledLanguageRows(ResolvedSetting setting) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (String code : LanguageCanonicalCodes.FIXED_ORDER) {
			if (!isEnabledForCompany(setting, code) || !isDeploymentSupported(code)) {
				continue;
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("code", code);
			row.put("name", LanguageCanonicalCodes.displayName(code));
			row.put("sort", LanguageCanonicalCodes.sortOf(code));
			rows.add(row);
		}
		return rows;
	}

	private static List<Map<String, Object>> buildStoredLanguageRows(Map<String, Boolean> enabledByCode) {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (String code : LanguageCanonicalCodes.FIXED_ORDER) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("code", code);
			row.put("enabled", Boolean.TRUE.equals(enabledByCode.get(code)));
			row.put("sort", LanguageCanonicalCodes.sortOf(code));
			rows.add(row);
		}
		return rows;
	}

	private Map<String, Boolean> parseAndValidateLanguages(Object rawLanguages) {
		if (!(rawLanguages instanceof List<?> list)) {
			throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
		}
		Map<String, Boolean> enabledByCode = new LinkedHashMap<>();
		Set<String> seen = new HashSet<>();
		for (Object item : list) {
			if (!(item instanceof Map<?, ?> row)) {
				throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
			}
			String codeRaw = stringValue(row.get("code"));
			if (!StringUtils.hasText(codeRaw)) {
				throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
			}
			String code = LanguageCanonicalCodes.normalize(codeRaw.trim());
			if (!LanguageCanonicalCodes.isFixedCanonical(code)) {
				throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
			}
			if (!seen.add(code)) {
				throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
			}
			enabledByCode.put(code, parseEnabled(row.get("enabled")));
		}
		if (seen.size() != LanguageCanonicalCodes.FIXED_ORDER.size()) {
			throw validationError(LanguageSettingErrorCodes.INVALID_CODE);
		}
		for (String fixed : LanguageCanonicalCodes.FIXED_ORDER) {
			enabledByCode.putIfAbsent(fixed, Boolean.FALSE);
		}
		return enabledByCode;
	}

	private ResolvedSetting loadSetting(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (Exception e) {
			log.warn("LanguageSetting Redis read failed for companyId={}", companyId, e);
			return defaultSetting(false);
		}
		if (raw == null || raw.isBlank()) {
			return defaultSetting(false);
		}
		try {
			Map<String, Object> root =
					objectMapper.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
			if (root == null || root.isEmpty()) {
				log.warn("LanguageSetting empty JSON for companyId={}", companyId);
				return defaultSetting(false);
			}
			return parseStoredSetting(root, true);
		} catch (Exception e) {
			log.warn("LanguageSetting JSON parse failed for companyId={}", companyId, e);
			return defaultSetting(false);
		}
	}

	private ResolvedSetting parseStoredSetting(Map<String, Object> root, boolean persisted) {
		int version = parseVersion(root.get("version"));
		String defaultLanguage = stringValue(root.get("defaultLanguage"));
		defaultLanguage = LanguageCanonicalCodes.normalize(defaultLanguage);
		if (!LanguageCanonicalCodes.isFixedCanonical(defaultLanguage)) {
			defaultLanguage = "zh-CN";
		}
		Map<String, Boolean> enabledByCode = defaultEnabledMap();
		Object languagesObj = root.get("languages");
		if (languagesObj instanceof List<?> list) {
			for (Object item : list) {
				if (!(item instanceof Map<?, ?> row)) {
					continue;
				}
				String code = LanguageCanonicalCodes.normalize(stringValue(row.get("code")));
				if (LanguageCanonicalCodes.isFixedCanonical(code)) {
					enabledByCode.put(code, parseEnabled(row.get("enabled")));
				}
			}
		}
		Long updatedAt = parseLong(root.get("updatedAt"));
		return new ResolvedSetting(version, defaultLanguage, enabledByCode, updatedAt, persisted);
	}

	private static ResolvedSetting defaultSetting(boolean persisted) {
		Map<String, Boolean> enabled = defaultEnabledMap();
		return new ResolvedSetting(0, "zh-CN", enabled, null, persisted);
	}

	private static Map<String, Boolean> defaultEnabledMap() {
		Map<String, Boolean> enabled = new LinkedHashMap<>();
		for (String code : LanguageCanonicalCodes.FIXED_ORDER) {
			enabled.put(code, "zh-CN".equals(code));
		}
		return enabled;
	}

	private void writePayload(long companyId, Map<String, Object> payload) {
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new ResourceException("companys 语言设置 JSON 序列化失败");
		}
		try {
			companysRedisTemplate.opsForValue().set(key(companyId), json);
		} catch (DataAccessException e) {
			throw new ResourceException("companys 语言设置 Redis 写入失败");
		}
	}

	private String effectiveDefaultLanguage(ResolvedSetting setting) {
		String def = setting.defaultLanguage();
		if (isEnabledForCompany(setting, def) && isDeploymentSupported(def)) {
			return def;
		}
		for (String code : LanguageCanonicalCodes.FIXED_ORDER) {
			if (isEnabledForCompany(setting, code) && isDeploymentSupported(code)) {
				return code;
			}
		}
		return langueProperties.resolveToSupportedTag(null);
	}

	private static boolean isEnabledForCompany(ResolvedSetting setting, String code) {
		return Boolean.TRUE.equals(setting.enabledByCode().get(code));
	}

	private static int countEnabled(ResolvedSetting setting) {
		int count = 0;
		for (Boolean enabled : setting.enabledByCode().values()) {
			if (Boolean.TRUE.equals(enabled)) {
				count++;
			}
		}
		return count;
	}

	private boolean isDeploymentSupported(String code) {
		List<String> list = langueProperties.getList();
		if (list == null || list.isEmpty()) {
			return true;
		}
		for (String lang : list) {
			if (lang != null && lang.equalsIgnoreCase(code)) {
				return true;
			}
		}
		return false;
	}

	private BadRequestException validationError(String errorCode) {
		log.warn("LanguageSetting validation failed: error_code={}", errorCode);
		return new BadRequestException(LanguageSettingErrorMessages.message(messageSource, errorCode));
	}

	private static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value).trim();
	}

	private static boolean parseEnabled(Object value) {
		if (value instanceof Boolean b) {
			return b;
		}
		if (value == null) {
			return false;
		}
		String s = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
		return "true".equals(s) || "1".equals(s);
	}

	private static int parseVersion(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static Long parseLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private record ResolvedSetting(
			int version,
			String defaultLanguage,
			Map<String, Boolean> enabledByCode,
			Long updatedAt,
			boolean persisted) {}
}
