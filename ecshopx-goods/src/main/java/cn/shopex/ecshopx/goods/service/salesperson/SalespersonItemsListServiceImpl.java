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

package cn.shopex.ecshopx.goods.service.salesperson;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.distributor.DistributorCouponListAppendService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemCrossBorderTaxQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.goods.service.pointsmall.PointsmallFrontBrandFilter;
import cn.shopex.ecshopx.goods.service.pointsmall.PointsmallFrontBrandListService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class SalespersonItemsListServiceImpl implements SalespersonItemsListService {

	public static final Object EMPTY_TOP_LEVEL_ARRAY = new Object();

	private static final String LIST_ERR = "获取商品列表出错.";

	private static final Set<String> ITEMS_LIST_NEW_FILTER_COLUMNS = Set.of("item_id", "store", "is_point", "barcode", "approve_status", "company_id",
			"item_type", "is_default", "regions_id", "goods_id", "distributor_id", "brand_id", "rebate", "price", "item_category", "rebate_type",
			"audit_status", "type", "item_name", "item_bn", "keywords", "default_item_id", "is_gift", "templates_id", "goods_bn", "supplier_id");

	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final DistributorMapper distributorMapper;
	private final DistributorCouponListAppendService distributorCouponListAppendService;
	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final MemberItemsFavMapper memberItemsFavMapper;
	private final CartMapper cartMapper;
	private final ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository;
	private final ItemsRepository itemsRepository;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final TdkGivenSaveService tdkGivenSaveService;
	private final PointsmallFrontBrandListService pointsmallFrontBrandListService;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsRepository itemsTagsRepository;
	private final DistributorListQueryService distributorListQueryService;
	private final LangueProperties langueProperties;

	public SalespersonItemsListServiceImpl(WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			DistributorMapper distributorMapper,
			DistributorCouponListAppendService distributorCouponListAppendService,
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			MemberItemsFavMapper memberItemsFavMapper,
			CartMapper cartMapper,
			ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository,
			ItemsRepository itemsRepository,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			TdkGivenSaveService tdkGivenSaveService,
			PointsmallFrontBrandListService pointsmallFrontBrandListService,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsTagsRepository itemsTagsRepository,
			DistributorListQueryService distributorListQueryService,
			LangueProperties langueProperties) {
		this.wxappGoodsItemsListQueryOrchestrator = wxappGoodsItemsListQueryOrchestrator;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.distributorMapper = distributorMapper;
		this.distributorCouponListAppendService = distributorCouponListAppendService;
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.memberItemsFavMapper = memberItemsFavMapper;
		this.cartMapper = cartMapper;
		this.itemCrossBorderTaxQueryRepository = itemCrossBorderTaxQueryRepository;
		this.itemsRepository = itemsRepository;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.tdkGivenSaveService = tdkGivenSaveService;
		this.pointsmallFrontBrandListService = pointsmallFrontBrandListService;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
		this.distributorListQueryService = distributorListQueryService;
		this.langueProperties = langueProperties;
	}

	@Override
	public Object execute(long companyId, long userId, HttpServletRequest request) {
		validatePaging(request);
		if (shouldValidateDistributor(request) && !isDistributorValidForCompany(companyId, request)) {
			return EMPTY_TOP_LEVEL_ARRAY;
		}

		ParamsBuildOutcome built = buildSalespersonParams(companyId, userId, request);
		if (built.earlyEmptyListBody != null) {
			return built.earlyEmptyListBody;
		}
		LinkedHashMap<String, Object> params = built.params;

		int page = Integer.parseInt(request.getParameter("page").trim());
		int pageSize = Integer.parseInt(request.getParameter("pageSize").trim());
		boolean defaultItemMode = Boolean.TRUE.equals(params.get("is_default"));

		String acceptLang = resolveAcceptLanguage(request);
		LinkedHashMap<String, Object> orchestrationParams = new LinkedHashMap<>(params);
		orchestrationParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE, acceptLang);

		LinkedHashMap<String, Object> effectiveParamsForNewFilter;
		Map<String, Object> result;
		if (!defaultItemMode) {
			effectiveParamsForNewFilter =
					wxappGoodsItemsListQueryOrchestrator.effectiveBusinessParamsForWxappItemList(companyId, new LinkedHashMap<>(orchestrationParams));
			result = wxappGoodsItemsListQueryOrchestrator.querySalespersonSkuItemsList(companyId, orchestrationParams, List.of());
		} else {
			orchestrationParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE, page);
			orchestrationParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE_SIZE, pageSize);
			effectiveParamsForNewFilter =
					wxappGoodsItemsListQueryOrchestrator.effectiveBusinessParamsForWxappItemList(companyId, orchestrationParams);
			result = wxappGoodsItemsListQueryOrchestrator.queryItemListData(companyId, orchestrationParams, List.of());
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		if (list == null) {
			list = List.of();
		}

		if (defaultItemMode) {
			distributorCouponListAppendService.appendDistributorInfo(companyId, list);
		}

		if (!list.isEmpty()) {
			wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, userId, list, acceptLang);
			goodsItemsListPromotionEnrichmentService.enrich(list);
		}

		if (!list.isEmpty()) {
			applyCartNumAndFav(companyId, userId, request, list);
		}
		if (!list.isEmpty()) {
			applyCrossBorderTax(companyId, list);
		}

		for (Map<String, Object> row : list) {
			Object ty = row.get("type");
			row.put("type", ty != null ? String.valueOf(ty) : "0");
		}

		LinkedHashMap<String, Object> body = new LinkedHashMap<>(result);
		body.put("newFilter", buildNewFilterEcho(companyId, effectiveParamsForNewFilter));
		Map<String, Object> brandEnv = pointsmallFrontBrandListService.getBrandList(buildWxappBrandFilter(companyId, params));
		Object bl = brandEnv.get("brand_list");
		body.put("brand_list", bl != null ? bl : Map.of("total_count", 0L, "list", List.of()));
		body.put("select_tags_list", buildSelectTagsListForFront(companyId, list));
		body.put("list", list);
		appendCur(body, companyId);
		if ("1".equals(String.valueOf(params.get("is_tdk")))) {
			String countryCodeRaw = request.getParameter("country_code");
			body.put("tdk_data", tdkGivenSaveService.getGivenSetInfo("list", companyId, countryCodeRaw));
		}
		return body;
	}

	private static boolean shouldValidateDistributor(HttpServletRequest request) {
		String distributorRaw = request.getParameter("distributor_id");
		if (!StringUtils.hasText(distributorRaw) || "false".equalsIgnoreCase(distributorRaw.trim())) {
			return false;
		}
		String t = distributorRaw.trim();
		if (t.contains(",")) {
			String first = t.split(",")[0].trim();
			if (!StringUtils.hasText(first)) {
				return false;
			}
			try {
				return Long.parseLong(first) > 0L;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		try {
			return Long.parseLong(t) > 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isDistributorValidForCompany(long companyId, HttpServletRequest request) {
		String raw = request.getParameter("distributor_id").trim();
		long did;
		if (raw.contains(",")) {
			try {
				did = Long.parseLong(raw.split(",")[0].trim());
			} catch (NumberFormatException e) {
				return true;
			}
		} else {
			try {
				did = Long.parseLong(raw);
			} catch (NumberFormatException e) {
				return true;
			}
		}
		if (did <= 0L) {
			return true;
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getDistributorId, did).eq(Distributor::getIsValid, "true");
		return distributorMapper.selectCount(w) > 0L;
	}

	private ParamsBuildOutcome buildSalespersonParams(long companyId, long userId, HttpServletRequest request) {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put(WxappGoodsItemsListQueryOrchestrator.PARAM_SALESPERSON_EXCLUDE_CROSS_BORDER_TYPE, Boolean.TRUE);

		String approveRaw = request.getParameter("approve_status");
		if (StringUtils.hasText(approveRaw)) {
			String[] parts = approveRaw.split(",", -1);
			List<String> tokens = new ArrayList<>();
			for (String p : parts) {
				tokens.add(p.trim());
			}
			boolean anyWhitelist = tokens.stream().anyMatch(t -> "onsale".equals(t) || "only_show".equals(t));
			if (anyWhitelist) {
				params.put("approve_status", tokens);
			} else {
				params.put("approve_status", List.of("onsale", "only_show"));
			}
		} else {
			params.put("approve_status", List.of("onsale", "only_show"));
		}
		params.put("audit_status", "approved");

		String categoryAlias = request.getParameter("category");
		if (ValuePresence.hasEffectiveValue(categoryAlias)) {
			params.put("category_id", categoryAlias.trim());
		}

		String keywords = request.getParameter("keywords");
		if (ValuePresence.hasEffectiveValue(keywords)) {
			params.put("item_name", keywords.trim());
		}

		String itemType = request.getParameter("item_type");
		params.put("item_type", StringUtils.hasText(itemType) ? itemType.trim() : "normal");

		String distributorRaw = request.getParameter("distributor_id");
		if (StringUtils.hasText(distributorRaw) && !"false".equalsIgnoreCase(distributorRaw.trim())) {
			if (distributorRaw.contains(",")) {
				List<Long> ds = parseLongListCsv(distributorRaw);
				if (!ds.isEmpty()) {
					params.put("distributor_id", ds);
					params.put("is_can_sale", Boolean.TRUE);
				}
			} else {
				try {
					long d = Long.parseLong(distributorRaw.trim());
					params.put("distributor_id", d);
					if (d != 0L) {
						params.put("is_can_sale", Boolean.TRUE);
					}
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		if ("0".equals(StringUtils.hasText(distributorRaw) ? distributorRaw.trim() : "")) {
			params.put("distributor_id", 0L);
		}

		params.put("is_gift", 0);

		String goodsSort = request.getParameter("goodsSort");
		params.put("goodsSort", StringUtils.hasText(goodsSort) ? goodsSort.trim() : "");

		String isDefault = request.getParameter("is_default");
		boolean defaultTrue = isDefault == null || !"false".equalsIgnoreCase(isDefault.trim());
		if (defaultTrue) {
			params.put("is_default", Boolean.TRUE);
		}

		String mainCat = request.getParameter("main_category");
		if (ValuePresence.hasEffectiveValue(mainCat)) {
			try {
				long mid = Long.parseLong(mainCat.trim());
				List<String> keys = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, List.of(mid));
				if (!keys.isEmpty()) {
					params.put("item_category", keys);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}

		String tagId = request.getParameter("tag_id");
		if (StringUtils.hasText(tagId)) {
			params.put("tag_id", tagId.trim());
		}

		String categoryIdTree = request.getParameter("category_id");
		if (ValuePresence.hasEffectiveValue(categoryIdTree)) {
			try {
				long cid = Long.parseLong(categoryIdTree.trim());
				List<Long> treeIds = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, cid);
				if (treeIds.isEmpty()) {
					return new ParamsBuildOutcome(null, Map.of("list", List.of(), "total_count", 0L));
				}
				@SuppressWarnings("unchecked")
				List<Long> existing = (List<Long>) params.get("item_id");
				if (existing != null && !existing.isEmpty()) {
					Set<Long> treeSet = new LinkedHashSet<>(treeIds);
					List<Long> inter = existing.stream().filter(treeSet::contains).collect(Collectors.toList());
					if (inter.isEmpty()) {
						return new ParamsBuildOutcome(null, Map.of("list", List.of(), "total_count", 0L));
					}
					params.put("item_id", inter);
				} else {
					params.put("category_resolved_item_ids", treeIds);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid category_id
			}
		}

		String isTdk = request.getParameter("is_tdk");
		if ("1".equals(isTdk != null ? isTdk.trim() : "")) {
			params.put("is_tdk", "1");
		}

		return new ParamsBuildOutcome(params, null);
	}

	private static List<Long> parseLongListCsv(String raw) {
		String[] parts = raw.split(",");
		List<Long> out = new ArrayList<>();
		for (String p : parts) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				out.add(Long.parseLong(p.trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	private void validatePaging(HttpServletRequest request) {
		String ps = request.getParameter("page");
		String pz = request.getParameter("pageSize");
		LinkedHashMap<String, List<String>> requiredErrors = new LinkedHashMap<>();
		if (!StringUtils.hasText(ps)) {
			requiredErrors.put("page", List.of("validation.required"));
		}
		if (!StringUtils.hasText(pz)) {
			requiredErrors.put("pageSize", List.of("validation.required"));
		}
		if (!requiredErrors.isEmpty()) {
			throw new BadRequestException(LIST_ERR, requiredErrors);
		}
		int p;
		try {
			p = Integer.parseInt(ps.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(LIST_ERR, Map.of("page", List.of("validation.integer")));
		}
		int s;
		try {
			s = Integer.parseInt(pz.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(LIST_ERR, Map.of("pageSize", List.of("validation.integer")));
		}
		if (p < 1) {
			throw new BadRequestException(LIST_ERR, Map.of("page", List.of("validation.min.numeric")));
		}
		if (s < 1) {
			throw new BadRequestException(LIST_ERR, Map.of("pageSize", List.of("validation.min.numeric")));
		}
		if (s > 50) {
			throw new BadRequestException(LIST_ERR, Map.of("pageSize", List.of("validation.max.numeric")));
		}
	}

	private void applyCartNumAndFav(long companyId, long userId, HttpServletRequest request, List<Map<String, Object>> list) {
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			long iid = longVal(row.get("item_id"));
			if (iid > 0L) {
				itemIds.add(iid);
			}
		}
		Set<Long> favIds = Set.of();
		Map<Long, Integer> cartByItem = Map.of();
		if (userId > 0L && !itemIds.isEmpty()) {
			LambdaQueryWrapper<MemberItemsFav> fw = new LambdaQueryWrapper<>();
			fw.eq(MemberItemsFav::getCompanyId, companyId).eq(MemberItemsFav::getUserId, userId).in(MemberItemsFav::getItemId, itemIds);
			favIds = memberItemsFavMapper.selectList(fw).stream().map(MemberItemsFav::getItemId).filter(Objects::nonNull).collect(Collectors.toSet());
			long shopId = 0L;
			String dist = request.getParameter("distributor_id");
			if (StringUtils.hasText(dist) && !"false".equalsIgnoreCase(dist)) {
				try {
					shopId = Long.parseLong(dist.trim().split(",")[0]);
				} catch (NumberFormatException ignored) {
					shopId = 0L;
				}
			}
			LambdaQueryWrapper<Cart> cw = new LambdaQueryWrapper<>();
			cw.eq(Cart::getCompanyId, companyId).eq(Cart::getUserId, userId).in(Cart::getItemId, itemIds).eq(Cart::getShopId, shopId);
			cartByItem = new LinkedHashMap<>();
			for (Cart c : cartMapper.selectList(cw)) {
				if (c.getItemId() == null) {
					continue;
				}
				int n = c.getNum() != null ? c.getNum() : 0;
				cartByItem.merge(c.getItemId(), n, Integer::sum);
			}
		}
		for (Map<String, Object> row : list) {
			long iid = longVal(row.get("item_id"));
			row.put("cart_num", userId > 0L ? cartByItem.getOrDefault(iid, 0) : 0);
			row.put("is_fav", userId > 0L && favIds.contains(iid));
		}
	}

	private void applyCrossBorderTax(long companyId, List<Map<String, Object>> list) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : list) {
			if (longVal(row.get("type")) == 1L) {
				ids.add(longVal(row.get("item_id")));
			}
		}
		Map<Long, Items> byId = Map.of();
		if (!ids.isEmpty()) {
			List<Items> entities = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, ids);
			byId = entities.stream().filter(i -> i.getItemId() != null).collect(Collectors.toMap(Items::getItemId, i -> i, (a, b) -> a));
		}
		for (Map<String, Object> row : list) {
			if (longVal(row.get("type")) != 1L) {
				row.put("cross_border_tax", 0);
				row.put("cross_border_tax_rate", 0);
				continue;
			}
			long itemId = longVal(row.get("item_id"));
			Items it = byId.get(itemId);
			int priceFen = intVal(row.get("price"));
			String taxField = "price";
			int taxBase = priceFen;
			if (nonEmptyInt(row.get("member_price"))) {
				taxField = "member_price";
				taxBase = intVal(row.get("member_price"));
			}
			if (nonEmptyInt(row.get("activity_price"))) {
				taxField = "activity_price";
				taxBase = intVal(row.get("activity_price"));
			}
			BigDecimal ratePercent = itemCrossBorderTaxQueryRepository.resolveEffectiveTaxRatePercent(it, taxBase);
			BigDecimal base = BigDecimal.valueOf(taxBase);
			BigDecimal tax = base.multiply(ratePercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
			row.put("cross_border_tax_rate", ratePercent);
			row.put("cross_border_tax", tax.longValue());
			BigDecimal newPrice = base.add(tax);
			row.put(taxField, newPrice.longValue());
			if ("activity_price".equals(taxField)) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> acts = (List<Map<String, Object>>) row.get("promotion_activity");
				if (acts != null && !acts.isEmpty()) {
					Map<String, Object> last = acts.get(acts.size() - 1);
					last.put("activity_price", newPrice.longValue());
				}
			}
		}
	}

	private static boolean nonEmptyInt(Object v) {
		return intVal(v) > 0;
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
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

	private LinkedHashMap<String, Object> buildNewFilterEcho(long companyId, LinkedHashMap<String, Object> effectiveParams) {
		LinkedHashMap<String, Object> echo = new LinkedHashMap<>();
		for (Map.Entry<String, Object> en : effectiveParams.entrySet()) {
			String key = en.getKey();
			if (key.startsWith("__")) {
				continue;
			}
			if (!isItemsListNewFilterColumnKey(key)) {
				continue;
			}
			echo.put(key, en.getValue());
		}
		if (echo.containsKey("item_id") && echo.containsKey("keywords")) {
			echo.remove("keywords");
		}
		if (!echo.containsKey("distributor_id")) {
			echo.put("distributor_id", buildDefaultNewFilterDistributorIdList(companyId));
		} else {
			echo.put("distributor_id", normalizeDistributorIdForEcho(echo.get("distributor_id")));
		}
		return echo;
	}

	private List<String> buildDefaultNewFilterDistributorIdList(long companyId) {
		List<String> out = new ArrayList<>();
		out.add("0");
		for (Long id : distributorListQueryService.listValidDistributorIdsForCompany(companyId)) {
			if (id != null) {
				out.add(String.valueOf(id));
			}
		}
		return out;
	}

	private static List<String> normalizeDistributorIdForEcho(Object raw) {
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String s = o.toString().trim();
				if (!s.isEmpty()) {
					out.add(s);
				}
			}
			return out;
		}
		if (raw == null) {
			return List.of();
		}
		return List.of(raw.toString().trim());
	}

	private static boolean isItemsListNewFilterColumnKey(String key) {
		if ("price|gte".equals(key) || "price|lte".equals(key) || "store|gt".equals(key) || "store|lt".equals(key)) {
			return true;
		}
		return ITEMS_LIST_NEW_FILTER_COLUMNS.contains(key);
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
					// skip
				}
			}
		}
		Long saleCat = null;
		Object cid = params.get("category_id");
		if (cid != null && StringUtils.hasText(cid.toString())) {
			try {
				saleCat = Long.parseLong(cid.toString().trim());
			} catch (NumberFormatException ignored) {
				// skip
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

	private void appendCur(LinkedHashMap<String, Object> result, long companyId) {
		CurrencyExchangeRate row = companyDefaultCurrencyService.getCur(companyId);
		LinkedHashMap<String, Object> curMap = new LinkedHashMap<>();
		if (row.getId() != null) {
			curMap.put("id", String.valueOf(row.getId()));
		}
		curMap.put("company_id", String.valueOf(companyId));
		curMap.put("currency", row.getCurrency());
		curMap.put("title", row.getTitle());
		curMap.put("symbol", row.getSymbol());
		curMap.put("rate", row.getRate());
		curMap.put("is_default", Boolean.TRUE.equals(row.getIsDefault()));
		if (row.getUsePlatform() != null) {
			curMap.put("use_platform", row.getUsePlatform());
		}
		result.put("cur", curMap);
	}

	private String resolveAcceptLanguage(HttpServletRequest request) {
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

	private static final class ParamsBuildOutcome {
		final LinkedHashMap<String, Object> params;
		final Map<String, Object> earlyEmptyListBody;

		ParamsBuildOutcome(LinkedHashMap<String, Object> params, Map<String, Object> earlyEmptyListBody) {
			this.params = params;
			this.earlyEmptyListBody = earlyEmptyListBody;
		}
	}
}
