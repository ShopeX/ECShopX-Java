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

import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PagesTemplateRowMapper {

	public Map<String, Object> toRowMap(PagesTemplate entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("pages_template_id", entity.getPagesTemplateId());
		row.put("company_id", entity.getCompanyId());
		row.put("regionauth_id", entity.getRegionauthId());
		row.put("distributor_id", entity.getDistributorId());
		row.put("template_name", entity.getTemplateName());
		row.put("template_title", entity.getTemplateTitle());
		row.put("template_pic", entity.getTemplatePic());
		row.put("template_type", entity.getTemplateType());
		row.put("element_edit_status", entity.getElementEditStatus());
		row.put("status", entity.getStatus());
		row.put("timer_status", entity.getTimerStatus());
		row.put("timer_time", entity.getTimerTime());
		row.put("template_status_modify_time", entity.getTemplateStatusModifyTime());
		row.put("weapp_pages", entity.getWeappPages());
		row.put("template_content", entity.getTemplateContent());
		row.put("lang", entity.getLang());
		return row;
	}
}
