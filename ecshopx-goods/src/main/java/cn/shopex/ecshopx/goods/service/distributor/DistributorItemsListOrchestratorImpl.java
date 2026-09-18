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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorItemsListOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.supplier.repository.SupplierOperatorQueryRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorItemsListOrchestratorImpl implements DistributorItemsListOrchestrator {

	private final DistributorItemsListItemFilterBuilder itemFilterBuilder;
	private final DistributorItemsRelListCoreService relListCore;
	private final ItemStoreService itemStoreService;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsRepository itemsTagsRepository;
	private final SupplierOperatorQueryRepository supplierOperatorQueryRepository;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final DistributorListQueryService distributorListQueryService;

	public DistributorItemsListOrchestratorImpl(
			DistributorItemsListItemFilterBuilder itemFilterBuilder,
			DistributorItemsRelListCoreService relListCore,
			ItemStoreService itemStoreService,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsTagsRepository itemsTagsRepository,
			SupplierOperatorQueryRepository supplierOperatorQueryRepository,
			ItemsCategoryRepository itemsCategoryRepository,
			DistributorListQueryService distributorListQueryService) {
		this.itemFilterBuilder = itemFilterBuilder;
		this.relListCore = relListCore;
		this.itemStoreService = itemStoreService;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
		this.supplierOperatorQueryRepository = supplierOperatorQueryRepository;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.distributorListQueryService = distributorListQueryService;
	}

	@Override
	public Map<String, Object> list(HttpServletRequest request, Map<String, Object> operatorJwt, Map<String, Object> merged) {
		long companyId = longVal(operatorJwt.get("company_id"));
		Optional<Map<String, Object>> filterOpt = itemFilterBuilder.build(companyId, merged);
		if (filterOpt.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", List.of());
			empty.put("total_count", 0);
			return empty;
		}

		Map<String, Object> filterBase = filterOpt.get();
		long distributorId = longVal(filterBase.get("distributor_id"));
		int warningStore = itemStoreService.getDistributorWarningStore(companyId, distributorId);

		int pageSize = -1;
		Object ps = merged.get("pageSize");
		if (ps != null && StringUtils.hasText(ps.toString())) {
			try {
				pageSize = Integer.parseInt(ps.toString().trim());
			} catch (NumberFormatException ignored) {
				pageSize = -1;
			}
		}
		int page = 1;
		Object pg = merged.get("page");
		if (pg != null && StringUtils.hasText(pg.toString())) {
			try {
				page = Integer.parseInt(pg.toString().trim());
			} catch (NumberFormatException ignored) {
				page = 1;
			}
		}
		if (page < 1) {
			page = 1;
		}

		Map<String, Object> filterForResponse = new LinkedHashMap<>();
		Map<String, Object> coreOut =
				relListCore.query(request, companyId, distributorId, filterBase, pageSize, page, filterForResponse);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) coreOut.get("list");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list != null ? list : List.of());
		data.put("total_count", coreOut.get("total_count"));
		data.put("warning_store", warningStore);
		data.put("filter", new LinkedHashMap<>(filterForResponse));

		if (list == null || list.isEmpty()) {
			return data;
		}

		enrichRows(companyId, list);
		return data;
	}

	private void enrichRows(long companyId, List<Map<String, Object>> rows) {
		Set<Long> itemIds = rows.stream().map(r -> longVal(r.get("item_id"))).filter(id -> id > 0).collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, List<Map<String, Object>>> newTags = buildTagLists(companyId, itemIds);

		Set<Long> supOpIds = new LinkedHashSet<>();
		Set<Long> distIds = new LinkedHashSet<>();
		Set<Long> allCategoryIds = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			long sid = longVal(r.get("supplier_id"));
			if (sid > 0) {
				supOpIds.add(sid);
			}
			long did = longVal(r.get("distributor_id"));
			if (did > 0) {
				distIds.add(did);
			}
			Object mid = r.get("item_main_cat_id");
			long mainCat = parseCatId(mid);
			if (mainCat > 0) {
				allCategoryIds.add(mainCat);
			}
			@SuppressWarnings("unchecked")
			List<Object> catObjs = (List<Object>) r.get("item_cat_id");
			if (catObjs != null) {
				for (Object c : catObjs) {
					long cid = longVal(c);
					if (cid > 0) {
						allCategoryIds.add(cid);
					}
				}
			}
		}

		Map<Long, String> supNames = supplierOperatorQueryRepository.mapSupplierNameByOperatorIds(companyId, supOpIds);
		Map<Long, String> distNames = new HashMap<>();
		if (!distIds.isEmpty()) {
			for (Distributor d : distributorListQueryService.listByIdsAndCompany(companyId, new ArrayList<>(distIds))) {
				distNames.put(d.getDistributorId(), d.getName() != null ? d.getName() : "");
			}
		}

		Map<Long, String> idToName = new HashMap<>();
		if (!allCategoryIds.isEmpty()) {
			for (ItemsCategory c : itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, allCategoryIds)) {
				if (c.getCategoryId() != null && StringUtils.hasText(c.getCategoryName())) {
					idToName.put(c.getCategoryId(), c.getCategoryName());
				}
			}
		}

		for (Map<String, Object> r : rows) {
			long itemId = longVal(r.get("item_id"));
			r.put("tagList", newTags.getOrDefault(itemId, List.of()));

			long sid = longVal(r.get("supplier_id"));
			long ownerDid = r.containsKey("item_owner_distributor_id")
					? longVal(r.get("item_owner_distributor_id"))
					: longVal(r.get("distributor_id"));
			if (sid > 0) {
				r.put("item_holder", "supplier");
			} else if (ownerDid > 0) {
				r.put("item_holder", "distributor");
			} else {
				r.put("item_holder", "self");
			}
			r.put("supplier_name", supNames.getOrDefault(sid, ""));

			Integer price = intOrNull(r.get("price"));
			Integer cost = intOrNull(r.get("cost_price"));
			r.put("gross_profit_rate", ItemsListQueryRepository.listRowGrossProfitRate(price, cost));
			r.put("is_default", truthy(r.get("is_default")) ? 1 : 0);

			Object mid = r.get("item_main_cat_id");
			long mainCat = parseCatId(mid);
			r.put("itemMainCatName", mainCat > 0 ? idToName.getOrDefault(mainCat, "") : "");

			@SuppressWarnings("unchecked")
			List<Object> catObjs = (List<Object>) r.get("item_cat_id");
			List<String> itemCatName = new ArrayList<>();
			if (catObjs != null) {
				for (Object c : catObjs) {
					long cid = longVal(c);
					String nm = idToName.get(cid);
					if (nm != null) {
						itemCatName.add("[" + nm + "]");
					}
				}
			}
			r.put("itemCatName", itemCatName);

			long did = longVal(r.get("distributor_id"));
			r.put("distributor_name", distNames.getOrDefault(did, ""));
		}
	}

	private Map<Long, List<Map<String, Object>>> buildTagLists(long companyId, Set<Long> itemIds) {
		Map<Long, List<Map<String, Object>>> map = new HashMap<>();
		if (itemIds.isEmpty()) {
			return map;
		}
		List<ItemsRelTags> rels = itemsRelTagsRepository.listRowsByCompanyIdAndItemIdIn(companyId, itemIds);
		Set<Long> tagIds = rels.stream().map(ItemsRelTags::getTagId).filter(Objects::nonNull).collect(Collectors.toSet());
		Map<Long, ItemsTags> tagById = new HashMap<>();
		for (Long tid : tagIds) {
			ItemsTags t = itemsTagsRepository.selectById(tid);
			if (t != null) {
				tagById.put(tid, t);
			}
		}
		for (ItemsRelTags rt : rels) {
			Long iid = rt.getItemId();
			if (iid == null) {
				continue;
			}
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("item_id", iid);
			ItemsTags tg = tagById.get(rt.getTagId());
			if (tg != null && tg.getCompanyId() != null) {
				one.put("company_id", tg.getCompanyId());
			} else if (rt.getCompanyId() != null) {
				one.put("company_id", rt.getCompanyId());
			} else {
				one.put("company_id", companyId);
			}
			one.put("tag_id", rt.getTagId());
			if (tg != null) {
				one.put("tag_name", tg.getTagName() != null ? tg.getTagName() : "");
				one.put("tag_color", tg.getTagColor() != null ? tg.getTagColor() : "");
				one.put("font_color", tg.getFontColor() != null ? tg.getFontColor() : "");
				one.put("description", tg.getDescription());
				Long dist = tg.getDistributorId();
				one.put("distributor_id", dist != null ? dist : 0L);
				one.put("front_show", tg.getFrontShow() != null ? tg.getFrontShow() : 0);
				one.put("tag_icon", tg.getTagIcon());
				one.put("created", tg.getCreated());
				one.put("updated", tg.getUpdated());
			} else {
				one.put("tag_name", "");
				one.put("tag_color", "");
				one.put("font_color", "");
				one.put("description", null);
				one.put("distributor_id", 0L);
				one.put("front_show", 0);
				one.put("tag_icon", null);
				one.put("created", null);
				one.put("updated", null);
			}
			map.computeIfAbsent(iid, k -> new ArrayList<>()).add(one);
		}
		return map;
	}

	private static long parseCatId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean truthy(Object o) {
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		return o != null && ("true".equalsIgnoreCase(o.toString().trim()) || "1".equals(o.toString().trim()));
	}

	private static Integer intOrNull(Object o) {
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

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
