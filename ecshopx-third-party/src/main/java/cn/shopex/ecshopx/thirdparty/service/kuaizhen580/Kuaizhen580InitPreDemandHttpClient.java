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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class Kuaizhen580InitPreDemandHttpClient {

	private static final String INIT_PRE_DEMAND_PATH =
			"/v1_0/ehospital/openapi/kz/web/predemand/initPreDemand";

	private final CompanyRelKuaizhenMapper companyRelKuaizhenMapper;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	public Kuaizhen580InitPreDemandHttpClient(
			CompanyRelKuaizhenMapper companyRelKuaizhenMapper, ObjectMapper objectMapper) {
		this.companyRelKuaizhenMapper = companyRelKuaizhenMapper;
		this.objectMapper = objectMapper;
	}

	public String initPreDemand(long companyId, Map<String, Object> requestParamsSnake) {
		CompanyRelKuaizhen rel = loadConfigOrThrow(companyId);
		String host = Boolean.TRUE.equals(rel.getOnline())
				? "https://ehospital-openapi.sq580.com"
				: "https://ehospital-openapi-test.sq580.com";
		String url = host + INIT_PRE_DEMAND_PATH;

		ObjectMapper writeMapper =
				objectMapper
						.copy()
						.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

		Map<String, Object> business = new LinkedHashMap<>();
		business.put("memberId", requestParamsSnake.get("order_id"));
		business.put("openid", String.valueOf(requestParamsSnake.getOrDefault("openid", "")));
		business.put("headimgurl", String.valueOf(requestParamsSnake.getOrDefault("headimgurl", "")));
		business.put("storeId", requestParamsSnake.get("store_id"));
		business.put("serviceType", requestParamsSnake.get("service_type"));
		business.put("isExamine", requestParamsSnake.get("is_examine"));
		business.put("isPregnantWoman", requestParamsSnake.get("is_pregnant_woman"));
		business.put("isLactation", requestParamsSnake.get("is_lactation"));
		business.put("souceFrom", requestParamsSnake.get("source_from"));
		business.put("userFamilyName", requestParamsSnake.get("user_family_name"));
		business.put("userFamilyIdCard", requestParamsSnake.get("user_family_id_card"));
		business.put("userFamilyAge", requestParamsSnake.get("user_family_age"));
		business.put("userFamilyGender", requestParamsSnake.get("user_family_gender"));
		business.put("userFamilyPhone", requestParamsSnake.get("user_family_phone"));
		business.put("relationship", requestParamsSnake.get("relationship"));
		business.put("bizOrderId", requestParamsSnake.get("order_id"));

		List<Map<String, Object>> beforeAiDataList = new ArrayList<>();
		Map<String, Object> q1 = new LinkedHashMap<>();
		q1.put("subjectId", 1);
		q1.put("answer", String.valueOf(requestParamsSnake.get("before_ai_result_symptom")));
		beforeAiDataList.add(q1);

		Map<String, Object> q2 = new LinkedHashMap<>();
		q2.put("subjectId", 2);
		q2.put("answer", "");
		Object meds = requestParamsSnake.get("before_ai_result_medicines");
		String medicinesJson;
		if (meds instanceof List<?> list) {
			try {
				medicinesJson = writeMapper.writeValueAsString(list);
			} catch (JsonProcessingException e) {
				throw new ResourceException("问诊单创建失败");
			}
		} else {
			medicinesJson = String.valueOf(meds);
		}
		q2.put("answerMedicine", medicinesJson);
		beforeAiDataList.add(q2);

		Map<String, Object> q3 = new LinkedHashMap<>();
		q3.put("subjectId", 3);
		q3.put("answer", requestParamsSnake.get("before_ai_result_used_medicine"));
		beforeAiDataList.add(q3);

		Map<String, Object> q4 = new LinkedHashMap<>();
		q4.put("subjectId", 4);
		q4.put("answer", requestParamsSnake.get("before_ai_result_allergy_history"));
		beforeAiDataList.add(q4);

		Map<String, Object> q5 = new LinkedHashMap<>();
		q5.put("subjectId", 5);
		q5.put("answer", requestParamsSnake.get("before_ai_result_body_abnormal"));
		beforeAiDataList.add(q5);

		business.put("beforeAiDataList", beforeAiDataList);
		business.put("userFamilyAddr", requestParamsSnake.getOrDefault("user_family_addr", ""));
		business.put("imgList", requestParamsSnake.getOrDefault("img_list", ""));
		business.put("thirdReturnUrl", requestParamsSnake.getOrDefault("third_return_url", ""));

		Map<String, Object> body = new LinkedHashMap<>(business);
		body.put("clientId", rel.getClientId());
		body.put("timeStamp", System.currentTimeMillis());
		String sign = Kuaizhen580OpenApiSigner.signPayload(body, rel.getClientSecret());
		body.put("sign", sign);

		String rawResponse;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			String jsonBody = objectMapper.writeValueAsString(body);
			HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
				throw new ResourceException("问诊单创建请求失败: HTTP " + resp.getStatusCode().value());
			}
			rawResponse = resp.getBody();
		} catch (ResourceException e) {
			throw e;
		} catch (RestClientException e) {
			String hint = e.getMessage() != null ? truncate(e.getMessage(), 200) : "网络异常";
			throw new ResourceException("问诊单创建请求失败: " + hint);
		} catch (JsonProcessingException e) {
			throw new ResourceException("问诊单创建失败");
		} catch (Exception e) {
			String hint = e.getMessage() != null ? truncate(e.getMessage(), 200) : e.getClass().getSimpleName();
			throw new ResourceException("问诊单创建请求失败: " + hint);
		}

		if (!StringUtils.hasText(rawResponse)) {
			throw new ResourceException("问诊单创建请求失败: 空响应");
		}
		try {
			JsonNode root = objectMapper.readTree(rawResponse);
			int err = root.path("err").asInt(-1);
			if (err != 0) {
				String errmsg = root.path("errmsg").asText("");
				throw new ResourceException(StringUtils.hasText(errmsg) ? errmsg : "问诊单创建失败");
			}
			JsonNode data = root.get("data");
			if (data != null && data.isTextual()) {
				return data.asText();
			}
			return "";
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("问诊单创建失败");
		}
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

	private static String truncate(String s, int max) {
		if (s == null || s.length() <= max) {
			return s;
		}
		return s.substring(0, max);
	}
}
