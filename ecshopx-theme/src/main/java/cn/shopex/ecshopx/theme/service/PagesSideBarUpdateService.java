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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.domain.PagesSideBar;
import cn.shopex.ecshopx.theme.mapper.PagesSideBarMapper;
import cn.shopex.ecshopx.theme.support.PagesSideBarColumnNamesDataMapper;
import cn.shopex.ecshopx.theme.support.PagesSideBarPagesConflictChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PagesSideBarUpdateService {

	private static final List<String> WHITELIST_COLS =
			List.of("company_id", "regionauth_id", "name", "pages", "disabled", "setting");

	private final PagesSideBarMapper pagesSideBarMapper;

	private final PagesSideBarColumnNamesDataMapper pagesSideBarColumnNamesDataMapper;

	private final PagesSideBarPagesConflictChecker pagesSideBarPagesConflictChecker;

	private final MessageSource messageSource;

	public Map<String, Object> update(long companyId, String pathSidebarId, Map<String, Object> body) {
		Map<String, Object> params = new LinkedHashMap<>(body == null ? Map.of() : body);
		params.put("company_id", companyId);

		LambdaQueryWrapper<PagesSideBar> pkWrapper = buildPrimaryKeyWrapper(companyId, pathSidebarId);

		if (hasRequestKey(params, "name")) {
			String name = String.valueOf(params.get("name")).trim();
			if (!StringUtils.hasText(name)) {
				throw new BadRequestException(messageSource.getMessage(
						"theme.pages_sidebar.name_required", null, LocaleContextHolder.getLocale()));
			}
			params.put("name", name);
		}

		if (hasRequestKey(params, "pages")) {
			validateAndNormalizePages(params, params.get("pages"));
		}

		if (hasRequestKey(params, "disabled")) {
			Object v = params.get("disabled");
			if (v instanceof String s && "true".equalsIgnoreCase(s.trim())) {
				params.put("disabled", Boolean.TRUE);
			} else if (v instanceof Boolean b) {
				params.put("disabled", b);
			} else if (v instanceof Number n) {
				params.put("disabled", n.longValue() != 0L);
			} else {
				params.put("disabled", Boolean.parseBoolean(String.valueOf(v).trim()));
			}
		}

		if (hasRequestKey(params, "pages")
				&& params.get("pages") instanceof List<?> list
				&& !list.isEmpty()) {
			List<String> tokens = normalizeTokensFromList(list);
			params.put("pages", "," + String.join(",", tokens) + ",");
		}

		boolean pagesTruthy = hasRequestKey(params, "pages")
				&& ((params.get("pages") instanceof String s && StringUtils.hasText(s.trim()))
						|| (params.get("pages") instanceof java.util.Collection<?> c && !c.isEmpty()));
		if (pagesTruthy) {
			long regionauthIdForCheck;
			if (hasRequestKey(params, "regionauth_id") && toPositiveOrZeroLong(params.get("regionauth_id")) > 0) {
				regionauthIdForCheck = toPositiveOrZeroLong(params.get("regionauth_id"));
			} else {
				PagesSideBar existingForRegion = pagesSideBarMapper.selectOne(pkWrapper);
				if (existingForRegion == null) {
					throw new ResourceException("未查询到更新数据");
				}
				regionauthIdForCheck =
						existingForRegion.getRegionauthId() == null ? 0L : existingForRegion.getRegionauthId();
			}

			List<String> tokensForCheck = buildConflictCheckTokens(params.get("pages"));
			if (!tokensForCheck.isEmpty()) {
				pagesSideBarPagesConflictChecker.checkPagesExist(
						companyId, regionauthIdForCheck, tokensForCheck, pathSidebarId);
			}
		}

		PagesSideBar entity = pagesSideBarMapper.selectOne(pkWrapper);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}

		for (String col : WHITELIST_COLS) {
			if (!hasRequestKey(params, col)) {
				continue;
			}
			switch (col) {
				case "company_id" -> entity.setCompanyId(companyId);
				case "regionauth_id" -> entity.setRegionauthId(toPositiveOrZeroLong(params.get("regionauth_id")));
				case "name" -> entity.setName(String.valueOf(params.get("name")).trim());
				case "pages" -> entity.setPages(String.valueOf(params.get("pages")));
				case "disabled" -> entity.setDisabled((Boolean) params.get("disabled"));
				case "setting" -> {
					Object s = params.get("setting");
					entity.setSetting(s == null ? null : String.valueOf(s));
				}
				default -> {
				}
			}
		}

		entity.setCompanyId(companyId);
		entity.setUpdated((int) (System.currentTimeMillis() / 1000L));

		pagesSideBarMapper.updateById(entity);

		Long pk = entity.getId();
		PagesSideBar refreshed = (pk == null) ? null : pagesSideBarMapper.selectById(pk);
		return pagesSideBarColumnNamesDataMapper.toColumnNamesData(refreshed != null ? refreshed : entity);
	}

	private LambdaQueryWrapper<PagesSideBar> buildPrimaryKeyWrapper(long companyId, String pathSidebarId) {
		String trimmed = pathSidebarId == null ? "" : pathSidebarId.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("id 不能为空");
		}
		LambdaQueryWrapper<PagesSideBar> w = new LambdaQueryWrapper<PagesSideBar>()
				.eq(PagesSideBar::getCompanyId, companyId);
		try {
			long parsedLong = Long.parseLong(trimmed);
			w.eq(PagesSideBar::getId, parsedLong);
		} catch (NumberFormatException ex) {
			w.apply("id = {0}", trimmed);
		}
		return w;
	}

	private void validateAndNormalizePages(Map<String, Object> params, Object raw) {
		Locale loc = LocaleContextHolder.getLocale();
		if (raw instanceof java.util.Collection<?> col) {
			List<String> tokens = new ArrayList<>();
			for (Object el : col) {
				if (el == null) {
					continue;
				}
				if (el instanceof String s) {
					String t = s.trim();
					if (StringUtils.hasText(t)) {
						tokens.add(t);
					}
					continue;
				}
				if (el instanceof Number n) {
					tokens.add(String.valueOf(n.longValue()).trim());
					continue;
				}
				throw new BadRequestException(
						messageSource.getMessage("theme.pages_sidebar.pages_invalid_element", null, loc));
			}
			if (tokens.isEmpty()) {
				throw new BadRequestException(
						messageSource.getMessage("theme.pages_sidebar.pages_required", null, loc));
			}
			params.put("pages", tokens);
			return;
		}
		if (raw instanceof String s) {
			boolean any = false;
			for (String part : s.split(",")) {
				if (StringUtils.hasText(part.trim())) {
					any = true;
					break;
				}
			}
			if (!any) {
				throw new BadRequestException(
						messageSource.getMessage("theme.pages_sidebar.pages_required", null, loc));
			}
			return;
		}
		throw new BadRequestException(
				messageSource.getMessage("theme.pages_sidebar.pages_invalid_type", null, loc));
	}

	private static List<String> normalizeTokensFromList(List<?> list) {
		List<String> tokens = new ArrayList<>();
		for (Object el : list) {
			if (el == null) {
				continue;
			}
			if (el instanceof String s) {
				String t = s.trim();
				if (StringUtils.hasText(t)) {
					tokens.add(t);
				}
			} else if (el instanceof Number n) {
				tokens.add(String.valueOf(n.longValue()).trim());
			}
		}
		return tokens;
	}

	private static List<String> buildConflictCheckTokens(Object pagesVal) {
		if (pagesVal instanceof String s) {
			String trimmed = s.trim();
			while (trimmed.startsWith(",")) {
				trimmed = trimmed.substring(1);
			}
			while (trimmed.endsWith(",")) {
				trimmed = trimmed.substring(0, trimmed.length() - 1);
			}
			if (!StringUtils.hasText(trimmed)) {
				return List.of();
			}
			return Arrays.stream(trimmed.split(","))
					.map(String::trim)
					.filter(StringUtils::hasText)
					.toList();
		}
		if (pagesVal instanceof List<?> list) {
			return normalizeTokensFromList(list);
		}
		return List.of();
	}

	private static boolean hasRequestKey(Map<String, ?> map, String key) {
		if (map == null || !map.containsKey(key)) {
			return false;
		}
		return map.get(key) != null;
	}

	private static long toPositiveOrZeroLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		if (o instanceof String s) {
			return parsePositiveLongFromString(s.trim());
		}
		return parsePositiveLongFromString(String.valueOf(o).trim());
	}

	private static long parsePositiveLongFromString(String t) {
		if (t == null || t.isEmpty()) {
			return 0L;
		}
		try {
			long p = Long.parseLong(t);
			return p > 0L ? p : 0L;
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
