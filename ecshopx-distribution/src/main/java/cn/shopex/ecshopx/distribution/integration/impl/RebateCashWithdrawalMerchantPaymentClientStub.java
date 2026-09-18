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

package cn.shopex.ecshopx.distribution.integration.impl;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.integration.RebateCashWithdrawalMerchantPaymentClient;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RebateCashWithdrawalMerchantPaymentClientStub implements RebateCashWithdrawalMerchantPaymentClient {

	private static final String MSG_PAYMENT_UNSUPPORTED = "不支持支付服务，请联系商家";
	private static final String MSG_WX_PAY_CONFIG_INCOMPLETE = "请检查微信支付相关配置是否完成";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public RebateCashWithdrawalMerchantPaymentClientStub(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> merchantPayment(long companyId, String wxaAppid, Map<String, Object> paymentData) {
		String raw = companysRedisTemplate.opsForValue()
				.get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, 0L));
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		if (!StringUtils.hasText(raw) || cfg.isEmpty()) {
			throw new BadRequestException(MSG_PAYMENT_UNSUPPORTED);
		}
		if (!hasCompleteWxpayCertificates(cfg)) {
			return wxPayConfigFailMap();
		}
		// Stub: real WeChat transfer not implemented; returns business-level FAIL.
		return wxPayConfigFailMap();
	}

	/**
	 * 缺少证书路径时返回 FAIL，而非 HTTP 400。
	 * 同时支持 {@code cert}/{@code cert_key} 和 {@code cert_url}/{@code cert_key_url} 两种命名。
	 */
	private static boolean hasCompleteWxpayCertificates(Map<String, Object> cfg) {
		boolean legacyPair = !PaymentConfigJsonSupport.isRequiredCredentialMissing(cfg.get("cert"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(cfg.get("cert_key"));
		boolean urlNamedPair = !PaymentConfigJsonSupport.isRequiredCredentialMissing(cfg.get("cert_url"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(cfg.get("cert_key_url"));
		return legacyPair || urlNamedPair;
	}

	private static Map<String, Object> wxPayConfigFailMap() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", "FAIL");
		out.put("error_desc", MSG_WX_PAY_CONFIG_INCOMPLETE);
		return out;
	}
}
