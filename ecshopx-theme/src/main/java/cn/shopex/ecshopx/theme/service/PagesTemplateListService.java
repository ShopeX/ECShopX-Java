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

import cn.shopex.ecshopx.companys.domain.Regionauth;
import cn.shopex.ecshopx.companys.mapper.RegionauthMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateListQuery;
import cn.shopex.ecshopx.theme.support.PagesTemplateRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PagesTemplateListService {

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private final PagesTemplateMapper pagesTemplateMapper;

	private final PagesTemplateRowMapper pagesTemplateRowMapper;

	private final CommonLangModReadService commonLangModReadService;

	private final RegionauthMapper regionauthMapper;

	public PagesTemplateListService(
			PagesTemplateMapper pagesTemplateMapper,
			PagesTemplateRowMapper pagesTemplateRowMapper,
			CommonLangModReadService commonLangModReadService,
			RegionauthMapper regionauthMapper) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.pagesTemplateRowMapper = pagesTemplateRowMapper;
		this.commonLangModReadService = commonLangModReadService;
		this.regionauthMapper = regionauthMapper;
	}

	public Map<String, Object> lists(PagesTemplateListQuery query, String requestLocaleTag) {
		LambdaQueryWrapper<PagesTemplate> wrapper = buildWrapper(query);
		long total = pagesTemplateMapper.selectCount(wrapper);
		Page<PagesTemplate> page = new Page<>(query.page(), query.pageSize(), false);
		pagesTemplateMapper.selectPage(page, wrapper);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (page.getRecords().isEmpty()) {
			out.put("list", List.of());
			return out;
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (PagesTemplate entity : page.getRecords()) {
			Map<String, Object> row = pagesTemplateRowMapper.toRowMap(entity);
			commonLangModReadService.applyPagesTemplateDetailLangOverlay(
					query.companyId(), row, requestLocaleTag, false);
			normalizeListTemplateContentEmptyJsonObject(row, requestLocaleTag);
			listMaps.add(row);
		}

		List<Long> distinctIds =
				listMaps.stream()
						.map(r -> parseRegionauthIdKey(r.get("regionauth_id")))
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.distinct()
						.toList();

		if (distinctIds.isEmpty()) {
			for (Map<String, Object> row : listMaps) {
				row.put("regionauth_name", "");
			}
			out.put("list", listMaps);
			return out;
		}

		List<Regionauth> authRows =
				regionauthMapper.selectList(
						new LambdaQueryWrapper<Regionauth>()
								.eq(Regionauth::getCompanyId, query.companyId())
								.in(Regionauth::getRegionauthId, distinctIds));
		Map<Long, String> nameById = new HashMap<>();
		for (Regionauth ar : authRows) {
			Long id = ar.getRegionauthId();
			String nm = ar.getRegionauthName();
			nameById.put(id, nm == null ? "" : nm);
		}
		for (Map<String, Object> row : listMaps) {
			Long key = parseRegionauthIdKey(row.get("regionauth_id"));
			if (key == null || key <= 0L) {
				row.put("regionauth_name", "");
			} else {
				row.put("regionauth_name", nameById.getOrDefault(key, ""));
			}
		}

		out.put("list", listMaps);
		return out;
	}

	private static LambdaQueryWrapper<PagesTemplate> buildWrapper(PagesTemplateListQuery query) {
		LambdaQueryWrapper<PagesTemplate> w = new LambdaQueryWrapper<>();
		w.eq(PagesTemplate::getCompanyId, query.companyId())
				.eq(PagesTemplate::getDistributorId, query.distributorId())
				.eq(PagesTemplate::getWeappPages, query.weappPages())
				.isNull(PagesTemplate::getDeletedAt)
				.orderByDesc(PagesTemplate::getCreatedAt);
		if (query.regionauthIdPresent()) {
			w.eq(PagesTemplate::getRegionauthId, query.regionauthId());
		}
		return w;
	}

	/**
	 * 列表接口：将字面量空 JSON 对象与空数组在序列化层面的表示统一（主字段与多语言映射）。
	 */
	private static void normalizeListTemplateContentEmptyJsonObject(
			Map<String, Object> row, String requestLocaleTag) {
		Object main = row.get("template_content");
		if (main != null && "{}".equals(String.valueOf(main).trim())) {
			row.put("template_content", "[]");
		}
		Object langObj = row.get("template_content_lang");
		if (!(langObj instanceof Map<?, ?> langMap) || langMap.isEmpty()) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<Object, Object> mutableLang = (Map<Object, Object>) langObj;
		String canonical = resolveCanonicalLocaleTagForList(requestLocaleTag);
		boolean allKeys = langMap.size() == 1;
		for (Map.Entry<?, ?> e : langMap.entrySet()) {
			Object k = e.getKey();
			String keyStr = k == null ? "" : String.valueOf(k);
			if (!allKeys && !canonical.equalsIgnoreCase(keyStr)) {
				continue;
			}
			Object v = e.getValue();
			if (v == null) {
				continue;
			}
			if ("{}".equals(String.valueOf(v).trim())) {
				mutableLang.put(k, "[]");
			}
		}
	}

	private static String resolveCanonicalLocaleTagForList(String requestLocaleTag) {
		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		if (!StringUtils.hasText(langRaw)) {
			langRaw = LANG_ZH_CN;
		}
		langRaw = extractPrimaryLanguageTagForList(langRaw);
		String canonical = canonicalCommonLangLocaleTagForList(langRaw);
		return canonical == null ? LANG_ZH_CN : canonical;
	}

	private static String extractPrimaryLanguageTagForList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String s = raw.trim();
		int comma = s.indexOf(',');
		if (comma >= 0) {
			s = s.substring(0, comma).trim();
		}
		int semi = s.indexOf(';');
		if (semi >= 0) {
			s = s.substring(0, semi).trim();
		}
		return s;
	}

	private static String canonicalCommonLangLocaleTagForList(String langRaw) {
		if (LANG_ZH_CN.equalsIgnoreCase(langRaw)) {
			return LANG_ZH_CN;
		}
		if (LANG_EN_CN.equalsIgnoreCase(langRaw)) {
			return LANG_EN_CN;
		}
		if (LANG_AR_SA.equalsIgnoreCase(langRaw)) {
			return LANG_AR_SA;
		}
		return null;
	}

	private static Long parseRegionauthIdKey(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
