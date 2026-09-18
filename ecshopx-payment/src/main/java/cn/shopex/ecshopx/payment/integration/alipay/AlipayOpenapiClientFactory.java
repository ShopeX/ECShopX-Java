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

package cn.shopex.ecshopx.payment.integration.alipay;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builds a per-tenant {@link AlipayClient} from Redis payment settings (RSA2 / JSON).
 */
@Component
public class AlipayOpenapiClientFactory {

	private static final String DEFAULT_GATEWAY = "https://openapi.alipay.com/gateway.do";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String serverUrl;

	public AlipayOpenapiClientFactory(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.payment.alipay.server-url:https://openapi.alipay.com/gateway.do}") String serverUrl) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.serverUrl = serverUrl;
	}

	public AlipayClient alipayClient(long companyId, long distributorIdForSetting) {
		Map<String, Object> cfg = loadAlipayCfg(companyId, distributorIdForSetting);
		String appId = str(cfg.get("app_id"));
		String privateKey = str(cfg.get("private_key"));
		String alipayPublicKey = str(cfg.get("ali_public_key"));
		if (!StringUtils.hasText(appId) || !StringUtils.hasText(privateKey) || !StringUtils.hasText(alipayPublicKey)) {
			throw new BadRequestException("支付宝信息未配置，请联系商家");
		}
		String gateway = StringUtils.hasText(serverUrl) ? serverUrl.trim() : DEFAULT_GATEWAY;
		return new DefaultAlipayClient(gateway, appId, privateKey, "json", "UTF-8", alipayPublicKey, "RSA2");
	}

	public Map<String, Object> loadAlipayCfg(long companyId, long distributorIdForSetting) {
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, distributorIdForSetting));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
