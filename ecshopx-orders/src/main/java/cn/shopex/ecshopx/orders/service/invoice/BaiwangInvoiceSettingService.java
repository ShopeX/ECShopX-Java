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

package cn.shopex.ecshopx.orders.service.invoice;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BaiwangInvoiceSettingService {

	private static final Logger log = LoggerFactory.getLogger(BaiwangInvoiceSettingService.class);

	private static final Pattern TAX_NO_PATTERN = Pattern.compile("^[0-9A-Z]{15,20}$");

	private static final String DEFAULT_SANDBOX_API_URL = "https://sandbox-openapi.baiwang.com/router/rest";
	private static final String DEFAULT_SANDBOX_TOKEN_URL = "https://sandbox-openapi.baiwang.com/auth/token";

	private final StringRedisTemplate companysRedisTemplate;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String defaultApiUrl;
	private final String defaultTokenUrl;
	private final String envAppKey;
	private final String envAppSecret;

	public BaiwangInvoiceSettingService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.baiwang.api-url:}") String defaultApiUrl,
			@Value("${ecshopx.baiwang.token-url:}") String defaultTokenUrl,
			@Value("${BAIWANG_APP_KEY:}") String envAppKey,
			@Value("${BAIWANG_APP_SECRET:}") String envAppSecret) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.defaultApiUrl = defaultApiUrl == null ? "" : defaultApiUrl;
		this.defaultTokenUrl = defaultTokenUrl == null ? "" : defaultTokenUrl;
		this.envAppKey = envAppKey;
		this.envAppSecret = envAppSecret;
	}

	public Map<String, Object> setBaiwangInvoiceSetting(long companyId, Map<String, Object> data) {
		Map<String, Object> check = checkInvoiceSetting(data);
		if (!Boolean.TRUE.equals(check.get("success"))) {
			return check;
		}

		LinkedHashMap<String, Object> saveData = new LinkedHashMap<>();
		saveData.put("appKey", stringVal(data.get("appKey")));
		saveData.put("appSecret", stringVal(data.get("appSecret")));
		saveData.put("username", stringVal(data.get("username")));
		saveData.put("password", data.containsKey("password") ? stringVal(data.get("password")) : "");
		saveData.put("orgAuthCode", stringVal(data.get("orgAuthCode")));
		saveData.put("taxNo", stringVal(data.get("taxNo")));
		saveData.put("terminal", optString(data, "terminal"));
		saveData.put("mobile", optString(data, "mobile"));
		saveData.put("drawer", optString(data, "drawer"));
		saveData.put("payee", optString(data, "payee"));
		saveData.put("checker", optString(data, "checker"));

		String taxRateStr;
		if (data.containsKey("tax_rate") && !isLooseEmpty(data.get("tax_rate"))) {
			taxRateStr = formatTaxRateString(parseTaxRateDouble(data.get("tax_rate")));
		} else {
			taxRateStr = "0.03";
		}
		saveData.put("tax_rate", taxRateStr);

		String apiUrlRaw = data.containsKey("api_url") ? stringVal(data.get("api_url")) : "";
		String tokenUrlRaw = data.containsKey("token_url") ? stringVal(data.get("token_url")) : "";
		saveData.put("api_url", resolveApiUrl(apiUrlRaw));
		saveData.put("token_url", resolveTokenUrl(tokenUrlRaw));
		saveData.put("updated_at", System.currentTimeMillis() / 1000);

		String json;
		try {
			json = objectMapper.copy()
					.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)
					.writeValueAsString(saveData);
		} catch (JsonProcessingException e) {
			LinkedHashMap<String, Object> err = new LinkedHashMap<>();
			err.put("success", false);
			err.put("message", "保存失败");
			return err;
		}

		String key = "BaiwangInvoiceSetting:" + companyId;
		try {
			companysRedisTemplate.opsForValue().set(key, json);
		} catch (RuntimeException e) {
			LinkedHashMap<String, Object> err = new LinkedHashMap<>();
			err.put("success", false);
			err.put("message", "保存失败，Redis 连接异常");
			return err;
		}

		clearBaiwangCache();

		LinkedHashMap<String, Object> ok = new LinkedHashMap<>();
		ok.put("success", true);
		ok.put("message", "百旺发票配置保存成功");
		ok.put("data", saveData);
		return ok;
	}

	public Map<String, Object> getBaiwangInvoiceSetting(long companyId) {
		String key = "BaiwangInvoiceSetting:" + companyId;
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key);
		} catch (RuntimeException e) {
			log.debug("getBaiwangInvoiceSetting redis get skipped: {}", e.toString());
			return configNotExistsPayload();
		}
		if (raw == null || raw.isBlank()) {
			return configNotExistsPayload();
		}
		try {
			Map<String, Object> config = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("success", Boolean.TRUE);
			out.put("data", config);
			return out;
		} catch (JsonProcessingException e) {
			log.debug("getBaiwangInvoiceSetting parse failed: {}", e.toString());
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("success", Boolean.TRUE);
			out.put("data", null);
			return out;
		}
	}

	private static LinkedHashMap<String, Object> configNotExistsPayload() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("success", Boolean.TRUE);
		out.put("message", "配置不存在");
		out.put("data", List.of());
		return out;
	}

	private Map<String, Object> checkInvoiceSetting(Map<String, Object> data) {
		Object appKey = data.get("appKey");
		if (isLooseEmpty(appKey)) {
			return fail("AppKey不能为空");
		}
		if (!(appKey instanceof String)) {
			return fail("AppKey 必须是字符串");
		}

		Object appSecret = data.get("appSecret");
		if (isLooseEmpty(appSecret)) {
			return fail("AppSecret不能为空");
		}
		if (!(appSecret instanceof String)) {
			return fail("AppSecret 必须是字符串");
		}

		if (isLooseEmpty(data.get("username"))) {
			return fail("用户名不能为空");
		}
		if (isLooseEmpty(data.get("orgAuthCode"))) {
			return fail("机构认证码不能为空");
		}
		if (isLooseEmpty(data.get("taxNo"))) {
			return fail("税号不能为空");
		}
		String taxNoStr = stringVal(data.get("taxNo"));
		if (!TAX_NO_PATTERN.matcher(taxNoStr).matches()) {
			return fail("税号格式不正确");
		}

		if (data.containsKey("tax_rate") && !isLooseEmpty(data.get("tax_rate"))) {
			double rate;
			try {
				rate = parseTaxRateDouble(data.get("tax_rate"));
			} catch (NumberFormatException e) {
				return fail("税率必须在0到1之间");
			}
			if (rate < 0.0 || rate > 1.0 || Double.isNaN(rate)) {
				return fail("税率必须在0到1之间");
			}
		}

		if (data.containsKey("api_url") && !isLooseEmpty(data.get("api_url"))) {
			String u = stringVal(data.get("api_url")).trim();
			if (!isValidHttpUrl(u)) {
				return fail("API地址格式不正确");
			}
		}
		if (data.containsKey("token_url") && !isLooseEmpty(data.get("token_url"))) {
			String u = stringVal(data.get("token_url")).trim();
			if (!isValidHttpUrl(u)) {
				return fail("Token地址格式不正确");
			}
		}

		LinkedHashMap<String, Object> ok = new LinkedHashMap<>();
		ok.put("success", true);
		return ok;
	}

	private static LinkedHashMap<String, Object> fail(String message) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("success", false);
		m.put("message", message);
		return m;
	}

	private static boolean isLooseEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (v instanceof CharSequence s) {
			return s.length() == 0 || "0".contentEquals(s);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static String stringVal(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	private String resolveApiUrl(String fromRequest) {
		if (StringUtils.hasText(fromRequest.trim())) {
			return fromRequest.trim();
		}
		if (StringUtils.hasText(defaultApiUrl.trim())) {
			return defaultApiUrl.trim();
		}
		return DEFAULT_SANDBOX_API_URL;
	}

	private String resolveTokenUrl(String fromRequest) {
		if (StringUtils.hasText(fromRequest.trim())) {
			return fromRequest.trim();
		}
		if (StringUtils.hasText(defaultTokenUrl.trim())) {
			return defaultTokenUrl.trim();
		}
		return DEFAULT_SANDBOX_TOKEN_URL;
	}

	private static String optString(Map<String, Object> data, String key) {
		if (!data.containsKey(key) || data.get(key) == null) {
			return "";
		}
		return String.valueOf(data.get(key));
	}

	private static double parseTaxRateDouble(Object v) {
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		if (v instanceof CharSequence s) {
			return Double.parseDouble(s.toString().trim());
		}
		return Double.parseDouble(String.valueOf(v).trim());
	}

	private static String formatTaxRateString(double d) {
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			return "0";
		}
		return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
	}

	private static boolean isValidHttpUrl(String s) {
		if (!StringUtils.hasText(s)) {
			return false;
		}
		try {
			URI u = new URI(s);
			String scheme = u.getScheme();
			if (scheme == null) {
				return false;
			}
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				return false;
			}
			return StringUtils.hasText(u.getHost());
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private void clearBaiwangCache() {
		try {
			String raw = (envAppKey == null ? "" : envAppKey) + (envAppSecret == null ? "" : envAppSecret);
			String md5 = md5HexLowerCase(raw);
			sharedStringRedisTemplate.delete("baiwang:access_token:" + md5);
			sharedStringRedisTemplate.delete("baiwang:refresh_token:" + md5);
		} catch (RuntimeException e) {
			log.debug("clearBaiwangCache skipped: {}", e.toString());
		}
	}

	private static String md5HexLowerCase(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(Character.forDigit((b >> 4) & 0xF, 16));
				sb.append(Character.forDigit(b & 0xF, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}
}
