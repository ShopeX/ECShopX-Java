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

package cn.shopex.ecshopx.thirdparty.service.shopexcrm;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service("shopexCrmSyncSingleOrderHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.shopex-crm.sync-single-order",
		name = "http-enabled",
		havingValue = "true")
public class NormalOrderConfirmReceiptShopexCrmHttpSyncSingleOrderService implements ShopexCrmSyncSingleOrderPort {

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${ecshopx.thirdparty.shopex-crm.sync-single-order.endpoint:}")
	private String endpoint;

	public NormalOrderConfirmReceiptShopexCrmHttpSyncSingleOrderService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void syncSingleOrder(long companyId, Object orderId) {
		if (!StringUtils.hasText(endpoint)) {
			log.debug(
					"shopex crm sync single order endpoint not configured; skip companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("companyId", companyId);
			body.put("orderId", orderId);
			String json = objectMapper.writeValueAsString(body);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			restTemplate.postForEntity(endpoint.trim(), new HttpEntity<>(json, headers), String.class);
		} catch (RestClientException e) {
			log.warn(
					"shopex crm sync single order request failed companyId={} orderId={} msg={}",
					companyId,
					orderId,
					e.getMessage());
		} catch (Exception e) {
			log.warn(
					"shopex crm sync single order error companyId={} orderId={} msg={}",
					companyId,
					orderId,
					e.getMessage());
		}
	}
}
