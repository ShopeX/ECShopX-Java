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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.core.domain.PageResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.dto.CommunityItemsListQuery;
import cn.shopex.ecshopx.community.repository.CommunityItemsListJoinParams;
import cn.shopex.ecshopx.community.repository.CommunityItemsListRepository;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class CommunityItemsListService {

	private final CommunityItemsListRepository communityItemsListRepository;
	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsRepository itemsTagsRepository;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ObjectMapper objectMapper;

	public CommunityItemsListService(
			CommunityItemsListRepository communityItemsListRepository,
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsTagsRepository itemsTagsRepository,
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsRelCatsRepository itemsRelCatsRepository,
			ObjectMapper objectMapper) {
		this.communityItemsListRepository = communityItemsListRepository;
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.objectMapper = objectMapper;
	}

	public PageResult<Map<String, Object>> getItemsList(CommunityItemsListQuery q) {
		int page = q.getPage() < 1 ? 1 : q.getPage();
		int pageSize = q.getPageSize();
		if (pageSize > 2000) {
			pageSize = 2000;
		}
		if (pageSize <= 0) {
			pageSize = 10;
		}

		List<Long> itemIdOrDefault = null;

		if (StringUtils.hasText(q.getItemBn())) {
			List<Long> ids = itemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(q.getCompanyId(), q.getItemBn());
			if (ids.isEmpty()) {
				return PageResult.of(0L, List.of());
			}
			itemIdOrDefault = new ArrayList<>(new LinkedHashSet<>(ids));
		}

		if (StringUtils.hasText(q.getBarcode())) {
			List<Long> bc = itemsListQueryRepository.listDefaultItemIdsByBarcodeExact(q.getCompanyId(), q.getBarcode());
			if (bc.isEmpty()) {
				return PageResult.of(0L, List.of());
			}
			itemIdOrDefault = narrowItemIds(itemIdOrDefault, bc);
			if (itemIdOrDefault.isEmpty()) {
				return PageResult.of(0L, List.of());
			}
		}

		if (StringUtils.hasText(q.getCategoryRaw()) && !"0".equals(q.getCategoryRaw().trim())) {
			Long catId = parsePositiveLongLoose(q.getCategoryRaw().trim());
			if (catId == null) {
				return PageResult.of(0L, List.of());
			}
			List<Long> catItemIds = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(q.getCompanyId(), catId);
			if (catItemIds.isEmpty()) {
				return PageResult.of(0L, List.of());
			}
			itemIdOrDefault = narrowItemIds(itemIdOrDefault, catItemIds);
			if (itemIdOrDefault.isEmpty()) {
				return PageResult.of(0L, List.of());
			}
		}

		String itemNameContains = null;
		if (StringUtils.hasText(q.getItemName())) {
			itemNameContains = q.getItemName().trim();
		} else if (StringUtils.hasText(q.getKeywords())) {
			itemNameContains = q.getKeywords().trim();
		}

		String auditStatus = null;
		String approveStatus = null;
		boolean explicitAudit = StringUtils.hasText(q.getAuditStatus());
		if (explicitAudit) {
			auditStatus = q.getAuditStatus().trim();
		}
		if (StringUtils.hasText(q.getApproveStatus())) {
			String a = q.getApproveStatus().trim();
			if ("processing".equals(a) || "rejected".equals(a)) {
				if (!explicitAudit) {
					auditStatus = a;
				}
			} else {
				approveStatus = a;
			}
		}

		Integer brandId = null;
		if (StringUtils.hasText(q.getBrandIdRaw())) {
			try {
				int b = Integer.parseInt(q.getBrandIdRaw().trim());
				if (b != 0) {
					brandId = b;
				}
			} catch (NumberFormatException ignored) {
				// skip invalid brand filter
			}
		}

		boolean useActivityJoin = (q.isInActivityParameterPresent() && q.isInActivity())
				|| (q.getNormalizedActivityId() != null && q.getNormalizedActivityId() > 0);
		boolean applyActivityTimeWindow = q.isInActivityParameterPresent();
		Long activityIdEq = (q.getNormalizedActivityId() != null && q.getNormalizedActivityId() > 0)
				? q.getNormalizedActivityId()
				: null;

		int nowSec = (int) (System.currentTimeMillis() / 1000L);

		CommunityItemsListJoinParams.CommunityItemsListJoinParamsBuilder joinBuilder =
				CommunityItemsListJoinParams.builder().companyId(q.getCompanyId());
		if (q.getDistributorIds() != null && !q.getDistributorIds().isEmpty()) {
			joinBuilder.distributorIds(q.getDistributorIds()).distributorId(0);
		} else {
			joinBuilder.distributorIds(null).distributorId(q.getDistributorId());
		}
		CommunityItemsListJoinParams joinParams = joinBuilder
				.itemNameContains(itemNameContains)
				.itemIdOrDefaultIds(itemIdOrDefault)
				.auditStatus(auditStatus)
				.approveStatus(approveStatus)
				.brandId(brandId)
				.useActivityJoin(useActivityJoin)
				.applyActivityTimeWindow(applyActivityTimeWindow)
				.activityIdEq(activityIdEq)
				.nowEpochSec(nowSec)
				.build();

		PageResult<Map<String, Object>> pageResult = communityItemsListRepository.joinItemsList(joinParams, page, pageSize);
		List<Map<String, Object>> list = pageResult.getList();
		if (list == null) {
			list = List.of();
		} else {
			list = new ArrayList<>(list);
		}

		Set<Long> itemIdsForCats = list.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, List<Long>> catsByItem = loadItemCatIds(q.getCompanyId(), itemIdsForCats);

		for (Map<String, Object> row : list) {
			Object ic = row.get("item_category");
			row.put("item_main_cat_id", ic != null ? ic.toString() : "");
			long iid = toLong(row.get("item_id"));
			row.put("item_cat_id", catsByItem.getOrDefault(iid, List.of()));
			row.put("nospec", toNospecBoolean(row.get("nospec")));
			row.put("pics", parsePics(row.get("pics")));
			Object communitySort = row.remove("communityItemSort");
			if (communitySort == null) {
				communitySort = row.remove("community_item_sort");
			}
			row.put("sort", communitySort != null ? communitySort : 0);
		}

		itemsListQueryRepository.dealListStore(q.getCompanyId(), list);

		if (!list.isEmpty()) {
			enrichTagsAndCategoryNames(q.getCompanyId(), list);
		}

		return PageResult.of(pageResult.getTotal(), list);
	}

	public void assertNotInActiveActivityForDelete(long companyId, int distributorId, long goodsId) {
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		CommunityItemsListJoinParams joinParams = CommunityItemsListJoinParams.builder()
				.companyId(companyId)
				.distributorId(distributorId)
				.itemNameContains(null)
				.itemIdOrDefaultIds(null)
				.auditStatus(null)
				.approveStatus(null)
				.brandId(null)
				.useActivityJoin(true)
				.applyActivityTimeWindow(true)
				.activityIdEq(null)
				.goodsIdEq(goodsId)
				.nowEpochSec(nowSec)
				.build();
		PageResult<Map<String, Object>> pageResult = communityItemsListRepository.joinItemsList(joinParams, 1, 1);
		if (pageResult.getTotal() > 0) {
			throw new ResourceException("活动中的商品不能删除");
		}
	}

	private void enrichTagsAndCategoryNames(long companyId, List<Map<String, Object>> rows) {
		Set<Long> itemIds = rows.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, List<Map<String, Object>>> tagListByItem = buildTagLists(companyId, itemIds);

		Map<Long, List<String>> bracketedByItem = new HashMap<>();
		List<ItemsRelCats> rels = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds);
		Map<Long, Set<Long>> itemToCats = new HashMap<>();
		for (ItemsRelCats rc : rels) {
			itemToCats.computeIfAbsent(rc.getItemId(), k -> new LinkedHashSet<>()).add(rc.getCategoryId());
		}
		for (Map.Entry<Long, Set<Long>> e : itemToCats.entrySet()) {
			List<String> bracketed = new ArrayList<>();
			for (Long cid : e.getValue()) {
				itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, cid).map(ItemsCategory::getCategoryName)
						.ifPresent(name -> bracketed.add("[" + name + "]"));
			}
			bracketedByItem.put(e.getKey(), bracketed);
		}

		Map<Long, String> mainCatNames = new HashMap<>();
		for (Map<String, Object> r : rows) {
			long itemId = toLong(r.get("item_id"));
			r.put("tagList", tagListByItem.getOrDefault(itemId, List.of()));

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

			r.put("itemCatName", bracketedByItem.getOrDefault(itemId, List.of()));
		}
	}

	private Map<Long, List<Long>> loadItemCatIds(long companyId, Set<Long> itemIds) {
		Map<Long, List<Long>> out = new HashMap<>();
		if (itemIds.isEmpty()) {
			return out;
		}
		List<ItemsRelCats> rels = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds);
		for (ItemsRelCats rc : rels) {
			out.computeIfAbsent(rc.getItemId(), k -> new ArrayList<>()).add(rc.getCategoryId());
		}
		for (Map.Entry<Long, List<Long>> e : out.entrySet()) {
			e.setValue(e.getValue().stream().distinct().collect(Collectors.toCollection(ArrayList::new)));
		}
		return out;
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

	private static List<Long> narrowItemIds(List<Long> current, List<Long> incoming) {
		if (incoming == null || incoming.isEmpty()) {
			return List.of();
		}
		if (current == null) {
			return new ArrayList<>(new LinkedHashSet<>(incoming));
		}
		Set<Long> in = new LinkedHashSet<>(incoming);
		List<Long> out = new ArrayList<>();
		for (Long id : current) {
			if (id != null && in.contains(id)) {
				out.add(id);
			}
		}
		return out.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
	}

	private Object parsePics(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return objectMapper.readValue(s, new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean toNospecBoolean(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		String t = raw.toString().trim();
		return "true".equalsIgnoreCase(t) || "1".equals(t);
	}

	private static Long parsePositiveLongLoose(String s) {
		try {
			long v = Long.parseLong(s.trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
