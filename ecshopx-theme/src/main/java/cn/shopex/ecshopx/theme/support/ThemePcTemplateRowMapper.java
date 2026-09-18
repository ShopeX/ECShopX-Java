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

package cn.shopex.ecshopx.theme.support;

import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ThemePcTemplateRowMapper {

	public Map<String, Object> toRowMap(ThemePcTemplate entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("theme_pc_template_id", entity.getThemePcTemplateId());
		row.put("company_id", entity.getCompanyId());
		row.put(
				"distributor_id",
				entity.getDistributorId() == null ? 0 : entity.getDistributorId());
		row.put("template_title", entity.getTemplateTitle());
		row.put("template_description", entity.getTemplateDescription());
		row.put("page_type", entity.getPageType());
		row.put("status", entity.getStatus());
		row.put("version", entity.getVersion());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("deleted_at", null);
		return row;
	}
}
