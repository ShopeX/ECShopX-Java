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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.domain.ItemsCategoryProfit;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryProfitRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.popularize.CompanyPopularizeConfigReadService;
import cn.shopex.ecshopx.popularize.service.PromoterGoodsListForShopQueryService;
import cn.shopex.ecshopx.salesperson.service.WxappItemsListSalesmanDistributorResolveService;
import cn.shopex.ecshopx.wechat.domain.WeappCustomizePage;
import cn.shopex.ecshopx.wechat.repository.WeappCustomizePageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsCategoryQueryService {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsCategoryTreeService itemsCategoryTreeService;
	private final ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier;
	private final ItemsCategoryProfitRepository itemsCategoryProfitRepository;
	private final WeappCustomizePageRepository weappCustomizePageRepository;
	private final ItemsCategoryDistributorIdResolver distributorIdResolver;
	private final ItemsCategoryFrontDisplayResolver frontDisplayResolver;
	private final ObjectMapper objectMapper;
	private final CompanyPopularizeConfigReadService companyPopularizeConfigReadService;
	private final DistributorListQueryService distributorListQueryService;
	private final ItemsRepository itemsRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final WxappItemsListSalesmanDistributorResolveService wxappItemsListSalesmanDistributorResolveService;
	private final PromoterGoodsListForShopQueryService promoterGoodsListForShopQueryService;

	public ItemsCategoryQueryService(ItemsCategoryRepository itemsCategoryRepository,
			ItemsCategoryTreeService itemsCategoryTreeService,
			ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier,
			ItemsCategoryProfitRepository itemsCategoryProfitRepository,
			WeappCustomizePageRepository weappCustomizePageRepository,
			ItemsCategoryDistributorIdResolver distributorIdResolver,
			ItemsCategoryFrontDisplayResolver frontDisplayResolver, ObjectMapper objectMapper,
			CompanyPopularizeConfigReadService companyPopularizeConfigReadService,
			DistributorListQueryService distributorListQueryService, ItemsRepository itemsRepository,
			ItemsRelCatsRepository itemsRelCatsRepository,
			WxappItemsListSalesmanDistributorResolveService wxappItemsListSalesmanDistributorResolveService,
			PromoterGoodsListForShopQueryService promoterGoodsListForShopQueryService) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsCategoryTreeService = itemsCategoryTreeService;
		this.itemsCategoryMultiLangApplier = itemsCategoryMultiLangApplier;
		this.itemsCategoryProfitRepository = itemsCategoryProfitRepository;
		this.weappCustomizePageRepository = weappCustomizePageRepository;
		this.distributorIdResolver = distributorIdResolver;
		this.frontDisplayResolver = frontDisplayResolver;
		this.objectMapper = objectMapper;
		this.companyPopularizeConfigReadService = companyPopularizeConfigReadService;
		this.distributorListQueryService = distributorListQueryService;
		this.itemsRepository = itemsRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.wxappItemsListSalesmanDistributorResolveService = wxappItemsListSalesmanDistributorResolveService;
		this.promoterGoodsListForShopQueryService = promoterGoodsListForShopQueryService;
	}

	public List<Map<String, Object>> getCategory(long companyId, String isMainCategoryStr, String categoryLevel,
			String parentId, Long distributorIdParam, boolean isShow, boolean ignoreNone, String countryCode,
			long jwtDistributorId) {
		String productModel = distributorIdResolver.resolveProductModel(companyId);
		long filterDist = "standard".equals(productModel) ? 0L
				: (distributorIdParam != null ? distributorIdParam : jwtDistributorId);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("is_main_category", parseQueryIsMainCategoryToInt(isMainCategoryStr));
		filter.put("distributor_id", filterDist);
		if (categoryLevel != null) {
			filter.put("category_level", categoryLevel);
		}
		if (parentId != null) {
			filter.put("parent_id", parentId);
		}

		return buildTreeForFilter(filter, companyId, jwtDistributorId, countryCode, isShow, true, true, true, ignoreNone,
				false);
	}

	public List<Map<String, Object>> getWxappCategoryList(long companyId, String isMainCategoryRaw,
			Long distributorIdParam, String onlyTopRaw, String countryCode, long jwtDistributorId) {
		String productModel = distributorIdResolver.resolveProductModel(companyId);
		long requestDistributorId = distributorIdParam != null ? distributorIdParam : 0L;

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("is_show_front", 1);

		boolean isMain = frontDisplayResolver.resolveFrontCategoryListIsMainCategory(productModel, requestDistributorId,
				isMainCategoryRaw);
		filter.put("is_main_category", isMain ? 1 : 0);

		long categoryDistributorId = frontDisplayResolver.resolveCategoryDistributorIdForFront(productModel,
				requestDistributorId);
		if (!"standard".equals(productModel) && categoryDistributorId > 0L) {
			filter.put("distributor_id", categoryDistributorId);
		}

		boolean onlyTop = parseOnlyTop(onlyTopRaw);
		if (onlyTop) {
			filter.put("parent_id", "0");
			filter.put("category_level", "1");
		}

		List<Map<String, Object>> tree = buildTreeForFilter(filter, companyId, jwtDistributorId, countryCode, true, true,
				false, false, false, false);

		if (frontDisplayResolver.shouldFallbackFrontCategoryList(productModel, requestDistributorId, isMain)
				&& tree.isEmpty()) {
			filter.put("is_main_category", 1);
			filter.remove("distributor_id");
			tree = buildTreeForFilter(filter, companyId, jwtDistributorId, countryCode, true, true, false, false, false,
					false);
		}

		boolean profitApplied = isMainCategoryOne(filter.get("is_main_category")) && !tree.isEmpty();
		sanitizeTopLevelCustomizePageIds(companyId, tree);
		if (!tree.isEmpty()) {
			frontDisplayResolver.injectFrontCategoryListIsMainCategoryFlag(tree,
					isMainCategoryOne(filter.get("is_main_category")));
		}
		return applyWxappCategoryWhitelist(tree, profitApplied);
	}

	/**
	 * 店铺推广分类：不写 {@code is_show_front}，业务员多店维度与主类目回退与前台类目列表策略对齐，响应字段单独裁剪。
	 */
	public List<Map<String, Object>> getWxappPromoterCategoryList(long companyId, long userId, String isMainCategoryRaw,
			String distributorIdRaw, String isSalesmanPageRaw, String countryCode, long jwtDistributorId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		WxappItemsListSalesmanDistributorResolveService.PromoterCategoryDistributorResolution r =
				wxappItemsListSalesmanDistributorResolveService.resolvePromoterCategoryDistributorFilter(companyId, userId,
						isSalesmanPageRaw, distributorIdRaw);
		if (r.skipPlatformJwtDistributorMerge()) {
			filter.put("promoter_skip_jwt_distributor_merge", Boolean.TRUE);
		}
		if (r.singleDistributorId() != null) {
			filter.put("distributor_id", r.singleDistributorId());
		}
		if (r.distributorIdIn() != null && !r.distributorIdIn().isEmpty()) {
			filter.put("distributor_id_in", new ArrayList<>(r.distributorIdIn()));
		}
		filter.put("is_main_category", parseQueryIsMainCategoryToInt(isMainCategoryRaw));

		int mainCatFilter = (Integer) filter.get("is_main_category");
		boolean firstWasNonMain = mainCatFilter == 0;
		boolean hadExplicitDist = filter.containsKey("distributor_id") || filter.containsKey("distributor_id_in");

		List<Map<String, Object>> tree = buildPromoterCategoryTreeRound(filter, companyId, jwtDistributorId, countryCode);

		if (firstWasNonMain && tree.isEmpty()) {
			filter.put("is_main_category", 1);
			if (hadExplicitDist) {
				filter.remove("distributor_id");
				filter.remove("distributor_id_in");
			}
			tree = buildPromoterCategoryTreeRound(filter, companyId, jwtDistributorId, countryCode);
		}

		boolean profitApplied = isMainCategoryOne(filter.get("is_main_category")) && !tree.isEmpty();
		return applyPromoterCategoryWhitelist(tree, profitApplied);
	}

	private List<Map<String, Object>> buildPromoterCategoryTreeRound(LinkedHashMap<String, Object> filter, long companyId,
			long jwtDistributorId, String countryCode) {
		// 推广类目树经 listsForPromoterCategory 拉平后建树；该入口不要求 ECX-925 的顶层按 sort 分组再按 created 细排，
		// 故 buildTreeForFilter 将 applyEcx925Sort 置为 false（其它前台类目入口仍可单独开启）。
		return buildTreeForFilter(filter, companyId, jwtDistributorId, countryCode, true, true, false, false, false, true);
	}

	/**
	 * 小程序「商品分类信息」：单条详情或带子树的裁剪结构（与前台列表字段族一致，含多语言）。
	 */
	public Map<String, Object> getWxappCategoryInfo(long companyId, long categoryId, Long distributorIdParam,
			String countryCode, long jwtDistributorId) {
		Optional<ItemsCategory> entityOpt = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, categoryId);
		if (entityOpt.isEmpty()) {
			throw new ResourceException("未获取到分类详情");
		}
		ItemsCategory entity = entityOpt.get();
		Map<String, Object> row = ItemsCategoryRowMaps.fromListEntity(entity);
		List<Long> subtreeIds = collectWxappSubtreeCategoryIds(companyId, categoryId);

		if (subtreeIds.size() == 1) {
			List<Map<String, Object>> one = new ArrayList<>(1);
			one.add(deepCopyStringKeyMap(row));
			itemsCategoryMultiLangApplier.apply(companyId, countryCode, one);
			return formatWxappCategoryInfo(one.get(0));
		}

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("category_id_in", new ArrayList<>(subtreeIds));
		if (distributorIdParam != null && distributorIdParam > 0L) {
			filter.put("distributor_id", distributorIdParam);
		}

		List<Map<String, Object>> flat = itemsCategoryRepository.lists(filter, companyId, jwtDistributorId, 1, -1);
		itemsCategoryMultiLangApplier.apply(companyId, countryCode, flat);

		long treePid = entity.getParentId() == null ? 0L : entity.getParentId();
		List<Map<String, Object>> categoryLists = itemsCategoryTreeService.getTree(flat, treePid, 0, true);
		if (categoryLists == null || categoryLists.isEmpty()) {
			throw new ResourceException("未获取到分类信息");
		}
		return formatWxappCategoryInfo(categoryLists.get(0));
	}

	/**
	 * 指定父级下的子分类平面列表（含 total_count），默认第一页每页 100 条，按创建时间倒序。
	 */
	public Map<String, Object> getWxappChildrenCategorys(long companyId, String catId, Long distributorIdParam,
			String countryCode, long jwtDistributorId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("parent_id", catId);
		if (distributorIdParam != null && distributorIdParam != 0L) {
			filter.put("distributor_id", distributorIdParam);
		}
		Map<String, Object> raw = itemsCategoryRepository.listsWxappChildrenCategoriesWithTotalCount(filter, companyId,
				jwtDistributorId, 1, 100);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("list");
		if (list != null && !list.isEmpty()) {
			itemsCategoryMultiLangApplier.apply(companyId, countryCode, list);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", raw.get("total_count"));
		out.put("list", list != null ? list : List.of());
		return out;
	}

	/**
	 * 小程序「指定等级分类列表」：推广模式 → 有效店铺 → 在售商品 → 关联分类顶级 id → 类目平面列表（含多语言）。
	 */
	public Map<String, Object> getWxappLevelCategoryList(long companyId, String isMainCategoryRaw, String distributorIdRaw,
			String countryCode, long jwtDistributorId) {
		LinkedHashMap<String, Object> categoryFilter = new LinkedHashMap<>();
		categoryFilter.put("company_id", companyId);
		if (distributorIdRaw != null && StringUtils.hasText(distributorIdRaw.trim())) {
			try {
				long d = Long.parseLong(distributorIdRaw.trim());
				if (d > 0L) {
					categoryFilter.put("distributor_id", d);
				}
			} catch (NumberFormatException ignored) {
				// 不可解析为 long 时不写入 distributor_id
			}
		}
		categoryFilter.put("is_main_category", parseQueryIsMainCategoryToInt(isMainCategoryRaw));

		String goodsMode = companyPopularizeConfigReadService.getGoodsMode(companyId);
		boolean requireRebateOne = "select".equals(goodsMode);

		List<Long> validDistIds = distributorListQueryService.listValidDistributorIdsForCompany(companyId);
		List<Long> distributorIn = new ArrayList<>();
		distributorIn.add(0L);
		distributorIn.addAll(validDistIds);

		List<Long> itemIds = itemsRepository.listItemIdsForWxappLevelCategory(companyId, distributorIn, requireRebateOne);
		List<ItemsRelCats> relRows = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds);

		List<Long> topCategoryIds = new ArrayList<>();
		for (ItemsRelCats rel : relRows) {
			Long catId = rel.getCategoryId();
			if (catId == null) {
				continue;
			}
			Optional<ItemsCategory> entityOpt = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, catId);
			if (entityOpt.isEmpty()) {
				continue;
			}
			Long topId = resolveTopCategoryIdFromCategoryRow(entityOpt.get());
			if (topId != null) {
				topCategoryIds.add(topId);
			}
		}
		if (!topCategoryIds.isEmpty()) {
			categoryFilter.put("category_id_in", topCategoryIds);
		}

		Map<String, Object> raw = itemsCategoryRepository.listsWxappLevelCategoryWithTotalCount(categoryFilter, companyId,
				jwtDistributorId, 1, -1);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("list");
		if (list != null && !list.isEmpty()) {
			itemsCategoryMultiLangApplier.apply(companyId, countryCode, list);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", raw.get("total_count"));
		out.put("list", list != null ? list : List.of());
		return out;
	}

	/**
	 * 小店上架类目：推广商品 → 在售/线下在售货品 → 关联分类顶级 id → 类目平面列表（含多语言）；导购门店 id 取当前登录会员，推广范围取 {@code shop_user_id} 或登录会员。
	 */
	public Map<String, Object> getWxappShopShelvesCategoryList(long companyId, String shopUserIdRaw, long authUserId,
			String distributorIdRaw, String isMainCategoryRaw, String countryCode, long jwtDistributorId) {
		long promoterUserId = resolvePromoterUserIdForShopShelves(shopUserIdRaw, authUserId);
		String goodsMode = companyPopularizeConfigReadService.getGoodsMode(companyId);
		boolean promoterListsAllGoods = "all".equals(goodsMode);
		boolean requireItemRebateOne = !promoterListsAllGoods;

		List<Long> promoterGoodsIds = promoterGoodsListForShopQueryService.listGoodsIdsJoinedItemsForShop(companyId,
				promoterUserId, promoterListsAllGoods);
		// 上架货品 id 列表为空时，不施加类目 id 范围限制，仅按商户、分销维度与是否主类目等条件查询类目。
		List<Long> itemIds = itemsRepository.listItemIdsForShopShelvesListing(companyId, promoterGoodsIds,
				requireItemRebateOne);

		List<ItemsRelCats> relRows = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds);
		List<Long> topCategoryIds = new ArrayList<>();
		for (ItemsRelCats rel : relRows) {
			Long catId = rel.getCategoryId();
			if (catId == null) {
				continue;
			}
			Optional<ItemsCategory> entityOpt = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, catId);
			if (entityOpt.isEmpty()) {
				continue;
			}
			Long topId = resolveTopCategoryIdFromCategoryRow(entityOpt.get());
			if (topId != null) {
				topCategoryIds.add(topId);
			}
		}

		LinkedHashMap<String, Object> categoryFilter = new LinkedHashMap<>();
		categoryFilter.put("company_id", companyId);
		categoryFilter.put("is_main_category", parseQueryIsMainCategoryToInt(isMainCategoryRaw));

		List<Long> shopDistIds = wxappItemsListSalesmanDistributorResolveService.listRawSalespersonShopIdsForWxappUser(
				companyId, authUserId, 500);
		if (!shopDistIds.isEmpty()) {
			categoryFilter.put("distributor_id_in", new ArrayList<>(shopDistIds));
		} else if (distributorIdRaw != null && StringUtils.hasText(distributorIdRaw.trim())) {
			try {
				long d = Long.parseLong(distributorIdRaw.trim());
				if (d > 0L) {
					categoryFilter.put("distributor_id", d);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid distributor_id
			}
		}

		if (!topCategoryIds.isEmpty()) {
			categoryFilter.put("category_id_in", new ArrayList<>(new LinkedHashSet<>(topCategoryIds)));
		}

		Map<String, Object> raw = itemsCategoryRepository.listsWxappLevelCategoryWithTotalCount(categoryFilter, companyId,
				jwtDistributorId, 1, -1);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("list");
		if (list != null && !list.isEmpty()) {
			itemsCategoryMultiLangApplier.apply(companyId, countryCode, list);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", raw.get("total_count"));
		out.put("list", list != null ? list : List.of());
		return out;
	}

	private static long resolvePromoterUserIdForShopShelves(String shopUserIdRaw, long authUserId) {
		if (shopUserIdRaw != null && StringUtils.hasText(shopUserIdRaw.trim())) {
			try {
				long v = Long.parseLong(shopUserIdRaw.trim());
				if (v > 0L) {
					return v;
				}
			} catch (NumberFormatException ignored) {
				// use auth user below
			}
		}
		return authUserId;
	}

	/**
	 * 由分类行解析顶级 {@code category_id}：无父级信息则返回 null；{@code parent_id == 0} 时顶级为当前分类。
	 */
	private static Long resolveTopCategoryIdFromCategoryRow(ItemsCategory cat) {
		Long parentId = cat.getParentId();
		if (parentId == null) {
			return null;
		}
		if (parentId == 0L) {
			return cat.getCategoryId();
		}
		String path = cat.getPath();
		if (path == null || path.isBlank()) {
			return null;
		}
		int comma = path.indexOf(',');
		String first = comma >= 0 ? path.substring(0, comma).trim() : path.trim();
		if (first.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(first);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 与删除/关联模块一致：当前 id、一级子 id、二级子 id（去重保序）。
	 */
	private List<Long> collectWxappSubtreeCategoryIds(long companyId, long categoryId) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		ids.add(categoryId);
		List<Long> level1 = itemsCategoryRepository.listCategoryIdsByParentId(companyId, categoryId);
		if (!level1.isEmpty()) {
			ids.addAll(level1);
			List<Long> level2 = itemsCategoryRepository.listCategoryIdsByParentIds(companyId, level1);
			if (!level2.isEmpty()) {
				ids.addAll(level2);
			}
		}
		return new ArrayList<>(ids);
	}

	private static Map<String, Object> deepCopyStringKeyMap(Map<String, Object> src) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : src.entrySet()) {
			m.put(e.getKey(), e.getValue());
		}
		return m;
	}

	/**
	 * 小程序分类详情输出白名单；{@code category_level} 优先于树节点 {@code level}；仅在有非空子列表时输出 {@code children}。
	 */
	private Map<String, Object> formatWxappCategoryInfo(Map<String, Object> node) {
		Map<String, Object> out = new LinkedHashMap<>();
		Object categoryLevel = node.get("category_level");
		if (!isTruthyCategoryScalar(categoryLevel)) {
			categoryLevel = node.get("level");
		}
		if (categoryLevel == null) {
			categoryLevel = "0";
		}
		out.put("category_id", node.get("category_id"));
		out.put("category_name", node.get("category_name"));
		out.put("category_level", categoryLevel);
		out.put("parent_id", node.get("parent_id"));
		out.put("path", node.get("path"));
		out.put("sort", node.get("sort"));
		out.put("image_url", node.get("image_url"));
		out.put("distributor_id", node.get("distributor_id"));
		Object customizePageId = node.get("customize_page_id");
		out.put("customize_page_id", customizePageId != null ? customizePageId : "0");

		Object ch = node.get("children");
		if (ch instanceof List<?> list && !list.isEmpty()) {
			List<Map<String, Object>> childrenOut = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> raw) {
					Map<String, Object> cm = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : raw.entrySet()) {
						if (e.getKey() != null) {
							cm.put(e.getKey().toString(), e.getValue());
						}
					}
					childrenOut.add(formatWxappCategoryInfo(cm));
				}
			}
			if (!childrenOut.isEmpty()) {
				out.put("children", childrenOut);
			}
		}
		return out;
	}

	/** 分类层级字段是否视为有效：数值 0、字符串 {@code "0"}、空串视为无效。 */
	private static boolean isTruthyCategoryScalar(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private List<Map<String, Object>> buildTreeForFilter(LinkedHashMap<String, Object> filter, long companyId,
			long jwtDistributorId, String countryCode, boolean isShowChildren, boolean applyProfitIfMainCategory,
			boolean applyEcx925Sort, boolean applyCustomizePageNames, boolean removeNoneCategories,
			boolean loadFlatViaPromoterLists) {
		boolean singleLevel = filter.containsKey("parent_id") || filter.containsKey("category_level");
		List<Map<String, Object>> flat;
		if (singleLevel) {
			flat = itemsCategoryRepository.getSingleLevelList(filter, companyId, jwtDistributorId, 1, -1);
		} else if (loadFlatViaPromoterLists) {
			flat = itemsCategoryRepository.listsForPromoterCategory(filter, companyId, jwtDistributorId, 1, -1);
		} else {
			flat = itemsCategoryRepository.lists(filter, companyId, jwtDistributorId, 1, -1);
		}

		itemsCategoryMultiLangApplier.apply(companyId, countryCode, flat);

		long treePid = filter.containsKey("parent_id")
				? ItemsCategoryTreeService.parseLeadingIntFromString(String.valueOf(filter.get("parent_id")))
				: 0L;
		int treeStart = filter.containsKey("category_level")
				? ItemsCategoryTreeService.parseLeadingIntFromString(String.valueOf(filter.get("category_level"))) - 1
				: 0;

		List<Map<String, Object>> result = itemsCategoryTreeService.getTree(flat, treePid, treeStart, isShowChildren);

		boolean mainCat = false;
		Object im = filter.get("is_main_category");
		if (im instanceof Number n) {
			mainCat = n.intValue() == 1;
		}
		if (applyProfitIfMainCategory && mainCat && result != null && !result.isEmpty()) {
			applyProfit(companyId, singleLevel, result);
		}

		if (applyCustomizePageNames && result != null && !result.isEmpty()) {
			applyCustomizePageNames(result);
		}
		if (applyEcx925Sort && result != null && !result.isEmpty()) {
			applyEcx925Sort(result);
		}
		if (removeNoneCategories && result != null) {
			result = this.removeNoneCategories(result);
		}
		return result == null ? List.of() : result;
	}

	/**
	 * 与前台/管理端 query 一致：{@code is_main_category=1}、{@code true} 为主类目；缺省与 {@code 0}/{@code false} 为非主类目。
	 */
	private static int parseQueryIsMainCategoryToInt(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0;
		}
		String t = raw.trim();
		if ("1".equals(t) || "true".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t)) {
			return 1;
		}
		if ("0".equals(t) || "false".equalsIgnoreCase(t) || "no".equalsIgnoreCase(t)) {
			return 0;
		}
		try {
			return Integer.parseInt(t) != 0 ? 1 : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static boolean isMainCategoryOne(Object isMainCategoryVal) {
		if (isMainCategoryVal instanceof Number n) {
			return n.intValue() == 1;
		}
		return false;
	}

	private static boolean parseOnlyTop(String raw) {
		if (raw == null || !StringUtils.hasText(raw)) {
			return false;
		}
		String t = raw.trim();
		if ("1".equals(t)) {
			return true;
		}
		if ("true".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t)) {
			return true;
		}
		try {
			return Integer.parseInt(t) != 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void sanitizeTopLevelCustomizePageIds(long companyId, List<Map<String, Object>> topLevel) {
		if (topLevel == null || topLevel.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> v : topLevel) {
			long id = ItemsCategoryTreeService.toLongKey(v.get("customize_page_id"));
			if (id > 0L) {
				ids.add(id);
			}
		}
		Set<Long> valid = weappCustomizePageRepository.listValidCategoryPageIds(companyId, ids);
		for (Map<String, Object> v : topLevel) {
			long id = ItemsCategoryTreeService.toLongKey(v.get("customize_page_id"));
			if (id != 0L && !valid.contains(id)) {
				v.put("customize_page_id", 0L);
			}
		}
	}

	private static List<Map<String, Object>> applyWxappCategoryWhitelist(List<Map<String, Object>> tree,
			boolean includeProfitKeys) {
		if (tree == null || tree.isEmpty()) {
			return tree == null ? List.of() : tree;
		}
		List<Map<String, Object>> out = new ArrayList<>(tree.size());
		for (Map<String, Object> node : tree) {
			out.add(whitelistWxappCategoryNode(node, includeProfitKeys));
		}
		return out;
	}

	private static Map<String, Object> whitelistWxappCategoryNode(Map<String, Object> node, boolean includeProfitKeys) {
		Map<String, Object> m = new LinkedHashMap<>();
		copyWhitelistField(m, node, "category_id");
		copyWhitelistField(m, node, "category_name");
		copyWhitelistField(m, node, "category_level");
		copyWhitelistField(m, node, "parent_id");
		copyWhitelistField(m, node, "image_url");
		copyWhitelistField(m, node, "customize_page_id");
		copyWhitelistField(m, node, "is_main_category");
		copyWhitelistField(m, node, "level");
		copyWhitelistField(m, node, "has_children");
		if (includeProfitKeys) {
			copyWhitelistField(m, node, "profit_type");
			copyWhitelistField(m, node, "profit_conf_profit");
			copyWhitelistField(m, node, "profit_conf_popularize_profit");
		}
		List<Map<String, Object>> childrenOut = new ArrayList<>();
		Object ch = node.get("children");
		if (ch instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof Map<?, ?> cm) {
					Map<String, Object> child = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : cm.entrySet()) {
						if (e.getKey() != null) {
							child.put(e.getKey().toString(), e.getValue());
						}
					}
					childrenOut.add(whitelistWxappCategoryNode(child, includeProfitKeys));
				}
			}
		}
		m.put("children", childrenOut);
		return m;
	}

	private static void copyWhitelistField(Map<String, Object> to, Map<String, Object> from, String key) {
		if (from.containsKey(key)) {
			to.put(key, from.get(key));
		}
	}

	private List<Map<String, Object>> applyPromoterCategoryWhitelist(List<Map<String, Object>> tree,
			boolean includeProfitKeys) {
		if (tree == null || tree.isEmpty()) {
			return tree == null ? List.of() : tree;
		}
		List<Map<String, Object>> out = new ArrayList<>(tree.size());
		for (Map<String, Object> node : tree) {
			out.add(whitelistPromoterCategoryNode(node, includeProfitKeys));
		}
		return out;
	}

	private static Map<String, Object> whitelistPromoterCategoryNode(Map<String, Object> node, boolean includeProfitKeys) {
		Map<String, Object> m = new LinkedHashMap<>();
		copyWhitelistField(m, node, "category_id");
		copyWhitelistField(m, node, "category_name");
		copyWhitelistField(m, node, "category_level");
		copyWhitelistField(m, node, "parent_id");
		copyWhitelistField(m, node, "image_url");
		copyWhitelistField(m, node, "level");
		copyWhitelistField(m, node, "has_children");
		if (includeProfitKeys) {
			copyWhitelistField(m, node, "profit_type");
			copyWhitelistField(m, node, "profit_conf_profit");
			copyWhitelistField(m, node, "profit_conf_popularize_profit");
		}
		List<Map<String, Object>> childrenOut = new ArrayList<>();
		Object ch = node.get("children");
		if (ch instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof Map<?, ?> cm) {
					Map<String, Object> child = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : cm.entrySet()) {
						if (e.getKey() != null) {
							child.put(e.getKey().toString(), e.getValue());
						}
					}
					childrenOut.add(whitelistPromoterCategoryNode(child, includeProfitKeys));
				}
			}
		}
		m.put("children", childrenOut);
		return m;
	}

	private void applyProfit(long companyId, boolean singleLevel, List<Map<String, Object>> result) {
		Set<Long> categoryIds = new LinkedHashSet<>();
		if (singleLevel) {
			for (Map<String, Object> v : result) {
				if (ItemsCategoryTreeService.toIntOrZero(v.get("category_level")) == 3) {
					categoryIds.add(ItemsCategoryTreeService.toLongKey(v.get("category_id")));
				}
			}
		} else {
			for (Map<String, Object> v1 : result) {
				List<Map<String, Object>> ch2 = getChildrenMaps(v1.get("children"));
				for (Map<String, Object> v2 : ch2) {
					List<Map<String, Object>> ch3 = getChildrenMaps(v2.get("children"));
					for (Map<String, Object> v3 : ch3) {
						categoryIds.add(ItemsCategoryTreeService.toLongKey(v3.get("category_id")));
					}
				}
			}
		}
		if (categoryIds.isEmpty()) {
			return;
		}
		List<ItemsCategoryProfit> profits = itemsCategoryProfitRepository.listByCompanyAndCategoryIds(companyId,
				categoryIds);
		Map<Long, ItemsCategoryProfit> byId = profits.stream()
				.collect(Collectors.toMap(ItemsCategoryProfit::getCategoryId, p -> p, (a, b) -> a));

		if (singleLevel) {
			for (Map<String, Object> v : result) {
				if (ItemsCategoryTreeService.toIntOrZero(v.get("category_level")) != 3) {
					continue;
				}
				long cid = ItemsCategoryTreeService.toLongKey(v.get("category_id"));
				attachProfitFields(v, byId.get(cid));
			}
		} else {
			for (Map<String, Object> v1 : result) {
				List<Map<String, Object>> ch2 = getChildrenMaps(v1.get("children"));
				for (Map<String, Object> v2 : ch2) {
					List<Map<String, Object>> ch3 = getChildrenMaps(v2.get("children"));
					for (Map<String, Object> v3 : ch3) {
						long cid = ItemsCategoryTreeService.toLongKey(v3.get("category_id"));
						attachProfitFields(v3, byId.get(cid));
					}
				}
			}
		}
	}

	private void attachProfitFields(Map<String, Object> node, ItemsCategoryProfit row) {
		if (row == null) {
			return;
		}
		node.put("profit_type", parseProfitTypeAsInt(row.getProfitType()));
		JsonNode confNode = parseProfitConfQuiet(row.getProfitConf());
		if (confNode != null) {
			if (confNode.has("profit")) {
				node.put("profit_conf_profit", confNode.get("profit").asText(""));
			} else {
				node.put("profit_conf_profit", "");
			}
			if (confNode.has("popularize_profit")) {
				node.put("profit_conf_popularize_profit", confNode.get("popularize_profit").asText(""));
			} else {
				node.put("profit_conf_popularize_profit", "");
			}
		} else {
			node.put("profit_conf_profit", "");
			node.put("profit_conf_popularize_profit", "");
		}
	}

	private JsonNode parseProfitConfQuiet(String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readTree(json);
		} catch (Exception e) {
			return null;
		}
	}

	private static int parseProfitTypeAsInt(String s) {
		if (s == null) {
			return 0;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void applyCustomizePageNames(List<Map<String, Object>> topLevel) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Map<String, Object> v : topLevel) {
			ids.add(ItemsCategoryTreeService.toLongKey(v.get("customize_page_id")));
		}
		Map<Long, String> idToName = new LinkedHashMap<>();
		if (!ids.isEmpty()) {
			List<WeappCustomizePage> pages = weappCustomizePageRepository.listByIds(ids);
			for (WeappCustomizePage p : pages) {
				if (p.getPageName() != null) {
					idToName.put(p.getId(), p.getPageName());
				}
			}
		}
		for (Map<String, Object> v : topLevel) {
			long cpid = ItemsCategoryTreeService.toLongKey(v.get("customize_page_id"));
			v.put("customize_page_name", idToName.getOrDefault(cpid, ""));
			// 仅顶层节点将排序值规范为数值；子树节点仍使用行映射中的字符串形式
			v.put("sort", topLevelSortAsJsonNumber(v.get("sort")));
		}
	}

	/** 顶层列表项的排序字段序列化为 JSON 数字。 */
	private static Long topLevelSortAsJsonNumber(Object sortVal) {
		return sortAsLong(sortVal);
	}

	private static long sortAsLong(Object sortVal) {
		if (sortVal == null) {
			return 0L;
		}
		if (sortVal instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(sortVal.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void applyEcx925Sort(List<Map<String, Object>> topLevel) {
		LinkedHashMap<Long, List<Map<String, Object>>> groups = new LinkedHashMap<>();
		for (Map<String, Object> v : topLevel) {
			long sk = sortAsLong(v.get("sort"));
			groups.computeIfAbsent(sk, k -> new ArrayList<>()).add(v);
		}
		List<Map<String, Object>> merged = new ArrayList<>();
		for (List<Map<String, Object>> group : groups.values()) {
			group.sort((a, b) -> Integer.compare(createdSortKey(b.get("created")), createdSortKey(a.get("created"))));
			merged.addAll(group);
		}
		topLevel.clear();
		topLevel.addAll(merged);
	}

	private static int createdSortKey(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private List<Map<String, Object>> removeNoneCategories(List<Map<String, Object>> categories) {
		List<Map<String, Object>> a = removeNoneCategory(copyList(categories), 0);
		a = removeNoneCategory(a, 2);
		return removeNoneCategory(a, 1);
	}

	private List<Map<String, Object>> copyList(List<Map<String, Object>> in) {
		return new ArrayList<>(in);
	}

	private List<Map<String, Object>> removeNoneCategory(List<Map<String, Object>> categories, int level) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> cate : categories) {
			if (cate == null) {
				continue;
			}
			if (level > 0) {
				int cl = ItemsCategoryTreeService.toIntOrZero(cate.get("category_level"));
				if (cl > level) {
					out.add(cate);
					continue;
				}
			}
			if (!cate.containsKey("children")) {
				out.add(cate);
				continue;
			}
			List<Map<String, Object>> chList = getChildrenMaps(cate.get("children"));
			if (chList.isEmpty()) {
				continue;
			}
			Map<String, Object> next = new LinkedHashMap<>(cate);
			next.put("children", removeNoneCategory(chList, level));
			out.add(next);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> getChildrenMaps(Object children) {
		if (!(children instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> r = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				Map<String, Object> cm = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					if (e.getKey() != null) {
						cm.put(e.getKey().toString(), e.getValue());
					}
				}
				r.add(cm);
			}
		}
		return r;
	}
}
