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

package cn.shopex.ecshopx.adapay.service.integration;

import cn.shopex.ecshopx.adapay.config.AdapayCallbackProperties;
import cn.shopex.ecshopx.adapay.service.AdapayPaymentSettingRedisReader;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantAdaPayOutboundGateway;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayDrawCashAdaPayGateway {

	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway;
	private final AdapayCallbackProperties adapayCallbackProperties;

	public AdapayDrawCashAdaPayGateway(
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway,
			AdapayCallbackProperties adapayCallbackProperties) {
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapaySubMerchantAdaPayOutboundGateway = adapaySubMerchantAdaPayOutboundGateway;
		this.adapayCallbackProperties = adapayCallbackProperties;
	}

	public Map<String, Object> settleAccountBalance(Map<String, Object> baseParams) {
		requireOutboundBaseUrl();
		long companyId = ((Number) baseParams.get("company_id")).longValue();
		Map<String, Object> body = new LinkedHashMap<>(baseParams);
		Map<String, Object> merchantInfo =
				new LinkedHashMap<>(adapayPaymentSettingRedisReader.getPaymentSetting(companyId));
		body.put("merchant_info", merchantInfo);
		if (merchantInfo.isEmpty()) {
			throw new BadRequestException("adapay 支付信息未配置");
		}
		return adapaySubMerchantAdaPayOutboundGateway.postAdaPayRequest(body);
	}

	public Map<String, Object> drawCashCreate(Map<String, Object> baseParams) {
		requireOutboundBaseUrl();
		long companyId = ((Number) baseParams.get("company_id")).longValue();
		Map<String, Object> body = new LinkedHashMap<>(baseParams);
		Map<String, Object> merchantInfo =
				new LinkedHashMap<>(adapayPaymentSettingRedisReader.getPaymentSetting(companyId));
		body.put("merchant_info", merchantInfo);
		if (merchantInfo.isEmpty()) {
			throw new BadRequestException("adapay 支付信息未配置");
		}
		return adapaySubMerchantAdaPayOutboundGateway.postAdaPayRequest(body);
	}

	private void requireOutboundBaseUrl() {
		if (!StringUtils.hasText(adapayCallbackProperties.getSettleOutboundBaseUrl())) {
			throw new ResourceException("汇付 outbound 地址未配置");
		}
	}
}
