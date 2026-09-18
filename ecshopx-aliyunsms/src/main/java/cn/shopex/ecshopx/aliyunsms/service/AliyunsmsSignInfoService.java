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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class AliyunsmsSignInfoService {

	private static final String TABLE_ALIYUNSMS_SIGN = "aliyunsms_sign";
	private static final String MODULE_ALIYUNSMS_SIGN = "aliyunsms_sign";
	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private final SignMapper signMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AliyunsmsSignInfoService(SignMapper signMapper, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.signMapper = signMapper;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public Map<String, Object> getInfo(Long id, HttpServletRequest request) {
		if (id == null) {
			return new LinkedHashMap<>();
		}
		Sign row = signMapper.selectById(id);
		if (row == null) {
			return new LinkedHashMap<>();
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", row.getId());
		body.put("company_id", row.getCompanyId());
		body.put("sign_name", row.getSignName());
		body.put("sign_source", row.getSignSource());
		body.put("remark", row.getRemark());
		body.put("sign_file", row.getSignFile());
		body.put("delegate_file", row.getDelegateFile());
		body.put("status", row.getStatus());
		body.put("reason", emptyReasonToNull(row.getReason()));
		body.put("third_party", row.getThirdParty());
		body.put("qualification_id", row.getQualificationId());
		body.put("created", row.getCreated());
		body.put("updated", row.getUpdated());

		String raw = request.getParameter("country_code");
		applyOutsideItemMultiLangOverlay(body, row.getId(), raw);

		if (!body.isEmpty()) {
			body.put(
					"third_party",
					Integer.valueOf(1).equals(row.getThirdParty()) ? "true" : "false");
		}

		return body;
	}

	/**
	 * Resolves a locale tag from {@code country_code} (blank → {@code zh-CN}, trim, underscores → hyphens). When the tag
	 * matches {@code zh-CN}, {@code en-CN}, or {@code ar-SA} (case-insensitive for {@code zh-CN}), loads rows from
	 * {@code outside_item_multi_lang_mod_lang_{suffix}} — {@code suffix} is the tag with hyphens removed — and merges
	 * translated {@code sign_name}, {@code remark}, and {@code reason} into {@code body} (including {@code *_lang} maps).
	 * For any other locale, leaves {@code body} unchanged.
	 */
	private void applyOutsideItemMultiLangOverlay(Map<String, Object> body, long dataId, String rawCountryCode) {
		String tag;
		if (rawCountryCode == null || rawCountryCode.isBlank()) {
			tag = LANG_ZH_CN;
		} else {
			tag = rawCountryCode.trim().replace('_', '-');
		}
		if (!StringUtils.hasText(tag)) {
			tag = LANG_ZH_CN;
		}
		if (LANG_ZH_CN.equalsIgnoreCase(tag)) {
			loadAndApplyOutsideLangRows(body, dataId, LANG_ZH_CN);
		} else if (LANG_EN_CN.equalsIgnoreCase(tag)) {
			loadAndApplyOutsideLangRows(body, dataId, LANG_EN_CN);
		} else if (LANG_AR_SA.equalsIgnoreCase(tag)) {
			loadAndApplyOutsideLangRows(body, dataId, LANG_AR_SA);
		}
	}

	private void loadAndApplyOutsideLangRows(Map<String, Object> body, long dataId, String canonicalLocaleKey) {
		String suffix = normalizeLangTableSuffix(canonicalLocaleKey);
		String table = OUTSIDE_LANG_TABLE_PREFIX + suffix;
		String sql =
				"SELECT `field` AS lang_field, attribute_value AS attribute_value FROM `"
						+ table
						+ "` WHERE table_name = :tableName AND module_name = :moduleName AND data_id = :dataId "
						+ "AND `field` IN ('sign_name','remark','reason')";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("tableName", TABLE_ALIYUNSMS_SIGN);
		params.addValue("moduleName", MODULE_ALIYUNSMS_SIGN);
		params.addValue("dataId", dataId);
		try {
			List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, params);
			for (Map<String, Object> row : rows) {
				Object f = rowValueIgnoreCase(row, "lang_field");
				Object av = rowValueIgnoreCase(row, "attribute_value");
				applyOneLangField(
						body,
						f != null ? f.toString() : null,
						av != null ? av.toString() : null,
						canonicalLocaleKey);
			}
		} catch (DataAccessException ignored) {
			// Missing table or DB error: keep main-row fields only.
		}
	}

	private static Object rowValueIgnoreCase(Map<String, Object> row, String key) {
		for (Map.Entry<String, Object> e : row.entrySet()) {
			if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
	}

	/** Hyphens removed from locale tag; if the result is not alphanumeric, falls back to {@code zhCN}. */
	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}

	private static void applyOneLangField(Map<String, Object> body, String field, String attributeValue, String localeKey) {
		if (field == null) {
			return;
		}
		if (!"sign_name".equals(field) && !"remark".equals(field) && !"reason".equals(field)) {
			return;
		}
		if ("reason".equals(field)) {
			if (attributeValue == null) {
				return;
			}
			String t = attributeValue.trim();
			if ("0".equals(t)) {
				return;
			}
			String valueForBody = t.isEmpty() ? null : t;
			body.put("reason", valueForBody);
			Map<String, String> langMap = new LinkedHashMap<>();
			langMap.put(localeKey, valueForBody);
			body.put("reason_lang", langMap);
			return;
		}
		if (isIgnoredLangAttributeValue(attributeValue)) {
			return;
		}
		body.put(field, attributeValue);
		Map<String, String> langMap = new LinkedHashMap<>();
		langMap.put(localeKey, attributeValue);
		body.put(field + "_lang", langMap);
	}

	private static String emptyReasonToNull(String reason) {
		if (reason == null) {
			return null;
		}
		return reason.isEmpty() ? null : reason;
	}

	/** Blank or single {@code "0"} stored values do not override the main row (same as legacy API). */
	private static boolean isIgnoredLangAttributeValue(String attributeValue) {
		if (attributeValue == null) {
			return true;
		}
		String t = attributeValue.trim();
		if (t.isEmpty()) {
			return true;
		}
		return "0".equals(t);
	}
}
