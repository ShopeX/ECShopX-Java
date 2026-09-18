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

package cn.shopex.ecshopx.distribution.support;

import cn.shopex.ecshopx.distribution.domain.DistributorCategory;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DistributorCategoryRowMapper {

	private DistributorCategoryRowMapper() {
	}

	public static Map<String, Object> toRow(DistributorCategory e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("category_id", e.getCategoryId());
		row.put("company_id", e.getCompanyId());
		row.put("category_name", e.getCategoryName());
		row.put("category_code", e.getCategoryCode());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}
}
