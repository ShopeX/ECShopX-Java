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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreatePagesSideBarRequest;
import cn.shopex.ecshopx.theme.domain.PagesSideBar;
import cn.shopex.ecshopx.theme.mapper.PagesSideBarMapper;
import cn.shopex.ecshopx.theme.support.PagesSideBarColumnNamesDataMapper;
import cn.shopex.ecshopx.theme.support.PagesSideBarPagesConflictChecker;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PagesSideBarCreateService {

	private final PagesSideBarMapper pagesSideBarMapper;

	private final PagesSideBarColumnNamesDataMapper pagesSideBarColumnNamesDataMapper;

	private final PagesSideBarPagesConflictChecker pagesSideBarPagesConflictChecker;

	private final MessageSource messageSource;

	public Map<String, Object> create(long companyId, CreatePagesSideBarRequest request) {
		JsonNode pages = request.getPages();
		String pagesForDb;
		List<String> tokens;
		if (pages.isArray()) {
			List<String> normalizedList = new ArrayList<>();
			for (JsonNode el : pages) {
				if (el == null || el.isNull()) {
					continue;
				}
				String raw;
				if (el.isTextual()) {
					raw = el.asText();
				} else if (el.isIntegralNumber()) {
					raw = String.valueOf(el.longValue());
				} else {
					continue;
				}
				String t = raw == null ? "" : raw.trim();
				if (StringUtils.hasText(t)) {
					normalizedList.add(t);
				}
			}
			if (normalizedList.isEmpty()) {
				throw new BadRequestException(messageSource.getMessage(
						"theme.pages_sidebar.pages_invalid", null, LocaleContextHolder.getLocale()));
			}
			pagesForDb = "," + String.join(",", normalizedList) + ",";
			tokens = normalizedList;
		} else if (pages.isTextual()) {
			String text = pages.asText();
			pagesForDb = text;
			tokens = Arrays.stream(text.trim().split(","))
					.map(String::trim)
					.filter(StringUtils::hasText)
					.toList();
			if (tokens.isEmpty()) {
				throw new BadRequestException(messageSource.getMessage(
						"theme.pages_sidebar.pages_invalid", null, LocaleContextHolder.getLocale()));
			}
		} else {
			throw new BadRequestException(messageSource.getMessage(
					"theme.pages_sidebar.pages_invalid", null, LocaleContextHolder.getLocale()));
		}

		long regionauthId = Optional.ofNullable(request.getRegionauthId()).orElse(0L);
		pagesSideBarPagesConflictChecker.checkPagesExist(companyId, regionauthId, tokens, null);

		PagesSideBar entity = new PagesSideBar();
		entity.setCompanyId(companyId);
		entity.setName(request.getName().trim());
		entity.setPages(pagesForDb);
		entity.setRegionauthId(regionauthId);
		if (request.getDisabled() != null) {
			entity.setDisabled(request.getDisabled());
		}
		if (request.getSetting() != null) {
			entity.setSetting(request.getSetting());
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(now);
		entity.setUpdated(now);

		pagesSideBarMapper.insert(entity);
		return pagesSideBarColumnNamesDataMapper.toColumnNamesData(entity);
	}
}
