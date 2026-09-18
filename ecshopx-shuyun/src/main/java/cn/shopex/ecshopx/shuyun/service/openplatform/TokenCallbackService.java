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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.mapper.CompanyShuyunOpenPlatformConfigMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * C1 Token 回调：不验签。对齐 PHP {@code ShuyunOpenPlatformTokenCallbackService}。
 */
@Service
public class TokenCallbackService {

	private static final Logger log = LoggerFactory.getLogger(TokenCallbackService.class);

	private final CompanyShuyunOpenPlatformConfigMapper configMapper;
	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final ObjectMapper objectMapper;

	public TokenCallbackService(
			CompanyShuyunOpenPlatformConfigMapper configMapper,
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			ObjectMapper objectMapper) {
		this.configMapper = configMapper;
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	/** @return code/msg/data */
	public Map<String, Object> handle(String rawBody) {
		List<Map<String, Object>> items = parsePayload(rawBody);
		if (items == null) {
			return resp(400, "INVALID_BODY");
		}
		if (items.isEmpty()) {
			return resp(200, "SUCCESS");
		}

		ExtractAppIdResult appIdResult = extractUniqueAppId(items);
		if (appIdResult.error != null) {
			return resp(appIdResult.code, appIdResult.error);
		}
		if (appIdResult.appId == null) {
			return resp(200, "SUCCESS");
		}
		String appId = appIdResult.appId;

		CompanyShuyunOpenPlatformConfig credentialRow = openPlatformConfigService.findByAppId(appId);
		if (credentialRow == null) {
			return resp(403, "NO_APP_CONFIG");
		}

		for (Map<String, Object> item : items) {
			if (item == null) {
				continue;
			}
			String authValue = stringVal(item.get("authValue"));
			if (!StringUtils.hasText(authValue)) {
				continue;
			}
			String accessToken = stringVal(item.get("accessToken"));
			String isOverDue = item.containsKey("isOverDue") ? stringVal(item.get("isOverDue")) : null;

			CompanyShuyunOpenPlatformConfig target = openPlatformConfigService.findByAuthValue(authValue);
			if (target == null) {
				if (Objects.equals(credentialRow.getAuthValue(), authValue)) {
					target = credentialRow;
				} else {
					return resp(400, "UNKNOWN_AUTH_VALUE");
				}
			}
			if (!Objects.equals(target.getCompanyId(), credentialRow.getCompanyId())) {
				return resp(403, "COMPANY_MISMATCH");
			}

			target.setAccessToken(StringUtils.hasText(accessToken) ? accessToken : null);
			if (isOverDue != null) {
				target.setIsOverDue(isOverDue);
			}
			if (!StringUtils.hasText(target.getAppId())) {
				target.setAppId(appId);
			}
			saveWithRetry(target);
		}
		return resp(200, "SUCCESS");
	}

	void saveWithRetry(CompanyShuyunOpenPlatformConfig target) {
		int max = Math.max(1, properties.getTokenCallbackSaveMaxAttempts());
		long base = Math.max(5_000L, properties.getTokenCallbackSaveRetryBaseUsleep());
		long maxSleep = Math.max(50_000L, properties.getTokenCallbackSaveRetryMaxUsleep());
		int now = (int) (System.currentTimeMillis() / 1000L);
		target.setUpdated(now);
		RuntimeException last = null;
		for (int attempt = 1; attempt <= max; attempt++) {
			try {
				configMapper.updateById(target);
				return;
			} catch (CannotAcquireLockException | DeadlockLoserDataAccessException e) {
				last = e;
				if (attempt >= max) {
					break;
				}
				long sleepUs = Math.min(maxSleep, base * (1L << (attempt - 1)));
				try {
					Thread.sleep(Math.max(1L, sleepUs / 1000L));
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
		log.warn("token callback save retry exhausted companyId={}", target.getCompanyId(), last);
		if (last != null) {
			throw last;
		}
	}

	private List<Map<String, Object>> parsePayload(String rawBody) {
		if (rawBody == null || rawBody.isBlank()) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(rawBody);
			if (root == null || root.isNull()) {
				return null;
			}
			if (root.isArray()) {
				if (root.isEmpty()) {
					return List.of();
				}
				return objectMapper.convertValue(root, new TypeReference<List<Map<String, Object>>>() {});
			}
			if (root.isObject() && root.has("accessToken") && root.has("authValue")) {
				Map<String, Object> one = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
				List<Map<String, Object>> list = new ArrayList<>(1);
				list.add(one);
				return list;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private ExtractAppIdResult extractUniqueAppId(List<Map<String, Object>> items) {
		Set<String> seen = new LinkedHashSet<>();
		boolean hasPayload = false;
		for (Map<String, Object> item : items) {
			if (item == null) {
				continue;
			}
			String authValue = stringVal(item.get("authValue"));
			if (!StringUtils.hasText(authValue)) {
				continue;
			}
			hasPayload = true;
			String appId = stringVal(item.get("appId"));
			if (!StringUtils.hasText(appId)) {
				return ExtractAppIdResult.error(400, "MISSING_APP_ID");
			}
			seen.add(appId);
		}
		if (!hasPayload) {
			return ExtractAppIdResult.ok(null);
		}
		if (seen.size() > 1) {
			return ExtractAppIdResult.error(400, "INCONSISTENT_APP_ID");
		}
		return ExtractAppIdResult.ok(seen.iterator().next());
	}

	private static Map<String, Object> resp(int code, String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("code", code);
		m.put("msg", msg);
		m.put("data", "");
		return m;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static final class ExtractAppIdResult {
		final String error;
		final String appId;
		final int code;

		private ExtractAppIdResult(String error, String appId, int code) {
			this.error = error;
			this.appId = appId;
			this.code = code;
		}

		static ExtractAppIdResult ok(String appId) {
			return new ExtractAppIdResult(null, appId, 200);
		}

		static ExtractAppIdResult error(int code, String error) {
			return new ExtractAppIdResult(error, null, code);
		}
	}
}
