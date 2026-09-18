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
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaRechargePort;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service("dadaLocalDeliveryRechargeHttp")
@ConditionalOnProperty(prefix = "ecshopx.thirdparty.dada", name = "http-enabled", havingValue = "true")
public class DadaLocalDeliveryRechargeHttpService implements LocalDeliveryDadaRechargePort {

	private final DadaOpenPlatformProperties properties;
	private final LocalDeliveryDadaCompanyBindingPort localDeliveryDadaCompanyBindingPort;
	private final DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient;
	private final ObjectMapper objectMapper;

	public DadaLocalDeliveryRechargeHttpService(
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
	public String recharge(long companyId, String amount, String notifyUrl) {
		String sourceId = localDeliveryDadaCompanyBindingPort.sourceIdForCompany(companyId);
		sourceId = sourceId == null ? "" : sourceId.trim();

		String bodyJson;
		try {
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("amount", amount);
			inner.put("category", "PC");
			inner.put("notify_url", notifyUrl);
			bodyJson = objectMapper.writeValueAsString(inner);
		} catch (Exception e) {
			throw new ResourceException("达达充值参数构造失败: " + e.getMessage());
		}

		String apiPath = "/api/recharge";
		log.info("dada recharge request apiPath={} companyId={}", apiPath, companyId);
		JsonNode root =
				dadaOpenPlatformJsonApiClient.postJsonBody(
						properties, sourceId, apiPath, bodyJson, false);
		log.info("dada recharge response companyId={} parsed", companyId);

		JsonNode statusNode = root.get("status");
		String status = statusNode == null || !statusNode.isTextual() ? "" : statusNode.asText("");
		if (!"success".equals(status)) {
			JsonNode msgNode = root.get("msg");
			String msg =
					msgNode == null || !msgNode.isTextual() || msgNode.asText("").isBlank()
							? "接口请求超时或失败"
							: msgNode.asText("");
			throw new ResourceException(msg);
		}
		JsonNode result = root.get("result");
		if (result == null || result.isNull() || !result.isTextual()) {
			throw new ResourceException("达达充值返回结果非文本");
		}
		return result.asText();
	}
}
