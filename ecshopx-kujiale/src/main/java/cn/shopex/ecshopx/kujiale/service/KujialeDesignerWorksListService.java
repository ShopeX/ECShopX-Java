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
import cn.shopex.ecshopx.kujiale.api.admin.v1.dto.DesignerWorksListRowDto;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksListQueryMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模糊条件使用 {@code CONCAT('%', 参数, '%')}；参数中的 {@code _}、{@code %} 按 SQL LIKE 通配符语义生效，不做转义。
 */
@Service
public class KujialeDesignerWorksListService {

	private final KujialeDesignerWorksListQueryMapper queryMapper;

	public KujialeDesignerWorksListService(KujialeDesignerWorksListQueryMapper queryMapper) {
		this.queryMapper = queryMapper;
	}

	public Map<String, Object> list(
			String pageRaw,
			String pageSizeRaw,
			String designName,
			String designId,
			String keyword) {
		KujialeDesignerWorksPagingParser.Result paging =
				KujialeDesignerWorksPagingParser.parse(pageRaw, pageSizeRaw);
		if (paging.hasErrors()) {
			throw new ResourceException("422 Unprocessable Content");
		}
		int p = paging.page();
		int ps = paging.pageSize();

		boolean applyPaging = ps > 0;
		Integer limit = applyPaging ? ps : null;
		Integer offset = applyPaging ? (p - 1) * ps : null;

		long total;
		List<Map<String, Object>> rawList;
		if (keyword != null && !keyword.isEmpty()) {
			total = queryMapper.countByKeyword(keyword);
			rawList = queryMapper.selectPageByKeyword(keyword, limit, offset, applyPaging);
		} else {
			String designNameLike = blankToNull(designName);
			String designIdEq = blankToNull(designId);
			total = queryMapper.countFiltered(designNameLike, designIdEq);
			rawList = queryMapper.selectPageFiltered(designNameLike, designIdEq, limit, offset, applyPaging);
		}

		if (rawList.isEmpty()) {
			return buildSuccessBody(p, ps, total, Collections.emptyList());
		}

		List<String> designIds = new ArrayList<>();
		for (Map<String, Object> row : rawList) {
			Object did = row.get("design_id");
			if (did != null) {
				String ds = did.toString();
				if (StringUtils.hasText(ds)) {
					designIds.add(ds);
				}
			}
		}

		List<Map<String, Object>> boundRels =
				designIds.isEmpty() ? List.of() : queryMapper.selectRelsByDesignIds(designIds);

		Map<String, Map<String, Object>> boundDesignMap = new LinkedHashMap<>();
		for (Map<String, Object> rel : boundRels) {
			Object did = rel.get("design_id");
			if (did == null) {
				continue;
			}
			String key = did.toString();
			if (StringUtils.hasText(key)) {
				boundDesignMap.put(key, rel);
			}
		}

		Set<Long> itemIds = new LinkedHashSet<>();
		for (Map<String, Object> rel : boundRels) {
			Long iid = toLong(rel.get("item_id"));
			if (iid != null) {
				itemIds.add(iid);
			}
		}

		Map<Long, Map<String, Object>> boundItemMap = new LinkedHashMap<>();
		if (!itemIds.isEmpty()) {
			List<Map<String, Object>> itemRows = queryMapper.selectItemsByItemIds(new ArrayList<>(itemIds));
			for (Map<String, Object> ir : itemRows) {
				Long iid = toLong(ir.get("item_id"));
				if (iid != null) {
					boundItemMap.put(iid, ir);
				}
			}
		}

		List<DesignerWorksListRowDto> out = new ArrayList<>(rawList.size());
		for (Map<String, Object> row : rawList) {
			out.add(toRowDto(row, boundDesignMap, boundItemMap));
		}

		return buildSuccessBody(p, ps, total, out);
	}

	private static Map<String, Object> buildSuccessBody(int p, int ps, long total, List<DesignerWorksListRowDto> list) {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("success", Boolean.TRUE);
		inner.put("total_count", total);
		inner.put("page", p);
		inner.put("pageSize", ps);
		inner.put("list", list);
		return inner;
	}

	private DesignerWorksListRowDto toRowDto(
			Map<String, Object> row,
			Map<String, Map<String, Object>> boundDesignMap,
			Map<Long, Map<String, Object>> boundItemMap) {
		Object didObj = row.get("design_id");
		String designId = didObj != null ? didObj.toString() : "";

		DesignerWorksListRowDto dto = new DesignerWorksListRowDto();
		dto.setId(row.get("id"));
		dto.setDesignId(designId);
		Object dn = row.get("design_name");
		dto.setDesignName(dn != null ? dn.toString() : "");
		dto.setCoverPic(row.get("cover_pic"));
		dto.setPlanId(row.get("plan_id"));
		dto.setCommName(row.get("comm_name"));
		dto.setCity(row.get("city"));
		dto.setName(row.get("name"));
		dto.setViewCount(toIntCount(row.get("view_count")));
		dto.setLikeCount(toIntCount(row.get("like_count")));
		dto.setCreated(row.get("created"));
		dto.setUpdated(row.get("updated"));

		boolean rowBound = StringUtils.hasText(designId) && boundDesignMap.containsKey(designId);
		dto.setBound(rowBound);

		DesignerWorksListRowDto.DesignerWorksListBoundItemDto bound = null;
		if (rowBound) {
			Map<String, Object> rel = boundDesignMap.get(designId);
			Long itemId = rel != null ? toLong(rel.get("item_id")) : null;
			if (itemId != null) {
				Map<String, Object> item = boundItemMap.get(itemId);
				if (item != null) {
					bound = new DesignerWorksListRowDto.DesignerWorksListBoundItemDto();
					bound.setItemId(rel.get("item_id"));
					Object iname = item.get("item_name");
					bound.setItemName(iname != null ? iname.toString() : "");
					Object ibn = item.get("item_bn");
					bound.setItemBn(ibn != null ? ibn.toString() : "");
					Object rgb = rel.get("goods_bn");
					Object igb = item.get("goods_bn");
					String relBn = rgb != null && StringUtils.hasText(rgb.toString()) ? rgb.toString() : null;
					bound.setGoodsBn(relBn != null ? relBn : (igb != null ? igb.toString() : ""));
				}
			}
		}
		dto.setBoundItem(bound);
		return dto;
	}

	private static String blankToNull(String s) {
		if (s == null || !StringUtils.hasText(s.trim())) {
			return null;
		}
		return s.trim();
	}

	private static int toIntCount(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString());
		} catch (NumberFormatException e) {
			return 0;
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
