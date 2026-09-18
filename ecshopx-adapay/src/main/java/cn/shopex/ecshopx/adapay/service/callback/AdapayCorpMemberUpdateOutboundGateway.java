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

package cn.shopex.ecshopx.adapay.service.callback;

import cn.shopex.ecshopx.adapay.config.AdapayCallbackProperties;
import cn.shopex.ecshopx.adapay.service.AdapayPaymentSettingRedisReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class AdapayCorpMemberUpdateOutboundGateway {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapayCallbackProperties properties;
	private final String adapayNotifyUrl;

	public AdapayCorpMemberUpdateOutboundGateway(
			@Qualifier("adapayCallbackRestTemplate") RestTemplate adapayCallbackRestTemplate,
			ObjectMapper objectMapper,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayCallbackProperties properties,
			@Value("${adapay.notify-url:}") String adapayNotifyUrl) {
		this.restTemplate = adapayCallbackRestTemplate;
		this.objectMapper = objectMapper;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.properties = properties;
		this.adapayNotifyUrl = adapayNotifyUrl == null ? "" : adapayNotifyUrl;
	}

	public Map<String, Object> corpMemberUpdate(
			long companyId,
			String appId,
			Map<String, String> memberInfo,
			byte[] outboundZipBytesOrNull,
			String outboundAttachFileUrlOrNull) {
		if (!StringUtils.hasText(properties.getSettleOutboundBaseUrl())) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("status", "succeeded");
			return Map.of("data", data);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", companyId);
		body.put("app_id", appId);
		body.put("member_id", strOrEmpty(memberInfo.get("member_id")));
		String orderNo =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
						.withZone(ZoneId.systemDefault())
						.format(Instant.now())
						+ ThreadLocalRandom.current().nextInt(1000, 10000)
						+ strOrEmpty(memberInfo.get("member_id"));
		body.put("order_no", orderNo);
		body.put("name", strOrEmpty(memberInfo.get("name")));
		body.put("prov_code", strOrEmpty(memberInfo.get("prov_code")));
		body.put("area_code", strOrEmpty(memberInfo.get("area_code")));
		body.put("social_credit_code", strOrEmpty(memberInfo.get("social_credit_code")));
		body.put("social_credit_code_expires", strOrEmpty(memberInfo.get("social_credit_code_expires")));
		body.put("business_scope", strOrEmpty(memberInfo.get("business_scope")));
		body.put("legal_person", strOrEmpty(memberInfo.get("legal_person")));
		body.put("legal_cert_id", strOrEmpty(memberInfo.get("legal_cert_id")));
		body.put("legal_cert_id_expires", strOrEmpty(memberInfo.get("legal_cert_id_expires")));
		body.put("legal_mp", strOrEmpty(memberInfo.get("legal_mp")));
		body.put("address", strOrEmpty(memberInfo.get("address")));
		body.put("zip_code", strOrEmpty(memberInfo.get("zip_code")));
		body.put("telphone", strOrEmpty(memberInfo.get("telphone")));
		body.put("email", strOrEmpty(memberInfo.get("email")));
		body.put("notify_url", adapayNotifyUrl);
		body.put("api_method", "CorpMember.update");
		if (outboundZipBytesOrNull != null) {
			body.put("attach_file", outboundAttachFileUrlOrNull == null ? "" : outboundAttachFileUrlOrNull);
			body.put("file_content", Base64.getEncoder().encodeToString(outboundZipBytesOrNull));
		}
		Map<String, Object> merchant = adapayPaymentSettingRedisReader.getPaymentSetting(companyId);
		body.put("merchant_info", merchant == null ? new LinkedHashMap<>() : merchant);
		return postJson(body);
	}

	private Map<String, Object> postJson(Map<String, Object> body) {
		String url = properties.getSettleOutboundBaseUrl();
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String json;
		try {
			json = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("status", "failed");
			err.put("error_msg", "请求序列化失败");
			return Map.of("data", err);
		}
		ResponseEntity<String> resp =
				restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
		try {
			return objectMapper.readValue(resp.getBody(), new TypeReference<>() {});
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("status", "failed");
			err.put("error_msg", "响应解析失败");
			return Map.of("data", err);
		}
	}

	private static String strOrEmpty(String s) {
		return s == null ? "" : s;
	}
}
