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

import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves SKU-level规格图 from {@code items_rel_attributes.image_url} for cart / checkout display.
 */
@Service
public class ItemSpecImageLookupService {

	private final ItemRelAttributesRepository itemRelAttributesRepository;

	public ItemSpecImageLookupService(ItemRelAttributesRepository itemRelAttributesRepository) {
		this.itemRelAttributesRepository = itemRelAttributesRepository;
	}

	/**
	 * First plain image URL per item_id from item_spec rel rows (JSON array → first element).
	 */
	public Map<Long, String> firstSpecImageUrlByItemId(long companyId, Collection<Long> itemIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		List<Long> ids = distinctPositive(itemIds);
		if (ids.isEmpty()) {
			return out;
		}
		List<ItemRelAttributes> rels =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, ids, "item_spec");
		if (rels == null || rels.isEmpty()) {
			return out;
		}
		for (ItemRelAttributes rel : rels) {
			if (rel == null || rel.getItemId() == null || rel.getItemId() <= 0L) {
				continue;
			}
			long itemId = rel.getItemId();
			if (out.containsKey(itemId)) {
				continue;
			}
			String first = firstUrlFromImageField(rel.getImageUrl());
			if (StringUtils.hasText(first)) {
				out.put(itemId, first);
			}
		}
		return out;
	}

	/** Puts {@code spec_image_url} on each row when a规格图 exists for {@code item_id}. */
	public void applySpecImageUrl(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			long id = toLong(row.get("item_id"));
			if (id > 0L) {
				itemIds.add(id);
			}
		}
		Map<Long, String> byItem = firstSpecImageUrlByItemId(companyId, itemIds);
		if (byItem.isEmpty()) {
			return;
		}
		for (Map<String, Object> row : rows) {
			long id = toLong(row.get("item_id"));
			String url = byItem.get(id);
			if (StringUtils.hasText(url)) {
				row.put("spec_image_url", url);
			}
		}
	}

	/**
	 * Prefer规格图 for the SKU; otherwise first URL from fallback pic fields (pic / pics / etc.).
	 */
	public String resolveDisplayPic(long companyId, long itemId, Object... fallbacks) {
		if (itemId > 0L) {
			String fromRel = firstSpecImageUrlByItemId(companyId, List.of(itemId)).get(itemId);
			if (StringUtils.hasText(fromRel)) {
				return fromRel;
			}
		}
		if (fallbacks == null) {
			return "";
		}
		for (Object fb : fallbacks) {
			String first = firstUrlFromPicsObject(fb);
			if (StringUtils.hasText(first)) {
				return first;
			}
		}
		return "";
	}

	/** Parses stored image_url / pics JSON or plain URL into the first display URL. */
	public static String firstUrlFromImageField(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String trimmed = raw.trim();
		Object resolved = GoodsItemsListRowMapper.resolvePicsForListRow(trimmed);
		String fromResolved = firstUrlFromPicsObject(resolved);
		if (StringUtils.hasText(fromResolved)) {
			return fromResolved;
		}
		// resolvePicsForListRow returns [] for a bare URL string; keep the plain URL.
		if (!trimmed.startsWith("[")) {
			return trimmed;
		}
		return "";
	}

	public static String firstUrlFromPicsObject(Object pics) {
		if (pics == null) {
			return "";
		}
		if (pics instanceof List<?> list) {
			if (list.isEmpty()) {
				return "";
			}
			Object first = list.get(0);
			return first == null ? "" : String.valueOf(first).trim();
		}
		if (pics instanceof String s) {
			s = s.trim();
			if (s.isEmpty()) {
				return "";
			}
			if (s.startsWith("[")) {
				return firstUrlFromImageField(s);
			}
			return s;
		}
		return String.valueOf(pics).trim();
	}

	private static List<Long> distinctPositive(Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		Set<Long> seen = new LinkedHashSet<>();
		List<Long> out = new ArrayList<>();
		for (Long id : itemIds) {
			if (id == null || id <= 0L) {
				continue;
			}
			if (seen.add(id)) {
				out.add(id);
			}
		}
		return out;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
