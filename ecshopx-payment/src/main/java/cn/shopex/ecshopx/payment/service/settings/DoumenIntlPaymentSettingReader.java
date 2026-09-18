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

import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 斗门国际支付配置读取；GET 对 {@code X-SecretKey} 脱敏。
 */
@Service
public class DoumenIntlPaymentSettingReader {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DoumenIntlPaymentSettingReader(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	/** 脱敏后的配置；无配置返回空 Map（由调用方转 {@code []}）。 */
	public Map<String, Object> getMasked(long companyId) {
		Map<String, Object> data = getRaw(companyId);
		if (data.isEmpty()) {
			return Collections.emptyMap();
		}
		Object secret = data.get("X-SecretKey");
		data.put("X-SecretKey", maskSecret(secret == null ? null : String.valueOf(secret)));
		return data;
	}

	public Map<String, Object> getRaw(long companyId) {
		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.doumenIntlPaymentSettingKey(companyId));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}

	/**
	 * 配置完整且已启用（曝光 / 支付 / Token 刷新前置）。
	 * shop 线 Redis 存 bool {@code is_open}；用 {@link PaymentConfigJsonSupport#normalizeIsOpen} 判断开启。
	 */
	public boolean isConfigured(long companyId) {
		Map<String, Object> data = getRaw(companyId);
		if (data.isEmpty() || !PaymentConfigJsonSupport.normalizeIsOpen(data.get("is_open"))) {
			return false;
		}
		return !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("X-AccessCode"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("X-SecretKey"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("appId"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("return_url"));
	}

	public boolean isOpen(long companyId) {
		Map<String, Object> data = getRaw(companyId);
		return PaymentConfigJsonSupport.normalizeIsOpen(data.get("is_open"));
	}

	static String maskSecret(String secret) {
		if (secret == null || secret.isEmpty()) {
			return "";
		}
		if (secret.length() <= 4) {
			return "****";
		}
		return "****" + secret.substring(secret.length() - 4);
	}
}
