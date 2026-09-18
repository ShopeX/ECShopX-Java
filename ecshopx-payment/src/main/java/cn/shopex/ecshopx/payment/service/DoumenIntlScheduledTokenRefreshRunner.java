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

import cn.shopex.ecshopx.payment.config.DoumenIntlProperties;
import cn.shopex.ecshopx.payment.integration.doumenintl.DoumenIntlGatewayClient;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 扫描 eligible 斗门配置并刷新 token；全量刷新成功后更新调度水位。
 */
@Service
public class DoumenIntlScheduledTokenRefreshRunner {

	private static final String KEY_PREFIX = "doumenIntlPaymentSetting:";

	private final DoumenIntlPaymentSettingReader paymentSettingReader;
	private final DoumenIntlGatewayClient gatewayClient;
	private final DoumenIntlProperties properties;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DoumenIntlScheduledTokenRefreshRunner(
			DoumenIntlPaymentSettingReader paymentSettingReader,
			DoumenIntlGatewayClient gatewayClient,
			DoumenIntlProperties properties,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.paymentSettingReader = paymentSettingReader;
		this.gatewayClient = gatewayClient;
		this.properties = properties;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * @param companyId null=全量；非 null=仅该公司
	 * @return attempted / ok / failed
	 */
	public Map<String, Integer> run(Long companyId) {
		List<Map<String, Object>> configs =
				companyId != null ? loadConfigForCompany(companyId) : scanEligibleConfigs();
		int attempted = configs.size();
		int ok = 0;
		for (Map<String, Object> config : configs) {
			String accessCode = stringVal(config.get("X-AccessCode"));
			String secretKey = stringVal(config.get("X-SecretKey"));
			if (gatewayClient.refreshToken(accessCode, secretKey)) {
				ok++;
			}
		}
		if (companyId == null) {
			companysRedisTemplate
					.opsForValue()
					.set(
							PaymentSettingRedisKeys.doumenIntlTokenRefreshLastScheduledRunKey(),
							String.valueOf(System.currentTimeMillis() / 1000L));
		}
		Map<String, Integer> stats = new LinkedHashMap<>();
		stats.put("attempted", attempted);
		stats.put("ok", ok);
		stats.put("failed", attempted - ok);
		return stats;
	}

	/** 距上次全量水位是否达到刷新间隔。 */
	public boolean shouldRunScheduledRefresh() {
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.doumenIntlTokenRefreshLastScheduledRunKey());
		if (!StringUtils.hasText(raw)) {
			return true;
		}
		long last;
		try {
			last = Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return true;
		}
		long intervalSec = Math.max(1, properties.getTokenRefreshIntervalMinutes()) * 60L;
		return (System.currentTimeMillis() / 1000L) - last >= intervalSec;
	}

	private List<Map<String, Object>> loadConfigForCompany(long companyId) {
		if (!paymentSettingReader.isConfigured(companyId)) {
			return List.of();
		}
		Map<String, Object> config = paymentSettingReader.getRaw(companyId);
		return isEligibleConfig(config) ? List.of(config) : List.of();
	}

	private List<Map<String, Object>> scanEligibleConfigs() {
		List<Map<String, Object>> configs = new ArrayList<>();
		ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(100).build();
		try (Cursor<String> cursor = companysRedisTemplate.scan(options)) {
			while (cursor.hasNext()) {
				String key = cursor.next();
				String raw = companysRedisTemplate.opsForValue().get(key);
				Map<String, Object> decoded =
						PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
				if (isEligibleConfig(decoded)) {
					configs.add(decoded);
				}
			}
		}
		return configs;
	}

	private static boolean isEligibleConfig(Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return false;
		}
		if (!PaymentConfigJsonSupport.normalizeIsOpen(data.get("is_open"))) {
			return false;
		}
		return !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("X-AccessCode"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("X-SecretKey"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("appId"))
				&& !PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("return_url"));
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
