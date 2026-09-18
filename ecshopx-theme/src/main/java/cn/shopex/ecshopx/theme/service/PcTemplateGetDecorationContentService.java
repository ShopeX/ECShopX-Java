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

import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.support.PcTemplateDecorationDefaults;
import cn.shopex.ecshopx.theme.support.PcTemplateStorageUrlNormalizer;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateContentRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * PHP parity: {@code ThemePcTemplateContentServices::decorationContent}.
 *
 * <p>Admin {@code getDecorationContent} seeds a default DSL when rows are missing so the shop editor
 * can open. Mall front {@code getTemplateContent} passes {@code seedIfMissing=false} and never writes
 * on GET, matching PHP open.
 */
@Service
@RequiredArgsConstructor
public class PcTemplateGetDecorationContentService {

	private static final String DSL_MARKER = "ECX_SP_WEB_DECORATION_DSL_V1";

	private final ThemePcTemplateMapper themePcTemplateMapper;
	private final ThemePcTemplateContentMapper themePcTemplateContentMapper;
	private final ThemePcTemplateContentRowMapper themePcTemplateContentRowMapper;
	private final ThemePcTemplateContentLangReadService themePcTemplateContentLangReadService;
	private final PcTemplateStorageUrlNormalizer pcTemplateStorageUrlNormalizer;
	private final PcTemplateDecorationDefaults pcTemplateDecorationDefaults;
	private final ObjectMapper objectMapper;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getDecorationContent(
			long companyId,
			String requestLang,
			String rawThemePcTemplateId,
			String pageType,
			String pageId,
			long distributorId) {
		return getDecorationContent(
				companyId, requestLang, rawThemePcTemplateId, pageType, pageId, distributorId, true);
	}

	/**
	 * @param seedIfMissing {@code true} for shop editor (admin {@code getDecorationContent}); {@code false}
	 *     for mall front {@code getTemplateContent}, matching PHP which never writes on GET.
	 */
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> getDecorationContent(
			long companyId,
			String requestLang,
			String rawThemePcTemplateId,
			String pageType,
			String pageId,
			long distributorId,
			boolean seedIfMissing) {
		String effectivePageType = pageType == null ? "" : pageType.trim();
		if ("header".equals(effectivePageType) || "footer".equals(effectivePageType)) {
			if (seedIfMissing) {
				return formatDecorationContent(
						loadOrInitHeaderOrFooterRow(companyId, requestLang, effectivePageType));
			}
			return formatDecorationContent(loadHeaderOrFooterRow(companyId, requestLang, effectivePageType));
		}
		return pageDecorationDetail(
				companyId,
				requestLang,
				rawThemePcTemplateId,
				effectivePageType,
				pageId,
				distributorId,
				seedIfMissing);
	}

	private Map<String, Object> pageDecorationDetail(
			long companyId,
			String requestLang,
			String rawThemePcTemplateId,
			String pageType,
			String pageId,
			long distributorId,
			boolean seedIfMissing) {
		LambdaQueryWrapper<ThemePcTemplate> filter =
				new LambdaQueryWrapper<ThemePcTemplate>()
						.eq(ThemePcTemplate::getCompanyId, companyId)
						.isNull(ThemePcTemplate::getDeletedAt);

		if (!isPhpEmpty(rawThemePcTemplateId)) {
			Long themeId = parsePositiveLongOrNull(rawThemePcTemplateId.trim());
			if (themeId == null) {
				return emptyDecorationContent();
			}
			filter.eq(ThemePcTemplate::getThemePcTemplateId, themeId);
		} else if ("custom".equals(pageType) && !isPhpEmpty(pageId)) {
			Long themeId = parsePositiveLongOrNull(pageId.trim());
			if (themeId == null) {
				return emptyDecorationContent();
			}
			filter.eq(ThemePcTemplate::getThemePcTemplateId, themeId);
		} else {
			String resolvedPageType =
					!StringUtils.hasText(pageType)
							? "index"
							: ("home".equals(pageType) ? "index" : pageType);
			filter.eq(ThemePcTemplate::getPageType, resolvedPageType)
					.eq(ThemePcTemplate::getStatus, 1)
					.eq(
							ThemePcTemplate::getDistributorId,
							(int) Math.min(Integer.MAX_VALUE, Math.max(0L, distributorId)));
		}

		filter.orderByAsc(ThemePcTemplate::getThemePcTemplateId).last("LIMIT 1");
		List<ThemePcTemplate> tpls = themePcTemplateMapper.selectList(filter);
		if (tpls == null || tpls.isEmpty()) {
			return emptyDecorationContent();
		}
		ThemePcTemplate tpl = tpls.get(0);
		long themePcTemplateId = tpl.getThemePcTemplateId();

		List<ThemePcTemplateContent> list =
				themePcTemplateContentMapper.selectList(
						new LambdaQueryWrapper<ThemePcTemplateContent>()
								.eq(ThemePcTemplateContent::getCompanyId, companyId)
								.eq(ThemePcTemplateContent::getThemePcTemplateId, themePcTemplateId)
								.orderByAsc(ThemePcTemplateContent::getThemePcTemplateContentId));
		if (list == null || list.isEmpty()) {
			if (!seedIfMissing) {
				return emptyDecorationContent();
			}
			ThemePcTemplateContent seeded =
					insertPageContent(companyId, themePcTemplateId, tpl.getPageType());
			Map<String, Object> row = themePcTemplateContentRowMapper.toRowMap(seeded);
			themePcTemplateContentLangReadService.applyThemePcTemplateContentDetailLangOverlay(
					companyId, row, requestLang);
			return formatDecorationContent(row);
		}

		ThemePcTemplateContent target = null;
		for (ThemePcTemplateContent value : list) {
			String params = value.getParams() == null ? "" : value.getParams();
			if (params.contains(DSL_MARKER)) {
				target = value;
				break;
			}
		}
		if (target == null) {
			target = list.get(0);
		}

		Map<String, Object> row = themePcTemplateContentRowMapper.toRowMap(target);
		themePcTemplateContentLangReadService.applyThemePcTemplateContentDetailLangOverlay(
				companyId, row, requestLang);
		return formatDecorationContent(row);
	}

	private Map<String, Object> loadOrInitHeaderOrFooterRow(
			long companyId, String requestLang, String pageType) {
		Map<String, Object> existing = loadHeaderOrFooterRow(companyId, requestLang, pageType);
		if (existing != null && !existing.isEmpty()) {
			Object params = existing.get("params");
			if (params != null && StringUtils.hasText(String.valueOf(params))) {
				return existing;
			}
		}
		ThemePcTemplateContent seeded = insertHeaderOrFooterContent(companyId, pageType);
		Map<String, Object> row = themePcTemplateContentRowMapper.toRowMap(seeded);
		themePcTemplateContentLangReadService.applyThemePcTemplateContentDetailLangOverlay(
				companyId, row, requestLang);
		return row;
	}

	private Map<String, Object> loadHeaderOrFooterRow(
			long companyId, String requestLang, String pageType) {
		// PHP decorationContent → detail(): exact match company_id + name (header|footer).
		LambdaQueryWrapper<ThemePcTemplateContent> w =
				new LambdaQueryWrapper<ThemePcTemplateContent>()
						.eq(ThemePcTemplateContent::getCompanyId, companyId)
						.eq(ThemePcTemplateContent::getName, pageType)
						.orderByAsc(ThemePcTemplateContent::getThemePcTemplateContentId)
						.last("LIMIT 1");
		List<ThemePcTemplateContent> list = themePcTemplateContentMapper.selectList(w);
		if (list == null || list.isEmpty()) {
			return null;
		}
		Map<String, Object> row = themePcTemplateContentRowMapper.toRowMap(list.get(0));
		themePcTemplateContentLangReadService.applyThemePcTemplateContentDetailLangOverlay(
				companyId, row, requestLang);
		return row;
	}

	private ThemePcTemplateContent insertHeaderOrFooterContent(long companyId, String pageType) {
		String config =
				"header".equals(pageType)
						? pcTemplateDecorationDefaults.defaultHeaderConfigJson()
						: pcTemplateDecorationDefaults.defaultFooterConfigJson();
		int now = (int) (System.currentTimeMillis() / 1000L);
		ThemePcTemplateContent entity = new ThemePcTemplateContent();
		entity.setCompanyId(companyId);
		entity.setThemePcTemplateId(0L);
		entity.setName(pageType);
		entity.setParams(config);
		entity.setSortBy(0);
		entity.setCreated(now);
		entity.setUpdated(now);
		themePcTemplateContentMapper.insert(entity);
		return themePcTemplateContentMapper.selectById(entity.getThemePcTemplateContentId());
	}

	private ThemePcTemplateContent insertPageContent(
			long companyId, long themePcTemplateId, String pageType) {
		String config = pcTemplateDecorationDefaults.defaultPageConfigJson(pageType, themePcTemplateId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		ThemePcTemplateContent entity = new ThemePcTemplateContent();
		entity.setCompanyId(companyId);
		entity.setThemePcTemplateId(themePcTemplateId);
		entity.setName("");
		entity.setParams(config);
		entity.setSortBy(1);
		entity.setCreated(now);
		entity.setUpdated(now);
		themePcTemplateContentMapper.insert(entity);
		ThemePcTemplateContent fresh =
				themePcTemplateContentMapper.selectById(entity.getThemePcTemplateContentId());
		if (fresh == null) {
			return entity;
		}
		// Mirror PHP save: write content id back into params for front edit stability.
		try {
			JsonNode root = objectMapper.readTree(config);
			if (root != null && root.isObject()) {
				((ObjectNode) root).put("id", fresh.getThemePcTemplateContentId());
				String updated = objectMapper.writeValueAsString(root);
				ThemePcTemplateContent patch = new ThemePcTemplateContent();
				patch.setThemePcTemplateContentId(fresh.getThemePcTemplateContentId());
				patch.setParams(updated);
				patch.setUpdated(now);
				themePcTemplateContentMapper.updateById(patch);
			}
		} catch (Exception ignored) {
			// keep original config
		}
		return themePcTemplateContentMapper.selectById(fresh.getThemePcTemplateContentId());
	}

	private Map<String, Object> formatDecorationContent(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return emptyDecorationContent();
		}
		Object configRaw = row.get("params");
		if (configRaw == null) {
			configRaw = row.get("config");
		}
		String config = configRaw == null ? "" : String.valueOf(configRaw);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", resolveContentId(row));
		Object name = row.get("name");
		out.put("name", name == null ? "" : String.valueOf(name));
		out.put("config", pcTemplateStorageUrlNormalizer.normalize(config));
		return out;
	}

	private static Map<String, Object> emptyDecorationContent() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", 0);
		out.put("name", "");
		out.put("config", "");
		return out;
	}

	private static Object resolveContentId(Map<String, Object> row) {
		Object id = row.get("theme_pc_template_content_id");
		if (id == null) {
			id = row.get("id");
		}
		if (id == null) {
			return 0;
		}
		// PHP/Dingo often serializes bigint ids as strings; keep string when numeric for front parity.
		if (id instanceof Number n) {
			return Long.toString(n.longValue());
		}
		String s = String.valueOf(id).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return Long.toString(Long.parseLong(s));
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static Long parsePositiveLongOrNull(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/** Matches PHP {@code empty()} for scalar id-like strings ({@code null}, {@code ""}, {@code "0"}). */
	private static boolean isPhpEmpty(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		return t.isEmpty() || "0".equals(t);
	}
}
