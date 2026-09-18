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

import cn.shopex.ecshopx.payment.dto.PaymentOpenStatusSnapshot;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompanyPaymentOpenStatusReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public CompanyPaymentOpenStatusReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public PaymentOpenStatusSnapshot getOpenStatus(long companyId) {
		String wxRaw = companysRedisTemplate.opsForValue().get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, 0L));
		Map<String, Object> wxMap = PaymentConfigJsonSupport.parseObjectMap(objectMapper, wxRaw);
		boolean wxOpen = PaymentConfigJsonSupport.normalizeIsOpen(wxMap.get("is_open"));

		String aliRaw = companysRedisTemplate.opsForValue().get(PaymentSettingRedisKeys.alipayRedisKey(companyId, 0L));
		Map<String, Object> aliMap = PaymentConfigJsonSupport.parseObjectMap(objectMapper, aliRaw);
		boolean aliOpen = PaymentConfigJsonSupport.normalizeIsOpen(aliMap.get("is_open"));

		String chinaumsRaw = companysRedisTemplate.opsForValue()
				.get(PaymentSettingRedisKeys.chinaumsPaymentSettingKey(companyId, ""));
		Map<String, Object> chinaumsMap = PaymentConfigJsonSupport.parseObjectMap(objectMapper, chinaumsRaw);
		boolean chinaumsOpen = PaymentConfigJsonSupport.normalizeIsOpen(chinaumsMap.get("is_open"));

		return PaymentOpenStatusSnapshot.builder()
				.wxpayOpen(wxOpen)
				.alipayOpen(aliOpen)
				.chinaumspayOpen(chinaumsOpen)
				.build();
	}
}
