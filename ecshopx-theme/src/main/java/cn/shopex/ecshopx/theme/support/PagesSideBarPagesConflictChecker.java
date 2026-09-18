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

package cn.shopex.ecshopx.theme.support;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.domain.PagesSideBar;
import cn.shopex.ecshopx.theme.mapper.PagesSideBarMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PagesSideBarPagesConflictChecker {

	private final PagesSideBarMapper pagesSideBarMapper;

	public void checkPagesExist(
			long companyId, long regionauthId, List<String> pageTokens, String excludePathSidebarId) {
		if (pageTokens == null || pageTokens.isEmpty()) {
			return;
		}
		long excludeNumeric =
				(excludePathSidebarId == null) ? 0L : tryParsePositiveLongStrict(excludePathSidebarId);
		LambdaQueryWrapper<PagesSideBar> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(PagesSideBar::getDisabled, false)
				.eq(PagesSideBar::getCompanyId, companyId)
				.eq(PagesSideBar::getRegionauthId, regionauthId);
		if (excludeNumeric > 0L) {
			wrapper.ne(PagesSideBar::getId, excludeNumeric);
		}
		wrapper.and(w -> w.nested(inner -> {
			boolean first = true;
			for (String token : pageTokens) {
				if (!first) {
					inner.or();
				}
				first = false;
				String escaped = token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
				inner.like(PagesSideBar::getPages, "%" + escaped + "%");
			}
		}));

		List<PagesSideBar> rows = pagesSideBarMapper.selectList(wrapper);
		Set<String> conflicts = new LinkedHashSet<>();
		for (PagesSideBar row : rows) {
			String stored = row.getPages();
			List<String> storedTokens;
			if (!StringUtils.hasText(stored)) {
				storedTokens = List.of();
			} else {
				String s = stored.trim();
				while (s.startsWith(",")) {
					s = s.substring(1);
				}
				while (s.endsWith(",")) {
					s = s.substring(0, s.length() - 1);
				}
				if (!StringUtils.hasText(s)) {
					storedTokens = List.of();
				} else {
					storedTokens = Arrays.stream(s.split(","))
							.map(String::trim)
							.filter(StringUtils::hasText)
							.toList();
				}
			}
			for (String p : pageTokens) {
				if (storedTokens.contains(p)) {
					conflicts.add(p);
				}
			}
		}
		if (!conflicts.isEmpty()) {
			throw new ResourceException(String.join(",", conflicts) + "不可以重复配置");
		}
	}

	private static long tryParsePositiveLongStrict(String s) {
		if (s == null) {
			return 0L;
		}
		String t = s.trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			if (v > 0) {
				return v;
			}
			return 0L;
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
