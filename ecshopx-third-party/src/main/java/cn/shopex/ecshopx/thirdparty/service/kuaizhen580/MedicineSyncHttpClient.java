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

package cn.shopex.ecshopx.thirdparty.service.kuaizhen580;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.domain.CompanyRelKuaizhen;
import cn.shopex.ecshopx.thirdparty.mapper.CompanyRelKuaizhenMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class MedicineSyncHttpClient {

	private static final Logger log = LoggerFactory.getLogger(MedicineSyncHttpClient.class);

	private static final String MEDICINE_SYNC_PATH = "/v1_0/ehospital/openapi/kz/medicine/sync";
	private static final String CALLBACK_PATH = "/third/kuaizhen/medicineAuditResult";

	private final CompanyRelKuaizhenMapper companyRelKuaizhenMapper;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${ecshopx.api.public-base-url:}")
	private String publicApiBaseUrl;

	public MedicineSyncHttpClient(CompanyRelKuaizhenMapper companyRelKuaizhenMapper, ObjectMapper objectMapper) {
		this.companyRelKuaizhenMapper = companyRelKuaizhenMapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * 调用快诊 580 同步药品接口；失败仅抛出 {@link ResourceException}。
	 */
	public void medicineSync(long companyId, List<MedicineSyncMedicineItem> medicines) {
		if (medicines == null || medicines.isEmpty()) {
			throw new ResourceException("快诊580药品同步失败");
		}
		CompanyRelKuaizhen rel = loadConfigOrThrow(companyId);
		String host = Boolean.TRUE.equals(rel.getOnline())
				? "https://ehospital-openapi.sq580.com"
				: "https://ehospital-openapi-test.sq580.com";
		String url = host + MEDICINE_SYNC_PATH;

		String callbackUrl = buildCallbackUrl();
		List<Map<String, Object>> medicineList = new ArrayList<>();
		for (MedicineSyncMedicineItem it : medicines) {
			medicineList.add(objectMapper.convertValue(it, new TypeReference<Map<String, Object>>() {}));
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("medicineList", medicineList);
		body.put("callbackUrl", callbackUrl);
		body.put("clientId", rel.getClientId());
		body.put("timeStamp", System.currentTimeMillis());

		String sign = Kuaizhen580OpenApiSigner.signPayload(body, rel.getClientSecret());
		body.put("sign", sign);

		String rawResponse;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
				throw new ResourceException("快诊580药品同步请求失败: HTTP " + resp.getStatusCode().value());
			}
			rawResponse = resp.getBody();
		} catch (ResourceException e) {
			throw e;
		} catch (RestClientException e) {
			String hint = e.getMessage() != null ? truncate(e.getMessage(), 200) : "网络异常";
			throw new ResourceException("快诊580药品同步请求失败: " + hint);
		} catch (Exception e) {
			String hint = e.getMessage() != null ? truncate(e.getMessage(), 200) : e.getClass().getSimpleName();
			throw new ResourceException("快诊580药品同步请求失败: " + hint);
		}

		parse580ResponseOrThrow(rawResponse);
		log.debug("medicineSync ok companyId={} bodySnippet={}", companyId, truncate(rawResponse, 500));
	}

	private CompanyRelKuaizhen loadConfigOrThrow(long companyId) {
		LambdaQueryWrapper<CompanyRelKuaizhen> w = new LambdaQueryWrapper<>();
		w.eq(CompanyRelKuaizhen::getCompanyId, companyId).last("LIMIT 1");
		CompanyRelKuaizhen rel = companyRelKuaizhenMapper.selectOne(w);
		if (rel == null) {
			throw new ResourceException("快诊580未配置或已禁用");
		}
		if (Boolean.FALSE.equals(rel.getIsOpen())) {
			throw new ResourceException("快诊580未配置或已禁用");
		}
		if (!StringUtils.hasText(rel.getClientId()) || !StringUtils.hasText(rel.getClientSecret())) {
			throw new ResourceException("快诊580未配置或已禁用");
		}
		return rel;
	}

	private String buildCallbackUrl() {
		String base = publicApiBaseUrl != null ? publicApiBaseUrl.trim() : "";
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		if (!base.isEmpty() && !CALLBACK_PATH.startsWith("/")) {
			return base + "/" + CALLBACK_PATH;
		}
		return base + CALLBACK_PATH;
	}

	private void parse580ResponseOrThrow(String rawResponse) {
		if (!StringUtils.hasText(rawResponse)) {
			throw new ResourceException("快诊580药品同步请求失败: 空响应");
		}
		try {
			JsonNode root = objectMapper.readTree(rawResponse);
			int err = root.path("err").asInt(-1);
			if (err == 0) {
				return;
			}
			String errmsg = root.path("errmsg").asText("");
			throw new ResourceException(StringUtils.hasText(errmsg) ? errmsg : "快诊580药品同步失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("快诊580药品同步失败");
		}
	}

	private static String truncate(String s, int max) {
		if (s == null || s.length() <= max) {
			return s;
		}
		return s.substring(0, max);
	}
}
