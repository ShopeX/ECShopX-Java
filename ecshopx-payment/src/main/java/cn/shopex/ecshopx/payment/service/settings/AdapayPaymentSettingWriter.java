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

import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
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

@Service
public class AdapayPaymentSettingWriter {

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;

	public AdapayPaymentSettingWriter(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> s = cmd.scalarFields();
		Map<String, Object> data = new LinkedHashMap<>();
		copy(s, data, "app_id");
		copy(s, data, "test_api_key");
		copy(s, data, "live_api_key");
		copy(s, data, "rsa_private_key");
		copy(s, data, "pay_channel");
		copy(s, data, "wxpay_fee_type");
		copy(s, data, "wx_pub_online");
		copy(s, data, "wx_pub_offline");
		copy(s, data, "wx_lite_online");
		copy(s, data, "wx_lite_offline");
		copy(s, data, "wx_scan");
		copy(s, data, "alipay_fee_type");
		copy(s, data, "alipay_qr_online");
		copy(s, data, "alipay_qr_offline");
		copy(s, data, "alipay_scan");
		copy(s, data, "alipay_lite_online");
		copy(s, data, "alipay_lite_offline");
		copy(s, data, "alipay_call");
		copy(s, data, "ali_pub_off_b2b");
		copy(s, data, "ali_pub_online_b2b");
		data.put("is_open", PaymentSettingBooleanParsing.looseTrueString(s.get("is_open")));

		String redisKey = PaymentSettingRedisKeys.adapaySettingKey(cmd.companyId());
		try {
			sharedStringRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", cmd.companyId());
		adapayOperationLogRecordPort.logRecord(
				logParams, cmd.operatorId(), "set_payment_setting", "merchant", cmd.operatorId());
	}

	private static void copy(Map<String, Object> from, Map<String, Object> to, String key) {
		if (from.containsKey(key)) {
			to.put(key, from.get(key));
		}
	}
}
