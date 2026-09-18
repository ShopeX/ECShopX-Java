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

import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcLoginPageSettingSaveRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PcLoginPageSettingSaveService {

	private static final String PC_LOGIN_PAGE_SETTING_KEY_PREFIX = "pc_login_page:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public PcLoginPageSettingSaveService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private static String redisKey(long companyId) {
		return PC_LOGIN_PAGE_SETTING_KEY_PREFIX + companyId;
	}

	public void saveLoginPageSetting(long companyId, PcLoginPageSettingSaveRequest body) {
		String logoLight = firstNonBlank(body.getLogoLight(), body.getLogo(), "");
		String logoDark = firstNonBlank(body.getLogoDark(), body.getLogo(), "");
		String logo = firstNonBlank(body.getLogo(), logoLight, "");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("logo", logo);
		params.put("logo_light", logoLight);
		params.put("logo_dark", logoDark);
		params.put("background", body.getBackground() != null ? body.getBackground() : "");
		String json;
		try {
			json = objectMapper.writeValueAsString(params);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		companysRedisTemplate.opsForValue().set(redisKey(companyId), json);
	}

	private static String firstNonBlank(String... candidates) {
		for (String c : candidates) {
			if (StringUtils.hasText(c)) {
				return c;
			}
		}
		return "";
	}
}
