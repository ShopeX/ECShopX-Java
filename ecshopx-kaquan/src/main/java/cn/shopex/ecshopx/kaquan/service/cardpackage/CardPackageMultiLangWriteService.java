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

import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CardPackageMultiLangWriteService {

	private static final String TABLE = "card_package";
	private static final String DEFAULT_LANG = "zh-CN";
	private static final String TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private final JdbcTemplate jdbcTemplate;

	public CardPackageMultiLangWriteService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 创建卡券包后，将标题与描述写入 {@code outside_item_multi_lang_mod_lang_*}（模块名与表名均为 {@code card_package}，默认语言
	 * zh-CN）。
	 */
	public void writeAfterCreate(long packageId, long companyId, String title, String packageDescribe) {
		upsertField(companyId, packageId, "title", title == null ? "" : title);
		upsertField(companyId, packageId, "package_describe", packageDescribe == null ? "" : packageDescribe);
	}

	/**
	 * 更新卡券包主表后，同步 {@code outside_item_multi_lang_mod_lang_*} 中默认语言的标题与描述；无对应行时插入。
	 */
	public void syncFieldsAfterUpdate(long packageId, long companyId, String title, String packageDescribe) {
		writeAfterCreate(packageId, companyId, title, packageDescribe);
	}

	private void upsertField(long companyId, long dataId, String field, String attributeValue) {
		if (dataId <= 0L || !StringUtils.hasText(field)) {
			return;
		}
		String shard = TABLE_PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(DEFAULT_LANG);
		int now = (int) (System.currentTimeMillis() / 1000L);
		String value = attributeValue == null ? "" : attributeValue;
		int updated =
				jdbcTemplate.update(
						"UPDATE `"
								+ shard
								+ "` SET attribute_value = ?, updated = ?, company_id = ? "
								+ "WHERE table_name = ? AND module_name = ? AND data_id = ? AND `field` = ?",
						value,
						now,
						companyId,
						TABLE,
						TABLE,
						dataId,
						field);
		if (updated == 0) {
			jdbcTemplate.update(
					"INSERT INTO `"
							+ shard
							+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
							+ "VALUES (?,?,?,?,?,?,?,?)",
					companyId,
					field,
					value,
					TABLE,
					TABLE,
					dataId,
					now,
					now);
		}
	}
}
