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

package cn.shopex.ecshopx.companys.service.privacy;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.PrivacySettingData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysPrivacySettingService {

	private static final Logger log = LoggerFactory.getLogger(CompanysPrivacySettingService.class);

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public CompanysPrivacySettingService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String redisKey(long companyId, String lang) {
		return "PrivacySetting:" + companyId + "_" + lang;
	}

	public PrivacySettingData getPrivacySetting(long companyId, String lang) {
		String key = redisKey(companyId, lang);
		String raw = companysRedisTemplate.opsForValue().get(key);
		PrivacySettingData data = new PrivacySettingData();
		if (raw == null || !StringUtils.hasText(raw)) {
			data.setPcPrivacyContent("");
			data.setH5PrivacyContent("");
			return data;
		}
		Map<String, Object> parsed;
		try {
			parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			log.warn("Failed to parse privacy setting JSON for companyId={}, lang={}", companyId, lang, e);
			data.setPcPrivacyContent("");
			data.setH5PrivacyContent("");
			return data;
		}
		if (parsed == null) {
			log.warn("Privacy setting parsed to null map for companyId={}, lang={}", companyId, lang);
			data.setPcPrivacyContent("");
			data.setH5PrivacyContent("");
			return data;
		}
		Object pc = parsed.get("pc_privacy_content");
		Object h5 = parsed.get("h5_privacy_content");
		data.setPcPrivacyContent(pc == null ? "" : String.valueOf(pc));
		data.setH5PrivacyContent(h5 == null ? "" : String.valueOf(h5));
		return data;
	}

	public PrivacySettingData setPrivacySetting(long companyId, String lang, Map<String, Object> patch) {
		PrivacySettingData data = getPrivacySetting(companyId, lang);
		if (patch != null) {
			if (patch.containsKey("pc_privacy_content") && patch.get("pc_privacy_content") != null) {
				data.setPcPrivacyContent(String.valueOf(patch.get("pc_privacy_content")));
			}
			if (patch.containsKey("h5_privacy_content") && patch.get("h5_privacy_content") != null) {
				data.setH5PrivacyContent(String.valueOf(patch.get("h5_privacy_content")));
			}
		}
		String key = redisKey(companyId, lang);
		String jsonString;
		try {
			jsonString = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new ResourceException("隐私设置缓存写入失败");
		}
		companysRedisTemplate.opsForValue().set(key, jsonString);
		return data;
	}
}
