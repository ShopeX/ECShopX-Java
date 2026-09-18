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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.popularize.CompanyPopularizeConfigReadService;
import cn.shopex.ecshopx.distribution.repository.DistributorOnsaleSkuListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorOnsaleSkuListRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsListSkuSpecApplier;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAttr;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsAttrListRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsListQueryRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierOperatorQueryRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsItemsListFacadeService {

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final SupplierItemsListQueryRepository supplierItemsListQueryRepository;
	private final ItemsRepository itemsRepository;
	private final CompanyPopularizeConfigReadService companyPopularizeConfigReadService;
	private final DistributorListQueryService distributorListQueryService;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemStoreService itemStoreService;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final SupplierOperatorQueryRepository supplierOperatorQueryRepository;
	private final ItemsSkuListAssembler itemsSkuListAssembler;
	private final GoodsItemsListEnrichmentService goodsItemsListEnrichmentService;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final ItemsMedicineService itemsMedicineService;
	private final SupplierItemsAttrListRepository supplierItemsAttrListRepository;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final DistributorOnsaleSkuListRepository distributorOnsaleSkuListRepository;
	private final DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;
	private final LangueProperties langueProperties;

	public GoodsItemsListFacadeService(ItemsListQueryRepository itemsListQueryRepository,
			SupplierItemsListQueryRepository supplierItemsListQueryRepository, ItemsRepository itemsRepository,
			CompanyPopularizeConfigReadService companyPopularizeConfigReadService, DistributorListQueryService distributorListQueryService,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver, ItemStoreService itemStoreService,
			ItemsRelTagsRepository itemsRelTagsRepository, SupplierOperatorQueryRepository supplierOperatorQueryRepository,
			ItemsSkuListAssembler itemsSkuListAssembler, GoodsItemsListEnrichmentService goodsItemsListEnrichmentService,
			ItemsListMultiLangApplier itemsListMultiLangApplier, ItemsMedicineService itemsMedicineService,
			SupplierItemsAttrListRepository supplierItemsAttrListRepository,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			DistributorOnsaleSkuListRepository distributorOnsaleSkuListRepository,
			DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher,
			LangueProperties langueProperties) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.supplierItemsListQueryRepository = supplierItemsListQueryRepository;
		this.itemsRepository = itemsRepository;
		this.companyPopularizeConfigReadService = companyPopularizeConfigReadService;
		this.distributorListQueryService = distributorListQueryService;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemStoreService = itemStoreService;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.supplierOperatorQueryRepository = supplierOperatorQueryRepository;
		this.itemsSkuListAssembler = itemsSkuListAssembler;
		this.goodsItemsListEnrichmentService = goodsItemsListEnrichmentService;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.itemsMedicineService = itemsMedicineService;
		this.supplierItemsAttrListRepository = supplierItemsAttrListRepository;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.distributorOnsaleSkuListRepository = distributorOnsaleSkuListRepository;
		this.distributorItemsListSkuSpecApplier = distributorItemsListSkuSpecApplier;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
		this.langueProperties = langueProperties;
	}

	private void applyReplaceSkuSpecForPlatformItemRows(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		distributorItemsListSkuSpecApplier.apply(companyId, rows);
	}

	private void dealListStoreForRows(long companyId, List<Map<String, Object>> rows, boolean supplierPath) {
		dealListStoreForRows(companyId, rows, supplierPath, false);
	}

	/** PHP {@code ItemsService::dealListStore} — {@code isGetSkuList=true} 时不汇总多规格库存。 */
	private void dealListStoreForRows(long companyId, List<Map<String, Object>> rows, boolean supplierPath,
			boolean isGetSkuList) {
		if (isGetSkuList || rows == null || rows.isEmpty()) {
			return;
		}
		if (supplierPath) {
			supplierItemsListQueryRepository.dealListStore(companyId, rows);
		} else {
			itemsListQueryRepository.dealListStore(companyId, rows);
		}
	}

	private void applyWxappDealListStoreIfNeeded(long companyId, Map<String, Object> repoParams, List<Map<String, Object>> rows) {
		if (Boolean.TRUE.equals(repoParams.get(ItemsListQueryRepository.KEY_DIST_REL_VIRT_JOIN))) {
			return;
		}
		itemsListQueryRepository.dealListStore(companyId, rows);
	}

	/** 与商品列表 / 详情一致：填充 logistics_store 并按归属店配送方式合成展示 store。 */
	private void applyWxappFrontDisplayStore(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		itemLogisticsStoreEnricher.enrichListRowsAndApplyDisplayTotal(companyId, 0L, rows);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> listOnsaleItems(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> jwt = (Map<String, Object>) jwtRaw;
		GoodsItemsListParamBuilder pb = new GoodsItemsListParamBuilder(request, jwt, Integer.valueOf(2000));
		LinkedHashMap<String, Object> p = pb.toParamsMap();
		p.put("type", Integer.valueOf(0));
		p.put("is_gift", "0");
		p.put("approve_status", List.of("onsale", "offline_sale"));
		p.put("audit_status", "approved");
		p.put("item_type", "normal");
		p.put("is_sku", Boolean.TRUE);
		p.remove("operate_source");

		long companyId = toLong(p.get("company_id"));
		String operatorType = str(jwt.get("operator_type"));
		long operatorId = toLong(jwt.get("operator_id"));
		long merchantId = toLong(jwt.get("merchant_id"));

		if ("merchant".equalsIgnoreCase(operatorType)) {
			List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
			if (v.isEmpty()) {
				return emptyOnsaleResponseEarly(p, companyId, operatorType, operatorId);
			}
			p.put("distributor_id_in", v.stream().map(Long::intValue).collect(Collectors.toList()));
		}

		applyDistributorQueryBranch(request, p, operatorType, companyId, merchantId, "platform");

		if ("merchant".equalsIgnoreCase(operatorType) && !hasResolvedDistributorId(request)) {
			List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
			if (!v.isEmpty()) {
				p.put("distributor_id_in", v.stream().map(Long::intValue).collect(Collectors.toList()));
			}
		}

		String pm = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
		boolean hasDistributorIn = p.containsKey("distributor_id_in") && p.get("distributor_id_in") instanceof List<?> l && !l.isEmpty();
		boolean eqHeadquarters = p.containsKey("distributor_id_eq") && Integer.valueOf(0).equals(toIntegerBoxed(p.get("distributor_id_eq")));
		boolean usePlatformItemsTable = "platform".equals(pm) || (!hasDistributorIn && eqHeadquarters);

		int warningStore;
		if (usePlatformItemsTable) {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		} else if ("supplier".equalsIgnoreCase(operatorType)) {
			warningStore = itemStoreService.getSupplierWarningStore(companyId, operatorId);
		} else {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		}

		normalizeStoreAndPrice(p);

		Map<String, Object> textEmpty = applyOnsaleTextFilters(p, companyId, operatorType, operatorId);
		if (textEmpty != null) {
			return textEmpty;
		}

		LinkedHashMap<String, Object> querySnap = new LinkedHashMap<>(p);

		int page = pb.getPage();
		int pageSize = pb.getPageSize();
		if (pageSize <= 0) {
			pageSize = 10;
		}
		pageSize = Math.min(pageSize, 2000);
		int off = (page - 1) * pageSize;

		Integer shopDistributorEq = null;
		List<Integer> shopDistributorIn = null;
		if (!usePlatformItemsTable) {
			shopDistributorEq = toIntegerBoxed(p.get("distributor_id_eq"));
			@SuppressWarnings("unchecked")
			List<Integer> din = (List<Integer>) p.get("distributor_id_in");
			if (din != null && !din.isEmpty()) {
				shopDistributorIn = new ArrayList<>(din);
			}
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
		}

		p.remove("is_default_true");
		Map<String, Object> skuParams = new LinkedHashMap<>(p);

		long total;
		List<Map<String, Object>> rows;
		if (usePlatformItemsTable) {
			total = itemsListQueryRepository.countByParams(skuParams);
			List<Items> items = itemsListQueryRepository.selectPageByParamsForSku(skuParams, off, pageSize);
			rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		} else {
			DistributorOnsaleSkuListFilter df = new DistributorOnsaleSkuListFilter();
			df.setCompanyId(companyId);
			df.setDistributorIdEq(shopDistributorEq);
			df.setDistributorIdIn(shopDistributorIn);
			@SuppressWarnings("unchecked")
			List<Long> defIds = (List<Long>) skuParams.get("item_id_or_default_ids");
			if (defIds != null && !defIds.isEmpty()) {
				df.setDefaultItemIds(defIds);
			}
			total = distributorOnsaleSkuListRepository.countByFilter(df);
			List<Long> itemIds = distributorOnsaleSkuListRepository.selectItemIdsPageByFilter(df, off, pageSize);
			List<Items> items = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, itemIds);
			rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		}

		applyReplaceSkuSpecForPlatformItemRows(companyId, rows);
		// is_sku=true：与 PHP dealListStore($result, $isGetSkuList) 一致，保留各 SKU 自身 store
		itemsSkuListAssembler.applyTypeLabels(rows);
		itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
		goodsItemsListEnrichmentService.enrichAll(companyId, rows, false, null);
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);

		LinkedHashMap<String, Object> filterInternal = new LinkedHashMap<>(p);
		if (!usePlatformItemsTable) {
			filterInternal.remove("approve_status");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rows);
		out.put("total_count", total);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, filterInternal, true));
		return out;
	}

	private Map<String, Object> emptyOnsaleResponseEarly(LinkedHashMap<String, Object> p, long companyId, String operatorType, long operatorId) {
		int warningStore;
		if ("supplier".equalsIgnoreCase(operatorType)) {
			warningStore = itemStoreService.getSupplierWarningStore(companyId, operatorId);
		} else {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		}
		LinkedHashMap<String, Object> querySnap = new LinkedHashMap<>(p);
		LinkedHashMap<String, Object> filterInternal = new LinkedHashMap<>(p);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("total_count", 0);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, filterInternal, true));
		return out;
	}

	/**
	 * @return 非 null 表示应直接返回的空列表响应（filter 已组装）
	 */
	private Map<String, Object> applyOnsaleTextFilters(LinkedHashMap<String, Object> p, long companyId, String operatorType, long operatorId) {
		List<Long> mergedItemIds = null;
		if (StringUtils.hasText(str(p.get("keywords")))) {
			List<Long> ids = itemsListQueryRepository.listDefaultItemIdsByKeywordsOrBlock(companyId, str(p.get("keywords")));
			if (ids.isEmpty()) {
				return emptyOnsaleTextFail(p, companyId, operatorType, operatorId);
			}
			mergedItemIds = new ArrayList<>(ids);
		}
		if (StringUtils.hasText(str(p.get("item_name")))) {
			List<Long> ids = itemsListQueryRepository.listDefaultItemIdsByItemNameContainsOnly(companyId, str(p.get("item_name")));
			if (ids.isEmpty()) {
				return emptyOnsaleTextFail(p, companyId, operatorType, operatorId);
			}
			if (mergedItemIds == null || mergedItemIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(ids);
			} else {
				mergedItemIds = mergedItemIds.stream().filter(ids::contains).collect(Collectors.toList());
				if (mergedItemIds.isEmpty()) {
					return emptyOnsaleTextFail(p, companyId, operatorType, operatorId);
				}
			}
		}
		if (StringUtils.hasText(str(p.get("item_bn")))) {
			List<Long> ids = itemsListQueryRepository.listDefaultItemIdsByItemBnExact(companyId, str(p.get("item_bn")));
			if (ids.isEmpty()) {
				return emptyOnsaleTextFail(p, companyId, operatorType, operatorId);
			}
			if (mergedItemIds == null || mergedItemIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(ids);
			} else {
				mergedItemIds = mergedItemIds.stream().filter(ids::contains).collect(Collectors.toList());
				if (mergedItemIds.isEmpty()) {
					return emptyOnsaleTextFail(p, companyId, operatorType, operatorId);
				}
			}
		}
		if (StringUtils.hasText(str(p.get("barcode")))) {
			List<Long> ids = itemsListQueryRepository.listDefaultItemIdsByBarcodeExact(companyId, str(p.get("barcode")));
			if (ids.isEmpty()) {
				return emptyOnsaleTextFail(p, companyId, operatorType, operatorId);
			}
			if (mergedItemIds == null || mergedItemIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(ids);
			} else {
				mergedItemIds = mergedItemIds.stream().filter(ids::contains).collect(Collectors.toList());
				if (mergedItemIds.isEmpty()) {
					return emptyOnsaleTextFail(p, companyId, operatorType, operatorId);
				}
			}
		}
		if (mergedItemIds != null && !mergedItemIds.isEmpty()) {
			p.put("item_id_or_default_ids", mergedItemIds);
		}
		return null;
	}

	private Map<String, Object> emptyOnsaleTextFail(LinkedHashMap<String, Object> p, long companyId, String operatorType, long operatorId) {
		String pm = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
		boolean hasDistributorIn = p.containsKey("distributor_id_in") && p.get("distributor_id_in") instanceof List<?> l && !l.isEmpty();
		boolean eqHeadquarters = p.containsKey("distributor_id_eq") && Integer.valueOf(0).equals(toIntegerBoxed(p.get("distributor_id_eq")));
		boolean usePlatformItemsTable = "platform".equals(pm) || (!hasDistributorIn && eqHeadquarters);
		int warningStore;
		if (usePlatformItemsTable) {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		} else if ("supplier".equalsIgnoreCase(operatorType)) {
			warningStore = itemStoreService.getSupplierWarningStore(companyId, operatorId);
		} else {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		}
		LinkedHashMap<String, Object> querySnap = new LinkedHashMap<>(p);
		LinkedHashMap<String, Object> filterInternal = new LinkedHashMap<>(p);
		if (!usePlatformItemsTable) {
			filterInternal.remove("approve_status");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("total_count", 0);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, filterInternal, true));
		return out;
	}

	private static Integer toIntegerBoxed(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static final String SKU_LIST_ERR_MSG = "获取商品列表出错.";

	@SuppressWarnings("unchecked")
	public Map<String, Object> listSku(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> jwt = (Map<String, Object>) jwtRaw;
		GoodsItemsListParamBuilder pb = new GoodsItemsListParamBuilder(request, jwt, null, true);
		LinkedHashMap<String, Object> p = pb.toParamsMap();
		LinkedHashMap<String, Object> querySnap = new LinkedHashMap<>(p);

		long companyId = toLong(p.get("company_id"));
		String operatorType = str(jwt.get("operator_type"));
		long operatorId = toLong(jwt.get("operator_id"));

		String opSrcParam = request.getParameter("operate_source");
		String operateSource = StringUtils.hasText(opSrcParam) ? opSrcParam.trim() : "platform";
		if (StringUtils.hasText(opSrcParam)
				&& !"platform".equalsIgnoreCase(operateSource)
				&& !"supplier".equalsIgnoreCase(operateSource)) {
			throw new BadRequestException("operate_source 仅支持 platform 或 supplier");
		}
		boolean platformPath = "platform".equalsIgnoreCase(operateSource);

		long distributorIdForWarning = toLong(p.get("distributor_id"));
		normalizeStoreAndPrice(p);

		if (!platformPath && "supplier".equalsIgnoreCase(operatorType)) {
			p.put("supplier_id_eq", (int) operatorId);
			p.remove("supplier_id_in");
		}

		p.remove("distributor_id");
		applySkuDistributorEq(p, distributorIdForWarning);
		long mainItemScopeDistributorId = SupplierLinkedMainItemIdResolver.resolveScopeDistributorId(jwt, p);

		@SuppressWarnings("unchecked")
		List<Long> mainCats = (List<Long>) p.remove("main_cat_id_list");
		if (mainCats != null && !mainCats.isEmpty()) {
			List<String> cats = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, mainCats);
			if (!cats.isEmpty()) {
				p.put("item_category_in", cats);
			}
		}

		List<Long> mergedItemIds = null;
		@SuppressWarnings("unchecked")
		List<Long> catList = (List<Long>) p.remove("category_list");
		if (catList != null && !catList.isEmpty()) {
			List<Long> union = new ArrayList<>();
			for (Long c : catList) {
				union.addAll(itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, c));
			}
			union = union.stream().distinct().collect(Collectors.toList());
			if (union.isEmpty()) {
				return emptySkuListResponse(querySnap, p, companyId, platformPath, operatorType, operatorId);
			}
			mergedItemIds = new ArrayList<>(union);
		}

		if (p.containsKey("item_id") && StringUtils.hasText(str(p.get("item_id")))) {
			List<Long> reqIds = parseIdList(str(p.get("item_id")));
			p.remove("item_id");
			if (!reqIds.isEmpty()) {
				if (mergedItemIds == null || mergedItemIds.isEmpty()) {
					mergedItemIds = new ArrayList<>(reqIds);
				} else {
					mergedItemIds = mergedItemIds.stream().filter(reqIds::contains).collect(Collectors.toList());
					if (mergedItemIds.isEmpty()) {
						return emptySkuListResponse(querySnap, p, companyId, platformPath, operatorType, operatorId);
					}
				}
			}
			if (distributorIdForWarning == 0) {
				p.remove("distributor_id_eq");
				p.remove("distributor_id_in");
			}
		}

		if (mergedItemIds != null && !mergedItemIds.isEmpty()) {
			p.put("item_id_or_default_ids", mergedItemIds);
		}

		int warningStore = resolveSkuWarningStore(platformPath, operatorType, companyId, operatorId);
		if (truthy(p.get("is_warning"))) {
			p.put("store_lte_warning", warningStore);
		}

		@SuppressWarnings("unchecked")
		List<Long> tagList = (List<Long>) p.remove("tag_id_list");
		if (tagList != null && !tagList.isEmpty()) {
			List<Long> itemIds = itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIds(companyId, tagList);
			if (itemIds.isEmpty()) {
				return emptySkuListResponse(querySnap, p, companyId, platformPath, operatorType, operatorId);
			}
			@SuppressWarnings("unchecked")
			List<Long> existing = (List<Long>) p.get("item_id_or_default_ids");
			if (existing == null || existing.isEmpty()) {
				p.put("item_id_or_default_ids", itemIds);
			} else {
				List<Long> inter = existing.stream().filter(itemIds::contains).collect(Collectors.toList());
				if (inter.isEmpty()) {
					return emptySkuListResponse(querySnap, p, companyId, platformPath, operatorType, operatorId);
				}
				p.put("item_id_or_default_ids", inter);
			}
		}

		if (StringUtils.hasText(str(p.get("item_bn")))) {
			Map<String, Object> bnQuery = new LinkedHashMap<>(p);
			List<Long> ids = platformPath
					? itemsListQueryRepository.listDistinctDefaultItemIdsByFullParams(bnQuery)
					: supplierItemsListQueryRepository.listDistinctDefaultItemIdsByFullParams(bnQuery);
			if (ids.isEmpty()) {
				return emptySkuListResponse(querySnap, p, companyId, platformPath, operatorType, operatorId);
			}
			intersectItemIdOrDefault(p, ids);
			if (isEmptyIdOrDefault(p)) {
				return emptySkuListResponse(querySnap, p, companyId, platformPath, operatorType, operatorId);
			}
			p.remove("item_bn");
		}

		boolean skuMode = "true".equalsIgnoreCase(str(p.get("is_sku")));
		if (!skuMode) {
			p.put("is_default_true", Boolean.TRUE);
		}

		int page = pb.getPage();
		int pageSize = pb.getPageSize();

		List<Map<String, Object>> rows;
		long total;
		List<Map<String, Object>> supplierMeta;

		if (skuMode) {
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
			p.remove("is_default_true");
			Map<String, Object> skuParams = new LinkedHashMap<>(p);
			@SuppressWarnings("unchecked")
			List<Long> idOrDef = (List<Long>) skuParams.get("item_id_or_default_ids");
			boolean forceFull = idOrDef != null && !idOrDef.isEmpty();
			int off;
			int limit;
			if (forceFull) {
				off = 0;
				limit = -1;
			} else {
				if (pageSize < 1) {
					throw new BadRequestException(SKU_LIST_ERR_MSG);
				}
				off = (page - 1) * pageSize;
				limit = pageSize;
			}
			supplierMeta = null;
			List<SupplierItems> supplierSrows = null;
			if (platformPath) {
				total = itemsListQueryRepository.countByParams(skuParams);
				List<Items> items = itemsListQueryRepository.selectPageByParamsForSku(skuParams, off, limit);
				rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
			} else {
				total = supplierItemsListQueryRepository.countByParams(skuParams);
				supplierSrows = supplierItemsListQueryRepository.selectPageByParams(skuParams, off, limit);
				if (supplierSrows.isEmpty()) {
					throw new ResourceException("查询条件不完整");
				}
				SupplierListAssembly assembly = assembleSupplierListRows(companyId, supplierSrows, mainItemScopeDistributorId);
				rows = assembly.rows();
				supplierMeta = assembly.supplierMeta();
			}
			applyReplaceSkuSpecForPlatformItemRows(companyId, rows);
			// is_sku=true：不汇总多规格库存（PHP dealListStore 第二参数为 true 时直接返回）
			if (platformPath) {
				itemsSkuListAssembler.applyTypeLabels(rows);
			}
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, !platformPath, supplierMeta);
			if (!platformPath) {
				SupplierAdminItemsListWireMapper.apply(rows, supplierSrows, supplierWireNullableColumns(supplierSrows));
			}
			if (platformPath) {
				itemsMedicineService.applyMedicineDataToRows(companyId, rows);
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("list", rows);
			out.put("total_count", total);
			out.put("warning_store", warningStore);
			out.put("filter", GoodsItemsListResponseFilterBuilder.buildForPath(!platformPath, querySnap, p, skuMode));
			return out;
		}

		if (pageSize <= 0) {
			pageSize = 10;
		}
		supplierMeta = null;
		List<SupplierItems> supplierSrows = null;
		if (platformPath) {
			total = itemsListQueryRepository.countByParams(p);
			int off = (page - 1) * pageSize;
			List<Items> items = itemsListQueryRepository.selectPageByParams(p, off, pageSize);
			rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
			dealListStoreForRows(companyId, rows, false);
			itemsSkuListAssembler.applyTypeLabels(rows);
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, false, null);
		} else {
			total = supplierItemsListQueryRepository.countByParams(p);
			int off = (page - 1) * pageSize;
			supplierSrows = supplierItemsListQueryRepository.selectPageByParams(p, off, pageSize);
			SupplierListAssembly assembly = assembleSupplierListRows(companyId, supplierSrows, mainItemScopeDistributorId);
			rows = assembly.rows();
			supplierMeta = assembly.supplierMeta();
			dealListStoreForRows(companyId, rows, true);
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, true, supplierMeta);
			SupplierAdminItemsListWireMapper.apply(rows, supplierSrows, supplierWireNullableColumns(supplierSrows));
		}
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rows);
		out.put("total_count", total);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.buildForPath(!platformPath, querySnap, p, skuMode));
		return out;
	}

	/**
	 * 运营后台会员价列表：平台 SKU 管道与 {@link #listSku} 平台分支一致，成功体仅含 {@code total_count} 与 {@code list}。
	 */
	public Map<String, Object> buildAdminPlatformSkuListForMemberPrice(long companyId, long seedItemId, boolean multiSpec,
			String acceptLanguageHeader) {
		LinkedHashMap<String, Object> skuParams = new LinkedHashMap<>();
		skuParams.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		skuParams.put("is_sku", Boolean.TRUE);
		if (multiSpec) {
			skuParams.put(ItemsListQueryRepository.KEY_MEMBER_PRICE_DEFAULT_ITEM_ID_EQ, seedItemId);
		} else {
			skuParams.put(ItemsListQueryRepository.KEY_MEMBER_PRICE_ITEM_ID_EQ, seedItemId);
		}
		long total = itemsListQueryRepository.countByParams(skuParams);
		List<Items> items = itemsListQueryRepository.selectPageByParamsForSku(skuParams, 0, -1);
		List<Map<String, Object>> rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		replaceDefaultItemIdWhenUnsetForMemberPriceSkuRows(rows);
		applyReplaceSkuSpecForPlatformItemRows(companyId, rows);
		// is_sku=true：与 PHP dealListStore($result, $isGetSkuList) 一致，保留各 SKU 自身 store
		itemsSkuListAssembler.applyTypeLabels(rows);
		itemsListMultiLangApplier.applyToRows(companyId, acceptLanguageHeader, rows);
		goodsItemsListEnrichmentService.enrichAll(companyId, rows, false, null);
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	/**
	 * When {@code default_item_id} is absent, null, zero, or non-numeric, set it to the row's {@code item_id} so SKU list
	 * rows match the member-price list contract without altering generic goods list responses.
	 */
	private static void replaceDefaultItemIdWhenUnsetForMemberPriceSkuRows(List<Map<String, Object>> rows) {
		for (Map<String, Object> row : rows) {
			if (!row.containsKey("default_item_id")) {
				row.put("default_item_id", row.get("item_id"));
				continue;
			}
			Object def = row.get("default_item_id");
			if (def == null) {
				row.put("default_item_id", row.get("item_id"));
				continue;
			}
			try {
				if (toLong(def) == 0L) {
					row.put("default_item_id", row.get("item_id"));
				}
			} catch (NumberFormatException ignored) {
				row.put("default_item_id", row.get("item_id"));
			}
		}
	}

	/**
	 * 微信小程序商品列表（默认商品维度）：查询、聚合库存与规格标签、多语言、药品字段；不执行后台列表用的 {@link GoodsItemsListEnrichmentService#enrichAll}。
	 */
	public Map<String, Object> wxappQueryDefaultItemList(long companyId, Map<String, Object> repoParams, int page, int pageSize,
			String goodsSortToken, String acceptLanguageHeader) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(repoParams);
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		p.put("is_default_true", Boolean.TRUE);
		int pg = page < 1 ? 1 : page;
		int ps = pageSize > 50 ? 50 : (pageSize < 1 ? 10 : pageSize);
		int off = (pg - 1) * ps;
		long total = itemsListQueryRepository.countByParams(p);
		List<Items> items = itemsListQueryRepository.selectPageByParamsWithWxappGoodsSort(p, off, ps, goodsSortToken);
		List<Map<String, Object>> rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		applyWxappDealListStoreIfNeeded(companyId, p, rows);
		applyWxappFrontDisplayStore(companyId, rows);
		itemsSkuListAssembler.applyTypeLabels(rows);
		String lang = StringUtils.hasText(acceptLanguageHeader) ? acceptLanguageHeader.trim() : "zh-CN";
		itemsListMultiLangApplier.applyToRows(companyId, lang, rows);
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	/**
	 * 与 {@link #wxappQueryDefaultItemList} 相同，但不将 {@code pageSize} 上限截断为 50；{@code pageSize} &lt; 1 时默认 20。
	 */
	public Map<String, Object> wxappQueryDefaultItemListNoMaxPageSize(long companyId, Map<String, Object> repoParams, int page, int pageSize,
			String goodsSortToken, String acceptLanguageHeader) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(repoParams);
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		p.put("is_default_true", Boolean.TRUE);
		int pg = page < 1 ? 1 : page;
		int ps = pageSize < 1 ? 20 : pageSize;
		int off = (pg - 1) * ps;
		long total = itemsListQueryRepository.countByParams(p);
		List<Items> items = itemsListQueryRepository.selectPageByParamsWithWxappGoodsSort(p, off, ps, goodsSortToken);
		List<Map<String, Object>> rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		applyWxappDealListStoreIfNeeded(companyId, p, rows);
		applyWxappFrontDisplayStore(companyId, rows);
		itemsSkuListAssembler.applyTypeLabels(rows);
		String lang = StringUtils.hasText(acceptLanguageHeader) ? acceptLanguageHeader.trim() : "zh-CN";
		itemsListMultiLangApplier.applyToRows(companyId, lang, rows);
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	/**
	 * 微信小程序 SKU 全量列表：未限定默认商品维度时，按筛选条件一次性查询全部 SKU（全量拉取，不做分页切片），并聚合库存、规格标签、多语言与药品字段。
	 */
	public Map<String, Object> wxappQuerySkuItemList(long companyId, Map<String, Object> repoParams, String acceptLanguageHeader) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(repoParams);
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		p.remove("is_default_true");
		long total = itemsListQueryRepository.countByParams(p);
		List<Items> items = itemsListQueryRepository.selectPageByParamsForSku(p, 0, -1);
		List<Map<String, Object>> rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		applyReplaceSkuSpecForPlatformItemRows(companyId, rows);
		itemsSkuListAssembler.applyTypeLabels(rows);
		String lang = StringUtils.hasText(acceptLanguageHeader) ? acceptLanguageHeader.trim() : "zh-CN";
		itemsListMultiLangApplier.applyToRows(companyId, lang, rows);
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	/**
	 * Wxapp SKU full list with {@code goodsSort} + {@code item_id} DESC ordering (salesperson and similar paths).
	 */
	public Map<String, Object> wxappQuerySkuItemListWithGoodsSort(long companyId, Map<String, Object> repoParams, String goodsSortToken,
			String acceptLanguageHeader) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(repoParams);
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		p.remove("is_default_true");
		long total = itemsListQueryRepository.countByParams(p);
		List<Items> items = itemsListQueryRepository.selectPageByParamsForSkuWithWxappGoodsSort(p, 0, -1, goodsSortToken);
		List<Map<String, Object>> rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		applyReplaceSkuSpecForPlatformItemRows(companyId, rows);
		itemsSkuListAssembler.applyTypeLabels(rows);
		String lang = StringUtils.hasText(acceptLanguageHeader) ? acceptLanguageHeader.trim() : "zh-CN";
		itemsListMultiLangApplier.applyToRows(companyId, lang, rows);
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	private Map<String, Object> emptySkuListResponse(LinkedHashMap<String, Object> querySnap, LinkedHashMap<String, Object> p, long companyId,
			boolean platformPath, String operatorType, long operatorId) {
		int warningStore = resolveSkuWarningStore(platformPath, operatorType, companyId, operatorId);
		boolean skuMode = "true".equalsIgnoreCase(str(p.get("is_sku")));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("total_count", 0);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
		return out;
	}

	private static void applySkuDistributorEq(LinkedHashMap<String, Object> p, long distributorVal) {
		p.put("distributor_id_eq", distributorVal == 0 ? 0 : (int) distributorVal);
	}

	private int resolveSkuWarningStore(boolean platformPath, String operatorType, long companyId, long operatorId) {
		if (platformPath) {
			return itemStoreService.getPlatformWarningStore(companyId);
		}
		if ("supplier".equalsIgnoreCase(operatorType)) {
			return itemStoreService.getSupplierWarningStore(companyId, operatorId);
		}
		return itemStoreService.getPlatformWarningStore(companyId);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> list(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> jwt = (Map<String, Object>) jwtRaw;
		GoodsItemsListParamBuilder pb = new GoodsItemsListParamBuilder(request, jwt);
		LinkedHashMap<String, Object> p = pb.toParamsMap();
		LinkedHashMap<String, Object> querySnap = new LinkedHashMap<>(p);
		long companyId = toLong(p.get("company_id"));
		String operatorType = str(jwt.get("operator_type"));
		long operatorId = toLong(jwt.get("operator_id"));
		long merchantId = toLong(jwt.get("merchant_id"));

		String operateSource = str(request.getParameter("operate_source"));
		if (!StringUtils.hasText(operateSource)) {
			operateSource = "platform";
		}
		boolean platformPath = "platform".equalsIgnoreCase(operateSource.trim());

		int warningStore;
		if (platformPath) {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		} else if ("supplier".equalsIgnoreCase(operatorType)) {
			warningStore = itemStoreService.getSupplierWarningStore(companyId, operatorId);
		} else {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		}

		normalizeStoreAndPrice(p);
		final boolean skuMode = truthy(p.get("is_sku"));

		if (platformPath) {
			applyItemHolderToItemSource(request, p);
			String itemSource = str(p.get("item_source"));
			if (StringUtils.hasText(itemSource)) {
				if ("supplier".equalsIgnoreCase(itemSource.trim())) {
					p.put("item_source_supplier_mode", Boolean.TRUE);
				} else {
					p.put("item_source_non_supplier_mode", Boolean.TRUE);
				}
			}
		}

		if ("supplier".equalsIgnoreCase(operatorType)) {
			p.put("supplier_id_eq", (int) operatorId);
			p.remove("supplier_id_in");
		} else if (p.containsKey("supplier_id") && StringUtils.hasText(str(p.get("supplier_id")))) {
			p.put("supplier_id_eq", Integer.parseInt(str(p.get("supplier_id"))));
		}

		if ("merchant".equalsIgnoreCase(operatorType)) {
			List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
			if (v.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			p.put("distributor_id_in", v.stream().map(Long::intValue).collect(Collectors.toList()));
		}

		applyDistributorQueryBranch(request, p, operatorType, companyId, merchantId, operateSource);

		if ("goodsAudit".equalsIgnoreCase(str(p.get("from_page"))) && !hasResolvedDistributorId(request)) {
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
		}

		long mainItemScopeDistributorId = SupplierLinkedMainItemIdResolver.resolveScopeDistributorId(jwt, p);

		if (p.containsKey("approve_status") && StringUtils.hasText(str(p.get("approve_status")))) {
			// kept in p for repository
		}
		if (p.containsKey("audit_status") && StringUtils.hasText(str(p.get("audit_status")))) {
			// kept
		}

		Integer rebateVal = parseRebateQuery(p.get("rebate"));
		if (rebateVal != null) {
			String goodsMode = companyPopularizeConfigReadService.getGoodsMode(companyId);
			if ("all".equals(goodsMode)) {
				if (rebateVal == 0) {
					return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
				}
				// Full-catalog popularize mode: rebate query does not narrow by rebate column.
				p.remove("rebate");
			} else {
				p.put("rebate", rebateVal);
			}
		} else {
			p.remove("rebate");
		}

		if (StringUtils.hasText(str(p.get("supplier_name")))) {
			List<Long> sids = supplierOperatorQueryRepository.listOperatorIdsBySupplierNameLike(companyId, str(p.get("supplier_name")));
			if (sids.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			p.put("supplier_id_in", sids.stream().map(Long::intValue).collect(Collectors.toList()));
			p.remove("supplier_id_eq");
		}

		List<Long> mergedItemIds = null;

		if (StringUtils.hasText(str(p.get("keywords")))) {
			List<Long> ids = platformPath
					? itemsListQueryRepository.mergeDefaultItemIdsByItemNameOrBrief(companyId, str(p.get("keywords")))
					: supplierItemsListQueryRepository.mergeDefaultItemIdsByItemNameOrBrief(companyId, str(p.get("keywords")));
			if (ids.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			mergedItemIds = new ArrayList<>(ids);
		}

		if (p.containsKey("item_id") && StringUtils.hasText(str(p.get("item_id")))) {
			List<Long> reqIds = parseIdList(str(p.get("item_id")));
			if (!reqIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(reqIds);
			}
			if (!hasResolvedDistributorId(request)) {
				p.remove("distributor_id_in");
				p.remove("distributor_id_eq");
			}
			p.remove("item_id");
		}

		@SuppressWarnings("unchecked")
		List<Long> mainCats = (List<Long>) p.remove("main_cat_id_list");
		if (mainCats != null && !mainCats.isEmpty()) {
			List<String> cats = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, mainCats);
			if (!cats.isEmpty()) {
				p.put("item_category_in", cats);
			}
		}

		@SuppressWarnings("unchecked")
		List<Long> catList = (List<Long>) p.remove("category_list");
		if (catList != null && !catList.isEmpty()) {
			List<Long> union = new ArrayList<>();
			for (Long c : catList) {
				union.addAll(itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, c));
			}
			union = union.stream().distinct().collect(Collectors.toList());
			if (union.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			if (mergedItemIds == null || mergedItemIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(union);
			} else {
				mergedItemIds = mergedItemIds.stream().filter(union::contains).collect(Collectors.toList());
				if (mergedItemIds.isEmpty()) {
					return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
				}
			}
		}

		if (mergedItemIds != null && !mergedItemIds.isEmpty()) {
			p.put("item_id_or_default_ids", mergedItemIds);
		}

		if (truthy(p.get("is_warning"))) {
			p.put("store_lte_warning", warningStore);
		}

		@SuppressWarnings("unchecked")
		List<Long> tagList = (List<Long>) p.remove("tag_id_list");
		if (tagList != null && !tagList.isEmpty()) {
			List<Long> itemIds = itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIds(companyId, tagList);
			if (itemIds.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			@SuppressWarnings("unchecked")
			List<Long> existing = (List<Long>) p.get("item_id_or_default_ids");
			if (existing == null || existing.isEmpty()) {
				p.put("item_id_or_default_ids", itemIds);
			} else {
				List<Long> inter = existing.stream().filter(itemIds::contains).collect(Collectors.toList());
				if (inter.isEmpty()) {
					return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
				}
				p.put("item_id_or_default_ids", inter);
			}
		}

		if (StringUtils.hasText(str(p.get("item_bn")))) {
			List<Long> ids = platformPath
					? itemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(companyId, str(p.get("item_bn")))
					: supplierItemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(companyId, str(p.get("item_bn")));
			if (ids.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			intersectItemIdOrDefault(p, ids);
			if (isEmptyIdOrDefault(p)) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			p.remove("item_bn");
		}
		if (StringUtils.hasText(str(p.get("barcode")))) {
			List<Long> ids = platformPath ? itemsListQueryRepository.listDefaultItemIdsByBarcodeExact(companyId, str(p.get("barcode")))
					: supplierItemsListQueryRepository.listDefaultItemIdsByBarcodeExact(companyId, str(p.get("barcode")));
			if (ids.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			intersectItemIdOrDefault(p, ids);
			if (isEmptyIdOrDefault(p)) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
		}

		if ("merchant".equalsIgnoreCase(operatorType) && !hasResolvedDistributorId(request)) {
			List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
			if (!v.isEmpty()) {
				p.put("distributor_id_in", v.stream().map(Long::intValue).collect(Collectors.toList()));
			}
		}

		if (truthy(p.get("pathSource"))) {
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
			p.put("distributor_id_gt0_only", Boolean.TRUE);
		}

		int page = pb.getPage();
		int pageSize = pb.getPageSize();

		if (skuMode) {
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
			p.remove("is_default_true");
			Map<String, Object> skuParams = new LinkedHashMap<>(p);
			@SuppressWarnings("unchecked")
			List<Long> idOrDef = (List<Long>) skuParams.get("item_id_or_default_ids");
			boolean forceFull = idOrDef != null && !idOrDef.isEmpty();
			int off;
			int qLimit;
			if (forceFull) {
				off = 0;
				qLimit = -1;
			} else {
				if (pageSize <= 0) {
					pageSize = 10;
				}
				off = (page - 1) * pageSize;
				qLimit = pageSize;
			}
			long total;
			List<Map<String, Object>> rows;
			List<Map<String, Object>> supplierMeta = null;
			List<SupplierItems> supplierSrows = null;
			if (platformPath) {
				total = itemsListQueryRepository.countByParams(skuParams);
				List<Items> items = itemsListQueryRepository.selectPageByParamsForSku(skuParams, off, qLimit);
				rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
			} else {
				total = supplierItemsListQueryRepository.countByParams(skuParams);
				supplierSrows = supplierItemsListQueryRepository.selectPageByParams(skuParams, off, qLimit);
				if (supplierSrows.isEmpty()) {
					throw new ResourceException("查询条件不完整");
				}
				SupplierListAssembly assembly = assembleSupplierListRows(companyId, supplierSrows, mainItemScopeDistributorId);
				rows = assembly.rows();
				supplierMeta = assembly.supplierMeta();
			}
			applyReplaceSkuSpecForPlatformItemRows(companyId, rows);
			// is_sku=true：不汇总多规格库存（PHP dealListStore 第二参数为 true 时直接返回）
			if (platformPath) {
				itemsSkuListAssembler.applyTypeLabels(rows);
			}
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, !platformPath, supplierMeta);
			if (!platformPath) {
				SupplierAdminItemsListWireMapper.apply(rows, supplierSrows, supplierWireNullableColumns(supplierSrows));
			}
			itemsMedicineService.applyMedicineDataToRows(companyId, rows);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("list", rows);
			out.put("total_count", total);
			out.put("warning_store", warningStore);
			out.put("filter", GoodsItemsListResponseFilterBuilder.buildForPath(!platformPath, querySnap, p, skuMode));
			return out;
		}

		if (pageSize <= 0) {
			pageSize = 10;
		}

		p.put("is_default_true", Boolean.TRUE);

		List<Map<String, Object>> rows;
		long total;
		List<Map<String, Object>> supplierMeta = null;
		List<SupplierItems> supplierSrows = null;
		if (platformPath) {
			total = itemsListQueryRepository.countByParams(p);
			int off = (page - 1) * pageSize;
			List<Items> items = itemsListQueryRepository.selectPageByParams(p, off, pageSize);
			rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
			dealListStoreForRows(companyId, rows, false);
			itemsSkuListAssembler.applyTypeLabels(rows);
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, false, null);
		} else {
			total = supplierItemsListQueryRepository.countByParams(p);
			int off = (page - 1) * pageSize;
			supplierSrows = supplierItemsListQueryRepository.selectPageByParams(p, off, pageSize);
			SupplierListAssembly assembly = assembleSupplierListRows(companyId, supplierSrows, mainItemScopeDistributorId);
			rows = assembly.rows();
			supplierMeta = assembly.supplierMeta();
			dealListStoreForRows(companyId, rows, true);
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, true, supplierMeta);
			SupplierAdminItemsListWireMapper.apply(rows, supplierSrows, supplierWireNullableColumns(supplierSrows));
		}

		itemsMedicineService.applyMedicineDataToRows(companyId, rows);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rows);
		out.put("total_count", total);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.buildForPath(!platformPath, querySnap, p, skuMode));
		return out;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> listForSeckillActivitySearch(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> jwt = (Map<String, Object>) jwtRaw;
		GoodsItemsListParamBuilder pb = new GoodsItemsListParamBuilder(request, jwt);
		LinkedHashMap<String, Object> p = pb.toParamsMap();
		LinkedHashMap<String, Object> querySnap = new LinkedHashMap<>(p);
		String isSkuParam = request.getParameter("is_sku");
		boolean skuMode = isSkuParam != null && "true".equalsIgnoreCase(isSkuParam.trim());
		p.put("is_sku", skuMode ? "true" : "false");
		long companyId = toLong(p.get("company_id"));
		String operatorType = str(jwt.get("operator_type"));
		long operatorId = toLong(jwt.get("operator_id"));
		long merchantId = toLong(jwt.get("merchant_id"));

		String operateSource = str(request.getParameter("operate_source"));
		if (!StringUtils.hasText(operateSource)) {
			operateSource = "platform";
		}
		boolean platformPath = "platform".equalsIgnoreCase(operateSource.trim());

		int warningStore;
		if (platformPath) {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		} else if ("supplier".equalsIgnoreCase(operatorType)) {
			warningStore = itemStoreService.getSupplierWarningStore(companyId, operatorId);
		} else {
			warningStore = itemStoreService.getPlatformWarningStore(companyId);
		}

		normalizeStoreAndPrice(p);

		if (platformPath) {
			String itemSource = str(p.get("item_source"));
			if (StringUtils.hasText(itemSource)) {
				if ("supplier".equalsIgnoreCase(itemSource.trim())) {
					p.put("item_source_supplier_mode", Boolean.TRUE);
				} else {
					p.put("item_source_non_supplier_mode", Boolean.TRUE);
				}
			}
		}

		if ("supplier".equalsIgnoreCase(operatorType)) {
			p.put("supplier_id_eq", (int) operatorId);
			p.remove("supplier_id_in");
		} else if (p.containsKey("supplier_id") && StringUtils.hasText(str(p.get("supplier_id")))) {
			p.put("supplier_id_eq", Integer.parseInt(str(p.get("supplier_id"))));
		}

		if ("merchant".equalsIgnoreCase(operatorType)) {
			List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
			if (v.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			p.put("distributor_id_in", v.stream().map(Long::intValue).collect(Collectors.toList()));
		}

		applyDistributorQueryBranch(request, p, operatorType, companyId, merchantId, operateSource);

		if ("goodsAudit".equalsIgnoreCase(str(p.get("from_page"))) && !hasResolvedDistributorId(request)) {
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
		}

		long mainItemScopeDistributorId = SupplierLinkedMainItemIdResolver.resolveScopeDistributorId(jwt, p);

		if (p.containsKey("approve_status") && StringUtils.hasText(str(p.get("approve_status")))) {
			// kept in p for repository
		}
		if (p.containsKey("audit_status") && StringUtils.hasText(str(p.get("audit_status")))) {
			// kept
		}

		Integer rebateVal = parseRebateQuery(p.get("rebate"));
		if (rebateVal != null) {
			String goodsMode = companyPopularizeConfigReadService.getGoodsMode(companyId);
			if ("all".equals(goodsMode)) {
				if (rebateVal == 0) {
					return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
				}
				// Full-catalog popularize mode: rebate query does not narrow by rebate column.
				p.remove("rebate");
			} else {
				p.put("rebate", rebateVal);
			}
		} else {
			p.remove("rebate");
		}

		if (StringUtils.hasText(str(p.get("supplier_name")))) {
			List<Long> sids = supplierOperatorQueryRepository.listOperatorIdsBySupplierNameLike(companyId, str(p.get("supplier_name")));
			if (sids.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			p.put("supplier_id_in", sids.stream().map(Long::intValue).collect(Collectors.toList()));
			p.remove("supplier_id_eq");
		}

		List<Long> mergedItemIds = null;

		if (StringUtils.hasText(str(p.get("keywords")))) {
			List<Long> ids = platformPath
					? itemsListQueryRepository.mergeDefaultItemIdsByItemNameOrBrief(companyId, str(p.get("keywords")))
					: supplierItemsListQueryRepository.mergeDefaultItemIdsByItemNameOrBrief(companyId, str(p.get("keywords")));
			if (ids.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			mergedItemIds = new ArrayList<>(ids);
		}

		if (p.containsKey("item_id") && StringUtils.hasText(str(p.get("item_id")))) {
			List<Long> reqIds = parseIdList(str(p.get("item_id")));
			if (!reqIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(reqIds);
			}
			if (!hasResolvedDistributorId(request)) {
				p.remove("distributor_id_in");
				p.remove("distributor_id_eq");
			}
			p.remove("item_id");
		}

		@SuppressWarnings("unchecked")
		List<Long> mainCats = (List<Long>) p.remove("main_cat_id_list");
		if (mainCats != null && !mainCats.isEmpty()) {
			List<String> cats = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, mainCats);
			if (!cats.isEmpty()) {
				p.put("item_category_in", cats);
			}
		}

		@SuppressWarnings("unchecked")
		List<Long> catList = (List<Long>) p.remove("category_list");
		if (catList != null && !catList.isEmpty()) {
			List<Long> union = new ArrayList<>();
			for (Long c : catList) {
				union.addAll(itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, c));
			}
			union = union.stream().distinct().collect(Collectors.toList());
			if (union.isEmpty()) {
				Map<String, Object> outFlat = new LinkedHashMap<>();
				outFlat.put("list", List.of());
				outFlat.put("total_count", 0);
				outFlat.put("warning_store", warningStore);
				outFlat.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
				outFlat.put("_seckillFlatEarly", Boolean.TRUE);
				return outFlat;
			}
			if (mergedItemIds == null || mergedItemIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(union);
			} else {
				mergedItemIds = mergedItemIds.stream().filter(union::contains).collect(Collectors.toList());
				if (mergedItemIds.isEmpty()) {
					Map<String, Object> outFlat = new LinkedHashMap<>();
					outFlat.put("list", List.of());
					outFlat.put("total_count", 0);
					outFlat.put("warning_store", warningStore);
					outFlat.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
					outFlat.put("_seckillFlatEarly", Boolean.TRUE);
					return outFlat;
				}
			}
		}

		if (mergedItemIds != null && !mergedItemIds.isEmpty()) {
			p.put("item_id_or_default_ids", mergedItemIds);
		}

		if (truthy(p.get("is_warning"))) {
			p.put("store_lte_warning", warningStore);
		}

		@SuppressWarnings("unchecked")
		List<Long> tagList = (List<Long>) p.remove("tag_id_list");
		if (tagList != null && !tagList.isEmpty()) {
			List<Long> itemIds = itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIds(companyId, tagList);
			if (itemIds.isEmpty()) {
				Map<String, Object> outFlat = new LinkedHashMap<>();
				outFlat.put("list", List.of());
				outFlat.put("total_count", 0);
				outFlat.put("warning_store", warningStore);
				outFlat.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
				outFlat.put("_seckillFlatEarly", Boolean.TRUE);
				return outFlat;
			}
			@SuppressWarnings("unchecked")
			List<Long> existing = (List<Long>) p.get("item_id_or_default_ids");
			if (existing == null || existing.isEmpty()) {
				p.put("item_id_or_default_ids", itemIds);
			} else {
				List<Long> inter = existing.stream().filter(itemIds::contains).collect(Collectors.toList());
				if (inter.isEmpty()) {
					Map<String, Object> outFlat = new LinkedHashMap<>();
					outFlat.put("list", List.of());
					outFlat.put("total_count", 0);
					outFlat.put("warning_store", warningStore);
					outFlat.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
					outFlat.put("_seckillFlatEarly", Boolean.TRUE);
					return outFlat;
				}
				p.put("item_id_or_default_ids", inter);
			}
		}

		if (StringUtils.hasText(str(p.get("item_bn")))) {
			List<Long> ids = platformPath
					? itemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(companyId, str(p.get("item_bn")))
					: supplierItemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(companyId, str(p.get("item_bn")));
			if (ids.isEmpty()) {
				Map<String, Object> outFlat = new LinkedHashMap<>();
				outFlat.put("list", List.of());
				outFlat.put("total_count", 0);
				outFlat.put("warning_store", warningStore);
				outFlat.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
				outFlat.put("_seckillFlatEarly", Boolean.TRUE);
				return outFlat;
			}
			intersectItemIdOrDefault(p, ids);
			if (isEmptyIdOrDefault(p)) {
				Map<String, Object> outFlat = new LinkedHashMap<>();
				outFlat.put("list", List.of());
				outFlat.put("total_count", 0);
				outFlat.put("warning_store", warningStore);
				outFlat.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
				outFlat.put("_seckillFlatEarly", Boolean.TRUE);
				return outFlat;
			}
			p.remove("item_bn");
		}
		if (StringUtils.hasText(str(p.get("barcode")))) {
			List<Long> ids = platformPath ? itemsListQueryRepository.listDefaultItemIdsByBarcodeExact(companyId, str(p.get("barcode")))
					: supplierItemsListQueryRepository.listDefaultItemIdsByBarcodeExact(companyId, str(p.get("barcode")));
			if (ids.isEmpty()) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
			intersectItemIdOrDefault(p, ids);
			if (isEmptyIdOrDefault(p)) {
				return emptyResponse(querySnap, p, warningStore, skuMode, platformPath);
			}
		}

		if ("merchant".equalsIgnoreCase(operatorType) && !hasResolvedDistributorId(request)) {
			List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
			if (!v.isEmpty()) {
				p.put("distributor_id_in", v.stream().map(Long::intValue).collect(Collectors.toList()));
			}
		}

		if (truthy(p.get("pathSource"))) {
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
			p.put("distributor_id_gt0_only", Boolean.TRUE);
		}

		int page = pb.getPage();
		int pageSize = pb.getPageSize();

		if (skuMode) {
			p.remove("distributor_id_in");
			p.remove("distributor_id_eq");
			p.remove("is_default_true");
			Map<String, Object> skuParams = new LinkedHashMap<>(p);
			@SuppressWarnings("unchecked")
			List<Long> idOrDefSeckill = (List<Long>) skuParams.get("item_id_or_default_ids");
			boolean forceFull = idOrDefSeckill != null && !idOrDefSeckill.isEmpty();
			int off;
			int qLimit;
			if (forceFull) {
				off = 0;
				qLimit = -1;
			} else {
				if (pageSize < 1) {
					throw new BadRequestException(SKU_LIST_ERR_MSG);
				}
				off = (page - 1) * pageSize;
				qLimit = pageSize;
			}
			long total;
			List<Map<String, Object>> rows;
			List<Map<String, Object>> supplierMeta = null;
			if (platformPath) {
				total = itemsListQueryRepository.countByParams(skuParams);
				List<Items> items = itemsListQueryRepository.selectPageByParamsForSku(skuParams, off, qLimit);
				rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
			} else {
				total = supplierItemsListQueryRepository.countByParams(skuParams);
				List<SupplierItems> srows =
						supplierItemsListQueryRepository.selectPageByParams(skuParams, off, qLimit);
				SupplierListAssembly assembly = assembleSupplierListRows(companyId, srows, mainItemScopeDistributorId);
				rows = assembly.rows();
				supplierMeta = assembly.supplierMeta();
			}
			applyReplaceSkuSpecForPlatformItemRows(companyId, rows);
			if (forceFull && pageSize > 0) {
				int sliceFrom = Math.max(0, (page - 1) * pageSize);
				int sliceTo = Math.min(rows.size(), sliceFrom + pageSize);
				if (sliceFrom >= rows.size()) {
					rows = new ArrayList<>();
				} else {
					rows = new ArrayList<>(rows.subList(sliceFrom, sliceTo));
				}
			}
			// is_sku=true：不汇总多规格库存
			itemsSkuListAssembler.applyTypeLabels(rows);
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, !platformPath, supplierMeta);
			itemsMedicineService.applyMedicineDataToRows(companyId, rows);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("list", rows);
			out.put("total_count", total);
			out.put("warning_store", warningStore);
			out.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
			return out;
		}

		if (pageSize <= 0) {
			pageSize = 10;
		}

		p.put("is_default_true", Boolean.TRUE);

		List<Map<String, Object>> rows;
		long total;
		List<Map<String, Object>> supplierMeta = null;
		if (platformPath) {
			total = itemsListQueryRepository.countByParams(p);
			int off = (page - 1) * pageSize;
			List<Items> items = itemsListQueryRepository.selectPageByParams(p, off, pageSize);
			rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
			dealListStoreForRows(companyId, rows, false);
			itemsSkuListAssembler.applyTypeLabels(rows);
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, false, null);
		} else {
			total = supplierItemsListQueryRepository.countByParams(p);
			int off = (page - 1) * pageSize;
			List<SupplierItems> srows = supplierItemsListQueryRepository.selectPageByParams(p, off, pageSize);
			SupplierListAssembly assembly = assembleSupplierListRows(companyId, srows, mainItemScopeDistributorId);
			rows = assembly.rows();
			supplierMeta = assembly.supplierMeta();
			dealListStoreForRows(companyId, rows, true);
			itemsSkuListAssembler.applyTypeLabels(rows);
			itemsListMultiLangApplier.applyToRows(companyId, resolveListLang(request), rows);
			goodsItemsListEnrichmentService.enrichAll(companyId, rows, true, supplierMeta);
		}

		itemsMedicineService.applyMedicineDataToRows(companyId, rows);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rows);
		out.put("total_count", total);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.build(querySnap, p, skuMode));
		return out;
	}

	/**
	 * 平台主表 {@code items} 按默认商品 ID 列表全量查询（无分页上限语义），加工链与 {@link #list} 中 platform + 非 SKU + {@code is_default_true} 分支一致。
	 */
	public Map<String, Object> listPlatformUnpagedByDefaultItemIdsForRecommendLike(
			long companyId, List<Long> defaultItemIds, String acceptLanguageHeader) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>();
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, Long.valueOf(companyId));
		p.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(defaultItemIds));
		p.put("is_default_true", Boolean.TRUE);

		long total = itemsListQueryRepository.countByParams(p);
		if (total == 0L) {
			return Map.of("total_count", 0L, "list", List.of());
		}
		int limit = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
		List<Items> items = itemsListQueryRepository.selectPageByParams(p, 0, limit);
		List<Map<String, Object>> rows =
				items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		itemsListQueryRepository.dealListStore(companyId, rows);
		itemsSkuListAssembler.applyTypeLabels(rows);
		itemsListMultiLangApplier.applyToRows(companyId, acceptLanguageHeader, rows);
		goodsItemsListEnrichmentService.enrichAll(companyId, rows, false, null);
		itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	private static void applyItemHolderToItemSource(HttpServletRequest request, LinkedHashMap<String, Object> p) {
		String itemHolder = str(request.getParameter("item_holder"));
		if (!StringUtils.hasText(itemHolder)) {
			return;
		}
		String h = itemHolder.trim();
		if ("supplier".equalsIgnoreCase(h)) {
			p.put("item_source", "supplier");
		} else if ("self".equalsIgnoreCase(h) || "platform".equalsIgnoreCase(h)) {
			p.put("item_source", "platform");
		}
	}

	private static boolean isEmptyIdOrDefault(LinkedHashMap<String, Object> p) {
		@SuppressWarnings("unchecked")
		List<Long> idf = (List<Long>) p.get("item_id_or_default_ids");
		return idf != null && idf.isEmpty();
	}

	private static void intersectItemIdOrDefault(LinkedHashMap<String, Object> p, List<Long> ids) {
		@SuppressWarnings("unchecked")
		List<Long> ex = (List<Long>) p.get("item_id_or_default_ids");
		if (ex == null || ex.isEmpty()) {
			p.put("item_id_or_default_ids", ids);
			return;
		}
		List<Long> inter = ex.stream().filter(ids::contains).collect(Collectors.toList());
		p.put("item_id_or_default_ids", inter);
	}

	private void applyDistributorQueryBranch(HttpServletRequest request, LinkedHashMap<String, Object> p, String operatorType, long companyId,
			long merchantId, String operateSource) {
		boolean supplierOperateSource = StringUtils.hasText(operateSource) && "supplier".equalsIgnoreCase(operateSource.trim());
		String didRaw = resolveDistributorIdRaw(request);
		if ("all_distributor".equalsIgnoreCase(str(didRaw))) {
			List<Long> all = distributorListQueryService.listAllDistributorIdsForCompany(companyId);
			if ("merchant".equalsIgnoreCase(operatorType)) {
				List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
				List<Long> inter = distributorListQueryService.intersectSorted(v, all);
				if (inter.isEmpty()) {
					p.put("distributor_id_in", List.of(-1));
				} else {
					p.put("distributor_id_in", inter.stream().map(Long::intValue).collect(Collectors.toList()));
				}
			} else {
				p.put("distributor_id_in", all.stream().map(Long::intValue).collect(Collectors.toList()));
			}
			return;
		}
		if (!supplierOperateSource && didRaw != null && StringUtils.hasText(didRaw.trim())) {
			List<Long> q = parseIdList(didRaw);
			if ("merchant".equalsIgnoreCase(operatorType)) {
				List<Long> v = distributorListQueryService.listValidDistributorIdsForMerchant(companyId, merchantId);
				List<Long> inter = distributorListQueryService.intersectSorted(v, q);
				if (inter.isEmpty()) {
					p.put("distributor_id_in", List.of(-1));
				} else {
					p.put("distributor_id_in", inter.stream().map(Long::intValue).collect(Collectors.toList()));
				}
			} else {
				p.put("distributor_id_in", q.stream().map(Long::intValue).collect(Collectors.toList()));
			}
			return;
		}
		if ("admin".equalsIgnoreCase(operatorType) || "staff".equalsIgnoreCase(operatorType)) {
			p.put("distributor_id_eq", 0);
		}
	}

	/** Query param first; else activated / JWT selected store. */
	private static String resolveDistributorIdRaw(HttpServletRequest request) {
		String didRaw = request.getParameter("distributor_id");
		if (didRaw != null && StringUtils.hasText(didRaw.trim())) {
			return didRaw.trim();
		}
		Object attr = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
		if (attr != null) {
			long v = toLong(attr);
			if (v > 0L) {
				return String.valueOf(v);
			}
		}
		Object jwtRaw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (jwtRaw instanceof Map<?, ?> jwtMap) {
			Object jwtDid = jwtMap.get("distributor_id");
			if (jwtDid != null) {
				long v = toLong(jwtDid);
				if (v > 0L) {
					return String.valueOf(v);
				}
			}
		}
		return null;
	}

	private static boolean hasResolvedDistributorId(HttpServletRequest request) {
		return resolveDistributorIdRaw(request) != null;
	}

	private static List<Long> parseIdList(String raw) {
		String[] parts = raw.split(",");
		List<Long> out = new ArrayList<>();
		for (String s : parts) {
			if (StringUtils.hasText(s)) {
				out.add(Long.parseLong(s.trim()));
			}
		}
		return out;
	}

	private static void normalizeStoreAndPrice(Map<String, Object> p) {
		Object ss = p.get("store_status");
		if (ss != null && StringUtils.hasText(str(ss))) {
			if (truthy(ss)) {
				p.put("store_status", "positive");
			} else if ("0".equals(str(ss).trim())) {
				p.put("store_status", "zero");
			}
		}
		if (p.containsKey("store_gt")) {
			p.put("store_gt", Integer.parseInt(str(p.get("store_gt"))));
		}
		if (p.containsKey("store_lt")) {
			p.put("store_lt", Integer.parseInt(str(p.get("store_lt"))));
		}
		if (p.containsKey("price_gt")) {
			p.put("price_gt_cents", GoodsItemsListParamBuilder.priceToCents(p.remove("price_gt")));
		}
		if (p.containsKey("price_lt")) {
			p.put("price_lt_cents", GoodsItemsListParamBuilder.priceToCents(p.remove("price_lt")));
		}
		if (p.containsKey("distributor_approve_status") && StringUtils.hasText(str(p.get("distributor_approve_status")))) {
			p.put("distributor_approve_status", str(p.get("distributor_approve_status")).trim());
		}
	}

	private static Integer parseRebateQuery(Object o) {
		if (o == null || !StringUtils.hasText(str(o))) {
			return null;
		}
		int v = Integer.parseInt(str(o).trim());
		if (v >= 0 && v <= 3) {
			return v;
		}
		return null;
	}

	private record SupplierListAssembly(List<Map<String, Object>> rows, List<Map<String, Object>> supplierMeta) {
	}

	private Map<Long, Map<String, Object>> supplierWireNullableColumns(List<SupplierItems> srows) {
		if (srows == null || srows.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = srows.stream().map(SupplierItems::getItemId).filter(Objects::nonNull).collect(Collectors.toList());
		return supplierItemsListQueryRepository.mapWireNullableColumnsByItemIds(ids);
	}

	private SupplierListAssembly assembleSupplierListRows(long companyId, List<SupplierItems> srows, long scopeDistributorId) {
		if (srows == null || srows.isEmpty()) {
			return new SupplierListAssembly(new ArrayList<>(), new ArrayList<>());
		}
		List<Long> supItemIds = srows.stream().map(SupplierItems::getItemId).filter(Objects::nonNull).collect(Collectors.toList());
		List<Long> categoryAttrItemIds = srows.stream().map(GoodsItemsListFacadeService::supplierCategoryAttrItemId).filter(id -> id > 0).distinct()
				.collect(Collectors.toList());

		Map<Integer, Long> mainItemIdBySupItemId =
				SupplierLinkedMainItemIdResolver.resolve(itemsRepository, companyId, supItemIds, scopeDistributorId);

		Map<Long, String> catJsonByDefaultItemId = new LinkedHashMap<>();
		for (SupplierItemsAttr a : supplierItemsAttrListRepository.listCategoryAttrsByCompanyAndItemIds(companyId, categoryAttrItemIds)) {
			if (a.getItemId() != null && a.getAttrData() != null) {
				catJsonByDefaultItemId.put(a.getItemId(), a.getAttrData());
			}
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		List<Map<String, Object>> supplierMeta = new ArrayList<>();
		for (SupplierItems sr : srows) {
			if (sr.getItemId() == null) {
				continue;
			}
			long mainItemId = mainItemIdBySupItemId.getOrDefault(sr.getItemId().intValue(), 0L);
			Map<String, Object> row = GoodsItemsListRowMapper.toRowFromSupplier(sr, mainItemId);
			long catKey = supplierCategoryAttrItemId(sr);
			String categoryJson = catJsonByDefaultItemId.get(catKey);
			if (categoryJson != null) {
				row.put("item_cat_id", GoodsItemsListRowMapper.parseSupplierCategoryIds(categoryJson));
			}
			rows.add(row);
			Map<String, Object> meta = new LinkedHashMap<>();
			meta.put("item_id", sr.getItemId());
			if (categoryJson != null) {
				meta.put("category_json", categoryJson);
			}
			supplierMeta.add(meta);
		}
		return new SupplierListAssembly(rows, supplierMeta);
	}

	private static long supplierCategoryAttrItemId(SupplierItems sr) {
		if (sr.getDefaultItemId() != null && sr.getDefaultItemId() > 0) {
			return sr.getDefaultItemId();
		}
		return sr.getItemId() != null ? sr.getItemId() : 0L;
	}

	private static Map<String, Object> emptyResponse(LinkedHashMap<String, Object> querySnap, LinkedHashMap<String, Object> p, int warningStore,
			boolean skuMode, boolean platformPath) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("total_count", 0);
		out.put("warning_store", warningStore);
		out.put("filter", GoodsItemsListResponseFilterBuilder.buildForPath(!platformPath, querySnap, p, skuMode));
		return out;
	}

	private String resolveListLang(HttpServletRequest request) {
		if (request != null) {
			String q = request.getParameter("country_code");
			if (StringUtils.hasText(q)) {
				return langueProperties.resolveToSupportedTag(q.trim());
			}
		}
		return RequestLangTag.current(langueProperties);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static boolean truthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		String s = o.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
	}
}
