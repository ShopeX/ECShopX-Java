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
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReAddOrderPort;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaCompanyBindingPort;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("dadaLocalDeliveryReAddOrderHttp")
@ConditionalOnProperty(prefix = "ecshopx.thirdparty.dada", name = "http-enabled", havingValue = "true")
public class DadaLocalDeliveryReAddOrderHttpService implements DadaLocalDeliveryReAddOrderPort {

	private final DadaOpenPlatformProperties properties;
	private final LocalDeliveryDadaCompanyBindingPort localDeliveryDadaCompanyBindingPort;
	private final DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient;
	private final ObjectMapper objectMapper;

	public DadaLocalDeliveryReAddOrderHttpService(
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
	public void reAddOrder(long companyId, long orderId) {
		String sourceId = resolveSourceIdForRequest(companyId);
		Map<String, String> body = new LinkedHashMap<>();
		body.put("order_id", String.valueOf(orderId));
		String inner;
		try {
			inner = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			throw new ResourceException("达达重新加单参数构造失败");
		}
		JsonNode root;
		try {
			root =
					dadaOpenPlatformJsonApiClient.postJsonBody(
							properties, sourceId, "/api/order/reAddOrder", inner);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("达达重新加单请求异常: " + e.getMessage());
		}
		JsonNode statusNode = root.get("status");
		String status = statusNode == null || !statusNode.isTextual() ? "" : statusNode.asText("");
		if (!"success".equals(status)) {
			JsonNode msgNode = root.get("msg");
			String msg =
					msgNode == null || !msgNode.isTextual() || msgNode.asText("").isBlank()
							? "达达重新加单失败"
							: msgNode.asText("");
			throw new ResourceException(msg);
		}
	}

	private String resolveSourceIdForRequest(long companyId) {
		if (!properties.isOnline()) {
			return properties.getSandboxSourceId() == null ? "" : properties.getSandboxSourceId().trim();
		}
		String sid = localDeliveryDadaCompanyBindingPort.sourceIdForCompany(companyId);
		if (!StringUtils.hasText(sid)) {
			throw new ResourceException("达达商户 source_id 未配置");
		}
		return sid.trim();
	}
}
