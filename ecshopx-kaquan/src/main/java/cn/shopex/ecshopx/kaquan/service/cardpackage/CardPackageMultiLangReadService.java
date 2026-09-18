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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

/**
 * 从语言分表覆盖卡券包 {@code title}、{@code package_describe}。
 */
@Service
public class CardPackageMultiLangReadService {

	private static final String TABLE = "card_package";

	private final JdbcTemplate jdbcTemplate;

	public CardPackageMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private static String outsideItemLangShardTable(String lang) {
		String suffix = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		if (suffix.isEmpty()) {
			suffix = "zhCN";
		} else if (!suffix.matches("[A-Za-z0-9_]+")) {
			suffix = "zhCN";
		}
		return "outside_item_multi_lang_mod_lang_" + suffix;
	}

	/**
	 * 详情：按请求 {@code country_code} 选语言分表（空/blank → zh-CN）；先按
	 * {@code module_name} 过滤；若未覆盖 {@code title}/{@code package_describe}，再与列表一致做一次无
	 * {@code module_name} 查询（历史数据里 module 与表名可能不一致，需二次合并文案）。
	 */
	public void overlayForDetail(long companyId, long packageId, Map<String, Object> target, String countryCode) {
		String effectiveLang = effectiveLang(countryCode);
		String initialTitle = stringOrEmpty(target.get("title"));
		String initialDescribe = stringOrEmpty(target.get("package_describe"));
		doOverlay(companyId, packageId, target, effectiveLang, true);
		if (stringOrEmpty(target.get("title")).equals(initialTitle)
				&& stringOrEmpty(target.get("package_describe")).equals(initialDescribe)) {
			doOverlay(companyId, packageId, target, effectiveLang, false);
		}
	}

	private static String stringOrEmpty(Object value) {
		return value == null ? "" : value.toString();
	}

	public void overlay(long companyId, long packageId, Map<String, Object> target) {
		overlayForDetail(companyId, packageId, target, null);
	}

	public void overlay(long companyId, long packageId, Map<String, Object> target, String lang) {
		doOverlay(companyId, packageId, target, effectiveLang(lang), false);
	}

	private static String effectiveLang(String lang) {
		return (lang != null && StringUtils.hasText(lang.trim())) ? lang.trim() : "zh-CN";
	}

	private void doOverlay(
			long companyId,
			long packageId,
			Map<String, Object> target,
			String effectiveLang,
			boolean requireModuleName) {
		if (companyId <= 0L) {
			return;
		}
		String shard = outsideItemLangShardTable(effectiveLang);
		String sql = "SELECT `field` AS attr_field, attribute_value FROM `" + shard + "` WHERE table_name = ? ";
		if (requireModuleName) {
			// getOneLangData: filter has table_name, field, data_id, module_name — no company_id.
			sql += "AND module_name = ? ";
		}
		sql += "AND data_id = ? AND `field` IN ('title','package_describe')";
		List<Map<String, Object>> rows =
				requireModuleName
						? jdbcTemplate.queryForList(sql, TABLE, TABLE, packageId)
						: jdbcTemplate.queryForList(sql, TABLE, packageId);
		for (Map<String, Object> row : rows) {
			Object f = row.get("attr_field");
			if (f == null) {
				f = row.get("ATTR_FIELD");
			}
			Object v = row.get("attribute_value");
			if (f == null) {
				continue;
			}
			String field = f.toString();
			if (("title".equals(field) || "package_describe".equals(field))
					&& v != null
					&& StringUtils.hasText(v.toString())) {
				target.put(field, v.toString());
			}
		}
	}
}
