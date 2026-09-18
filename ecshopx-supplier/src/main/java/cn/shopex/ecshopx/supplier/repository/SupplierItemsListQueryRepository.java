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

package cn.shopex.ecshopx.supplier.repository;

import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.mapper.SupplierItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * supplier_items 列表条件与 {@code ItemsListQueryRepository} 使用相同的 params 键语义（与平台 items 对齐的列）。
 */
@Repository
public class SupplierItemsListQueryRepository {

	private final SupplierItemsMapper mapper;

	public SupplierItemsListQueryRepository(SupplierItemsMapper mapper) {
		this.mapper = mapper;
	}

	public List<Long> mergeDefaultItemIdsByItemNameOrBrief(long companyId, String keywords) {
		if (!StringUtils.hasText(keywords)) {
			return List.of();
		}
		String kw = keywords.trim();
		Set<Long> ids = new LinkedHashSet<>();
		LambdaQueryWrapper<SupplierItems> w1 = Wrappers.lambdaQuery();
		w1.eq(SupplierItems::getCompanyId, companyId).like(SupplierItems::getItemName, kw);
		ids.addAll(distinctDefaultItemIds(w1));
		LambdaQueryWrapper<SupplierItems> w2 = Wrappers.lambdaQuery();
		w2.eq(SupplierItems::getCompanyId, companyId).like(SupplierItems::getBrief, kw);
		ids.addAll(distinctDefaultItemIds(w2));
		return new ArrayList<>(ids);
	}

	public List<Long> mergeDefaultItemIdsByItemBnOrBarcodeContains(long companyId, String itemBn) {
		if (!StringUtils.hasText(itemBn)) {
			return List.of();
		}
		String kw = itemBn.trim();
		Set<Long> ids = new LinkedHashSet<>();
		LambdaQueryWrapper<SupplierItems> w1 = Wrappers.lambdaQuery();
		w1.eq(SupplierItems::getCompanyId, companyId).like(SupplierItems::getItemBn, kw);
		ids.addAll(distinctDefaultItemIds(w1));
		LambdaQueryWrapper<SupplierItems> w2 = Wrappers.lambdaQuery();
		w2.eq(SupplierItems::getCompanyId, companyId).like(SupplierItems::getBarcode, kw);
		ids.addAll(distinctDefaultItemIds(w2));
		return new ArrayList<>(ids);
	}

	public List<Long> listDefaultItemIdsByBarcodeExact(long companyId, String barcode) {
		if (!StringUtils.hasText(barcode)) {
			return List.of();
		}
		LambdaQueryWrapper<SupplierItems> w = Wrappers.lambdaQuery();
		w.eq(SupplierItems::getCompanyId, companyId).eq(SupplierItems::getBarcode, barcode.trim());
		return distinctDefaultItemIds(w);
	}

	private List<Long> distinctDefaultItemIds(LambdaQueryWrapper<SupplierItems> w) {
		return mapper.selectList(w).stream().map(SupplierItems::getDefaultItemId).filter(Objects::nonNull).filter(id -> id > 0).distinct()
				.collect(Collectors.toList());
	}

	public long countByParams(Map<String, Object> p) {
		return mapper.selectCount(buildWrapper(p));
	}

	public List<SupplierItems> selectPageByParams(Map<String, Object> p, int offset, int limit) {
		LambdaQueryWrapper<SupplierItems> w = buildWrapper(p);
		applyStableListOrder(w);
		if (limit >= 0) {
			w.last("LIMIT " + offset + "," + limit);
		}
		return mapper.selectList(w);
	}

	public Map<Long, Map<String, Object>> mapWireNullableColumnsByItemIds(Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<SupplierItems> w = Wrappers.lambdaQuery();
		w.in(SupplierItems::getItemId, itemIds);
		w.select(SupplierItems::getItemId, SupplierItems::getSales, SupplierItems::getIsPoint, SupplierItems::getStartNum);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> row : mapper.selectMaps(w)) {
			Object id = row.get("item_id");
			if (id instanceof Number n) {
				out.put(n.longValue(), row);
			}
		}
		return out;
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
			r.put("store", 0);
			Object g = r.get("goods_id");
			if (g instanceof Number n) {
				Long gid = n.longValue();
				if (gid > 0 && sum.containsKey(gid)) {
					r.put("store", sum.get(gid));
				}
			}
		}
	}

	private Map<Long, Integer> sumStoreByGoodsIds(long companyId, Collection<Long> goodsIds) {
		Map<Long, Integer> sums = new LinkedHashMap<>();
		if (goodsIds == null || goodsIds.isEmpty()) {
			return sums;
		}
		for (Long gid : goodsIds) {
			if (gid == null || gid <= 0) {
				continue;
			}
			LambdaQueryWrapper<SupplierItems> w = Wrappers.lambdaQuery();
			w.eq(SupplierItems::getCompanyId, companyId).eq(SupplierItems::getGoodsId, gid);
			List<SupplierItems> skuRows = mapper.selectList(w);
			int total = 0;
			for (SupplierItems row : skuRows) {
				if (row.getStore() != null) {
					total += row.getStore();
				}
			}
			sums.put(gid, total);
		}
		return sums;
	}

	/**
	 * Same filters as list queries; returns distinct {@code default_item_id} values for SKU {@code item_bn} pre-filtering.
	 */
	public List<Long> listDistinctDefaultItemIdsByFullParams(Map<String, Object> p) {
		LambdaQueryWrapper<SupplierItems> w = buildWrapper(p);
		List<SupplierItems> rows = mapper.selectList(w);
		return rows.stream().map(SupplierItems::getDefaultItemId).filter(Objects::nonNull).filter(id -> id > 0).distinct().collect(Collectors.toList());
	}

	private LambdaQueryWrapper<SupplierItems> buildWrapper(Map<String, Object> p) {
		LambdaQueryWrapper<SupplierItems> w = Wrappers.lambdaQuery();
		long companyId = toLong(p.get("company_id"));
		w.eq(SupplierItems::getCompanyId, companyId);

		if (Boolean.TRUE.equals(p.get("item_source_supplier_mode"))) {
			w.ge(SupplierItems::getSupplierId, 1).eq(SupplierItems::getAuditStatus, "approved");
		} else if (Boolean.TRUE.equals(p.get("item_source_non_supplier_mode"))) {
			w.eq(SupplierItems::getSupplierId, 0);
		}

		putEqInt(w, p, "type", SupplierItems::getType);
		likeIfPresent(w, p, "item_name", SupplierItems::getItemName);
		rangeCreated(w, p);
		eqStrIfPresent(w, p, "consume_type", SupplierItems::getConsumeType);
		putEqIntObj(w, p, "templates_id", SupplierItems::getTemplatesId);
		if (p.containsKey("is_market") && p.get("is_market") != null && StringUtils.hasText(String.valueOf(p.get("is_market")))) {
			w.eq(SupplierItems::getIsMarket, toInt(p.get("is_market")));
		}
		eqStrIfPresent(w, p, "goods_bn", SupplierItems::getGoodsBn);
		eqStrIfPresent(w, p, "item_bn", SupplierItems::getItemBn);
		if (p.containsKey("regions_id") && StringUtils.hasText(str(p.get("regions_id")))) {
			w.eq(SupplierItems::getRegionsId, str(p.get("regions_id")));
		}
		if (p.containsKey("nospec")) {
			w.eq(SupplierItems::getNospec, str(p.get("nospec")));
		}
		if (p.containsKey("is_gift")) {
			Object ig = p.get("is_gift");
			boolean t = ig instanceof Number n ? n.intValue() == 1 : truthy(ig);
			w.eq(SupplierItems::getIsGift, t);
		}

		if (p.containsKey("supplier_id_eq")) {
			w.eq(SupplierItems::getSupplierId, toInt(p.get("supplier_id_eq")));
		} else if (p.get("supplier_id_in") instanceof List<?> l && !l.isEmpty()) {
			List<Integer> ints = l.stream().map(SupplierItemsListQueryRepository::toInt).collect(Collectors.toList());
			w.in(SupplierItems::getSupplierId, ints);
		}

		applyApproveAudit(w, p);
		if (p.containsKey("audit_status") && StringUtils.hasText(str(p.get("audit_status")))) {
			w.eq(SupplierItems::getAuditStatus, str(p.get("audit_status")));
		}
		if (p.containsKey("rebate") && p.get("rebate") != null) {
			w.eq(SupplierItems::getRebate, toInt(p.get("rebate")));
		}
		eqStrIfPresent(w, p, "rebate_type", SupplierItems::getRebateType);

		@SuppressWarnings("unchecked")
		List<String> catIn = (List<String>) p.get("item_category_in");
		if (catIn != null && !catIn.isEmpty()) {
			w.in(SupplierItems::getItemCategory, catIn);
		}

		@SuppressWarnings("unchecked")
		List<Long> idOrDef = (List<Long>) p.get("item_id_or_default_ids");
		if (idOrDef != null && !idOrDef.isEmpty()) {
			w.and(q -> q.in(SupplierItems::getItemId, idOrDef).or().in(SupplierItems::getDefaultItemId, idOrDef));
		}

		applyDistributor(w, p);
		applyDistributorApproveStatus(w, p);

		String ss = str(p.get("store_status"));
		if ("positive".equals(ss)) {
			w.gt(SupplierItems::getStore, 0);
		} else if ("zero".equals(ss)) {
			w.lt(SupplierItems::getStore, 1);
		}
		if (p.containsKey("store_gt")) {
			w.gt(SupplierItems::getStore, toInt(p.get("store_gt")));
		}
		if (p.containsKey("store_lt")) {
			w.lt(SupplierItems::getStore, toInt(p.get("store_lt")));
		}
		if (p.containsKey("price_gt_cents")) {
			w.gt(SupplierItems::getPrice, toInt(p.get("price_gt_cents")));
		}
		if (p.containsKey("price_lt_cents")) {
			w.lt(SupplierItems::getPrice, toInt(p.get("price_lt_cents")));
		}
		eqStrIfPresent(w, p, "special_type", SupplierItems::getSpecialType);
		if (p.containsKey("store_lte_warning")) {
			w.le(SupplierItems::getStore, toInt(p.get("store_lte_warning")));
		}
		if (p.containsKey("brand_id")) {
			int bid = toInt(p.get("brand_id"));
			if (bid != 0) {
				w.eq(SupplierItems::getBrandId, bid);
			}
		}
		if (p.containsKey("is_medicine")) {
			w.eq(SupplierItems::getIsMedicine, toInt(p.get("is_medicine")));
		}
		if (p.containsKey("is_prescription")) {
			w.eq(SupplierItems::getIsPrescription, toInt(p.get("is_prescription")));
		}
		if (Boolean.TRUE.equals(p.get("is_default_true"))) {
			w.eq(SupplierItems::getIsDefault, true);
		}
		if (p.containsKey("item_type") && StringUtils.hasText(str(p.get("item_type")))) {
			w.eq(SupplierItems::getItemType, str(p.get("item_type")).trim());
		}
		return w;
	}

	/**
	 * Stable ordering for admin list views (newer default groups and SKUs first).
	 */
	private static void applyStableListOrder(LambdaQueryWrapper<SupplierItems> w) {
		w.orderByDesc(SupplierItems::getDefaultItemId).orderByDesc(SupplierItems::getItemId);
	}

	private static void applyDistributorApproveStatus(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p) {
		String d = str(p.get("distributor_approve_status"));
		if (!StringUtils.hasText(d)) {
			return;
		}
		if ("onsale".equalsIgnoreCase(d)) {
			w.eq(SupplierItems::getApproveStatus, "onsale").eq(SupplierItems::getAuditStatus, "approved");
		} else if ("instock".equalsIgnoreCase(d)) {
			w.and(x -> x.ne(SupplierItems::getApproveStatus, "onsale").or().ne(SupplierItems::getAuditStatus, "approved"));
		}
	}

	private static void applyDistributor(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p) {
		if (Boolean.TRUE.equals(p.get("distributor_id_gt0_only"))) {
			w.gt(SupplierItems::getDistributorId, 0);
			return;
		}
		if (p.containsKey("distributor_id_eq")) {
			w.eq(SupplierItems::getDistributorId, toInt(p.get("distributor_id_eq")));
			return;
		}
		@SuppressWarnings("unchecked")
		List<Integer> din = (List<Integer>) p.get("distributor_id_in");
		if (din != null && !din.isEmpty()) {
			w.in(SupplierItems::getDistributorId, din);
		}
	}

	private static void applyApproveAudit(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p) {
		if (!p.containsKey("approve_status") || !StringUtils.hasText(str(p.get("approve_status")))) {
			return;
		}
		String a = str(p.get("approve_status"));
		if ("processing".equals(a) || "rejected".equals(a)) {
			w.eq(SupplierItems::getAuditStatus, a);
		} else {
			w.eq(SupplierItems::getApproveStatus, a);
		}
	}

	private static void rangeCreated(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p) {
		Object st = p.get("created_time_start");
		Object en = p.get("created_time_end");
		if (st != null && en != null && StringUtils.hasText(st.toString()) && StringUtils.hasText(en.toString())) {
			w.ge(SupplierItems::getCreated, toInt(st)).le(SupplierItems::getCreated, toInt(en));
		}
	}

	private static void likeIfPresent(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<SupplierItems, String> col) {
		if (!p.containsKey(key) || !StringUtils.hasText(str(p.get(key)))) {
			return;
		}
		w.like(col, str(p.get(key)).trim());
	}

	private static void eqStrIfPresent(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<SupplierItems, String> col) {
		if (!p.containsKey(key) || !StringUtils.hasText(str(p.get(key)))) {
			return;
		}
		w.eq(col, str(p.get(key)));
	}

	private static void putEqInt(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<SupplierItems, Integer> col) {
		if (!p.containsKey(key) || p.get(key) == null) {
			return;
		}
		w.eq(col, toInt(p.get(key)));
	}

	private static void putEqIntObj(LambdaQueryWrapper<SupplierItems> w, Map<String, Object> p, String key,
			com.baomidou.mybatisplus.core.toolkit.support.SFunction<SupplierItems, Integer> col) {
		if (!p.containsKey(key) || p.get(key) == null || !StringUtils.hasText(str(p.get(key)))) {
			return;
		}
		w.eq(col, toInt(p.get(key)));
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

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
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
