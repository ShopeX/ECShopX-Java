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

import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 有数 analysis add_wxapp_visit_distribution；对齐 PHP {@code Analysis\Client::addWxappVisitDistribution}。
 */
@Service
public class YoushuAnalysisAddWxappVisitDistributionClient
		implements YoushuAnalysisAddWxappVisitDistributionPort {

	private static final RestTemplate REST = new RestTemplate();

	private final ObjectMapper objectMapper;

	public YoushuAnalysisAddWxappVisitDistributionClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void addWxappVisitDistribution(
			String dataSourceId, JsonNode visitData, YoushuOpenApiCredentials credentials) {
		String base = normalizeBase(credentials.baseUri());
		String appId = credentials.appId();
		String appSecret = credentials.appSecret();
		Map<String, String> sig = YoushuOpenApiSignSupport.buildSignedQueryParams(appId, appSecret);

		ObjectNode body = objectMapper.createObjectNode();
		body.put("dataSourceId", dataSourceId);
		body.set("rawMsg", visitData == null ? objectMapper.getNodeFactory().nullNode() : visitData);
		String jsonBody;
		try {
			jsonBody = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		UriComponentsBuilder b =
				UriComponentsBuilder.fromUriString(base + "/data-api/v1/analysis/add_wxapp_visit_distribution");
		sig.forEach(b::queryParam);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
		ResponseEntity<String> resp = REST.postForEntity(b.build(true).toUri(), entity, String.class);
		String raw = resp.getBody();
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			int rc = root.path("retcode").asInt(0);
			if (rc != 0) {
				throw new RuntimeException(
						"有数 add_wxapp_visit_distribution 失败: " + root.path("errmsg").asText("retcode=" + rc));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
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
