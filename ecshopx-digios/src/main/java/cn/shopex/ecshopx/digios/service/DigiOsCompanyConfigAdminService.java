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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.digios.domain.DigiOsCompanyConfig;
import cn.shopex.ecshopx.digios.mapper.DigiOsCompanyConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Admin「POS进销存配置」读写 {@code digios_company_config}（ECX-10671 / G16～G20）。
 */
@Service
public class DigiOsCompanyConfigAdminService {

	private final DigiOsCompanyConfigMapper configMapper;

	public DigiOsCompanyConfigAdminService(DigiOsCompanyConfigMapper configMapper) {
		this.configMapper = configMapper;
	}

	public Map<String, Object> get(long companyId) {
		return toResponse(find(companyId));
	}

	public Map<String, Object> save(long companyId, Map<String, Object> body) {
		Map<String, Object> input = body == null ? Map.of() : body;
		int isEnabled = parseEnabled(input.get("is_enabled"));
		String baseUrl = normalizeBaseUrl(str(input.get("base_url")));
		String clientId = trimToEmpty(str(input.get("client_id")));
		boolean secretPresent = input.containsKey("app_secret");
		String submittedSecret = secretPresent ? str(input.get("app_secret")) : null;

		DigiOsCompanyConfig existing = find(companyId);
		String effectiveSecret;
		if (secretPresent) {
			effectiveSecret = submittedSecret == null ? "" : submittedSecret;
		} else if (existing != null && existing.getAppSecret() != null) {
			effectiveSecret = existing.getAppSecret();
		} else {
			effectiveSecret = "";
		}

		if (isEnabled == 1) {
			if (!StringUtils.hasText(baseUrl)) {
				throw new BadRequestException("进销存 API 地址必填");
			}
			if (!StringUtils.hasText(clientId)) {
				throw new BadRequestException("Client ID必填");
			}
			if (!StringUtils.hasText(effectiveSecret)) {
				throw new BadRequestException("App Secret必填");
			}
			if (!looksLikeHttpUrl(baseUrl)) {
				throw new BadRequestException("进销存 API 地址须以 http:// 或 https:// 开头");
			}
		}

		String appKeyForDb = StringUtils.hasText(clientId) ? clientId : null;
		if (StringUtils.hasText(appKeyForDb)) {
			long conflict =
					configMapper.selectCount(
							new LambdaQueryWrapper<DigiOsCompanyConfig>()
									.ne(DigiOsCompanyConfig::getCompanyId, companyId)
									.eq(DigiOsCompanyConfig::getAppKey, appKeyForDb));
			if (conflict > 0) {
				throw new BadRequestException("Client ID已存在");
			}
		}

		int now = (int) Instant.now().getEpochSecond();
		if (existing == null) {
			DigiOsCompanyConfig row = new DigiOsCompanyConfig();
			row.setCompanyId(companyId);
			row.setIsEnabled(isEnabled);
			row.setBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl : null);
			row.setAppKey(appKeyForDb);
			row.setAppSecret(effectiveSecret);
			row.setCreated(now);
			row.setUpdated(now);
			configMapper.insert(row);
			return toResponse(row);
		}

		existing.setIsEnabled(isEnabled);
		existing.setBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl : null);
		existing.setAppKey(appKeyForDb);
		existing.setAppSecret(effectiveSecret);
		existing.setUpdated(now);
		configMapper.updateById(existing);
		return toResponse(existing);
	}

	private DigiOsCompanyConfig find(long companyId) {
		return configMapper.selectOne(
				new LambdaQueryWrapper<DigiOsCompanyConfig>()
						.eq(DigiOsCompanyConfig::getCompanyId, companyId)
						.last("LIMIT 1"));
	}

	private static Map<String, Object> toResponse(DigiOsCompanyConfig row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (row == null) {
			m.put("is_enabled", 0);
			m.put("base_url", "");
			m.put("client_id", "");
			m.put("app_secret", "");
			return m;
		}
		m.put("is_enabled", row.getIsEnabled() == null ? 0 : row.getIsEnabled());
		m.put("base_url", normalizeBaseUrl(row.getBaseUrl() == null ? "" : row.getBaseUrl()));
		m.put("client_id", row.getAppKey() == null ? "" : row.getAppKey());
		m.put("app_secret", row.getAppSecret() == null ? "" : row.getAppSecret());
		return m;
	}

	private static int parseEnabled(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 0 ? 0 : 1;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return 0;
		}
		return 1;
	}

	private static String str(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	static String normalizeBaseUrl(String url) {
		if (url == null) {
			return "";
		}
		String s = url.trim();
		while (s.endsWith("/")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static boolean looksLikeHttpUrl(String url) {
		String lower = url.toLowerCase();
		return lower.startsWith("http://") || lower.startsWith("https://");
	}
}
