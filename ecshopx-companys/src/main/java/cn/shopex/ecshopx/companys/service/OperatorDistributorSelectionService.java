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

package cn.shopex.ecshopx.companys.service;

import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorDistributorSelectionService {

	private static final Logger log = LoggerFactory.getLogger(OperatorDistributorSelectionService.class);

	private final StringRedisTemplate companysRedisTemplate;

	public OperatorDistributorSelectionService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void persistSelection(long operatorId, long companyId, Object setDistributorIdRaw) {
		String key = "select_distributor" + operatorId + "-" + companyId;
		String value = setDistributorIdRaw == null ? "0" : String.valueOf(setDistributorIdRaw);
		log.debug("operator distributor selection context operatorId={} companyId={}", operatorId, companyId);
		log.debug("operator distributor selection redis key={} value={}", key, value);
		companysRedisTemplate.opsForValue().set(key, value);
	}

	public OptionalLong readSelectedDistributorId(long operatorId, long companyId) {
		String key = "select_distributor" + operatorId + "-" + companyId;
		String text = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(text)) {
			return OptionalLong.empty();
		}
		OptionalLong parsed = normalizePositiveOperatorId(text.trim());
		return parsed.isPresent() && parsed.getAsLong() > 0 ? parsed : OptionalLong.empty();
	}

	private static OptionalLong normalizePositiveOperatorId(Object raw) {
		if (raw == null) {
			return OptionalLong.empty();
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? OptionalLong.of(v) : OptionalLong.empty();
		}
		if (raw instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty() || "0".equals(s)) {
				return OptionalLong.empty();
			}
			try {
				long v = Long.parseLong(s);
				return v > 0 ? OptionalLong.of(v) : OptionalLong.empty();
			} catch (NumberFormatException e) {
				return OptionalLong.empty();
			}
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return OptionalLong.empty();
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? OptionalLong.of(v) : OptionalLong.empty();
		} catch (NumberFormatException e) {
			return OptionalLong.empty();
		}
	}
}
