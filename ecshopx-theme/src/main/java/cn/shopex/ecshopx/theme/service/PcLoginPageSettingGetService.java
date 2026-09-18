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

package cn.shopex.ecshopx.theme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PcLoginPageSettingGetService {

	private static final String PC_LOGIN_PAGE_SETTING_KEY_PREFIX = "pc_login_page:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public PcLoginPageSettingGetService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String redisKey(long companyId) {
		return PC_LOGIN_PAGE_SETTING_KEY_PREFIX + companyId;
	}

	static Map<String, Object> defaultLoginPageSettingMap() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("logo", "");
		m.put("logo_light", "");
		m.put("logo_dark", "");
		m.put("background", "");
		return m;
	}

	static Map<String, Object> normalizeLoginPageSetting(Map<String, Object> raw) {
		if (raw == null || raw.isEmpty()) {
			return defaultLoginPageSettingMap();
		}
		String logoLight = stringOrEmpty(raw.get("logo_light"));
		if (!StringUtils.hasText(logoLight)) {
			logoLight = stringOrEmpty(raw.get("logo"));
		}
		String logoDark = stringOrEmpty(raw.get("logo_dark"));
		if (!StringUtils.hasText(logoDark)) {
			logoDark = stringOrEmpty(raw.get("logo"));
		}
		String logo = stringOrEmpty(raw.get("logo"));
		if (!StringUtils.hasText(logo)) {
			logo = logoLight;
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("logo", logo);
		m.put("logo_light", logoLight);
		m.put("logo_dark", logoDark);
		m.put("background", stringOrEmpty(raw.get("background")));
		return m;
	}

	private static String stringOrEmpty(Object v) {
		if (v == null) {
			return "";
		}
		String s = v.toString();
		return s.isEmpty() ? "" : s;
	}

	private static boolean isMissingOrInactiveString(String raw) {
		if (raw == null) {
			return true;
		}
		if (raw.isEmpty()) {
			return true;
		}
		return "0".equals(raw);
	}

	public Map<String, Object> getLoginPageSetting(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get(redisKey(companyId));
		if (isMissingOrInactiveString(raw)) {
			return defaultLoginPageSettingMap();
		}
		try {
			Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return normalizeLoginPageSetting(parsed);
		} catch (JsonProcessingException ex) {
			return defaultLoginPageSettingMap();
		}
	}
}
