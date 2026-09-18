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

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.goods.service.pointsmall.PointsmallFrontBrandFilter;
import cn.shopex.ecshopx.goods.service.pointsmall.PointsmallFrontBrandListService;
import cn.shopex.ecshopx.goods.service.popularize.CompanyPopularizeConfigReadService;
import cn.shopex.ecshopx.popularize.service.PromoterGoodsListForShopQueryService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Shop-shelves item listing; the related shop-shelves category API resolves promoter goods through the same
 * {@link PromoterGoodsListForShopQueryService} for aligned JOIN semantics and ordering.
 */
@Service
public class WxappShopItemsListServiceImpl implements WxappShopItemsListService {

	private static final Logger log = LoggerFactory.getLogger(WxappShopItemsListServiceImpl.class);

	private final CompanyPopularizeConfigReadService companyPopularizeConfigReadService;
	private final PromoterGoodsListForShopQueryService promoterGoodsListForShopQueryService;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final PointsmallFrontBrandListService pointsmallFrontBrandListService;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsRepository itemsTagsRepository;
	private final DistributorItemsMapper distributorItemsMapper;
	private final ItemsMapper itemsMapper;
	private final LangueProperties langueProperties;

	public WxappShopItemsListServiceImpl(CompanyPopularizeConfigReadService companyPopularizeConfigReadService,
			PromoterGoodsListForShopQueryService promoterGoodsListForShopQueryService,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator,
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			PointsmallFrontBrandListService pointsmallFrontBrandListService,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsTagsRepository itemsTagsRepository,
			DistributorItemsMapper distributorItemsMapper,
			ItemsMapper itemsMapper,
			LangueProperties langueProperties) {
		this.companyPopularizeConfigReadService = companyPopularizeConfigReadService;
		this.promoterGoodsListForShopQueryService = promoterGoodsListForShopQueryService;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.wxappGoodsItemsListQueryOrchestrator = wxappGoodsItemsListQueryOrchestrator;
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.pointsmallFrontBrandListService = pointsmallFrontBrandListService;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
		this.distributorItemsMapper = distributorItemsMapper;
		this.itemsMapper = itemsMapper;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> execute(long companyId, long authUserId, HttpServletRequest request) {
		String su = request.getParameter("shop_user_id");
		long promoterUserId;
		if (su != null && !su.isBlank()) {
			try {
				promoterUserId = Long.parseLong(su.trim());
			} catch (NumberFormatException e) {
				promoterUserId = 0L;
			}
		} else {
			promoterUserId = authUserId;
		}

		String goodsMode = companyPopularizeConfigReadService.getGoodsMode(companyId);
		boolean isAllGoods = "all".equals(goodsMode);

		long earlyTotal = promoterGoodsListForShopQueryService.countJoinedItemsForShop(companyId, promoterUserId, isAllGoods);
		if (earlyTotal == 0L) {
			return shopEarlyEmpty();
		}

		List<Long> rawGoodsIds = promoterGoodsListForShopQueryService.listGoodsIdsJoinedItemsForShop(companyId, promoterUserId, isAllGoods);
		LinkedHashSet<Long> distinctGoodsIds = new LinkedHashSet<>();
		if (rawGoodsIds != null) {
			for (Long gid : rawGoodsIds) {
				if (gid != null) {
					distinctGoodsIds.add(gid);
				}
			}
		}
		if (distinctGoodsIds.isEmpty()) {
			return shopEarlyEmpty();
		}

		int page = parseListPage(request.getParameter("page"));
		int pageSize = parseListPageSize(request.getParameter("pageSize"));

		LinkedHashMap<String, Object> orchestrationParams = new LinkedHashMap<>();
		String kw = request.getParameter("keywords");
		if (StringUtils.hasText(kw)) {
			String trimmed = kw.trim();
			orchestrationParams.put("item_name", trimmed);
			orchestrationParams.put("keywords", trimmed);
		}
		orchestrationParams.put("is_default", Boolean.TRUE);
		orchestrationParams.put("approve_status", List.of("onsale", "only_show"));

		String distRaw = request.getParameter("distributor_id");
		if (StringUtils.hasText(distRaw) && !"false".equalsIgnoreCase(distRaw.trim())) {
			orchestrationParams.put("distributor_id", distRaw.trim());
		}

		boolean salesmanPage = isSalesmanPageQuery(request.getParameter("isSalesmanPage"));
		long shopUserIdForSales = 0L;
		if (su != null && !su.isBlank()) {
			try {
				shopUserIdForSales = Long.parseLong(su.trim());
			} catch (NumberFormatException ignored) {
				shopUserIdForSales = 0L;
			}
		}
		if (salesmanPage) {
			orchestrationParams.put("isSalesmanPage", "1");
			orchestrationParams.put("user_id", shopUserIdForSales);
			if (log.isDebugEnabled()) {
				log.debug("wxapp shop items list salesman branch companyId={} shopUserIdForSales={}", companyId, shopUserIdForSales);
			}
		}

		long categoryId = 0L;
		String catRaw = request.getParameter("category_id");
		if (StringUtils.hasText(catRaw)) {
			try {
				categoryId = Long.parseLong(catRaw.trim());
			} catch (NumberFormatException ignored) {
				categoryId = 0L;
			}
		}
		orchestrationParams.put("category_id", categoryId);
		if (categoryId > 0L) {
			orchestrationParams.put("item_id", itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, categoryId));
		}

		String goodsSort = request.getParameter("goodsSort");
		if (StringUtils.hasText(goodsSort)) {
			orchestrationParams.put("goodsSort", goodsSort.trim());
		}

		orchestrationParams.put("company_id", companyId);
		orchestrationParams.put("goods_id", new ArrayList<>(distinctGoodsIds));
		if ("select".equals(goodsMode)) {
			orchestrationParams.put("rebate", 1);
		}

		String acceptLang = resolveCountryCode(request);
		orchestrationParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE, page);
		orchestrationParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE_SIZE, pageSize);
		orchestrationParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE, acceptLang);

		Map<String, Object> result = wxappGoodsItemsListQueryOrchestrator.queryShopItemListData(companyId, orchestrationParams, List.of());
		if (result.containsKey("nosalestore")) {
			return result;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		if (list == null) {
			list = new ArrayList<>();
		}

		wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, authUserId, list, acceptLang);
		goodsItemsListPromotionEnrichmentService.enrich(list);
		result.put("goods_total", result.get("total_count"));
		applyShopItemsListStoreBackfill(companyId, list);

		LinkedHashMap<String, Object> brandParams =
				wxappGoodsItemsListQueryOrchestrator.effectiveBusinessParamsForWxappItemList(companyId, new LinkedHashMap<>(orchestrationParams));
		Map<String, Object> brandEnv = pointsmallFrontBrandListService.getBrandList(buildWxappBrandFilter(companyId, brandParams));
		result.put("brand_list", brandEnv.get("brand_list") != null ? brandEnv.get("brand_list") : Map.of("total_count", 0L, "list", List.of()));
		result.put("select_tags_list", buildSelectTagsListForFront(companyId, list));
		result.put("list", list);
		return result;
	}

	private static Map<String, Object> shopEarlyEmpty() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("total_count", 0L);
		m.put("list", Collections.emptyList());
		return m;
	}

	private static int parseListPage(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 1;
		}
		int p;
		try {
			p = Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			p = 1;
		}
		return Math.max(1, p);
	}

	private static int parseListPageSize(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 20;
		}
		try {
			int parsed = Integer.parseInt(raw.trim());
			return parsed > 0 ? parsed : 20;
		} catch (NumberFormatException e) {
			return 20;
		}
	}

	private static boolean isSalesmanPageQuery(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		String t = raw.trim();
		if ("1".equals(t)) {
			return true;
		}
		try {
			return Integer.parseInt(t) == 1;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private String resolveCountryCode(HttpServletRequest request) {
		String q = request.getParameter("country_code");
		if (StringUtils.hasText(q)) {
			String t = q.trim();
			if (!t.isEmpty()) {
				return t;
			}
		}
		String resolved = RequestLangTag.current(langueProperties);
		if (StringUtils.hasText(resolved) && !"zh-CN".equals(resolved)) {
			return resolved;
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw instanceof Map<?, ?> ud) {
			Object cc = ud.get("country_code");
			if (cc != null) {
				String s = cc.toString().trim();
				if (!s.isEmpty()) {
					return s;
				}
			}
		}
		return "zh-CN";
	}

	private void applyShopItemsListStoreBackfill(long companyId, List<Map<String, Object>> rows) {
		for (Map<String, Object> row : rows) {
			if (longVal(row.get("store")) > 0L) {
				continue;
			}
			long distId = longVal(row.get("distributor_id"));
			boolean allowPlatform = true;
			if (distId > 0L) {
				long itemId = longVal(row.get("item_id"));
				List<DistributorItems> dis = distributorItemsMapper.selectList(new LambdaQueryWrapper<DistributorItems>()
						.eq(DistributorItems::getCompanyId, companyId)
						.eq(DistributorItems::getDistributorId, distId)
						.eq(DistributorItems::getItemId, itemId)
						.last("LIMIT 1"));
				DistributorItems di = dis.isEmpty() ? null : dis.get(0);
				if (di != null) {
					Long dst = di.getStore();
					if (dst != null && dst > 0L) {
						row.put("store", dst.intValue());
					}
					if (Boolean.TRUE.equals(di.getIsTotalStore())) {
						allowPlatform = false;
					}
				}
			}
			if (allowPlatform && longVal(row.get("store")) <= 0L) {
				long goodsId = longVal(row.get("goods_id"));
				if (goodsId <= 0L) {
					continue;
				}
				List<Items> pl = itemsMapper.selectList(new LambdaQueryWrapper<Items>().eq(Items::getCompanyId, companyId).eq(Items::getGoodsId, goodsId)
						.eq(Items::getApproveStatus, "onsale").gt(Items::getStore, 0).orderByAsc(Items::getItemId).last("LIMIT 1"));
				if (!pl.isEmpty() && pl.get(0).getStore() != null && pl.get(0).getStore() > 0) {
					row.put("store", pl.get(0).getStore());
				}
			}
		}
	}

	private PointsmallFrontBrandFilter buildWxappBrandFilter(long companyId, LinkedHashMap<String, Object> params) {
		List<Long> mainCats = new ArrayList<>();
		Object ic = params.get("item_category");
		if (ic instanceof List<?> l) {
			for (Object o : l) {
				if (o == null) {
					continue;
				}
				try {
					mainCats.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		Long saleCat = null;
		Object cid = params.get("category_id");
		if (cid != null && StringUtils.hasText(cid.toString())) {
			try {
				saleCat = Long.parseLong(cid.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		String kw = null;
		Object kwo = params.get("keywords");
		if (kwo != null && StringUtils.hasText(kwo.toString())) {
			kw = kwo.toString().trim();
		}
		Long distributorId = null;
		Object d = params.get("distributor_id");
		if (d instanceof List<?> dl && !dl.isEmpty()) {
			distributorId = parseLongLoose(dl.get(0));
		} else if (d != null) {
			distributorId = parseLongLoose(d);
		}
		return new PointsmallFrontBrandFilter(companyId, mainCats, saleCat, kw, distributorId);
	}

	private static Long parseLongLoose(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v >= 0L ? v : null;
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s) || "false".equalsIgnoreCase(s)) {
			return null;
		}
		try {
			return Long.parseLong(s.split(",")[0].trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private List<Map<String, Object>> buildSelectTagsListForFront(long companyId, List<Map<String, Object>> list) {
		LinkedHashSet<Long> itemIdSet = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			long id = longVal(row.get("item_id"));
			if (id > 0L) {
				itemIdSet.add(id);
			}
		}
		if (itemIdSet.isEmpty()) {
			return List.of();
		}
		List<ItemsRelTags> rels = itemsRelTagsRepository.listRowsByCompanyIdAndItemIdIn(companyId, itemIdSet);
		Map<Long, Map<String, Object>> byTagId = new LinkedHashMap<>();
		for (ItemsRelTags rt : rels) {
			if (rt.getTagId() == null) {
				continue;
			}
			ItemsTags tg = itemsTagsRepository.selectById(rt.getTagId());
			if (tg == null) {
				continue;
			}
			int fs = tg.getFrontShow() != null ? tg.getFrontShow() : 0;
			if (fs != 1) {
				continue;
			}
			long tid = tg.getTagId();
			if (byTagId.containsKey(tid)) {
				continue;
			}
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("item_id", rt.getItemId());
			one.put("company_id", companyId);
			one.put("tag_id", tid);
			one.put("tag_name", tg.getTagName() != null ? tg.getTagName() : "");
			one.put("tag_color", tg.getTagColor() != null ? tg.getTagColor() : "");
			one.put("font_color", tg.getFontColor() != null ? tg.getFontColor() : "");
			one.put("description", tg.getDescription());
			Long dist = tg.getDistributorId();
			one.put("distributor_id", dist != null ? dist : 0L);
			one.put("front_show", fs);
			one.put("tag_icon", tg.getTagIcon());
			one.put("created", tg.getCreated());
			one.put("updated", tg.getUpdated());
			byTagId.put(tid, one);
		}
		return new ArrayList<>(byTagId.values());
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
