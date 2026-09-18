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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.dispatch.ItemTagEditEventDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.promotions.dto.ItemActivityCheckRow;
import cn.shopex.ecshopx.promotions.service.ItemsTagActivityCheckService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsTagsRelService {

	private final ItemsQueryRepository itemsQueryRepository;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagActivityCheckService itemsTagActivityCheckService;
	private final ItemTagEditEventDispatchPublisher itemTagEditEventDispatchPublisher;

	public ItemsTagsRelService(ItemsQueryRepository itemsQueryRepository, ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsTagActivityCheckService itemsTagActivityCheckService,
			ItemTagEditEventDispatchPublisher itemTagEditEventDispatchPublisher) {
		this.itemsQueryRepository = itemsQueryRepository;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagActivityCheckService = itemsTagActivityCheckService;
		this.itemTagEditEventDispatchPublisher = itemTagEditEventDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public void relateTags(long companyId, Map<String, Object> mergedInput) {
		List<Long> itemIds = parseIdList(mergedInput.get("item_ids"));
		List<Long> tagIds = parseIdList(mergedInput.get("tag_ids"));
		boolean itemIdsWasArray = isArrayLike(mergedInput.get("item_ids"));
		boolean tagIdsWasArray = isArrayLike(mergedInput.get("tag_ids"));

		if (itemIds.isEmpty()) {
			throw new BadRequestException("请选择商品");
		}

		if (!tagIds.isEmpty()) {
			AtomicReference<String> errRef = new AtomicReference<>("商品标签导致活动冲突");
			List<Items> items = itemsQueryRepository.listByCompanyIdAndItemIds(companyId, itemIds);
			if (!items.isEmpty()) {
				List<ItemActivityCheckRow> rows = toCheckRows(items, itemIds);
				boolean ok = itemsTagActivityCheckService.checkActivity(rows, tagIds, companyId, errRef);
				if (!ok) {
					String msg = errRef.get();
					if (msg == null || msg.isBlank()) {
						msg = "商品标签导致活动冲突";
					}
					throw new ResourceException(msg);
				}
			}
		}

		if (itemIdsWasArray && tagIdsWasArray) {
			createRelTags(companyId, itemIds, tagIds);
		} else if (!itemIdsWasArray) {
			if (itemIds.size() != 1) {
				throw new BadRequestException("参数格式错误");
			}
			createRelTagsByItemId(companyId, itemIds.get(0), tagIds);
		} else {
			if (!tagIdsWasArray && tagIds.size() > 1) {
				throw new BadRequestException("参数格式错误");
			}
			long singleTag = tagIds.isEmpty() ? 0L : tagIds.get(0);
			createRelTagsByTagId(companyId, itemIds, singleTag);
		}

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", companyId);
		itemTagEditEventDispatchPublisher.publish(entities);
	}

	private List<ItemActivityCheckRow> toCheckRows(List<Items> items, List<Long> requestItemIds) {
		List<Long> reqCopy = new ArrayList<>(requestItemIds);
		List<ItemActivityCheckRow> rows = new ArrayList<>();
		for (Items it : items) {
			ItemActivityCheckRow r = new ItemActivityCheckRow();
			r.setItemId(it.getItemId());
			r.setMainCatId(parseLongOrNull(it.getItemCategory()));
			r.setBrandId(it.getBrandId() != null ? it.getBrandId().longValue() : null);
			r.setAllRequestItemIds(reqCopy);
			rows.add(r);
		}
		return rows;
	}

	private static Long parseLongOrNull(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void createRelTags(long companyId, List<Long> itemIds, List<Long> tagIds) {
		if (itemIds.isEmpty()) {
			return;
		}
		Map<Long, Set<Long>> remaining = new HashMap<>();
		List<ItemsRelTags> existing = itemsRelTagsRepository.getLists(companyId, itemIds);
		for (ItemsRelTags rel : existing) {
			remaining.computeIfAbsent(rel.getItemId(), k -> new HashSet<>()).add(rel.getTagId());
		}

		for (long itemId : itemIds) {
			for (long tagId : tagIds) {
				Set<Long> set = remaining.get(itemId);
				if (set != null && set.contains(tagId)) {
					set.remove(tagId);
				} else {
					ItemsRelTags row = new ItemsRelTags();
					row.setCompanyId(companyId);
					row.setItemId(itemId);
					row.setTagId(tagId);
					itemsRelTagsRepository.create(row);
				}
			}
		}

		for (Map.Entry<Long, Set<Long>> e : remaining.entrySet()) {
			Set<Long> left = e.getValue();
			if (left == null || left.isEmpty()) {
				continue;
			}
			itemsRelTagsRepository.deleteBy(companyId, e.getKey(), new ArrayList<>(left));
		}
	}

	private void createRelTagsByItemId(long companyId, long itemId, List<Long> tagIds) {
		if (itemsRelTagsRepository.getInfo(companyId, itemId) != null) {
			itemsRelTagsRepository.deleteBy(companyId, itemId);
		}
		if (!tagIds.isEmpty()) {
			for (long tagId : tagIds) {
				ItemsRelTags row = new ItemsRelTags();
				row.setCompanyId(companyId);
				row.setItemId(itemId);
				row.setTagId(tagId);
				itemsRelTagsRepository.create(row);
			}
		}
	}

	private void createRelTagsByTagId(long companyId, List<Long> itemIds, long tagId) {
		for (long itemId : itemIds) {
			itemsRelTagsRepository.deleteBy(companyId, itemId);
			if (tagId != 0L) {
				ItemsRelTags row = new ItemsRelTags();
				row.setCompanyId(companyId);
				row.setItemId(itemId);
				row.setTagId(tagId);
				itemsRelTagsRepository.create(row);
			}
		}
	}

	private static List<Long> parseIdListFromElements(Iterable<?> elements) {
		List<Long> out = new ArrayList<>();
		for (Object el : elements) {
			if (el == null) {
				continue;
			}
			if (el instanceof Number n) {
				out.add(n.longValue());
			} else if (el instanceof String es) {
				String p = es.trim();
				if (p.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(p));
				} catch (NumberFormatException e) {
					throw new BadRequestException("参数格式错误");
				}
			} else {
				throw new BadRequestException("参数格式错误");
			}
		}
		return out;
	}

	private static boolean isArrayLike(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Collection<?>) {
			return true;
		}
		return v.getClass().isArray();
	}

	private static List<Long> parseIdList(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof Number n) {
			return new ArrayList<>(List.of(n.longValue()));
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return new ArrayList<>();
			}
			List<Long> out = new ArrayList<>();
			for (String seg : t.split(",")) {
				String p = seg.trim();
				if (p.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(p));
				} catch (NumberFormatException e) {
					throw new BadRequestException("参数格式错误");
				}
			}
			return out;
		}
		if (raw instanceof Collection<?> c) {
			return parseIdListFromElements(c);
		}
		if (raw instanceof long[] la) {
			List<Long> out = new ArrayList<>(la.length);
			for (long v : la) {
				out.add(v);
			}
			return out;
		}
		if (raw instanceof int[] ia) {
			List<Long> out = new ArrayList<>(ia.length);
			for (int v : ia) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof short[] sa) {
			List<Long> out = new ArrayList<>(sa.length);
			for (short v : sa) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof byte[] ba) {
			List<Long> out = new ArrayList<>(ba.length);
			for (byte v : ba) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof Object[] oa) {
			return parseIdListFromElements(Arrays.asList(oa));
		}
		throw new BadRequestException("参数格式错误");
	}
}
