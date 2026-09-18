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

package cn.shopex.ecshopx.kaquan.service.discount;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 兑换卡模板解析所需的跨表查询；实现于 kaquan 模块以避免 ecshopx-goods ↔ ecshopx-kaquan 的 Maven 循环依赖（与商品域共用同一数据源）。
 */
@Service
public class UserDiscountExchangeCardGoodsLookupService {

	private final JdbcTemplate jdbcTemplate;

	public UserDiscountExchangeCardGoodsLookupService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/** 与 {@code ItemsCategoryItemIdResolver#getItemIdsByCategoryTree} 一致：销售类目树 → item_id。 */
	public List<Long> getItemIdsByCategoryTree(long companyId, long categoryId) {
		List<Long> treeIds = listItemsCategoryIdsForTree(companyId, categoryId);
		if (treeIds.isEmpty()) {
			return List.of();
		}
		String in = inClausePlaceholders(treeIds.size());
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		args.addAll(treeIds);
		String sql = "SELECT DISTINCT item_id FROM goods_items_rel_cats WHERE company_id = ? AND category_id IN (" + in + ")";
		List<Long> raw = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("item_id"), args.toArray());
		return raw.stream().distinct().toList();
	}

	public List<Long> listItemIdsByTagIds(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		String in = inClausePlaceholders(tagIds.size());
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		args.addAll(tagIds);
		String sql = "SELECT item_id FROM items_rel_tags WHERE company_id = ? AND tag_id IN (" + in + ") ORDER BY item_id";
		List<Long> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("item_id"), args.toArray());
		return new ArrayList<>(new LinkedHashSet<>(rows));
	}

	/**
	 * 公司维度下，符合品牌属性且为默认主商品的全部 item_id（用于卡券适用商品枚举）。
	 */
	public List<Long> listAllItemIdsByBrandAttributeIds(long companyId, List<Long> brandAttributeIds) {
		if (brandAttributeIds == null || brandAttributeIds.isEmpty()) {
			return List.of();
		}
		List<Integer> brands = new ArrayList<>();
		for (Long b : brandAttributeIds) {
			if (b == null) {
				continue;
			}
			if (b > Integer.MAX_VALUE || b < Integer.MIN_VALUE) {
				continue;
			}
			brands.add(b.intValue());
		}
		if (brands.isEmpty()) {
			return List.of();
		}
		String in = inClausePlaceholders(brands.size());
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		args.addAll(brands);
		String sql = "SELECT DISTINCT item_id FROM items WHERE company_id = ? AND item_type = 'normal' "
				+ "AND special_type IN ('normal', 'drug') AND IFNULL(is_gift, 0) = 0 AND IFNULL(is_default, 0) = 1 "
				+ "AND brand_id IN (" + in + ")";
		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("item_id"), args.toArray());
	}

	/**
	 * 默认主商品 SKU 中排除指定 item_id 后的列表（use_bound=5 过滤语义的近似实现）。
	 */
	public List<Long> listDefaultNormalItemIdsExcluding(long companyId, List<Long> excludedItemIds) {
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		String notIn = "";
		if (excludedItemIds != null && !excludedItemIds.isEmpty()) {
			notIn = " AND item_id NOT IN (" + inClausePlaceholders(excludedItemIds.size()) + ")";
			args.addAll(excludedItemIds);
		}
		String sql = "SELECT item_id FROM items WHERE company_id = ? AND item_type = 'normal' "
				+ "AND special_type IN ('normal', 'drug') AND IFNULL(is_gift, 0) = 0 AND IFNULL(is_default, 0) = 1 "
				+ notIn;
		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("item_id"), args.toArray());
	}

	public List<Long> listItemIdsByBrandAttributeIds(long companyId, List<Long> brandAttributeIds, long selectedItemId) {
		if (brandAttributeIds == null || brandAttributeIds.isEmpty() || selectedItemId <= 0L) {
			return List.of();
		}
		List<Integer> brands = new ArrayList<>();
		for (Long b : brandAttributeIds) {
			if (b == null) {
				continue;
			}
			if (b > Integer.MAX_VALUE || b < Integer.MIN_VALUE) {
				continue;
			}
			brands.add(b.intValue());
		}
		if (brands.isEmpty()) {
			return List.of();
		}
		String in = inClausePlaceholders(brands.size());
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		args.add(selectedItemId);
		args.addAll(brands);
		String sql = "SELECT item_id FROM items WHERE company_id = ? AND item_id = ? AND item_type = 'normal' "
				+ "AND special_type IN ('normal', 'drug') AND IFNULL(is_gift, 0) = 0 AND IFNULL(is_default, 0) = 1 "
				+ "AND brand_id IN (" + in + ") LIMIT 1";
		List<Long> hit = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("item_id"), args.toArray());
		return hit.isEmpty() ? List.of() : List.of(hit.get(0));
	}

	private List<Long> listItemsCategoryIdsForTree(long companyId, long categoryId) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		ids.add(categoryId);
		List<Long> level1 = jdbcTemplate.query(
				"SELECT category_id FROM items_category WHERE company_id = ? AND parent_id = ?",
				(rs, rowNum) -> rs.getLong("category_id"),
				companyId,
				categoryId);
		if (!level1.isEmpty()) {
			ids.addAll(level1);
			String in = inClausePlaceholders(level1.size());
			List<Object> args = new ArrayList<>();
			args.add(companyId);
			args.addAll(level1);
			List<Long> level2 = jdbcTemplate.query(
					"SELECT category_id FROM items_category WHERE company_id = ? AND parent_id IN (" + in + ")",
					(rs, rowNum) -> rs.getLong("category_id"),
					args.toArray());
			if (!level2.isEmpty()) {
				ids.addAll(level2);
			}
		}
		return new ArrayList<>(ids);
	}

	private static String inClausePlaceholders(int n) {
		if (n <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append('?');
		}
		return sb.toString();
	}
}
