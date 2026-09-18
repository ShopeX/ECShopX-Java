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

package cn.shopex.ecshopx.wechat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TrustLoginWeixinTouchConfigService {

	private static final Logger log = LoggerFactory.getLogger(TrustLoginWeixinTouchConfigService.class);

	private static final String REDIS_KEY_PREFIX = "trustlogin_config_";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public TrustLoginWeixinTouchConfigService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getConfigRow(String trustloginTag, String versionTag, long companyId) {
		return getConfigRow(trustloginTag, versionTag, String.valueOf(companyId));
	}

	public Map<String, Object> getConfigRow(String trustloginTag, String versionTag, String companyIdRaw) {
		String key = REDIS_KEY_PREFIX + companyIdRaw;
		try {
			String raw = sharedStringRedisTemplate.opsForValue().get(key);
			if (!StringUtils.hasText(raw)) {
				return Collections.emptyMap();
			}
			JsonNode root = objectMapper.readTree(raw);
			JsonNode versionArr = root.path(versionTag);
			if (!versionArr.isArray()) {
				return Collections.emptyMap();
			}
			for (JsonNode el : versionArr) {
				if (el.hasNonNull("type") && trustloginTag.equals(el.get("type").asText())) {
					Map<String, Object> row = objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {});
					return row != null ? row : Collections.emptyMap();
				}
			}
			return Collections.emptyMap();
		} catch (DataAccessException e) {
			log.warn("trustlogin redis read failed: companyIdRaw={}", companyIdRaw, e);
			return Collections.emptyMap();
		} catch (Exception e) {
			log.warn("trustlogin config parse failed: companyIdRaw={}", companyIdRaw, e);
			return Collections.emptyMap();
		}
	}
}
