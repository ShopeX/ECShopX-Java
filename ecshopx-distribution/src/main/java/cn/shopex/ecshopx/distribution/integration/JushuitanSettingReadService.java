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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JushuitanSettingReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public JushuitanSettingReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void assertEnabledIfJstIdPositive(long companyId, Map<String, Object> merged) {
		long jst = longOrZero(merged.get("jst_shop_id"));
		if (jst <= 0) {
			return;
		}
		String raw = companysRedisTemplate.opsForValue().get(SystemLinkRedisJsonReadSupport.jushuitanRedisKey(companyId));
		Map<String, Object> cfg = SystemLinkRedisJsonReadSupport.parseJsonObject(raw, objectMapper);
		Object open = cfg.get("is_open");
		boolean enabled = open instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(open));
		if (!enabled) {
			throw new ResourceException("聚水潭ERP未开启");
		}
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
