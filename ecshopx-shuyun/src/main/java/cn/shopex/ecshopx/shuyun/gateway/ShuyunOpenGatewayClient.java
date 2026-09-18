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

package cn.shopex.ecshopx.shuyun.gateway;

import cn.shopex.ecshopx.shuyun.auth.CallbackSignatureVerifier;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.service.openplatform.OpenPlatformConfigService;
import cn.shopex.ecshopx.shuyun.service.openplatform.TrafficAuditWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 数云开放网关出站 Client。对齐 PHP {@code ShuyunGatewayClient} + {@code ShuyunSigner}。
 * DELTA-001：fallback token 默认关闭。
 */
@Service
public class ShuyunOpenGatewayClient {

	private static final Logger log = LoggerFactory.getLogger(ShuyunOpenGatewayClient.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenPlatformProperties properties;
	private final CallbackSignatureVerifier signatureVerifier;
	private final TrafficAuditWriter trafficAuditWriter;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public ShuyunOpenGatewayClient(
			OpenPlatformConfigService openPlatformConfigService,
			ShuyunOpenPlatformProperties properties,
			CallbackSignatureVerifier signatureVerifier,
			TrafficAuditWriter trafficAuditWriter,
			ObjectMapper objectMapper) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.properties = properties;
		this.signatureVerifier = signatureVerifier;
		this.trafficAuditWriter = trafficAuditWriter;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		int timeoutMs = (int) (properties.getTimeoutSeconds() * 1000);
		factory.setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 10_000)));
		factory.setReadTimeout(Duration.ofMillis(timeoutMs));
		this.restTemplate = new RestTemplate(factory);
	}

	public JsonNode postJson(long companyId, String actionMethod, Object body, String platform) {
		return exchange(companyId, HttpMethod.POST, actionMethod, body, Map.of(), platform);
	}

	public JsonNode putJson(long companyId, String actionMethod, Object body, String platform) {
		return exchange(companyId, HttpMethod.PUT, actionMethod, body, Map.of(), platform);
	}

	public JsonNode getQuery(long companyId, String actionMethod, Map<String, ?> query, String platform) {
		Map<String, Object> q = query == null ? Map.of() : new LinkedHashMap<>(query);
		return exchange(companyId, HttpMethod.GET, actionMethod, null, q, platform);
	}

	private JsonNode exchange(
			long companyId,
			HttpMethod method,
			String actionMethod,
			Object body,
			Map<String, ?> query,
			String platform) {
		CompanyShuyunOpenPlatformConfig config = openPlatformConfigService.findByCompanyId(companyId);
		if (config == null
				|| !StringUtils.hasText(config.getAppId())
				|| !StringUtils.hasText(config.getAppSecret())) {
			throw new IllegalStateException("Shuyun open platform config missing for company " + companyId);
		}
		String accessToken = resolveEffectiveAccessToken(config.getAccessToken());
		Map<String, String> signParams = new LinkedHashMap<>();
		for (Map.Entry<String, ?> e : query.entrySet()) {
			if ("Gateway-Request-Time".equalsIgnoreCase(e.getKey())) {
				continue;
			}
			signParams.put(e.getKey(), scalarToGatewaySignString(e.getValue()));
		}
		String requestTime = String.valueOf(System.currentTimeMillis());
		signParams.put("Gateway-Request-Time", requestTime);
		String sign = signatureVerifier.signParams(config.getAppSecret(), signParams);

		HttpHeaders headers = new HttpHeaders();
		headers.set("Gateway-Authid", config.getAppId());
		headers.set("Gateway-Sign", sign);
		headers.set("Gateway-Action-Method", actionMethod);
		headers.set("Gateway-Request-Time", requestTime);
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (StringUtils.hasText(accessToken)) {
			headers.set("Gateway-Access-Token", accessToken);
		}
		if (StringUtils.hasText(platform)) {
			headers.set("platform", platform.trim().toLowerCase());
		}

		String base = properties.getBaseUri() == null ? "" : properties.getBaseUri().replaceAll("/+$", "");
		UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(base + "/");
		for (Map.Entry<String, ?> e : query.entrySet()) {
			ub.queryParam(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
		}
		URI uri = ub.build(true).toUri();
		String callId = "gw_" + UUID.randomUUID().toString().replace("-", "");
		String reqBody;
		try {
			reqBody = body == null ? null : objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to encode gateway JSON body", e);
		}

		try {
			HttpEntity<String> entity = new HttpEntity<>(reqBody, headers);
			ResponseEntity<String> resp =
					restTemplate.exchange(uri, method, entity, String.class);
			String respBody = resp.getBody();
			int status = resp.getStatusCode().value();
			boolean ok = status >= 200 && status < 300;
			try {
				trafficAuditWriter.writeOutbound(
						companyId,
						callId,
						method.name(),
						actionMethod,
						"{}",
						reqBody,
						respBody,
						status,
						ok ? "success" : "business_error",
						ok ? null : "http_" + status);
			} catch (Exception ignore) {
				// audit best-effort
			}
			if (!ok) {
				throw new IllegalStateException("Shuyun gateway HTTP " + status + " action=" + actionMethod);
			}
			if (!StringUtils.hasText(respBody)) {
				return objectMapper.nullNode();
			}
			return objectMapper.readTree(respBody);
		} catch (RestClientException e) {
			log.error("Shuyun gateway transport error action={} url={} err={}", actionMethod, uri, e.getMessage());
			throw new IllegalStateException("Shuyun gateway request failed: " + e.getMessage(), e);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Shuyun gateway parse failed: " + e.getMessage(), e);
		}
	}

	private String resolveEffectiveAccessToken(String dbToken) {
		if (StringUtils.hasText(dbToken)) {
			return dbToken;
		}
		// DELTA-001：默认关；仅当显式配置非空时才回退
		String fb = properties.getFallbackGatewayAccessToken();
		return StringUtils.hasText(fb) ? fb.trim() : null;
	}

	private static String scalarToGatewaySignString(Object value) {
		if (value == null || Boolean.FALSE.equals(value)) {
			return "";
		}
		if (Boolean.TRUE.equals(value)) {
			return "1";
		}
		return String.valueOf(value);
	}
}
