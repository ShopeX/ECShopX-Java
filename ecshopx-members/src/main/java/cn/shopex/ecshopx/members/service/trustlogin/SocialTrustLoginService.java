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

package cn.shopex.ecshopx.members.service.trustlogin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 海外社交 OAuth（Apple / Google / Meta·facebook / Line）客户端行为，对齐 PHP {@code SocialTrustLoginService} +
 * overtrue/socialite 4.14.2 默认。
 */
@Service
public class SocialTrustLoginService {

	private static final String APPLE_STATE_REDIS_PREFIX = "ecx:apple_oauth:h5:";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	@Value("${common.h5-base-url:}")
	private String commonH5BaseUrl;

	@Value("${common.api-base-url:}")
	private String commonApiBaseUrl;

	@Value("${APP_URL:}")
	private String appUrl;

	public SocialTrustLoginService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.httpClient =
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
	}

	public boolean isSocialProvider(String trustloginTag) {
		return SocialTrustLoginTypes.isSocial(trustloginTag);
	}

	public String resolveH5Host(String h5HostFromRequest) {
		if (StringUtils.hasText(h5HostFromRequest)) {
			return trimTrailingSlash(h5HostFromRequest.trim());
		}
		String configured = commonH5BaseUrl == null ? "" : commonH5BaseUrl.trim();
		if (StringUtils.hasText(configured)) {
			return trimTrailingSlash(configured);
		}
		if (StringUtils.hasText(appUrl)) {
			return trimTrailingSlash(appUrl.trim());
		}
		return "";
	}

	public String buildRedirectUri(String h5Host, String trustloginTag) {
		if ("apple".equalsIgnoreCase(trustloginTag)) {
			return buildAppleApiCallbackUri();
		}
		String base = trimTrailingSlash(h5Host);
		return base
				+ "/subpages/auth/auth-social-loading?trustlogin_tag="
				+ urlEncode(trustloginTag);
	}

	public String buildAppleApiCallbackUri() {
		String apiBase = resolveApiBaseUrl();
		return trimTrailingSlash(apiBase) + "/h5app/wxapp/trustlogin/apple/callback";
	}

	public String buildAppleH5LandingUrl(String h5Host, String code, Map<String, String> extra) {
		String host = trimTrailingSlash(h5Host);
		if (!host.matches("(?i)^https?://.*")) {
			throw new ResourceException("缺少 H5 域名配置");
		}
		Map<String, String> query = new LinkedHashMap<>();
		query.put("trustlogin_tag", "apple");
		if (extra != null) {
			query.putAll(extra);
		}
		if (StringUtils.hasText(code)) {
			query.put("code", code);
		}
		StringBuilder sb = new StringBuilder(host).append("/subpages/auth/auth-social-loading?");
		boolean first = true;
		for (Map.Entry<String, String> e : query.entrySet()) {
			if (!first) {
				sb.append('&');
			}
			sb.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
			first = false;
		}
		return sb.toString();
	}

	public String getAuthorizeUrl(
			String trustloginTag, Map<String, Object> configRow, String redirectUri, String h5Host) {
		String tag = trustloginTag.toLowerCase();
		String clientId = stringVal(configRow.get("app_id"));
		Map<String, String> query = new LinkedHashMap<>();
		query.put("client_id", clientId);
		query.put("redirect_uri", redirectUri);
		query.put("response_type", "code");
		switch (tag) {
			case "apple" -> {
				query.put("scope", "name email");
				query.put("response_mode", "form_post");
				if (StringUtils.hasText(h5Host)) {
					String state = encodeAppleOAuthState(h5Host);
					rememberAppleOAuthH5Host(state, h5Host);
					query.put("state", state);
				}
				return appendQuery("https://appleid.apple.com/auth/authorize", query);
			}
			case "google" -> {
				query.put(
						"scope",
						"https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
				return appendQuery("https://accounts.google.com/o/oauth2/v2/auth", query);
			}
			case "facebook" -> {
				query.put("scope", "");
				return appendQuery("https://www.facebook.com/v3.3/dialog/oauth", query);
			}
			case "line" -> {
				query.put("scope", "profile");
				query.put("state", md5Hex(UUID.randomUUID().toString()));
				return appendQuery("https://access.line.me/oauth2/v2.1/authorize", query);
			}
			default -> throw new ResourceException("不支持的第三方登录方式");
		}
	}

	public String resolveAppleCallbackH5Host(String queryH5Host, String state) {
		for (String candidate :
				new String[] {
					queryH5Host,
					recallAppleOAuthH5Host(state),
					decodeAppleOAuthStateHost(state),
					System.getenv("H5_BASE_URL") == null ? "" : System.getenv("H5_BASE_URL"),
					System.getenv("COMMON_H5_BASE_URL") == null ? "" : System.getenv("COMMON_H5_BASE_URL")
				}) {
			String host = trimTrailingSlash(candidate == null ? "" : candidate.trim());
			if (StringUtils.hasText(host) && host.matches("(?i)^https?://.*")) {
				return host;
			}
		}
		return "";
	}

	public SocialOAuthUser resolveUserFromCode(
			String trustloginTag, Map<String, Object> configRow, String rawCode, String redirectUri) {
		if (!isSocialProvider(trustloginTag)) {
			throw new ResourceException("不支持的第三方登录方式");
		}
		String code = normalizeOAuthCode(rawCode);
		if (!StringUtils.hasText(code)) {
			throw new ResourceException("授权码无效");
		}
		String tag = trustloginTag.toLowerCase();
		return switch (tag) {
			case "apple" -> resolveAppleUser(configRow, code, redirectUri);
			case "google" -> resolveGoogleUser(configRow, code, redirectUri);
			case "facebook" -> resolveFacebookUser(configRow, code, redirectUri);
			case "line" -> resolveLineUser(configRow, code, redirectUri);
			default -> throw new ResourceException("不支持的第三方登录方式");
		};
	}

	private SocialOAuthUser resolveGoogleUser(
			Map<String, Object> configRow, String code, String redirectUri) {
		Map<String, String> form = tokenForm(configRow, code, redirectUri, true);
		JsonNode token = postForm("https://www.googleapis.com/oauth2/v4/token", form);
		String accessToken = textOrEmpty(token, "access_token");
		JsonNode user = getJson(
				"https://www.googleapis.com/userinfo/v2/me", Map.of(), Map.of("Authorization", "Bearer " + accessToken));
		String id = textOrEmpty(user, "id");
		if (!StringUtils.hasText(id)) {
			throw new ResourceException("第三方授权信息无效");
		}
		return new SocialOAuthUser(
				id,
				"google",
				textOrEmpty(user, "name"),
				textOrEmpty(user, "email"),
				textOrEmpty(user, "picture"));
	}

	private SocialOAuthUser resolveFacebookUser(
			Map<String, Object> configRow, String code, String redirectUri) {
		Map<String, String> query = new LinkedHashMap<>();
		query.put("client_id", stringVal(configRow.get("app_id")));
		query.put("client_secret", stringVal(configRow.get("secret")));
		query.put("code", code);
		query.put("redirect_uri", redirectUri);
		JsonNode token = getJson("https://graph.facebook.com/oauth/access_token", query, Map.of());
		String accessToken = textOrEmpty(token, "access_token");
		String secret = stringVal(configRow.get("secret"));
		String proof = hmacSha256Hex(accessToken, secret);
		JsonNode user =
				getJson(
						"https://graph.facebook.com/v3.3/me",
						Map.of(
								"access_token",
								accessToken,
								"appsecret_proof",
								proof,
								"fields",
								"first_name,last_name,email,gender,verified,picture"),
						Map.of());
		String id = textOrEmpty(user, "id");
		if (!StringUtils.hasText(id)) {
			throw new ResourceException("第三方授权信息无效");
		}
		String name = (textOrEmpty(user, "first_name") + " " + textOrEmpty(user, "last_name")).trim();
		String avatar =
				"https://graph.facebook.com/v3.3/" + id + "/picture?type=normal";
		return new SocialOAuthUser(id, "facebook", name, textOrEmpty(user, "email"), avatar);
	}

	private SocialOAuthUser resolveLineUser(Map<String, Object> configRow, String code, String redirectUri) {
		Map<String, String> form = tokenForm(configRow, code, redirectUri, true);
		JsonNode token = postForm("https://api.line.me/oauth2/v2.1/token", form);
		String accessToken = textOrEmpty(token, "access_token");
		JsonNode user =
				getJson("https://api.line.me/v2/profile", Map.of(), Map.of("Authorization", "Bearer " + accessToken));
		String id = textOrEmpty(user, "userId");
		if (!StringUtils.hasText(id)) {
			throw new ResourceException("第三方授权信息无效");
		}
		return new SocialOAuthUser(
				id, "line", textOrEmpty(user, "displayName"), "", textOrEmpty(user, "pictureUrl"));
	}

	private SocialOAuthUser resolveAppleUser(Map<String, Object> configRow, String code, String redirectUri) {
		Map<String, String> extra = parseExtraConfigMap(configRow.get("extra_config"));
		String teamId = stringVal(extra.get("team_id"));
		String keyId = stringVal(extra.get("key_id"));
		String privateKeyPem = normalizeApplePrivateKey(stringVal(extra.get("private_key")));
		if (!StringUtils.hasText(privateKeyPem) || "***".equals(privateKeyPem)) {
			throw new ResourceException(
					"Apple 私钥未配置：请在后台信任登录中填写 extra_config.private_key（完整 .p8 PEM）后保存");
		}
		if (!StringUtils.hasText(teamId) || !StringUtils.hasText(keyId)) {
			throw new ResourceException("Apple 配置不完整：extra_config 需包含 team_id、key_id、private_key");
		}
		String clientSecret = generateAppleClientSecret(stringVal(configRow.get("app_id")), teamId, keyId, privateKeyPem);
		Map<String, String> form = new LinkedHashMap<>();
		form.put("client_id", stringVal(configRow.get("app_id")));
		form.put("client_secret", clientSecret);
		form.put("code", code);
		form.put("redirect_uri", redirectUri);
		form.put("grant_type", "authorization_code");
		JsonNode token = postForm("https://appleid.apple.com/auth/token", form);
		String idToken = textOrEmpty(token, "id_token");
		if (!StringUtils.hasText(idToken)) {
			throw new ResourceException("第三方授权信息无效");
		}
		JWTClaimsSet claims = verifyAppleIdToken(idToken, stringVal(configRow.get("app_id")));
		String sub = claims.getSubject();
		if (!StringUtils.hasText(sub)) {
			throw new ResourceException("第三方授权信息无效");
		}
		String email = "";
		try {
			email = claims.getStringClaim("email");
		} catch (java.text.ParseException ignored) {
			email = "";
		}
		return new SocialOAuthUser(sub, "apple", "", email == null ? "" : email, "");
	}

	private JWTClaimsSet verifyAppleIdToken(String idToken, String clientId) {
		try {
			SignedJWT jwt = SignedJWT.parse(idToken);
			DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
			JWKSourceBuilder<SecurityContext> builder =
					JWKSourceBuilder.create(new URI("https://appleid.apple.com/auth/keys").toURL());
			JWSKeySelector<SecurityContext> keySelector =
					new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, builder.build());
			processor.setJWSKeySelector(keySelector);
			JWTClaimsSet claims = processor.process(jwt, null);
			if (!"https://appleid.apple.com".equals(claims.getIssuer())) {
				throw new ResourceException("第三方授权信息无效");
			}
			if (claims.getAudience() == null || !claims.getAudience().contains(clientId)) {
				throw new ResourceException("第三方授权信息无效");
			}
			Date exp = claims.getExpirationTime();
			if (exp != null && exp.before(new Date())) {
				throw new ResourceException("第三方授权信息无效");
			}
			return claims;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("第三方授权信息无效");
		}
	}

	private String generateAppleClientSecret(
			String clientId, String teamId, String keyId, String privateKeyPem) {
		try {
			long now = System.currentTimeMillis() / 1000L;
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.issuer(teamId)
					.issueTime(new Date(now * 1000L))
					.expirationTime(new Date((now + 86400L * 180L) * 1000L))
					.audience("https://appleid.apple.com")
					.subject(clientId)
					.build();
			JWSHeader header =
					new JWSHeader.Builder(JWSAlgorithm.ES256)
							.keyID(keyId)
							.type(JOSEObjectType.JWT)
							.build();
			ECPrivateKey privateKey = loadEcPrivateKey(privateKeyPem);
			SignedJWT signed = new SignedJWT(header, claims);
			signed.sign(new ECDSASigner(privateKey));
			return signed.serialize();
		} catch (Exception e) {
			throw new ResourceException("Apple 配置不完整：extra_config 需包含 team_id、key_id、private_key");
		}
	}

	private Map<String, String> tokenForm(
			Map<String, Object> configRow, String code, String redirectUri, boolean withGrantType) {
		Map<String, String> form = new LinkedHashMap<>();
		form.put("client_id", stringVal(configRow.get("app_id")));
		form.put("client_secret", stringVal(configRow.get("secret")));
		form.put("code", code);
		form.put("redirect_uri", redirectUri);
		if (withGrantType) {
			form.put("grant_type", "authorization_code");
		}
		return form;
	}

	public void rememberAppleOAuthH5Host(String state, String h5Host) {
		String s = state == null ? "" : state.trim();
		String host = trimTrailingSlash(h5Host == null ? "" : h5Host.trim());
		if (!StringUtils.hasText(s) || !StringUtils.hasText(host)) {
			return;
		}
		try {
			sharedStringRedisTemplate
					.opsForValue()
					.set(APPLE_STATE_REDIS_PREFIX + md5Hex(s), host, Duration.ofSeconds(600));
		} catch (DataAccessException e) {
			// best effort
		}
	}

	public String recallAppleOAuthH5Host(String state) {
		String s = state == null ? "" : state.trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		try {
			String v = sharedStringRedisTemplate.opsForValue().get(APPLE_STATE_REDIS_PREFIX + md5Hex(s));
			return v == null ? "" : trimTrailingSlash(v.trim());
		} catch (DataAccessException e) {
			return "";
		}
	}

	public String encodeAppleOAuthState(String h5Host) {
		try {
			String json =
					objectMapper.writeValueAsString(Map.of("h5_host", trimTrailingSlash(h5Host.trim())));
			return base64UrlEncode(json.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new ResourceException("缺少 H5 域名配置");
		}
	}

	private String decodeAppleOAuthStateHost(String state) {
		try {
			byte[] raw = base64UrlDecode(state);
			JsonNode node = objectMapper.readTree(raw);
			return textOrEmpty(node, "h5_host");
		} catch (Exception e) {
			return "";
		}
	}

	public static String normalizeOAuthCode(String code) {
		String c = code == null ? "" : code.trim();
		if (c.isEmpty()) {
			return "";
		}
		String prev;
		do {
			prev = c;
			c = java.net.URLDecoder.decode(c, StandardCharsets.UTF_8);
		} while (!prev.equals(c));
		return c;
	}

	public static String normalizeApplePrivateKey(String privateKey) {
		String pk = privateKey == null ? "" : privateKey.trim();
		if (pk.isEmpty() || "***".equals(pk)) {
			return pk;
		}
		pk = pk.replace("\\n", "\n").replace("\r\n", "\n").replace("\r", "\n");
		if (!pk.contains("BEGIN PRIVATE KEY")) {
			String body = pk.replaceAll("\\s+", "");
			StringBuilder wrapped = new StringBuilder("-----BEGIN PRIVATE KEY-----\n");
			for (int i = 0; i < body.length(); i += 64) {
				wrapped.append(body, i, Math.min(i + 64, body.length())).append('\n');
			}
			wrapped.append("-----END PRIVATE KEY-----");
			pk = wrapped.toString();
		}
		return pk;
	}

	private String resolveApiBaseUrl() {
		String configured = commonApiBaseUrl == null ? "" : commonApiBaseUrl.trim();
		if (StringUtils.hasText(configured)) {
			return trimTrailingSlash(configured);
		}
		String base = appUrl == null ? "" : appUrl.trim();
		if (StringUtils.hasText(base)) {
			return trimTrailingSlash(base) + "/api/v1";
		}
		return "";
	}

	private JsonNode postForm(String url, Map<String, String> form) {
		String body =
				form.entrySet().stream()
						.map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
						.reduce((a, b) -> a + "&" + b)
						.orElse("");
		try {
			HttpRequest req =
					HttpRequest.newBuilder()
							.uri(URI.create(url))
							.header("Content-Type", "application/x-www-form-urlencoded")
							.header("Accept", "application/json")
							.POST(HttpRequest.BodyPublishers.ofString(body))
							.build();
			HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			return objectMapper.readTree(resp.body());
		} catch (Exception e) {
			throw new ResourceException("第三方授权信息无效");
		}
	}

	private JsonNode getJson(String url, Map<String, String> query, Map<String, String> headers) {
		try {
			StringBuilder sb = new StringBuilder(url);
			if (!query.isEmpty()) {
				sb.append('?');
				boolean first = true;
				for (Map.Entry<String, String> e : query.entrySet()) {
					if (!first) {
						sb.append('&');
					}
					sb.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
					first = false;
				}
			}
			HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(sb.toString())).GET();
			headers.forEach(b::header);
			b.header("Accept", "application/json");
			HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
			return objectMapper.readTree(resp.body());
		} catch (Exception e) {
			throw new ResourceException("第三方授权信息无效");
		}
	}

	private static Map<String, String> parseExtraConfigMap(Object raw) {
		if (raw instanceof Map<?, ?> map) {
			Map<String, String> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				if (e.getKey() != null) {
					out.put(String.valueOf(e.getKey()), e.getValue() == null ? "" : String.valueOf(e.getValue()));
				}
			}
			return out;
		}
		if (raw == null) {
			return Map.of();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return Map.of();
		}
		try {
			Map<String, String> decoded =
					new ObjectMapper().readValue(s, new TypeReference<Map<String, String>>() {});
			return decoded != null ? decoded : Map.of();
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static ECPrivateKey loadEcPrivateKey(String pem) throws Exception {
		String normalized =
				pem.replace("-----BEGIN PRIVATE KEY-----", "")
						.replace("-----END PRIVATE KEY-----", "")
						.replaceAll("\\s+", "");
		byte[] decoded = Base64.getDecoder().decode(normalized);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
		return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
	}

	private static String appendQuery(String base, Map<String, String> query) {
		StringBuilder sb = new StringBuilder(base).append('?');
		boolean first = true;
		for (Map.Entry<String, String> e : query.entrySet()) {
			if (!first) {
				sb.append('&');
			}
			sb.append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
			first = false;
		}
		return sb.toString();
	}

	private static String urlEncode(String v) {
		return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
	}

	private static String trimTrailingSlash(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		String out = s.trim();
		while (out.endsWith("/")) {
			out = out.substring(0, out.length() - 1);
		}
		return out;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static String textOrEmpty(JsonNode node, String field) {
		if (node == null || node.get(field) == null || node.get(field).isNull()) {
			return "";
		}
		return node.get(field).asText("");
	}

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			return input;
		}
	}

	private static String hmacSha256Hex(String data, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : raw) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new ResourceException("第三方授权信息无效");
		}
	}

	private static String base64UrlEncode(byte[] data) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}

	private static byte[] base64UrlDecode(String state) {
		String normalized = state.replace('-', '+').replace('_', '/');
		int padding = normalized.length() % 4;
		if (padding > 0) {
			normalized += "====".substring(padding);
		}
		return Base64.getDecoder().decode(normalized);
	}
}
