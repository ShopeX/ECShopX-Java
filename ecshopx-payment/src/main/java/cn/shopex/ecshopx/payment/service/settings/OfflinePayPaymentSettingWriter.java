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
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OfflinePayPaymentSettingWriter {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public OfflinePayPaymentSettingWriter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> scalar = cmd.scalarFields();
		String lang = "zh-CN";
		Object cc = scalar.get("country_code");
		if (cc != null && StringUtils.hasText(String.valueOf(cc).trim())) {
			lang = String.valueOf(cc).trim();
		}

		Object rawName = scalar.get("pay_name");
		String payName = rawName != null ? String.valueOf(rawName).trim() : "";
		if (StringUtils.hasText(payName)) {
			companysRedisTemplate
					.opsForValue()
					.set(PaymentSettingRedisKeys.offlinePayNameLangKey(cmd.companyId(), lang), payName);
		}

		if (!StringUtils.hasText(payName)) {
			payName = "线下支付";
		}

		int autoCancel = 0;
		Object rawAct = scalar.get("auto_cancel_time");
		if (rawAct instanceof Number n) {
			autoCancel = n.intValue();
		} else if (rawAct != null && StringUtils.hasText(String.valueOf(rawAct).trim())) {
			try {
				autoCancel = Integer.parseInt(String.valueOf(rawAct).trim());
			} catch (NumberFormatException e) {
				autoCancel = 0;
			}
		}
		if (autoCancel < 1) {
			throw new BadRequestException("订单自动取消时间不能小于1小时");
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("pay_name", payName);
		data.put("pay_tips", scalar.getOrDefault("pay_tips", ""));
		data.put("pay_desc", scalar.getOrDefault("pay_desc", ""));
		data.put("auto_cancel_time", autoCancel);
		data.put("is_open", PaymentSettingBooleanParsing.offlinePayIsOpenString(scalar.get("is_open")));

		String redisKey = PaymentSettingRedisKeys.offlinePaySettingKey(cmd.companyId(), lang);
		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}
}
