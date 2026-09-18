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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.crossborder.domain.OriginCountry;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.goods.service.items.ItemsSkuListAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
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
public class DistributorItemsRelListCoreService {

	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final ItemsListQueryRepository itemsListQueryRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final ItemsRepository itemsRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final OriginCountryMapper originCountryMapper;
	private final DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier;
	private final ItemsSkuListAssembler itemsSkuListAssembler;
	private final ItemsMedicineService itemsMedicineService;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final LangueProperties langueProperties;

	public DistributorItemsRelListCoreService(
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			ItemsListQueryRepository itemsListQueryRepository,
			DistributorItemsRepository distributorItemsRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier,
			ItemsRepository itemsRepository,
			ItemsRelCatsRepository itemsRelCatsRepository,
			OriginCountryMapper originCountryMapper,
			DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier,
			ItemsSkuListAssembler itemsSkuListAssembler,
			ItemsMedicineService itemsMedicineService,
			ItemsAttributesRepository itemsAttributesRepository,
			LangueProperties langueProperties) {
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.itemsRepository = itemsRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.originCountryMapper = originCountryMapper;
		this.distributorItemsListSkuSpecApplier = distributorItemsListSkuSpecApplier;
		this.itemsSkuListAssembler = itemsSkuListAssembler;
		this.itemsMedicineService = itemsMedicineService;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> query(
			HttpServletRequest request,
			long companyId,
			long distributorId,
			Map<String, Object> filterBase,
			int pageSize,
			int page,
			Map<String, Object> filterResponse) {
		return query(companyId, distributorId, filterBase, pageSize, page, filterResponse, resolveLang(request, null));
	}

	/**
	 * @param langOverride when non-empty, used as locale tag for row i18n; otherwise {@code zh-CN}.
	 */
	public Map<String, Object> query(
			long companyId,
			long distributorId,
			Map<String, Object> filterBase,
			int pageSize,
			int page,
			Map<String, Object> filterResponse,
			String langOverride) {
		Map<String, Object> qp = new LinkedHashMap<>(filterBase);
		Object canSaleFilter = qp.remove("__dist_is_can_sale_filter");
		qp.remove("distributor_id");
		qp.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);

		final boolean standard = "standard".equals(itemsCategoryDistributorIdResolver.resolveProductModel(companyId));
		applyDistributorItemsScopeFilter(qp, distributorId, standard, canSaleFilter);

		filterResponse.clear();
		filterResponse.putAll(toFilterSnapshot(filterBase));
		filterResponse.put("distributor_id", distributorId);
		filterResponse.put("company_id", companyId);

		long total = itemsListQueryRepository.countByParams(qp);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total <= 0) {
			out.put("list", List.of());
			return out;
		}

		int off = Math.max(0, (page - 1) * pageSize);
		int limit = pageSize;
		List<Items> items = itemsListQueryRepository.selectPageByParamsItemIdDesc(qp, off, limit);
		List<Map<String, Object>> rows = items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		for (Map<String, Object> r : rows) {
			Object st = r.get("store");
			long sv = st instanceof Number n ? n.longValue() : 0L;
			r.put("v_store", sv != 0 ? 1 : 0);
		}

		if (!standard) {
			if (!isSkuListMode(filterBase)) {
				itemsListQueryRepository.dealListStore(companyId, rows);
				for (Map<String, Object> r : rows) {
					Object st = r.get("store");
					long sv = st instanceof Number n ? n.longValue() : 0L;
					r.put("v_store", sv != 0 ? 1 : 0);
				}
			}
		}

		if (standard) {
			applyDistributorSkuReplace(companyId, distributorId, rows, false);
			if (!isSkuListMode(filterBase)) {
				dealListStoreBySpuForDistributor(companyId, distributorId, rows);
			}
			distributorItemsListSkuSpecApplier.apply(companyId, rows);
		} else {
			// Platform product model: same pipeline as admin goods list (ItemsService::getItemsList): no distributor SKU overlay, add type labels and medicine.
			itemsSkuListAssembler.applyTypeLabels(rows);
		}

		String lang = StringUtils.hasText(langOverride) ? langOverride.trim() : "zh-CN";
		itemsListMultiLangApplier.applyToRows(companyId, lang, rows);
		for (Map<String, Object> r : rows) {
			r.put("itemName", r.get("item_name"));
		}

		if (!standard) {
			applyBrandLogoFieldsFromAttributes(companyId, rows);
			itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		}

		fillSaleCategoryIds(companyId, rows);
		applyOrigincountry(companyId, rows);

		out.put("list", rows);
		return out;
	}

	public Map<String, Object> queryOpenapiSpuPage(
			long companyId,
			long distributorId,
			Map<String, Object> filterBase,
			int pageSize,
			int page,
			String langOverride) {
		Map<String, Object> qp = new LinkedHashMap<>(filterBase);
		qp.remove("distributor_id");
		qp.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);

		final boolean standard =
				"standard".equals(itemsCategoryDistributorIdResolver.resolveProductModel(companyId));
		applyDistributorItemsScopeFilter(qp, distributorId, standard, null);

		long total = itemsListQueryRepository.countByParams(qp);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total <= 0) {
			out.put("list", List.of());
			return out;
		}

		int off = Math.max(0, (page - 1) * pageSize);
		int limit = pageSize;
		List<Items> items = itemsListQueryRepository.selectPageByParamsItemIdDesc(qp, off, limit);
		List<Map<String, Object>> rows =
				items.stream().map(GoodsItemsListRowMapper::toRow).collect(Collectors.toList());
		for (Map<String, Object> r : rows) {
			Object st = r.get("store");
			long sv = st instanceof Number n ? n.longValue() : 0L;
			r.put("v_store", sv != 0 ? 1 : 0);
		}

		if (!standard) {
			if (!isSkuListMode(filterBase)) {
				itemsListQueryRepository.dealListStore(companyId, rows);
				for (Map<String, Object> r : rows) {
					Object st = r.get("store");
					long sv = st instanceof Number n ? n.longValue() : 0L;
					r.put("v_store", sv != 0 ? 1 : 0);
				}
			}
		}

		if (standard) {
			applyDistributorSkuReplace(companyId, distributorId, rows, false);
			if (!isSkuListMode(filterBase)) {
				dealListStoreBySpuForDistributor(companyId, distributorId, rows);
			}
			distributorItemsListSkuSpecApplier.apply(companyId, rows);
		} else {
			itemsSkuListAssembler.applyTypeLabels(rows);
		}

		String lang = StringUtils.hasText(langOverride) ? langOverride.trim() : "zh-CN";
		itemsListMultiLangApplier.applyToRows(companyId, lang, rows);
		for (Map<String, Object> r : rows) {
			r.put("itemName", r.get("item_name"));
		}

		if (!standard) {
			applyBrandLogoFieldsFromAttributes(companyId, rows);
			itemsMedicineService.applyMedicineDataToRows(companyId, rows);
		}

		fillSaleCategoryIds(companyId, rows);
		applyOrigincountry(companyId, rows);

		out.put("list", rows);
		return out;
	}

	/**
	 * Echo filter keys for API consumers: match {@code __getItemFilter} shape, not internal query params
	 * (no virtual-join flags, {@code is_default} not {@code is_default_true}).
	 */
	private static Map<String, Object> toFilterSnapshot(Map<String, Object> filterBase) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : filterBase.entrySet()) {
			String k = e.getKey();
			if ("__dist_is_can_sale_filter".equals(k)) {
				out.put("is_can_sale", e.getValue());
				continue;
			}
			if ("is_default_true".equals(k)) {
				out.put("is_default", Boolean.TRUE);
				continue;
			}
			if (ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS.equals(k)) {
				out.put("item_id", e.getValue());
				continue;
			}
			if (ItemsListQueryRepository.KEY_ITEM_CATEGORY_IN.equals(k)) {
				out.put("item_category", e.getValue());
				continue;
			}
			if (ItemsListQueryRepository.KEY_SUPPLIER_ID_IN.equals(k)) {
				out.put("supplier_id", e.getValue());
				continue;
			}
			if (ItemsListQueryRepository.KEY_SUPPLIER_ID_EQ.equals(k)) {
				out.put("supplier_id", e.getValue());
				continue;
			}
			if (ItemsListQueryRepository.KEY_STORE_GT.equals(k)) {
				out.put("store|gt", e.getValue());
				continue;
			}
			if (ItemsListQueryRepository.KEY_STORE_LT.equals(k)) {
				out.put("store|lt", e.getValue());
				continue;
			}
			if (ItemsListQueryRepository.KEY_STORE_LTE_WARNING.equals(k)) {
				out.put("store|lte", e.getValue());
				continue;
			}
			if (ItemsListQueryRepository.KEY_DIST_LIST_SUPPLIER_ID_GTE1.equals(k)) {
				out.put("supplier_id|gte", 1);
				continue;
			}
			if (ItemsListQueryRepository.KEY_ITEM_SOURCE_SUPPLIER_MODE.equals(k)) {
				out.put("supplier_id|gte", 1);
				out.put("audit_status", "approved");
				continue;
			}
			if (ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE.equals(k)) {
				out.put("supplier_id", 0);
				continue;
			}
			if (ItemsListQueryRepository.KEY_COMPANY_ID.equals(k)) {
				out.put("company_id", e.getValue());
				continue;
			}
			if ("item_name".equals(k)) {
				out.put("item_name|contains", e.getValue());
				continue;
			}
			if (k != null && k.startsWith("dist_rel_virt_")) {
				continue;
			}
			out.put(k, e.getValue());
		}
		return out;
	}

	/**
	 * standard 模式：仅返回已在 {@code distribution_distributor_items} 关联到该店铺的商品；
	 * platform 模式：按 {@code items.distributor_id} 过滤商户商品。
	 */
	private static void applyDistributorItemsScopeFilter(
			Map<String, Object> qp, long distributorId, boolean standard, Object canSaleFilter) {
		if (standard && distributorId > 0) {
			qp.put(ItemsListQueryRepository.KEY_DIST_REL_VIRT_JOIN, Boolean.TRUE);
			qp.put(ItemsListQueryRepository.KEY_DIST_REL_VIRT_DISTRIBUTOR_ID, distributorId);
			// 与 PHP getDistributorRelItemList：请求 is_can_sale 映射为 d_items.goods_can_sale
			if (canSaleFilter instanceof Boolean b) {
				qp.put(ItemsListQueryRepository.KEY_DIST_REL_VIRT_GOODS_CAN_SALE, b);
			}
			return;
		}
		if (!standard && !qp.containsKey(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ)) {
			qp.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, (int) distributorId);
		}
	}

	private void applyBrandLogoFieldsFromAttributes(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		for (Map<String, Object> r : rows) {
			int bid = (int) longVal(r.get("brand_id"));
			if (bid <= 0) {
				continue;
			}
			ItemsAttributes brand = itemsAttributesRepository.selectByCompanyAndAttributeId(companyId, bid);
			if (brand == null) {
				continue;
			}
			if (!StringUtils.hasText(str(r.get("goods_brand")))) {
				r.put("goods_brand", brand.getAttributeName() != null ? brand.getAttributeName() : "");
			}
			if (brand.getImageUrl() != null && StringUtils.hasText(brand.getImageUrl())) {
				r.put("brand_logo", brand.getImageUrl());
			}
		}
	}

	private void fillSaleCategoryIds(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		Set<Long> itemIds = rows.stream().map(r -> longVal(r.get("item_id"))).filter(id -> id > 0).collect(Collectors.toCollection(LinkedHashSet::new));
		List<ItemsRelCats> rels = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds);
		Map<Long, List<Long>> byItem = new HashMap<>();
		for (ItemsRelCats rc : rels) {
			if (rc.getItemId() == null || rc.getCategoryId() == null) {
				continue;
			}
			byItem.computeIfAbsent(rc.getItemId().longValue(), k -> new ArrayList<>()).add(rc.getCategoryId());
		}
		for (Map<String, Object> r : rows) {
			long iid = longVal(r.get("item_id"));
			List<Long> cats = byItem.getOrDefault(iid, List.of());
			r.put("item_cat_id", cats);
		}
	}

	private void applyOrigincountry(long companyId, List<Map<String, Object>> rows) {
		Set<Long> ocIds = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			long oc = longVal(r.get("origincountry_id"));
			if (oc > 0) {
				ocIds.add(oc);
			}
		}
		if (ocIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<OriginCountry> w = new LambdaQueryWrapper<>();
		w.eq(OriginCountry::getCompanyId, companyId).in(OriginCountry::getOrigincountryId, ocIds);
		List<OriginCountry> list = originCountryMapper.selectList(w);
		Map<Long, OriginCountry> byId = list.stream().collect(Collectors.toMap(OriginCountry::getOrigincountryId, x -> x, (a, b) -> a));
		Set<Long> validIds = list.stream().map(OriginCountry::getOrigincountryId).collect(Collectors.toSet());
		for (Map<String, Object> r : rows) {
			int type = (int) longVal(r.get("type"));
			long oc = longVal(r.get("origincountry_id"));
			if (type != 1 || oc <= 0 || !validIds.contains(oc)) {
				r.put("origincountry_name", "");
				r.put("origincountry_img_url", "");
				continue;
			}
			OriginCountry o = byId.get(oc);
			if (o == null) {
				r.put("origincountry_name", "");
				r.put("origincountry_img_url", "");
			} else {
				r.put("origincountry_name", o.getOrigincountryName() != null ? o.getOrigincountryName() : "");
				r.put("origincountry_img_url", o.getOrigincountryImgUrl() != null ? o.getOrigincountryImgUrl() : "");
			}
		}
	}

	private void dealListStoreBySpuForDistributor(long companyId, long distributorId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> goodsIds = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			long gid = longVal(r.get("goods_id"));
			if (gid > 0) {
				goodsIds.add(gid);
			}
		}
		if (goodsIds.isEmpty()) {
			return;
		}
		List<Items> skus = itemsRepository.listItemIdGoodsIdStoreByCompanyAndGoodsIds(companyId, goodsIds);
		if (skus.isEmpty()) {
			return;
		}
		List<Long> itemIds = skus.stream().map(Items::getItemId).filter(Objects::nonNull).distinct().toList();
		Map<Long, DistributorItems> diByItem =
				distributorItemsRepository.listByDistributorAndItemIds(companyId, distributorId, itemIds).stream()
						.collect(Collectors.toMap(DistributorItems::getItemId, x -> x, (a, b) -> a));
		Map<Long, Integer> sumByGoods = new LinkedHashMap<>();
		for (Items sku : skus) {
			Long gid = sku.getGoodsId();
			if (gid == null || gid <= 0) {
				continue;
			}
			long itemId = sku.getItemId() != null ? sku.getItemId() : 0L;
			int effective = resolveEffectiveListStore(sku, itemId > 0 ? diByItem.get(itemId) : null);
			sumByGoods.merge(gid, effective, Integer::sum);
		}
		for (Map<String, Object> r : rows) {
			long gid = longVal(r.get("goods_id"));
			if (gid <= 0 || !sumByGoods.containsKey(gid)) {
				continue;
			}
			int total = sumByGoods.get(gid);
			r.put("store", total);
			r.put("v_store", total != 0 ? 1 : 0);
		}
	}

	private static int resolveEffectiveListStore(Items sku, DistributorItems di) {
		if (di != null && !Boolean.TRUE.equals(di.getIsTotalStore()) && di.getStore() != null) {
			return di.getStore().intValue();
		}
		return sku.getStore() != null ? sku.getStore() : 0;
	}

	/** 与 PHP {@code dealListStore($result, $isGetSkuList)} 一致：SKU 列表不汇总 SPU 库存。 */
	private static boolean isSkuListMode(Map<String, Object> filterBase) {
		if (filterBase == null) {
			return false;
		}
		Object raw = filterBase.get("is_sku");
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw == null) {
			return false;
		}
		String t = String.valueOf(raw).trim();
		return "true".equalsIgnoreCase(t) || "1".equals(t);
	}

	private void applyDistributorSkuReplace(long companyId, long distributorId, List<Map<String, Object>> rows, boolean replaceApprove) {
		if (rows.isEmpty()) {
			return;
		}
		List<Long> itemIds = rows.stream().map(r -> longVal(r.get("item_id"))).filter(id -> id > 0).distinct().toList();
		List<DistributorItems> rels = distributorItemsRepository.listByDistributorAndItemIds(companyId, distributorId, itemIds);
		Map<Long, DistributorItems> byItem = rels.stream().collect(Collectors.toMap(DistributorItems::getItemId, x -> x, (a, b) -> a));

		for (Map<String, Object> row : rows) {
			long itemId = longVal(row.get("item_id"));
			DistributorItems di = byItem.get(itemId);
			row.put("item_owner_distributor_id", row.get("distributor_id"));
			row.put("distributor_id", distributorId);
			if (di != null) {
				row.put("distributor_store", di.getStore() != null ? di.getStore().intValue() : -1);
				boolean totalStore = Boolean.TRUE.equals(di.getIsTotalStore());
				if (!totalStore) {
					if (di.getStore() != null) {
						row.put("store", di.getStore());
					}
					if (di.getPrice() != null) {
						row.put("price", di.getPrice().intValue());
					}
				}
				if (replaceApprove) {
					if (Boolean.FALSE.equals(di.getIsCanSale()) || (di.getIsCanSale() == null)) {
						row.put("approve_status", "instock");
					}
					if (Boolean.TRUE.equals(di.getIsCanSale()) && !totalStore) {
						row.put("approve_status", "onsale");
					}
				}
				row.put("goods_can_sale", di.getGoodsCanSale());
				row.put("is_can_sale", di.getIsCanSale());
				row.put("is_total_store", totalStore);
			} else {
				row.put("distributor_store", -1);
				row.put("is_can_sale", false);
				row.put("goods_can_sale", false);
				row.put("is_total_store", false);
				if (replaceApprove) {
					row.put("approve_status", "instock");
				}
			}
		}
	}

	private String resolveLang(HttpServletRequest request, String overrideLang) {
		if (StringUtils.hasText(overrideLang)) {
			return overrideLang.trim();
		}
		return RequestLangTag.current(langueProperties);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
