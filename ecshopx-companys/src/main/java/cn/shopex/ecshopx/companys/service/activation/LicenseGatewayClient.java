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

package cn.shopex.ecshopx.companys.service.activation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
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
public class LicenseGatewayClient {

	private static final int CONNECT_TIMEOUT_SEC = 30;
	private static final int READ_TIMEOUT_SEC = 120;

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public LicenseGatewayClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(CONNECT_TIMEOUT_SEC).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(READ_TIMEOUT_SEC).toMillis());
		this.restTemplate = new RestTemplate(factory);
	}

	/**
	 * POST application/x-www-form-urlencoded; returns parsed JSON or null on transport / empty body.
	 */
	public JsonNode postFormUrlEncoded(String url, Map<String, String> formFields) {
		if (url == null || url.isBlank()) {
			return null;
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (Map.Entry<String, String> e : formFields.entrySet()) {
			if (e.getValue() != null) {
				form.add(e.getKey(), e.getValue());
			}
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		try {
			ResponseEntity<String> resp =
					restTemplate.postForEntity(url, entity, String.class);
			String body = resp.getBody();
			if (body == null || body.isBlank()) {
				return null;
			}
			return objectMapper.readTree(body);
		} catch (RestClientException | java.io.IOException e) {
			return null;
		}
	}
}
