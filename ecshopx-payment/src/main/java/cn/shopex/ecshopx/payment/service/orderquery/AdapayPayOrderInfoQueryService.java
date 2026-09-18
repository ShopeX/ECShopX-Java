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

package cn.shopex.ecshopx.payment.service.orderquery;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.adapay.Adapay;
import com.huifu.adapay.core.exception.BaseAdaPayException;
import com.huifu.adapay.model.MerConfig;
import com.huifu.adapay.model.Payment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayPayOrderInfoQueryService {

	private static final Object ADAPAY_ORDER_QUERY_MUTEX = new Object();

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.adapay.prod-mode:true}")
	private boolean prodMode;

	public AdapayPayOrderInfoQueryService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public String queryPayOrderInfoJson(long companyId, String tradeId, String transactionId) {
		if (tradeId == null || tradeId.isBlank()) {
			throw new BadRequestException("交易ID不存在", 400);
		}
		Map<String, Object> cfg = loadAdapaySetting(companyId);
		String appId = str(cfg.get("app_id"));
		String liveKey = str(cfg.get("live_api_key"));
		String testKey = str(cfg.get("test_api_key"));
		String rsaPrivate = str(cfg.get("rsa_private_key"));
		if (!StringUtils.hasText(appId)
				|| !StringUtils.hasText(rsaPrivate)
				|| (!StringUtils.hasText(liveKey) && !StringUtils.hasText(testKey))) {
			throw new BadRequestException("请检查adapay支付配置", 400);
		}
		String merKey = String.valueOf(companyId);
		Map<String, Object> root;
		synchronized (ADAPAY_ORDER_QUERY_MUTEX) {
			try {
				Adapay.prodMode = prodMode;
				MerConfig mc = new MerConfig();
				mc.setApiKey(prodMode ? firstNonBlank(liveKey, testKey) : firstNonBlank(testKey, liveKey));
				mc.setApiMockKey(testKey);
				mc.setRSAPrivateKey(rsaPrivate);
				Adapay.addMerConfig(mc, merKey);
				root = Payment.query(transactionId, merKey);
			} catch (BaseAdaPayException e) {
				String msg = e.getMessage();
				throw new BadRequestException(StringUtils.hasText(msg) ? msg : "AdaPay 查询失败", 400);
			} catch (Exception e) {
				throw new BadRequestException("AdaPay 查询失败", 400);
			}
		}
		Object dataNodeObj = root == null ? null : root.get("data");
		JsonNode dataNode = objectMapper.valueToTree(dataNodeObj == null ? Map.of() : dataNodeObj);
		try {
			return AdaBsPayOrderQueryJsonMapper.get().writeValueAsString(dataNode);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("AdaPay 查询失败", 400);
		}
	}

	private Map<String, Object> loadAdapaySetting(long companyId) {
		String key = "adaPaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = objectMapper.readValue(raw, Map.class);
			return m != null ? m : Map.of();
		} catch (Exception e) {
			return Map.of();
		}
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

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		return b == null ? "" : b;
	}
}
