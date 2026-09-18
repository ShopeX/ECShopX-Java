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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class KdniaoTrackQueryService {

	private static final String API_URL = "http://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx";
	private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public KdniaoTrackQueryService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(factory);
	}

	public List<LinkedHashMap<String, String>> queryTraces(
			long companyId, String shipperCode, String logisticCode, JsonNode kdniaoSettings) {
		String ebusinessId = textOrEmpty(kdniaoSettings, "EBusinessID");
		String apiKey = textOrEmpty(kdniaoSettings, "ApiKey");
		if (apiKey.isEmpty()) {
			apiKey = textOrEmpty(kdniaoSettings, "appkey");
		}
		if (ebusinessId.isEmpty() || apiKey.isEmpty()) {
			throw new ResourceException("无效的快递查询配置");
		}
		String requestType = textOrEmpty(kdniaoSettings, "request_type");
		if (requestType.isEmpty()) {
			requestType = "8001";
		}
		String requestJson;
		try {
			LinkedHashMap<String, String> reqMap = new LinkedHashMap<>();
			reqMap.put("OrderCode", "");
			reqMap.put("CustomerName", "");
			reqMap.put("ShipperCode", shipperCode == null ? "" : shipperCode);
			reqMap.put("LogisticCode", logisticCode == null ? "" : logisticCode);
			requestJson = objectMapper.writeValueAsString(reqMap);
		} catch (Exception e) {
			throw new ResourceException("查询失败，请到快递公司官网查询");
		}
		byte[] md5Bytes;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md5Bytes = md.digest((requestJson + apiKey).getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
		String dataSign = Base64.getEncoder().encodeToString(md5Bytes);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("RequestData", requestJson);
		form.add("EBusinessID", ebusinessId);
		form.add("RequestType", requestType);
		form.add("DataSign", dataSign);
		form.add("DataType", "2");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		String raw = null;
		try {
			ResponseEntity<String> resp = restTemplate.postForEntity(API_URL, entity, String.class);
			raw = resp.getBody();
		} catch (RestClientException e) {
			log.warn("kdniao HTTP error companyId={}", companyId, e);
		}
		JsonNode root;
		try {
			if (raw == null || raw.isBlank()) {
				throw new ResourceException("查询失败，请到快递公司官网查询");
			}
			root = objectMapper.readTree(raw);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("查询失败，请到快递公司官网查询");
		}
		if (isKdniaoSuccess(root)) {
			List<LinkedHashMap<String, String>> traces = parseTraces(root);
			traces.sort(
					Comparator.comparing(
							(Map<String, String> m) -> m.getOrDefault("AcceptTime", ""),
							Comparator.reverseOrder()));
			return traces;
		}
		String reason = textOrEmpty(root, "Reason");
		if (!reason.isEmpty()) {
			throw new ResourceException(reason);
		}
		throw new ResourceException("查询失败，请到快递公司官网查询");
	}

	private static boolean isKdniaoSuccess(JsonNode root) {
		if (root == null || root.isNull()) {
			return false;
		}
		JsonNode s = root.get("Success");
		if (s == null || s.isNull()) {
			return false;
		}
		if (s.isBoolean()) {
			return s.booleanValue();
		}
		if (s.isTextual()) {
			return "true".equalsIgnoreCase(s.asText());
		}
		return false;
	}

	private List<LinkedHashMap<String, String>> parseTraces(JsonNode root) {
		JsonNode tracesNode = root.get("Traces");
		List<LinkedHashMap<String, String>> out = new ArrayList<>();
		if (tracesNode == null || !tracesNode.isArray()) {
			return out;
		}
		for (JsonNode el : tracesNode) {
			if (el == null || !el.isObject()) {
				continue;
			}
			String acceptTime = el.has("AcceptTime") ? el.get("AcceptTime").asText("") : "";
			String station = "";
			if (el.has("AcceptStation") && el.get("AcceptStation").isTextual()) {
				String a = el.get("AcceptStation").asText();
				if (a != null && !a.isBlank()) {
					station = a;
				}
			}
			if (station.isEmpty() && el.has("Station")) {
				station = el.get("Station").asText("");
			}
			LinkedHashMap<String, String> row = new LinkedHashMap<>();
			row.put("AcceptTime", acceptTime);
			row.put("AcceptStation", stripTags(station).trim());
			out.add(row);
		}
		return out;
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
}
