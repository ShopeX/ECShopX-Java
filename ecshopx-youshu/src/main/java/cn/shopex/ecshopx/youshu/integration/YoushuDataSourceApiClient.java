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

package cn.shopex.ecshopx.youshu.integration;

import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 有数 data source HTTP；对齐 PHP {@code YoushuBundle\Services\src\DataSource\Client} + Kernel 签名。
 */
@Service
public class YoushuDataSourceApiClient implements YoushuDataSourceApiPort {

	private static final RestTemplate REST = new RestTemplate();

	private final ObjectMapper objectMapper;

	public YoushuDataSourceApiClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	@SuppressWarnings("unused")
	public String getOrCreateDataSourceId(
			String merchantId, int dataSourceType, YoushuOpenApiCredentials credentials) {
		String base = normalizeBase(credentials.baseUri());
		String appId = credentials.appId();
		String appSecret = credentials.appSecret();
		Map<String, String> sig = YoushuOpenApiSignSupport.buildSignedQueryParams(appId, appSecret);

		UriComponentsBuilder getBuilder = UriComponentsBuilder.fromUriString(base + "/data-api/v1/data_source/get");
		sig.forEach(getBuilder::queryParam);
		getBuilder.queryParam("merchantId", merchantId);
		ResponseEntity<String> getResp = REST.getForEntity(getBuilder.build(true).toUri(), String.class);
		JsonNode getRoot = readTreeOrThrow(getResp.getBody());
		if (getRoot.path("retcode").asInt(Integer.MIN_VALUE) != 0) {
			throw new RuntimeException("未查询到腾讯有数对应数据仓库");
		}
		String fromGet = firstNonEmptyId(getRoot.path("data").path("dataSources"), true);
		if (fromGet != null) {
			return fromGet;
		}

		Map<String, Object> addBody = new LinkedHashMap<>();
		addBody.put("merchantId", merchantId);
		addBody.put("multi", true);
		String jsonBody;
		try {
			jsonBody = objectMapper.writeValueAsString(addBody);
		} catch (Exception e) {
			throw new RuntimeException("未查询到腾讯有数对应数据仓库", e);
		}
		UriComponentsBuilder postBuilder = UriComponentsBuilder.fromUriString(base + "/data-api/v1/data_source/add");
		sig.forEach(postBuilder::queryParam);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
		ResponseEntity<String> addResp = REST.postForEntity(postBuilder.build(true).toUri(), entity, String.class);
		JsonNode addRoot = readTreeOrThrow(addResp.getBody());
		if (addRoot.path("retcode").asInt(Integer.MIN_VALUE) != 0) {
			throw new RuntimeException("未查询到腾讯有数对应数据仓库");
		}
		String fromAdd = firstNonEmptyId(addRoot.path("data").path("dataSource"), false);
		if (fromAdd != null) {
			return fromAdd;
		}
		throw new RuntimeException("未查询到腾讯有数对应数据仓库");
	}

	private static String firstNonEmptyId(JsonNode n, boolean arrayFirstElement) {
		if (arrayFirstElement) {
			JsonNode first = n.path(0);
			if (first.isMissingNode() || !first.isObject()) {
				return null;
			}
			String id = first.path("id").asText("");
			return id.isEmpty() ? null : id;
		}
		if (n == null || n.isMissingNode() || !n.isObject()) {
			return null;
		}
		String id = n.path("id").asText("");
		return id.isEmpty() ? null : id;
	}

	private JsonNode readTreeOrThrow(String body) {
		if (body == null || body.isBlank()) {
			throw new RuntimeException("未查询到腾讯有数对应数据仓库");
		}
		try {
			return objectMapper.readTree(body);
		} catch (Exception e) {
			throw new RuntimeException("未查询到腾讯有数对应数据仓库", e);
		}
	}

	private static String normalizeBase(String baseUri) {
		if (baseUri == null) {
			return "";
		}
		String t = baseUri.trim();
		while (t.endsWith("/")) {
			t = t.substring(0, t.length() - 1);
		}
		return t;
	}
}
