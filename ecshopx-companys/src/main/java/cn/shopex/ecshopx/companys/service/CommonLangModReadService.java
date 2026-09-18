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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommonLangModReadService {

	private static final String TABLE_SHOP_MENU = "shop_menu";
	private static final String FIELD_NAME = "name";

	private static final String TABLE_ARTICLE_CATEGORY = "companys_article_category";
	private static final String FIELD_CATEGORY_NAME = "category_name";

	private static final String TABLE_COMPANYS_ROLES = "companys_roles";
	private static final String FIELD_ROLE_NAME = "role_name";
	private static final String MODULE_COMPANYS_ROLES = "companys_roles";
	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private static final String TABLE_COMPANYS_ARTICLE = "companys_article";
	private static final List<String> ARTICLE_LIST_LANG_FIELDS =
			List.of("title", "summary", "content", "author", "province", "city", "area", "regions");

	private static final String TABLE_THEME_PC_TEMPLATE = "theme_pc_template";
	private static final List<String> THEME_PC_TEMPLATE_LIST_LANG_FIELDS =
			List.of("template_title", "template_description");

	private static final String TABLE_PAGES_TEMPLATE = "pages_template";
	private static final String MODULE_PAGES_TEMPLATE = "pages_template";
	private static final String FIELD_PAGES_TEMPLATE_NAME = "template_name";
	private static final String FIELD_PAGES_TEMPLATE_TITLE = "template_title";
	private static final String FIELD_PAGES_TEMPLATE_CONTENT = "template_content";
	private static final List<String> PAGES_TEMPLATE_DETAIL_LANG_FIELDS =
			List.of(FIELD_PAGES_TEMPLATE_NAME, FIELD_PAGES_TEMPLATE_TITLE);

	private static final String TABLE_ESPIER_UPLOADIMAGES_CAT = "espier_uploadimages_cat";
	private static final String FIELD_IMAGE_CAT_NAME = "image_cat_name";

	private static final String TABLE_ESPIER_PRINTER = "espier_printer";
	private static final String FIELD_PRINTER_NAME = "name";

	private static final String TABLE_SHIPPING_TEMPLATES = "shipping_templates";
	private static final String MODULE_SHIPPING_TEMPLATES = "shipping_templates";

	private static final String TABLE_WECHAT_WEAPP_CUSTOMIZE_PAGE = "wechat_weapp_customize_page";
	private static final List<String> WEAPP_CUSTOMIZE_PAGE_LIST_LANG_FIELDS =
			List.of("page_name", "page_description", "page_share_title", "page_share_desc");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;

	public CommonLangModReadService(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			ObjectMapper objectMapper) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public String findModAttributeForPagesTemplateSet(
			int companyId, long dataId, String field, String requestLang) {
		if (field == null || requestLang == null) {
			return null;
		}
		String lang = requestLang.trim();
		if (!LANG_ZH_CN.equalsIgnoreCase(lang)
				&& !LANG_EN_CN.equalsIgnoreCase(lang)
				&& !LANG_AR_SA.equalsIgnoreCase(lang)) {
			return null;
		}
		String tableName = "pages_template_set";
		String moduleName = "pages_template_set";
		String outsideTable = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(lang);
		String sql =
				"SELECT attribute_value FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND `field` = :field AND data_id = :data_id ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", (long) companyId);
		p.addValue("table_name", tableName);
		p.addValue("module_name", moduleName);
		p.addValue("field", field);
		p.addValue("data_id", dataId);
		String[] last = {null};
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av)) {
							last[0] = av;
						}
					});
		} catch (DataAccessException ignored) {
			return null;
		}
		return last[0];
	}

	/**
	 * For {@code theme_pc_template} list rows: when a mod row has a non-empty {@code attribute_value} for the
	 * resolved request locale, overwrites the base field and adds a single-entry {@code field_lang} map; otherwise
	 * leaves the row unchanged for that field (no empty {@code *_lang} map). Per-locale outside-item rows are matched
	 * by {@code table_name}, {@code field}, and {@code data_id} only (no {@code module_name}).
	 */
	public void applyThemePcTemplateListLangOverlay(long companyId, Map<String, Object> row, String requestLocaleTag) {
		if (row == null || row.isEmpty()) {
			return;
		}
		Object idObj = row.get("theme_pc_template_id");
		long dataId = parsePositiveLong(idObj);
		if (dataId <= 0L) {
			return;
		}
		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		if (!StringUtils.hasText(langRaw)) {
			langRaw = LANG_ZH_CN;
		}
		langRaw = extractPrimaryLanguageTag(langRaw);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			canonicalTag = LANG_ZH_CN;
		}
		for (String fieldName : THEME_PC_TEMPLATE_LIST_LANG_FIELDS) {
			String mod = loadThemePcTemplateListModValue(companyId, dataId, fieldName, canonicalTag);
			if (StringUtils.hasText(mod)) {
				row.put(fieldName, mod);
				Map<String, Object> langMap = new LinkedHashMap<>();
				langMap.put(canonicalTag, mod);
				row.put(fieldName + "_lang", langMap);
			}
		}
	}

	/**
	 * Merges outside-shard {@code image_cat_name} for {@code espier_uploadimages_cat} list rows for the
	 * resolved request locale (filters by {@code company_id}, {@code table_name}, {@code field}, {@code data_id} only).
	 * <p>
	 * For each row with a valid {@code image_cat_id}, {@code image_cat_name_lang} is <strong>always</strong> set: a
	 * single-entry map whose key is the canonical locale tag for the request, and whose value is the non-empty mod
	 * overlay when one exists, otherwise the same string as {@code image_cat_name} on that row after merge (including
	 * when there is no mod row or the mod value is empty). The key is never omitted for those rows.
	 */
	public void mergeEspierUploadImagesCatImageCatNameForList(
			long companyId, List<Map<String, Object>> rows, String requestLocaleTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long id = parsePositiveLong(row.get("image_cat_id"));
			if (id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}

		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		if (!StringUtils.hasText(langRaw)) {
			langRaw = LANG_ZH_CN;
		}
		langRaw = extractPrimaryLanguageTag(langRaw);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			canonicalTag = LANG_ZH_CN;
		}

		Map<Long, String> byDataId = loadEspierUploadImagesCatImageCatNamesByCanonical(companyId, ids, canonicalTag);

		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long dataId = parsePositiveLong(row.get("image_cat_id"));
			if (dataId <= 0L) {
				continue;
			}
			String baseName = row.get("image_cat_name") == null ? "" : String.valueOf(row.get("image_cat_name"));
			String modValue = byDataId.get(dataId);
			String effective = baseName;
			if (modValue != null && !isEffectivelyEmptyForLangOverlay(modValue)) {
				row.put("image_cat_name", modValue);
				effective = modValue;
			} else if (!StringUtils.hasText(baseName)) {
				effective = "";
			}
			Map<String, Object> langMap = new LinkedHashMap<>();
			langMap.put(canonicalTag, effective);
			row.put("image_cat_name_lang", langMap);
		}
	}

	/**
	 * Merges {@code outside_item_multi_lang_mod_lang_*} {@code name} for {@code espier_printer} list rows for the
	 * resolved request locale (filters by {@code company_id}, {@code table_name}, {@code field}, {@code data_id}
	 * only). When a mod value exists and is non-empty, overwrites the row {@code name} and sets {@code name_lang} to
	 * a single-entry map {@code { canonical_locale_tag -> mod_value }}; otherwise leaves the row unchanged (no
	 * {@code name_lang} key).
	 */
	public void mergeEspierPrinterListNameForLocale(
			long companyId, List<Map<String, Object>> rows, String requestLocaleTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long id = parsePositiveLong(row.get("id"));
			if (id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}

		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		if (!StringUtils.hasText(langRaw)) {
			langRaw = LANG_ZH_CN;
		}
		langRaw = extractPrimaryLanguageTag(langRaw);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			canonicalTag = LANG_ZH_CN;
		}

		Map<Long, String> byDataId = loadEspierPrinterNamesByCanonical(companyId, ids, canonicalTag);

		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long dataId = parsePositiveLong(row.get("id"));
			if (dataId <= 0L) {
				continue;
			}
			String modValue = byDataId.get(dataId);
			if (modValue != null && !isEffectivelyEmptyForLangOverlay(modValue)) {
				row.put("name", modValue);
				Map<String, Object> nameLang = new LinkedHashMap<>();
				nameLang.put(canonicalTag, modValue);
				row.put("name_lang", nameLang);
			}
		}
	}

	/**
	 * Applies a locale-specific display name to one shipping-template detail map. The row must carry
	 * {@code template_id} as the logical template primary key. The request locale is normalized to a canonical tag
	 * ({@code zh-CN}, {@code en-CN}, or {@code ar-SA}); other tags leave the row unchanged. For a supported tag, loads
	 * overrides from the matching {@code outside_item_multi_lang_mod_lang_*} table using {@code table_name} and
	 * {@code module_name} {@code shipping_templates}, {@code field} {@code name}, and {@code data_id} equal to
	 * {@code template_id} (no {@code company_id} predicate on that table). When the query yields a last non-empty
	 * {@code attribute_value} in ascending {@code id} order, replaces {@code name} and sets {@code name_lang} to a
	 * single-entry map keyed by the canonical tag.
	 */
	public void applyShippingTemplateDetailNameLangOverlay(
			long companyId, Map<String, Object> row, String requestLocaleTag) {
		if (row == null || row.isEmpty()) {
			return;
		}
		long dataId = parsePositiveLong(row.get("template_id"));
		if (dataId <= 0L) {
			return;
		}
		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		if (!StringUtils.hasText(langRaw)) {
			langRaw = LANG_ZH_CN;
		}
		langRaw = extractPrimaryLanguageTag(langRaw);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			return;
		}
		String mod = loadShippingTemplateDetailNameMod(companyId, dataId, canonicalTag);
		if (StringUtils.hasText(mod) && !isEffectivelyEmptyForLangOverlay(mod)) {
			row.put("name", mod);
			Map<String, Object> nameLang = new LinkedHashMap<>();
			nameLang.put(canonicalTag, mod);
			row.put("name_lang", nameLang);
		}
	}

	/**
	 * Batch variant of {@link #applyShippingTemplateDetailNameLangOverlay(long, Map, String)} for list rows carrying
	 * {@code template_id}: reads {@code outside_item_multi_lang_mod_lang_*} with the same keys as the detail path and
	 * applies the last non-empty {@code attribute_value} per {@code data_id} (ordered by {@code id} ascending).
	 */
	@SuppressWarnings("unused")
	public void mergeShippingTemplateListNamesForLocale(
			long companyId, List<Map<String, Object>> rows, String requestLocaleTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long id = parsePositiveLong(row.get("template_id"));
			if (id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		if (!StringUtils.hasText(langRaw)) {
			langRaw = LANG_ZH_CN;
		}
		langRaw = extractPrimaryLanguageTag(langRaw);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			return;
		}
		Map<Long, String> byDataId = loadShippingTemplateListNamesFromOutsideBatch(ids, canonicalTag);
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long dataId = parsePositiveLong(row.get("template_id"));
			if (dataId <= 0L) {
				continue;
			}
			String modValue = byDataId.get(dataId);
			if (modValue != null && !isEffectivelyEmptyForLangOverlay(modValue)) {
				row.put("name", modValue);
				Map<String, Object> nameLang = new LinkedHashMap<>();
				nameLang.put(canonicalTag, modValue);
				row.put("name_lang", nameLang);
			}
		}
	}

	private Map<Long, String> loadShippingTemplateListNamesFromOutsideBatch(List<Long> dataIds, String canonicalTag) {
		if (dataIds == null || dataIds.isEmpty()) {
			return Map.of();
		}
		if (!LANG_ZH_CN.equals(canonicalTag)
				&& !LANG_EN_CN.equals(canonicalTag)
				&& !LANG_AR_SA.equals(canonicalTag)) {
			return Map.of();
		}
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonicalTag);
		List<Long> ids = new ArrayList<>(dataIds);
		String sql =
				"SELECT data_id, attribute_value FROM "
						+ table
						+ " WHERE table_name = :table_name AND module_name = :module_name AND `field` = :field "
						+ "AND data_id IN (:ids) ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("table_name", TABLE_SHIPPING_TEMPLATES);
		p.addValue("module_name", MODULE_SHIPPING_TEMPLATES);
		p.addValue("field", FIELD_NAME);
		p.addValue("ids", ids);
		Map<Long, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av) && !isEffectivelyEmptyForLangOverlay(av)) {
							out.put(dataId, av);
						}
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}

	@SuppressWarnings("unused")
	private String loadShippingTemplateDetailNameMod(long companyId, long dataId, String canonicalTag) {
		if (LANG_ZH_CN.equals(canonicalTag)
				|| LANG_EN_CN.equals(canonicalTag)
				|| LANG_AR_SA.equals(canonicalTag)) {
			return loadShippingTemplateDetailNameModFromOutside(dataId, canonicalTag);
		}
		return "";
	}

	/**
	 * Loads a single override string from the per-locale table {@code outside_item_multi_lang_mod_lang_*} whose name
	 * suffix matches {@code canonicalTag}. Selects rows where {@code table_name} and {@code module_name} are
	 * {@code shipping_templates}, {@code field} is {@code name}, and {@code data_id} equals the template id. Rows are
	 * ordered by {@code id} ascending; while iterating, each non-blank {@code attribute_value} replaces the previous
	 * candidate so the final non-empty value wins. Returns an empty string when inputs are invalid, the table is
	 * missing, no qualifying rows exist, or the data access layer reports an error.
	 */
	private String loadShippingTemplateDetailNameModFromOutside(long dataId, String canonicalTag) {
		if (dataId <= 0L || !StringUtils.hasText(canonicalTag)) {
			return "";
		}
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonicalTag);
		String sql = "SELECT attribute_value FROM "
				+ table
				+ " WHERE table_name = :table_name AND module_name = :module_name AND `field` = :field "
				+ "AND data_id = :data_id ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("table_name", TABLE_SHIPPING_TEMPLATES);
		p.addValue("module_name", MODULE_SHIPPING_TEMPLATES);
		p.addValue("field", FIELD_NAME);
		p.addValue("data_id", dataId);
		final String[] lastNonEmpty = {""};
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av) && !isEffectivelyEmptyForLangOverlay(av)) {
							lastNonEmpty[0] = av;
						}
					});
		} catch (DataAccessException ignored) {
			return "";
		}
		return lastNonEmpty[0];
	}

	private Map<Long, String> loadEspierPrinterNamesByCanonical(
			long companyId, List<Long> dataIds, String canonicalTag) {
		if (LANG_ZH_CN.equals(canonicalTag)) {
			return loadEspierPrinterNamesCn(companyId, dataIds);
		}
		if (LANG_EN_CN.equals(canonicalTag)) {
			return loadEspierPrinterNamesEn(companyId, dataIds);
		}
		if (LANG_AR_SA.equals(canonicalTag)) {
			return loadEspierPrinterNamesAr(companyId, dataIds);
		}
		return Map.of();
	}

	/**
	 * Batch-loads {@code name} for {@code espier_printer} from {@code outside_item_multi_lang_mod_lang_*}
	 * ({@code company_id}, {@code table_name}, {@code field}, {@code data_id}); rows are read in {@code id} order so
	 * the last non-empty {@code attribute_value} wins per {@code data_id}.
	 */
	private Map<Long, String> loadEspierPrinterNamesCn(long companyId, List<Long> dataIds) {
		return loadEspierPrinterNamesFromOutsideTable(companyId, dataIds, normalizeLangTableSuffix(LANG_ZH_CN));
	}

	private Map<Long, String> loadEspierPrinterNamesEn(long companyId, List<Long> dataIds) {
		return loadEspierPrinterNamesFromOutsideTable(companyId, dataIds, normalizeLangTableSuffix(LANG_EN_CN));
	}

	private Map<Long, String> loadEspierPrinterNamesAr(long companyId, List<Long> dataIds) {
		return loadEspierPrinterNamesFromOutsideTable(companyId, dataIds, normalizeLangTableSuffix(LANG_AR_SA));
	}

	/**
	 * List-style outside_item query: {@code company_id}, {@code table_name}, {@code data_id IN (...)} ,
	 * {@code field}; multiple rows per {@code data_id} resolve to the last non-empty {@code attribute_value} by
	 * ascending {@code id}.
	 */
	private Map<Long, String> loadEspierPrinterNamesFromOutsideTable(
			long companyId, List<Long> dataIds, String langTableSuffix) {
		List<Long> ids = new ArrayList<>(dataIds);
		String table = OUTSIDE_LANG_TABLE_PREFIX + langTableSuffix;
		String sql = "SELECT data_id, attribute_value FROM "
				+ table
				+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
				+ "AND data_id IN (:ids) ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_ESPIER_PRINTER);
		p.addValue("field", FIELD_PRINTER_NAME);
		p.addValue("ids", ids);
		Map<Long, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av) && !isEffectivelyEmptyForLangOverlay(av)) {
							out.put(dataId, av);
						}
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}


	private Map<Long, String> loadEspierUploadImagesCatImageCatNamesByCanonical(
			long companyId, List<Long> dataIds, String canonicalTag) {
		if (LANG_ZH_CN.equals(canonicalTag)) {
			return loadEspierUploadImagesCatImageCatNamesCn(companyId, dataIds);
		}
		if (LANG_EN_CN.equals(canonicalTag)) {
			return loadEspierUploadImagesCatImageCatNamesEn(companyId, dataIds);
		}
		if (LANG_AR_SA.equals(canonicalTag)) {
			return loadEspierUploadImagesCatImageCatNamesAr(companyId, dataIds);
		}
		return Map.of();
	}

	private Map<Long, String> loadEspierUploadImagesCatImageCatNamesCn(long companyId, List<Long> dataIds) {
		return loadEspierUploadImagesCatNamesFromOutside(companyId, dataIds, normalizeLangTableSuffix(LANG_ZH_CN));
	}

	private Map<Long, String> loadEspierUploadImagesCatImageCatNamesEn(long companyId, List<Long> dataIds) {
		return loadEspierUploadImagesCatNamesFromOutside(companyId, dataIds, normalizeLangTableSuffix(LANG_EN_CN));
	}

	private Map<Long, String> loadEspierUploadImagesCatImageCatNamesAr(long companyId, List<Long> dataIds) {
		return loadEspierUploadImagesCatNamesFromOutside(companyId, dataIds, normalizeLangTableSuffix(LANG_AR_SA));
	}

	private Map<Long, String> loadEspierUploadImagesCatNamesFromOutside(
			long companyId, List<Long> dataIds, String langTableSuffix) {
		if (dataIds == null || dataIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = new ArrayList<>(dataIds);
		String table = OUTSIDE_LANG_TABLE_PREFIX + langTableSuffix;
		String sql =
				"SELECT data_id, attribute_value FROM "
						+ table
						+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
						+ "AND data_id IN (:ids) ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_ESPIER_UPLOADIMAGES_CAT);
		p.addValue("field", FIELD_IMAGE_CAT_NAME);
		p.addValue("ids", ids);
		Map<Long, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av) && !isEffectivelyEmptyForLangOverlay(av)) {
							out.put(dataId, av);
						}
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}

	/**
	 * Same as {@link #applyPagesTemplateDetailLangOverlay(long, Map, String, boolean)} with
	 * {@code restrictModuleName == true}.
	 */
	public void applyPagesTemplateDetailLangOverlay(long companyId, Map<String, Object> row, String requestLocaleTag) {
		applyPagesTemplateDetailLangOverlay(companyId, row, requestLocaleTag, true);
	}

	/**
	 * Merges outside-shard values for the resolved locale into {@code template_name} /
	 * {@code template_title}, and always sets {@code template_name_lang} / {@code template_title_lang} to a
	 * single-entry map keyed by that locale (base row text when no mod row exists). When the row still
	 * contains {@code template_content} (raw column text), sets {@code template_content_lang} to a
	 * single-entry map for that locale: mod-table value when present, otherwise the base column string.
	 *
	 * @param restrictModuleName when {@code true}, mod queries require {@code module_name = pages_template}; when
	 *     {@code false}, uses list-style filters only ({@code company_id}, {@code table_name}, {@code field},
	 *     {@code data_id}) without {@code module_name}.
	 */
	public void applyPagesTemplateDetailLangOverlay(
			long companyId, Map<String, Object> row, String requestLocaleTag, boolean restrictModuleName) {
		if (row == null || row.isEmpty()) {
			return;
		}
		Object idObj = row.get("pages_template_id");
		long dataId = parsePositiveLong(idObj);
		if (dataId <= 0L) {
			return;
		}
		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		if (!StringUtils.hasText(langRaw)) {
			langRaw = LANG_ZH_CN;
		}
		langRaw = extractPrimaryLanguageTag(langRaw);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			canonicalTag = LANG_ZH_CN;
		}
		Map<String, String> byField =
				loadPagesTemplateNameTitleOverlay(companyId, dataId, canonicalTag, restrictModuleName);
		for (String fieldName : PAGES_TEMPLATE_DETAIL_LANG_FIELDS) {
			String base = row.get(fieldName) == null ? "" : String.valueOf(row.get(fieldName));
			String mod = byField.get(fieldName);
			String effective = base;
			if (StringUtils.hasText(mod)) {
				row.put(fieldName, mod);
				effective = mod;
			} else if (!StringUtils.hasText(base)) {
				effective = "";
			}
			Map<String, Object> langMap = new LinkedHashMap<>();
			langMap.put(canonicalTag, effective);
			row.put(fieldName + "_lang", langMap);
		}
		Object rawTemplateContentObj = row.get(FIELD_PAGES_TEMPLATE_CONTENT);
		String baseTemplateContent = rawTemplateContentObj == null ? "" : String.valueOf(rawTemplateContentObj);
		String modTemplateContent =
				loadPagesTemplateTemplateContentOverlay(companyId, dataId, canonicalTag, restrictModuleName);
		String effectiveTemplateContent =
				StringUtils.hasText(modTemplateContent) ? modTemplateContent : baseTemplateContent;
		row.put(FIELD_PAGES_TEMPLATE_CONTENT, effectiveTemplateContent);
		if (StringUtils.hasText(effectiveTemplateContent)) {
			Map<String, Object> templateContentLang = new LinkedHashMap<>();
			templateContentLang.put(canonicalTag, effectiveTemplateContent);
			row.put("template_content_lang", templateContentLang);
		}
	}

	private static long parsePositiveLong(Object idObj) {
		if (idObj == null) {
			return 0L;
		}
		if (idObj instanceof Number idNum) {
			return idNum.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(idObj).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/** First language tag from a header value (handles {@code Accept-Language} lists and q-values). */
	private static String extractPrimaryLanguageTag(String raw) {
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

	private static String canonicalCommonLangLocaleTag(String langRaw) {
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

	private String loadPagesTemplateTemplateContentOverlay(
			long companyId, long dataId, String canonicalTag, boolean restrictModuleName) {
		if (LANG_ZH_CN.equals(canonicalTag)) {
			return loadPagesTemplateTemplateContentCn(companyId, dataId, restrictModuleName);
		}
		if (LANG_EN_CN.equals(canonicalTag)) {
			return loadPagesTemplateTemplateContentEn(companyId, dataId, restrictModuleName);
		}
		if (LANG_AR_SA.equals(canonicalTag)) {
			return loadPagesTemplateTemplateContentAr(companyId, dataId, restrictModuleName);
		}
		return "";
	}

	private String loadPagesTemplateTemplateContentCn(long companyId, long dataId, boolean restrictModuleName) {
		return loadPagesTemplateFieldFromOutside(
				companyId, dataId, FIELD_PAGES_TEMPLATE_CONTENT, LANG_ZH_CN, restrictModuleName);
	}

	private String loadPagesTemplateTemplateContentEn(long companyId, long dataId, boolean restrictModuleName) {
		return loadPagesTemplateFieldFromOutside(
				companyId, dataId, FIELD_PAGES_TEMPLATE_CONTENT, LANG_EN_CN, restrictModuleName);
	}

	private String loadPagesTemplateTemplateContentAr(long companyId, long dataId, boolean restrictModuleName) {
		return loadPagesTemplateFieldFromOutside(
				companyId, dataId, FIELD_PAGES_TEMPLATE_CONTENT, LANG_AR_SA, restrictModuleName);
	}

	private String loadPagesTemplateFieldFromOutside(
			long companyId, long dataId, String fieldName, String canonicalTag, boolean restrictModuleName) {
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonicalTag);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT attribute_value FROM ").append(table);
		sql.append(" WHERE company_id = :company_id AND table_name = :table_name ");
		if (restrictModuleName) {
			sql.append("AND module_name = :module_name ");
		}
		sql.append("AND data_id = :data_id AND `field` = :field ORDER BY id ASC");
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_PAGES_TEMPLATE);
		if (restrictModuleName) {
			p.addValue("module_name", MODULE_PAGES_TEMPLATE);
		}
		p.addValue("data_id", dataId);
		p.addValue("field", fieldName);
		String[] lastNonEmpty = {""};
		try {
			namedParameterJdbcTemplate.query(
					sql.toString(),
					p,
					rs -> {
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av)) {
							lastNonEmpty[0] = av;
						}
					});
		} catch (DataAccessException ignored) {
			return "";
		}
		return lastNonEmpty[0];
	}

	/**
	 * List overlay: per-locale {@code outside_item_multi_lang_mod_lang_*} using {@code table_name},
	 * {@code field}, and {@code data_id} only (no {@code module_name}).
	 */
	private String loadThemePcTemplateListModFromOutside(long dataId, String fieldName, String canonicalTag) {
		if (dataId <= 0L || !StringUtils.hasText(fieldName) || !StringUtils.hasText(canonicalTag)) {
			return null;
		}
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonicalTag);
		String sql = "SELECT attribute_value FROM "
				+ table
				+ " WHERE table_name = :table_name AND `field` = :field "
				+ "AND data_id = :data_id ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("table_name", TABLE_THEME_PC_TEMPLATE);
		p.addValue("field", fieldName);
		p.addValue("data_id", dataId);
		String[] holder = {null};
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						if (holder[0] != null) {
							return;
						}
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av)) {
							holder[0] = av;
						}
					});
		} catch (DataAccessException ignored) {
			return null;
		}
		return holder[0];
	}

	private String loadThemePcTemplateListModValue(
			long companyId, long dataId, String fieldName, String canonicalTag) {
		return loadThemePcTemplateListModFromOutside(dataId, fieldName, canonicalTag);
	}

	private Map<String, String> loadPagesTemplateNameTitleOverlay(
			long companyId, long dataId, String canonicalTag, boolean restrictModuleName) {
		if (LANG_ZH_CN.equals(canonicalTag)) {
			return loadPagesTemplateNameTitleCn(companyId, dataId, restrictModuleName);
		}
		if (LANG_EN_CN.equals(canonicalTag)) {
			return loadPagesTemplateNameTitleEn(companyId, dataId, restrictModuleName);
		}
		if (LANG_AR_SA.equals(canonicalTag)) {
			return loadPagesTemplateNameTitleAr(companyId, dataId, restrictModuleName);
		}
		return Map.of();
	}

	private Map<String, String> loadPagesTemplateNameTitleCn(long companyId, long dataId, boolean restrictModuleName) {
		return loadPagesTemplateNameTitleFromOutside(companyId, dataId, LANG_ZH_CN, restrictModuleName);
	}

	private Map<String, String> loadPagesTemplateNameTitleEn(long companyId, long dataId, boolean restrictModuleName) {
		return loadPagesTemplateNameTitleFromOutside(companyId, dataId, LANG_EN_CN, restrictModuleName);
	}

	private Map<String, String> loadPagesTemplateNameTitleAr(long companyId, long dataId, boolean restrictModuleName) {
		return loadPagesTemplateNameTitleFromOutside(companyId, dataId, LANG_AR_SA, restrictModuleName);
	}

	private Map<String, String> loadPagesTemplateNameTitleFromOutside(
			long companyId, long dataId, String canonicalTag, boolean restrictModuleName) {
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonicalTag);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT `field`, attribute_value FROM ").append(table);
		sql.append(" WHERE company_id = :company_id AND table_name = :table_name ");
		if (restrictModuleName) {
			sql.append("AND module_name = :module_name ");
		}
		sql.append("AND data_id = :data_id AND `field` IN (:fields) ORDER BY id ASC");
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_PAGES_TEMPLATE);
		if (restrictModuleName) {
			p.addValue("module_name", MODULE_PAGES_TEMPLATE);
		}
		p.addValue("data_id", dataId);
		p.addValue("fields", PAGES_TEMPLATE_DETAIL_LANG_FIELDS);
		Map<String, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql.toString(),
					p,
					rs -> {
						String field = rs.getString("field");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(field) || !StringUtils.hasText(av)) {
							return;
						}
						out.putIfAbsent(field, av);
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}

	/**
	 * Batch-load translated {@code name} values for shop menu rows from the per-locale mod tables.
	 *
	 * <p>Reads {@code outside_item_multi_lang_mod_lang_*} rows (same source as border shop menu queries).
	 *
	 * @return map {@code data_id -> attribute_value} for non-empty values only
	 */
	public Map<Long, String> findShopMenuNamesByLocale(long companyId, Collection<Long> dataIds, String localeTag) {
		if (dataIds == null || dataIds.isEmpty()) {
			return Map.of();
		}
		String lang = localeTag != null ? localeTag.trim() : "";
		if (!StringUtils.hasText(lang)) {
			return Map.of();
		}
		return loadShopMenuNamesFromOutsideTable(companyId, dataIds, lang);
	}

	private Map<Long, String> loadShopMenuNamesFromOutsideTable(
			long companyId, Collection<Long> dataIds, String localeTag) {
		List<Long> ids = new ArrayList<>(dataIds);
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(localeTag);
		String sql = "SELECT data_id, attribute_value FROM "
				+ table
				+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
				+ "AND data_id IN (:ids)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_SHOP_MENU);
		p.addValue("field", FIELD_NAME);
		p.addValue("ids", ids);
		Map<Long, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(av)) {
							return;
						}
						out.putIfAbsent(dataId, av);
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}

	/**
	 * Batch-load translated {@code role_name} values for {@code companys_roles} rows from the per-locale mod tables.
	 *
	 * @return map {@code data_id -> attribute_value} for non-empty values only
	 */
	public Map<Long, String> findRolesRoleNamesByLocale(long companyId, Collection<Long> roleIds, String localeTag) {
		if (roleIds == null || roleIds.isEmpty()) {
			return Map.of();
		}
		String lang = localeTag != null ? localeTag.trim() : "";
		if (!StringUtils.hasText(lang)) {
			return Map.of();
		}
		if (LANG_ZH_CN.equalsIgnoreCase(lang)) {
			return loadRolesRoleNamesCn(companyId, roleIds);
		}
		if (LANG_EN_CN.equalsIgnoreCase(lang)) {
			return loadRolesRoleNamesEn(companyId, roleIds);
		}
		if (LANG_AR_SA.equalsIgnoreCase(lang)) {
			return loadRolesRoleNamesAr(companyId, roleIds);
		}
		// zh-TW and future locales: route by shard suffix (zhtw, ...)
		return loadRolesRoleNamesFromOutside(companyId, roleIds, normalizeLangTableSuffix(lang));
	}

	private Map<Long, String> loadRolesRoleNamesCn(long companyId, Collection<Long> roleIds) {
		return loadRolesRoleNamesFromOutside(companyId, roleIds, normalizeLangTableSuffix(LANG_ZH_CN));
	}

	private Map<Long, String> loadRolesRoleNamesEn(long companyId, Collection<Long> roleIds) {
		return loadRolesRoleNamesFromOutside(companyId, roleIds, normalizeLangTableSuffix(LANG_EN_CN));
	}

	private Map<Long, String> loadRolesRoleNamesAr(long companyId, Collection<Long> roleIds) {
		return loadRolesRoleNamesFromOutside(companyId, roleIds, normalizeLangTableSuffix(LANG_AR_SA));
	}

	private Map<Long, String> loadRolesRoleNamesFromOutside(
			long companyId, Collection<Long> roleIds, String langTableSuffix) {
		List<Long> ids = new ArrayList<>(roleIds);
		String table = OUTSIDE_LANG_TABLE_PREFIX + langTableSuffix;
		String sql =
				"SELECT data_id, attribute_value FROM "
						+ table
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND `field` = :field AND data_id IN (:ids)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_COMPANYS_ROLES);
		p.addValue("module_name", MODULE_COMPANYS_ROLES);
		p.addValue("field", FIELD_ROLE_NAME);
		p.addValue("ids", ids);
		Map<Long, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av)) {
							out.putIfAbsent(dataId, av);
						}
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}

	/**
	 * Batch-load {@code role_name} translations for {@code companys_roles} rows from CN / EN / AR mod tables.
	 *
	 * @return map {@code role_id -> (locale tag -> attribute_value)}; only locales with non-empty values appear
	 */
	public Map<Long, Map<String, String>> findRolesRoleNamesAllLocales(long companyId, Collection<Long> roleIds) {
		if (roleIds == null || roleIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Map<String, String>> out = new LinkedHashMap<>();
		mergeRoleLocaleMap(out, LANG_ZH_CN, loadRolesRoleNamesCn(companyId, roleIds));
		mergeRoleLocaleMap(out, LANG_EN_CN, loadRolesRoleNamesEn(companyId, roleIds));
		mergeRoleLocaleMap(out, LANG_AR_SA, loadRolesRoleNamesAr(companyId, roleIds));
		return out;
	}

	private static void mergeRoleLocaleMap(
			Map<Long, Map<String, String>> acc, String localeTag, Map<Long, String> byRoleId) {
		if (byRoleId == null || byRoleId.isEmpty()) {
			return;
		}
		for (Map.Entry<Long, String> e : byRoleId.entrySet()) {
			if (e.getKey() == null || !StringUtils.hasText(e.getValue())) {
				continue;
			}
			acc.computeIfAbsent(e.getKey(), k -> new LinkedHashMap<>()).put(localeTag, e.getValue());
		}
	}

	/**
	 * Batch-load translated {@code category_name} values for article category rows from the per-locale mod
	 * tables.
	 *
	 * @return map {@code data_id -> attribute_value} for non-empty values only
	 */
	public Map<Long, String> findArticleCategoryCategoryNamesByLocale(
			long companyId, Collection<Long> dataIds, String localeTag) {
		if (dataIds == null || dataIds.isEmpty()) {
			return Map.of();
		}
		String lang = localeTag != null ? localeTag.trim() : "";
		if (!StringUtils.hasText(lang)) {
			return Map.of();
		}
		if (LANG_ZH_CN.equalsIgnoreCase(lang)) {
			return loadArticleCategoryCategoryNamesCn(companyId, dataIds);
		}
		if (LANG_EN_CN.equalsIgnoreCase(lang)) {
			return loadArticleCategoryCategoryNamesEn(companyId, dataIds);
		}
		if (LANG_AR_SA.equalsIgnoreCase(lang)) {
			return loadArticleCategoryCategoryNamesAr(companyId, dataIds);
		}
		return Map.of();
	}

	/**
	 * Batch-load {@code category_name} translations for article category rows from CN / EN / AR mod tables.
	 *
	 * @return map {@code data_id -> (locale tag -> attribute_value)}; only locales with non-empty values appear
	 */
	public Map<Long, Map<String, String>> findArticleCategoryCategoryNamesAllLocales(
			long companyId, Collection<Long> dataIds) {
		if (dataIds == null || dataIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Map<String, String>> out = new LinkedHashMap<>();
		mergeArticleCategoryLocaleMap(out, LANG_ZH_CN, loadArticleCategoryCategoryNamesCn(companyId, dataIds));
		mergeArticleCategoryLocaleMap(out, LANG_EN_CN, loadArticleCategoryCategoryNamesEn(companyId, dataIds));
		mergeArticleCategoryLocaleMap(out, LANG_AR_SA, loadArticleCategoryCategoryNamesAr(companyId, dataIds));
		return out;
	}

	private static void mergeArticleCategoryLocaleMap(
			Map<Long, Map<String, String>> acc, String localeTag, Map<Long, String> byDataId) {
		if (byDataId == null || byDataId.isEmpty()) {
			return;
		}
		for (Map.Entry<Long, String> e : byDataId.entrySet()) {
			if (e.getKey() == null || !StringUtils.hasText(e.getValue())) {
				continue;
			}
			acc.computeIfAbsent(e.getKey(), k -> new LinkedHashMap<>()).put(localeTag, e.getValue());
		}
	}

	private Map<Long, String> loadArticleCategoryCategoryNamesCn(long companyId, Collection<Long> dataIds) {
		return loadArticleCategoryNamesFromOutsideTable(companyId, dataIds, normalizeLangTableSuffix(LANG_ZH_CN));
	}

	private Map<Long, String> loadArticleCategoryCategoryNamesEn(long companyId, Collection<Long> dataIds) {
		return loadArticleCategoryNamesFromOutsideTable(companyId, dataIds, normalizeLangTableSuffix(LANG_EN_CN));
	}

	private Map<Long, String> loadArticleCategoryCategoryNamesAr(long companyId, Collection<Long> dataIds) {
		return loadArticleCategoryNamesFromOutsideTable(companyId, dataIds, normalizeLangTableSuffix(LANG_AR_SA));
	}

	/**
	 * List enrichment uses per-locale {@code outside_item_multi_lang_mod_lang_*} rows filtered by
	 * {@code company_id}, {@code table_name}, {@code field}, {@code data_id} (no {@code module_name}).
	 */
	private Map<Long, String> loadArticleCategoryNamesFromOutsideTable(
			long companyId, Collection<Long> dataIds, String langTableSuffix) {
		List<Long> ids = new ArrayList<>(dataIds);
		String table = OUTSIDE_LANG_TABLE_PREFIX + langTableSuffix;
		String sql = "SELECT data_id, attribute_value FROM "
				+ table
				+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
				+ "AND data_id IN (:ids)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_ARTICLE_CATEGORY);
		p.addValue("field", FIELD_CATEGORY_NAME);
		p.addValue("ids", ids);
		Map<Long, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(av)) {
							return;
						}
						out.putIfAbsent(dataId, av);
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}


	public List<Long> filterArticleIdsByTitleContainsForList(
			String requestLang, String tableName, String fieldName, String containsNeedle) {
		if (!StringUtils.hasText(containsNeedle)) {
			return List.of();
		}
		String lang = requestLang == null ? "" : requestLang.trim();
		if (!LANG_ZH_CN.equalsIgnoreCase(lang)
				&& !LANG_EN_CN.equalsIgnoreCase(lang)
				&& !LANG_AR_SA.equalsIgnoreCase(lang)) {
			return List.of();
		}
		String like = "%" + escapeLike(containsNeedle.trim()) + "%";
		String outsideTable = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(lang);
		String sql =
				"SELECT DISTINCT data_id FROM "
						+ outsideTable
						+ " WHERE table_name = :table_name AND field = :field_name AND attribute_value LIKE :like_pat";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("table_name", tableName);
		p.addValue("field_name", fieldName);
		p.addValue("like_pat", like);
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						if (dataId > 0L) {
							out.add(dataId);
						}
					});
		} catch (DataAccessException ignored) {
			return List.of();
		}
		return new ArrayList<>(out);
	}

	public void enrichArticleRowsWithListLang(List<Map<String, Object>> rows, String requestLang) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		String lang = requestLang == null ? "" : requestLang.trim();
		if (!StringUtils.hasText(lang)) {
			return;
		}
		if (!LANG_ZH_CN.equalsIgnoreCase(lang)
				&& !LANG_EN_CN.equalsIgnoreCase(lang)
				&& !LANG_AR_SA.equalsIgnoreCase(lang)) {
			return;
		}

		List<Long> ids = new ArrayList<>();
		Map<Long, Map<String, Object>> rowByArticleId = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			Object aid = row.get("article_id");
			if (!(aid instanceof Number n)) {
				continue;
			}
			long id = n.longValue();
			if (id <= 0L) {
				continue;
			}
			ids.add(id);
			rowByArticleId.put(id, row);
		}
		if (ids.isEmpty()) {
			return;
		}

		applyArticleListLangFromOutsideTable(ids, rowByArticleId, lang);
	}

	public void enrichWeappCustomizePageListRows(long companyId, List<Map<String, Object>> rows, String requestLang) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		String lang = requestLang == null ? "" : requestLang.trim();
		if (!StringUtils.hasText(lang)) {
			return;
		}
		if (!LANG_ZH_CN.equalsIgnoreCase(lang)
				&& !LANG_EN_CN.equalsIgnoreCase(lang)
				&& !LANG_AR_SA.equalsIgnoreCase(lang)) {
			return;
		}

		List<Long> ids = new ArrayList<>();
		Map<Long, Map<String, Object>> rowByDataId = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long id = parsePositiveLong(row.get("id"));
			if (id <= 0L) {
				continue;
			}
			ids.add(id);
			rowByDataId.put(id, row);
		}
		if (ids.isEmpty()) {
			return;
		}

		String langRaw = extractPrimaryLanguageTag(lang);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			canonicalTag = LANG_ZH_CN;
		}

		Map<Long, Map<String, String>> outside =
				loadWeappCustomizePageListFieldsFromOutside(companyId, ids, normalizeLangTableSuffix(canonicalTag));

		for (Map.Entry<Long, Map<String, Object>> e : rowByDataId.entrySet()) {
			long dataId = e.getKey();
			Map<String, Object> row = e.getValue();
			Map<String, String> byField = outside.get(dataId);
			if (byField == null || byField.isEmpty()) {
				continue;
			}
			for (String field : WEAPP_CUSTOMIZE_PAGE_LIST_LANG_FIELDS) {
				String modValue = byField.get(field);
				if (modValue == null || isEffectivelyEmptyForLangOverlay(modValue)) {
					continue;
				}
				row.put(field, modValue);
				Map<String, Object> langMap = new LinkedHashMap<>();
				langMap.put(canonicalTag, modValue);
				row.put(field + "_lang", langMap);
			}
		}
	}


	private Map<Long, Map<String, String>> loadWeappCustomizePageListFieldsFromOutside(long companyId, List<Long> dataIds,
			String langTableSuffix) {
		if (dataIds == null || dataIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = new ArrayList<>(dataIds);
		String table = OUTSIDE_LANG_TABLE_PREFIX + langTableSuffix;
		String sql =
				"SELECT data_id, field, attribute_value FROM "
						+ table
						+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` IN (:fields) "
						+ "AND data_id IN (:ids) ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_WECHAT_WEAPP_CUSTOMIZE_PAGE);
		p.addValue("fields", WEAPP_CUSTOMIZE_PAGE_LIST_LANG_FIELDS);
		p.addValue("ids", ids);
		Map<Long, Map<String, String>> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String field = rs.getString("field");
						String av = rs.getString("attribute_value");
						if (dataId <= 0L || !StringUtils.hasText(field) || !WEAPP_CUSTOMIZE_PAGE_LIST_LANG_FIELDS.contains(field)) {
							return;
						}
						if (!StringUtils.hasText(av) || isEffectivelyEmptyForLangOverlay(av)) {
							return;
						}
						out.computeIfAbsent(dataId, k -> new LinkedHashMap<>()).put(field, av);
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}


	private void applyArticleListLangFromOutsideTable(
			List<Long> ids, Map<Long, Map<String, Object>> rowByArticleId, String requestLang) {
		String outsideTable = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(requestLang);
		String sql =
				"SELECT id, data_id, field, attribute_value FROM "
						+ outsideTable
						+ " WHERE table_name = :table_name AND data_id IN (:ids) AND field IN (:fields) ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("table_name", TABLE_COMPANYS_ARTICLE);
		p.addValue("ids", ids);
		p.addValue("fields", ARTICLE_LIST_LANG_FIELDS);
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String field = rs.getString("field");
						String attributeValue = rs.getString("attribute_value");
						applyOneArticleLangRow(rowByArticleId, dataId, field, attributeValue, requestLang);
					});
		} catch (DataAccessException ignored) {
			// table missing or DB error: leave rows unchanged
		}
	}

	private void applyOneArticleLangRow(
			Map<Long, Map<String, Object>> rowByArticleId,
			long dataId,
			String field,
			String attributeValue,
			String requestLang) {
		if (dataId <= 0L || !StringUtils.hasText(field)) {
			return;
		}
		if (!ARTICLE_LIST_LANG_FIELDS.contains(field)) {
			return;
		}
		Map<String, Object> row = rowByArticleId.get(dataId);
		if (row == null) {
			return;
		}
		String raw = attributeValue != null ? attributeValue : "";
		Object parsed = parseArticleLangAttributeValue(field, raw);
		if (isEffectivelyEmptyForLangOverlay(parsed)) {
			return;
		}
		row.put(field, parsed);
		@SuppressWarnings("unchecked")
		Map<String, Object> langMap =
				(Map<String, Object>)
						row.computeIfAbsent(field + "_lang", k -> new LinkedHashMap<String, Object>());
		langMap.put(requestLang, parsed);
	}

	private Object parseArticleLangAttributeValue(String field, String raw) {
		if ("content".equals(field) || "regions".equals(field)) {
			if (!StringUtils.hasText(raw)) {
				return raw;
			}
			try {
				JsonNode node = objectMapper.readTree(raw);
				if (node == null || node.isNull()) {
					return null;
				}
				return objectMapper.convertValue(node, Object.class);
			} catch (JsonProcessingException e) {
				return raw;
			}
		}
		return raw;
	}

	/**
	 * True when the parsed overlay value should not replace the row or populate {@code *_lang} (blank string,
	 * {@code "0"}, empty collection/map, false, numeric zero).
	 */
	private static boolean isEffectivelyEmptyForLangOverlay(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		return false;
	}

	private static String escapeLike(String needle) {
		if (needle == null) {
			return "";
		}
		return needle
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}
