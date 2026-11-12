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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.common.discount.DiscountCardItemScopeGateway;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Admin 详情：在 {@code rule_config} 上附加类目/店铺/SKU 回显（不落库）。 */
@Service
public class MemberTaskRuleConfigDisplayService {

	private final DiscountCardItemScopeGateway discountCardItemScopeGateway;
	private final DistributorListQueryService distributorListQueryService;
	private final LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort;

	public MemberTaskRuleConfigDisplayService(
			DiscountCardItemScopeGateway discountCardItemScopeGateway,
			DistributorListQueryService distributorListQueryService,
			LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort) {
		this.discountCardItemScopeGateway = discountCardItemScopeGateway;
		this.distributorListQueryService = distributorListQueryService;
		this.limitPromotionAdminGoodsSupportPort = limitPromotionAdminGoodsSupportPort;
	}

	public Map<String, Object> enrichForAdminDetail(long companyId, Map<String, Object> rule) {
		if (rule == null || rule.isEmpty()) {
			return rule == null ? Map.of() : new LinkedHashMap<>(rule);
		}
		Map<String, Object> out = new LinkedHashMap<>(rule);
		List<Long> categoryIds = parsePositiveLongIds(rule.get("category_ids"));
		List<Long> distributorIds = parsePositiveLongIds(rule.get("distributor_ids"));
		List<Long> itemIds = parsePositiveLongIds(rule.get("item_ids"));
		out.put("category_list", buildCategoryList(companyId, categoryIds));
		out.put("distributor_list", buildDistributorList(companyId, distributorIds));
		out.put("item_list", buildItemList(companyId, itemIds));
		return out;
	}

	private List<Map<String, Object>> buildCategoryList(long companyId, List<Long> categoryIds) {
		if (categoryIds.isEmpty()) {
			return List.of();
		}
		Map<Long, String> names =
				discountCardItemScopeGateway.categoryNamesByIds(companyId, categoryIds);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Long id : categoryIds) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("category_id", id);
			row.put("category_name", names.getOrDefault(id, ""));
			out.add(row);
		}
		return out;
	}

	private List<Map<String, Object>> buildDistributorList(long companyId, List<Long> distributorIds) {
		if (distributorIds.isEmpty()) {
			return List.of();
		}
		List<Distributor> rows = distributorListQueryService.listByIdsAndCompany(companyId, distributorIds);
		Map<Long, String> nameById =
				rows.stream()
						.filter(d -> d.getDistributorId() != null)
						.collect(
								Collectors.toMap(
										Distributor::getDistributorId,
										d -> d.getName() == null ? "" : d.getName(),
										(a, b) -> a,
										LinkedHashMap::new));
		List<Map<String, Object>> out = new ArrayList<>();
		for (Long id : distributorIds) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("distributor_id", id);
			row.put("name", nameById.getOrDefault(id, ""));
			out.add(row);
		}
		return out;
	}

	private List<Map<String, Object>> buildItemList(long companyId, List<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> rows =
				limitPromotionAdminGoodsSupportPort.loadItemNameBnRowsByItemIds(companyId, itemIds);
		Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			Long id = toPositiveLong(row.get("item_id"));
			if (id != null) {
				byId.put(id, row);
			}
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Long id : itemIds) {
			Map<String, Object> src = byId.get(id);
			if (src != null) {
				out.add(new LinkedHashMap<>(src));
				continue;
			}
			Map<String, Object> missing = new LinkedHashMap<>();
			missing.put("item_id", id);
			missing.put("item_name", "");
			missing.put("item_bn", "");
			out.add(missing);
		}
		return out;
	}

	static List<Long> parsePositiveLongIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				Long id = toPositiveLong(o);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		Long single = toPositiveLong(raw);
		if (single != null) {
			out.add(single);
		}
		return out;
	}

	private static Long toPositiveLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		if (o instanceof String s && !s.isBlank()) {
			try {
				long v = Long.parseLong(s.trim());
				return v > 0L ? v : null;
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
