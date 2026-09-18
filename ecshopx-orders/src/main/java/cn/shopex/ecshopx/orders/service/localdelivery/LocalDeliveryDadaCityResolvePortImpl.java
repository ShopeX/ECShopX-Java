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

package cn.shopex.ecshopx.orders.service.localdelivery;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaCityResolvePort;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaCompanyBindingPort;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import cn.shopex.ecshopx.thirdparty.service.dada.DadaOpenPlatformJsonApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LocalDeliveryDadaCityResolvePortImpl implements LocalDeliveryDadaCityResolvePort {

	private static final TypeReference<List<Map<String, String>>> CITY_LIST_TYPE =
			new TypeReference<>() {};

	private final LocalDeliveryDadaCompanyBindingPort companyBindingPort;
	private final DadaOpenPlatformProperties dadaOpenPlatformProperties;
	private final DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient;
	private final ObjectMapper objectMapper;

	private volatile List<Map<String, String>> cachedLocalList;

	public LocalDeliveryDadaCityResolvePortImpl(
			LocalDeliveryDadaCompanyBindingPort companyBindingPort,
			DadaOpenPlatformProperties dadaOpenPlatformProperties,
			DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient,
			ObjectMapper objectMapper) {
		this.companyBindingPort = companyBindingPort;
		this.dadaOpenPlatformProperties = dadaOpenPlatformProperties;
		this.dadaOpenPlatformJsonApiClient = dadaOpenPlatformJsonApiClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public String resolveCityCode(long companyId, String receiverCity) {
		List<Map<String, String>> cityList = loadCityList(companyId);
		if (cityList == null || cityList.isEmpty()) {
			throw new ResourceException("当前城市不支持同城配，请重新选择收货地址");
		}
		String cityName = receiverCity == null ? "" : receiverCity.trim();
		if (cityName.endsWith("市")) {
			cityName = cityName.substring(0, cityName.length() - 1);
		}
		String cityCode = "";
		for (Map<String, String> city : cityList) {
			String cn = city.get("cityName");
			if (cn != null && cityName.equals(cn)) {
				cityCode = city.getOrDefault("cityCode", "");
				break;
			}
		}
		if (cityCode.isEmpty()) {
			throw new ResourceException("当前城市不支持同城配，请重新选择收货地址");
		}
		return cityCode;
	}

	private List<Map<String, String>> loadCityList(long companyId) {
		if (!dadaOpenPlatformProperties.isOnline()) {
			return readLocalCityList();
		}
		String sid = companyBindingPort.sourceIdForCompany(companyId);
		if (!StringUtils.hasText(sid)) {
			return readLocalCityList();
		}
		try {
			JsonNode root =
					dadaOpenPlatformJsonApiClient.postJsonBody(
							dadaOpenPlatformProperties, sid, "/api/cityCode/list", "");
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
			JsonNode result = root.get("result");
			if (result == null || !result.isArray()) {
				return List.of();
			}
			List<Map<String, String>> out = new ArrayList<>();
			for (JsonNode n : result) {
				Map<String, String> row = new LinkedHashMap<>();
				JsonNode name = n.get("cityName");
				JsonNode code = n.get("cityCode");
				row.put("cityName", name == null ? "" : name.asText(""));
				row.put("cityCode", code == null ? "" : code.asText(""));
				out.add(row);
			}
			return out;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("达达城市列表获取异常: " + e.getMessage());
		}
	}

	private List<Map<String, String>> readLocalCityList() {
		if (cachedLocalList != null) {
			return cachedLocalList;
		}
		synchronized (this) {
			if (cachedLocalList != null) {
				return cachedLocalList;
			}
			try {
				ClassPathResource res = new ClassPathResource("localdelivery/dada-local-cities.json");
				try (InputStream in = res.getInputStream()) {
					cachedLocalList = objectMapper.readValue(in, CITY_LIST_TYPE);
				}
			} catch (Exception e) {
				cachedLocalList = List.of();
			}
			return cachedLocalList;
		}
	}
}
