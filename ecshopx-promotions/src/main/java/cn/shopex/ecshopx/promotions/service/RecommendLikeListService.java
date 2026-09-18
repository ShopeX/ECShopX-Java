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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.promotions.RecommendLikeAdminListRowEnrichmentPort;
import cn.shopex.ecshopx.promotions.mapper.RecommendLikeMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecommendLikeListService {

	/** 列表 SQL 选定列在 JDBC 为 NULL 时可能不出现在 {@code Map} 中；补全键后再做后置增强。 */
	private static final List<String> RECOMMEND_LIKE_LIST_SQL_KEYS = List.of(
			"distributor_id",
			"price",
			"store",
			"company_id",
			"id",
			"sort",
			"item_id",
			"goods_id",
			"item_name",
			"itemName",
			"brief",
			"market_price",
			"nospec",
			"type",
			"approve_status",
			"pics");

	private final ShopMenuService shopMenuService;
	private final RecommendLikeMapper recommendLikeMapper;
	private final RecommendLikeAdminListRowEnrichmentPort recommendLikeAdminListRowEnrichmentPort;
	private final ObjectMapper objectMapper;

	public RecommendLikeListService(
			ShopMenuService shopMenuService,
			RecommendLikeMapper recommendLikeMapper,
			RecommendLikeAdminListRowEnrichmentPort recommendLikeAdminListRowEnrichmentPort,
			ObjectMapper objectMapper) {
		this.shopMenuService = shopMenuService;
		this.recommendLikeMapper = recommendLikeMapper;
		this.recommendLikeAdminListRowEnrichmentPort = recommendLikeAdminListRowEnrichmentPort;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getRecommendLikeLists(
			long companyId,
			int page,
			int pageSize,
			long distributorId,
			Boolean isCanSale,
			long userId,
			String acceptLanguageHeader) {
		String productModel = shopMenuService.resolveProductModelKeyForCompany(Long.valueOf(companyId));
		boolean standard = "standard".equalsIgnoreCase(productModel);
		boolean useDistributorJoin = standard && distributorId != 0L;

		long total =
				recommendLikeMapper.countRecommendLikeList(
						companyId, useDistributorJoin, distributorId, isCanSale);

		Map<String, Object> out = new LinkedHashMap<>();
		if (total == 0L) {
			out.put("total_count", 0L);
			out.put("list", List.of());
			return out;
		}

		long offset = (long) (page - 1) * (long) pageSize;
		Integer limit = pageSize > 0 ? pageSize : null;
		List<Map<String, Object>> list =
				recommendLikeMapper.selectRecommendLikeList(
						companyId, useDistributorJoin, distributorId, isCanSale, offset, limit);
		if (list == null) {
			list = new ArrayList<>();
		} else {
			list = new ArrayList<>(list);
		}

		for (Map<String, Object> row : list) {
			ensureSqlColumnKeysPresent(row);
			normalizeTypeForEnrichment(row);
			row.put("pics", parsePics(row.get("pics")));
		}

		recommendLikeAdminListRowEnrichmentPort.enrichRows(
				companyId, userId, list, acceptLanguageHeader);

		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private static void ensureSqlColumnKeysPresent(Map<String, Object> row) {
		for (String k : RECOMMEND_LIKE_LIST_SQL_KEYS) {
			if (!row.containsKey(k)) {
				row.put(k, null);
			}
		}
	}

	/** When present, coerce {@code type} to {@link Long}; missing or unparseable values remain null. */
	private void normalizeTypeForEnrichment(Map<String, Object> row) {
		Object ty = row.get("type");
		if (ty == null) {
			return;
		}
		if (ty instanceof Number n) {
			row.put("type", n.longValue());
			return;
		}
		String s = ty.toString().trim();
		if (s.isEmpty()) {
			row.put("type", null);
			return;
		}
		try {
			row.put("type", Long.parseLong(s));
		} catch (NumberFormatException ignored) {
			row.put("type", null);
		}
	}

	private Object parsePics(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> || raw instanceof Map<?, ?>) {
			return raw;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(s, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
