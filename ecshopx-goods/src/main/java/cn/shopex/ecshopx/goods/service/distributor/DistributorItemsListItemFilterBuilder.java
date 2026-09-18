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

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.supplier.repository.SupplierOperatorQueryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DistributorItemsListItemFilterBuilder {

	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final SupplierOperatorQueryRepository supplierOperatorQueryRepository;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemStoreService itemStoreService;
	private final ItemsListQueryRepository itemsListQueryRepository;

	public DistributorItemsListItemFilterBuilder(
			ItemsRelTagsRepository itemsRelTagsRepository,
			SupplierOperatorQueryRepository supplierOperatorQueryRepository,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			ItemStoreService itemStoreService,
			ItemsListQueryRepository itemsListQueryRepository) {
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.supplierOperatorQueryRepository = supplierOperatorQueryRepository;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemStoreService = itemStoreService;
		this.itemsListQueryRepository = itemsListQueryRepository;
	}

	/**
	 * Builds query parameters for the distributor item list.
	 * Empty result means no matching scope: callers should return an empty list and zero total without error.
	 */
	public Optional<Map<String, Object>> build(long companyId, Map<String, Object> merged) {
		long distributorId = longVal(merged.get("distributor_id"));
		if (distributorId <= 0) {
			return Optional.empty();
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("distributor_id", distributorId);
		filter.put("item_type", "normal");

		List<Long> tagIds = parseTagIds(merged.get("tag_id"));
		if (!tagIds.isEmpty()) {
			List<Long> tagItemIds = itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIds(companyId, tagIds);
			if (tagItemIds.isEmpty()) {
				return Optional.empty();
			}
			Object itemIdParam = merged.get("item_id");
			if (itemIdParam != null && StringUtils.hasText(itemIdParam.toString())) {
				List<Long> req = parseIdList(str(itemIdParam));
				tagItemIds = tagItemIds.stream().filter(req::contains).collect(Collectors.toList());
				if (tagItemIds.isEmpty()) {
					return Optional.empty();
				}
			}
			filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, tagItemIds);
		}

		String isSku = str(merged.get("is_sku"));
		if (!StringUtils.hasText(isSku) || "false".equalsIgnoreCase(isSku)) {
			filter.put("is_default_true", Boolean.TRUE);
		} else if ("true".equalsIgnoreCase(isSku) || "1".equals(isSku.trim())) {
			filter.put("is_sku", Boolean.TRUE);
		}

		String keywords = str(merged.get("keywords"));
		if (StringUtils.hasText(keywords)) {
			filter.put("item_name", keywords.trim());
		}

		applyItemSourceFilter(filter, merged);

		String supplierName = str(merged.get("supplier_name")).trim();
		if (StringUtils.hasText(supplierName) && !isPlatformItemSource(merged)) {
			List<Long> sids = supplierOperatorQueryRepository.listOperatorIdsBySupplierNameLike(companyId, supplierName);
			if (sids.isEmpty()) {
				return Optional.empty();
			}
			filter.put(ItemsListQueryRepository.KEY_SUPPLIER_ID_IN, sids.stream().map(Long::intValue).collect(Collectors.toList()));
		}

		String goodsBn = str(merged.get("goods_bn")).trim();
		if (StringUtils.hasText(goodsBn)) {
			filter.put("goods_bn", goodsBn);
		}

		String itemBn = str(merged.get("item_bn")).trim();
		if (StringUtils.hasText(itemBn)) {
			List<Long> defIds = itemsListQueryRepository.listDefaultItemIdsByItemBnExact(companyId, itemBn);
			if (defIds.isEmpty()) {
				return Optional.empty();
			}
			if (!putOrIntersectIds(filter, defIds)) {
				return Optional.empty();
			}
		}

		String barcode = str(merged.get("barcode"));
		if (StringUtils.hasText(barcode)) {
			filter.put("barcode", barcode.trim());
		}

		int warningStore = itemStoreService.getDistributorWarningStore(companyId, distributorId);
		if ("true".equalsIgnoreCase(str(merged.get("is_warning")))) {
			filter.put(ItemsListQueryRepository.KEY_STORE_LTE_WARNING, warningStore);
		}

		Object itemIdQ = merged.get("item_id");
		if (itemIdQ != null && StringUtils.hasText(itemIdQ.toString())) {
			List<Long> ids = parseIdList(str(itemIdQ));
			if (!ids.isEmpty()) {
				filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(ids));
			}
		}

		String isCanSale = str(merged.get("is_can_sale"));
		if ("false".equalsIgnoreCase(isCanSale)) {
			filter.put("__dist_is_can_sale_filter", Boolean.FALSE);
		} else if ("true".equalsIgnoreCase(isCanSale)) {
			filter.put("__dist_is_can_sale_filter", Boolean.TRUE);
		}

		long storeGt = longVal(merged.get("store_gt"));
		if (storeGt > 0) {
			filter.put(ItemsListQueryRepository.KEY_STORE_GT, (int) storeGt);
		}
		long storeLt = longVal(merged.get("store_lt"));
		if (storeLt > 0) {
			filter.put(ItemsListQueryRepository.KEY_STORE_LT, (int) storeLt);
		}

		long brandId = longVal(merged.get("brand_id"));
		if (brandId > 0) {
			filter.put("brand_id", brandId);
		}

		String approveStatus = str(merged.get("approve_status"));
		if (StringUtils.hasText(approveStatus)) {
			if ("rejected".equals(approveStatus) || "processing".equals(approveStatus)) {
				filter.put("audit_status", approveStatus);
			} else {
				filter.put("approve_status", approveStatus);
			}
		}

		Object ig = merged.get("is_gift");
		if (ig != null) {
			filter.put("is_gift", truthy(ig) ? 1 : 0);
		}

		Object templatesId = merged.get("templates_id");
		if (templatesId != null && StringUtils.hasText(str(templatesId))) {
			filter.put("templates_id", templatesId);
		}

		List<Long> categoryIds = pickCategoryIds(merged.get("category"));
		if (categoryIds != null) {
			long catLeaf = categoryIds.get(categoryIds.size() - 1);
			List<Long> itemIds = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, catLeaf);
			if (itemIds.isEmpty()) {
				return Optional.empty();
			}
			if (!intersectItemIds(filter, itemIds)) {
				return Optional.empty();
			}
		}

		List<Long> catIdList = pickCategoryIds(merged.get("cat_id"));
		if (catIdList != null) {
			long catLeaf = catIdList.get(catIdList.size() - 1);
			List<Long> itemIds = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, catLeaf);
			if (itemIds.isEmpty()) {
				return Optional.empty();
			}
			if (!intersectItemIds(filter, itemIds)) {
				return Optional.empty();
			}
		}

		Object mainCat = merged.get("main_cat_id");
		if (mainCat != null) {
			List<Long> mainIds = flattenMainCatIds(mainCat);
			if (!mainIds.isEmpty()) {
				log.info("distributorItems filter main_cat_id =====> {}", mainIds);
				List<String> catKeys = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, mainIds);
				if (!catKeys.isEmpty()) {
					filter.put(ItemsListQueryRepository.KEY_ITEM_CATEGORY_IN, catKeys);
				}
			}
		}

		filter.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		return Optional.of(filter);
	}

	/**
	 * 与 {@code /goods/items} 商品类型一致：{@code item_holder} 优先映射为 {@code item_source}。
	 * {@code supplier} → 供应商商品；{@code distributor} → 商户商品（supplier_id=0 且 items.distributor_id>0）；
	 * {@code self}/{@code platform} → 自营（supplier_id=0 且 items.distributor_id=0）。
	 */
	private static void applyItemSourceFilter(Map<String, Object> filter, Map<String, Object> merged) {
		String itemSource = resolveListItemSource(merged);
		if (!StringUtils.hasText(itemSource)) {
			return;
		}
		String src = itemSource.trim();
		if ("supplier".equalsIgnoreCase(src)) {
			filter.put(ItemsListQueryRepository.KEY_ITEM_SOURCE_SUPPLIER_MODE, Boolean.TRUE);
			return;
		}
		filter.put(ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE, Boolean.TRUE);
		if ("distributor".equalsIgnoreCase(src)) {
			filter.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_GT0_ONLY, Boolean.TRUE);
		} else {
			filter.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, 0);
		}
	}

	static String resolveListItemSource(Map<String, Object> merged) {
		String itemHolder = str(merged.get("item_holder")).trim();
		if (StringUtils.hasText(itemHolder)) {
			if ("supplier".equalsIgnoreCase(itemHolder)) {
				return "supplier";
			}
			if ("distributor".equalsIgnoreCase(itemHolder)) {
				return "distributor";
			}
			if ("self".equalsIgnoreCase(itemHolder) || "platform".equalsIgnoreCase(itemHolder)) {
				return "platform";
			}
		}
		return str(merged.get("item_source")).trim();
	}

	private static boolean isPlatformItemSource(Map<String, Object> merged) {
		String itemSource = resolveListItemSource(merged);
		return StringUtils.hasText(itemSource) && !"supplier".equalsIgnoreCase(itemSource);
	}

	private static boolean putOrIntersectIds(Map<String, Object> filter, List<Long> defIds) {
		@SuppressWarnings("unchecked")
		List<Long> existing = (List<Long>) filter.get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		if (existing == null || existing.isEmpty()) {
			filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(defIds));
			return true;
		}
		List<Long> inter = existing.stream().filter(defIds::contains).collect(Collectors.toList());
		if (inter.isEmpty()) {
			return false;
		}
		filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, inter);
		return true;
	}

	private boolean intersectItemIds(Map<String, Object> filter, List<Long> itemIds) {
		@SuppressWarnings("unchecked")
		List<Long> existing = (List<Long>) filter.get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		if (existing == null || existing.isEmpty()) {
			filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(itemIds));
			return true;
		}
		List<Long> inter = existing.stream().filter(itemIds::contains).collect(Collectors.toList());
		if (inter.isEmpty()) {
			return false;
		}
		filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, inter);
		return true;
	}

	/** Uses the last element when the request sends a category path array; a scalar is treated as one id. */
	private static List<Long> pickCategoryIds(Object raw) {
		if (raw == null) {
			return null;
		}
		long categoryId;
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return null;
			}
			Object last = list.get(list.size() - 1);
			categoryId = longVal(last);
		} else {
			categoryId = longVal(raw);
		}
		return categoryId > 0 ? List.of(categoryId) : null;
	}

	private static List<Long> flattenMainCatIds(Object raw) {
		List<Long> out = new ArrayList<>();
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return out;
			}
			Object last = list.get(list.size() - 1);
			if (last instanceof List<?> inner) {
				for (Object o : inner) {
					out.add(longVal(o));
				}
			} else {
				out.add(longVal(last));
			}
		} else if (raw != null) {
			out.add(longVal(raw));
		}
		return out;
	}

	private static List<Long> parseTagIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return List.of();
			}
			for (Object o : list) {
				out.add(longVal(o));
			}
			return out;
		}
		if (!StringUtils.hasText(str(raw))) {
			return List.of();
		}
		out.add(longVal(raw));
		return out;
	}

	private static List<Long> parseIdList(String s) {
		List<Long> out = new ArrayList<>();
		if (!StringUtils.hasText(s)) {
			return out;
		}
		for (String part : s.split(",")) {
			String t = part.trim();
			if (StringUtils.hasText(t)) {
				out.add(Long.parseLong(t));
			}
		}
		return out;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(s);
	}

	private static boolean truthy(Object o) {
		if (o instanceof Boolean b) {
			return b;
		}
		String s = str(o).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}
}
