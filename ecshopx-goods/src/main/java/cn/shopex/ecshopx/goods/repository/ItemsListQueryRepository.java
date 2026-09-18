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

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Slf4j
@Repository
public class ItemsListQueryRepository {

	public static final String KEY_COMPANY_ID = "company_id";
	public static final String KEY_ITEM_ID_OR_DEFAULT_IDS = "item_id_or_default_ids";

	/** Single-SKU filter: {@code item_id = value} (mutually exclusive with {@link #KEY_ITEM_ID_OR_DEFAULT_IDS}). */
	public static final String KEY_MEMBER_PRICE_ITEM_ID_EQ = "member_price_item_id_eq";

	/** Multi-SKU filter: {@code default_item_id = value} (mutually exclusive with {@link #KEY_ITEM_ID_OR_DEFAULT_IDS}). */
	public static final String KEY_MEMBER_PRICE_DEFAULT_ITEM_ID_EQ = "member_price_default_item_id_eq";
	public static final String KEY_ITEM_CATEGORY_IN = "item_category_in";
	public static final String KEY_DISTRIBUTOR_ID_IN = "distributor_id_in";
	public static final String KEY_DISTRIBUTOR_ID_EQ = "distributor_id_eq";
	public static final String KEY_DISTRIBUTOR_ID_GT0_ONLY = "distributor_id_gt0_only";
	public static final String KEY_DISTRIBUTOR_APPROVE_STATUS = "distributor_approve_status";
	public static final String KEY_STORE_STATUS = "store_status";
	public static final String KEY_STORE_GT = "store_gt";
	public static final String KEY_STORE_LT = "store_lt";
	public static final String KEY_PRICE_GT_CENTS = "price_gt_cents";
	public static final String KEY_PRICE_LT_CENTS = "price_lt_cents";

	public static final String KEY_SORT_GTE = "sort_gte";
	public static final String KEY_STORE_LTE_WARNING = "store_lte_warning";
	public static final String KEY_ITEM_SOURCE_SUPPLIER_MODE = "item_source_supplier_mode";
	public static final String KEY_ITEM_SOURCE_NON_SUPPLIER_MODE = "item_source_non_supplier_mode";
	public static final String KEY_SUPPLIER_ID_IN = "supplier_id_in";
	public static final String KEY_SUPPLIER_ID_EQ = "supplier_id_eq";

	/** Virtual store: extra EXISTS filter on {@code distribution_distributor_items}. */
	public static final String KEY_DIST_REL_VIRT_JOIN = "dist_rel_virt_join";

	public static final String KEY_DIST_REL_VIRT_DISTRIBUTOR_ID = "dist_rel_virt_distributor_id";

	public static final String KEY_DIST_REL_VIRT_GOODS_CAN_SALE = "dist_rel_virt_goods_can_sale";

	/** Distributor item list legacy flag: {@code supplier_id >= 1} only (prefer {@link #KEY_ITEM_SOURCE_SUPPLIER_MODE}). */
	public static final String KEY_DIST_LIST_SUPPLIER_ID_GTE1 = "dist_list_supplier_id_gte1";

	/** Lower bound on {@code items.updated} (Unix seconds), for incremental sync filters. */
	public static final String KEY_UPDATED_GTE = "updated_gte";

	/** Upper bound on {@code items.updated} (Unix seconds). */
	public static final String KEY_UPDATED_LTE = "updated_lte";

	/**
	 * OpenAPI ecx.items.entity.get：复现不存在列 {@code items.category_id} 的等值过滤（触发 SQL 错误 → E5000）。
	 */
	public static final String KEY_LEGACY_OPENAPI_CATEGORY_ID_EQ = "legacy_openapi_category_id_eq";

	/**
	 * OpenAPI：{@code is_default=true} 且传入 approve_status 时，EXISTS 子查询绑定动态单值 approve_status。
	 */
	public static final String KEY_EXISTS_INNER_ITEMS_APPROVE_STATUS_EQ = "exists_inner_items_approve_status_eq";

	/** Restrict to {@code goods_id IN (...)} (wxapp / 推广商品等). */
	public static final String KEY_GOODS_ID_IN = "goods_id_in";

	/**
	 * When present as non-empty collection of status tokens, adds EXISTS(inner_items...) and drops outer
	 * {@code approve_status} from the filter map so outer approve IN is not applied.
	 */
	public static final String KEY_EXISTS_INNER_ITEMS_APPROVE_STATUS_IN = "exists_inner_items_approve_status_in";

	/**
	 * Outer {@code items.is_default} numeric equality. Mutually exclusive usage with legacy {@code is_default_true} for
	 * paths that pass this key: use only {@link #KEY_IS_DEFAULT_EQ}.
	 */
	public static final String KEY_IS_DEFAULT_EQ = "is_default_eq";

	/** When true, exclude rows with {@code items.type = 1} (cross-border), equivalent to {@code type|neq} = 1. */
	public static final String KEY_EXCLUDE_TYPE_EQ_1 = "exclude_type_eq_1";

	private final ItemsMapper mapper;

	public ItemsListQueryRepository(ItemsMapper mapper) {
		this.mapper = mapper;
	}

	public List<Long> mergeDefaultItemIdsByItemNameOrBrief(long companyId, String keywords) {
		if (!StringUtils.hasText(keywords)) {
			return List.of();
		}
		String kw = keywords.trim();
		Set<Long> ids = new LinkedHashSet<>();
		ids.addAll(selectDistinctDefaultItemIdsLikeItemName(companyId, kw));
		ids.addAll(selectDistinctDefaultItemIdsLikeBrief(companyId, kw));
		return new ArrayList<>(ids);
	}

	public List<Long> mergeDefaultItemIdsByItemBnOrBarcodeContains(long companyId, String itemBn) {
		if (!StringUtils.hasText(itemBn)) {
			return List.of();
		}
		String kw = itemBn.trim();
		Set<Long> ids = new LinkedHashSet<>();
		ids.addAll(selectDistinctDefaultItemIdsLikeItemBn(companyId, kw));
		ids.addAll(selectDistinctDefaultItemIdsLikeBarcode(companyId, kw));
		return new ArrayList<>(ids);
	}

	public List<Long> listDefaultItemIdsByBarcodeExact(long companyId, String barcode) {
		if (!StringUtils.hasText(barcode)) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).eq(Items::getBarcode, barcode.trim());
		return selectDistinctDefaultItemIds(w);
	}

	/**
	 * default_item_id 并集：商品名包含 K、或货号精确 K、或条码精确 K（同一 K）。
	 */
	public List<Long> listDefaultItemIdsByKeywordsOrBlock(long companyId, String keywords) {
		if (!StringUtils.hasText(keywords)) {
			return List.of();
		}
		String k = keywords.trim();
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).and(q -> q.like(Items::getItemName, k).or().eq(Items::getItemBn, k).or().eq(Items::getBarcode, k));
		return selectDistinctDefaultItemIds(w);
	}

	/** 仅商品名包含（不含 brief），对齐 onsale item_name|contains。 */
	public List<Long> listDefaultItemIdsByItemNameContainsOnly(long companyId, String text) {
		if (!StringUtils.hasText(text)) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).like(Items::getItemName, text.trim());
		return selectDistinctDefaultItemIds(w);
	}

	/** 货号精确匹配。 */
	public List<Long> listDefaultItemIdsByItemBnExact(long companyId, String itemBn) {
		if (!StringUtils.hasText(itemBn)) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemBn, itemBn.trim());
		return selectDistinctDefaultItemIds(w);
	}

	private List<Long> selectDistinctDefaultItemIdsLikeItemName(long companyId, String kw) {
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).like(Items::getItemName, kw);
		return selectDistinctDefaultItemIds(w);
	}

	private List<Long> selectDistinctDefaultItemIdsLikeBrief(long companyId, String kw) {
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).like(Items::getBrief, kw);
		return selectDistinctDefaultItemIds(w);
	}

	private List<Long> selectDistinctDefaultItemIdsLikeItemBn(long companyId, String kw) {
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).like(Items::getItemBn, kw);
		return selectDistinctDefaultItemIds(w);
	}

	private List<Long> selectDistinctDefaultItemIdsLikeBarcode(long companyId, String kw) {
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).like(Items::getBarcode, kw);
		return selectDistinctDefaultItemIds(w);
	}

	private List<Long> selectDistinctDefaultItemIds(LambdaQueryWrapper<Items> w) {
		List<Items> rows = mapper.selectList(w);
		return rows.stream().map(Items::getDefaultItemId).filter(Objects::nonNull).filter(id -> id > 0).distinct().collect(Collectors.toList());
	}

	public long countByParams(Map<String, Object> p) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		long c = mapper.selectCount(w);
		log.info("items list count={} companyId={}", c, p.get(KEY_COMPANY_ID));
		return c;
	}

	public List<Items> selectPageByParams(Map<String, Object> p, int offset, int limit) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		applyStableListOrder(w);
		w.last("LIMIT " + offset + "," + limit);
		return mapper.selectList(w);
	}

	/** OpenAPI goods_list：与 {@link #buildWrapper} 相同过滤，无 ORDER BY，LIMIT/OFFSET 分页。 */
	public List<Items> selectPageByParamsWithoutOrder(Map<String, Object> p, int offset, int limit) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		w.last("LIMIT " + offset + "," + limit);
		return mapper.selectList(w);
	}

	/** OpenAPI goods_list：pageSize <= 0 时全量返回，无 ORDER BY、无 LIMIT。 */
	public List<Items> selectAllByParamsWithoutOrder(Map<String, Object> p) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		return mapper.selectList(w);
	}

	/** Paged list for distributor-associated SKUs, ordered by {@code item_id} descending (default for shop item list). */
	public List<Items> selectPageByParamsItemIdDesc(Map<String, Object> p, int offset, int limit) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		w.orderByDesc(Items::getItemId);
		if (limit >= 0) {
			w.last("LIMIT " + offset + "," + limit);
		}
		return mapper.selectList(w);
	}

	/** Same filters as list pagination; used when {@code is_sku} returns all SKU rows (no default-item-only constraint). */
	public List<Items> selectPageByParamsForSku(Map<String, Object> p, int offset, int limit) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		applyStableListOrder(w);
		if (limit >= 0) {
			w.last("LIMIT " + offset + "," + limit);
		}
		return mapper.selectList(w);
	}

	/**
	 * Same as {@link #selectPageByParamsForSku} but orders like wxapp default list ({@code goodsSort} + {@code item_id} DESC),
	 * without stable {@code default_item_id} ordering.
	 */
	public List<Items> selectPageByParamsForSkuWithWxappGoodsSort(Map<String, Object> p, int offset, int limit, String orderBySpec) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		applyWxappDefaultListOrder(w, orderBySpec == null ? "" : orderBySpec.trim());
		if (limit >= 0) {
			w.last("LIMIT " + offset + "," + limit);
		}
		return mapper.selectList(w);
	}

	/**
	 * 与 {@link #selectPageByParams} 相同过滤条件，排序由 {@code orderBySpec} 决定（小程序商品列表 goodsSort）。
	 *
	 * @param orderBySpec {@code 1}–{@code 5} 或空字符串表示默认按 sort；始终追加 item_id DESC。
	 */
	public List<Items> selectPageByParamsWithWxappGoodsSort(Map<String, Object> p, int offset, int limit, String orderBySpec) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		applyWxappDefaultListOrder(w, orderBySpec == null ? "" : orderBySpec.trim());
		w.last("LIMIT " + offset + "," + limit);
		return mapper.selectList(w);
	}

	/**
	 * Same filters as list queries; returns distinct {@code default_item_id} values for SKU {@code item_bn} pre-filtering.
	 */
	public List<Long> listDistinctDefaultItemIdsByFullParams(Map<String, Object> p) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		List<Items> rows = mapper.selectList(w);
		return rows.stream().map(Items::getDefaultItemId).filter(Objects::nonNull).filter(id -> id > 0).distinct().collect(Collectors.toList());
	}

	/**
	 * Returns distinct {@code goods_id} values for rows matching the same filters as list queries.
	 */
	public List<Long> listDistinctGoodsIdsByFullParams(Map<String, Object> p) {
		LambdaQueryWrapper<Items> w = buildWrapper(p);
		List<Items> rows = mapper.selectList(w);
		return rows.stream().map(Items::getGoodsId).filter(Objects::nonNull).filter(id -> id > 0).distinct().collect(Collectors.toList());
	}

	public Map<Long, Integer> sumStoreByGoodsIds(long companyId, Collection<Long> goodsIds) {
		Map<Long, Integer> sums = new LinkedHashMap<>();
		if (goodsIds == null || goodsIds.isEmpty()) {
			return sums;
		}
		for (Long gid : goodsIds) {
			if (gid == null || gid <= 0) {
				continue;
			}
			LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
			w.eq(Items::getCompanyId, companyId).eq(Items::getGoodsId, gid);
			List<Items> rows = mapper.selectList(w);
			int t = 0;
			for (Items it : rows) {
				if (it.getStore() != null) {
					t += it.getStore();
				}
			}
			sums.put(gid, t);
		}
		return sums;
	}

	public void dealListStore(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> goodsIds = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			Object g = r.get("goods_id");
			if (g instanceof Number n) {
				long gid = n.longValue();
				if (gid > 0) {
					goodsIds.add(gid);
				}
			}
		}
		Map<Long, Integer> sum = sumStoreByGoodsIds(companyId, goodsIds);
		for (Map<String, Object> r : rows) {
			Object g = r.get("goods_id");
			if (g instanceof Number n) {
				Long gid = n.longValue();
				if (gid > 0 && sum.containsKey(gid)) {
					r.put("store", sum.get(gid));
				}
			}
		}
	}

	public static String grossProfitRateYuan(Integer priceCents, Integer costCents) {
		if (priceCents == null || priceCents <= 0 || costCents == null) {
			return "0.00";
		}
		BigDecimal price = BigDecimal.valueOf(priceCents).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
		BigDecimal cost = BigDecimal.valueOf(costCents).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
		BigDecimal diff = price.subtract(cost).multiply(BigDecimal.valueOf(100));
		BigDecimal rate = diff.divide(price, 2, RoundingMode.HALF_UP);
		return rate.toPlainString();
	}

	/**
	 * 商品列表行毛利率展示：成本缺失或为 0、或售价（分）不大于成本时返回 {@code "-"}；否则返回两位小数的百分比（售价与成本均为分，毛利率 = (售价 - 成本) / 售价 × 100）。
	 */
	public static String listRowGrossProfitRate(Integer priceCents, Integer costCents) {
		if (costCents == null || costCents == 0) {
			return "-";
		}
		if (priceCents == null || priceCents <= costCents) {
			return "-";
		}
		BigDecimal p = BigDecimal.valueOf(priceCents);
		BigDecimal c = BigDecimal.valueOf(costCents);
		return p.subtract(c).multiply(BigDecimal.valueOf(100)).divide(p, 2, RoundingMode.DOWN).toPlainString() + "%";
	}

	/** 供应商后台列表毛利率：与 PHP {@code bcmul(($price - $cost_price) * 100, $price, 2)} 一致。 */
	public static String listRowGrossProfitRatePhpParity(Integer priceCents, Integer costCents) {
		if (costCents == null || costCents == 0) {
			return "-";
		}
		if (priceCents == null || priceCents <= costCents) {
			return "-";
		}
		BigDecimal diff = BigDecimal.valueOf(priceCents - costCents).multiply(BigDecimal.valueOf(100));
		return diff.multiply(BigDecimal.valueOf(priceCents)).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
	}

	private LambdaQueryWrapper<Items> buildWrapper(Map<String, Object> p) {
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		Object cid = p.get(KEY_COMPANY_ID);
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());
		w.eq(Items::getCompanyId, companyId);

		if (Boolean.TRUE.equals(p.get(KEY_DIST_LIST_SUPPLIER_ID_GTE1))) {
			w.ge(Items::getSupplierId, 1);
		} else if (Boolean.TRUE.equals(p.get(KEY_ITEM_SOURCE_SUPPLIER_MODE))) {
			w.ge(Items::getSupplierId, 1).eq(Items::getAuditStatus, "approved");
		} else if (Boolean.TRUE.equals(p.get(KEY_ITEM_SOURCE_NON_SUPPLIER_MODE))) {
			w.eq(Items::getSupplierId, 0);
		}

		if (Boolean.TRUE.equals(p.get(KEY_EXCLUDE_TYPE_EQ_1))) {
			w.ne(Items::getType, 1);
		}

		putEqInt(w, p, "type", Items::getType);
		likeIfPresent(w, p, "item_name", Items::getItemName);
		rangeCreated(w, p);
		eqStrIfPresent(w, p, "consume_type", Items::getConsumeType);
		putEqIntObj(w, p, "templates_id", Items::getTemplatesId);
		if (p.containsKey("is_market") && p.get("is_market") != null && StringUtils.hasText(String.valueOf(p.get("is_market")))) {
			w.eq(Items::getIsMarket, toInt(p.get("is_market")));
		}
		eqStrIfPresent(w, p, "goods_bn", Items::getGoodsBn);
		eqStrIfPresent(w, p, "item_bn", Items::getItemBn);
		if (p.containsKey("regions_id") && StringUtils.hasText(str(p.get("regions_id")))) {
			w.eq(Items::getRegionsId, str(p.get("regions_id")));
		}
		if (p.containsKey("nospec")) {
			w.eq(Items::getNospec, str(p.get("nospec")));
		}
		if (p.containsKey("is_gift")) {
			Object ig = p.get("is_gift");
			boolean t = ig instanceof Number n ? n.intValue() == 1 : truthy(ig);
			w.eq(Items::getIsGift, t);
		}
		if (truthy(p.get("is_taobao"))) {
			w.eq(Items::getIsTaobao, 1);
		}

		if (p.containsKey(KEY_SUPPLIER_ID_EQ)) {
			w.eq(Items::getSupplierId, toInt(p.get(KEY_SUPPLIER_ID_EQ)));
		} else if (p.get(KEY_SUPPLIER_ID_IN) instanceof List<?> l && !l.isEmpty()) {
			List<Integer> ints = l.stream().map(ItemsListQueryRepository::toInt).collect(Collectors.toList());
			w.in(Items::getSupplierId, ints);
		}

		applyInnerItemsExistsForSalesmanStoreitems(w, p);
		applyInnerItemsExistsForOpenapiApproveStatus(w, p);
		applyApproveAudit(w, p);

		if (p.containsKey(KEY_IS_DEFAULT_EQ) && p.get(KEY_IS_DEFAULT_EQ) != null) {
			int isDef = toInt(p.get(KEY_IS_DEFAULT_EQ));
			w.apply("items.is_default = {0}", isDef);
		} else if (Boolean.TRUE.equals(p.get("is_default_true"))) {
			w.eq(Items::getIsDefault, true);
		}

		if (p.containsKey("audit_status") && StringUtils.hasText(str(p.get("audit_status")))) {
			w.eq(Items::getAuditStatus, str(p.get("audit_status")));
		}

		if (p.containsKey("rebate") && p.get("rebate") != null) {
			w.eq(Items::getRebate, toInt(p.get("rebate")));
		}
		eqStrIfPresent(w, p, "rebate_type", Items::getRebateType);

		@SuppressWarnings("unchecked")
		List<String> catIn = (List<String>) p.get(KEY_ITEM_CATEGORY_IN);
		if (catIn != null && !catIn.isEmpty()) {
			w.in(Items::getItemCategory, catIn);
		}

		Object mpItemEq = p.get(KEY_MEMBER_PRICE_ITEM_ID_EQ);
		Object mpDefEq = p.get(KEY_MEMBER_PRICE_DEFAULT_ITEM_ID_EQ);
		if (mpItemEq != null) {
			long id = mpItemEq instanceof Number n ? n.longValue() : Long.parseLong(mpItemEq.toString().trim());
			w.eq(Items::getItemId, id);
		} else if (mpDefEq != null) {
			long id = mpDefEq instanceof Number n ? n.longValue() : Long.parseLong(mpDefEq.toString().trim());
			w.eq(Items::getDefaultItemId, id);
		} else {
			@SuppressWarnings("unchecked")
			List<Long> idOrDef = (List<Long>) p.get(KEY_ITEM_ID_OR_DEFAULT_IDS);
			if (idOrDef != null && !idOrDef.isEmpty()) {
				w.and(q -> q.in(Items::getItemId, idOrDef).or().in(Items::getDefaultItemId, idOrDef));
			}
		}

		applyDistributor(w, p);
		applyDistributorApproveStatus(w, p);

		String ss = str(p.get(KEY_STORE_STATUS));
		if ("positive".equals(ss)) {
			w.gt(Items::getStore, 0);
		} else if ("zero".equals(ss)) {
			w.lt(Items::getStore, 1);
		}
		if (p.containsKey(KEY_STORE_GT)) {
			w.gt(Items::getStore, toInt(p.get(KEY_STORE_GT)));
		}
		if (p.containsKey(KEY_STORE_LT)) {
			w.lt(Items::getStore, toInt(p.get(KEY_STORE_LT)));
		}
		if (p.containsKey(KEY_PRICE_GT_CENTS)) {
			w.gt(Items::getPrice, toInt(p.get(KEY_PRICE_GT_CENTS)));
		}
		if (p.containsKey(KEY_PRICE_LT_CENTS)) {
			w.lt(Items::getPrice, toInt(p.get(KEY_PRICE_LT_CENTS)));
		}
		if (p.containsKey(KEY_SORT_GTE) && p.get(KEY_SORT_GTE) != null) {
			int bound = toInt(p.get(KEY_SORT_GTE));
			w.ge(Items::getSort, bound);
		}
		eqStrIfPresent(w, p, "special_type", Items::getSpecialType);
		if (p.containsKey(KEY_STORE_LTE_WARNING)) {
			w.le(Items::getStore, toInt(p.get(KEY_STORE_LTE_WARNING)));
		}
		if (p.containsKey("brand_id")) {
			int bid = toInt(p.get("brand_id"));
			if (bid != 0) {
				w.eq(Items::getBrandId, bid);
			}
		}
		if (p.containsKey("is_medicine")) {
			w.eq(Items::getIsMedicine, toInt(p.get("is_medicine")));
		}
		if (p.containsKey("is_prescription")) {
			w.eq(Items::getIsPrescription, toInt(p.get("is_prescription")));
		}
		if (p.containsKey("item_type") && StringUtils.hasText(str(p.get("item_type")))) {
			w.eq(Items::getItemType, str(p.get("item_type")).trim());
		}
		@SuppressWarnings("unchecked")
		List<Long> goodsIdIn = (List<Long>) p.get(KEY_GOODS_ID_IN);
		if (goodsIdIn != null && !goodsIdIn.isEmpty()) {
			List<Long> gids = goodsIdIn.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().collect(Collectors.toList());
			if (!gids.isEmpty()) {
				w.in(Items::getGoodsId, gids);
			}
		}
		if (p.containsKey(KEY_UPDATED_GTE) && p.get(KEY_UPDATED_GTE) != null) {
			Object ug = p.get(KEY_UPDATED_GTE);
			if (ug instanceof Number n) {
				if (n.longValue() > 0) {
					w.ge(Items::getUpdated, n.intValue());
				}
			} else if (StringUtils.hasText(str(ug))) {
				try {
					int t = Integer.parseInt(str(ug).trim());
					if (t > 0) {
						w.ge(Items::getUpdated, t);
					}
				} catch (NumberFormatException ignored) {
					// skip invalid bound
				}
			}
		}
		if (p.containsKey(KEY_UPDATED_LTE) && p.get(KEY_UPDATED_LTE) != null) {
			Object ul = p.get(KEY_UPDATED_LTE);
			if (ul instanceof Number n) {
				w.le(Items::getUpdated, n.intValue());
			} else if (StringUtils.hasText(str(ul))) {
				try {
					w.le(Items::getUpdated, Integer.parseInt(str(ul).trim()));
				} catch (NumberFormatException ignored) {
					w.le(Items::getUpdated, 0);
				}
			}
		}
		if (p.containsKey(KEY_LEGACY_OPENAPI_CATEGORY_ID_EQ)) {
			Object categoryIdVal = p.get(KEY_LEGACY_OPENAPI_CATEGORY_ID_EQ);
			if (categoryIdVal != null && StringUtils.hasText(str(categoryIdVal)) && !"0".equals(str(categoryIdVal).trim())) {
				w.apply("items.category_id = {0}", toInt(categoryIdVal));
			}
		}
		applyDistributorRelVirtualExists(w, p);
		return w;
	}

	private static void applyDistributorRelVirtualExists(LambdaQueryWrapper<Items> w, Map<String, Object> p) {
		if (!Boolean.TRUE.equals(p.get(KEY_DIST_REL_VIRT_JOIN))) {
			return;
		}
		Object didObj = p.get(KEY_DIST_REL_VIRT_DISTRIBUTOR_ID);
		long distributorId = didObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(didObj));
		Object cidObj = p.get(KEY_COMPANY_ID);
		long companyId = cidObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(cidObj));
		StringBuilder sql = new StringBuilder();
		sql.append("EXISTS (SELECT 1 FROM distribution_distributor_items d WHERE d.item_id = items.item_id");
		sql.append(" AND d.company_id = ").append(companyId);
		sql.append(" AND d.distributor_id = ").append(distributorId);
		if (p.containsKey(KEY_DIST_REL_VIRT_GOODS_CAN_SALE)) {
			Object g = p.get(KEY_DIST_REL_VIRT_GOODS_CAN_SALE);
			boolean v = g instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(g));
			sql.append(" AND d.goods_can_sale = ").append(v ? 1 : 0);
		}
		sql.append(")");
		w.apply(sql.toString());
	}

	/**
	 * Stable ordering for admin list views (newer default groups and SKUs first).
	 */
	private static void applyStableListOrder(LambdaQueryWrapper<Items> w) {
		w.orderByDesc(Items::getDefaultItemId).orderByDesc(Items::getItemId);
	}

	private static void applyWxappDefaultListOrder(LambdaQueryWrapper<Items> w, String goodsSort) {
		switch (goodsSort) {
			case "1" -> w.orderByDesc(Items::getSales);
			case "2" -> w.orderByDesc(Items::getPrice);
			case "3" -> w.orderByAsc(Items::getPrice);
			case "4" -> w.orderByDesc(Items::getCreated);
			case "5" -> w.orderByDesc(Items::getStore);
			default -> w.orderByDesc(Items::getSort);
		}
		w.orderByDesc(Items::getItemId);
	}

	private static void applyDistributorApproveStatus(LambdaQueryWrapper<Items> w, Map<String, Object> p) {
		String d = str(p.get(KEY_DISTRIBUTOR_APPROVE_STATUS));
		if (!StringUtils.hasText(d)) {
			return;
		}
		if ("onsale".equalsIgnoreCase(d)) {
			w.eq(Items::getApproveStatus, "onsale").eq(Items::getAuditStatus, "approved");
		} else if ("instock".equalsIgnoreCase(d)) {
			w.and(x -> x.ne(Items::getApproveStatus, "onsale").or().ne(Items::getAuditStatus, "approved"));
		}
	}

	private static void applyDistributor(LambdaQueryWrapper<Items> w, Map<String, Object> p) {
		if (Boolean.TRUE.equals(p.get(KEY_DISTRIBUTOR_ID_GT0_ONLY))) {
			w.gt(Items::getDistributorId, 0);
		}
		if (p.containsKey(KEY_DISTRIBUTOR_ID_EQ)) {
			w.eq(Items::getDistributorId, toInt(p.get(KEY_DISTRIBUTOR_ID_EQ)));
			return;
		}
		@SuppressWarnings("unchecked")
		List<Integer> din = (List<Integer>) p.get(KEY_DISTRIBUTOR_ID_IN);
		if (din != null && !din.isEmpty()) {
			w.in(Items::getDistributorId, din);
		}
	}

	private static void applyInnerItemsExistsForSalesmanStoreitems(LambdaQueryWrapper<Items> w, Map<String, Object> p) {
		Object raw = p.get(KEY_EXISTS_INNER_ITEMS_APPROVE_STATUS_IN);
		if (!(raw instanceof Collection<?> coll) || coll.isEmpty()) {
			return;
		}
		boolean any = false;
		for (Object o : coll) {
			if (o != null && StringUtils.hasText(o.toString().trim())) {
				any = true;
				break;
			}
		}
		if (!any) {
			return;
		}
		w.apply(
				"EXISTS ( SELECT 1 FROM items inner_items WHERE inner_items.goods_id = items.goods_id AND inner_items.approve_status IN ('onsale','only_show') )");
		p.remove("approve_status");
	}

	private static void applyInnerItemsExistsForOpenapiApproveStatus(LambdaQueryWrapper<Items> w, Map<String, Object> p) {
		Object raw = p.get(KEY_EXISTS_INNER_ITEMS_APPROVE_STATUS_EQ);
		if (raw == null || !StringUtils.hasText(raw.toString().trim())) {
			return;
		}
		String status = raw.toString().trim();
		w.apply(
				"EXISTS ( SELECT 1 FROM items inner_items WHERE inner_items.goods_id = items.goods_id AND inner_items.approve_status = {0} )",
				status);
		p.remove("approve_status");
	}

	private static void applyApproveAudit(LambdaQueryWrapper<Items> w, Map<String, Object> p) {
		if (!p.containsKey("approve_status")) {
			return;
		}
		Object raw = p.get("approve_status");
		if (raw instanceof Collection<?> coll && !coll.isEmpty()) {
			List<String> statuses = new ArrayList<>();
			for (Object o : coll) {
				if (o != null && StringUtils.hasText(o.toString())) {
					statuses.add(o.toString().trim());
				}
			}
			if (statuses.isEmpty()) {
				return;
			}
			w.in(Items::getApproveStatus, statuses);
			return;
		}
		String a = str(raw);
		if (!StringUtils.hasText(a)) {
			return;
		}
		if (a.contains(",")) {
			List<String> statuses = Arrays.stream(a.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
			if (!statuses.isEmpty()) {
				w.in(Items::getApproveStatus, statuses);
			}
			return;
		}
		if ("processing".equals(a) || "rejected".equals(a)) {
			w.eq(Items::getAuditStatus, a);
		} else {
			w.eq(Items::getApproveStatus, a);
		}
	}

	private static void rangeCreated(LambdaQueryWrapper<Items> w, Map<String, Object> p) {
		Object st = p.get("created_time_start");
		Object en = p.get("created_time_end");
		if (st != null && en != null && StringUtils.hasText(st.toString()) && StringUtils.hasText(en.toString())) {
			w.ge(Items::getCreated, toInt(st)).le(Items::getCreated, toInt(en));
		}
	}

	private static void likeIfPresent(LambdaQueryWrapper<Items> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<Items, ?> col) {
		if (!p.containsKey(key) || !StringUtils.hasText(str(p.get(key)))) {
			return;
		}
		String v = str(p.get(key)).trim();
		w.like(col, v);
	}

	private static void eqStrIfPresent(LambdaQueryWrapper<Items> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<Items, String> col) {
		if (!p.containsKey(key) || !StringUtils.hasText(str(p.get(key)))) {
			return;
		}
		w.eq(col, str(p.get(key)));
	}

	private static void putEqInt(LambdaQueryWrapper<Items> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<Items, Integer> col) {
		if (!p.containsKey(key) || p.get(key) == null) {
			return;
		}
		w.eq(col, toInt(p.get(key)));
	}

	private static void putEqIntObj(LambdaQueryWrapper<Items> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<Items, Integer> col) {
		if (!p.containsKey(key) || p.get(key) == null || !StringUtils.hasText(str(p.get(key)))) {
			return;
		}
		w.eq(col, toInt(p.get(key)));
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

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
	}

	/**
	 * 统计普通商品 SKU：{@code company_id} + {@code item_type=normal}；{@code defaultItemIds} 非空时增加
	 * {@code default_item_id IN (...)}（与列表 OR 条件区分）。
	 */
	public long countSkusNormalByCompanyAndDefaultItemIds(long companyId, Collection<Long> defaultItemIds) {
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemType, "normal");
		if (defaultItemIds != null && !defaultItemIds.isEmpty()) {
			w.in(Items::getDefaultItemId, defaultItemIds);
		}
		return mapper.selectCount(w);
	}

	/**
	 * 分页查询普通商品 SKU，按 {@code item_id ASC}。
	 */
	public List<Items> selectSkusNormalPageAsc(long companyId, Collection<Long> defaultItemIds, int offset, int limit) {
		LambdaQueryWrapper<Items> w = Wrappers.lambdaQuery();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemType, "normal");
		if (defaultItemIds != null && !defaultItemIds.isEmpty()) {
			w.in(Items::getDefaultItemId, defaultItemIds);
		}
		w.orderByAsc(Items::getItemId);
		w.last("LIMIT " + offset + "," + limit);
		return mapper.selectList(w);
	}
}
