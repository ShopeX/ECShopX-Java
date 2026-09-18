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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.mapper.ItemsCategoryMapper;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.ItemsCategoryRowMaps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsCategoryRepository {

	private static final Logger log = LoggerFactory.getLogger(ItemsCategoryRepository.class);

	private final ItemsCategoryMapper mapper;
	private final ItemsCategoryDistributorIdResolver distributorIdResolver;

	public ItemsCategoryRepository(ItemsCategoryMapper mapper,
			ItemsCategoryDistributorIdResolver distributorIdResolver) {
		this.mapper = mapper;
		this.distributorIdResolver = distributorIdResolver;
	}

	/**
	 * 按 company_id + category_id 查单条（详情接口等）。
	 */
	public Optional<ItemsCategory> findEntityByCompanyAndCategoryId(long companyId, long categoryId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getCategoryId, categoryId).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	/**
	 * 主类目：按公司、父级、名称、主类目标记、店铺维度（通常为 0）解析下一级类目。
	 */
	public ItemsCategory findMainCategoryChild(long companyId, long parentId, String categoryName, long distributorId) {
		if (categoryName == null || categoryName.isEmpty()) {
			return null;
		}
		String trimmed = categoryName.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId)
				.eq(ItemsCategory::getParentId, parentId)
				.eq(ItemsCategory::getIsMainCategory, true)
				.eq(ItemsCategory::getDistributorId, distributorId)
				.eq(ItemsCategory::getCategoryName, trimmed)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public List<ItemsCategory> listNonMainByCompanyDistributorAndNames(long companyId, long distributorId, Collection<String> names) {
		if (names == null || names.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId)
				.eq(ItemsCategory::getDistributorId, distributorId)
				.eq(ItemsCategory::getIsMainCategory, false)
				.in(ItemsCategory::getCategoryName, names);
		return mapper.selectList(w);
	}

	public List<ItemsCategory> listByCompanyAndCategoryIdIn(long companyId, Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).in(ItemsCategory::getCategoryId, categoryIds);
		return mapper.selectList(w);
	}

	public List<ItemsCategory> listByCompanyAndParentId(long companyId, long parentId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getParentId, parentId).orderByDesc(ItemsCategory::getSort).orderByAsc(ItemsCategory::getCreated);
		return mapper.selectList(w);
	}

	/**
	 * 平面创建分类：同公司、同父级、同主类目标记、同店铺维度下的重名检测。
	 */
	public Optional<ItemsCategory> findDuplicateForClassification(long companyId, String categoryName, long parentId, boolean isMainCategory,
			long distributorId) {
		if (categoryName == null) {
			return Optional.empty();
		}
		String trimmed = categoryName.trim();
		if (trimmed.isEmpty()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getParentId, parentId).eq(ItemsCategory::getIsMainCategory, isMainCategory)
				.eq(ItemsCategory::getDistributorId, distributorId).eq(ItemsCategory::getCategoryName, trimmed).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public Optional<ItemsCategory> getByCategoryCode(String categoryCode, long companyId) {
		if (categoryCode == null || categoryCode.isEmpty()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCategoryCode, categoryCode).eq(ItemsCategory::getCompanyId, companyId).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public void insert(ItemsCategory entity) {
		mapper.insert(entity);
	}

	public void updateOneBy(ItemsCategory patch, Long categoryId, Long companyId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCategoryId, categoryId).eq(ItemsCategory::getCompanyId, companyId);
		ItemsCategory existing = mapper.selectOne(w);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		patch.setCategoryId(categoryId);
		patch.setCompanyId(companyId);
		patch.setCreated(existing.getCreated());
		patch.setUpdated((int) (System.currentTimeMillis() / 1000L));
		mapper.updateById(patch);
	}

	/**
	 * 管理端分类局部更新：仅对 {@code applySets} 中显式 {@code set} 的列写入，避免实体字段默认值污染其它列。
	 */
	public void updateAdminCategoryPartial(long companyId, long categoryId,
			Consumer<LambdaUpdateWrapper<ItemsCategory>> applySets) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCategoryId, categoryId).eq(ItemsCategory::getCompanyId, companyId);
		ItemsCategory existing = mapper.selectOne(w);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		LambdaUpdateWrapper<ItemsCategory> u = new LambdaUpdateWrapper<>();
		u.eq(ItemsCategory::getCategoryId, categoryId).eq(ItemsCategory::getCompanyId, companyId);
		applySets.accept(u);
		u.set(ItemsCategory::getUpdated, (int) (System.currentTimeMillis() / 1000L));
		int n = mapper.update(null, u);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public void updatePath(Long categoryId, Long companyId, String path) {
		LambdaUpdateWrapper<ItemsCategory> u = new LambdaUpdateWrapper<>();
		u.eq(ItemsCategory::getCategoryId, categoryId).eq(ItemsCategory::getCompanyId, companyId).set(ItemsCategory::getPath, path);
		int n = mapper.update(null, u);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	/**
	 * 列表查询（全量树数据源）：在 filter 上应用 distributor 规则后查询。
	 */
	public List<Map<String, Object>> lists(Map<String, Object> filter, long companyId, long jwtDistributorId, int page,
			int pageSize) {
		LinkedHashMap<String, Object> f = copyFilter(filter);
		applyGetDistributorIdEquivalent(f, companyId, jwtDistributorId);
		QueryWrapper<ItemsCategory> w = filterToWrapper(f);
		w.orderByDesc("sort").orderByAsc("created");
		if (pageSize > 0) {
			long offset = (long) (page - 1) * pageSize;
			w.last("LIMIT " + offset + "," + pageSize);
		}
		return mapper.selectList(w).stream().map(ItemsCategoryRowMaps::fromListEntity).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * 店铺推广分类列表：可选跳过平台 JWT 分销商合并，排序与 {@link #lists} 一致。
	 */
	public List<Map<String, Object>> listsForPromoterCategory(Map<String, Object> filter, long companyId, long jwtDistributorId,
			int page, int pageSize) {
		LinkedHashMap<String, Object> f = copyFilter(filter);
		if (Boolean.TRUE.equals(f.get("promoter_skip_jwt_distributor_merge"))) {
			applyGetDistributorIdEquivalentForPromoterSkipJwt(f, companyId, jwtDistributorId);
		} else {
			applyGetDistributorIdEquivalent(f, companyId, jwtDistributorId);
		}
		f.remove("promoter_skip_jwt_distributor_merge");
		QueryWrapper<ItemsCategory> w = filterToWrapper(f);
		w.orderByDesc("sort").orderByAsc("created");
		if (pageSize > 0) {
			long offset = (long) (page - 1) * pageSize;
			w.last("LIMIT " + offset + "," + pageSize);
		}
		return mapper.selectList(w).stream().map(ItemsCategoryRowMaps::fromListEntity).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * 小程序子分类列表：与 lists 相同过滤链，单列按创建时间倒序，并返回 total_count。
	 */
	public Map<String, Object> listsWxappChildrenCategoriesWithTotalCount(Map<String, Object> filter, long companyId,
			long jwtDistributorId, int page, int pageSize) {
		LinkedHashMap<String, Object> f = copyFilter(filter);
		applyGetDistributorIdEquivalent(f, companyId, jwtDistributorId);
		QueryWrapper<ItemsCategory> w = filterToWrapper(f);
		long total = mapper.selectCount(w);
		w.orderByDesc("created");
		if (pageSize > 0) {
			long offset = (long) (page - 1) * pageSize;
			w.last("LIMIT " + offset + "," + pageSize);
		}
		List<Map<String, Object>> list = mapper.selectList(w).stream().map(ItemsCategoryRowMaps::fromListEntity)
				.collect(Collectors.toCollection(ArrayList::new));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	/**
	 * 小程序「等级类目」平面列表：与 {@link #lists} 相同过滤链，仅按 {@code created} 倒序，并返回 total_count。
	 */
	public Map<String, Object> listsWxappLevelCategoryWithTotalCount(Map<String, Object> filter, long companyId,
			long jwtDistributorId, int page, int pageSize) {
		LinkedHashMap<String, Object> f = copyFilter(filter);
		applyGetDistributorIdEquivalent(f, companyId, jwtDistributorId);
		QueryWrapper<ItemsCategory> w = filterToWrapper(f);
		if (log.isDebugEnabled()) {
			log.debug("listsWxappLevelCategoryWithTotalCount companyId={} filterKeys={}", companyId, f.keySet());
		}
		long total = mapper.selectCount(w);
		w.orderByDesc("created");
		if (pageSize > 0) {
			long offset = (long) (page - 1) * pageSize;
			w.last("LIMIT " + offset + "," + pageSize);
		}
		List<Map<String, Object>> list = mapper.selectList(w).stream().map(ItemsCategoryRowMaps::fromListEntity)
				.collect(Collectors.toCollection(ArrayList::new));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	/**
	 * 单层列表（带 has_children），filter 须含 parent_id 或 category_level 键时再调用。
	 */
	public List<Map<String, Object>> getSingleLevelList(Map<String, Object> filter, long companyId, long jwtDistributorId,
			int page, int pageSize) {
		LinkedHashMap<String, Object> f = copyFilter(filter);
		applyGetDistributorIdEquivalent(f, companyId, jwtDistributorId);
		int isMain = mainCategoryToInt(f.get("is_main_category"));
		long dist = toLong(f.get("distributor_id"));
		boolean parentPresent = f.containsKey("parent_id");
		String parentRaw = parentPresent ? String.valueOf(f.get("parent_id")) : "";
		boolean levelPresent = f.containsKey("category_level");
		String levelRaw = levelPresent ? String.valueOf(f.get("category_level")) : "";
		long offset = pageSize > 0 ? (long) (page - 1) * pageSize : 0L;
		int limit = pageSize > 0 ? pageSize : 0;
		boolean isShowFrontPresent = f.containsKey("is_show_front");
		int isShowFront = parseIsShowFrontInt(f.get("is_show_front"));
		List<Map<String, Object>> raw = mapper.selectSingleLevelList(companyId, isMain, dist, parentPresent, parentRaw,
				levelPresent, levelRaw, offset, limit, pageSize > 0, isShowFrontPresent, isShowFront);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : raw) {
			out.add(ItemsCategoryRowMaps.fromFlexibleMap(row));
		}
		return out;
	}

	private static LinkedHashMap<String, Object> copyFilter(Map<String, Object> filter) {
		return new LinkedHashMap<>(filter);
	}

	/** 在进入列表 SQL 前调整 {@code distributor_id}（无分类维度键时按主类目与平台模式合并）。 */
	public void applyGetDistributorIdEquivalent(Map<String, Object> filter, long companyId, long jwtDistributorId) {
		if (filter.containsKey("category_id") || filter.containsKey("category_id_in")
				|| filter.containsKey("parent_id") || filter.containsKey("parent_id_in")) {
			return;
		}
		if (isTruthyMainCategory(filter.get("is_main_category"))) {
			filter.put("distributor_id", 0L);
			return;
		}
		if (hasNonEmptyDistributorIdIn(filter)) {
			return;
		}
		if ("platform".equals(distributorIdResolver.resolveProductModel(companyId)) && jwtDistributorId != 0L) {
			filter.put("distributor_id", jwtDistributorId);
		}
	}

	/**
	 * 与 {@link #applyGetDistributorIdEquivalent} 相同，但不写入平台 JWT 的 {@code distributor_id}。
	 */
	private void applyGetDistributorIdEquivalentForPromoterSkipJwt(Map<String, Object> filter, long companyId, long jwtDistributorId) {
		if (filter.containsKey("category_id") || filter.containsKey("category_id_in")
				|| filter.containsKey("parent_id") || filter.containsKey("parent_id_in")) {
			return;
		}
		if (isTruthyMainCategory(filter.get("is_main_category"))) {
			filter.put("distributor_id", 0L);
			return;
		}
		if (hasNonEmptyDistributorIdIn(filter)) {
			return;
		}
	}

	private static boolean hasNonEmptyDistributorIdIn(Map<String, Object> filter) {
		if (!filter.containsKey("distributor_id_in")) {
			return false;
		}
		Object v = filter.get("distributor_id_in");
		return v instanceof Collection<?> coll && !coll.isEmpty();
	}

	private static boolean isTruthyMainCategory(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return false;
	}

	private static int mainCategoryToInt(Object v) {
		return isTruthyMainCategory(v) ? 1 : 0;
	}

	private static QueryWrapper<ItemsCategory> filterToWrapper(Map<String, Object> filter) {
		QueryWrapper<ItemsCategory> w = new QueryWrapper<>();
		putEq(w, filter, "company_id");
		putEq(w, filter, "is_main_category");
		if (!applyDistributorIdInFilter(w, filter)) {
			putEq(w, filter, "distributor_id");
		}
		putEq(w, filter, "category_level");
		putEq(w, filter, "category_name");
		applyParentIdInFilter(w, filter);
		putEq(w, filter, "parent_id");
		applyCustomizePageIdInFilter(w, filter);
		applyCategoryIdInFilter(w, filter);
		applyCategoryIdScalarFilter(w, filter);
		if (filter.containsKey("is_show_front")) {
			w.eq("is_show_front", parseIsShowFrontInt(filter.get("is_show_front")));
		}
		return w;
	}

	/**
	 * {@code distributor_id_in} 非空时展开为 IN 条件，且不再对标量 {@code distributor_id} 使用 eq。
	 *
	 * @return 是否已应用 IN 条件
	 */
	private static boolean applyDistributorIdInFilter(QueryWrapper<ItemsCategory> w, Map<String, Object> filter) {
		if (!filter.containsKey("distributor_id_in")) {
			return false;
		}
		Object v = filter.get("distributor_id_in");
		if (!(v instanceof Collection<?> coll)) {
			return false;
		}
		List<Long> ids = new ArrayList<>();
		for (Object o : coll) {
			long id = toLong(o);
			if (id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return false;
		}
		w.in("distributor_id", ids);
		return true;
	}

	private static void applyCustomizePageIdInFilter(QueryWrapper<ItemsCategory> w, Map<String, Object> filter) {
		if (!filter.containsKey("customize_page_id_in")) {
			return;
		}
		Object v = filter.get("customize_page_id_in");
		if (!(v instanceof Collection<?> coll)) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Object o : coll) {
			long id = toLong(o);
			if (id > 0L) {
				ids.add(id);
			}
		}
		if (!ids.isEmpty()) {
			w.in("customize_page_id", ids);
		}
	}

	/**
	 * 显式多 id 查询（如小程序分类详情子树）；避免与标量 {@code category_id} 混用。
	 */
	private static void applyParentIdInFilter(QueryWrapper<ItemsCategory> w, Map<String, Object> filter) {
		if (!filter.containsKey("parent_id_in")) {
			return;
		}
		Object v = filter.get("parent_id_in");
		if (!(v instanceof Collection<?> coll)) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Object o : coll) {
			long id = toLong(o);
			if (id > 0L) {
				ids.add(id);
			}
		}
		if (!ids.isEmpty()) {
			w.in("parent_id", ids);
		}
	}

	private static void applyCategoryIdInFilter(QueryWrapper<ItemsCategory> w, Map<String, Object> filter) {
		if (!filter.containsKey("category_id_in")) {
			return;
		}
		Object v = filter.get("category_id_in");
		if (!(v instanceof Collection<?> coll)) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Object o : coll) {
			ids.add(toLong(o));
		}
		if (!ids.isEmpty()) {
			w.in("category_id", ids);
		}
	}

	private static void applyCategoryIdScalarFilter(QueryWrapper<ItemsCategory> w, Map<String, Object> filter) {
		if (!filter.containsKey("category_id")) {
			return;
		}
		w.eq("category_id", toLong(filter.get("category_id")));
	}

	private static int parseIsShowFrontInt(Object v) {
		if (v == null) {
			return 1;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static void putEq(QueryWrapper<ItemsCategory> w, Map<String, Object> filter, String col) {
		if (!filter.containsKey(col)) {
			return;
		}
		Object val = filter.get(col);
		if ("is_main_category".equals(col)) {
			w.eq(col, mainCategoryToInt(val));
			return;
		}
		if ("company_id".equals(col) || "distributor_id".equals(col)) {
			w.eq(col, toLong(val));
			return;
		}
		w.eq(col, val);
	}

	private static long toLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/** 按 company_id + parent_id 取一条子类目行（LIMIT 1）。 */
	public Optional<ItemsCategory> findOneByCompanyIdAndParentId(long companyId, long parentId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getParentId, parentId).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	/** 删除路径下列出某父级下全部子类目行（无 distributor 注入）。 */
	public List<ItemsCategory> listEntitiesByCompanyIdAndParentId(long companyId, long parentId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getParentId, parentId);
		return mapper.selectList(w);
	}

	/** 同条件下行数。 */
	public long countByCompanyIdAndParentId(long companyId, long parentId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getParentId, parentId);
		return mapper.selectCount(w);
	}

	/** 按 category_id + company_id 物理删除行；返回影响行数。 */
	public int deleteByCategoryIdAndCompanyId(long categoryId, long companyId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCategoryId, categoryId).eq(ItemsCategory::getCompanyId, companyId);
		return mapper.delete(w);
	}

	/** 按 parent_id + company_id 物理删除行；返回影响行数。 */
	public int deleteByParentIdAndCompanyId(long parentId, long companyId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getParentId, parentId).eq(ItemsCategory::getCompanyId, companyId);
		return mapper.delete(w);
	}

	public Optional<ItemsCategory> findOneByCompanyIdAndCategoryIdAndIsMainCategory(long companyId, long categoryId,
			boolean isMainCategory) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getCategoryId, categoryId)
				.eq(ItemsCategory::getIsMainCategory, isMainCategory).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	/** path 分段长度为 2 时：lists(parent_id=categoryId, is_main_category, company_id)，返回 category_id 列表。 */
	public List<Long> listCategoryIdsByCompanyIdParentIdAndIsMainCategory(long companyId, long parentId,
			boolean isMainCategory) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getParentId, parentId)
				.eq(ItemsCategory::getIsMainCategory, isMainCategory);
		return mapper.selectList(w).stream().map(ItemsCategory::getCategoryId).collect(Collectors.toList());
	}

	/** path 形如 "{topCategoryId},%" 的子类目 category_id 列表。 */
	public List<Long> listCategoryIdsByPathLikeChildOf(long companyId, long topCategoryId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).likeRight(ItemsCategory::getPath, topCategoryId + ",");
		return mapper.selectList(w).stream().map(ItemsCategory::getCategoryId).collect(Collectors.toList());
	}

	public List<Long> listCategoryIdsByParentId(long companyId, long parentId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getParentId, parentId);
		return mapper.selectList(w).stream().map(ItemsCategory::getCategoryId).collect(Collectors.toList());
	}

	public List<Long> listCategoryIdsByParentIds(long companyId, Collection<Long> parentIds) {
		if (parentIds == null || parentIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).in(ItemsCategory::getParentId, parentIds);
		return mapper.selectList(w).stream().map(ItemsCategory::getCategoryId).collect(Collectors.toList());
	}

	public long countByCompanyAndCategoryIdsAndMainFlag(long companyId, Collection<Long> categoryIds, boolean mainCategory) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).in(ItemsCategory::getCategoryId, categoryIds).eq(ItemsCategory::getIsMainCategory, mainCategory);
		return mapper.selectCount(w);
	}

	public Optional<String> findFirstCategoryNameByPathCommaSuffix(String pathSuffix) {
		if (pathSuffix == null || pathSuffix.isEmpty()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.apply("path LIKE CONCAT('%,', {0})", pathSuffix)
				.orderByAsc(ItemsCategory::getCategoryId)
				.last("LIMIT 1");
		ItemsCategory one = mapper.selectOne(w);
		if (one == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(one.getCategoryName());
	}

	/**
	 * 由子类目 id 向上最多两轮父级查询，拼出与列表接口一致的顶层类目行集合（列：category_id、category_name、is_main_category、parent_id、sort）。
	 */
	public List<Map<String, Object>> getTopByChildrenId(List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		List<Long> level = new ArrayList<>(categoryIds);
		List<ItemsCategory> rows1 = listCategoriesByIdsForTopResolve(level);
		List<Long> level2 = new ArrayList<>();
		for (ItemsCategory row : rows1) {
			long pid = parentIdOrZero(row);
			if (pid == 0L) {
				result.add(toCategoryFilterRow(row));
			} else {
				level2.add(pid);
			}
		}
		if (!level2.isEmpty()) {
			List<ItemsCategory> rows2 = listCategoriesByIdsForTopResolve(level2);
			List<Long> level3 = new ArrayList<>();
			for (ItemsCategory row : rows2) {
				long pid = parentIdOrZero(row);
				if (pid == 0L) {
					result.add(toCategoryFilterRow(row));
				} else {
					level3.add(pid);
				}
			}
			if (!level3.isEmpty()) {
				List<ItemsCategory> rows3 = listCategoriesByIdsForTopResolve(level3);
				for (ItemsCategory row : rows3) {
					result.add(toCategoryFilterRow(row));
				}
			}
		}
		return result;
	}

	private List<ItemsCategory> listCategoriesByIdsForTopResolve(List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.in(ItemsCategory::getCategoryId, categoryIds);
		w.select(ItemsCategory::getCategoryId, ItemsCategory::getCategoryName, ItemsCategory::getIsMainCategory,
				ItemsCategory::getParentId, ItemsCategory::getSort);
		return mapper.selectList(w);
	}

	private static long parentIdOrZero(ItemsCategory row) {
		Long p = row.getParentId();
		return p == null ? 0L : p;
	}

	private static Map<String, Object> toCategoryFilterRow(ItemsCategory row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("category_id", row.getCategoryId());
		m.put("category_name", row.getCategoryName());
		m.put("is_main_category", Boolean.TRUE.equals(row.getIsMainCategory()) ? 1 : 0);
		m.put("parent_id", parentIdOrZero(row));
		m.put("sort", row.getSort() != null ? row.getSort() : 0L);
		return m;
	}

	public Optional<ItemsCategory> findOneByCompanyIdRegionauthIdAndCategoryId(long companyId, long regionauthId, long categoryId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getRegionauthId, regionauthId).eq(ItemsCategory::getCategoryId, categoryId)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public Optional<ItemsCategory> findOneForCustomizePageOccupant(long companyId, long regionauthId, long customizePageId,
			long jwtDistributorId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("regionauth_id", regionauthId);
		filter.put("customize_page_id", customizePageId);
		applyGetDistributorIdEquivalent(filter, companyId, jwtDistributorId);
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getRegionauthId, regionauthId).eq(ItemsCategory::getCustomizePageId, customizePageId);
		if (filter.containsKey("distributor_id")) {
			w.eq(ItemsCategory::getDistributorId, toLong(filter.get("distributor_id")));
		}
		w.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public void updateCustomizePageIdByCompanyRegionauthAndCategoryId(long companyId, long regionauthId, long categoryId,
			long newCustomizePageId) {
		LambdaUpdateWrapper<ItemsCategory> u = new LambdaUpdateWrapper<>();
		u.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getRegionauthId, regionauthId).eq(ItemsCategory::getCategoryId, categoryId)
				.set(ItemsCategory::getCustomizePageId, newCustomizePageId)
				.set(ItemsCategory::getUpdated, (int) (System.currentTimeMillis() / 1000L));
		int n = mapper.update(null, u);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public void clearCustomizePageIdForOccupant(long companyId, long regionauthId, long customizePageId, long jwtDistributorId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("regionauth_id", regionauthId);
		filter.put("customize_page_id", customizePageId);
		applyGetDistributorIdEquivalent(filter, companyId, jwtDistributorId);
		LambdaUpdateWrapper<ItemsCategory> u = new LambdaUpdateWrapper<>();
		u.eq(ItemsCategory::getCompanyId, companyId).eq(ItemsCategory::getRegionauthId, regionauthId).eq(ItemsCategory::getCustomizePageId, customizePageId);
		if (filter.containsKey("distributor_id")) {
			u.eq(ItemsCategory::getDistributorId, toLong(filter.get("distributor_id")));
		}
		u.set(ItemsCategory::getCustomizePageId, 0L).set(ItemsCategory::getUpdated, (int) (System.currentTimeMillis() / 1000L));
		int n = mapper.update(null, u);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	/**
	 * 按公司加载全部分类行（外部同步等场景）。
	 */
	public List<ItemsCategory> listAllEntitiesByCompanyId(long companyId) {
		LambdaQueryWrapper<ItemsCategory> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCategory::getCompanyId, companyId);
		return mapper.selectList(w);
	}
}
