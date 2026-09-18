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

package cn.shopex.ecshopx.orders.service.companyreldada;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CompanyRelDada;
import cn.shopex.ecshopx.orders.mapper.CompanyRelDadaMapper;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import cn.shopex.ecshopx.thirdparty.service.dada.DadaOpenPlatformJsonApiClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanyRelDadaAdminInfoService {

	private static final String REDIS_KEY_DADA_CITY_LIST = "dada_city_list";
	private static final Duration DADA_CITY_LIST_TTL = Duration.ofSeconds(86400);

	private final CompanyRelDadaMapper companyRelDadaMapper;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final DadaOpenPlatformProperties dadaOpenPlatformProperties;
	private final DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient;
	private final ObjectMapper objectMapper;

	public CompanyRelDadaAdminInfoService(
			CompanyRelDadaMapper companyRelDadaMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			DadaOpenPlatformProperties dadaOpenPlatformProperties,
			DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient,
			ObjectMapper objectMapper) {
		this.companyRelDadaMapper = companyRelDadaMapper;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.dadaOpenPlatformProperties = dadaOpenPlatformProperties;
		this.dadaOpenPlatformJsonApiClient = dadaOpenPlatformJsonApiClient;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getCompanyRelDadaInfo(long companyId) {
		Map<String, Object> result = new LinkedHashMap<>();
		CompanyRelDada row =
				companyRelDadaMapper.selectOne(
						new LambdaQueryWrapper<CompanyRelDada>()
								.eq(CompanyRelDada::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row != null) {
			result.put("id", row.getId());
			result.put("company_id", row.getCompanyId());
			result.put("source_id", row.getSourceId());
			result.put("enterprise_name", row.getEnterpriseName());
			result.put("enterprise_address", row.getEnterpriseAddress());
			result.put("mobile", row.getMobile());
			result.put("city_name", row.getCityName());
			result.put("contact_name", row.getContactName());
			result.put("contact_phone", row.getContactPhone());
			result.put("email", row.getEmail());
			result.put("freight_type", scalarToOneZeroBitString(row.getFreightType()));
			result.put("created", row.getCreated());
			result.put("updated", row.getUpdated());
			result.put("status", scalarToOneZeroBitString(row.getStatus()));
			result.put("is_open", scalarToOneZeroBitString(row.getIsOpen()));
		}
		try {
			String cached = sharedStringRedisTemplate.opsForValue().get(REDIS_KEY_DADA_CITY_LIST);
			if (StringUtils.hasText(cached)) {
				List<Object> cityList = parseCityListJson(cached);
				result.put("city_list", cityList);
			} else {
				CompanyRelDada rel = row;
				if (rel == null) {
					rel =
							companyRelDadaMapper.selectOne(
									new LambdaQueryWrapper<CompanyRelDada>()
											.eq(CompanyRelDada::getCompanyId, companyId)
											.last("LIMIT 1"));
				}
				if (rel == null || !StringUtils.hasText(rel.getSourceId())) {
					List<Map<String, Object>> local = readLocalCityListFromClasspath();
					result.put("city_list", local);
				} else {
					List<Map<String, Object>> fromApi = fetchCityListFromDadaApi(rel.getSourceId().trim());
					result.put("city_list", fromApi);
				}
			}
		} catch (Exception e) {
			result.put("city_list", Collections.emptyList());
			result.put("is_open", "0");
			result.put("error_message", e.getMessage() == null ? "" : e.getMessage());
		}
		result.put("business_list", new LinkedHashMap<>(DadaShopBusinessList.asMap()));
		return result;
	}

	private static String scalarToOneZeroBitString(Object v) {
		return "true".equals(v == null ? null : String.valueOf(v)) ? "1" : "0";
	}

	private List<Object> parseCityListJson(String json) throws Exception {
		JsonNode root = objectMapper.readTree(json);
		if (root == null || !root.isArray()) {
			throw new IllegalStateException("invalid dada_city_list cache");
		}
		List<Object> out = new ArrayList<>();
		for (JsonNode n : root) {
			Map<String, Object> row = new LinkedHashMap<>();
			JsonNode name = n.get("cityName");
			JsonNode code = n.get("cityCode");
			row.put("cityName", name == null ? "" : name.asText(""));
			row.put("cityCode", code == null ? "" : code.asText(""));
			out.add(row);
		}
		return out;
	}

	private List<Map<String, Object>> readLocalCityListFromClasspath() throws Exception {
		ClassPathResource res = new ClassPathResource("localdelivery/dada-local-cities.json");
		try (InputStream in = res.getInputStream()) {
			List<Map<String, String>> raw =
					objectMapper.readValue(in, new TypeReference<List<Map<String, String>>>() {});
			List<Map<String, Object>> out = new ArrayList<>();
			for (Map<String, String> m : raw) {
				Map<String, Object> o = new LinkedHashMap<>();
				o.put("cityName", m.getOrDefault("cityName", ""));
				o.put("cityCode", m.getOrDefault("cityCode", ""));
				out.add(o);
			}
			return out;
		}
	}

	private List<Map<String, Object>> fetchCityListFromDadaApi(String sourceId) throws Exception {
		JsonNode root =
				dadaOpenPlatformJsonApiClient.postJsonBody(
						dadaOpenPlatformProperties, sourceId, "/api/cityCode/list", "");
		JsonNode statusNode = root.get("status");
		String status = statusNode == null || !statusNode.isTextual() ? "" : statusNode.asText("");
		if (!"success".equals(status)) {
			JsonNode msgNode = root.get("msg");
			String msg =
					msgNode == null || !msgNode.isTextual() || msgNode.asText("").isBlank()
							? "达达城市列表获取失败"
							: msgNode.asText("");
			throw new ResourceException(msg);
		}
		JsonNode resultNode = root.get("result");
		if (resultNode == null || !resultNode.isArray()) {
			return List.of();
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (JsonNode n : resultNode) {
			Map<String, Object> item = new LinkedHashMap<>();
			JsonNode name = n.get("cityName");
			JsonNode code = n.get("cityCode");
			item.put("cityName", name == null ? "" : name.asText(""));
			item.put("cityCode", code == null ? "" : code.asText(""));
			list.add(item);
		}
		String json = objectMapper.writeValueAsString(list);
		sharedStringRedisTemplate.opsForValue().set(REDIS_KEY_DADA_CITY_LIST, json, DADA_CITY_LIST_TTL);
		return list;
	}
}
