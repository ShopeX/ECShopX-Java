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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.digios.config.DigiOsProperties;
import cn.shopex.ecshopx.digios.config.DigiOsResolvedConfig;
import cn.shopex.ecshopx.digios.domain.DigiOsCompanyConfig;
import cn.shopex.ecshopx.digios.mapper.DigiOsCompanyConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigiOsConfigService {

	private final DigiOsCompanyConfigMapper configMapper;
	private final DigiOsProperties properties;

	public DigiOsConfigService(DigiOsCompanyConfigMapper configMapper, DigiOsProperties properties) {
		this.configMapper = configMapper;
		this.properties = properties;
	}

	public DigiOsResolvedConfig resolve(long companyId) {
		String clientId = trim(properties.getClientId());
		String appSecret = trim(properties.getAppSecret());
		boolean enabled = properties.isEnabled();
		String baseUrl = rtrim(properties.getBaseUrl());
		Map<String, String> methods = new LinkedHashMap<>(properties.getMethods());

		if (companyId > 0) {
			DigiOsCompanyConfig row = configMapper.selectOne(
					new LambdaQueryWrapper<DigiOsCompanyConfig>()
							.eq(DigiOsCompanyConfig::getCompanyId, companyId)
							.last("LIMIT 1"));
			if (row != null) {
				if (row.getIsEnabled() != null && row.getIsEnabled() == 1) {
					enabled = true;
				}
				if (StringUtils.hasText(row.getBaseUrl())) {
					baseUrl = rtrim(row.getBaseUrl());
				}
				String rowKey = trim(row.getAppKey());
				if (StringUtils.hasText(rowKey)) {
					clientId = rowKey;
				}
				if (StringUtils.hasText(row.getAppSecret())) {
					appSecret = trim(row.getAppSecret());
				}
			}
		}

		return new DigiOsResolvedConfig(
				enabled,
				baseUrl,
				clientId,
				appSecret,
				properties.getTimeoutSeconds(),
				properties.getConnectTimeoutSeconds(),
				properties.getTokenPath(),
				properties.getTokenExpireSkewSeconds(),
				properties.getCoCode(),
				companyId,
				methods);
	}

	public String resolvePath(String method, long companyId) {
		DigiOsResolvedConfig cfg = resolve(companyId);
		String path = cfg.methods().get(method);
		if (StringUtils.hasText(path)) {
			return path;
		}
		return "/" + method.replace('.', '/');
	}

	private static String rtrim(String url) {
		if (url == null) {
			return "";
		}
		String s = url.trim();
		while (s.endsWith("/")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
