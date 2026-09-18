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

package cn.shopex.ecshopx.community.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import cn.shopex.ecshopx.common.cron.CommunitySettingReadPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CommunitySettingService implements CommunitySettingReadPort {

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public CommunitySettingService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> saveSetting(
			long companyId, boolean distributorBranch, Object distributorIdRaw, Map<String, Object> data)
			throws JsonProcessingException {
		String key = buildRedisKey(companyId, distributorBranch, distributorIdRaw);
		redis.opsForValue().set(key, objectMapper.writeValueAsString(data));
		return getSetting(companyId, distributorBranch, distributorIdRaw);
	}

	public Map<String, Object> getSetting(long companyId, boolean distributorBranch, Object distributorIdRaw) {
		String json = redis.opsForValue().get(buildRedisKey(companyId, distributorBranch, distributorIdRaw));
		Map<String, Object> parsed = new LinkedHashMap<>();
		if (json != null && !json.isBlank()) {
			try {
				Map<String, Object> read =
						objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
				if (read != null) {
					parsed = read;
				}
			} catch (JsonProcessingException ignored) {
				parsed = new LinkedHashMap<>();
			}
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("condition_type", "num");
		merged.put("condition_money", 0);
		merged.put("aggrement", "");
		merged.put("explanation", "");
		merged.put("rebate_ratio", 0);
		merged.putAll(parsed);
		return merged;
	}

	private String buildRedisKey(long companyId, boolean distributorBranch, Object distributorIdRaw) {
		if (!distributorBranch) {
			return "community_setting:" + companyId + "_0";
		}
		String suffix = distributorIdRaw == null ? "" : String.valueOf(distributorIdRaw);
		return "community_setting:" + companyId + "_" + suffix;
	}
}
