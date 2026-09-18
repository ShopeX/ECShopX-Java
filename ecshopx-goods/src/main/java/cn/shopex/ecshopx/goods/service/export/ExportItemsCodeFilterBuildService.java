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

package cn.shopex.ecshopx.goods.service.export;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.ItemsTagsQueryService;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeFilterBuildResult;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListParamBuilder;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsListQueryRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierOperatorQueryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportItemsCodeFilterBuildService {

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final SupplierItemsListQueryRepository supplierItemsListQueryRepository;
	private final SupplierOperatorQueryRepository supplierOperatorQueryRepository;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemsTagsQueryService itemsTagsQueryService;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;

	public ExportItemsCodeFilterBuildService(ItemsListQueryRepository itemsListQueryRepository,
			SupplierItemsListQueryRepository supplierItemsListQueryRepository,
			SupplierOperatorQueryRepository supplierOperatorQueryRepository,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			ItemsTagsQueryService itemsTagsQueryService,
			DistributorListQueryService distributorListQueryService,
			DistributorItemsRepository distributorItemsRepository,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.supplierItemsListQueryRepository = supplierItemsListQueryRepository;
		this.supplierOperatorQueryRepository = supplierOperatorQueryRepository;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemsTagsQueryService = itemsTagsQueryService;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorItemsRepository = distributorItemsRepository;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
	}

	public ExportItemsCodeFilterBuildResult build(long companyId, long operatorId, String operatorType, Long merchantId,
			Map<String, Object> inputData) {
		validateRebateIfPresent(inputData);
		String opType = operatorType != null ? operatorType : "";
		boolean platformPath = !"supplier".equalsIgnoreCase(opType);

		LinkedHashMap<String, Object> p = new LinkedHashMap<>();
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		p.put("operator_type", opType);

		Object itemTypeObj = inputData.get("item_type");
		p.put("item_type", itemTypeObj != null && StringUtils.hasText(str(itemTypeObj)) ? str(itemTypeObj).trim() : "services");

		if (platformPath) {
			String itemSource = resolveItemSource(inputData);
			if (StringUtils.hasText(itemSource)) {
				if ("supplier".equalsIgnoreCase(itemSource)) {
					p.put(ItemsListQueryRepository.KEY_ITEM_SOURCE_SUPPLIER_MODE, Boolean.TRUE);
				} else {
					p.put(ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE, Boolean.TRUE);
				}
				p.put("item_source", itemSource);
			}
		}

		if ("supplier".equalsIgnoreCase(opType)) {
			p.put(ItemsListQueryRepository.KEY_SUPPLIER_ID_EQ, (int) operatorId);
			p.remove(ItemsListQueryRepository.KEY_SUPPLIER_ID_IN);
		}

		putTrimmedStringIfPresent(p, inputData, "brand_id");
		putTrimmedStringIfPresent(p, inputData, "consume_type");
		putTrimmedStringIfPresent(p, inputData, "templates_id");
		putTrimmedStringIfPresent(p, inputData, "rebate_type");
		if (inputData.containsKey("nospec")) {
			p.put("nospec", inputData.get("nospec"));
		}

		String kw = str(inputData.get("keywords"));
		String iname = str(inputData.get("item_name"));
		if (StringUtils.hasText(iname)) {
			p.put("item_name", iname.trim());
		} else if (StringUtils.hasText(kw)) {
			p.put("item_name", kw.trim());
		}

		Object regions = inputData.get("regions_id");
		if (regions != null) {
			String joined = joinRegions(regions);
			if (StringUtils.hasText(joined)) {
				p.put("regions_id", joined);
			}
		}

		String supplierName = str(inputData.get("supplier_name"));
		if (StringUtils.hasText(supplierName)) {
			List<Long> sids = supplierOperatorQueryRepository.listOperatorIdsBySupplierNameLike(companyId, supplierName);
			if (sids.isEmpty()) {
				throw new ResourceException("没有符合条件的供应商商品");
			}
			p.put(ItemsListQueryRepository.KEY_SUPPLIER_ID_IN, sids.stream().map(Long::intValue).collect(Collectors.toList()));
			p.remove(ItemsListQueryRepository.KEY_SUPPLIER_ID_EQ);
		}

		Object mainCat = inputData.get("main_cat_id");
		if (mainCat != null) {
			Long single = normalizeMainCatId(mainCat);
			if (single != null) {
				List<Long> mainCats = List.of(single);
				List<String> cats = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, mainCats);
				if (!cats.isEmpty()) {
					p.put(ItemsListQueryRepository.KEY_ITEM_CATEGORY_IN, cats);
				}
			}
		}

		Object approve = inputData.get("approve_status");
		if (approve != null && StringUtils.hasText(str(approve))) {
			String a = str(approve).trim();
			if ("processing".equals(a) || "rejected".equals(a)) {
				p.put("audit_status", a);
			} else {
				p.put("approve_status", a);
			}
		}

		Object auditStatus = inputData.get("audit_status");
		if (auditStatus != null && isPhpTruthy(str(auditStatus))) {
			String a = str(auditStatus).trim();
			if (!"rebate".equalsIgnoreCase(a)) {
				p.put("audit_status", a);
			}
		}

		Object rebate = inputData.get("rebate");
		if (rebate != null && StringUtils.hasText(str(rebate))) {
			p.put("rebate", Integer.parseInt(str(rebate).trim()));
		}

		String st = str(inputData.get("special_type"));
		if ("normal".equals(st) || "drug".equals(st)) {
			p.put("special_type", st);
		}

		List<Long> mergedItemIds = null;

		Object catObj = inputData.get("category");
		if (catObj != null) {
			Long catId = parseSingleLong(catObj);
			// 0 / "0" means no category filter, not "root category id 0".
			if (catId != null && catId != 0) {
				List<Long> ids = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, catId);
				if (ids.isEmpty()) {
					throw new ResourceException("指定的分类下没有商品");
				}
				mergedItemIds = new ArrayList<>(ids);
			}
		}

		normalizeStoreAndPrice(p, inputData);

		List<Long> tagIds = parseTagIds(inputData.get("tag_id"));
		if (!tagIds.isEmpty()) {
			List<String> tagItemStr = itemsTagsQueryService.getItemIdsByTagIds(companyId, tagIds);
			if (tagItemStr.isEmpty()) {
				throw new ResourceException("指定的标签下没有商品");
			}
			List<Long> itemIds = tagItemStr.stream().map(Long::parseLong).collect(Collectors.toList());
			if (mergedItemIds == null || mergedItemIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(itemIds);
			} else {
				mergedItemIds = mergedItemIds.stream().filter(itemIds::contains).collect(Collectors.toList());
				if (mergedItemIds.isEmpty()) {
					throw new ResourceException("没有符合条件的商品");
				}
			}
		}

		Object distributorIdRaw = inputData.get("distributor_id");
		if ("merchant".equalsIgnoreCase(opType)) {
			List<Long> merchantDists = distributorListQueryService.listValidDistributorIdsForMerchant(companyId,
					merchantId != null ? merchantId : 0L);
			List<Long> effective = resolveMerchantDistributorFilter(distributorIdRaw, merchantDists);
			if (effective.isEmpty()) {
				throw new ResourceException("没有符合条件的店铺");
			}
			p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN, effective.stream().map(Long::intValue).collect(Collectors.toList()));
		} else {
			applyNonMerchantDistributor(p, distributorIdRaw, companyId, opType);
		}

		Object explicitItemId = inputData.get("item_id");
		if (explicitItemId != null && hasEffectiveItemId(explicitItemId)) {
			List<Long> reqIds = parseIdListFlexible(explicitItemId);
			if (!reqIds.isEmpty()) {
				mergedItemIds = new ArrayList<>(reqIds);
			}
			clearDistributorIfEmpty(p);
		} else if ("distributor".equalsIgnoreCase(opType)) {
			if ("standard".equals(itemsCategoryDistributorIdResolver.resolveProductModel(companyId))) {
				List<Long> distIdsForCheck = extractDistributorIdsForStandardCheck(p, distributorIdRaw);
				if (!distributorItemsRepository.existsAnyForCompanyAndDistributorIds(companyId, distIdsForCheck)) {
					throw new ResourceException("不存在店铺商品");
				}
				List<Long> fromDist = listDefaultItemIdsForDistributors(companyId, distIdsForCheck);
				if (fromDist.isEmpty()) {
					throw new ResourceException("不存在店铺商品");
				}
				mergedItemIds = new ArrayList<>(fromDist);
				p.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
				p.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
			}
		}

		if (mergedItemIds != null && !mergedItemIds.isEmpty()) {
			p.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, mergedItemIds);
		}

		Object itemBnObj = inputData.get("item_bn");
		if (itemBnObj != null && StringUtils.hasText(str(itemBnObj))) {
			String bn = str(itemBnObj).trim();
			List<Long> ids = platformPath
					? itemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(companyId, bn)
					: supplierItemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(companyId, bn);
			if (ids.isEmpty()) {
				return new ExportItemsCodeFilterBuildResult.EmptyItemBnList();
			}
			intersectItemIdOrDefault(p, ids);
			p.remove("item_bn");
		}

		p.put("isGetSkuList", true);

		return new ExportItemsCodeFilterBuildResult.ParamsReady(p);
	}

	private static String resolveItemSource(Map<String, Object> inputData) {
		String itemHolder = str(inputData.get("item_holder")).trim();
		if (StringUtils.hasText(itemHolder)) {
			if ("supplier".equalsIgnoreCase(itemHolder)) {
				return "supplier";
			}
			if ("self".equalsIgnoreCase(itemHolder) || "platform".equalsIgnoreCase(itemHolder)) {
				return "platform";
			}
		}
		String itemSource = str(inputData.get("item_source")).trim();
		return StringUtils.hasText(itemSource) ? itemSource : "";
	}

	private List<Long> listDefaultItemIdsForDistributors(long companyId, List<Long> distributorIds) {
		if (distributorIds.isEmpty()) {
			return List.of();
		}
		LinkedHashMap<String, Object> q = new LinkedHashMap<>();
		q.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		if (distributorIds.size() == 1) {
			q.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, distributorIds.get(0).intValue());
		} else {
			q.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN,
					distributorIds.stream().map(Long::intValue).collect(Collectors.toList()));
		}
		return itemsListQueryRepository.listDistinctDefaultItemIdsByFullParams(q);
	}

	private static List<Long> extractDistributorIdsForStandardCheck(LinkedHashMap<String, Object> p, Object distributorIdRaw) {
		@SuppressWarnings("unchecked")
		List<Integer> din = (List<Integer>) p.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
		if (din != null && !din.isEmpty()) {
			return din.stream().map(Integer::longValue).collect(Collectors.toList());
		}
		Object eq = p.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
		if (eq != null) {
			return List.of(parseLong(eq));
		}
		if (distributorIdRaw instanceof List<?> l && !l.isEmpty()) {
			List<Long> o = new ArrayList<>();
			for (Object x : l) {
				o.add(parseLong(x));
			}
			return o;
		}
		if (distributorIdRaw != null && StringUtils.hasText(str(distributorIdRaw))) {
			return List.of(parseLong(distributorIdRaw));
		}
		return List.of(0L);
	}

	private void applyNonMerchantDistributor(LinkedHashMap<String, Object> p,
			Object distributorIdRaw, long companyId, String opType) {
		String didStr = distributorIdRaw != null ? str(distributorIdRaw) : "";
		if ("all_distributor".equalsIgnoreCase(didStr)) {
			List<Long> all = distributorListQueryService.listAllDistributorIdsForCompany(companyId);
			p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN, all.stream().map(Long::intValue).collect(Collectors.toList()));
			return;
		}
		if (distributorIdRaw instanceof List<?> l && !l.isEmpty()) {
			List<Integer> ints = new ArrayList<>();
			for (Object o : l) {
				ints.add((int) parseLong(o));
			}
			p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN, ints);
			return;
		}
		if (distributorIdRaw != null && StringUtils.hasText(didStr) && !"all_distributor".equalsIgnoreCase(didStr)) {
			try {
				long v = Long.parseLong(didStr.trim());
				if (v > 0) {
					p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN, List.of((int) v));
				} else {
					p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, 0);
				}
			} catch (NumberFormatException e) {
				p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, 0);
			}
			return;
		}
		if ("admin".equalsIgnoreCase(opType) || "staff".equalsIgnoreCase(opType)) {
			p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, 0);
		}
	}

	private List<Long> resolveMerchantDistributorFilter(Object distributorIdRaw, List<Long> merchantDists) {
		if (merchantDists.isEmpty()) {
			return List.of();
		}
		String didStr = distributorIdRaw != null ? str(distributorIdRaw) : "";
		if ("all_distributor".equalsIgnoreCase(didStr)) {
			return merchantDists;
		}
		if (distributorIdRaw instanceof List<?> l && !l.isEmpty()) {
			List<Long> q = new ArrayList<>();
			for (Object o : l) {
				q.add(parseLong(o));
			}
			return distributorListQueryService.intersectSorted(merchantDists, q);
		}
		if (distributorIdRaw != null && StringUtils.hasText(didStr)) {
			try {
				long v = Long.parseLong(didStr.trim());
				if (v > 0 && merchantDists.contains(v)) {
					return List.of(v);
				}
			} catch (NumberFormatException ignored) {
				// fall through
			}
			return List.of();
		}
		return merchantDists;
	}

	private static void clearDistributorIfEmpty(LinkedHashMap<String, Object> p) {
		Object eq = p.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
		@SuppressWarnings("unchecked")
		List<Integer> din = (List<Integer>) p.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
		boolean emptyEq = eq != null && Integer.valueOf(0).equals(toIntBox(eq)) && (din == null || din.isEmpty());
		boolean emptyIn = din != null && din.isEmpty();
		if (emptyEq || emptyIn) {
			p.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ);
			p.remove(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN);
		}
	}

	private static Integer toIntBox(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(str(o).trim());
	}

	private static void intersectItemIdOrDefault(LinkedHashMap<String, Object> p, List<Long> ids) {
		@SuppressWarnings("unchecked")
		List<Long> ex = (List<Long>) p.get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		if (ex == null || ex.isEmpty()) {
			p.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(ids));
			return;
		}
		List<Long> inter = ex.stream().filter(ids::contains).collect(Collectors.toList());
		p.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, inter);
	}

	private static void normalizeStoreAndPrice(LinkedHashMap<String, Object> p, Map<String, Object> inputData) {
		Object storeGt = inputData.get("store_gt");
		if (storeGt != null && StringUtils.hasText(str(storeGt))) {
			p.put(ItemsListQueryRepository.KEY_STORE_GT, Integer.parseInt(str(storeGt).trim()));
		}
		Object storeLt = inputData.get("store_lt");
		if (storeLt != null && StringUtils.hasText(str(storeLt))) {
			p.put(ItemsListQueryRepository.KEY_STORE_LT, Integer.parseInt(str(storeLt).trim()));
		}
		Object priceGt = inputData.get("price_gt");
		if (priceGt != null && StringUtils.hasText(str(priceGt))) {
			p.put(ItemsListQueryRepository.KEY_PRICE_GT_CENTS, GoodsItemsListParamBuilder.priceToCents(priceGt));
		}
		Object priceLt = inputData.get("price_lt");
		if (priceLt != null && StringUtils.hasText(str(priceLt))) {
			p.put(ItemsListQueryRepository.KEY_PRICE_LT_CENTS, GoodsItemsListParamBuilder.priceToCents(priceLt));
		}
	}

	private static void validateRebateIfPresent(Map<String, Object> inputData) {
		if (!inputData.containsKey("rebate")) {
			return;
		}
		Object r = inputData.get("rebate");
		if (r == null || !StringUtils.hasText(str(r))) {
			return;
		}
		int v;
		try {
			v = Integer.parseInt(str(r).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("rebate 参数无效");
		}
		if (v < 0 || v > 3) {
			throw new BadRequestException("rebate 参数无效");
		}
	}

	private static void putTrimmedStringIfPresent(LinkedHashMap<String, Object> p, Map<String, Object> in, String key) {
		Object v = in.get(key);
		if (v != null && StringUtils.hasText(str(v))) {
			p.put(key, str(v).trim());
		}
	}

	private static boolean isPhpTruthy(String value) {
		return StringUtils.hasText(value) && !"0".equals(value.trim());
	}

	private static String joinRegions(Object regions) {
		if (regions instanceof List<?> l) {
			List<String> parts = new ArrayList<>();
			for (Object o : l) {
				if (o != null && StringUtils.hasText(str(o))) {
					parts.add(str(o).trim());
				}
			}
			return parts.isEmpty() ? "" : String.join(",", parts);
		}
		if (regions != null && StringUtils.hasText(str(regions))) {
			return str(regions).trim();
		}
		return "";
	}

	private static Long normalizeMainCatId(Object mainCat) {
		if (mainCat instanceof List<?> l && !l.isEmpty()) {
			return parseLong(l.get(l.size() - 1));
		}
		return parseSingleLong(mainCat);
	}

	private static Long parseSingleLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = str(o).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static List<Long> parseTagIds(Object v) {
		if (v == null) {
			return List.of();
		}
		if (v instanceof List<?> l) {
			List<Long> o = new ArrayList<>();
			for (Object x : l) {
				Long id = parseSingleLong(x);
				if (id != null) {
					o.add(id);
				}
			}
			return o;
		}
		Long single = parseSingleLong(v);
		return single != null ? List.of(single) : List.of();
	}

	private static boolean hasEffectiveItemId(Object explicitItemId) {
		if (explicitItemId instanceof List<?> l) {
			return !l.isEmpty();
		}
		return StringUtils.hasText(str(explicitItemId));
	}

	private static List<Long> parseIdListFlexible(Object raw) {
		if (raw instanceof List<?> l) {
			List<Long> o = new ArrayList<>();
			for (Object x : l) {
				Long id = parseSingleLong(x);
				if (id != null) {
					o.add(id);
				}
			}
			return o;
		}
		String s = str(raw);
		if (!StringUtils.hasText(s)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : s.split(",")) {
			if (StringUtils.hasText(part)) {
				out.add(Long.parseLong(part.trim()));
			}
		}
		return out;
	}

	private static long parseLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(str(o).trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
