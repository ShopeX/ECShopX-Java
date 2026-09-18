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

package cn.shopex.ecshopx.kujiale.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class KujialeH5CitiesClient {

	private static final Logger log = LoggerFactory.getLogger(KujialeH5CitiesClient.class);

	private static final String CITIES_JSON_URL = "https://qhstatic-cos.kujiale.com/openapi/cities.json";

	private final ObjectMapper objectMapper;

	private final RestTemplate restTemplate;

	public KujialeH5CitiesClient(
			ObjectMapper objectMapper,
			@Value("${ecshopx.kujiale.cities-json.insecure-tls:false}") boolean insecureTls) {
		this.objectMapper = objectMapper;
		this.restTemplate = buildRestTemplate(insecureTls);
	}

	private static RestTemplate buildRestTemplate(boolean insecureTls) {
		SimpleClientHttpRequestFactory factory = insecureTls
				? new KujialeH5CitiesInsecureTlsClientHttpRequestFactory()
				: new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(10_000);
		RestTemplate restTemplate = new RestTemplate(factory);
		restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
		return restTemplate;
	}

	public Object fetchCities() {
		try {
			ResponseEntity<String> response =
					restTemplate.exchange(CITIES_JSON_URL, HttpMethod.GET, null, String.class);
			if (!HttpStatus.OK.equals(response.getStatusCode())) {
				throw new ResourceException("获取城市列表失败");
			}
			String body = response.getBody();
			if (body == null) {
				throw new ResourceException("获取城市列表失败");
			}
			try {
				return objectMapper.readValue(body, Object.class);
			} catch (JsonProcessingException e) {
				throw new ResourceException("解析JSON数据失败");
			}
		} catch (RestClientException e) {
			log.error("获取城市列表API失败: {}", e.getMessage());
			throw new ResourceException("获取城市列表失败: " + e.getMessage());
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error("获取城市列表失败: {}", e.getMessage());
			throw new ResourceException("获取城市列表失败");
		}
	}
}
