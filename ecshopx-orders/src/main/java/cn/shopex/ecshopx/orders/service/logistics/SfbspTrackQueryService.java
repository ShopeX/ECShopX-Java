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

package cn.shopex.ecshopx.orders.service.logistics;

import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.service.setting.SfbspSettingRedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class SfbspTrackQueryService {

	private static final Logger log = LoggerFactory.getLogger(SfbspTrackQueryService.class);

	private static final String DEFAULT_API_URL = "https://sfapi-sbox.sf-express.com/std/service";

	private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final SfbspSettingRedisService sfbspSettingRedisService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public SfbspTrackQueryService(
			SfbspSettingRedisService sfbspSettingRedisService, ObjectMapper objectMapper) {
		this.sfbspSettingRedisService = sfbspSettingRedisService;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(factory);
	}

	public List<LinkedHashMap<String, String>> queryTracesIfApplicable(long companyId, OrdersDelivery row) {
		String corp = row.getDeliveryCorp() == null ? "" : row.getDeliveryCorp().trim();
		if (!"SF".equals(corp)) {
			return null;
		}
		if (companyId <= 0L) {
			return null;
		}

		JsonNode setting = sfbspSettingRedisService.getSfbspSetting(companyId);
		if (setting == null || !setting.isObject()) {
			return null;
		}
		if (setting.has("is_open")) {
			JsonNode openNode = setting.get("is_open");
			String openVal = openNode == null || openNode.isNull() ? "" : openNode.asText();
			if (!"true".equals(openVal)) {
				return null;
			}
		}

		String accesscode = textOrEmpty(setting, "accesscode");
		String checkword = textOrEmpty(setting, "checkword");
		if (accesscode.isEmpty() || checkword.isEmpty()) {
			return null;
		}

		String apiUrl = textOrEmpty(setting, "url");
		if (apiUrl.isEmpty()) {
			apiUrl = DEFAULT_API_URL;
		}

		String logisticCode = row.getDeliveryCode() == null ? "" : row.getDeliveryCode();
		String receiverMobile = row.getReceiverMobile() == null ? "" : row.getReceiverMobile();

		String msgDataJson;
		try {
			LinkedHashMap<String, Object> msg = new LinkedHashMap<>();
			msg.put("language", "0");
			msg.put("trackingType", 1);
			msg.put("trackingNumber", logisticCode);
			msg.put("methodType", 1);
			String mobile = receiverMobile;
			String last4 = mobile.length() >= 4 ? mobile.substring(mobile.length() - 4) : mobile;
			msg.put("checkPhoneNo", last4);
			msgDataJson = objectMapper.writeValueAsString(msg);
		} catch (Exception e) {
			log.warn("顺丰 BSP 构造 msgData 失败 companyId={}", companyId, e);
			return null;
		}

		long timestamp = System.currentTimeMillis() / 1000L;
		String msgDigest;
		try {
			msgDigest = sign(msgDataJson, timestamp, checkword);
		} catch (Exception e) {
			log.warn("顺丰 BSP 签名失败 companyId={}", companyId, e);
			return null;
		}

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("partnerID", accesscode);
		form.add("requestID", newRequestId());
		form.add("serviceCode", "EXP_RECE_SEARCH_ROUTES");
		form.add("timestamp", String.valueOf(timestamp));
		form.add("msgDigest", msgDigest);
		form.add("msgData", msgDataJson);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

		String raw;
		try {
			ResponseEntity<String> resp = restTemplate.postForEntity(apiUrl, entity, String.class);
			raw = resp.getBody();
		} catch (RestClientException e) {
			log.warn("顺丰 BSP HTTP 失败 companyId={}", companyId, e);
			return null;
		}

		try {
			if (raw == null || raw.isBlank()) {
				return null;
			}
			JsonNode envelope = objectMapper.readTree(raw);
			String apiResultCode = textOrEmpty(envelope, "apiResultCode");
			if (!"A1000".equals(apiResultCode)) {
				log.warn(
						"顺丰 BSP api 外层失败 companyId={} code={} msg={}",
						companyId,
						apiResultCode,
						textOrEmpty(envelope, "apiErrorMsg"));
				return null;
			}
			JsonNode dataNode = envelope.get("apiResultData");
			if (dataNode == null || dataNode.isNull()) {
				return null;
			}
			JsonNode inner;
			if (dataNode.isTextual()) {
				String innerStr = dataNode.asText();
				if (innerStr.isBlank()) {
					return null;
				}
				inner = objectMapper.readTree(innerStr);
			} else if (dataNode.isObject()) {
				inner = dataNode;
			} else {
				return null;
			}
			String errorCode = textOrEmpty(inner, "errorCode");
			if (!"S0000".equals(errorCode)) {
				log.info("顺丰 BSP 业务未成功 companyId={} errorCode={}", companyId, errorCode);
				return null;
			}
			JsonNode msgData = inner.get("msgData");
			if (msgData == null || !msgData.isObject()) {
				return Collections.emptyList();
			}
			JsonNode routeResps = msgData.get("routeResps");
			if (routeResps == null || !routeResps.isArray() || routeResps.size() == 0) {
				return Collections.emptyList();
			}
			JsonNode routes = routeResps.get(0).get("routes");
			if (routes == null || !routes.isArray()) {
				return Collections.emptyList();
			}

			List<LinkedHashMap<String, String>> traces = new ArrayList<>();
			for (JsonNode node : routes) {
				LinkedHashMap<String, String> m = new LinkedHashMap<>();
				m.put("AcceptTime", textOrEmpty(node, "acceptTime"));
				String addr = stripTags(textOrEmpty(node, "acceptAddress"));
				String remark = stripTags(textOrEmpty(node, "remark"));
				m.put("AcceptStation", addr + "-" + remark);
				traces.add(m);
			}
			return traces;
		} catch (Exception e) {
			log.warn("顺丰 BSP 解析响应失败 companyId={}", companyId, e);
			return null;
		}
	}

	private static String sign(String msgData, long timestamp, String checkword)
			throws NoSuchAlgorithmException {
		String plain = msgData + timestamp + checkword;
		String urlEncoded = URLEncoder.encode(plain, StandardCharsets.UTF_8);
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] hash = md.digest(urlEncoded.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(hash);
	}

	private static String newRequestId() {
		byte[] rnd = new byte[16];
		SECURE_RANDOM.nextBytes(rnd);
		String chars = md5Hex(rnd);
		return chars.substring(0, 8)
				+ "-"
				+ chars.substring(8, 12)
				+ "-"
				+ chars.substring(12, 16)
				+ "-"
				+ chars.substring(16, 20)
				+ "-"
				+ chars.substring(20, 32);
	}

	private static String md5Hex(byte[] input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input);
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}

	private static String textOrEmpty(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("").trim();
	}

	private static String stripTags(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		return HTML_TAGS.matcher(s).replaceAll("");
	}
}
