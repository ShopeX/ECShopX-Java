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

package cn.shopex.ecshopx.distribution.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributionConfigRedisReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DistributionConfigRedisReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate, ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public String buildDistributionConfigRedisKey(long companyId) {
		return "distribution:config:" + sha1Hex(String.valueOf(companyId));
	}

	/** 默认结构合并 Redis 后的完整配置（含 {@code company_id} 与 {@code distributor}）。 */
	public Map<String, Object> getMergedDistributionConfig(long companyId) {
		String key = buildDistributionConfigRedisKey(companyId);
		String raw = companysRedisTemplate.opsForValue().get(key);
		Map<String, Object> merged = defaultStructure(companyId);
		if (StringUtils.hasText(raw) && !"null".equalsIgnoreCase(raw.trim())) {
			try {
				Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (parsed != null && !parsed.isEmpty()) {
					mergeDeep(merged, parsed);
				}
			} catch (Exception ignored) {
				// keep defaults
			}
		}
		return merged;
	}

	/**
	 * 分销默认配置中的推广卖家分润比例（百分比数值，如 5 表示 5%）；未配置时为 0。
	 */
	public BigDecimal getPopularizeSellerPercent(long companyId) {
		Map<String, Object> merged = getMergedDistributionConfig(companyId);
		Object distributorObj = merged.get("distributor");
		if (!(distributorObj instanceof Map<?, ?> dist)) {
			return BigDecimal.ZERO;
		}
		return toBigDecimal(dist.get("popularize_seller"));
	}

	private static Map<String, Object> defaultStructure(long companyId) {
		Map<String, Object> distributor = new LinkedHashMap<>();
		distributor.put("show", 0);
		distributor.put("distributor", 0);
		distributor.put("seller", 0);
		distributor.put("popularize_seller", 0);
		distributor.put("distributor_seller", 0);
		distributor.put("plan_limit_time", 0);
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("company_id", companyId);
		info.put("distributor", distributor);
		return info;
	}

	@SuppressWarnings("unchecked")
	private static void mergeDeep(Map<String, Object> base, Map<String, Object> overlay) {
		for (Map.Entry<String, Object> e : overlay.entrySet()) {
			String k = e.getKey();
			Object ov = e.getValue();
			Object bv = base.get(k);
			if (ov instanceof Map<?, ?> om && bv instanceof Map<?, ?> bm) {
				mergeDeep((Map<String, Object>) bm, (Map<String, Object>) om);
			} else {
				base.put(k, ov);
			}
		}
	}

	private static BigDecimal toBigDecimal(Object v) {
		if (v == null) {
			return BigDecimal.ZERO;
		}
		if (v instanceof BigDecimal bd) {
			return bd;
		}
		if (v instanceof Long l) {
			return BigDecimal.valueOf(l);
		}
		if (v instanceof Integer i) {
			return BigDecimal.valueOf(i.longValue());
		}
		if (v instanceof Number n) {
			return new BigDecimal(n.toString());
		}
		try {
			String s = v.toString().trim();
			if (s.isEmpty()) {
				return BigDecimal.ZERO;
			}
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
