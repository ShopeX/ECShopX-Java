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

package cn.shopex.ecshopx.chinaumspay.setting;

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsPaymentSettingLoadPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 与 PHP {@code ChinaumsPayService::getPaymentSetting} 对齐：Redis key
 * {@code chinaumsPaymentSetting:sha1(companyId)[_subKey]}；可选补充本地 pfx 路径。
 */
@Service
@Profile("!test-cron")
public class ChinaumsPaymentSettingLoadService implements ChinaumsPaymentSettingLoadPort {

	private static final String KEY_PREFIX = "chinaumsPaymentSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String storageLocalRoot;

	public ChinaumsPaymentSettingLoadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.storage.local.root:storage/app/public}") String storageLocalRoot) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.storageLocalRoot = storageLocalRoot;
	}

	@Override
	public Map<String, Object> load(long companyId, String subKey) {
		String base = KEY_PREFIX + sha1Hex(String.valueOf(companyId));
		String redisKey = (subKey == null || subKey.isEmpty()) ? base : (base + "_" + subKey);
		String raw = companysRedisTemplate.opsForValue().get(redisKey);
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = objectMapper.readValue(raw, Map.class);
			if (m == null) {
				return Map.of();
			}
			enrichKeyPaths(m);
			return m;
		} catch (Exception e) {
			return Map.of();
		}
	}

	/** 在 PHP 一致前提下补充 rsa 证书路径，供签名使用。 */
	private void enrichKeyPaths(Map<String, Object> m) {
		var copy = new HashMap<>(m);
		Object mid = copy.get("mid");
		if (mid == null) {
			return;
		}
		String midStr = String.valueOf(mid).trim();
		if (midStr.isEmpty()) {
			return;
		}
		try {
			Path base = Path.of(storageLocalRoot).toAbsolutePath().normalize();
			Path ums = base.resolve("chinaumsPayment");
			Path midDir = ums.resolve(midStr).normalize();
			if (!midDir.startsWith(ums)) {
				return;
			}
			Path pfx = midDir.resolve("rsa_private.pfx");
			Path pub = midDir.resolve("rsa_public.cer");
			if (Files.isRegularFile(pfx)) {
				copy.put("rsa_private_name", "rsa_private.pfx");
				copy.put("rsa_private_path", pfx.toString());
			}
			if (Files.isRegularFile(pub)) {
				copy.put("rsa_public_name", "rsa_public.cer");
				copy.put("rsa_public_path", pub.toString());
			}
			m.putAll(copy);
		} catch (Exception ignored) {
			// 保持 Redis 原样
		}
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
