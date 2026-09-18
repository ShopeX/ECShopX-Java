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

package cn.shopex.ecshopx.promotions.service.multilang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsActivityItemMultiLangReadService {

	private static final String TABLE_NAME = "promotions_point_upvaluation";
	private static final int IN_CHUNK_SIZE = 500;

	private final JdbcTemplate jdbcTemplate;

	public PromotionGroupsActivityItemMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void applyBatch(
			long companyId,
			List<Long> groupsActivityIds,
			Map<Long, Map<String, Object>> rowByActivityId,
			String requestLangTag) {
		if (groupsActivityIds == null
				|| groupsActivityIds.isEmpty()
				|| rowByActivityId == null
				|| rowByActivityId.isEmpty()) {
			return;
		}
		String suffix = PromotionGroupsActivityMultiLangWriteService.resolveItemMultiLangTableSuffix(requestLangTag);
		String langTable = "item_multi_lang_mod_lang_" + suffix;
		List<Long> distinctIds = groupsActivityIds.stream().distinct().toList();
		for (int i = 0; i < distinctIds.size(); i += IN_CHUNK_SIZE) {
			List<Long> chunk = distinctIds.subList(i, Math.min(i + IN_CHUNK_SIZE, distinctIds.size()));
			String placeholders = chunk.stream().map(id -> "?").collect(Collectors.joining(","));
			String sql =
					"SELECT data_id, `field`, attribute_value FROM `"
							+ langTable
							+ "` WHERE company_id = ? AND table_name = ? AND data_id IN ("
							+ placeholders
							+ ") AND `field` IN ('act_name','pics','share_desc')";
			List<Object> args = new ArrayList<>();
			args.add(companyId);
			args.add(TABLE_NAME);
			args.addAll(chunk);
			jdbcTemplate.query(
					sql,
					rs -> {
						long dataId = rs.getLong("data_id");
						String field = rs.getString("field");
						String attributeValue = rs.getString("attribute_value");
						if (!StringUtils.hasText(attributeValue)) {
							return;
						}
						Map<String, Object> row = rowByActivityId.get(dataId);
						if (row != null) {
							row.put(field, attributeValue);
						}
					},
					args.toArray());
		}
	}
}
