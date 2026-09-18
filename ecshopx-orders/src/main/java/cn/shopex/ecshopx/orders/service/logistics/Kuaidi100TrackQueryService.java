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

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class Kuaidi100TrackQueryService {

	private static final String POLL_URL = "https://poll.kuaidi100.com/poll/query.do";
	private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public Kuaidi100TrackQueryService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(factory);
	}

	public List<LinkedHashMap<String, String>> queryTraces(
			long companyId,
			String comParam,
			String logisticCode,
			String receiverMobile,
			JsonNode kuaidi100Settings) {
		// Admin UI: app_key=Key, app_secret=Customer. PHP LogisticTracker posts
		// customer=app_secret and signs with MD5(param + app_key + app_secret).
		String customer = textOrEmpty(kuaidi100Settings, "app_secret");
		String keySecret = textOrEmpty(kuaidi100Settings, "app_key");
		if (customer.isEmpty() || keySecret.isEmpty()) {
			throw new ResourceException("无效的快递查询配置");
		}
		String phone = receiverMobile == null ? "" : receiverMobile;
		String paramJson;
		try {
			ObjectNode paramObj = objectMapper.createObjectNode();
			paramObj.put("com", comParam == null ? "" : comParam);
			paramObj.put("num", logisticCode == null ? "" : logisticCode);
			paramObj.put("phone", phone);
			paramObj.put("from", "");
			paramObj.put("to", "");
			paramJson = objectMapper.writeValueAsString(paramObj);
		} catch (Exception e) {
			throw new ResourceException("查询失败，请到快递公司官网查询");
		}
		String sign = md5UpperHex(paramJson + keySecret + customer);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("customer", customer);
		form.add("param", paramJson);
		form.add("sign", sign);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		String raw;
		try {
			ResponseEntity<String> resp = restTemplate.postForEntity(POLL_URL, entity, String.class);
			raw = resp.getBody();
		} catch (RestClientException e) {
			log.warn("kuaidi100 poll query HTTP error companyId={}", companyId, e);
			throw new ResourceException("查询失败，请到快递公司官网查询");
		}
		if (raw == null || raw.isBlank()) {
			throw new ResourceException("查询失败，请到快递公司官网查询");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new ResourceException("查询失败，请到快递公司官网查询");
		}
		if (isKuaidi100Failure(root)) {
			String msg = kuaidi100ErrorMessage(root);
			log.warn("kuaidi100 poll query failed companyId={} msg={}", companyId, msg);
			throw new ResourceException(msg);
		}
		JsonNode data = root.get("data");
		if (data == null || !data.isArray()) {
			return new ArrayList<>();
		}
		List<LinkedHashMap<String, String>> out = new ArrayList<>();
		for (JsonNode item : data) {
			if (item == null || !item.isObject()) {
				continue;
			}
			String ftime = item.has("ftime") ? item.get("ftime").asText("") : "";
			String context = item.has("context") ? item.get("context").asText("") : "";
			LinkedHashMap<String, String> row = new LinkedHashMap<>();
			row.put("AcceptTime", ftime);
			row.put("AcceptStation", stripTags(context).trim());
			out.add(row);
		}
		return out;
	}

	private static boolean isKuaidi100Failure(JsonNode root) {
		if (root == null || root.isNull()) {
			return true;
		}
		JsonNode resultNode = root.get("result");
		if (resultNode != null && !resultNode.isNull()) {
			if (resultNode.isBoolean() && !resultNode.booleanValue()) {
				return true;
			}
			if (resultNode.isTextual() && "false".equalsIgnoreCase(resultNode.asText())) {
				return true;
			}
		}
		if (root.has("status_code")) {
			JsonNode sc = root.get("status_code");
			String scStr;
			if (sc.isNumber()) {
				scStr = String.valueOf(sc.asInt());
			} else {
				scStr = sc.asText("").trim();
			}
			if (!"200".equals(scStr)) {
				return true;
			}
		}
		return false;
	}

	private static String kuaidi100ErrorMessage(JsonNode root) {
		if (root.has("message") && root.get("message").isTextual()) {
			String m = root.get("message").asText().trim();
			if (!m.isEmpty()) {
				return m;
			}
		}
		return "查询失败，请到快递公司官网查询";
	}

	private static String textOrEmpty(JsonNode root, String field) {
		if (root == null || !root.isObject() || field == null) {
			return "";
		}
		JsonNode n = root.get(field);
		if (n == null || n.isNull() || n.isMissingNode()) {
			return "";
		}
		if (!n.isTextual()) {
			return "";
		}
		return n.asText("").trim();
	}

	private static String stripTags(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		return HTML_TAGS.matcher(s).replaceAll("");
	}

	private static String md5UpperHex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(32);
			for (byte b : digest) {
				sb.append(String.format("%02X", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}
}
