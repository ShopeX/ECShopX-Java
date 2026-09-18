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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.RecommendLike;
import cn.shopex.ecshopx.promotions.mapper.RecommendLikeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecommendLikeCreateService {

	private final RecommendLikeMapper recommendLikeMapper;

	public RecommendLikeCreateService(RecommendLikeMapper recommendLikeMapper) {
		this.recommendLikeMapper = recommendLikeMapper;
	}

	public Map<String, Object> updateRecommendLike(long companyId, Map<String, Object> merged) {
		Long rowId = parseRowIdFromMerged(merged.get("id"));
		Integer sortRaw = parseIntegerAllowingZero(merged.get("sort"));
		Long itemIdRaw = parseLong(merged.get("item_id"));

		LambdaQueryWrapper<RecommendLike> wrapper =
				new LambdaQueryWrapper<RecommendLike>().eq(RecommendLike::getCompanyId, companyId);
		if (rowId != null) {
			wrapper.eq(RecommendLike::getId, rowId);
		} else {
			wrapper.isNull(RecommendLike::getId);
		}
		wrapper.last("LIMIT 1");
		RecommendLike existing = recommendLikeMapper.selectOne(wrapper);

		if (existing == null) {
			RecommendLike entity = new RecommendLike();
			entity.setCompanyId(companyId);
			if (itemIdRaw != null && idLooksPresent(itemIdRaw.longValue())) {
				entity.setItemId(itemIdRaw);
			}
			if (sortRaw != null && sortLooksPresent(sortRaw)) {
				entity.setSort(sortRaw);
			}
			recommendLikeMapper.insert(entity);
			Objects.requireNonNull(
					entity.getId(), "promotions_recommend_like_items insert missing generated id");
			return toRecommendLikeRowMap(entity);
		}

		if (itemIdRaw != null && idLooksPresent(itemIdRaw.longValue())) {
			existing.setItemId(itemIdRaw);
		}
		if (sortRaw != null && sortLooksPresent(sortRaw)) {
			existing.setSort(sortRaw);
		}
		recommendLikeMapper.updateById(existing);
		return toRecommendLikeRowMap(existing);
	}

	public Map<String, Object> createRecommendLike(long companyId, List<Map<String, Object>> items) {
		List<Long> requestItemIds = items.stream().map(m -> parseLong(m.get("item_id"))).toList();
		long existingOther =
				recommendLikeMapper.selectCount(
						new LambdaQueryWrapper<RecommendLike>()
								.eq(RecommendLike::getCompanyId, companyId)
								.notIn(RecommendLike::getItemId, requestItemIds));
		if (existingOther + items.size() > 30) {
			throw new ResourceException("不能超过30件商品");
		}
		for (Map<String, Object> item : items) {
			long itemId = Objects.requireNonNull(parseLong(item.get("item_id")));
			Integer sortRaw = parseIntegerAllowingZero(item.get("sort"));
			long distributorId = optionalLong(item.get("distributor_id"), 0L);

			RecommendLike row =
					recommendLikeMapper.selectOne(
							new LambdaQueryWrapper<RecommendLike>()
									.eq(RecommendLike::getCompanyId, companyId)
									.eq(RecommendLike::getItemId, itemId)
									.last("LIMIT 1"));
			if (row == null) {
				RecommendLike entity = new RecommendLike();
				entity.setCompanyId(companyId);
				if (idLooksPresent(itemId)) {
					entity.setItemId(itemId);
				}
				if (sortLooksPresent(sortRaw)) {
					entity.setSort(sortRaw);
				}
				if (distributorId != 0L) {
					entity.setDistributorId(distributorId);
				}
				recommendLikeMapper.insert(entity);
			} else {
				if (idLooksPresent(companyId)) {
					row.setCompanyId(companyId);
				}
				if (idLooksPresent(itemId)) {
					row.setItemId(itemId);
				}
				if (sortLooksPresent(sortRaw)) {
					row.setSort(sortRaw);
				}
				if (distributorId != 0L) {
					row.setDistributorId(distributorId);
				}
				recommendLikeMapper.updateById(row);
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", Long.valueOf(companyId));
		out.put("items", items);
		return out;
	}

	private static Long parseLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer parseIntegerAllowingZero(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long optionalLong(Object o, long default0) {
		if (o == null) {
			return default0;
		}
		Long p = parseLong(o);
		return p != null ? p : default0;
	}

	private static boolean sortLooksPresent(Integer v) {
		return v != null && v != 0;
	}

	private static boolean idLooksPresent(long v) {
		return v != 0L;
	}

	private static Long parseRowIdFromMerged(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, Object> toRecommendLikeRowMap(RecommendLike e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("item_id", e.getItemId());
		m.put("sort", e.getSort());
		return m;
	}
}
