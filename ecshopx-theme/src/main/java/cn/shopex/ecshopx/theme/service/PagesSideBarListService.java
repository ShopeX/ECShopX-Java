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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.theme.domain.PagesSideBar;
import cn.shopex.ecshopx.theme.mapper.PagesSideBarMapper;
import cn.shopex.ecshopx.theme.service.dto.PagesSideBarListCriteria;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PagesSideBarListService {

	private final PagesSideBarMapper pagesSideBarMapper;

	public Map<String, Object> getList(PagesSideBarListCriteria criteria, int page, int pageSize) {
		LambdaQueryWrapper<PagesSideBar> w = new LambdaQueryWrapper<>();
		w.eq(PagesSideBar::getCompanyId, criteria.getCompanyId());
		if (criteria.getRegionauthId() != null) {
			w.eq(PagesSideBar::getRegionauthId, criteria.getRegionauthId());
		}
		if (criteria.getId() != null) {
			w.eq(PagesSideBar::getId, criteria.getId());
		}
		if (criteria.getNameContains() != null) {
			w.like(
					PagesSideBar::getName,
					"%" + escapeLike(criteria.getNameContains()) + "%");
		}
		if (criteria.getPagesContainsInner() != null) {
			w.like(
					PagesSideBar::getPages,
					"%" + escapeLike(criteria.getPagesContainsInner()) + "%");
		}
		w.orderByDesc(PagesSideBar::getId);

		long total = pagesSideBarMapper.selectCount(w);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}
		w.select(
				PagesSideBar::getId,
				PagesSideBar::getCompanyId,
				PagesSideBar::getRegionauthId,
				PagesSideBar::getName,
				PagesSideBar::getPages,
				PagesSideBar::getDisabled,
				PagesSideBar::getCreated,
				PagesSideBar::getUpdated);
		Page<PagesSideBar> p = new Page<>(page, pageSize, false);
		pagesSideBarMapper.selectPage(p, w);
		List<Map<String, Object>> list =
				p.getRecords().stream().map(this::toRowMap).toList();
		out.put("list", list);
		return out;
	}

	private Map<String, Object> toRowMap(PagesSideBar e) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("regionauth_id", e.getRegionauthId());
		row.put("name", e.getName());
		String raw = e.getPages();
		if (!StringUtils.hasText(raw)) {
			row.put("pages", "");
		} else {
			String inner = trimLeadingTrailingCommas(raw);
			List<String> parts = Arrays.asList(inner.split(",", -1));
			row.put("pages", parts);
		}
		int disabledVal =
				e.getDisabled() == null || Boolean.FALSE.equals(e.getDisabled()) ? 0 : 1;
		row.put("disabled", Integer.valueOf(disabledVal));
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}

	private static String trimLeadingTrailingCommas(String s) {
		int start = 0;
		int end = s.length();
		while (start < end && s.charAt(start) == ',') {
			start++;
		}
		while (end > start && s.charAt(end - 1) == ',') {
			end--;
		}
		return s.substring(start, end);
	}

	private static String escapeLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
