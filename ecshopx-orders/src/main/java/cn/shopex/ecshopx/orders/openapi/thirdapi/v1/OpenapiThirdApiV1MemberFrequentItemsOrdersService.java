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

package cn.shopex.ecshopx.orders.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersFrequentItemsMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1MemberFrequentItemsOrdersService {

	private static final TypeReference<List<Map<String, Object>>> AGGREGATE_LIST_TYPE =
			new TypeReference<>() {};

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final NormalOrdersFrequentItemsMapper frequentItemsMapper;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV1MemberFrequentItemsOrdersService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			NormalOrdersFrequentItemsMapper frequentItemsMapper,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.frequentItemsMapper = frequentItemsMapper;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> getFrequentItemAggregates(
			long companyId, Object userId, String timeRange) {
		long startTime = resolveStartEpochSeconds(timeRange);
		String cacheKey = buildCacheKey(companyId, userId, timeRange);

		String cached = sharedStringRedisTemplate.opsForValue().get(cacheKey);
		if (StringUtils.hasText(cached)) {
			try {
				List<Map<String, Object>> parsed =
						objectMapper.readValue(cached, AGGREGATE_LIST_TYPE);
				return parsed != null ? parsed : Collections.emptyList();
			} catch (Exception ignored) {
				// cache miss on parse failure
			}
		}

		List<Map<String, Object>> result =
				frequentItemsMapper.selectFrequentItemAggregates(companyId, userId, startTime);
		if (result == null) {
			result = Collections.emptyList();
		}

		try {
			String json = objectMapper.writeValueAsString(result);
			sharedStringRedisTemplate.opsForValue().set(cacheKey, json);
			sharedStringRedisTemplate.expire(cacheKey, Duration.ofSeconds(3600));
		} catch (Exception ignored) {
			// cache write failure does not block response
		}

		return result;
	}

	private static long resolveStartEpochSeconds(String timeRange) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		ZonedDateTime start;
		if ("0".equals(timeRange)) {
			start = now.minusYears(1);
		} else if ("1".equals(timeRange)) {
			start = now.minusMonths(6);
		} else if ("2".equals(timeRange)) {
			start = now.minusMonths(3);
		} else {
			throw new OpenapiLegacyZeroCodeFailException("时间段值无效");
		}
		return start.toEpochSecond();
	}

	private static String buildCacheKey(long companyId, Object userId, String timeRange) {
		return "user:frequent:item:c:" + companyId + ":u:" + userId + ":p:" + timeRange;
	}
}
