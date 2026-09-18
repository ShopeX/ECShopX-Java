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

package cn.shopex.ecshopx.shopexai.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OutfitAnyoneGenerationService {

	private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(300);

	private final RestTemplate restTemplate;
	private final String remoteUrl;

	public OutfitAnyoneGenerationService(@Value("${shopexai.outfit.remote-url:}") String remoteUrl) {
		this.remoteUrl = remoteUrl == null ? "" : remoteUrl.trim();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(HTTP_TIMEOUT);
		factory.setReadTimeout(HTTP_TIMEOUT);
		this.restTemplate = new RestTemplate(factory);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> generateOutfit(
			String personImageUrl, String topGarmentUrl, String bottomGarmentUrl) {
		if (remoteUrl.isEmpty()) {
			throw new IllegalStateException("shopexai.outfit.remote-url is not configured");
		}
		Map<String, Object> req = new LinkedHashMap<>();
		req.put("person_image_url", personImageUrl);
		req.put("top_garment_url", topGarmentUrl);
		req.put("bottom_garment_url", bottomGarmentUrl == null ? "" : bottomGarmentUrl);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(remoteUrl, entity, Map.class);
		Map<?, ?> raw = response.getBody();
		if (raw == null) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			if (e.getKey() != null) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}
}
