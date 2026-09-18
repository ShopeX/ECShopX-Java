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

package cn.shopex.ecshopx.kaquan.service.discount;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

/**
 * 从语言分表覆盖卡券 {@code title}、{@code description}（与卡券仓储侧多语言读取一致）。
 */
@Service
public class DiscountCardsMultiLangReadService {

	private static final String TABLE = "kaquan_discount_cards";
	private static final String LANG = "zh-CN";

	private final JdbcTemplate jdbcTemplate;

	public DiscountCardsMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 与卡券列表/详情仓储一致：非 {@code item} 模块走 {@code outside_item_multi_lang_mod_lang_{lang}}，
	 * {@code lang} 中的 {@code -} 去掉后拼表名；查询条件为 {@code table_name}、{@code data_id}、{@code field}。
	 */
	private static String outsideItemLangShardTable(String lang) {
		String suffix = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		if (suffix.isEmpty()) {
			suffix = "zhCN";
		} else if (!suffix.matches("[A-Za-z0-9_]+")) {
			suffix = "zhCN";
		}
		return "outside_item_multi_lang_mod_lang_" + suffix;
	}

	public void overlay(long companyId, long cardId, Map<String, Object> target) {
		if (companyId <= 0L) {
			return;
		}
		String shard = outsideItemLangShardTable(LANG);
		String sql = "SELECT `field` AS attr_field, attribute_value FROM `" + shard
				+ "` WHERE table_name = ? AND data_id = ? AND `field` IN ('title','description')";
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, TABLE, cardId);
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
			if (("title".equals(field) || "description".equals(field))
					&& v != null
					&& StringUtils.hasText(v.toString())) {
				target.put(field, v.toString());
			}
		}
	}
}
