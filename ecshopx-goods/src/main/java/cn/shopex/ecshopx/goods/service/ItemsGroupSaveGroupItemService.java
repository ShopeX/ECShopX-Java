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
import cn.shopex.ecshopx.goods.domain.ItemsGroup;
import cn.shopex.ecshopx.goods.domain.ItemsGroupRelItem;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupRelItemMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsGroupSaveGroupItemService {

	private static final int BATCH_SIZE = 500;
	private static final String GROUP_TYPE_WIDGET = "widget";
	private static final DateTimeFormatter GROUP_KEY_TIME =
			DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

	private final ItemsGroupMapper itemsGroupMapper;
	private final ItemsGroupRelItemMapper itemsGroupRelItemMapper;

	public ItemsGroupSaveGroupItemService(
			ItemsGroupMapper itemsGroupMapper,
			ItemsGroupRelItemMapper itemsGroupRelItemMapper) {
		this.itemsGroupMapper = itemsGroupMapper;
		this.itemsGroupRelItemMapper = itemsGroupRelItemMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveGroupItem(long companyId, Map<String, Object> input) {
		validateRelGoodsIds(input);
		long regionauthId = parseRegionauthId(input);

		Map<String, Object> itemsGroupRow;
		long resolvedGroupId;

		if (isEmptyGroupId(input)) {
			ItemsGroup group = createNewGroup(companyId, regionauthId, input);
			int inserted = itemsGroupMapper.insert(group);
			if (inserted != 1 || group.getId() == null) {
				throw new ResourceException("分组创建失败，请稍后重试");
			}
			resolvedGroupId = group.getId();
			itemsGroupRow = entityToItemsGroupMap(group);
		} else {
			long groupId = parseNonEmptyGroupId(input.get("group_id"));
			ItemsGroup existing = itemsGroupMapper.selectById(groupId);
			if (existing == null) {
				throw new ResourceException("group_id错误");
			}
			resolvedGroupId = existing.getId();
			itemsGroupRow = entityToItemsGroupMap(existing);
		}

		syncRelItems(companyId, regionauthId, resolvedGroupId, input.get("rel_goods_ids"));
		return itemsGroupRow;
	}

	private static void validateRelGoodsIds(Map<String, Object> input) {
		if (!input.containsKey("rel_goods_ids")) {
			throw new BadRequestException("商品ID不能为空", 422);
		}
		Object v = input.get("rel_goods_ids");
		if (v == null) {
			throw new BadRequestException("商品ID不能为空", 422);
		}
		if (v instanceof String s) {
			if (s.trim().isEmpty()) {
				throw new BadRequestException("商品ID不能为空", 422);
			}
			return;
		}
		if (v instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new BadRequestException("商品ID不能为空", 422);
			}
		}
	}

	private static long parseRegionauthId(Map<String, Object> input) {
		if (!input.containsKey("regionauth_id")) {
			throw new BadRequestException("区域ID不能为空", 422);
		}
		Object v = input.get("regionauth_id");
		if (v == null) {
			throw new BadRequestException("区域ID不能为空", 422);
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("区域ID不能为空", 422);
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("区域ID不能为空", 422);
			}
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("区域ID不能为空", 422);
		}
	}

	private static boolean isEmptyGroupId(Map<String, Object> input) {
		if (!input.containsKey("group_id")) {
			return true;
		}
		Object v = input.get("group_id");
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		String t = v.toString().trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static long parseNonEmptyGroupId(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new ResourceException("group_id错误");
			}
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("group_id错误");
		}
	}

	private ItemsGroup createNewGroup(long companyId, long regionauthId, Map<String, Object> input) {
		String suffix;
		Object ptid = input.get("pages_template_id");
		if (ptid != null) {
			String asText;
			if (ptid instanceof Number n) {
				if (n.doubleValue() == Math.floor(n.doubleValue())) {
					asText = String.valueOf(n.longValue());
				} else {
					asText = String.valueOf(n).trim();
				}
			} else {
				asText = ptid.toString().trim();
			}
			if (!asText.isEmpty()) {
				suffix = asText;
			} else {
				suffix = ZonedDateTime.now(ZoneId.systemDefault()).format(GROUP_KEY_TIME);
			}
		} else {
			suffix = ZonedDateTime.now(ZoneId.systemDefault()).format(GROUP_KEY_TIME);
		}
		String groupKey = "widget-" + suffix;
		int now = (int) Instant.now().getEpochSecond();
		ItemsGroup group = new ItemsGroup();
		group.setCompanyId(companyId);
		group.setRegionauthId(regionauthId);
		group.setGroupKey(groupKey);
		group.setRemark("");
		group.setCreated(now);
		group.setUpdated(now);
		return group;
	}

	private void syncRelItems(long companyId, long regionauthId, long groupId, Object relRaw) {
		List<Long> parsedIds = parseRelGoodsIdTokens(relRaw);
		List<Long> newIds = parsedIds.stream()
				.filter(Objects::nonNull)
				.filter(id -> id > 0)
				.distinct()
				.collect(Collectors.toList());
		if (newIds.isEmpty()) {
			return;
		}
		Set<Long> newIdSet = new LinkedHashSet<>(newIds);

		List<ItemsGroupRelItem> existingRows = itemsGroupRelItemMapper.selectList(
				Wrappers.lambdaQuery(ItemsGroupRelItem.class)
						.select(ItemsGroupRelItem::getGoodsId)
						.eq(ItemsGroupRelItem::getGroupId, groupId));
		Set<Long> oldIds = existingRows.stream()
				.map(ItemsGroupRelItem::getGoodsId)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<Long> toDelete = oldIds.stream().filter(id -> !newIdSet.contains(id)).collect(Collectors.toList());
		List<Long> toInsert = newIds.stream().filter(id -> !oldIds.contains(id)).collect(Collectors.toList());

		if (!toDelete.isEmpty()) {
			itemsGroupRelItemMapper.delete(
					Wrappers.<ItemsGroupRelItem>lambdaQuery()
							.eq(ItemsGroupRelItem::getGroupId, groupId)
							.in(ItemsGroupRelItem::getGoodsId, toDelete));
		}

		if (!toInsert.isEmpty()) {
			int now = (int) Instant.now().getEpochSecond();
			for (int i = 0; i < toInsert.size(); i += BATCH_SIZE) {
				int end = Math.min(i + BATCH_SIZE, toInsert.size());
				for (int j = i; j < end; j++) {
					ItemsGroupRelItem row = new ItemsGroupRelItem();
					row.setCompanyId(companyId);
					row.setRegionauthId(regionauthId);
					row.setGroupId(groupId);
					row.setGroupType(GROUP_TYPE_WIDGET);
					row.setGoodsId(toInsert.get(j));
					row.setItemId(0L);
					row.setIsDel(0L);
					row.setCreated(now);
					row.setUpdated(now);
					itemsGroupRelItemMapper.insert(row);
				}
			}
		}
	}

	private static List<Long> parseRelGoodsIdTokens(Object relRaw) {
		List<Long> out = new ArrayList<>();
		if (relRaw == null) {
			return out;
		}
		if (relRaw instanceof String s) {
			for (String part : s.split(",")) {
				String t = part.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException ignored) {
					// skip invalid token
				}
			}
			return out;
		}
		if (relRaw instanceof Collection<?> c) {
			for (Object el : c) {
				if (el == null) {
					continue;
				}
				String t = el.toString().trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
			return out;
		}
		if (relRaw instanceof Number n) {
			out.add(n.longValue());
			return out;
		}
		String t = relRaw.toString().trim();
		if (!t.isEmpty()) {
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	private static Map<String, Object> entityToItemsGroupMap(ItemsGroup e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("regionauth_id", e.getRegionauthId());
		m.put("group_key", e.getGroupKey());
		m.put("remark", e.getRemark() != null ? e.getRemark() : "");
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
