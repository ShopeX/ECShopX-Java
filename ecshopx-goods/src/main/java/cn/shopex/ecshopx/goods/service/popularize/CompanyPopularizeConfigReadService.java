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

package cn.shopex.ecshopx.goods.service.popularize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanyPopularizeConfigReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public CompanyPopularizeConfigReadService(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * Redis key popularizeConfig:{companyId}；JSON 字段 goods 缺失或非法时视为 "all"。
	 */
	public String getGoodsMode(long companyId) {
		String raw = companysRedisTemplate.opsForValue().get("popularizeConfig:" + companyId);
		if (!StringUtils.hasText(raw)) {
			return "all";
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			JsonNode g = root.get("goods");
			if (g == null || !g.isTextual()) {
				return "all";
			}
			String s = g.asText();
			return StringUtils.hasText(s) ? s : "all";
		} catch (Exception e) {
			return "all";
		}
	}

	/**
	 * 推广员列表价、积分换算使用的配置片段（与 Redis key {@code popularizeConfig:{companyId}} 的 JSON 结构一致；未配置时与现网 Redis 缺省行为一致）。
	 */
	public Map<String, Object> readWxappPromoterConfigSlice(long companyId) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("goods", getGoodsMode(companyId));
		out.put("commission_type", "money");
		Map<String, Object> ratioRoot = defaultPopularizeRatio();
		String raw = companysRedisTemplate.opsForValue().get("popularizeConfig:" + companyId);
		if (!StringUtils.hasText(raw)) {
			out.put("popularize_ratio", ratioRoot);
			return out;
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root != null) {
				JsonNode ct = root.get("commission_type");
				if (ct != null && ct.isTextual() && StringUtils.hasText(ct.asText())) {
					out.put("commission_type", ct.asText());
				}
				JsonNode pr = root.get("popularize_ratio");
				if (pr != null && pr.isObject()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> asMap = objectMapper.convertValue(pr, Map.class);
					if (asMap != null && !asMap.isEmpty()) {
						out.put("popularize_ratio", asMap);
						return out;
					}
				}
			}
		} catch (Exception ignored) {
			// keep defaults
		}
		out.put("popularize_ratio", ratioRoot);
		return out;
	}

	private static Map<String, Object> defaultPopularizeRatio() {
		Map<String, Object> order = new LinkedHashMap<>();
		Map<String, Object> fl = new LinkedHashMap<>();
		fl.put("ratio", 0);
		order.put("first_level", fl);
		Map<String, Object> om = new LinkedHashMap<>();
		om.put("first_level", order);
		Map<String, Object> profit = new LinkedHashMap<>();
		profit.put("first_level", order);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("type", "order_money");
		root.put("order_money", om);
		root.put("profit", profit);
		return root;
	}
}
