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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
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
public class AliyunsmsSignListService {

	private static final String TABLE_ALIYUNSMS_SIGN = "aliyunsms_sign";
	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private final SignMapper signMapper;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AliyunsmsSignListService(SignMapper signMapper, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.signMapper = signMapper;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public Map<String, Object> getList(long companyId, SignListQuery query, HttpServletRequest request) {
		String canonicalLocale = resolveCanonicalLocaleKey(request);

		LambdaQueryWrapper<Sign> w = new LambdaQueryWrapper<>();
		w.eq(Sign::getCompanyId, companyId);
		if (query.isStatusKeyPresent()) {
			w.eq(Sign::getStatus, query.getStatusValue());
		}

		boolean signNameFilterActive = query.isSignNameFilterActive();
		String signNameContains = query.getSignNameContains();

		boolean useSignNameLike = false;
		if (signNameFilterActive) {
			boolean resolvedByLangIds = false;
			if (canonicalLocale != null) {
				try {
					List<Long> langIds = loadOutsideFilterByLangDataIds(canonicalLocale, signNameContains);
					if (!langIds.isEmpty()) {
						w.in(Sign::getId, langIds);
						resolvedByLangIds = true;
					}
				} catch (DataAccessException ignored) {
					// JDBC access failed; fall back to filtering by sign_name on the main table with LIKE.
				}
			}
			if (!resolvedByLangIds) {
				useSignNameLike = true;
			}
		}
		if (useSignNameLike) {
			String escaped = escapeLikeContains(signNameContains);
			w.like(Sign::getSignName, "%" + escaped + "%");
		}

		w.select(
				Sign::getId,
				Sign::getCompanyId,
				Sign::getSignName,
				Sign::getSignSource,
				Sign::getRemark,
				Sign::getReason,
				Sign::getStatus,
				Sign::getCreated);
		w.orderByDesc(Sign::getCreated);

		Page<Sign> page = new Page<>(query.getPage(), query.getPageSize());
		signMapper.selectPage(page, w);

		long total = page.getTotal();
		if (total == 0) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		List<Sign> records = page.getRecords();
		List<Map<String, Object>> list = new ArrayList<>(records.size());
		for (Sign row : records) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", row.getId());
			m.put("company_id", row.getCompanyId());
			m.put("sign_name", row.getSignName());
			m.put("sign_source", row.getSignSource());
			m.put("remark", row.getRemark());
			m.put("reason", row.getReason());
			m.put("status", row.getStatus());
			m.put("created", row.getCreated());
			list.add(m);
		}

		if (canonicalLocale != null) {
			try {
				applyListOutsideLangOverlay(list, canonicalLocale);
			} catch (DataAccessException ignored) {
				// Keep main-row fields only.
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private void applyListOutsideLangOverlay(List<Map<String, Object>> list, String canonicalLocaleKey) {
		if (list.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>(list.size());
		for (Map<String, Object> row : list) {
			Object idObj = row.get("id");
			if (idObj instanceof Number n) {
				ids.add(n.longValue());
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String suffix = normalizeLangTableSuffix(canonicalLocaleKey);
		String table = OUTSIDE_LANG_TABLE_PREFIX + suffix;
		String sql =
				"SELECT data_id, `field` AS lang_field, attribute_value FROM `"
						+ table
						+ "` WHERE table_name = :tableName AND `field` IN ('sign_name','remark','reason') AND data_id IN (:ids)";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("tableName", TABLE_ALIYUNSMS_SIGN);
		params.addValue("ids", ids);
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, params);
		Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
		for (Map<String, Object> row : list) {
			Object idObj = row.get("id");
			if (idObj instanceof Number n) {
				byId.put(n.longValue(), row);
			}
		}
		for (Map<String, Object> dbRow : rows) {
			Object did = rowValueIgnoreCase(dbRow, "data_id");
			if (!(did instanceof Number n)) {
				continue;
			}
			Map<String, Object> target = byId.get(n.longValue());
			if (target == null) {
				continue;
			}
			Object f = rowValueIgnoreCase(dbRow, "lang_field");
			Object av = rowValueIgnoreCase(dbRow, "attribute_value");
			applyOneLangField(
					target,
					f != null ? f.toString() : null,
					av != null ? av.toString() : null,
					canonicalLocaleKey);
		}
	}

	private List<Long> loadOutsideFilterByLangDataIds(String canonicalLocaleKey, String containsRaw) {
		String suffix = normalizeLangTableSuffix(canonicalLocaleKey);
		String table = OUTSIDE_LANG_TABLE_PREFIX + suffix;
		String escaped = escapeLikeContains(containsRaw);
		String pattern = "%" + escaped + "%";
		String sql =
				"SELECT DISTINCT data_id FROM `"
						+ table
						+ "` WHERE table_name = :tableName AND `field` = 'sign_name' AND attribute_value LIKE :pat";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("tableName", TABLE_ALIYUNSMS_SIGN);
		params.addValue("pat", pattern);
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, params);
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object v = rowValueIgnoreCase(row, "data_id");
			if (v instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out;
	}

	private static String escapeLikeContains(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static String resolveCanonicalLocaleKey(HttpServletRequest request) {
		String raw = request.getParameter("country_code");
		String tag;
		if (raw == null || raw.isBlank()) {
			tag = LANG_ZH_CN;
		} else {
			tag = raw.trim().replace('_', '-');
		}
		if (!StringUtils.hasText(tag)) {
			tag = LANG_ZH_CN;
		}
		if (LANG_ZH_CN.equalsIgnoreCase(tag)) {
			return LANG_ZH_CN;
		}
		if (LANG_EN_CN.equalsIgnoreCase(tag)) {
			return LANG_EN_CN;
		}
		if (LANG_AR_SA.equalsIgnoreCase(tag)) {
			return LANG_AR_SA;
		}
		return null;
	}

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}

	private static Object rowValueIgnoreCase(Map<String, Object> row, String key) {
		for (Map.Entry<String, Object> e : row.entrySet()) {
			if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
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

	public static final class SignListQuery {
		private int page = 1;
		private int pageSize = 10;
		private boolean signNameFilterActive;
		private String signNameContains;
		private boolean statusKeyPresent;
		private String statusValue;

		public int getPage() {
			return page;
		}

		public void setPage(int page) {
			this.page = page;
		}

		public int getPageSize() {
			return pageSize;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public boolean isSignNameFilterActive() {
			return signNameFilterActive;
		}

		public void setSignNameFilterActive(boolean signNameFilterActive) {
			this.signNameFilterActive = signNameFilterActive;
		}

		public String getSignNameContains() {
			return signNameContains;
		}

		public void setSignNameContains(String signNameContains) {
			this.signNameContains = signNameContains;
		}

		public boolean isStatusKeyPresent() {
			return statusKeyPresent;
		}

		public void setStatusKeyPresent(boolean statusKeyPresent) {
			this.statusKeyPresent = statusKeyPresent;
		}

		public String getStatusValue() {
			return statusValue;
		}

		public void setStatusValue(String statusValue) {
			this.statusValue = statusValue;
		}
	}
}
