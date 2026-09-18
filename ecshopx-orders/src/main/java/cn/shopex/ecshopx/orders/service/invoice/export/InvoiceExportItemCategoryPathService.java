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

package cn.shopex.ecshopx.orders.service.invoice.export;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InvoiceExportItemCategoryPathService {

	private static final String SQL_ITEM_CATEGORIES = """
			SELECT item_id, item_category
			FROM items
			WHERE company_id = :companyId AND item_id IN (:itemIds)
			""";

	private static final String SQL_CATEGORY_ROW = """
			SELECT category_id, parent_id, category_name
			FROM items_category
			WHERE company_id = :companyId AND category_id IN (:categoryIds)
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public InvoiceExportItemCategoryPathService(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Returns {@code item_id ->} path of category names from root to leaf, segments joined with {@code ->}.
	 */
	public Map<Long, String> loadCategoryPaths(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Map.of();
		}
		List<Long> distinct = itemIds.stream().filter(id -> id != null && id > 0L).distinct().toList();
		if (distinct.isEmpty()) {
			return Map.of();
		}
		MapSqlParameterSource p = new MapSqlParameterSource("companyId", companyId).addValue("itemIds", distinct);
		List<Map<String, Object>> rows = jdbc.queryForList(SQL_ITEM_CATEGORIES, p);
		Map<Long, Long> itemToLeaf = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Long itemId = longObj(row.get("item_id"));
			String raw = row.get("item_category") == null ? "" : String.valueOf(row.get("item_category")).trim();
			if (itemId == null || !StringUtils.hasText(raw)) {
				continue;
			}
			try {
				long catId = Long.parseLong(raw);
				if (catId > 0L) {
					itemToLeaf.put(itemId, catId);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		if (itemToLeaf.isEmpty()) {
			return distinct.stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> ""));
		}
		Map<Long, CategoryNode> nodes = loadCategoryClosure(companyId, new HashSet<>(itemToLeaf.values()));
		Map<Long, String> out = new HashMap<>();
		for (Long itemId : distinct) {
			Long leaf = itemToLeaf.get(itemId);
			if (leaf == null) {
				out.put(itemId, "");
				continue;
			}
			out.put(itemId, buildPath(leaf, nodes));
		}
		return out;
	}

	private Map<Long, CategoryNode> loadCategoryClosure(long companyId, Set<Long> seeds) {
		Map<Long, CategoryNode> nodes = new HashMap<>();
		ArrayDeque<Long> queue = new ArrayDeque<>(seeds);
		Set<Long> pending = new HashSet<>(seeds);
		while (!queue.isEmpty()) {
			List<Long> batch = new ArrayList<>();
			while (!queue.isEmpty() && batch.size() < 200) {
				Long id = queue.poll();
				if (id == null || id <= 0L || nodes.containsKey(id)) {
					continue;
				}
				batch.add(id);
			}
			if (batch.isEmpty()) {
				continue;
			}
			MapSqlParameterSource p =
					new MapSqlParameterSource("companyId", companyId).addValue("categoryIds", batch);
			List<Map<String, Object>> rows = jdbc.queryForList(SQL_CATEGORY_ROW, p);
			for (Map<String, Object> row : rows) {
				Long cid = longObj(row.get("category_id"));
				if (cid == null) {
					continue;
				}
				Long parentId = longObj(row.get("parent_id"));
				String name = row.get("category_name") == null ? "" : String.valueOf(row.get("category_name"));
				nodes.put(cid, new CategoryNode(cid, parentId == null ? 0L : parentId, name));
				if (parentId != null && parentId > 0L && !nodes.containsKey(parentId) && pending.add(parentId)) {
					queue.add(parentId);
				}
			}
		}
		return nodes;
	}

	private static String buildPath(long leafId, Map<Long, CategoryNode> nodes) {
		List<String> names = new ArrayList<>();
		Long cur = leafId;
		Set<Long> guard = new HashSet<>();
		while (cur != null && cur > 0L) {
			if (!guard.add(cur)) {
				break;
			}
			CategoryNode n = nodes.get(cur);
			if (n == null) {
				break;
			}
			if (StringUtils.hasText(n.name)) {
				names.add(n.name);
			}
			cur = n.parentId;
		}
		Collections.reverse(names);
		return String.join("->", names);
	}

	private static Long longObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private record CategoryNode(long categoryId, long parentId, String name) {}
}
