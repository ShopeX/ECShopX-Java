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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagesSideBarWxappSidebarInfoService {

	private final PagesSideBarMapper pagesSideBarMapper;

	public Object getPagesSideBar(long companyId, Object regionauthIdEqScalar, String pageType) {
		LambdaQueryWrapper<PagesSideBar> w = new LambdaQueryWrapper<>();
		w.eq(PagesSideBar::getCompanyId, companyId);
		w.apply("regionauth_id = {0}", regionauthIdEqScalar);
		w.eq(PagesSideBar::getDisabled, Boolean.FALSE);
		String inner = "," + pageType + ",";
		String pattern = "%" + escapeLike(inner) + "%";
		w.like(PagesSideBar::getPages, pattern);
		w.last("LIMIT 1");
		List<PagesSideBar> rows = pagesSideBarMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		return toSidebarMapRow(rows.get(0));
	}

	private static Map<String, Object> toSidebarMapRow(PagesSideBar e) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("regionauth_id", e.getRegionauthId());
		row.put("name", e.getName());
		row.put("pages", e.getPages());
		Boolean disabled = e.getDisabled();
		row.put("disabled", disabled == null ? Boolean.FALSE : disabled);
		row.put("setting", e.getSetting());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}

	private static String escapeLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
