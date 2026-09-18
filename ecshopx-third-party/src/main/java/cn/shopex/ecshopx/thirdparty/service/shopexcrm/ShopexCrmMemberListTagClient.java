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

package cn.shopex.ecshopx.thirdparty.service.shopexcrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class ShopexCrmMemberListTagClient {

	private static final Logger log = LoggerFactory.getLogger(ShopexCrmMemberListTagClient.class);

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper;

	@Value("${crm.crm-sync:false}")
	private boolean crmSyncEnabled;

	@Value("${crm.crm-url:}")
	private String crmUrl;

	@Value("${crm.crm-app-key:}")
	private String crmAppKey;

	@Value("${crm.crm-app-secret:}")
	private String crmAppSecret;

	public ShopexCrmMemberListTagClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void mergeCrmTagsIntoRows(long companyId, List<Map<String, Object>> rows) {
		if (!crmSyncEnabled || rows == null || rows.isEmpty()) {
			return;
		}
		if (!StringUtils.hasText(crmUrl) || !StringUtils.hasText(crmAppKey) || !StringUtils.hasText(crmAppSecret)) {
			log.info("crm参数配置错误");
			return;
		}
		List<String> mobiles = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object m = row.get("mobile");
			if (m != null && StringUtils.hasText(String.valueOf(m))) {
				mobiles.add(String.valueOf(m).trim());
			}
		}
		if (mobiles.isEmpty()) {
			return;
		}
		String joined = String.join(",", mobiles);
		JsonNode root;
		try {
			root = callGetMemberListApi(joined);
		} catch (Exception e) {
			log.warn("crm merge skipped company_id={} reason={}", companyId, e.toString());
			return;
		}
		if (root == null || !root.isObject()) {
			return;
		}
		JsonNode result = root.path("result");
		JsonNode items = result.path("items");
		if (!items.isArray()) {
			return;
		}
		Map<Long, List<Map<String, Object>>> tagsByUserId = new LinkedHashMap<>();
		for (JsonNode item : items) {
			if (!item.isObject()) {
				continue;
			}
			long extMemberId = item.path("ext_member_id").asLong(0L);
			if (extMemberId == 0L) {
				continue;
			}
			List<Map<String, Object>> mergedTags = new ArrayList<>();
			appendTags(mergedTags, item.path("dynamic_tags"));
			appendTags(mergedTags, item.path("static_tags"));
			for (Map<String, Object> t : mergedTags) {
				t.put("tag_id", "crm");
			}
			tagsByUserId.put(extMemberId, mergedTags);
		}
		for (Map<String, Object> row : rows) {
			Object uidObj = row.get("user_id");
			long uid = uidObj instanceof Number n ? n.longValue() : 0L;
			if (uid == 0L) {
				continue;
			}
			List<Map<String, Object>> crmTags = tagsByUserId.get(uid);
			if (crmTags == null || crmTags.isEmpty()) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> tagList = (List<Map<String, Object>>) row.get("tagList");
			if (tagList == null) {
				tagList = new ArrayList<>();
				row.put("tagList", tagList);
			}
			tagList.addAll(crmTags);
		}
	}

	private JsonNode callGetMemberListApi(String mobilesCommaSeparated) throws Exception {
		String apiName = "getMemberList";
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("mobiles", mobilesCommaSeparated);
		data.put("source", "custom_source1");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("method", "MD5");
		params.put("app_key", crmAppKey);
		params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
		params.put("version", "1.0");
		params.put("data", objectMapper.writeValueAsString(data));
		params.put("sign", genSign(params, crmAppSecret));
		String url = crmUrl.endsWith("/") ? crmUrl + apiName : crmUrl + "/" + apiName;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
		ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
		String body = resp.getBody();
		if (!StringUtils.hasText(body)) {
			return null;
		}
		return objectMapper.readTree(body);
	}

	private static void appendTags(List<Map<String, Object>> out, JsonNode arr) {
		if (!arr.isArray()) {
			return;
		}
		for (JsonNode n : arr) {
			if (!n.isObject()) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			n.fields().forEachRemaining(e -> m.put(e.getKey(), objectValue(e.getValue())));
			out.add(m);
		}
	}

	private static Object objectValue(JsonNode n) {
		if (n == null || n.isNull()) {
			return null;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isIntegralNumber()) {
			return n.longValue();
		}
		if (n.isFloatingPointNumber()) {
			return n.doubleValue();
		}
		return n.asText();
	}

	private static String genSign(Map<String, Object> params, String secret) {
		return md5Hex(secret + assembleSorted(params) + secret).toUpperCase();
	}

	private static String assembleSorted(Map<String, Object> params) {
		TreeMap<String, Object> sorted = new TreeMap<>(String::compareTo);
		for (Map.Entry<String, Object> e : params.entrySet()) {
			if ("sign".equals(e.getKey())) {
				continue;
			}
			sorted.put(e.getKey(), e.getValue());
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object v = e.getValue();
			sb.append(e.getKey());
			if (v instanceof Map<?, ?> m) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cast = (Map<String, Object>) m;
				sb.append(assembleSorted(cast));
			} else {
				sb.append(v == null ? "" : String.valueOf(v));
			}
		}
		return sb.toString();
	}

	private static String md5Hex(String s) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
