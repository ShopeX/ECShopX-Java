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

package cn.shopex.ecshopx.companys.service.permission;

import cn.shopex.ecshopx.companys.dto.PermissionTreeFilter;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PermissionShopMenuTreeService {

	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final ShopMenuService shopMenuService;
	private final OperatorRoleMenuAliasService operatorRoleMenuAliasService;
	private final CommonLangModReadService commonLangModReadService;

	public PermissionShopMenuTreeService(
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			ShopMenuService shopMenuService,
			OperatorRoleMenuAliasService operatorRoleMenuAliasService,
			CommonLangModReadService commonLangModReadService) {
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
		this.shopMenuService = shopMenuService;
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.commonLangModReadService = commonLangModReadService;
	}

	public List<Map<String, Object>> getPermissionTree(PermissionTreeFilter filter, String requestLang) {
		long effectiveCompanyId = resolveEffectiveCompanyId(filter.companyId(), filter.version());

		Map<String, Object> menuTypeMap = shopMenuService.getMenuTypeByCompanyId(filter.companyId());
		int menuTypeInt = parseMenuTypeInt(menuTypeMap);

		String ot = filter.operatorType();
		String otLc = ot == null || ot.isEmpty() ? "" : ot.toLowerCase(Locale.ROOT);
		if ("staff".equals(otLc) || "distributor".equals(otLc) || "dealer".equals(otLc)) {
			return buildTreeForRoleAliases(filter, requestLang, effectiveCompanyId, menuTypeInt);
		}
		return buildTreeFull(filter, requestLang, effectiveCompanyId, menuTypeInt);
	}

	/**
	 * Builds the menu tree for a role's stored {@code shopmenu_alias_name} set (ancestor closure, same pruning as
	 * {@link #getPermissionTree} for role-alias paths).
	 */
	public List<Map<String, Object>> getRolePermissionMenuTree(
			long companyId,
			int menuVersion,
			java.util.Collection<String> shopmenuAliasNames,
			String requestLang) {
		if (shopmenuAliasNames == null || shopmenuAliasNames.isEmpty()) {
			return List.of();
		}
		Set<String> aliasSet = new LinkedHashSet<>();
		for (String a : shopmenuAliasNames) {
			if (a != null) {
				String t = a.trim();
				if (!t.isEmpty()) {
					aliasSet.add(t);
				}
			}
		}
		if (aliasSet.isEmpty()) {
			return List.of();
		}

		long effectiveCompanyId = resolveEffectiveCompanyId(companyId, menuVersion);
		Map<String, Object> menuTypeMap = shopMenuService.getMenuTypeByCompanyId(companyId);
		int menuTypeInt = parseMenuTypeInt(menuTypeMap);

		List<ShopMenu> entities = loadShopMenuEntities(effectiveCompanyId, menuVersion);
		if (entities == null || entities.isEmpty()) {
			return List.of();
		}
		List<ShopMenu> subset = filterEntitiesByAliasClosure(entities, aliasSet);
		PermissionTreeFilter filter = new PermissionTreeFilter(companyId, menuVersion, "", 0L, false);
		return buildTreeFromEntities(subset, effectiveCompanyId, companyId, requestLang, menuTypeInt, filter);
	}

	private long resolveEffectiveCompanyId(long companyId, int version) {
		LambdaQueryWrapper<ShopMenu> w0 = new LambdaQueryWrapper<>();
		w0.eq(ShopMenu::getCompanyId, toCompanyIdInt(companyId))
				.eq(ShopMenu::getVersion, version)
				.eq(ShopMenu::getDisabled, false);
		Long count = shopMenuMapper.selectCount(w0);
		return (count == null || count <= 0) ? 0L : companyId;
	}

	private static int parseMenuTypeInt(Map<String, Object> menuTypeMap) {
		Object mt = menuTypeMap != null ? menuTypeMap.get("menu_type") : null;
		int menuTypeInt = 1;
		if (mt instanceof Number n) {
			menuTypeInt = n.intValue();
		} else if (mt != null) {
			try {
				menuTypeInt = Integer.parseInt(mt.toString().trim());
			} catch (NumberFormatException ignored) {
				menuTypeInt = 1;
			}
		}
		return menuTypeInt;
	}

	private List<ShopMenu> filterEntitiesByAliasClosure(List<ShopMenu> entities, Set<String> aliasSet) {
		if (entities == null || entities.isEmpty() || aliasSet == null || aliasSet.isEmpty()) {
			return List.of();
		}
		Map<Long, ShopMenu> byId =
				entities.stream()
						.filter(e -> e.getShopmenuId() != null)
						.collect(Collectors.toMap(ShopMenu::getShopmenuId, e -> e, (a, b) -> a));

		Set<Long> direct = new LinkedHashSet<>();
		for (ShopMenu e : entities) {
			String an = e.getAliasName();
			if (an != null && !an.isBlank() && aliasSet.contains(an.trim())) {
				if (e.getShopmenuId() != null) {
					direct.add(e.getShopmenuId());
				}
			}
		}

		Set<Long> closure = new LinkedHashSet<>();
		for (Long id : direct) {
			Long cur = id;
			while (cur != null && cur > 0) {
				if (!closure.add(cur)) {
					break;
				}
				ShopMenu row = byId.get(cur);
				if (row == null) {
					break;
				}
				Long pid = row.getPid();
				if (pid == null || pid <= 0) {
					break;
				}
				cur = pid;
			}
		}

		return entities.stream()
				.filter(e -> e.getShopmenuId() != null && closure.contains(e.getShopmenuId()))
				.sorted(
						Comparator.comparing((ShopMenu e) -> e.getPid() == null ? 0L : e.getPid())
								.thenComparing(e -> e.getSort() == null ? 0 : e.getSort()))
				.toList();
	}

	private List<Map<String, Object>> buildTreeForRoleAliases(
			PermissionTreeFilter filter,
			String requestLang,
			long effectiveCompanyId,
			int menuTypeInt) {
		List<String> shopmenuAliasName =
				operatorRoleMenuAliasService.listShopMenuAliases(filter.companyId(), filter.operatorId());
		String roleOt = filter.operatorType();
		String roleOtLc = roleOt == null || roleOt.isEmpty() ? "" : roleOt.toLowerCase(Locale.ROOT);
		if (shopmenuAliasName == null && "staff".equals(roleOtLc)) {
			throw new ResourceException("帐号没有绑定角色，请联系管理员添加");
		}
		if (shopmenuAliasName == null
				&& ("distributor".equals(roleOtLc) || "dealer".equals(roleOtLc))) {
			return buildTreeFull(filter, requestLang, effectiveCompanyId, menuTypeInt);
		}
		Set<String> aliasSet = new LinkedHashSet<>();
		if (shopmenuAliasName != null) {
			for (String a : shopmenuAliasName) {
				if (a != null) {
					aliasSet.add(a.trim());
				}
			}
		}

		List<ShopMenu> entities = loadShopMenuEntities(effectiveCompanyId, filter.version());
		if (entities == null || entities.isEmpty()) {
			return List.of();
		}

		List<ShopMenu> subset = filterEntitiesByAliasClosure(entities, aliasSet);

		return buildTreeFromEntities(subset, effectiveCompanyId, filter.companyId(), requestLang, menuTypeInt, filter);
	}

	private List<Map<String, Object>> buildTreeFull(
			PermissionTreeFilter filter,
			String requestLang,
			long effectiveCompanyId,
			int menuTypeInt) {
		List<ShopMenu> entities = loadShopMenuEntities(effectiveCompanyId, filter.version());
		if (entities == null || entities.isEmpty()) {
			return List.of();
		}
		return buildTreeFromEntities(entities, effectiveCompanyId, filter.companyId(), requestLang, menuTypeInt, filter);
	}

	private List<ShopMenu> loadShopMenuEntities(long effectiveCompanyId, int version) {
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.eq(ShopMenu::getCompanyId, toCompanyIdInt(effectiveCompanyId))
				.eq(ShopMenu::getDisabled, false)
				.eq(ShopMenu::getVersion, version)
				.orderByAsc(ShopMenu::getPid, ShopMenu::getSort)
				.last("LIMIT 1000");
		return shopMenuMapper.selectList(w);
	}

	private List<Map<String, Object>> buildTreeFromEntities(
			List<ShopMenu> entities,
			long effectiveCompanyId,
			long langLookupCompanyId,
			String requestLang,
			int menuTypeInt,
			PermissionTreeFilter filter) {
		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (ShopMenu sm : entities) {
			rowMaps.add(toSnakeRow(sm));
		}
		enrichShopMenuNames(rowMaps, entities, effectiveCompanyId, langLookupCompanyId, requestLang);

		Map<Long, List<String>> menuRelIndex = loadMenuRelIndex(toCompanyIdInt(effectiveCompanyId));

		boolean emptyChildArrays = filter.serializeEmptyChildrenArrays();
		List<Map<String, Object>> tree = buildTreeFromSnakeRowsOnly(rowMaps, menuRelIndex, emptyChildArrays);
		applyMenuTypeToTree(tree, menuRelIndex, 0);
		helperFilterSubMenuType(tree, menuTypeInt, emptyChildArrays);
		return tree;
	}

	private void enrichShopMenuNames(
			List<Map<String, Object>> rowMaps,
			List<ShopMenu> entities,
			long effectiveCompanyId,
			long langLookupCompanyId,
			String requestLang) {
		List<Long> menuIds = new ArrayList<>(entities.size());
		for (ShopMenu sm : entities) {
			if (sm.getShopmenuId() != null) {
				menuIds.add(sm.getShopmenuId());
			}
		}
		Map<Long, String> nameTranslations =
				new LinkedHashMap<>(
						commonLangModReadService.findShopMenuNamesByLocale(effectiveCompanyId, menuIds, requestLang));
		if (langLookupCompanyId != effectiveCompanyId && langLookupCompanyId > 0) {
			Map<Long, String> extra =
					commonLangModReadService.findShopMenuNamesByLocale(
							langLookupCompanyId, menuIds, requestLang);
			for (Map.Entry<Long, String> e : extra.entrySet()) {
				nameTranslations.putIfAbsent(e.getKey(), e.getValue());
			}
		}
		for (int i = 0; i < rowMaps.size(); i++) {
			Map<String, Object> row = rowMaps.get(i);
			ShopMenu sm = entities.get(i);
			long sid = longVal(row.get("shopmenu_id"));
			String translated = nameTranslations.get(sid);
			String dbName = sm.getName() != null ? sm.getName() : "";
			if (StringUtils.hasText(translated)) {
				row.put("name", translated);
			}
			Map<String, Object> nameLang = new LinkedHashMap<>();
			nameLang.put("zh-CN", dbName);
			if (StringUtils.hasText(translated) && StringUtils.hasText(requestLang)) {
				nameLang.put(canonicalRequestLangTag(requestLang), translated);
			}
			row.put("name_lang", nameLang);
		}
	}

	private Map<Long, List<String>> loadMenuRelIndex(int companyIdInt) {
		LambdaQueryWrapper<ShopMenuRelType> rw = new LambdaQueryWrapper<>();
		rw.eq(ShopMenuRelType::getCompanyId, companyIdInt);
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

	private List<Map<String, Object>> buildTreeFromSnakeRowsOnly(
			List<Map<String, Object>> rowMaps, Map<Long, List<String>> menuRelIndex, boolean serializeEmptyChildrenArrays) {
		List<Map<String, Object>> rows = new ArrayList<>();
		if (rowMaps != null) {
			for (Map<String, Object> r : rowMaps) {
				rows.add(new LinkedHashMap<>(r));
			}
		}
		if (rows.isEmpty()) {
			return List.of();
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
		return buildTreeLevel(0L, 0, byPid, menusList, isChildrenMenuMap, serializeEmptyChildrenArrays);
	}

	private static List<Map<String, Object>> buildTreeLevel(
			long pid,
			int levelIn,
			Map<Long, List<Map<String, Object>>> byPid,
			List<Map<String, Object>> menusList,
			Map<Long, Boolean> isChildrenMenuMap,
			boolean serializeEmptyChildrenArrays) {
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
			List<Map<String, Object>> children =
					buildTreeLevel(sid, level, byPid, menusList, isChildrenMenuMap, serializeEmptyChildrenArrays);
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
			} else if (serializeEmptyChildrenArrays) {
				node.put("children", new ArrayList<>());
			}
			out.add(node);
		}
		return out;
	}

	private void applyMenuTypeToTree(
			List<Map<String, Object>> nodes, Map<Long, List<String>> menuRelIndex, int depth) {
		if (nodes == null || nodes.isEmpty()) {
			return;
		}
		Map<Long, List<String>> rel = menuRelIndex != null ? menuRelIndex : Map.of();
		for (Map<String, Object> node : nodes) {
			if (depth <= 2) {
				long id = longVal(node.get("shopmenu_id"));
				node.put("menu_type", new ArrayList<>(rel.getOrDefault(id, List.of("all"))));
			}
			if (depth < 2) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
				if (children != null && !children.isEmpty()) {
					applyMenuTypeToTree(children, rel, depth + 1);
				}
			}
		}
	}

	private void helperFilterSubMenuType(
			List<Map<String, Object>> tree, int menuTypeInt, boolean serializeEmptyChildrenArrays) {
		if (menuTypeInt == 0 || menuTypeInt == 1) {
			return;
		}
		String menuTypeStr = shopMenuService.menuTypeIdToName(menuTypeInt);
		filterMenuTypeAtLevel(tree, menuTypeStr, serializeEmptyChildrenArrays);
	}

	/**
	 * Mirrors legacy menu-type pruning: each level (including roots) drops nodes whose {@code menu_type} list
	 * contains neither {@code all} nor the company's type string; nodes without a {@code menu_type} key are kept.
	 */
	private void filterMenuTypeAtLevel(
			List<Map<String, Object>> level, String menuTypeStr, boolean serializeEmptyChildrenArrays) {
		if (level == null || level.isEmpty()) {
			return;
		}
		level.removeIf(node -> shouldRemoveNodeForMenuType(node, menuTypeStr));
		for (Map<String, Object> node : level) {
			Object chObj = node.get("children");
			if (!(chObj instanceof List<?>)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) chObj;
			filterMenuTypeAtLevel(children, menuTypeStr, serializeEmptyChildrenArrays);
			if (children.isEmpty()) {
				if (serializeEmptyChildrenArrays) {
					node.put("children", new ArrayList<>());
				} else {
					node.remove("children");
				}
			}
		}
	}

	private static boolean shouldRemoveNodeForMenuType(Map<String, Object> item, String menuTypeStr) {
		if (!item.containsKey("menu_type")) {
			return false;
		}
		Object mtObj = item.get("menu_type");
		if (!(mtObj instanceof List<?> rawList)) {
			return false;
		}
		List<String> mt = new ArrayList<>(rawList.size());
		for (Object o : rawList) {
			mt.add(o == null ? "" : o.toString());
		}
		if ("all".equals(menuTypeStr)) {
			return false;
		}
		boolean hasKey = mt.contains(menuTypeStr);
		boolean hasAll = mt.contains("all");
		return !hasKey && !hasAll;
	}

	private static Map<String, Object> toSnakeRow(ShopMenu m) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("shopmenu_id", m.getShopmenuId());
		map.put("company_id", m.getCompanyId());
		map.put("name", m.getName());
		map.put("url", m.getUrl());
		map.put("sort", m.getSort());
		map.put("is_menu", m.getIsMenu());
		map.put("pid", m.getPid());
		map.put("apis", normalizeApisColumn(m.getApis()));
		map.put("icon", m.getIcon());
		map.put("is_show", m.getIsShow());
		map.put("alias_name", m.getAliasName());
		map.put("version", m.getVersion());
		map.put("disabled", m.getDisabled());
		map.put("created", m.getCreated());
		map.put("updated", m.getUpdated());
		return map;
	}

	private static long longVal(Object o) {
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

	private static int toCompanyIdInt(long companyId) {
		if (companyId > Integer.MAX_VALUE || companyId < Integer.MIN_VALUE) {
			throw new ResourceException("登录验证错误");
		}
		return (int) companyId;
	}

	private static String normalizeApisColumn(String apis) {
		if (apis == null) {
			return null;
		}
		String t = apis.trim();
		if (t.isEmpty()) {
			return null;
		}
		return apis;
	}

	private static String canonicalRequestLangTag(String requestLang) {
		String t = requestLang.trim();
		if (t.equalsIgnoreCase("zh-CN")) {
			return "zh-CN";
		}
		if (t.equalsIgnoreCase("en-CN")) {
			return "en-CN";
		}
		if (t.equalsIgnoreCase("ar-SA")) {
			return "ar-SA";
		}
		return t;
	}
}
