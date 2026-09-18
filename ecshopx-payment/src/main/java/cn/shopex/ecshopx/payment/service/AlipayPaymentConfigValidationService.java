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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlipayPaymentConfigValidationService {

	private static final String MSG_DISTRIBUTOR = "店铺支付宝配置不完整，请先完成配置后再切换收款主体";
	private static final String MSG_PLATFORM = "平台支付宝配置不完整，请先完成配置后再切换收款主体";
	private static final String MSG_ASYNC_NOTIFY = "支付宝信息未配置，请联系商家";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public AlipayPaymentConfigValidationService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void assertConfigComplete(long companyId, long distributorIdForSetting) {
		if (!isConfigCredentialComplete(companyId, distributorIdForSetting)) {
			throw new ResourceException(distributorIdForSetting > 0 ? MSG_DISTRIBUTOR : MSG_PLATFORM);
		}
	}

	/**
	 * Async notify endpoint: missing credentials use the same user-visible message and Dingo-style 400 envelope
	 * as the legacy API (see {@code PaymentNotifyController} {@code @DingoResponse#badRequest}).
	 */
	public void assertAsyncNotifyConfigComplete(long companyId, long distributorIdForSetting) {
		if (!isConfigCredentialComplete(companyId, distributorIdForSetting)) {
			throw new BadRequestException(MSG_ASYNC_NOTIFY);
		}
	}

	private boolean isConfigCredentialComplete(long companyId, long distributorIdForSetting) {
		String raw = companysRedisTemplate.opsForValue()
				.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, distributorIdForSetting));
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		List<String> fields = List.of("app_id", "ali_public_key", "private_key");
		for (String field : fields) {
			if (PaymentConfigJsonSupport.isRequiredCredentialMissing(cfg.get(field))) {
				return false;
			}
		}
		return true;
	}
}
