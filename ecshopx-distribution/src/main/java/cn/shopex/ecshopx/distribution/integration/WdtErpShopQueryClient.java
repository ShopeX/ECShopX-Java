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
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class WdtErpShopQueryClient {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${ecshopx.wdterp.api-base-url:}")
	private String apiBaseUrl;

	@Value("${ecshopx.wdterp.methods.shop-query:}")
	private String shopQueryMethod;

	public WdtErpShopQueryClient(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void assertShopExistsIfWdtNo(long companyId, Map<String, Object> merged) {
		Object noRaw = merged.get("wdt_shop_no");
		if (noRaw == null || !StringUtils.hasText(noRaw.toString())) {
			return;
		}
		String shopNo = noRaw.toString().trim();
		String raw = companysRedisTemplate.opsForValue().get(SystemLinkRedisJsonReadSupport.wdtRedisKey(companyId));
		Map<String, Object> cfg = SystemLinkRedisJsonReadSupport.parseJsonObject(raw, objectMapper);
		Object open = cfg.get("is_open");
		boolean enabled = open instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(open));
		if (!enabled) {
			throw new ResourceException("旺店通ERP未开启");
		}
		if (!StringUtils.hasText(apiBaseUrl) || !StringUtils.hasText(shopQueryMethod)) {
			throw new ResourceException("旺店通接口未配置");
		}
		String sid = str(cfg.get("sid"));
		String appKey = str(cfg.get("app_key"));
		String appSecret = str(cfg.get("app_secret"));
		if (!StringUtils.hasText(sid) || !StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
			throw new ResourceException("旺店通ERP未配置完整");
		}
		try {
			Map<String, Object> body = new HashMap<>();
			body.put("sid", sid);
			body.put("app_key", appKey);
			body.put("app_secret", appSecret);
			body.put("method", shopQueryMethod);
			Map<String, Object> biz = new HashMap<>();
			biz.put("platform_id", 127);
			biz.put("shop_no", shopNo);
			body.put("biz", biz);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			ResponseEntity<String> resp = restTemplate.postForEntity(
					apiBaseUrl.endsWith("/") ? apiBaseUrl + "api" : apiBaseUrl + "/api",
					new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
					String.class);
			if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
				throw new ResourceException("旺店通店铺查询失败");
			}
			JsonNode root = objectMapper.readTree(resp.getBody());
			JsonNode total = root.path("data").path("total_count");
			if (!total.isNumber() || total.asInt() <= 0) {
				throw new ResourceException("旺店通门店不存在");
			}
			JsonNode details = root.path("data").path("details");
			if (details.isArray() && details.size() > 0) {
				JsonNode first = details.get(0);
				if (first.has("shop_id")) {
					merged.put("wdt_shop_id", first.get("shop_id").asLong());
				}
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("旺店通店铺查询失败");
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
