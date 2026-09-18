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

package cn.shopex.ecshopx.orders.service.distribution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributionPlanLimitTimeRedisReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DistributionPlanLimitTimeRedisReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public int readPlanLimitTimeDays(long companyId) {
		String key = "distribution:config:" + sha1Hex(String.valueOf(companyId));
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
		Object distributorObj = merged.get("distributor");
		if (!(distributorObj instanceof Map<?, ?> dist)) {
			return 0;
		}
		Object v = dist.get("plan_limit_time");
		return toInt(v, 0);
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

	private static int toInt(Object v, int dflt) {
		if (v == null) {
			return dflt;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			String s = v.toString().trim();
			if (s.isEmpty()) {
				return dflt;
			}
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return dflt;
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
