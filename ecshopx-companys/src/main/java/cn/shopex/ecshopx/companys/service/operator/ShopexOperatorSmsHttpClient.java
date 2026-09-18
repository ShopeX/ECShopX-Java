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

package cn.shopex.ecshopx.companys.service.operator;

import cn.shopex.ecshopx.companys.config.OperatorSmsGatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ShopexOperatorSmsHttpClient {

	private static final String SERVER_TIME_URL = "http://webapi.sms.shopex.cn";
	private static final String SERVER_TIME_SIGN_TOKEN = "SMS_TIME";

	private final OperatorSmsGatewayProperties properties;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public ShopexOperatorSmsHttpClient(OperatorSmsGatewayProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(15_000);
		this.restTemplate = new RestTemplate(factory);
	}

	/**
	 * @return true when gateway responds with res=succ
	 */
	public boolean sendMobileCode(String mobile, String verifyCode) {
		return sendMobileCode(
				mobile,
				verifyCode,
				"尊敬的用户，您好，您的手机验证码是：" + verifyCode + "。");
	}

	/**
	 * @param innerSmsBody user-visible sentence placed before the signature suffix in the gateway payload
	 * @return true when gateway responds with res=succ
	 */
	public boolean sendMobileCode(String mobile, String verifyCode, String innerSmsBody) {
		if (!properties.hasEntCredentials()) {
			return false;
		}
		String timestamp;
		try {
			timestamp = fetchServerTime();
		} catch (Exception e) {
			return false;
		}
		if (timestamp == null || timestamp.isEmpty()) {
			return false;
		}

		Map<String, String> sendStr = new LinkedHashMap<>();
		sendStr.put("certi_app", "sms.send");
		sendStr.put("entId", nullToEmpty(properties.getEntId()));
		sendStr.put("entPwd", nullToEmpty(properties.getEntPwd()));
		sendStr.put("license", nullToEmpty(properties.getLicense()));
		sendStr.put("source", nullToEmpty(properties.getSource()));
		sendStr.put("sendType", "notice");
		sendStr.put("version", "1.0");
		sendStr.put("format", "json");
		sendStr.put("timestamp", timestamp);
		try {
			sendStr.put("contents", buildContentsJson(mobile, innerSmsBody));
		} catch (Exception e) {
			return false;
		}
		sendStr.put("certi_ac", signParams(sendStr, nullToEmpty(properties.getSecret())));

		String apiUrl = properties.getApiUrl() != null && !properties.getApiUrl().isBlank()
				? properties.getApiUrl().trim()
				: "http://api.sms.shopex.cn";

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		sendStr.forEach(form::add);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

		try {
			ResponseEntity<String> res =
					restTemplate.postForEntity(apiUrl, new HttpEntity<>(form, headers), String.class);
			if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
				return false;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed = objectMapper.readValue(res.getBody(), Map.class);
			Object r = parsed.get("res");
			return "succ".equals(r != null ? r.toString() : null);
		} catch (RestClientException | java.io.IOException e) {
			return false;
		}
	}

	private String fetchServerTime() throws java.io.IOException {
		Map<String, String> substr = new LinkedHashMap<>();
		substr.put("certi_app", "sms.servertime");
		substr.put("version", "1.0");
		substr.put("format", "json");
		substr.put("certi_ac", signParams(substr, SERVER_TIME_SIGN_TOKEN));

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		substr.forEach(form::add);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		ResponseEntity<String> res =
				restTemplate.postForEntity(SERVER_TIME_URL, new HttpEntity<>(form, headers), String.class);
		if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> parsed = objectMapper.readValue(res.getBody(), Map.class);
		Object info = parsed.get("info");
		return info != null ? info.toString() : null;
	}

	private String buildContentsJson(String mobile, String innerSmsBody) throws Exception {
		List<Map<String, String>> row = new ArrayList<>();
		row.add(Map.of("phones", mobile, "content", innerSmsBody + "【商派】"));
		return objectMapper.writeValueAsString(row);
	}

	static String signParams(Map<String, String> params, String token) {
		String assembled = assemble(params);
		return md5Lower(assembled + md5Lower(token));
	}

	private static String assemble(Map<String, String> params) {
		List<String> keys = new ArrayList<>(params.keySet());
		Collections.sort(keys, String::compareTo);
		StringBuilder sign = new StringBuilder();
		for (String key : keys) {
			if ("certi_ac".equals(key)) {
				continue;
			}
			String val = params.get(key);
			sign.append(val != null ? val : "");
		}
		return sign.toString();
	}

	private static String md5Lower(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
