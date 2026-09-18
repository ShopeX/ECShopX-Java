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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributeCountReadService {

	public static final String METRIC_ITEM_TOTAL_PRICE = "itemTotalPrice";

	public static final String METRIC_REBATE_TOTAL = "rebateTotal";

	public static final String METRIC_NO_CLOSE_REBATE = "noCloseRebate";

	public static final String METRIC_CASH_WITHDRAWAL_REBATE = "cashWithdrawalRebate";

	public static final String METRIC_FREEZE_CASH_WITHDRAWAL_REBATE = "freezeCashWithdrawalRebate";

	private static final String[] METRIC_ORDER = {
			METRIC_ITEM_TOTAL_PRICE,
			METRIC_REBATE_TOTAL,
			METRIC_NO_CLOSE_REBATE,
			METRIC_CASH_WITHDRAWAL_REBATE,
			METRIC_FREEZE_CASH_WITHDRAWAL_REBATE,
	};

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DistributeCountReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDistributorCount(long distributorId) {
		Map<String, Object> out = new LinkedHashMap<>();
		long shard = distributorId / 20;
		String redisKey = "promoterPopularizeCount:" + shard;
		for (String metric : METRIC_ORDER) {
			String field = metric + "-" + distributorId;
			Object hv = companysRedisTemplate.opsForHash().get(redisKey, field);
			String raw = hv == null ? null : String.valueOf(hv);
			out.put(metric, Long.valueOf(parseMetricLong(raw)));
		}
		return out;
	}

	private long parseMetricLong(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		String s = raw.trim();
		JsonNode node;
		try {
			node = objectMapper.readTree(s);
		} catch (JsonProcessingException e) {
			return 0L;
		} catch (IllegalArgumentException e) {
			return 0L;
		}
		if (node == null || node.isNull()) {
			return 0L;
		}
		if (node.isNumber()) {
			return node.longValue();
		}
		if (node.isTextual()) {
			String t = node.asText();
			if (t == null || !StringUtils.hasText(t.trim())) {
				return 0L;
			}
			try {
				return Long.parseLong(t.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
