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

package cn.shopex.ecshopx.companys.service.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompanyTradeRateItemDisplayRedisReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public CompanyTradeRateItemDisplayRedisReadService(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public boolean readRateStatus(long companyId) {
		String key = "TradeRateSetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return false;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return jsonNodeToBooleanStrict(n.get("rate_status"), false);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean readItemSalesDisplay(long companyId) {
		String key = "ItemSalesSetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return true;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return jsonNodeToBooleanStrict(n.get("item_sales_status"), true);
		} catch (Exception e) {
			return true;
		}
	}

	public boolean readItemStoreDisplay(long companyId) {
		String key = "ItemStoreSetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return true;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return jsonNodeToBooleanStrict(n.get("item_store_status"), true);
		} catch (Exception e) {
			return true;
		}
	}

	private static boolean jsonNodeToBooleanStrict(JsonNode node, boolean defaultVal) {
		if (node == null || node.isNull()) {
			return defaultVal;
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isInt()) {
			return node.asInt() != 0;
		}
		if (node.isTextual()) {
			String s = node.asText().trim();
			return "1".equals(s) || "true".equalsIgnoreCase(s);
		}
		return defaultVal;
	}
}
