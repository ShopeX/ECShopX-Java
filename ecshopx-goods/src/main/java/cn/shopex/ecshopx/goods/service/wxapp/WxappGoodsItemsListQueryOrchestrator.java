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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListFacadeService;
import cn.shopex.ecshopx.merchant.service.MerchantDisabledDistributorIdsQueryService;
import cn.shopex.ecshopx.salesperson.service.WxappItemsListSalesmanDistributorResolveService;
import cn.shopex.ecshopx.salesperson.service.WxappItemsListSalesmanGateResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemsListQueryOrchestrator {

	public static final String KEY_INTERNAL_LIST_PAGE = "__list_page";
	public static final String KEY_INTERNAL_LIST_PAGE_SIZE = "__list_page_size";
	public static final String KEY_INTERNAL_ACCEPT_LANGUAGE = "__accept_language";

	/** When {@code Boolean.TRUE}, repository excludes {@code items.type = 1} (cross-border). */
	public static final String PARAM_SALESPERSON_EXCLUDE_CROSS_BORDER_TYPE = "__salesperson_exclude_type_1";

	private final MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService;
	private final WxappItemsListSalesmanDistributorResolveService wxappItemsListSalesmanDistributorResolveService;
	private final GoodsItemsListFacadeService goodsItemsListFacadeService;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final DistributorListQueryService distributorListQueryService;

	public WxappGoodsItemsListQueryOrchestrator(MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService,
			WxappItemsListSalesmanDistributorResolveService wxappItemsListSalesmanDistributorResolveService,
			GoodsItemsListFacadeService goodsItemsListFacadeService,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			DistributorListQueryService distributorListQueryService) {
		this.merchantDisabledDistributorIdsQueryService = merchantDisabledDistributorIdsQueryService;
		this.wxappItemsListSalesmanDistributorResolveService = wxappItemsListSalesmanDistributorResolveService;
		this.goodsItemsListFacadeService = goodsItemsListFacadeService;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.distributorListQueryService = distributorListQueryService;
	}

	public Map<String, Object> querySkuItemsList(long companyId, LinkedHashMap<String, Object> params, List<Map<String, Object>> orderBy) {
		LinkedHashMap<String, Object> work = cloneParams(params);
		String acceptLanguage = takeInternalString(work, KEY_INTERNAL_ACCEPT_LANGUAGE);
		if (!StringUtils.hasText(acceptLanguage)) {
			acceptLanguage = "zh-CN";
		}
		applyMerchantDisabledDistributorFilter(companyId, work);
		WxappItemsListSalesmanGateResult gate = wxappItemsListSalesmanDistributorResolveService.resolve(companyId, longUserId(work), work);
		if (gate.isReturnEmpty()) {
			return emptyWithNosale(gate.getNosalestore());
		}
		work.putAll(gate.getParamsPatch());
		Map<String, Object> repo = buildRepositoryParams(companyId, work);
		repo.remove("is_default_true");
		return goodsItemsListFacadeService.wxappQuerySkuItemList(companyId, repo, acceptLanguage);
	}

	public Map<String, Object> querySalespersonSkuItemsList(long companyId, LinkedHashMap<String, Object> params,
			List<Map<String, Object>> orderBy) {
		LinkedHashMap<String, Object> work = cloneParams(params);
		String acceptLanguage = takeInternalString(work, KEY_INTERNAL_ACCEPT_LANGUAGE);
		if (!StringUtils.hasText(acceptLanguage)) {
			acceptLanguage = "zh-CN";
		}
		applyMerchantDisabledDistributorFilter(companyId, work);
		WxappItemsListSalesmanGateResult gate = wxappItemsListSalesmanDistributorResolveService.resolve(companyId, longUserId(work), work);
		if (gate.isReturnEmpty()) {
			return emptyWithNosale(gate.getNosalestore());
		}
		work.putAll(gate.getParamsPatch());
		Map<String, Object> repo = buildRepositoryParams(companyId, work);
		repo.remove("is_default_true");
		String goodsSortToken = str(work.get("goodsSort"));
		return goodsItemsListFacadeService.wxappQuerySkuItemListWithGoodsSort(companyId, repo, goodsSortToken, acceptLanguage);
	}

	/**
	 * 与 {@link #queryItemListData} 相同的商户禁用店铺剔除、导购补丁合并后的业务参数，供响应体 {@code newFilter} 等与列表查询生效条件对齐。
	 */
	public LinkedHashMap<String, Object> effectiveBusinessParamsForWxappItemList(long companyId,
			LinkedHashMap<String, Object> orchestrationParams) {
		LinkedHashMap<String, Object> work = cloneParams(orchestrationParams);
		work.remove(KEY_INTERNAL_ACCEPT_LANGUAGE);
		work.remove(KEY_INTERNAL_LIST_PAGE);
		work.remove(KEY_INTERNAL_LIST_PAGE_SIZE);
		applyMerchantDisabledDistributorFilter(companyId, work);
		WxappItemsListSalesmanGateResult gate = wxappItemsListSalesmanDistributorResolveService.resolve(companyId, longUserId(work), work);
		if (!gate.isReturnEmpty()) {
			work.putAll(gate.getParamsPatch());
		}
		return work;
	}

	public Map<String, Object> queryItemListData(long companyId, LinkedHashMap<String, Object> params, List<Map<String, Object>> orderBy) {
		LinkedHashMap<String, Object> work = cloneParams(params);
		String acceptLanguage = takeInternalString(work, KEY_INTERNAL_ACCEPT_LANGUAGE);
		if (!StringUtils.hasText(acceptLanguage)) {
			acceptLanguage = "zh-CN";
		}
		int page = takeInternalInt(work, KEY_INTERNAL_LIST_PAGE, 1);
		int pageSize = takeInternalInt(work, KEY_INTERNAL_LIST_PAGE_SIZE, 10);
		applyMerchantDisabledDistributorFilter(companyId, work);
		WxappItemsListSalesmanGateResult gate = wxappItemsListSalesmanDistributorResolveService.resolve(companyId, longUserId(work), work);
		if (gate.isReturnEmpty()) {
			return emptyWithNosale(gate.getNosalestore());
		}
		work.putAll(gate.getParamsPatch());
		Map<String, Object> repo = buildRepositoryParams(companyId, work);
		String goodsSort = str(work.get("goodsSort"));
		return goodsItemsListFacadeService.wxappQueryDefaultItemList(companyId, repo, page, pageSize, goodsSort, acceptLanguage);
	}

	public Map<String, Object> queryShopItemListData(long companyId, LinkedHashMap<String, Object> params, List<Map<String, Object>> orderBy) {
		LinkedHashMap<String, Object> work = cloneParams(params);
		String acceptLanguage = takeInternalString(work, KEY_INTERNAL_ACCEPT_LANGUAGE);
		if (!StringUtils.hasText(acceptLanguage)) {
			acceptLanguage = "zh-CN";
		}
		int page = takeInternalInt(work, KEY_INTERNAL_LIST_PAGE, 1);
		int pageSize = takeInternalInt(work, KEY_INTERNAL_LIST_PAGE_SIZE, 10);
		applyMerchantDisabledDistributorFilter(companyId, work);
		WxappItemsListSalesmanGateResult gate = wxappItemsListSalesmanDistributorResolveService.resolve(companyId, longUserId(work), work);
		if (gate.isReturnEmpty()) {
			return emptyWithNosale(gate.getNosalestore());
		}
		work.putAll(gate.getParamsPatch());
		Map<String, Object> repo = buildRepositoryParams(companyId, work);
		String goodsSort = str(work.get("goodsSort"));
		return goodsItemsListFacadeService.wxappQueryDefaultItemListNoMaxPageSize(companyId, repo, page, pageSize, goodsSort, acceptLanguage);
	}

	private static long longUserId(LinkedHashMap<String, Object> work) {
		Object u = work.get("user_id");
		if (u instanceof Number n) {
			return n.longValue();
		}
		if (u != null) {
			try {
				return Long.parseLong(u.toString().trim());
			} catch (NumberFormatException ignored) {
				return 0L;
			}
		}
		return 0L;
	}

	private static String takeInternalString(LinkedHashMap<String, Object> work, String key) {
		Object v = work.remove(key);
		return v != null ? v.toString() : "";
	}

	private static int takeInternalInt(LinkedHashMap<String, Object> work, String key, int defaultVal) {
		Object v = work.remove(key);
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v != null) {
			try {
				return Integer.parseInt(v.toString().trim());
			} catch (NumberFormatException ignored) {
				return defaultVal;
			}
		}
		return defaultVal;
	}

	private static LinkedHashMap<String, Object> cloneParams(LinkedHashMap<String, Object> params) {
		return new LinkedHashMap<>(params);
	}

	private static Map<String, Object> emptyWithNosale(Integer nosalestore) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("total_count", 0L);
		m.put("list", List.of());
		if (nosalestore != null) {
			m.put("nosalestore", nosalestore);
		}
		return m;
	}

	private void applyMerchantDisabledDistributorFilter(long companyId, LinkedHashMap<String, Object> params) {
		List<Long> disabled = merchantDisabledDistributorIdsQueryService.listDistributorIdsLinkedToDisabledMerchants(companyId);
		if (disabled == null || disabled.isEmpty()) {
			return;
		}
		Set<Long> dis = new LinkedHashSet<>(disabled);
		Object raw = params.get("distributor_id");
		if (raw instanceof List<?> list) {
			List<Long> kept = new ArrayList<>();
			for (Object o : list) {
				Long v = toLongObj(o);
				if (v != null && !dis.contains(v)) {
					kept.add(v);
				}
			}
			params.put("distributor_id", kept);
		} else if (raw != null) {
			Long v = toLongObj(raw);
			if (v != null && dis.contains(v)) {
				params.remove("distributor_id");
			}
		}
	}

	private Map<String, Object> buildRepositoryParams(long companyId, LinkedHashMap<String, Object> p) {
		LinkedHashMap<String, Object> repo = new LinkedHashMap<>();
		repo.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);

		if (Boolean.TRUE.equals(p.get(PARAM_SALESPERSON_EXCLUDE_CROSS_BORDER_TYPE))) {
			repo.put(ItemsListQueryRepository.KEY_EXCLUDE_TYPE_EQ_1, Boolean.TRUE);
		}

		if (p.containsKey("approve_status")) {
			repo.put("approve_status", p.get("approve_status"));
		}
		repo.put("audit_status", p.getOrDefault("audit_status", "approved"));
		if (p.containsKey("is_gift")) {
			repo.put("is_gift", p.get("is_gift"));
		}
		if (p.containsKey("item_type")) {
			repo.put("item_type", p.get("item_type"));
		}
		if (p.containsKey("type")) {
			repo.put("type", p.get("type"));
		}
		if (p.containsKey("regions_id")) {
			repo.put("regions_id", p.get("regions_id"));
		}
		if (p.containsKey("brand_id")) {
			repo.put("brand_id", p.get("brand_id"));
		}
		if (p.containsKey("item_name")) {
			repo.put("item_name", p.get("item_name"));
		}
		if (p.containsKey("rebate")) {
			repo.put("rebate", p.get("rebate"));
		}
		if (p.containsKey("rebate_type")) {
			repo.put("rebate_type", p.get("rebate_type"));
		}
		if (p.containsKey("price|gte")) {
			repo.put(ItemsListQueryRepository.KEY_PRICE_GT_CENTS, toIntCents(p.get("price|gte")));
		}
		if (p.containsKey("price|lte")) {
			repo.put(ItemsListQueryRepository.KEY_PRICE_LT_CENTS, toIntCents(p.get("price|lte")));
		}
		if (p.containsKey("sort|gte")) {
			repo.put(ItemsListQueryRepository.KEY_SORT_GTE, toIntForSortGte(p.get("sort|gte")));
		}
		if (p.containsKey("store|gt")) {
			repo.put(ItemsListQueryRepository.KEY_STORE_GT, toIntCents(p.get("store|gt")));
		}
		if (p.containsKey("store|lt")) {
			repo.put(ItemsListQueryRepository.KEY_STORE_LT, toIntCents(p.get("store|lt")));
		}

		if (Boolean.TRUE.equals(p.get("is_default"))) {
			repo.put("is_default_true", Boolean.TRUE);
		}

		@SuppressWarnings("unchecked")
		List<Long> goodsIds = (List<Long>) p.get("goods_id");
		if (goodsIds != null && !goodsIds.isEmpty()) {
			repo.put(ItemsListQueryRepository.KEY_GOODS_ID_IN, goodsIds);
		}

		@SuppressWarnings("unchecked")
		List<String> itemCat = (List<String>) p.get("item_category");
		if (itemCat != null && !itemCat.isEmpty()) {
			repo.put(ItemsListQueryRepository.KEY_ITEM_CATEGORY_IN, itemCat);
		}

		LinkedHashSet<Long> idOrDef = new LinkedHashSet<>();
		@SuppressWarnings("unchecked")
		List<Long> itemIds = (List<Long>) p.get("item_id");
		if (itemIds != null) {
			idOrDef.addAll(itemIds.stream().filter(Objects::nonNull).filter(id -> id > 0).collect(Collectors.toList()));
		}
		@SuppressWarnings("unchecked")
		List<Long> catIds = (List<Long>) p.get("category_resolved_item_ids");
		boolean categoryRequested = p.get("category_id") != null && ValuePresence.hasEffectiveValue(p.get("category_id").toString());
		if (categoryRequested && (catIds == null || catIds.isEmpty())) {
			idOrDef.clear();
			idOrDef.add(-1L);
		} else if (catIds != null && !catIds.isEmpty()) {
			if (idOrDef.isEmpty()) {
				idOrDef.addAll(catIds);
			} else {
				Set<Long> catSet = new LinkedHashSet<>(catIds);
				idOrDef.removeIf(id -> !catSet.contains(id));
				if (idOrDef.isEmpty()) {
					idOrDef.add(-1L);
				}
			}
		}
		@SuppressWarnings("unchecked")
		List<Long> kwIds = (List<Long>) p.get("__keyword_default_item_ids");
		if (kwIds != null) {
			if (kwIds.isEmpty()) {
				// 关键词无命中：强制无结果（与 tag_id 无命中一致）
				idOrDef.clear();
				idOrDef.add(-1L);
			} else if (idOrDef.isEmpty()) {
				idOrDef.addAll(kwIds);
			} else {
				Set<Long> kwSet = new LinkedHashSet<>(kwIds);
				idOrDef.removeIf(id -> !kwSet.contains(id));
				if (idOrDef.isEmpty()) {
					idOrDef.add(-1L);
				}
			}
		}
		String tagIdStr = p.get("tag_id") != null ? p.get("tag_id").toString().trim() : "";
		if (StringUtils.hasText(tagIdStr)) {
			try {
				long tid = Long.parseLong(tagIdStr);
				List<Long> byTag = itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIds(companyId, List.of(tid));
				if (byTag.isEmpty()) {
					repo.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, List.of(-1L));
				} else if (idOrDef.isEmpty()) {
					idOrDef.addAll(byTag);
				} else {
					Set<Long> ts = new LinkedHashSet<>(byTag);
					idOrDef.removeIf(id -> !ts.contains(id));
				}
			} catch (NumberFormatException ignored) {
				// skip invalid tag
			}
		}
		if (!idOrDef.isEmpty()) {
			repo.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(idOrDef));
		}

		applyDistributorRepoFilter(companyId, p, repo);

		return repo;
	}

	private void applyDistributorRepoFilter(long companyId, LinkedHashMap<String, Object> p, LinkedHashMap<String, Object> repo) {
		Object dist = p.get("distributor_id");
		if (dist == null) {
			return;
		}
		if (!Boolean.TRUE.equals(p.get("is_can_sale"))) {
			applyDistributorRepoFilterDirect(dist, repo);
			return;
		}
		if (!"standard".equals(itemsCategoryDistributorIdResolver.resolveProductModel(companyId))) {
			applyDistributorRepoFilterDirect(dist, repo);
			return;
		}
		long distributorId = resolvePrimaryDistributorId(dist);
		if (distributorId <= 0) {
			repo.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, 0);
			return;
		}
		List<Distributor> distRows = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
		Distributor distributor = distRows != null && !distRows.isEmpty() ? distRows.get(0) : null;
		if (distributor != null && isNonSelfDistributor(distributor)) {
			repo.put(ItemsListQueryRepository.KEY_DIST_REL_VIRT_JOIN, Boolean.TRUE);
			repo.put(ItemsListQueryRepository.KEY_DIST_REL_VIRT_DISTRIBUTOR_ID, distributorId);
			repo.put(ItemsListQueryRepository.KEY_DIST_REL_VIRT_GOODS_CAN_SALE, Boolean.TRUE);
			return;
		}
		repo.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, 0);
	}

	private static void applyDistributorRepoFilterDirect(Object dist, LinkedHashMap<String, Object> repo) {
		if (dist instanceof List<?> dl && !dl.isEmpty()) {
			List<Integer> ints = new ArrayList<>();
			for (Object o : dl) {
				Long v = toLongObj(o);
				if (v != null) {
					ints.add((int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, v)));
				}
			}
			if (!ints.isEmpty()) {
				repo.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN, ints);
			}
		} else if (dist != null && StringUtils.hasText(dist.toString())) {
			Long v = toLongObj(dist);
			if (v != null) {
				repo.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ,
						(int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, v)));
			}
		}
	}

	private static long resolvePrimaryDistributorId(Object dist) {
		if (dist instanceof List<?> dl && !dl.isEmpty()) {
			Long v = toLongObj(dl.get(0));
			return v != null ? v : 0L;
		}
		Long v = toLongObj(dist);
		return v != null ? v : 0L;
	}

	private static boolean isNonSelfDistributor(Distributor d) {
		int self = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
		return self == 0;
	}

	private static int toIntCents(Object o) {
		if (o instanceof Number n) {
			long l = n.longValue();
			if (l > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (l < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) l;
		}
		return Integer.parseInt(o.toString().trim());
	}

	private static int toIntForSortGte(Object o) {
		if (o instanceof Number n) {
			long l = n.longValue();
			if (l > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (l < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) l;
		}
		return Integer.parseInt(o.toString().trim());
	}

	private static Long toLongObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
