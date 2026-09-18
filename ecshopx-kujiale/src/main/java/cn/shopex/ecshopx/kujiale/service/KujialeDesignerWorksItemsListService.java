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

package cn.shopex.ecshopx.kujiale.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kujiale.api.admin.v1.dto.DesignerWorksItemListRowDto;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksItemsQueryMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模糊条件使用 {@code CONCAT('%', 参数, '%')}；参数中的 {@code _}、{@code %} 按 SQL LIKE 通配符语义生效，不做转义。
 */
@Service
public class KujialeDesignerWorksItemsListService {

	private final KujialeDesignerWorksItemsQueryMapper queryMapper;
	private final ObjectMapper objectMapper;

	public KujialeDesignerWorksItemsListService(
			KujialeDesignerWorksItemsQueryMapper queryMapper,
			ObjectMapper objectMapper) {
		this.queryMapper = queryMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> list(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String itemName,
			String itemBn,
			String goodsBn,
			String designId,
			String designName,
			String[] approveStatus,
			String[] itemCategory) {
		KujialeDesignerWorksPagingParser.Result paging =
				KujialeDesignerWorksPagingParser.parse(pageRaw, pageSizeRaw);
		if (paging.hasErrors()) {
			throw new ResourceException("422 Unprocessable Content");
		}
		int p = paging.page();
		int ps = paging.pageSize();

		String itemNameF = blankToNull(itemName);
		String itemBnF = blankToNull(itemBn);
		String goodsBnF = blankToNull(goodsBn);
		String designIdF = blankToNull(designId);
		String designNameF = blankToNull(designName);
		String[] approveF = emptyArrayToNull(approveStatus);
		String[] categoryF = emptyArrayToNull(itemCategory);

		boolean applyPaging = ps > 0;
		Integer limit = applyPaging ? ps : null;
		Integer offset = applyPaging ? (p - 1) * ps : null;

		long total = queryMapper.countDistinct(
				companyId, itemNameF, itemBnF, goodsBnF, designIdF, designNameF, approveF, categoryF);

		List<Map<String, Object>> rawRows = queryMapper.selectPageRows(
				companyId,
				itemNameF,
				itemBnF,
				goodsBnF,
				designIdF,
				designNameF,
				approveF,
				categoryF,
				limit,
				offset,
				applyPaging);

		if (rawRows.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("success", Boolean.TRUE);
			empty.put("total_count", total);
			empty.put("page", p);
			empty.put("pageSize", ps);
			empty.put("list", Collections.emptyList());
			return empty;
		}

		List<Long> itemIds = new ArrayList<>();
		List<String> designIds = new ArrayList<>();
		for (Map<String, Object> row : rawRows) {
			Object iid = row.get("item_id");
			if (iid instanceof Number n) {
				itemIds.add(n.longValue());
			}
			Object did = row.get("design_id");
			if (did != null) {
				String ds = did.toString();
				if (StringUtils.hasText(ds)) {
					designIds.add(ds);
				}
			}
		}

		Map<Long, List<String>> salesByItem = loadSalesCategories(itemIds);
		Map<Long, List<DesignerWorksItemListRowDto.TagEntry>> tagsByItem =
				loadItemTags(companyId, itemIds);
		Map<String, List<String>> designTagsById = loadDesignTags(designIds);

		List<DesignerWorksItemListRowDto> list = new ArrayList<>(rawRows.size());
		for (Map<String, Object> row : rawRows) {
			list.add(toRowDto(row, salesByItem, tagsByItem, designTagsById));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("success", Boolean.TRUE);
		out.put("total_count", total);
		out.put("page", p);
		out.put("pageSize", ps);
		out.put("list", list);
		return out;
	}

	private static String blankToNull(String s) {
		if (s == null || !StringUtils.hasText(s.trim())) {
			return null;
		}
		return s.trim();
	}

	private static String[] emptyArrayToNull(String[] a) {
		if (a == null || a.length == 0) {
			return null;
		}
		return a;
	}

	private Map<Long, List<String>> loadSalesCategories(List<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Map<String, Object>> rows = queryMapper.selectSalesCategories(itemIds);
		Map<Long, List<String>> m = new LinkedHashMap<>();
		for (Map<String, Object> r : rows) {
			Object iid = r.get("item_id");
			if (!(iid instanceof Number n)) {
				continue;
			}
			Object name = r.get("category_name");
			String cn = name != null ? name.toString() : "";
			m.computeIfAbsent(n.longValue(), k -> new ArrayList<>()).add(cn);
		}
		return m;
	}

	private Map<Long, List<DesignerWorksItemListRowDto.TagEntry>> loadItemTags(long companyId, List<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Map<String, Object>> rows = queryMapper.selectItemTagRows(companyId, itemIds);
		Map<Long, List<DesignerWorksItemListRowDto.TagEntry>> m = new LinkedHashMap<>();
		for (Map<String, Object> r : rows) {
			Object iid = r.get("item_id");
			if (!(iid instanceof Number n)) {
				continue;
			}
			DesignerWorksItemListRowDto.TagEntry te = new DesignerWorksItemListRowDto.TagEntry();
			te.setTagId(r.get("tag_id"));
			Object tn = r.get("tag_name");
			te.setTagName(tn != null ? tn.toString() : "");
			te.setTagColor(r.get("tag_color"));
			te.setFontColor(r.get("font_color"));
			m.computeIfAbsent(n.longValue(), k -> new ArrayList<>()).add(te);
		}
		return m;
	}

	private Map<String, List<String>> loadDesignTags(List<String> designIds) {
		if (designIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<String> distinct = designIds.stream().distinct().collect(Collectors.toList());
		List<Map<String, Object>> rows = queryMapper.selectDesignTagNames(distinct);
		Map<String, List<String>> m = new LinkedHashMap<>();
		for (Map<String, Object> r : rows) {
			Object did = r.get("design_id");
			if (did == null) {
				continue;
			}
			String key = did.toString();
			Object tn = r.get("tag_name");
			if (tn == null) {
				continue;
			}
			m.computeIfAbsent(key, k -> new ArrayList<>()).add(tn.toString());
		}
		return m;
	}

	private DesignerWorksItemListRowDto toRowDto(
			Map<String, Object> row,
			Map<Long, List<String>> salesByItem,
			Map<Long, List<DesignerWorksItemListRowDto.TagEntry>> tagsByItem,
			Map<String, List<String>> designTagsById) {
		Long itemId = toLong(row.get("item_id"));
		DesignerWorksItemListRowDto dto = new DesignerWorksItemListRowDto();
		dto.setItemId(row.get("item_id"));
		Object name = row.get("item_name");
		dto.setItemName(name != null ? name.toString() : "");
		Object ibn = row.get("item_bn");
		dto.setItemBn(ibn != null ? ibn.toString() : "");
		Object rgb = row.get("rel_goods_bn");
		Object igb = row.get("item_goods_bn");
		String relBn = rgb != null && StringUtils.hasText(rgb.toString()) ? rgb.toString() : null;
		dto.setGoodsBn(relBn != null ? relBn : (igb != null ? igb.toString() : ""));
		dto.setApproveStatus(row.get("approve_status"));
		dto.setItemCategory(row.get("item_category"));
		Object store = row.get("store");
		if (store != null) {
			if (store instanceof Number n) {
				dto.setStock(n.intValue());
			} else {
				try {
					dto.setStock(Integer.parseInt(store.toString()));
				} catch (NumberFormatException e) {
					dto.setStock(null);
				}
			}
		} else {
			dto.setStock(null);
		}
		List<String> sales = itemId != null ? salesByItem.getOrDefault(itemId, List.of()) : List.of();
		dto.setItemCatName(sales.isEmpty() ? "" : String.join(",", sales));
		List<DesignerWorksItemListRowDto.TagEntry> tags =
				itemId != null ? tagsByItem.getOrDefault(itemId, List.of()) : List.of();
		dto.setTagList(tags.isEmpty() ? List.of() : tags);
		dto.setPrice(row.get("price"));
		dto.setMarketPrice(row.get("market_price"));
		dto.setPics(parsePics(row.get("pics")));
		dto.setCreated(row.get("created"));
		dto.setUpdated(row.get("updated"));

		Object didObj = row.get("design_id");
		String designIdStr = didObj != null ? didObj.toString() : "";
		DesignerWorksItemListRowDto.DesignBlock design = new DesignerWorksItemListRowDto.DesignBlock();
		design.setDesignId(designIdStr);
		Object dname = row.get("design_name");
		design.setDesignName(dname != null ? dname.toString() : null);
		design.setCoverPic(row.get("cover_pic"));
		List<String> dtags = designTagsById.getOrDefault(designIdStr, List.of());
		design.setTags(dtags.isEmpty() ? "" : String.join(",", dtags));
		dto.setDesign(design);

		DesignerWorksItemListRowDto.BindInfo bind = new DesignerWorksItemListRowDto.BindInfo();
		bind.setRelId(row.get("rel_id"));
		bind.setBindCreated(row.get("bind_created"));
		dto.setBindInfo(bind);
		return dto;
	}

	private Object parsePics(Object picsCol) {
		if (picsCol == null) {
			return List.of();
		}
		String s = picsCol.toString();
		if (!StringUtils.hasText(s)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(s, Object.class);
		} catch (JsonProcessingException e) {
			return List.of();
		}
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
