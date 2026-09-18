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

package cn.shopex.ecshopx.superadmin.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopMenuService {

	private static final Map<String, Integer> PRODUCT_TO_MENU_TYPE = new HashMap<>();

	static {
		PRODUCT_TO_MENU_TYPE.put("all", 1);
		PRODUCT_TO_MENU_TYPE.put("b2c", 2);
		PRODUCT_TO_MENU_TYPE.put("platform", 3);
		PRODUCT_TO_MENU_TYPE.put("standard", 4);
		PRODUCT_TO_MENU_TYPE.put("in_purchase", 5);
	}

	private static final Map<Integer, String> MENU_TYPE_TO_STR = new HashMap<>();

	static {
		MENU_TYPE_TO_STR.put(1, "all");
		MENU_TYPE_TO_STR.put(2, "b2c");
		MENU_TYPE_TO_STR.put(3, "platform");
		MENU_TYPE_TO_STR.put(4, "standard");
		MENU_TYPE_TO_STR.put(5, "in_purchase");
	}

	private final JdbcTemplate jdbcTemplate;
	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;

	@Value("${common.system-is-saas:false}")
	private boolean systemIsSaas;

	@Value("${common.product-model:platform}")
	private String productModel;

	public ShopMenuService(
			JdbcTemplate jdbcTemplate,
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
	}

	/**
	 * 合并菜单行中的 apis 字段（空格分隔）为去重后的路由别名列表。
	 */
	public List<String> collectApisFromMenus(int menuVersion, List<String> aliasNames) {
		if (aliasNames == null || aliasNames.isEmpty()) {
			return collectApisFromMenusUnrestricted(menuVersion);
		}
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.eq(ShopMenu::getVersion, menuVersion).eq(ShopMenu::getDisabled, false).in(ShopMenu::getAliasName, aliasNames);
		List<ShopMenu> rows = shopMenuMapper.selectList(w);
		Set<String> apis = new LinkedHashSet<>();
		for (ShopMenu row : rows) {
			mergeApisFieldToSet(row.getApis(), apis);
		}
		return new ArrayList<>(apis);
	}

	/**
	 * Aggregates {@code shop_menu.apis} for dashboard statistics: when alias restriction is empty, loads up to 1000
	 * rows by version only (same cap as legacy list pagination).
	 */
	public List<String> collectApisForStatisticsApp(int menuVersion, List<String> shopmenuAliasNamesRestrict) {
		if (shopmenuAliasNamesRestrict != null && !shopmenuAliasNamesRestrict.isEmpty()) {
			return collectApisFromMenus(menuVersion, shopmenuAliasNamesRestrict);
		}
		return collectApisFromMenusUnrestricted(menuVersion);
	}

	private List<String> collectApisFromMenusUnrestricted(int menuVersion) {
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.eq(ShopMenu::getVersion, menuVersion).eq(ShopMenu::getDisabled, false).last("LIMIT 1000");
		List<ShopMenu> rows = shopMenuMapper.selectList(w);
		Set<String> apis = new LinkedHashSet<>();
		for (ShopMenu row : rows) {
			mergeApisFieldToSet(row.getApis(), apis);
		}
		return new ArrayList<>(apis);
	}

	private static void mergeApisFieldToSet(String rawApis, Set<String> sink) {
		if (!StringUtils.hasText(rawApis)) {
			return;
		}
		String flat = rawApis.replace("\r", " ").replace("\n", " ");
		for (String part : flat.split("\\s+")) {
			if (StringUtils.hasText(part)) {
				sink.add(part.trim());
			}
		}
	}

	/**
	 * 将 {@code shop_menu_rel_type.menu_type} 整型 id 转为菜单类型展示名；未知或 {@code null} 时为 {@code all}。
	 */
	public String menuTypeIdToName(Integer menuTypeId) {
		if (menuTypeId == null) {
			return "all";
		}
		return MENU_TYPE_TO_STR.getOrDefault(menuTypeId, "all");
	}

	/**
	 * 将菜单类型名称列表解析为 menu_type 整型 id；空列表等价于仅 {@code all}。
	 */
	public List<Integer> menuTypeNamesToIds(List<String> menuTypeNames) {
		if (menuTypeNames == null || menuTypeNames.isEmpty()) {
			return List.of(1);
		}
		LinkedHashSet<Integer> ordered = new LinkedHashSet<>();
		for (String raw : menuTypeNames) {
			if (raw == null) {
				ordered.add(1);
				continue;
			}
			String name = raw.trim();
			if (name.isEmpty()) {
				ordered.add(1);
				continue;
			}
			ordered.add(PRODUCT_TO_MENU_TYPE.getOrDefault(name, 1));
		}
		return new ArrayList<>(ordered);
	}

	/**
	 * 校验 alias_name 是否与已有菜单在 menu_type 上冲突（对齐 PHP {@code ShopMenuService::create/updateMenus}）。
	 * <p>PHP {@code getInfo} 只取一条；更新时若命中自身则跳过。库内存在同名多行时，若当前菜单本身已持有该
	 * alias 则直接通过（避免扫其它同名行误伤）；否则只与 {@code shopmenu_id} 最小的一条比对。
	 *
	 * @param excludeShopmenuId 更新时排除自身；创建时传 {@code null}
	 */
	public void assertAliasNameNotConflict(
			String aliasName,
			int version,
			int companyId,
			Long excludeShopmenuId,
			List<String> menuTypeNames) {
		if (!StringUtils.hasText(aliasName)) {
			return;
		}
		// 更新且自身已是该 alias：与 PHP getInfo 命中自身一致，不做冲突校验
		if (excludeShopmenuId != null) {
			LambdaQueryWrapper<ShopMenu> selfW = new LambdaQueryWrapper<>();
			selfW.eq(ShopMenu::getShopmenuId, excludeShopmenuId)
					.eq(ShopMenu::getAliasName, aliasName)
					.eq(ShopMenu::getVersion, version)
					.eq(ShopMenu::getCompanyId, companyId);
			if (shopMenuMapper.selectOne(selfW) != null) {
				return;
			}
		}
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.eq(ShopMenu::getAliasName, aliasName)
				.eq(ShopMenu::getVersion, version)
				.eq(ShopMenu::getCompanyId, companyId)
				.orderByAsc(ShopMenu::getShopmenuId)
				.last("LIMIT 1");
		ShopMenu info = shopMenuMapper.selectOne(w);
		if (info == null || info.getShopmenuId() == null) {
			return;
		}
		LambdaQueryWrapper<ShopMenuRelType> relW = new LambdaQueryWrapper<>();
		relW.eq(ShopMenuRelType::getCompanyId, companyId)
				.eq(ShopMenuRelType::getShopmenuId, info.getShopmenuId());
		List<ShopMenuRelType> relRows = shopMenuRelTypeMapper.selectList(relW);
		List<Integer> relIdSet =
				relRows.stream().map(ShopMenuRelType::getMenuType).collect(Collectors.toList());
		List<Integer> compareTypeSet = menuTypeNamesToIds(menuTypeNames);
		if (checkDuplicateType(relIdSet, compareTypeSet)) {
			throw new ResourceException("已经相同的菜单唯一标识");
		}
	}

	/**
	 * 判断两组菜单类型是否冲突：任一侧包含 {@code all}（id 1），或两集合存在相同类型 id，则视为重复。
	 */
	public boolean checkDuplicateType(List<Integer> typeSet, List<Integer> compareTypeSet) {
		Set<Integer> a = new HashSet<>();
		if (typeSet != null) {
			a.addAll(typeSet);
		}
		Set<Integer> b = new HashSet<>();
		if (compareTypeSet != null) {
			b.addAll(compareTypeSet);
		}
		if (a.contains(1) || b.contains(1)) {
			return true;
		}
		a.retainAll(b);
		return !a.isEmpty();
	}

	/**
	 * 校验子菜单类型集合是否在父菜单类型范围内：父级含 {@code all} 时跳过；父级无类型记录时视为未限制也跳过；
	 * 子级含 {@code all} 而父级不含时报错；否则子级每个类型 id 须出现在父级集合中。
	 */
	public void checkParentMenuType(List<Integer> parentTypeList, List<Integer> sonTypeList) {
		List<Integer> parent = parentTypeList != null ? parentTypeList : List.of();
		List<Integer> son = sonTypeList != null ? sonTypeList : List.of();
		if (parent.isEmpty() || parent.contains(1)) {
			return;
		}
		if (son.contains(1)) {
			throw new ResourceException("子类菜单类型范围不能超过父类");
		}
		Set<Integer> pset = new HashSet<>(parent);
		for (Integer s : son) {
			if (s != null && !pset.contains(s)) {
				throw new ResourceException("子类菜单类型需在父类范围内");
			}
		}
	}

	/**
	 * Resolves the product-model key used for feature whitelists (e.g. SMS scene names): reads {@code companys.menu_type}
	 * when present and non-zero, maps via the same integer → string table as {@link #menuTypeIdToName}, otherwise uses
	 * {@code common.product-model}.
	 * Unlike {@link #getMenuTypeByCompanyId}, this always queries the database to read
	 * {@code menu_type} regardless of {@code common.system-is-saas}.
	 */
	public String resolveProductModelKeyForCompany(Long companyId) {
		String fallback = productModel != null ? productModel.trim() : "platform";
		if (fallback.isEmpty()) {
			fallback = "platform";
		}
		if (companyId == null) {
			return fallback;
		}
		List<Integer> rows = jdbcTemplate.query(
				"SELECT menu_type FROM companys WHERE company_id = ? LIMIT 1",
				(rs, rowNum) -> {
					int v = rs.getInt("menu_type");
					return rs.wasNull() ? null : v;
				},
				companyId);
		if (rows.isEmpty()) {
			return fallback;
		}
		Integer menuType = rows.get(0);
		if (menuType == null || menuType == 0) {
			return fallback;
		}
		return MENU_TYPE_TO_STR.getOrDefault(menuType, fallback);
	}

	/**
	 * Maps product menu key (e.g. {@code platform}) to {@code companys.menu_type} integer using the inverse of
	 * {@link #MENU_TYPE_TO_STR}.
	 */
	public int productModelKeyToMenuTypeInt(String menuTypeKey) {
		String fallback = productModel != null && !productModel.isBlank() ? productModel.trim() : "platform";
		String key = menuTypeKey != null && !menuTypeKey.isBlank() ? menuTypeKey.trim() : fallback;
		return PRODUCT_TO_MENU_TYPE.getOrDefault(key, PRODUCT_TO_MENU_TYPE.getOrDefault(fallback, 3));
	}

	public Map<String, Object> getMenuTypeByCompanyId(Long companyId) {
		Map<String, Object> out = new HashMap<>();
		if (!systemIsSaas) {
			String pm = productModel != null ? productModel : "platform";
			int mt = PRODUCT_TO_MENU_TYPE.getOrDefault(pm, 3);
			out.put("menu_type", mt);
			out.put("menu_type_str", MENU_TYPE_TO_STR.getOrDefault(mt, pm));
			return out;
		}
		if (companyId == null) {
			return out;
		}
		List<Integer> rows = jdbcTemplate.query(
				"SELECT menu_type FROM companys WHERE company_id = ? LIMIT 1",
				(rs, rowNum) -> rs.getInt("menu_type"),
				companyId);
		if (rows.isEmpty()) {
			return out;
		}
		Integer menuType = rows.get(0);
		if (menuType == null || menuType == 0) {
			String pm = productModel != null ? productModel : "platform";
			int mt = PRODUCT_TO_MENU_TYPE.getOrDefault(pm, 3);
			out.put("menu_type", mt);
			out.put("menu_type_str", MENU_TYPE_TO_STR.getOrDefault(mt, pm));
			return out;
		}
		out.put("menu_type", menuType);
		out.put("menu_type_str", MENU_TYPE_TO_STR.getOrDefault(menuType, productModel));
		return out;
	}
}
