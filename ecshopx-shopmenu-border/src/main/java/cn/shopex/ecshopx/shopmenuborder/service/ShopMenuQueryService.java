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

package cn.shopex.ecshopx.shopmenuborder.service;

import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopMenuQueryService {

	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final ShopMenuService shopMenuService;
	private final ShopMenuOutsideLangReadService shopMenuOutsideLangReadService;

	public ShopMenuQueryService(
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			ShopMenuService shopMenuService,
			ShopMenuOutsideLangReadService shopMenuOutsideLangReadService) {
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
		this.shopMenuService = shopMenuService;
		this.shopMenuOutsideLangReadService = shopMenuOutsideLangReadService;
	}

	public Map<String, Object> getBorderShopMenu(int version, String requestLang) {
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.eq(ShopMenu::getCompanyId, 0)
				.eq(ShopMenu::getDisabled, false)
				.eq(ShopMenu::getVersion, version)
				.orderByAsc(ShopMenu::getPid, ShopMenu::getSort)
				.last("LIMIT 1000");
		List<ShopMenu> entities = shopMenuMapper.selectList(w);
		if (entities == null || entities.isEmpty()) {
			return Map.of("tree", List.of(), "list", List.of());
		}

		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (ShopMenu sm : entities) {
			rowMaps.add(toSnakeRow(sm));
		}

		List<Long> menuIds = new ArrayList<>(entities.size());
		for (ShopMenu sm : entities) {
			if (sm.getShopmenuId() != null) {
				menuIds.add(sm.getShopmenuId());
			}
		}
		Map<Long, String> nameTranslations =
				shopMenuOutsideLangReadService.findShopMenuNamesByLocale(0L, menuIds, requestLang);
		for (Map<String, Object> row : rowMaps) {
			long sid = longVal(row.get("shopmenu_id"));
			String translated = nameTranslations.get(sid);
			if (StringUtils.hasText(translated)) {
				row.put("name", translated);
				Map<String, Object> nameLang = new LinkedHashMap<>();
				nameLang.put(requestLang, translated);
				row.put("name_lang", nameLang);
			}
		}

		Map<Long, List<String>> menuRelIndex = loadMenuRelIndexForCompanyZero();
		return buildTreeAndFlatListFromSnakeRows(rowMaps, menuRelIndex);
	}

	private Map<Long, List<String>> loadMenuRelIndexForCompanyZero() {
		LambdaQueryWrapper<ShopMenuRelType> rw = new LambdaQueryWrapper<>();
		rw.eq(ShopMenuRelType::getCompanyId, 0);
		List<ShopMenuRelType> relRows = shopMenuRelTypeMapper.selectList(rw);
		Map<Long, List<String>> menuRelIndex = new LinkedHashMap<>();
		if (relRows != null) {
			for (ShopMenuRelType rel : relRows) {
				if (rel.getShopmenuId() == null) {
					continue;
				}
				String name = shopMenuService.menuTypeIdToName(rel.getMenuType());
				menuRelIndex.computeIfAbsent(rel.getShopmenuId(), k -> new ArrayList<>()).add(name);
			}
		}
		return menuRelIndex;
	}

	/**
	 * Shared tree and flat list construction: sort by pid/sort, build tree levels, flatten with parent_name /
	 * isChildrenMenu, apply menu_type from {@code menuRelIndex} (tree depth ≤2 rules match list items).
	 */
	static Map<String, Object> buildTreeAndFlatListFromSnakeRows(
			List<Map<String, Object>> rowMaps, Map<Long, List<String>> menuRelIndex) {
		List<Map<String, Object>> rows = new ArrayList<>();
		if (rowMaps != null) {
			for (Map<String, Object> r : rowMaps) {
				rows.add(new LinkedHashMap<>(r));
			}
		}
		if (rows.isEmpty()) {
			return Map.of("tree", List.of(), "list", List.of());
		}

		rows.sort(
				Comparator.comparing((Map<String, Object> m) -> longVal(m.get("pid")))
						.thenComparing(m -> intVal(m.get("sort"))));

		Map<Long, List<Map<String, Object>>> byPid = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			long p = longVal(row.get("pid"));
			byPid.computeIfAbsent(p, k -> new ArrayList<>()).add(row);
		}

		List<Map<String, Object>> menusList = new ArrayList<>();
		Map<Long, Boolean> isChildrenMenuMap = new LinkedHashMap<>();
		List<Map<String, Object>> tree = buildTreeLevel(0L, 0, byPid, menusList, isChildrenMenuMap);

		List<Map<String, Object>> flat = new ArrayList<>();
		Map<Long, Map<String, Object>> menuListById = new LinkedHashMap<>();
		for (Map<String, Object> row : menusList) {
			menuListById.put(longVal(row.get("shopmenu_id")), row);
		}
		for (Map<String, Object> row : menusList) {
			Map<String, Object> item = new LinkedHashMap<>(row);
			long pid = longVal(row.get("pid"));
			String parentName = "无";
			if (pid != 0L && menuListById.containsKey(pid)) {
				Object pn = menuListById.get(pid).get("name");
				if (pn != null) {
					parentName = pn.toString();
				}
			}
			item.put("parent_name", parentName);
			long sid = longVal(row.get("shopmenu_id"));
			item.put("isChildrenMenu", isChildrenMenuMap.getOrDefault(sid, Boolean.FALSE));
			flat.add(item);
		}

		Map<Long, List<String>> rel = menuRelIndex != null ? menuRelIndex : Map.of();
		applyMenuTypeToTree(tree, rel, 0);
		for (Map<String, Object> item : flat) {
			long sid = longVal(item.get("shopmenu_id"));
			item.put("menu_type", new ArrayList<>(rel.getOrDefault(sid, List.of("all"))));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("tree", tree);
		out.put("list", flat);
		return out;
	}

	/**
	 * Builds the subtree for rows grouped under {@code pid}. The first sibling at this parent increments {@code level}
	 * (so top-level visible rows get level 1); subsequent siblings reuse the same level. Each visited row is copied
	 * into {@code menusList} in depth-first preorder. Rows that are not shown are still listed but do not recurse;
	 * shown rows attach {@code children} and set {@code isChildrenMenu} when any child is a menu.
	 */
	private static List<Map<String, Object>> buildTreeLevel(
			long pid,
			int levelIn,
			Map<Long, List<Map<String, Object>>> byPid,
			List<Map<String, Object>> menusList,
			Map<Long, Boolean> isChildrenMenuMap) {
		List<Map<String, Object>> siblings = byPid.getOrDefault(pid, List.of());
		List<Map<String, Object>> out = new ArrayList<>();
		boolean started = false;
		int level = levelIn;
		for (Map<String, Object> row : siblings) {
			if (!started) {
				level++;
				started = true;
			}
			Map<String, Object> node = new LinkedHashMap<>(row);
			node.put("level", level);
			menusList.add(new LinkedHashMap<>(node));

			if (!truthyIsShow(row.get("is_show"))) {
				continue;
			}

			long sid = longVal(row.get("shopmenu_id"));
			List<Map<String, Object>> children = buildTreeLevel(sid, level, byPid, menusList, isChildrenMenuMap);
			if (!children.isEmpty()) {
				boolean isChildrenMenu = false;
				for (Map<String, Object> c : children) {
					if (isMenuTruthy(c.get("is_menu"))) {
						isChildrenMenu = true;
						break;
					}
				}
				node.put("isChildrenMenu", isChildrenMenu);
				isChildrenMenuMap.put(sid, isChildrenMenu);
				node.put("children", children);
			}
			out.add(node);
		}
		return out;
	}

	private static void applyMenuTypeToTree(
			List<Map<String, Object>> nodes, Map<Long, List<String>> menuRelIndex, int depth) {
		if (nodes == null || nodes.isEmpty()) {
			return;
		}
		for (Map<String, Object> node : nodes) {
			if (depth <= 2) {
				long id = longVal(node.get("shopmenu_id"));
				node.put("menu_type", new ArrayList<>(menuRelIndex.getOrDefault(id, List.of("all"))));
			}
			if (depth < 2) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
				if (children != null && !children.isEmpty()) {
					applyMenuTypeToTree(children, menuRelIndex, depth + 1);
				}
			}
		}
	}

	static Map<String, Object> toSnakeRow(ShopMenu m) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("shopmenu_id", m.getShopmenuId());
		map.put("company_id", m.getCompanyId());
		map.put("name", m.getName());
		map.put("url", m.getUrl());
		map.put("sort", m.getSort());
		map.put("is_menu", m.getIsMenu());
		map.put("pid", m.getPid());
		map.put("apis", m.getApis());
		map.put("icon", m.getIcon());
		map.put("is_show", m.getIsShow());
		map.put("alias_name", m.getAliasName());
		map.put("version", m.getVersion());
		map.put("disabled", m.getDisabled());
		map.put("created", m.getCreated());
		map.put("updated", m.getUpdated());
		return map;
	}

	static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static boolean truthyIsShow(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return false;
		}
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static boolean isMenuTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return "true".equalsIgnoreCase(v.toString().trim());
	}
}
