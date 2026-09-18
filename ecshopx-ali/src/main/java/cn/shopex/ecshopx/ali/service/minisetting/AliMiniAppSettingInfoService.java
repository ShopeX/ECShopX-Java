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

package cn.shopex.ecshopx.ali.service.minisetting;

import cn.shopex.ecshopx.ali.domain.AliMiniAppSetting;
import cn.shopex.ecshopx.ali.mapper.AliMiniAppSettingMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
public class AliMiniAppSettingInfoService {

	private static final String CACHE_KEY_PATTERN = "CACHE:ALI:MINI:APP:%s";

	private final AliMiniAppSettingMapper mapper;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate companysRedisTemplate;

	public AliMiniAppSettingInfoService(
			AliMiniAppSettingMapper mapper,
			ObjectMapper objectMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.mapper = mapper;
		this.objectMapper = objectMapper;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public Map<String, Object> getInfoByCompanyId(long companyId) {
		String key = String.format(CACHE_KEY_PATTERN, companyId);
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (StringUtils.hasText(raw)) {
			try {
				Map<String, Object> cached = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (cached != null && !cached.isEmpty()) {
					return cached;
				}
			} catch (JsonProcessingException ignored) {
				// treat as cache miss
			}
		}

		AliMiniAppSetting row =
				mapper.selectOne(Wrappers.<AliMiniAppSetting>lambdaQuery().eq(AliMiniAppSetting::getCompanyId, companyId));

		Map<String, Object> result;
		if (row == null) {
			result = buildDefaultTemplate();
		} else {
			result = AliMiniAppSettingRowMapper.toSnakeRow(row);
		}

		try {
			String json = objectMapper.writeValueAsString(result);
			companysRedisTemplate.opsForValue().set(key, json);
		} catch (JsonProcessingException e) {
			throw new ResourceException("缓存写入失败");
		}

		return result;
	}

	private static Map<String, Object> buildDefaultTemplate() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("authorizer_appid", "");
		m.put("merchant_private_key", "");
		m.put("api_sign_method", "key");
		m.put("alipay_cert_path", "");
		m.put("alipay_root_cert_path", "");
		m.put("merchant_cert_path", "");
		m.put("alipay_public_key", "");
		m.put("notify_url", "");
		m.put("encrypt_key", "");
		return m;
	}
}
