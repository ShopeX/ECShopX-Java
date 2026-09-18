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

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCommissionQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.supplier.repository.SupplierOperatorQueryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class GoodsItemsListEnrichmentService {

	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsRepository itemsTagsRepository;
	private final OperatorsQueryService operatorsQueryService;
	private final SupplierOperatorQueryRepository supplierOperatorQueryRepository;
	private final ItemsCommissionQueryRepository itemsCommissionQueryRepository;
	private final DistributorListQueryService distributorListQueryService;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final ObjectMapper objectMapper;

	public GoodsItemsListEnrichmentService(ItemsRelTagsRepository itemsRelTagsRepository, ItemsTagsRepository itemsTagsRepository,
			OperatorsQueryService operatorsQueryService, SupplierOperatorQueryRepository supplierOperatorQueryRepository,
			ItemsCommissionQueryRepository itemsCommissionQueryRepository, DistributorListQueryService distributorListQueryService,
			ItemsCategoryRepository itemsCategoryRepository, ItemsRelCatsRepository itemsRelCatsRepository,
			ItemsAttributesRepository itemsAttributesRepository, GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			ObjectMapper objectMapper) {
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
		this.operatorsQueryService = operatorsQueryService;
		this.supplierOperatorQueryRepository = supplierOperatorQueryRepository;
		this.itemsCommissionQueryRepository = itemsCommissionQueryRepository;
		this.distributorListQueryService = distributorListQueryService;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.objectMapper = objectMapper;
	}

	public void applyActivityTagsMinimalNoOp(List<Map<String, Object>> rows) {
	}

	/** 单商品标签列表，与列表接口 {@link #enrichAll} 中 tagList 结构一致。 */
	public List<Map<String, Object>> buildTagListForSingleItem(long companyId, long itemId) {
		if (itemId <= 0) {
			return List.of();
		}
		return buildTagLists(companyId, Set.of(itemId)).getOrDefault(itemId, List.of());
	}

	public void enrichAll(long companyId, List<Map<String, Object>> rows, boolean supplierPath, List<Map<String, Object>> supplierCategoryJson) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		applyActivityTagsMinimalNoOp(rows);
		if (!supplierPath) {
			goodsItemsListPromotionEnrichmentService.enrich(rows);
		}

		Set<Long> itemIds = rows.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).collect(Collectors.toCollection(LinkedHashSet::new));

		Map<Long, List<Map<String, Object>>> tagListByItem = buildTagLists(companyId, itemIds);
		Set<Long> supOpIds = new LinkedHashSet<>();
		Set<Long> distIds = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			long sid = toLong(r.get("supplier_id"));
			if (sid > 0) {
				supOpIds.add(sid);
			}
			long did = toLong(r.get("distributor_id"));
			if (did > 0) {
				distIds.add(did);
			}
		}
		Map<Long, String> opNames = operatorsQueryService.mapUsernameByOperatorIds(companyId, supOpIds);
		Map<Long, String> supNames = supplierOperatorQueryRepository.mapSupplierNameByOperatorIds(companyId, supOpIds);
		Set<Long> goodsIds = rows.stream().map(r -> toLong(r.get("goods_id"))).filter(id -> id > 0).collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Number> commByGoods = itemsCommissionQueryRepository.mapCommissionRatioByGoodsIds(companyId, goodsIds);
		Map<Long, String> distNames = new HashMap<>();
		for (Distributor d : distributorListQueryService.listByIdsAndCompany(companyId, new ArrayList<>(distIds))) {
			distNames.put(d.getDistributorId(), d.getName() != null ? d.getName() : "");
		}

		Map<Long, String> mainCatNames = new HashMap<>();
		Map<Long, SaleCategoryDisplay> saleCatsByItem = buildSaleCategoryDisplay(companyId, itemIds);

		for (Map<String, Object> r : rows) {
			long itemId = toLong(r.get("item_id"));
			r.put("tagList", tagListByItem.getOrDefault(itemId, List.of()));
			long sid = toLong(r.get("supplier_id"));
			if (sid > 0) {
				r.put("operator_name", opNames.getOrDefault(sid, ""));
				r.put("supplier_name", supNames.getOrDefault(sid, ""));
			}
			long did = toLong(r.get("distributor_id"));
			r.put("distributor_name", List.of());
			if (did > 0) {
				String nm = distNames.getOrDefault(did, "");
				if (StringUtils.hasText(nm)) {
					Map<String, Object> drow = new LinkedHashMap<>();
					drow.put("name", nm);
					drow.put("distributor_id", String.valueOf(did));
					r.put("distributor_name", drow);
				}
			}
			long gid = toLong(r.get("goods_id"));
			Number cr = commByGoods.get(gid);
			r.put("commission_ratio", cr != null ? cr : 0);
			String ic = str(r.get("item_category"));
			if (StringUtils.hasText(ic)) {
				try {
					long mid = Long.parseLong(ic.trim());
					if (!mainCatNames.containsKey(mid)) {
						itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, mid).ifPresent(c -> mainCatNames.put(mid, c.getCategoryName()));
					}
					r.put("itemMainCatName", mainCatNames.getOrDefault(mid, ""));
				} catch (NumberFormatException e) {
					r.put("itemMainCatName", "");
				}
			} else {
				r.put("itemMainCatName", "");
			}
			if (!supplierPath) {
				SaleCategoryDisplay sc = saleCatsByItem.get(itemId);
				r.put("item_cat_id", sc != null ? sc.catIds() : List.of());
				r.put("itemCatName", sc != null ? sc.bracketedNames() : List.of());
			}
			int bid = (int) toLong(r.get("brand_id"));
			if (bid > 0) {
				ItemsAttributes brand = itemsAttributesRepository.selectByCompanyAndAttributeId(companyId, bid);
				if (brand != null) {
					if (!StringUtils.hasText(str(r.get("goods_brand")))) {
						r.put("goods_brand", brand.getAttributeName());
					}
				}
			}
		}

		if (supplierPath && supplierCategoryJson != null) {
			mergeSupplierCategoryJson(companyId, rows, supplierCategoryJson);
		}
	}

	private void mergeSupplierCategoryJson(long companyId, List<Map<String, Object>> rows, List<Map<String, Object>> meta) {
		Map<Long, String> byItem = new HashMap<>();
		for (Map<String, Object> m : meta) {
			Object i = m.get("item_id");
			Object j = m.get("category_json");
			if (i instanceof Number n && j != null) {
				byItem.put(n.longValue(), j.toString());
			}
		}
		for (Map<String, Object> r : rows) {
			long id = toLong(r.get("item_id"));
			String categoryJson = byItem.get(id);
			if (categoryJson == null) {
				continue;
			}
			List<Long> catIds = GoodsItemsListRowMapper.parseSupplierCategoryIds(categoryJson);
			r.put("item_cat_id", catIds);
			List<String> bracketed = new ArrayList<>();
			for (Long cid : catIds) {
				itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, cid).map(ItemsCategory::getCategoryName)
						.ifPresent(name -> bracketed.add("[" + name + "]"));
			}
			r.put("itemCatName", bracketed);
		}
	}

	private record SaleCategoryDisplay(List<Long> catIds, List<String> bracketedNames) {
	}

	private Map<Long, SaleCategoryDisplay> buildSaleCategoryDisplay(long companyId, Set<Long> itemIds) {
		Map<Long, SaleCategoryDisplay> out = new HashMap<>();
		List<ItemsRelCats> rels = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds);
		Map<Long, Set<Long>> itemToCats = new HashMap<>();
		for (ItemsRelCats rc : rels) {
			itemToCats.computeIfAbsent(rc.getItemId(), k -> new LinkedHashSet<>()).add(rc.getCategoryId());
		}
		for (Map.Entry<Long, Set<Long>> e : itemToCats.entrySet()) {
			List<Long> ids = new ArrayList<>(e.getValue());
			List<String> bracketed = new ArrayList<>();
			for (Long cid : e.getValue()) {
				itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, cid).map(ItemsCategory::getCategoryName).ifPresent(name -> bracketed.add("[" + name + "]"));
			}
			out.put(e.getKey(), new SaleCategoryDisplay(ids, bracketed));
		}
		return out;
	}

	private Map<Long, List<Map<String, Object>>> buildTagLists(long companyId, Set<Long> itemIds) {
		Map<Long, List<Map<String, Object>>> map = new HashMap<>();
		List<cn.shopex.ecshopx.goods.domain.ItemsRelTags> rels = itemsRelTagsRepository.listRowsByCompanyIdAndItemIdIn(companyId, itemIds);
		Set<Long> tagIds = rels.stream().map(cn.shopex.ecshopx.goods.domain.ItemsRelTags::getTagId).filter(Objects::nonNull).collect(Collectors.toSet());
		Map<Long, ItemsTags> tagById = new HashMap<>();
		for (Long tid : tagIds) {
			ItemsTags t = itemsTagsRepository.selectById(tid);
			if (t != null) {
				tagById.put(tid, t);
			}
		}
		for (cn.shopex.ecshopx.goods.domain.ItemsRelTags rt : rels) {
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
}
