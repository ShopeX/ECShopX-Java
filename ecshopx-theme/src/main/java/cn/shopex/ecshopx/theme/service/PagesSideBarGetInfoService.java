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
import cn.shopex.ecshopx.theme.support.PagesSideBarColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PagesSideBarGetInfoService {

	private final PagesSideBarMapper pagesSideBarMapper;

	private final PagesSideBarColumnNamesDataMapper pagesSideBarColumnNamesDataMapper;

	public Object getInfo(long companyId, long sidebarId) {
		LambdaQueryWrapper<PagesSideBar> w = new LambdaQueryWrapper<>();
		w.eq(PagesSideBar::getCompanyId, companyId);
		w.eq(PagesSideBar::getId, sidebarId);
		w.select(
				PagesSideBar::getId,
				PagesSideBar::getCompanyId,
				PagesSideBar::getRegionauthId,
				PagesSideBar::getName,
				PagesSideBar::getPages,
				PagesSideBar::getDisabled,
				PagesSideBar::getSetting);
		PagesSideBar row = pagesSideBarMapper.selectOne(w);
		if (row == null) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> result =
				new LinkedHashMap<>(pagesSideBarColumnNamesDataMapper.toColumnNamesData(row));
		Object p = result.get("pages");
		if (p instanceof String s && StringUtils.hasText(s)) {
			String inner = trimLeadingTrailingCommas(s);
			result.put("pages", Arrays.asList(inner.split(",", -1)));
		}
		return result;
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
}
