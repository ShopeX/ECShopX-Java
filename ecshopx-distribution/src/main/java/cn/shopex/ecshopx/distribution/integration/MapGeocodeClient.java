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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MapGeocodeClient {

	public record LngLat(String lng, String lat) {
	}

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.map.geocode-base-url:https://apis.map.qq.com/ws/geocoder/v1/}")
	private String geocodeBaseUrl;

	@Value("${ecshopx.map.tencent.key:}")
	private String tencentMapKey;

	public MapGeocodeClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public LngLat getLatAndLng(long companyId, String city, String address) {
		if (!StringUtils.hasText(tencentMapKey)) {
			throw new ResourceException("地图服务未配置");
		}
		String addr = StringUtils.hasText(city) ? city + address : address;
		if (!StringUtils.hasText(addr)) {
			throw new ResourceException("地址识别失败");
		}
		try {
			String url = UriComponentsBuilder.fromUriString(geocodeBaseUrl)
					.queryParam("address", addr)
					.queryParam("key", tencentMapKey)
					.build()
					.encode()
					.toUriString();
			String body = restTemplate.getForObject(url, String.class);
			if (body == null) {
				throw new ResourceException("地址识别失败");
			}
			JsonNode root = objectMapper.readTree(body);
			if (root.path("status").asInt() != 0) {
				throw new ResourceException("地址识别失败");
			}
			JsonNode loc = root.path("result").path("location");
			String lng = loc.path("lng").asText("");
			String lat = loc.path("lat").asText("");
			if (!StringUtils.hasText(lng) || !StringUtils.hasText(lat)) {
				throw new ResourceException("地址识别失败");
			}
			return new LngLat(lng, lat);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("地址识别失败");
		}
	}
}
