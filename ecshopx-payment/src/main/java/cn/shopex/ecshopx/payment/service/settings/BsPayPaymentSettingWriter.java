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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BsPayPaymentSettingWriter {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public BsPayPaymentSettingWriter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> s = cmd.scalarFields();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("sys_id", s.get("sys_id"));
		data.put("product_id", s.get("product_id"));
		data.put("rsa_merch_private_key", s.get("rsa_merch_private_key"));
		data.put("rsa_huifu_public_key", s.get("rsa_huifu_public_key"));
		data.put("admin_token_no", s.get("admin_token_no"));
		data.put("pay_channel", parsePayChannel(s.get("pay_channel")));
		data.put("wxpay_fee_type", s.get("wxpay_fee_type"));
		data.put("wx_lite_online", s.get("wx_lite_online"));
		data.put("wx_pub_online", s.get("wx_pub_online"));
		data.put("wx_qr_online", s.get("wx_qr_online"));
		data.put("alipay_fee_type", s.get("alipay_fee_type"));
		data.put("alipay_call", s.get("alipay_call"));
		data.put("alipay_qr_online", s.get("alipay_qr_online"));
		data.put("is_open", PaymentSettingBooleanParsing.looseTrueString(s.get("is_open")));

		String redisKey = PaymentSettingRedisKeys.bspaySettingKey(cmd.companyId());
		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private List<Object> parsePayChannel(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof List<?> l) {
			return new ArrayList<>(l);
		}
		if (raw instanceof String str) {
			String s = str.trim();
			if (s.isEmpty()) {
				return new ArrayList<>();
			}
			try {
				@SuppressWarnings("unchecked")
				List<Object> parsed = objectMapper.readValue(s, List.class);
				return parsed != null ? new ArrayList<>(parsed) : new ArrayList<>();
			} catch (Exception e) {
				return new ArrayList<>();
			}
		}
		return new ArrayList<>();
	}
}
