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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class McpSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(McpSettingRedisService.class);

	private static final String KEY_PREFIX = "mcpSetting:";

	private final StringRedisTemplate companysRedisTemplate;

	private final ObjectMapper objectMapper;

	private final String publicBaseUrl;

	private final String mcpPath;

	public McpSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecx.mcp.public-base-url:}") String publicBaseUrl,
			@Value("${ecx.mcp.path:/api/mcp}") String mcpPath) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
		this.mcpPath = StringUtils.hasText(mcpPath) ? mcpPath.trim() : "/api/mcp";
	}

	public Map<String, Object> getSetting(long companyId, String requestBaseUrl) {
		Map<String, Object> stored = load(companyId);
		boolean enabled = asBoolean(stored.get("enabled"), false);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("enabled", enabled);
		data.put("mcp_url", resolveMcpUrl(requestBaseUrl));
		data.put("server_name", "ecshopx-admin");
		return data;
	}

	public Map<String, Object> saveSetting(long companyId, Map<String, Object> input, String requestBaseUrl) {
		boolean enabled = asBoolean(input != null ? input.get("enabled") : null, false);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("enabled", enabled);
		try {
			companysRedisTemplate.opsForValue().set(key(companyId), objectMapper.writeValueAsString(payload));
		}
		catch (Exception ex) {
			log.warn("save mcp setting failed companyId={}", companyId, ex);
			throw new IllegalStateException("保存 MCP 设置失败");
		}
		return getSetting(companyId, requestBaseUrl);
	}

	public boolean isEnabled(long companyId) {
		return asBoolean(load(companyId).get("enabled"), false);
	}

	private Map<String, Object> load(long companyId) {
		try {
			String raw = companysRedisTemplate.opsForValue().get(key(companyId));
			if (!StringUtils.hasText(raw)) {
				return Map.of();
			}
			return objectMapper.readValue(raw, new TypeReference<>() {
			});
		}
		catch (Exception ex) {
			log.warn("load mcp setting failed companyId={}", companyId, ex);
			return Map.of();
		}
	}

	private String resolveMcpUrl(String requestBaseUrl) {
		String base = StringUtils.hasText(publicBaseUrl) ? publicBaseUrl : nullToEmpty(requestBaseUrl);
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		String path = mcpPath.startsWith("/") ? mcpPath : "/" + mcpPath;
		return base + path;
	}

	private String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	private static boolean asBoolean(Object value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Boolean b) {
			return b;
		}
		String text = String.valueOf(value).trim();
		if ("1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)) {
			return true;
		}
		if ("0".equals(text) || "false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text)) {
			return false;
		}
		return defaultValue;
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
