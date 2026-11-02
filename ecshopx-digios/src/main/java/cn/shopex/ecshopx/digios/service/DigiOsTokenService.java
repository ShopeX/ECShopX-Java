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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.digios.config.DigiOsResolvedConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class DigiOsTokenService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsTokenService.class);

	public record TokenInfo(String token, int expireSeconds, boolean fromCache) {}

	private final DigiOsConfigService configService;
	private final DigiOsApiLogService apiLogService;
	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public DigiOsTokenService(
			DigiOsConfigService configService,
			DigiOsApiLogService apiLogService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.configService = configService;
		this.apiLogService = apiLogService;
		this.redis = redis;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(15));
		this.restTemplate = new RestTemplate(factory);
	}

	public TokenInfo getAccessToken(long companyId, boolean forceRefresh) {
		DigiOsResolvedConfig cfg = configService.resolve(companyId);
		if (!StringUtils.hasText(cfg.clientId()) || !StringUtils.hasText(cfg.appSecret()) || !StringUtils.hasText(cfg.baseUrl())) {
			throw new IllegalStateException("digios token config incomplete (base_url/clientId/appSecret)");
		}
		String cacheKey = cacheKey(companyId, cfg.clientId());
		if (!forceRefresh) {
			String cached = redis.opsForValue().get(cacheKey);
			if (StringUtils.hasText(cached)) {
				return new TokenInfo(cached, 0, true);
			}
		}
		String lockKey = cacheKey + ":lock";
		Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
		if (Boolean.FALSE.equals(locked)) {
			try {
				TimeUnit.MILLISECONDS.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			String cached = redis.opsForValue().get(cacheKey);
			if (StringUtils.hasText(cached)) {
				return new TokenInfo(cached, 0, true);
			}
		}
		try {
			FetchedToken fetched = fetchFromGateway(cfg);
			int ttl = calcCacheTtl(fetched.expireSeconds, cfg.tokenExpireSkewSeconds());
			redis.opsForValue().set(cacheKey, fetched.token, Duration.ofSeconds(ttl));
			return new TokenInfo(fetched.token, fetched.expireSeconds, false);
		} finally {
			try {
				redis.delete(lockKey);
			} catch (Exception ignored) {
				// ignore
			}
		}
	}

	public void clearCache(long companyId) {
		DigiOsResolvedConfig cfg = configService.resolve(companyId);
		if (!StringUtils.hasText(cfg.clientId())) {
			return;
		}
		try {
			redis.delete(cacheKey(companyId, cfg.clientId()));
		} catch (Exception e) {
			log.warn("digios token cache clear failed: {}", e.getMessage());
		}
	}

	public static int calcCacheTtl(int expireSeconds, int skewSeconds) {
		return Math.max(60, expireSeconds - skewSeconds);
	}

	public static String cacheKey(long companyId, String clientId) {
		return "digios:token:" + companyId + ":" + clientId;
	}

	private record FetchedToken(String token, int expireSeconds) {}

	private FetchedToken fetchFromGateway(DigiOsResolvedConfig cfg) {
		String url = cfg.baseUrl() + cfg.tokenPath();
		Map<String, String> body = new LinkedHashMap<>();
		body.put("clientId", cfg.clientId());
		body.put("appSecret", cfg.appSecret());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		long started = System.currentTimeMillis();
		Integer httpStatus = null;
		String error = null;
		JsonNode response = null;
		try {
			ResponseEntity<String> entity =
					restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
			httpStatus = entity.getStatusCode().value();
			response = objectMapper.readTree(entity.getBody());
		} catch (Exception e) {
			error = e.getMessage();
		}
		int runtimeMs = (int) (System.currentTimeMillis() - started);
		if (error != null) {
			apiLogService.writeOutbound(
					cfg.companyId(), "auth.token", httpStatus, "fail", runtimeMs, body, response, error);
			throw new IllegalStateException("digios token fetch failed: " + error);
		}
		JsonNode data = response != null ? response.get("data") : null;
		String token = data != null && data.hasNonNull("token") ? data.get("token").asText() : "";
		int expire = data != null && data.has("expire") ? data.get("expire").asInt(0) : 0;
		if (!StringUtils.hasText(token) || expire <= 0) {
			apiLogService.writeOutbound(
					cfg.companyId(), "auth.token", httpStatus, "fail", runtimeMs, body, response, "invalid token response");
			throw new IllegalStateException("digios token response invalid");
		}
		apiLogService.writeOutbound(cfg.companyId(), "auth.token", httpStatus, "success", runtimeMs, body, response, null);
		return new FetchedToken(token, expire);
	}
}
