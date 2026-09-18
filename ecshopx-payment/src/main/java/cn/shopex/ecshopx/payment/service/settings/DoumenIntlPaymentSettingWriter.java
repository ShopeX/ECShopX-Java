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

package cn.shopex.ecshopx.payment.service.settings;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.service.admin.PaymentSettingBooleanParsing;
import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 斗门国际支付配置写入（平台级 Redis；空 {@code X-SecretKey} 保留旧值）。
 */
@Service
public class DoumenIntlPaymentSettingWriter {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DoumenIntlPaymentSettingWriter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> scalar = cmd.scalarFields();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("is_open", PaymentSettingBooleanParsing.strictTrueString(scalar.get("is_open")));
		data.put("X-AccessCode", scalar.get("X-AccessCode"));
		data.put("X-SecretKey", resolveSecretKey(cmd.companyId(), scalar.get("X-SecretKey")));
		data.put("appId", scalar.get("appId"));
		data.put("return_url", scalar.get("return_url"));

		String redisKey = PaymentSettingRedisKeys.doumenIntlPaymentSettingKey(cmd.companyId());
		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private Object resolveSecretKey(long companyId, Object incoming) {
		if (incoming instanceof String s && s.isEmpty()) {
			Map<String, Object> existing = loadRaw(companyId);
			Object old = existing.get("X-SecretKey");
			if (old != null && !(old instanceof String os && os.isEmpty())) {
				return old;
			}
		}
		return incoming;
	}

	private Map<String, Object> loadRaw(long companyId) {
		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.doumenIntlPaymentSettingKey(companyId));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}
}
