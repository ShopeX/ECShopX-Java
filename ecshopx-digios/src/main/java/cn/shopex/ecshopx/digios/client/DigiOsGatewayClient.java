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

package cn.shopex.ecshopx.digios.client;

import cn.shopex.ecshopx.digios.config.DigiOsResolvedConfig;
import cn.shopex.ecshopx.digios.service.DigiOsApiLogService;
import cn.shopex.ecshopx.digios.service.DigiOsConfigService;
import cn.shopex.ecshopx.digios.service.DigiOsTokenService;
import cn.shopex.ecshopx.digios.support.DigiOsMessageId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class DigiOsGatewayClient {

	private final DigiOsConfigService configService;
	private final DigiOsTokenService tokenService;
	private final DigiOsApiLogService apiLogService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	/** 生产注入：自建 RestTemplate，避免与上下文中其它 RestTemplate bean 冲突。 */
	@Autowired
	public DigiOsGatewayClient(
			DigiOsConfigService configService,
			DigiOsTokenService tokenService,
			DigiOsApiLogService apiLogService,
			ObjectMapper objectMapper) {
		this(configService, tokenService, apiLogService, objectMapper, defaultRestTemplate());
	}

	/** 单测可注入 Mock RestTemplate。 */
	DigiOsGatewayClient(
			DigiOsConfigService configService,
			DigiOsTokenService tokenService,
			DigiOsApiLogService apiLogService,
			ObjectMapper objectMapper,
			RestTemplate restTemplate) {
		this.configService = configService;
		this.tokenService = tokenService;
		this.apiLogService = apiLogService;
		this.objectMapper = objectMapper;
		this.restTemplate = restTemplate;
	}

	private static RestTemplate defaultRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(30));
		return new RestTemplate(factory);
	}

	public DigiOsGatewayResult put(long companyId, String method, Map<String, Object> payload) {
		return request(companyId, HttpMethod.PUT, method, payload, true);
	}

	public DigiOsGatewayResult post(long companyId, String method, Map<String, Object> payload) {
		return request(companyId, HttpMethod.POST, method, payload, true);
	}

	private DigiOsGatewayResult request(
			long companyId, HttpMethod httpMethod, String method, Map<String, Object> payload, boolean retryOnUnauthorized) {
		DigiOsResolvedConfig cfg = configService.resolve(companyId);
		if (!cfg.enabled()) {
			return new DigiOsGatewayResult(false, null, null, null, "digios disabled");
		}
		if (!StringUtils.hasText(cfg.baseUrl())
				|| !StringUtils.hasText(cfg.clientId())
				|| !StringUtils.hasText(cfg.appSecret())) {
			return new DigiOsGatewayResult(false, null, null, null, "digios config incomplete");
		}

		String accessToken;
		try {
			accessToken = tokenService.getAccessToken(companyId, false).token();
		} catch (Exception e) {
			return new DigiOsGatewayResult(false, null, null, null, e.getMessage());
		}

		String path = configService.resolvePath(method, companyId);
		String url = cfg.baseUrl() + path;
		Map<String, Object> bizParams = payload == null ? Map.of() : payload;

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
		headers.set("X-Auth-Client", cfg.clientId());
		headers.set("X-Auth-Token", accessToken);
		headers.set("X-Message-Id", DigiOsMessageId.newMessageId());

		long started = System.currentTimeMillis();
		Integer httpStatus = null;
		String raw = null;
		String error = null;
		Map<String, Object> body = null;
		try {
			ResponseEntity<String> entity = restTemplate.exchange(
					url, httpMethod, new HttpEntity<>(bizParams, headers), String.class);
			httpStatus = entity.getStatusCode().value();
			raw = entity.getBody();
			body = parseJsonBody(raw);
		} catch (RestClientResponseException e) {
			// RestTemplate 对 4xx/5xx 抛异常；仍落 http_status + response_body，便于 digios_api_log 排查
			httpStatus = e.getStatusCode().value();
			raw = e.getResponseBodyAsString();
			body = parseJsonBody(raw);
			if (body == null && !StringUtils.hasText(raw)) {
				error = e.getMessage();
			}
		} catch (Exception e) {
			error = e.getMessage();
		}

		int runtimeMs = (int) (System.currentTimeMillis() - started);
		String gatewayError = DigiOsGatewayResponse.resolveError(body);
		boolean ok = error == null && DigiOsGatewayResponse.isSuccess(httpStatus, body);
		if (!ok && error == null && gatewayError != null) {
			error = gatewayError;
		}

		if (retryOnUnauthorized && DigiOsGatewayResponse.isTokenUnauthorized(httpStatus, body)) {
			tokenService.clearCache(companyId);
			apiLogService.writeOutbound(
					companyId,
					method,
					httpStatus,
					"fail",
					runtimeMs,
					bizParams,
					body != null ? body : raw,
					"token unauthorized, refresh and retry");
			return request(companyId, httpMethod, method, payload, false);
		}

		apiLogService.writeOutbound(
				companyId,
				method,
				httpStatus,
				ok ? "success" : "fail",
				runtimeMs,
				bizParams,
				body != null ? body : raw,
				error != null ? error : gatewayError);

		return new DigiOsGatewayResult(ok, httpStatus, body, raw, error);
	}

	private Map<String, Object> parseJsonBody(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception ignored) {
			return null;
		}
	}
}
