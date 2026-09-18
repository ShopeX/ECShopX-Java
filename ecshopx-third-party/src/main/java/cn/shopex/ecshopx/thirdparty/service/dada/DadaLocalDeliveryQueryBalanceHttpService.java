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

package cn.shopex.ecshopx.thirdparty.service.dada;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaCompanyBindingPort;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaQueryBalancePort;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service("dadaLocalDeliveryQueryBalanceHttp")
@ConditionalOnProperty(prefix = "ecshopx.thirdparty.dada", name = "http-enabled", havingValue = "true")
public class DadaLocalDeliveryQueryBalanceHttpService implements LocalDeliveryDadaQueryBalancePort {

	private final DadaOpenPlatformProperties properties;
	private final LocalDeliveryDadaCompanyBindingPort localDeliveryDadaCompanyBindingPort;
	private final DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient;
	private final ObjectMapper objectMapper;

	public DadaLocalDeliveryQueryBalanceHttpService(
			DadaOpenPlatformProperties properties,
			LocalDeliveryDadaCompanyBindingPort localDeliveryDadaCompanyBindingPort,
			DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient,
			ObjectMapper objectMapper) {
		this.properties = properties;
		this.localDeliveryDadaCompanyBindingPort = localDeliveryDadaCompanyBindingPort;
		this.dadaOpenPlatformJsonApiClient = dadaOpenPlatformJsonApiClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> queryBalance(long companyId) {
		String sourceId = localDeliveryDadaCompanyBindingPort.sourceIdForCompany(companyId);
		sourceId = sourceId == null ? "" : sourceId.trim();

		String bodyJson;
		try {
			bodyJson = objectMapper.writeValueAsString(Map.of("category", "3"));
		} catch (Exception e) {
			throw new ResourceException("接口请求超时或失败");
		}

		String apiPath = "/api/balance/query";
		log.info("dada queryBalance request apiPath={} companyId={}", apiPath, companyId);

		JsonNode root;
		try {
			root =
					dadaOpenPlatformJsonApiClient.postJsonBody(
							properties, sourceId, apiPath, bodyJson, false);
		} catch (ResourceException e) {
			String m = e.getMessage();
			if ("达达接口无响应".equals(m) || "达达接口请求失败".equals(m)) {
				throw new ResourceException("接口请求超时或失败");
			}
			throw e;
		} catch (Exception e) {
			throw new ResourceException("接口请求超时或失败");
		}

		if (root == null || !root.isObject()) {
			throw new ResourceException("接口请求超时或失败");
		}

		JsonNode statusNode = root.get("status");
		if (statusNode == null || statusNode.isNull()) {
			throw resourceExceptionFromRootMsg(root);
		}
		if (!statusNode.isTextual()) {
			throw resourceExceptionFromRootMsg(root);
		}
		String statusText = statusNode.asText("");
		if (!"success".equalsIgnoreCase(statusText.trim())) {
			JsonNode msgNode = root.get("msg");
			String msg =
					msgNode == null || !msgNode.isTextual() || msgNode.asText("").isBlank()
							? "接口请求超时或失败"
							: msgNode.asText("");
			throw new ResourceException(msg);
		}

		JsonNode resultNode = root.get("result");
		if (resultNode == null || resultNode.isNull()) {
			log.info("dada queryBalance response companyId={} emptyResult", companyId);
			return new LinkedHashMap<>();
		}
		if (resultNode.isObject()) {
			Map<String, Object> out =
					objectMapper.convertValue(
							resultNode, new TypeReference<LinkedHashMap<String, Object>>() {});
			log.info("dada queryBalance response companyId={} parsed", companyId);
			return out;
		}
		throw new ResourceException("达达余额返回格式异常");
	}

	private static ResourceException resourceExceptionFromRootMsg(JsonNode root) {
		JsonNode msgNode = root.get("msg");
		String msg =
				msgNode != null && msgNode.isTextual() && !msgNode.asText("").isBlank()
						? msgNode.asText("")
						: "接口请求超时或失败";
		return new ResourceException(msg);
	}
}
