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
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayPaymentSettingWriter {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public AlipayPaymentSettingWriter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void write(PaymentSettingCommand cmd) {
		Map<String, Object> scalar = cmd.scalarFields();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("app_id", scalar.get("app_id"));
		data.put("private_key", scalar.get("private_key"));
		data.put("ali_public_key", scalar.get("ali_public_key"));
		data.put("is_open", PaymentSettingBooleanParsing.strictTrueString(scalar.get("is_open")));

		Object pk = data.get("private_key");
		if (pk != null && StringUtils.hasText(String.valueOf(pk))) {
			validatePrivateKey(String.valueOf(pk));
		}

		String redisKey = PaymentSettingRedisKeys.alipayRedisKey(cmd.companyId(), cmd.distributorId());
		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private static void validatePrivateKey(String privateKey) {
		try {
			String pk =
					privateKey
							.replace("-----BEGIN RSA PRIVATE KEY-----", "")
							.replace("-----END RSA PRIVATE KEY-----", "")
							.replace("-----BEGIN PRIVATE KEY-----", "")
							.replace("-----END PRIVATE KEY-----", "")
							.replaceAll("\\s", "");
			if (!StringUtils.hasText(pk)) {
				return;
			}
			byte[] keyBytes = Base64.getDecoder().decode(pk);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PrivateKey priv = kf.generatePrivate(spec);
			if (priv == null) {
				throw new BadRequestException("私钥内容无效");
			}
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("私钥内容无效");
		}
	}
}
