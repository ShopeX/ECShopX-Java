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

@Service
public class IcbcPayPaymentSettingWriter {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public IcbcPayPaymentSettingWriter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> s = cmd.scalarFields();
		String redisKey = PaymentSettingRedisKeys.icbcPaymentSettingKey(cmd.companyId());
		String rawExisting = companysRedisTemplate.opsForValue().get(redisKey);
		Map<String, Object> paySetting =
				rawExisting != null
						? new LinkedHashMap<>(PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawExisting))
						: new LinkedHashMap<>();

		putIfPresent(paySetting, s, "appid");
		putIfPresent(paySetting, s, "mer_id");
		putIfPresent(paySetting, s, "decive_info");
		putIfPresent(paySetting, s, "private_key");
		putIfPresent(paySetting, s, "public_key");

		Object isOpenRaw = s.get("is_open");
		if (isOpenRaw == null) {
			isOpenRaw = "false";
		}
		int isOpenNum = PaymentSettingBooleanParsing.looseTrueString(isOpenRaw) ? 1 : 0;
		paySetting.put("is_open", isOpenNum);

		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(paySetting));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private static void putIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
		if (source.containsKey(key)) {
			target.put(key, source.get(key));
		}
	}
}
