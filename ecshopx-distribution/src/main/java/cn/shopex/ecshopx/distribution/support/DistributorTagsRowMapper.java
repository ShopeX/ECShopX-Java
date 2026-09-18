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

import cn.shopex.ecshopx.distribution.domain.DistributorTags;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DistributorTagsRowMapper {

	private DistributorTagsRowMapper() {
	}

	public static Map<String, Object> toRow(DistributorTags e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", e.getCompanyId());
		row.put("tag_id", e.getTagId());
		row.put("tag_name", e.getTagName());
		row.put("tag_color", e.getTagColor());
		row.put("font_color", e.getFontColor());
		row.put("description", e.getDescription());
		row.put("tag_icon", e.getTagIcon());
		row.put("front_show", e.getFrontShow());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}
}
