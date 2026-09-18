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

package cn.shopex.ecshopx.espier.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code espier_uploadefile.relation_id} 列由迁移脚本添加；Flyway 默认关闭时需人工执行。
 * 探测列是否存在，避免未迁移环境 SQL 500。
 */
@Component
public class UploadeFileRelationIdSchemaSupport {

	private final JdbcTemplate jdbcTemplate;
	private volatile Boolean columnPresent;

	public UploadeFileRelationIdSchemaSupport(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean isColumnPresent() {
		Boolean cached = columnPresent;
		if (cached != null) {
			return cached;
		}
		synchronized (this) {
			if (columnPresent == null) {
				columnPresent = probeColumnPresent();
			}
			return columnPresent;
		}
	}

	private boolean probeColumnPresent() {
		Integer count =
				jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM information_schema.columns "
								+ "WHERE table_schema = DATABASE() AND table_name = 'espier_uploadefile' "
								+ "AND column_name = 'relation_id'",
						Integer.class);
		return count != null && count > 0;
	}
}
