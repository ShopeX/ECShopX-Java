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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.payment.BspayPaymentSettingsReadPort;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BsPayPaymentSettingService implements BspayPaymentSettingsReadPort {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public BsPayPaymentSettingService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> requireSettingMap(long companyId) {
		String key = "bspaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		if (cfg.isEmpty()) {
			throw new ResourceException("请先配置支付信息");
		}
		String sysId = Objects.toString(cfg.get("sys_id"), "").trim();
		if (!StringUtils.hasText(sysId)) {
			throw new ResourceException("请先配置支付信息");
		}
		return cfg;
	}

	public Optional<String> findOptionalSysId(long companyId) {
		String key = "bspaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		if (cfg.isEmpty()) {
			return Optional.empty();
		}
		Object sysRaw = cfg.get("sys_id");
		if (sysRaw == null) {
			return Optional.empty();
		}
		String sysId;
		if (sysRaw instanceof String s) {
			sysId = s.trim();
		} else if (sysRaw instanceof Number n) {
			sysId = n.toString().trim();
		} else {
			return Optional.empty();
		}
		if (!StringUtils.hasText(sysId)) {
			return Optional.empty();
		}
		return Optional.of(sysId);
	}

	private static String sha1Hex(String companyId) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(companyId.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
