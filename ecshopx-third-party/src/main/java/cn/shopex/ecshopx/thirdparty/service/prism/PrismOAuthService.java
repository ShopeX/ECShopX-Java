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

package cn.shopex.ecshopx.thirdparty.service.prism;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PrismOAuthService {

	private static final int PASSWORD_GRANT_MAX_TRIES = 3;

	private final PrismCoreHttpClient prismCoreHttpClient;
	private final ObjectMapper objectMapper;

	public PrismOAuthService(PrismCoreHttpClient prismCoreHttpClient, ObjectMapper objectMapper) {
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * Prism password mode ({@code grant_type=passwordv2} then {@code authorization_code}), aligned with PHP
	 * {@code PrismEgo::getPrismAuth}.
	 */
	public PrismOAuthTokenResult exchangePasswordGrant(String username, String password) {
		if (!prismCoreHttpClient.isConfigured()) {
			throw new ForbiddenException("用户名或者密码不正确");
		}
		String code = null;
		for (int attempt = 0; attempt < PASSWORD_GRANT_MAX_TRIES; attempt++) {
			LinkedHashMap<String, Object> form = new LinkedHashMap<>();
			form.put("username", username);
			form.put("password", password);
			form.put("grant_type", "passwordv2");
			String raw = prismCoreHttpClient.postSignedForm("/oauth/token", form);
			JsonNode root = readTreeOrNull(raw);
			if (root != null && !hasPrismError(root)) {
				String c = text(root, "code");
				if (StringUtils.hasText(c)) {
					code = c;
					break;
				}
			}
			if (attempt == PASSWORD_GRANT_MAX_TRIES - 1) {
				throw new ForbiddenException("用户名或者密码不正确");
			}
		}
		return exchangeAuthorizationCode(code);
	}

	/**
	 * Exchanges an authorization code for tokens and account payload (Prism {@code /oauth/token}).
	 */
	public PrismOAuthTokenResult exchangeAuthorizationCode(String code) {
		if (code == null || code.isBlank()) {
			throw new ResourceException("登录账号异常");
		}
		if (!prismCoreHttpClient.isConfigured()) {
			throw new ResourceException("登录账号异常");
		}
		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("code", code);
		form.put("grant_type", "authorization_code");
		String raw = prismCoreHttpClient.postSignedForm("/oauth/token", form);
		if (raw == null || raw.isBlank()) {
			throw new ResourceException("登录账号异常");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new ResourceException("登录账号异常");
		}
		return parseTokenResponse(root);
	}

	private PrismOAuthTokenResult parseTokenResponse(JsonNode root) {
		String accessToken = text(root, "access_token");
		if (!StringUtils.hasText(accessToken)) {
			throw new ResourceException("登录账号异常");
		}
		JsonNode dataNode = root.get("data");
		if (dataNode == null || !dataNode.isObject()) {
			throw new ResourceException("登录账号异常");
		}
		String shopexid = text(dataNode, "shopexid");
		String eid = text(dataNode, "eid");
		String passportUid = text(dataNode, "passport_uid");
		if (!StringUtils.hasText(shopexid) || !StringUtils.hasText(eid) || !StringUtils.hasText(passportUid)) {
			throw new ResourceException("登录账号异常");
		}
		LinkedHashMap<String, String> data = new LinkedHashMap<>();
		data.put("shopexid", shopexid);
		data.put("eid", eid);
		data.put("passport_uid", passportUid);
		putIfText(data, dataNode, "issue_id");
		putIfText(data, dataNode, "email");

		String refreshToken = text(root, "refresh_token");
		long expiresAt = longField(root, "expires_in");
		long refreshExpiresAt = longField(root, "refresh_expires");

		return new PrismOAuthTokenResult(accessToken, refreshToken, expiresAt, refreshExpiresAt, data);
	}

	private JsonNode readTreeOrNull(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readTree(raw);
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean hasPrismError(JsonNode root) {
		if (root == null || !root.has("error") || root.get("error").isNull()) {
			return false;
		}
		JsonNode e = root.get("error");
		if (e.isBoolean()) {
			return e.booleanValue();
		}
		if (e.isNumber()) {
			return e.intValue() != 0;
		}
		String s = e.asText("").trim();
		return !s.isEmpty() && !"false".equalsIgnoreCase(s) && !"0".equals(s);
	}

	private static void putIfText(LinkedHashMap<String, String> data, JsonNode dataNode, String field) {
		String v = text(dataNode, field);
		if (StringUtils.hasText(v)) {
			data.put(field, v.trim());
		}
	}

	private static String text(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	private static long longField(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return 0L;
		}
		JsonNode v = n.get(field);
		if (v.isNumber()) {
			return v.longValue();
		}
		String s = v.asText("").trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
